package com.coolpick.tlmvision.mixin;
import com.coolpick.tlmvision.client.ChatTokenLine;
import com.coolpick.tlmvision.VisionConfig;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.AIChatScreen;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces TLM's lifetime-only token figure. The drawing lives in ChatTokenLine. */
@Pseudo
@Mixin(value = AIChatScreen.class, remap = false)
public abstract class ChatTokenMixin extends Screen {
    @Shadow @Final private EntityMaid maid;
    @Shadow private EditBox input;
    @Shadow private int currentTokens;
    @Shadow private int maxTokens;
    private ChatTokenMixin() { super(null); }

    @Inject(method = "init", at = @At("HEAD"))
    private void tlmvision$loadChatSettings(CallbackInfo callback) {
        VisionConfig.refreshChatImprovements();
    }

    @Inject(method = "renderTokenUsage", at = @At("HEAD"), cancellable = true)
    private void contextAndSpend(GuiGraphics graphics, CallbackInfo callback) {
        if (!ChatTokenLine.enabled()) return;
        ChatTokenLine.render(graphics, this.font, this.input, this.maid, this.currentTokens, this.maxTokens);
        callback.cancel();
    }
}
