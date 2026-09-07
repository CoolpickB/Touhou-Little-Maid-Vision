package com.coolpick.tlmvision.client;
import com.coolpick.tlmvision.TlmVisionHelper;
import com.coolpick.tlmvision.VisionConfig;
import com.coolpick.tlmvision.compat.VisionMaidCompat;
import net.minecraft.network.chat.Component;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import java.util.List;
@EventBusSubscriber(modid = TlmVisionHelper.MOD_ID, value = Dist.CLIENT)
public final class VisionKeys {
    private static KeyMapping snapKey;
    private static KeyMapping borrowKey;
    public static void init(IEventBus bus) { bus.addListener(VisionKeys::registerKeys); }
    private static void registerKeys(RegisterKeyMappingsEvent event) {
        snapKey = new KeyMapping("key.tlmvision.snap", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, "key.categories.tlmvision");
        event.register(snapKey);
        borrowKey = new KeyMapping("key.tlmvision.borrow", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, "key.categories.tlmvision");
        event.register(borrowKey);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!ModList.get().isLoaded("touhou_little_maid")) return;
        Minecraft mc = Minecraft.getInstance();
        MaidCamera.tick(mc);
        if (borrowKey != null && borrowKey.isDown() && mc.player != null && mc.level != null && mc.screen == null) {
            MaidCamera.borrow(mc, eligible(mc));
        } else {
            MaidCamera.stopBorrowing(mc);
        }
        if (snapKey == null) return;
        while (snapKey.consumeClick()) {
            if (mc.player == null || mc.level == null || mc.screen != null || MaidCamera.busy()) continue;
            // Only loaded entities are on the client, so this box is the practical edge of the world.
            if (MaidCamera.borrowedMaid() != null) {
                MaidCamera.snapshot(mc, List.of(MaidCamera.borrowedMaid()), MaidCamera.borrowedMaid());
                continue;
            }
            List<EntityMaid> wearers = eligible(mc);
            if (wearers.isEmpty()) continue;
            wearers.removeIf(MaidCamera::thinking);
            if (wearers.isEmpty()) {
                mc.player.displayClientMessage(Component.literal(
                        "Your maids are already thinking. Let them finish before sharing another view."), false);
                continue;
            }
            MaidCamera.snapshot(mc, MaidCamera.group(mc, wearers, VisionConfig.load()));
        }
    }
    @SubscribeEvent public static void beforeTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (MaidCamera.isBorrowing()) MaidCamera.suppressPlayerMovement(mc);
    }
    @SubscribeEvent public static void hidePlayerHands(RenderHandEvent event) {
        if (MaidCamera.isBorrowing()) event.setCanceled(true);
    }
    /** Prevent attack, use/place and pick-block packets while the camera belongs to a maid. */
    @SubscribeEvent public static void cancelInteractions(InputEvent.InteractionKeyMappingTriggered event) {
        if (MaidCamera.isBorrowing()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || !MaidCamera.isBorrowing()) return;
        if (event.getScrollDeltaY() == 0) return;
        MaidCamera.cycleBorrowed(mc, eligible(mc), event.getScrollDeltaY() > 0 ? -1 : 1);
        event.setCanceled(true);
    }
    /** Client worlds contain only loaded entities; do not sweep a large synthetic AABB. */
    private static List<EntityMaid> eligible(Minecraft mc) {
        List<EntityMaid> wearers = new java.util.ArrayList<>();
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof EntityMaid maid && maid.isAlive() && maid.isOwnedBy(mc.player)
                    && VisionMaidCompat.wearsEye(maid)) wearers.add(maid);
        }
        return wearers;
    }
}
