package com.coolpick.tlmvision.mixin;

import com.coolpick.tlmvision.client.MaidCamera;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.ChatClientInfo;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.STTCallback;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Speech-to-text reaches TLM through the same chat packet, but without its chat screen. */
@Pseudo
@Mixin(value = STTCallback.class, remap = false)
public abstract class QueuedVisionVoiceMixin {
    @Shadow @Final private Player player;
    @Shadow @Final private EntityMaid maid;

    @Inject(method = "onSuccess(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void tlmvision$queueUntilSightArrives(String message, CallbackInfo ci) {
        if (message == null || message.isBlank() || !MaidCamera.queueChat(maid, message, ChatClientInfo.fromMaid(maid))) return;
        player.sendSystemMessage(Component.translatable("tlmvision.chat.queued", player.getScoreboardName(), message)
                .withStyle(ChatFormatting.GRAY));
        ci.cancel();
    }
}
