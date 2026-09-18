package dev.rdh.pulse.spark.client;

import dev.rdh.pulse.spark.AbstractFabricWorldInfoProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.List;

public final class FabricClientWorldInfoProvider extends AbstractFabricWorldInfoProvider {
	@Override
	protected Collection<? extends World> worlds() {
		ClientWorld world = Minecraft.getInstance().world;
		return world == null ? List.of() : List.of(world);
	}
}
