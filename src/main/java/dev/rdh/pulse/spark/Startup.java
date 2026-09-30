package dev.rdh.pulse.spark;

import dev.rdh.pulse.PulseMod;
import dev.rdh.pulse.spark.client.FabricClientSparkPlugin;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.sampler.Sampler;
import me.lucko.spark.common.sampler.SamplerBuilder;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.java.MergeStrategy;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Startup {
	private static final boolean ENABLED = Boolean.getBoolean("pulse.profileStartup");

	private static final List<String> phases = new ArrayList<>();
	private static final List<Long> uptimes = new ArrayList<>();
	private static boolean reported;

	private static SparkPlatform platform;
	private static Sampler sampler;

	private Startup() {}

	public static void mark(String phase) {
		if (ENABLED && !reported) {
			phases.add(phase);
			uptimes.add(ManagementFactory.getRuntimeMXBean().getUptime());
		}
	}

	public static void profile() {
		if (!ENABLED || sampler != null) {
			return;
		}
		SparkPlatform spark = FabricClientSparkPlugin.platform();
		if (spark == null) {
			return;
		}
		try {
			platform = spark;
			sampler = new SamplerBuilder()
					.threadDumper(ThreadDumper.ALL)
					.threadGrouper(ThreadGrouper.BY_POOL)
					.samplingInterval(2)
					.start(spark);
			PulseMod.LOG.info("Profiling startup");
		} catch (Throwable t) {
			sampler = null;
			PulseMod.LOG.error("Could not profile startup", t);
		}
	}

	public static void report() {
		if (!ENABLED || reported) {
			return;
		}
		mark("first frame");
		reported = true;

		StringBuilder breakdown = new StringBuilder();
		long previous = 0;
		for (int i = 0; i < phases.size(); i++) {
			if (i > 0) {
				breakdown.append(", ");
			}
			breakdown.append(phases.get(i)).append(' ').append(seconds(uptimes.get(i) - previous));
			previous = uptimes.get(i);
		}
		PulseMod.LOG.info("Startup took {}: {}", seconds(previous), breakdown);

		Sampler stopping = sampler;
		if (stopping == null) {
			return;
		}
		sampler = null;
		try {
			stopping.stop(false);
			platform.getPlugin().executeAsync(() -> save(stopping));
		} catch (Throwable t) {
			PulseMod.LOG.error("Could not stop the startup profiler", t);
		}
	}

	private static void save(Sampler stopped) {
		try {
			SamplerData data = stopped.toProto(platform, new Sampler.ExportProps()
					.creator(new CommandSender.Data("pulse", null))
					.comment("startup")
					.mergeStrategy(MergeStrategy.SAME_METHOD)
					.classSourceLookup(platform::createClassSourceLookup));
			Path file = platform.resolveSaveFile("startup", "sparkprofile");
			Files.write(file, data.toByteArray());
			PulseMod.LOG.info("Startup profile saved to {}", file);
		} catch (Throwable t) {
			PulseMod.LOG.error("Could not save the startup profile", t);
		}
	}

	private static String seconds(long millis) {
		return "%.2fs".formatted(millis / 1000.0);
	}
}
