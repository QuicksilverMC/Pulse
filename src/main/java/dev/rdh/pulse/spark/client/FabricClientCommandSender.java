package dev.rdh.pulse.spark.client;

import dev.rdh.pulse.spark.Texts;
import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;

import net.minecraft.client.Minecraft;

import java.util.UUID;

public final class FabricClientCommandSender extends AbstractCommandSender<UUID> {
	public FabricClientCommandSender(UUID id) {
		super(id);
	}

	@Override
	public String getName() {
		return Minecraft.getInstance().getSession().getUsername();
	}

	@Override
	public UUID getUniqueId() {
		return this.delegate;
	}

	@Override
	public void sendMessage(Component message) {
		Minecraft.getInstance().gui.getChat().addMessage(Texts.of(message));
	}

	@Override
	public boolean hasPermission(String permission) {
		return true;
	}

	@Override
	protected Object getObjectForComparison() {
		return getName();
	}
}
