package dev.rdh.pulse.spark;

import me.lucko.spark.common.tick.AbstractTickHook;

public abstract class AbstractFabricTickHook extends AbstractTickHook {
	private boolean closed;

	protected abstract void onTickStart(Runnable task);

	@Override
	public void start() {
		onTickStart(() -> {
			if (!this.closed) onTick();
		});
	}

	@Override
	public void close() {
		this.closed = true;
	}
}
