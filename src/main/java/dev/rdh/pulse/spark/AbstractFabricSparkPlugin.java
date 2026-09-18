package dev.rdh.pulse.spark;

import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.ThreadDumper.GameThread;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import net.ornithemc.osl.executors.api.BackgroundExecutor;

import dev.rdh.pulse.PulseMod;

import java.nio.file.Path;
import java.util.Collection;
import java.util.logging.Level;
import java.util.stream.Collectors;

public abstract class AbstractFabricSparkPlugin implements SparkPlugin {

	protected final GameThread gameThreadDumper;
	private final PlatformInfo.Type type;

	protected AbstractFabricSparkPlugin(GameThread gameThreadDumper, PlatformInfo.Type type) {
		this.gameThreadDumper = gameThreadDumper;
		this.type = type;
	}

	@Override
	public void log(Level level, String message) {
		var l = slf4jLevel(level);
		if (l != null) {
			PulseMod.LOG_SPARK.atLevel(l).log(message);
		}
	}

	@Override
	public void log(Level level, String message, Throwable throwable) {
		var l = slf4jLevel(level);
		if (l != null) {
			PulseMod.LOG_SPARK.atLevel(l).setCause(throwable).log(message);
		}
	}

	@Override
	public String getVersion() {
		return FabricLoader.getInstance().getModContainer("pulse").get().getMetadata().getVersion().getFriendlyString();
	}

	@Override
	public Path getPluginDirectory() {
		return FabricLoader.getInstance().getConfigDir().resolve("pulse");
	}

	@Override
	public void executeAsync(Runnable task) {
		BackgroundExecutor.get().execute(task);
	}

	@Override
	public ClassSourceLookup createClassSourceLookup() {
		return new FabricClassSourceLookup();
	}

	@Override
	public PlatformInfo getPlatformInfo() {
		return new FabricPlatformInfo(this.type);
	}

	@Override
	public ThreadDumper getDefaultThreadDumper() {
		return this.gameThreadDumper.get();
	}

	@Override
	public Collection<SourceMetadata> getKnownSources() {
		return SourceMetadata.gather(
				FabricLoader.getInstance().getAllMods(),
				mod -> mod.getMetadata().getId(),
				mod -> mod.getMetadata().getVersion().getFriendlyString(),
				mod -> mod.getMetadata().getAuthors().stream().map(Person::getName).collect(Collectors.joining(", ")),
				mod -> mod.getMetadata().getDescription(),
				mod -> mod.getMetadata().getId().equals("minecraft") || mod.getMetadata().getId().equals("java")
		);
	}

	private static org.slf4j.event.Level slf4jLevel(Level l) {
		if (l == Level.OFF) {
			return null;
		} else if (l == Level.SEVERE) {
			return org.slf4j.event.Level.ERROR;
		} else if (l == Level.WARNING) {
			return org.slf4j.event.Level.WARN;
		} else if (l == Level.INFO) {
			return org.slf4j.event.Level.INFO;
		} else if (l == Level.CONFIG) {
			return org.slf4j.event.Level.INFO;
		} else if (l == Level.FINE) {
			return org.slf4j.event.Level.DEBUG;
		} else if (l == Level.FINER) {
			return org.slf4j.event.Level.TRACE;
		} else if (l == Level.FINEST) {
			return org.slf4j.event.Level.TRACE;
		} else if (l == Level.ALL) {
			return org.slf4j.event.Level.TRACE;
		}
		return org.slf4j.event.Level.INFO;
	}
}
