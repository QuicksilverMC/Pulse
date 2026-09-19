package dev.rdh.pulse.render.mixin;

import dev.rdh.pulse.render.DrawCalls;
import dev.rdh.pulse.render.FrameStats;
import net.minecraft.client.gui.overlay.DebugOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugOverlay.class)
abstract class DebugOverlayMixin {
	@Inject(method = "getGameInfo", at = @At("RETURN"))
	private void pulse$addGameInfo(CallbackInfoReturnable<List<String>> cir) {
		List<String> lines = cir.getReturnValue();
		if (FrameStats.active()) {
			int fps = pulse$indexOf(lines, " fps");
			if (fps >= 0) {
				if (FrameStats.gpuSuffix() != null) {
					lines.set(fps, lines.get(fps) + FrameStats.gpuSuffix());
				}
				if (FrameStats.frameLine() != null) {
					lines.add(fps + 1, FrameStats.frameLine());
				}
			}
		}
		if (DrawCalls.active()) {
			lines.addAll(DrawCalls.lines());
		}
	}

	@Inject(method = "getSystemInfo", at = @At("RETURN"))
	private void pulse$addSystemInfo(CallbackInfoReturnable<List<String>> cir) {
		if (!FrameStats.active()) {
			return;
		}
		List<String> lines = cir.getReturnValue();
		pulse$insertAfter(lines, "Mem: ", FrameStats.allocationLine());
		pulse$insertAfter(lines, "CPU: ", FrameStats.cpuLine());
	}

	@Unique
	private static int pulse$indexOf(List<String> lines, String needle) {
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).contains(needle)) {
				return i;
			}
		}
		return -1;
	}

	@Unique
	private static void pulse$insertAfter(List<String> lines, String prefix, String line) {
		if (line == null) {
			return;
		}
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).startsWith(prefix)) {
				lines.add(i + 1, line);
				return;
			}
		}
		lines.add(line);
	}
}
