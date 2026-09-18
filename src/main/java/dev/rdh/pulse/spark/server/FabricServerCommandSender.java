package dev.rdh.pulse.spark.server;

import dev.rdh.pulse.spark.Texts;
import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.kyori.adventure.text.Component;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;

import java.util.UUID;

public final class FabricServerCommandSender extends AbstractCommandSender<CommandSource> {
	public FabricServerCommandSender(CommandSource source) {
		super(source);
	}

	@Override
	public String getName() {
		return this.delegate.getName();
	}

	@Override
	public UUID getUniqueId() {
		return this.delegate instanceof ServerPlayerEntity player ? player.getGameProfile().getId() : null;
	}

	@Override
	public void sendMessage(Component message) {
		this.delegate.sendMessage(Texts.of(message));
	}

	@Override
	public boolean hasPermission(String permission) {
		return isHost(this.delegate) || this.delegate.canUseCommand(4, "spark");
	}

	public static boolean isHost(CommandSource source) {
		MinecraftServer server = MinecraftServer.getInstance();
		return server != null && server.isSingleplayer() && source instanceof ServerPlayerEntity
				&& source.getName().equals(server.getUsername());
	}

	@Override
	protected Object getObjectForComparison() {
		return getUniqueId() == null ? this.delegate : getUniqueId();
	}
}
