package com.coolpick.tlmvision.mixin;

import com.coolpick.tlmvision.client.MaidCamera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nausea/confusion is driven by LocalPlayer even when another entity supplies the camera. */
@Mixin(GameRenderer.class)
public abstract class BorrowedSightRendererMixin {
    @Inject(method = "renderConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void tlmvision$hidePlayerConfusion(CallbackInfo callback) {
        if (MaidCamera.isBorrowing()) callback.cancel();
    }
}
