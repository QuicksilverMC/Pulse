package dev.rdh.pulse.spark.server;

import dev.rdh.pulse.spark.AbstractFabricTickHook;
import net.ornithemc.osl.lifecycle.api.server.MinecraftServerEvents;

public final class FabricServerTickHook extends AbstractFabricTickHook {
	@Override
	protected void onTickStart(Runnable task) {
		MinecraftServerEvents.TICK_START.register(_ -> task.run());
	}
}
