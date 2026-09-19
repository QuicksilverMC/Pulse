package dev.rdh.pulse.render;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

public final class FrameStats {
	private static final int WINDOW = 1000;
	private static final int QUERIES = 8;

	private static final long[] frameTimes = new long[WINDOW];
	private static int frameCount;
	private static int frameIndex;
	private static long frameStart;

	private static boolean enabled;
	private static long windowStart;

	private static int[] starts;
	private static int[] ends;
	private static final boolean[] pending = new boolean[QUERIES];
	private static int slot;
	private static boolean issued;
	private static long gpuNanos;

	private static long lastAllocated = -1;

	private static String frameLine;
	private static String gpuSuffix;
	private static String allocationLine;
	private static String cpuLine;

	private FrameStats() {}

	public static void beginFrame(boolean debug) {
		GlInfo.capture();
		long now = System.nanoTime();

		if (!debug) {
			enabled = false;
			return;
		}
		if (!enabled) {
			enabled = true;
			frameCount = 0;
			frameIndex = 0;
			frameStart = now;
			windowStart = now;
			lastAllocated = allocatedBytes();
			frameLine = gpuSuffix = allocationLine = cpuLine = null;
			if (starts == null && timestampsSupported()) {
				starts = new int[QUERIES];
				ends = new int[QUERIES];
				GL15C.glGenQueries(starts);
				GL15C.glGenQueries(ends);
			}
			collectGpu();
			gpuNanos = 0;
		} else {
			frameTimes[frameIndex] = now - frameStart;
			frameIndex = (frameIndex + 1) % WINDOW;
			frameCount = Math.min(frameCount + 1, WINDOW);
			frameStart = now;
		}

		if (starts != null) {
			collectGpu();
			slot = (slot + 1) % QUERIES;
			issued = !pending[slot];
			if (issued) {
				GL33C.glQueryCounter(starts[slot], GL33C.GL_TIMESTAMP);
			}
		}

		if (now - windowStart >= 1_000_000_000L) {
			publish(now - windowStart);
			windowStart = now;
		}
	}

	public static void endFrame() {
		if (enabled && issued) {
			GL33C.glQueryCounter(ends[slot], GL33C.GL_TIMESTAMP);
			pending[slot] = true;
			issued = false;
		}
	}

	private static boolean timestampsSupported() {
		GLCapabilities caps = GL.getCapabilities();
		return caps.OpenGL33 || caps.GL_ARB_timer_query;
	}

	private static void collectGpu() {
		for (int i = 0; i < QUERIES; i++) {
			if (pending[i] && GL15C.glGetQueryObjecti(ends[i], GL15C.GL_QUERY_RESULT_AVAILABLE) != 0) {
				gpuNanos += GL33C.glGetQueryObjectui64(ends[i], GL15C.GL_QUERY_RESULT) - GL33C.glGetQueryObjectui64(starts[i], GL15C.GL_QUERY_RESULT);
				pending[i] = false;
			}
		}
	}

	private static void publish(long windowNanos) {
		if (frameCount > 0) {
			long[] sorted = Arrays.copyOf(frameTimes, frameCount);
			Arrays.sort(sorted);
			long sum = 0;
			for (long t : sorted) {
				sum += t;
			}
			frameLine = "Frame: %.1f avg, %.1f p99, %.1f max ms".formatted(
					sum / (double) frameCount / 1e6,
					sorted[(int) Math.ceil(frameCount * 0.99) - 1] / 1e6,
					sorted[frameCount - 1] / 1e6);
		}

		if (starts != null) {
			gpuSuffix = " GPU: %d%%".formatted(Math.min(100, Math.round(gpuNanos * 100.0 / windowNanos)));
			gpuNanos = 0;
		}

		long allocated = allocatedBytes();
		if (allocated >= 0 && lastAllocated >= 0) {
			allocationLine = "Allocation rate: %dMB/s".formatted(Math.round((allocated - lastAllocated) / (windowNanos / 1e9) / (1024 * 1024)));
		}
		lastAllocated = allocated;

		double cpu = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuLoad();
		cpuLine = cpu >= 0 ? "Process CPU: %d%%".formatted(Math.round(cpu * 100)) : null;
	}

	private static long allocatedBytes() {
		return ((com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean()).getTotalThreadAllocatedBytes();
	}

	public static boolean active() {
		return enabled;
	}

	public static String frameLine() {
		return frameLine;
	}

	public static String gpuSuffix() {
		return gpuSuffix;
	}

	public static String allocationLine() {
		return allocationLine;
	}

	public static String cpuLine() {
		return cpuLine;
	}
}
