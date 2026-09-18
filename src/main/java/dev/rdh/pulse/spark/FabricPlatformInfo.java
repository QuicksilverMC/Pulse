package dev.rdh.pulse.spark;

import me.lucko.spark.common.platform.PlatformInfo;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

public final class FabricPlatformInfo implements PlatformInfo {
	private final Type type;

	public FabricPlatformInfo(Type type) {
		this.type = type;
	}

	@Override
	public Type getType() {
		return this.type;
	}

	@Override
	public String getName() {
		return "Ornithe";
	}

	@Override
	public String getBrand() {
		return "Fabric";
	}

	@Override
	public String getVersion() {
		return getModVersion("fabricloader").orElse("unknown");
	}

	@Override
	public String getMinecraftVersion() {
		return getModVersion("minecraft").orElse(null);
	}

	private Optional<String> getModVersion(String mod) {
		return FabricLoader.getInstance().getModContainer(mod).map(container -> container.getMetadata().getVersion().getFriendlyString());
	}
}
