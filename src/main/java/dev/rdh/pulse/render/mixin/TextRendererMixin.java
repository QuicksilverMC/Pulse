package dev.rdh.pulse.render.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.rdh.pulse.render.DrawCalls;
import dev.rdh.pulse.render.DrawCalls.Category;
import net.minecraft.client.render.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TextRenderer.class)
abstract class TextRendererMixin {
	@WrapMethod(method = "drawLayer(Ljava/lang/String;FFIZ)I")
	private int pulse$text(String text, float x, float y, int color, boolean shadow, Operation<Integer> original) {
		Category previous = DrawCalls.enter(Category.TEXT);
		try {
			return original.call(text, x, y, color, shadow);
		} finally {
			DrawCalls.exit(previous);
		}
	}
}
