package dev.rdh.pulse.spark.mixin;

import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor("thread")
    Thread spark$getThread();

    @Accessor("LOGGER")
    static Logger spark$getLogger() {
        throw new AssertionError();
    }
}
