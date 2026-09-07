package com.coolpick.tlmvision.mixin;

import com.coolpick.tlmvision.client.VisionScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings.AIChatSettingsHubScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.ai.SideButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs on every internal hub rebuild too, unlike ScreenEvent.Init. */
@org.spongepowered.asm.mixin.Pseudo
@Mixin(value = AIChatSettingsHubScreen.class, remap = false)
public abstract class SettingsHubMixin extends Screen {
    @Shadow protected int startX;
    @Shadow @Final protected Screen parent;
    @Shadow @Final protected AIChatSettingsHubScreen.SharedState state;
    @Shadow @Final protected boolean insufficientPermissions;
    protected SettingsHubMixin(Component title) { super(title); }
    @Inject(method = "addSiteSideButtons", at = @At("RETURN"), cancellable = true)
    private void addVision(int y, CallbackInfoReturnable<Integer> result) {
        int row = result.getReturnValue() + 20;
        addRenderableWidget(new SideButton(null, startX, row, Component.translatable("tlmvision.hub.vision"),
                button -> minecraft.setScreen(new VisionScreen(parent, state, insufficientPermissions))));
        result.setReturnValue(row);
    }
}
