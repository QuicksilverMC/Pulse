package dev.rdh.pulse.render.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rdh.pulse.render.DrawCalls;
import dev.rdh.pulse.render.DrawCalls.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GameGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@Inject(method = "render(FJ)V", at = @At("HEAD"))
	private void pulse$beginFrame(float tickDelta, long startTime, CallbackInfo ci) {
		DrawCalls.frame(Minecraft.getInstance().options.debugEnabled);
	}

	@WrapOperation(method = "render(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GameGui;render(F)V"))
	private void pulse$hud(GameGui gui, float tickDelta, Operation<Void> original) {
		Category previous = DrawCalls.enter(Category.GUI);
		try {
			original.call(gui, tickDelta);
		} finally {
			DrawCalls.exit(previous);
		}
	}

	@WrapOperation(method = "render(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(IIF)V"))
	private void pulse$screen(Screen screen, int mouseX, int mouseY, float tickDelta, Operation<Void> original) {
		Category previous = DrawCalls.enter(Category.GUI);
		try {
			original.call(screen, mouseX, mouseY, tickDelta);
		} finally {
			DrawCalls.exit(previous);
		}
	}
}
