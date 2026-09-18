package dev.rdh.pulse.spark;

import me.lucko.spark.common.tick.SimpleTickReporter;

public abstract class AbstractFabricTickReporter extends SimpleTickReporter {
	private boolean closed;

	protected abstract void onTickStart(Runnable task);

	protected abstract void onTickEnd(Runnable task);

	@Override
	public void start() {
		onTickStart(() -> {
			if (!this.closed) onStart();
		});
		onTickEnd(() -> {
			if (!this.closed) onEnd();
		});
	}

	@Override
	public void close() {
		this.closed = true;
		super.close();
	}
}
