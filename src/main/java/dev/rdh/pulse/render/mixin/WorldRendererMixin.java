package dev.rdh.pulse.render.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.rdh.pulse.render.DrawCalls;
import dev.rdh.pulse.render.DrawCalls.Category;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.block.BlockLayer;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin {
	@WrapMethod(method = "render(Lnet/minecraft/client/render/block/BlockLayer;DILnet/minecraft/entity/Entity;)I")
	private int pulse$terrain(BlockLayer layer, double tickDelta, int pass, Entity camera, Operation<Integer> original) {
		Category previous = DrawCalls.enter(Category.TERRAIN);
		try {
			return original.call(layer, tickDelta, pass, camera);
		} finally {
			DrawCalls.exit(previous);
		}
	}

	@WrapMethod(method = "renderEntities")
	private void pulse$entities(Entity camera, Culler culler, float tickDelta, Operation<Void> original) {
		Category previous = DrawCalls.enter(Category.ENTITIES);
		try {
			original.call(camera, culler, tickDelta);
		} finally {
			DrawCalls.exit(previous);
		}
	}

	@WrapMethod(method = {"renderSky(FI)V", "renderClouds(FI)V"})
	private void pulse$sky(float tickDelta, int pass, Operation<Void> original) {
		Category previous = DrawCalls.enter(Category.SKY);
		try {
			original.call(tickDelta, pass);
		} finally {
			DrawCalls.exit(previous);
		}
	}
}
