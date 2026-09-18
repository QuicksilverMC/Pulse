package dev.rdh.pulse.spark.server;

import dev.rdh.pulse.spark.AbstractFabricTickReporter;
import net.ornithemc.osl.lifecycle.api.server.MinecraftServerEvents;

public final class FabricServerTickReporter extends AbstractFabricTickReporter {
	@Override
	protected void onTickStart(Runnable task) {
		MinecraftServerEvents.TICK_START.register(server -> task.run());
	}

	@Override
	protected void onTickEnd(Runnable task) {
		MinecraftServerEvents.TICK_END.register(server -> task.run());
	}
}
