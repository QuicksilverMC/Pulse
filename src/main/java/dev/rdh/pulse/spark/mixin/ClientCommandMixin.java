package dev.rdh.pulse.spark.mixin;

import dev.rdh.pulse.spark.client.FabricClientSparkPlugin;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalClientPlayerEntity.class)
abstract class ClientCommandMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void spark$handleClientCommand(String message, CallbackInfo ci) {
        if (FabricClientSparkPlugin.executeClientCommand(message)) {
            ci.cancel();
        }
    }
}
