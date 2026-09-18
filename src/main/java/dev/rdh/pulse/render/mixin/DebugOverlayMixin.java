package dev.rdh.pulse.render.mixin;

import dev.rdh.pulse.render.DrawCalls;
import net.minecraft.client.gui.overlay.DebugOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugOverlay.class)
abstract class DebugOverlayMixin {
	@Inject(method = "getGameInfo", at = @At("RETURN"))
	private void pulse$addDrawCalls(CallbackInfoReturnable<List<String>> cir) {
		if (DrawCalls.active()) {
			cir.getReturnValue().addAll(DrawCalls.lines());
		}
	}
}
