package dev.rdh.pulse.tracy;

import dev.rdh.pulse.PulseMod;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class TracyGpu {
	private static final ValueLayout.OfLong LONG = ValueLayout.JAVA_LONG;
	private static final ValueLayout.OfFloat FLOAT = ValueLayout.JAVA_FLOAT;
	private static final ValueLayout.OfShort SHORT = ValueLayout.JAVA_SHORT;
	private static final ValueLayout.OfByte BYTE = ValueLayout.JAVA_BYTE;

	private static final StructLayout NEW_CONTEXT = MemoryLayout.structLayout(LONG, FLOAT, BYTE, BYTE, BYTE, MemoryLayout.paddingLayout(1));
	private static final StructLayout ZONE_BEGIN = MemoryLayout.structLayout(LONG, SHORT, BYTE, MemoryLayout.paddingLayout(5));
	private static final StructLayout ZONE_END = MemoryLayout.structLayout(SHORT, BYTE, MemoryLayout.paddingLayout(1));
	private static final StructLayout TIME = MemoryLayout.structLayout(LONG, SHORT, BYTE, MemoryLayout.paddingLayout(5));
	private static final StructLayout TIME_SYNC = MemoryLayout.structLayout(LONG, BYTE, MemoryLayout.paddingLayout(7));

	private static final int QUERIES = 4096;
	private static final int DEPTH = 64;
	private static final int SYNC_FRAMES = 100;
	private static final byte CONTEXT = 0;
	private static final byte OPENGL = 1;

	private static MethodHandle zoneBegin;
	private static MethodHandle zoneEnd;
	private static MethodHandle emitTime;
	private static MethodHandle emitTimeSync;

	private static MemorySegment beginData;
	private static MemorySegment endData;
	private static MemorySegment timeData;
	private static MemorySegment syncData;

	private static int[] queries;
	private static int head;
	private static int tail;
	private static int frames;

	private static final boolean[] emitted = new boolean[DEPTH];
	private static int depth;

	private static Thread renderThread;
	private static boolean enabled;
	private static boolean attempted;
	private static boolean active;

	private TracyGpu() {}

	public static void beginFrame() {
		if (!Tracy.available()) {
			return;
		}
		if (!attempted) {
			attempted = true;
			init();
		}
		if (!enabled) {
			return;
		}
		collect();
		if (++frames % SYNC_FRAMES == 0) {
			syncData.set(LONG, 0, GL32C.glGetInteger64(GL33C.GL_TIMESTAMP));
			syncData.set(BYTE, 8, CONTEXT);
			try {
				emitTimeSync.invokeExact(syncData);
			} catch (Throwable t) {
				Tracy.failed(t);
			}
		}
		active = true;
		depth = 0;
	}

	public static void endFrame() {
		active = false;
	}

	public static void zoneBegin(String name) {
		if (!active || Thread.currentThread() != renderThread) {
			return;
		}
		boolean recorded = false;
		if (depth < DEPTH && (tail - head) < QUERIES - 2) {
			int query = tail++ % QUERIES;
			beginData.set(LONG, 0, Tracy.sourceLocation(name).address());
			beginData.set(SHORT, 8, (short) query);
			beginData.set(BYTE, 10, CONTEXT);
			try {
				GL33C.glQueryCounter(queries[query], GL33C.GL_TIMESTAMP);
				zoneBegin.invokeExact(beginData);
				recorded = true;
			} catch (Throwable t) {
				Tracy.failed(t);
			}
		}
		if (depth < DEPTH) {
			emitted[depth] = recorded;
		}
		depth++;
	}

	public static void zoneEnd() {
		if (!active || Thread.currentThread() != renderThread || depth == 0) {
			return;
		}
		depth--;
		if (depth >= DEPTH || !emitted[depth]) {
			return;
		}
		int query = tail++ % QUERIES;
		endData.set(SHORT, 0, (short) query);
		endData.set(BYTE, 2, CONTEXT);
		try {
			GL33C.glQueryCounter(queries[query], GL33C.GL_TIMESTAMP);
			zoneEnd.invokeExact(endData);
		} catch (Throwable t) {
			Tracy.failed(t);
		}
	}

	private static void collect() {
		while (head != tail) {
			int query = head % QUERIES;
			if (GL15C.glGetQueryObjecti(queries[query], GL15C.GL_QUERY_RESULT_AVAILABLE) == 0) {
				return;
			}
			long gpuTime = GL33C.glGetQueryObjectui64(queries[query], GL15C.GL_QUERY_RESULT);
			timeData.set(LONG, 0, gpuTime);
			timeData.set(SHORT, 8, (short) query);
			timeData.set(BYTE, 10, CONTEXT);
			try {
				emitTime.invokeExact(timeData);
			} catch (Throwable t) {
				Tracy.failed(t);
				return;
			}
			head++;
		}
	}

	private static void init() {
		GLCapabilities caps = GL.getCapabilities();
		if (!caps.OpenGL33 && !caps.GL_ARB_timer_query) {
			PulseMod.LOG.info("Tracy GPU zones need ARB_timer_query, which this driver does not provide");
			return;
		}
		try {
			zoneBegin = Tracy.handle("___tracy_emit_gpu_zone_begin", FunctionDescriptor.ofVoid(ZONE_BEGIN));
			zoneEnd = Tracy.handle("___tracy_emit_gpu_zone_end", FunctionDescriptor.ofVoid(ZONE_END));
			emitTime = Tracy.handle("___tracy_emit_gpu_time", FunctionDescriptor.ofVoid(TIME));
			emitTimeSync = Tracy.handle("___tracy_emit_gpu_time_sync", FunctionDescriptor.ofVoid(TIME_SYNC));
			MethodHandle newContext = Tracy.handle("___tracy_emit_gpu_new_context", FunctionDescriptor.ofVoid(NEW_CONTEXT));

			Arena arena = Arena.global();
			beginData = arena.allocate(ZONE_BEGIN);
			endData = arena.allocate(ZONE_END);
			timeData = arena.allocate(TIME);
			syncData = arena.allocate(TIME_SYNC);

			queries = new int[QUERIES];
			GL15C.glGenQueries(queries);

			MemorySegment context = arena.allocate(NEW_CONTEXT);
			context.set(LONG, 0, GL32C.glGetInteger64(GL33C.GL_TIMESTAMP));
			context.set(FLOAT, 8, 1.0f);
			context.set(BYTE, 12, CONTEXT);
			context.set(BYTE, 13, (byte) 0);
			context.set(BYTE, 14, OPENGL);
			newContext.invokeExact(context);

			renderThread = Thread.currentThread();
			enabled = true;
			PulseMod.LOG.info("Tracy GPU zones enabled");
		} catch (Throwable t) {
			enabled = false;
			PulseMod.LOG.error("Could not set up Tracy GPU zones", t);
		}
	}
}
