package dev.rdh.pulse.render.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rdh.pulse.render.DrawCalls;
import dev.rdh.pulse.render.DrawCalls.Category;
import dev.rdh.pulse.render.FrameStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GameGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@WrapMethod(method = "render(FJ)V")
	private void pulse$frame(float tickDelta, long startTime, Operation<Void> original) {
		boolean debug = Minecraft.getInstance().options.debugEnabled;
		DrawCalls.frame(debug);
		FrameStats.beginFrame(debug);
		try {
			original.call(tickDelta, startTime);
		} finally {
			FrameStats.endFrame();
		}
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
