package com.coolpick.tlmvision.mixin;

import com.coolpick.tlmvision.client.MaidCamera;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.ChatClientInfo;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.AIChatScreen;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep a follow-up behind its matching vision observation in TLM's chat history. */
@Pseudo
@Mixin(value = AIChatScreen.class, remap = false)
public abstract class QueuedVisionChatMixin {
    @Shadow @Final private EntityMaid maid;
    @Shadow @Final private EditBox input;

    @Inject(method = "sendDoneMessage", at = @At("HEAD"), cancellable = true)
    private void tlmvision$queueUntilSightArrives(CallbackInfo ci) {
        String message = input.getValue();
        if (message.isBlank() || !MaidCamera.queueChat(maid, message, ChatClientInfo.fromMaid(maid))) return;
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.translatable("tlmvision.chat.queued", player.getScoreboardName(), message)
                    .withStyle(ChatFormatting.GRAY));
        }
        ((Screen) (Object) this).onClose();
        ci.cancel();
    }
}
