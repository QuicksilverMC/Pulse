package dev.rdh.pulse.render.mixin;

import dev.rdh.pulse.render.DrawCalls;

import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL40C;
import org.lwjgl.opengl.GL43C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {GL14C.class, GL32C.class, GL40C.class, GL43C.class}, remap = false)
abstract class GLMixin {
	@Inject(method = {
			// GL14
			"glMultiDrawArrays(I[I[I)V", "glMultiDrawElements(I[IILorg/lwjgl/PointerBuffer;)V",
			// GL32
			"glMultiDrawElementsBaseVertex(I[IILorg/lwjgl/PointerBuffer;[I)V",
			// GL40
			"glDrawArraysIndirect(I[I)V", "glDrawElementsIndirect(II[I)V",
			// GL43
			"glMultiDrawArraysIndirect(I[III)V", "glMultiDrawElementsIndirect(II[III)V"
	}, at = @At("HEAD"))
	private static void pulse$count(CallbackInfo ci) {
		DrawCalls.count();
	}
}
