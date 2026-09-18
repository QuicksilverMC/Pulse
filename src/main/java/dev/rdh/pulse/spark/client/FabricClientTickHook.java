package dev.rdh.pulse.spark.client;

import dev.rdh.pulse.spark.AbstractFabricTickHook;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;

public final class FabricClientTickHook extends AbstractFabricTickHook {
	@Override
	protected void onTickStart(Runnable task) {
		MinecraftClientEvents.TICK_START.register(_ -> task.run());
	}
}
