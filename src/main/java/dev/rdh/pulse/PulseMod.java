package dev.rdh.pulse;

import dev.rdh.pulse.spark.client.FabricClientSparkPlugin;
import dev.rdh.pulse.spark.server.FabricServerSparkPlugin;
import dev.rdh.pulse.tracy.Tracy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.discovery.ModResolutionException;
import net.ornithemc.osl.lifecycle.api.server.MinecraftServerEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PulseMod implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Pulse");
    public static final Logger LOG_SPARK = LoggerFactory.getLogger("Pulse/Spark");

    @Override
    public void onInitialize() {
        Tracy.init();
        MinecraftServerEvents.READY_WORLD.register(FabricServerSparkPlugin::initialize);
        MinecraftServerEvents.STOP.register(FabricServerSparkPlugin::stop);
    }

    public static void initializeClient() throws ModResolutionException {
        FabricLoader l = FabricLoader.getInstance();
        if (!l.isModLoaded("lenis") && !l.isModLoaded("legacy-lwjgl3")) {
            throw new ModResolutionException("Pulse requires lenis or legacy-lwjgl3 to be installed on the client!");
        }
        FabricClientSparkPlugin.initialize();
    }
}
