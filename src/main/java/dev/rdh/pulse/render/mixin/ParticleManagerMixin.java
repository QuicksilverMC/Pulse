package dev.rdh.pulse.render.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.rdh.pulse.render.DrawCalls;
import dev.rdh.pulse.render.DrawCalls.Category;
import net.minecraft.client.ParticleManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ParticleManager.class)
abstract class ParticleManagerMixin {
	@WrapMethod(method = {"render", "renderLit"})
	private void pulse$particles(Entity camera, float tickDelta, Operation<Void> original) {
		Category previous = DrawCalls.enter(Category.PARTICLES);
		try {
			original.call(camera, tickDelta);
		} finally {
			DrawCalls.exit(previous);
		}
	}
}
