package dev.rdh.pulse.spark.server;

import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricPlayerPingProvider implements PlayerPingProvider {
    private final MinecraftServer server;

    public FabricPlayerPingProvider(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public Map<String, Integer> poll() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (ServerPlayerEntity player : this.server.getPlayerManager().getAll()) {
            result.put(player.getGameProfile().getName(), player.ping);
        }
        return result;
    }
}
