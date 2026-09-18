package dev.rdh.pulse.render.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.rdh.pulse.render.DrawCalls;
import dev.rdh.pulse.render.DrawCalls.Category;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockEntityRenderDispatcher.class)
abstract class BlockEntityRenderDispatcherMixin {
	@WrapMethod(method = "render(Lnet/minecraft/block/entity/BlockEntity;FI)V")
	private void pulse$tiles(BlockEntity blockEntity, float tickDelta, int destroyProgress, Operation<Void> original) {
		Category previous = DrawCalls.enter(Category.TILES);
		try {
			original.call(blockEntity, tickDelta, destroyProgress);
		} finally {
			DrawCalls.exit(previous);
		}
	}
}
