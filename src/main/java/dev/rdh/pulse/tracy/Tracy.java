package dev.rdh.pulse.tracy;

import dev.rdh.pulse.PulseMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Tracy {
	private static final ValueLayout.OfInt INT = ValueLayout.JAVA_INT;
	private static final AddressLayout PTR = ValueLayout.ADDRESS;
	private static final StructLayout ZONE_CONTEXT = MemoryLayout.structLayout(INT, INT);
	private static final StructLayout SOURCE_LOCATION = MemoryLayout.structLayout(PTR, PTR, PTR, INT, INT);

	private static final Map<String, MemorySegment> SOURCE_LOCATIONS = new ConcurrentHashMap<>();
	private static final Map<String, MemorySegment> STRINGS = new ConcurrentHashMap<>();
	private static final ThreadLocal<Zones> ZONES = ThreadLocal.withInitial(Zones::new);

	private static SymbolLookup lookup;
	private static MethodHandle frameMark;
	private static MethodHandle zoneBegin;
	private static MethodHandle zoneEnd;
	private static MethodHandle setThreadName;
	private static MethodHandle connected;
	private static MethodHandle shutdown;
	private static volatile boolean available;

	private Tracy() {}

	public static void init() {
		if (available || !Boolean.getBoolean("pulse.tracy")) {
			return;
		}
		try {
			String version = version();
			lookup = SymbolLookup.libraryLookup(extract(version), Arena.global());

			frameMark = handle("___tracy_emit_frame_mark", FunctionDescriptor.ofVoid(PTR));
			zoneBegin = handle("___tracy_emit_zone_begin", FunctionDescriptor.of(ZONE_CONTEXT, PTR, INT));
			zoneEnd = handle("___tracy_emit_zone_end", FunctionDescriptor.ofVoid(ZONE_CONTEXT));
			setThreadName = handle("___tracy_set_thread_name", FunctionDescriptor.ofVoid(PTR));
			connected = handle("___tracy_connected", FunctionDescriptor.of(INT));
			shutdown = handle("___tracy_shutdown_profiler", FunctionDescriptor.ofVoid());
			handle("___tracy_startup_profiler", FunctionDescriptor.ofVoid()).invokeExact();

			available = true;
			Runtime.getRuntime().addShutdownHook(new Thread(Tracy::shutdown, "pulse-tracy-shutdown"));
			PulseMod.LOG.info("Tracy {} client started; connect a Tracy {} profiler to this machine", version, version);
		} catch (Throwable t) {
			available = false;
			PulseMod.LOG.error("Could not start the Tracy client", t);
		}
	}

	public static void shutdown() {
		if (available) {
			available = false;
			try {
				shutdown.invokeExact();
			} catch (Throwable t) {
				PulseMod.LOG.error("Could not stop the Tracy client", t);
			}
		}
	}

	public static boolean available() {
		return available;
	}

	public static boolean connected() {
		if (!available) {
			return false;
		}
		try {
			return (int) connected.invokeExact() != 0;
		} catch (Throwable t) {
			failed(t);
			return false;
		}
	}

	public static void frameMark() {
		if (available) {
			try {
				frameMark.invokeExact(MemorySegment.NULL);
			} catch (Throwable t) {
				failed(t);
			}
		}
	}

	public static void zoneBegin(String name) {
		if (!available) {
			return;
		}
		Zones zones = ZONES.get();
		if (zones.depth < zones.contexts.length) {
			try {
				MemorySegment ignored = (MemorySegment) zoneBegin.invokeExact((SegmentAllocator) zones, sourceLocation(name), 1);
			} catch (Throwable t) {
				failed(t);
				return;
			}
		}
		zones.depth++;
	}

	public static void zoneEnd() {
		if (!available) {
			return;
		}
		Zones zones = ZONES.get();
		if (zones.depth == 0) {
			return;
		}
		zones.depth--;
		if (zones.depth < zones.contexts.length) {
			try {
				zoneEnd.invokeExact(zones.contexts[zones.depth]);
			} catch (Throwable t) {
				failed(t);
			}
		}
	}

	static void failed(Throwable t) {
		available = false;
		PulseMod.LOG.error("Disabling Tracy after a call failed", t);
	}

	static MethodHandle handle(String symbol, FunctionDescriptor descriptor) throws IOException {
		return Linker.nativeLinker().downcallHandle(lookup.find(symbol).orElseThrow(() -> new IOException("the Tracy client does not export " + symbol)), descriptor);
	}

	static MemorySegment sourceLocation(String name) {
		return SOURCE_LOCATIONS.computeIfAbsent(name, key -> {
			MemorySegment location = Arena.global().allocate(SOURCE_LOCATION);
			location.set(PTR, 0, string(key));
			location.set(PTR, 8, string("Profiler"));
			location.set(PTR, 16, string("minecraft"));
			location.set(INT, 24, 0);
			location.set(INT, 28, 0);
			return location;
		});
	}

	private static MemorySegment string(String value) {
		return STRINGS.computeIfAbsent(value, key -> Arena.global().allocateFrom(key));
	}

	private static String version() throws IOException {
		try (InputStream in = Tracy.class.getResourceAsStream("/natives/tracy-version.txt")) {
			if (in == null) {
				throw new IOException("pulse does not bundle a Tracy client");
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
		}
	}

	private static Path extract(String version) throws IOException {
		String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
		boolean arm = arch.contains("aarch64") || arch.contains("arm64");

		String target;
		String name;
		if (os.contains("win")) {
			target = "windows-x64";
			name = "TracyClient.dll";
			if (arm) {
				throw new IOException("pulse does not bundle a Tracy client for windows on arm");
			}
		} else if (os.contains("mac") || os.contains("darwin")) {
			target = "macos";
			name = "libTracyClient.dylib";
		} else {
			target = arm ? "linux-arm64" : "linux-x64";
			name = "libTracyClient.so";
		}

		Path directory = FabricLoader.getInstance().getConfigDir().resolve("pulse").resolve("natives").resolve(version);
		Path library = directory.resolve(name);
		if (Files.notExists(library)) {
			Files.createDirectories(directory);
			try (InputStream in = Tracy.class.getResourceAsStream("/natives/" + target + "/" + name)) {
				if (in == null) {
					throw new IOException("pulse does not bundle a Tracy client for " + target);
				}
				Path temporary = Files.createTempFile(directory, name, ".tmp");
				Files.copy(in, temporary, StandardCopyOption.REPLACE_EXISTING);
				Files.move(temporary, library, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			}
		}
		return library;
	}

	private static final class Zones implements SegmentAllocator {
		private final MemorySegment[] contexts = new MemorySegment[64];
		private int depth;

		private Zones() {
			Arena arena = Arena.ofAuto();
			for (int i = 0; i < this.contexts.length; i++) {
				this.contexts[i] = arena.allocate(ZONE_CONTEXT);
			}
			try {
				setThreadName.invokeExact(string(Thread.currentThread().getName()));
			} catch (Throwable t) {
				failed(t);
			}
		}

		@Override
		public MemorySegment allocate(long byteSize, long byteAlignment) {
			return this.contexts[this.depth];
		}
	}
}
