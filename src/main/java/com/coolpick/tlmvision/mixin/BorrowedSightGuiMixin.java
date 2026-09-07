package com.coolpick.tlmvision.mixin;

import com.coolpick.tlmvision.client.MaidCamera;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The pumpkin, portal, spyglass, and related overlays belong to LocalPlayer, not the maid view. */
@Mixin(Gui.class)
public abstract class BorrowedSightGuiMixin {
    @Inject(method = "renderCameraOverlays", at = @At("HEAD"), cancellable = true)
    private void tlmvision$hidePlayerOverlays(CallbackInfo callback) {
        if (MaidCamera.isBorrowing()) callback.cancel();
    }
}
