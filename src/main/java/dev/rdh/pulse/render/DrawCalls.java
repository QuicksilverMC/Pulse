package dev.rdh.pulse.render;

import dev.rdh.pulse.PulseMod;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DrawCalls {
	private static final ValueLayout I = ValueLayout.JAVA_INT;
	private static final ValueLayout POINTER = ValueLayout.JAVA_LONG;

	private static final Map<String, FunctionDescriptor> FUNCTIONS = Map.ofEntries(
			hook("glDrawArrays", I, I, I),
			hook("glDrawElements", I, I, I, POINTER),
			hook("glDrawRangeElements", I, I, I, I, I, POINTER),
			hook("glMultiDrawArrays", I, POINTER, POINTER, I),
			hook("glMultiDrawElements", I, POINTER, I, POINTER, I),
			hook("glDrawArraysInstanced", I, I, I, I),
			hook("glDrawArraysInstancedARB", I, I, I, I),
			hook("glDrawArraysInstancedEXT", I, I, I, I),
			hook("glDrawElementsInstanced", I, I, I, POINTER, I),
			hook("glDrawElementsInstancedARB", I, I, I, POINTER, I),
			hook("glDrawElementsInstancedEXT", I, I, I, POINTER, I),
			hook("glDrawElementsBaseVertex", I, I, I, POINTER, I),
			hook("glDrawRangeElementsBaseVertex", I, I, I, I, I, POINTER, I),
			hook("glDrawElementsInstancedBaseVertex", I, I, I, POINTER, I, I),
			hook("glMultiDrawElementsBaseVertex", I, POINTER, I, POINTER, I, POINTER),
			hook("glDrawArraysIndirect", I, POINTER),
			hook("glDrawElementsIndirect", I, I, POINTER),
			hook("glMultiDrawArraysIndirect", I, POINTER, I, I),
			hook("glMultiDrawElementsIndirect", I, I, POINTER, I, I),
			hook("glCallList", I),
			hook("glCallLists", I, I, POINTER),
			hook("glEnd")
	);

	private static final MethodHandle COUNT;

	static {
		try {
			COUNT = MethodHandles.lookup().findStatic(DrawCalls.class, "count", MethodType.methodType(void.class));
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final Map<Long, Long> stubs = new HashMap<>();
	private static GLCapabilities patched;
	private static boolean failed;

	private static final long[] calls = new long[Category.VALUES.length];
	private static Category category = Category.OTHER;
	private static int frames;
	private static long windowStart;
	private static List<String> lines = List.of();

	private DrawCalls() {}

	private static Map.Entry<String, FunctionDescriptor> hook(String name, MemoryLayout... arguments) {
		return Map.entry(name, FunctionDescriptor.ofVoid(arguments));
	}

	public static void count() {
		calls[category.ordinal()]++;
	}

	public static Category enter(Category next) {
		Category previous = category;
		category = next;
		return previous;
	}

	public static void exit(Category previous) {
		category = previous;
	}

	public static void frame(boolean enabled) {
		if (failed) {
			return;
		}
		category = Category.OTHER;
		GLCapabilities caps = GL.getCapabilities();
		if (patched != null && (!enabled || patched != caps)) {
			swap(patched, false);
			patched = null;
		}
		if (enabled && patched == null) {
			try {
				swap(caps, true);
				patched = caps;
				Arrays.fill(calls, 0);
				frames = 0;
				windowStart = System.nanoTime();
				lines = List.of("Draw calls: ...");
			} catch (Throwable t) {
				failed = true;
				PulseMod.LOG.error("Could not hook GL draw calls, draw counting is disabled", t);
			}
			return;
		}
		if (patched == null) {
			return;
		}

		frames++;
		long now = System.nanoTime();
		if (now - windowStart >= 1_000_000_000L) {
			lines = format(frames);
			Arrays.fill(calls, 0);
			frames = 0;
			windowStart = now;
		}
	}

	public static List<String> lines() {
		return lines;
	}

	private static List<String> format(int frames) {
		long total = 0;
		for (long c : calls) {
			total += c;
		}
		return List.of(
				"Draw calls: %d/frame".formatted(Math.round((double) total / frames)),
				group(frames, Category.TERRAIN, Category.SKY, Category.ENTITIES, Category.TILES),
				group(frames, Category.PARTICLES, Category.TEXT, Category.GUI, Category.OTHER)
		);
	}

	private static String group(int frames, Category... categories) {
		StringBuilder line = new StringBuilder(" ");
		for (int i = 0; i < categories.length; i++) {
			if (i > 0) {
				line.append(", ");
			}
			line.append(categories[i].label).append(' ').append(Math.round((double) calls[categories[i].ordinal()] / frames));
		}
		return line.toString();
	}

	public enum Category {
		TERRAIN("Terrain"),
		SKY("Sky"),
		ENTITIES("Entities"),
		TILES("BEs"),
		PARTICLES("Particles"),
		TEXT("Text"),
		GUI("GUI"),
		OTHER("Other");

		static final Category[] VALUES = values();

		final String label;

		Category(String label) {
			this.label = label;
		}
	}

	public static boolean active() {
		return patched != null;
	}

	private static void swap(GLCapabilities caps, boolean install) {
		Map<Long, Long> replacements = new HashMap<>();
		for (Map.Entry<String, FunctionDescriptor> function : FUNCTIONS.entrySet()) {
			long original;
			try {
				original = GLCapabilities.class.getField(function.getKey()).getLong(caps);
			} catch (ReflectiveOperationException e) {
				continue;
			}
			if (original == 0L) {
				continue;
			}
			long stub = stubs.containsKey(original) ? stubs.get(original) : stub(original, function.getValue());
			if (install) {
				replacements.put(original, stub);
			} else {
				replacements.put(stub, original);
			}
		}

		PointerBuffer table = caps.getAddressBuffer();
		for (int i = 0; i < table.capacity(); i++) {
			Long replacement = replacements.get(table.get(i));
			if (replacement != null) {
				table.put(i, replacement);
			}
		}
	}

	private static long stub(long original, FunctionDescriptor descriptor) {
		Linker linker = Linker.nativeLinker();
		MethodHandle target = MethodHandles.foldArguments(linker.downcallHandle(MemorySegment.ofAddress(original), descriptor), COUNT);
		long stub = linker.upcallStub(target, descriptor, Arena.global()).address();
		stubs.put(original, stub);
		return stub;
	}
}
