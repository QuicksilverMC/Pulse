package dev.rdh.pulse.tracy.mixin;

import dev.rdh.pulse.tracy.Tracy;
import dev.rdh.pulse.tracy.TracyGpu;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Profiler.class)
abstract class ProfilerMixin {
	@Inject(method = "push", at = @At("HEAD"))
	private void pulse$zoneBegin(String name, CallbackInfo ci) {
		Tracy.zoneBegin(name);
		TracyGpu.zoneBegin(name);
	}

	@Inject(method = "pop", at = @At("HEAD"))
	private void pulse$zoneEnd(CallbackInfo ci) {
		TracyGpu.zoneEnd();
		Tracy.zoneEnd();
	}
}
