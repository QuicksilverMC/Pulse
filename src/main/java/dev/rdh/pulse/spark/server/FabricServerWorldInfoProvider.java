package dev.rdh.pulse.spark.server;

import dev.rdh.pulse.spark.AbstractFabricWorldInfoProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Collection;

public final class FabricServerWorldInfoProvider extends AbstractFabricWorldInfoProvider {
	private final MinecraftServer server;

	public FabricServerWorldInfoProvider(MinecraftServer server) {
		this.server = server;
	}

	@Override
	protected Collection<? extends World> worlds() {
		return Arrays.asList(this.server.worlds);
	}
}
