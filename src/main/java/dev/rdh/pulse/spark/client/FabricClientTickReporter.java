package dev.rdh.pulse.spark.client;

import dev.rdh.pulse.spark.AbstractFabricTickReporter;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;

public final class FabricClientTickReporter extends AbstractFabricTickReporter {
	@Override
	protected void onTickStart(Runnable task) {
		MinecraftClientEvents.TICK_START.register(client -> task.run());
	}

	@Override
	protected void onTickEnd(Runnable task) {
		MinecraftClientEvents.TICK_END.register(client -> task.run());
	}
}
