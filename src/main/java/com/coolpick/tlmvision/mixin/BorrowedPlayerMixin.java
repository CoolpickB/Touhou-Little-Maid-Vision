package com.coolpick.tlmvision.mixin;

import com.coolpick.tlmvision.client.MaidCamera;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LocalPlayer.class)
public abstract class BorrowedPlayerMixin {
    @Shadow protected abstract boolean isControlledCamera();

    /** Keep normal position/ground updates working while the player falls or is pushed. */
    @Redirect(method = "sendPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isControlledCamera()Z"))
    private boolean tlmvision$sendPlayerPosition(LocalPlayer player) {
        return MaidCamera.isBorrowing() || isControlledCamera();
    }
}
