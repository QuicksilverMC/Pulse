package dev.rdh.pulse.spark.server;

import me.lucko.spark.common.platform.serverconfig.PropertiesConfigParser;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FabricServerConfigProvider extends ServerConfigProvider {
    public FabricServerConfigProvider() {
        super(Map.of("server.properties", PropertiesConfigParser.INSTANCE), hiddenPaths());
    }

    private static Set<String> hiddenPaths() {
        Set<String> paths = new HashSet<>(BASE_HIDDEN_PATHS);
        paths.addAll(getSystemPropertyList("spark.serverconfigs.hiddenpaths"));
        return paths;
    }
}
