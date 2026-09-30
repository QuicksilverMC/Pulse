package dev.rdh.pulse.spark.mixin;

import dev.rdh.pulse.spark.Startup;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@Inject(method = "init", at = @At("RETURN"))
	private void pulse$markGameInit(CallbackInfo ci) {
		Startup.mark("game init");
	}
}
