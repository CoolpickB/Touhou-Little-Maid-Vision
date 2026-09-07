package com.coolpick.tlmvision.mixin;

import com.coolpick.tlmvision.client.MaidCamera;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class BorrowedMouseMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void tlmvision$turnCamera(LocalPlayer player, double yaw, double pitch) {
        if (MaidCamera.isBorrowing()) MaidCamera.turnBorrowedView(yaw, pitch);
        else player.turn(yaw, pitch);
    }
}
