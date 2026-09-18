package dev.rdh.pulse.spark.mixin;

import dev.rdh.pulse.spark.client.FabricClientSparkPlugin;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
abstract class ClientChatCompletionMixin {
    @Shadow protected TextFieldWidget chatField;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void spark$completeClientCommand(char character, int keyCode, CallbackInfo ci) {
        if (keyCode != Keyboard.KEY_TAB) {
            return;
        }
        String message = this.chatField.getText();
        String completed = FabricClientSparkPlugin.completeClientCommand(message);
        if (!completed.equals(message)) {
            this.chatField.setText(completed);
            ci.cancel();
        }
    }
}
