package com.coolpick.tlmvision.network;

import com.coolpick.tlmvision.compat.VisionMaidCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server validation and brain-level look ownership for the borrowed camera. */
public final class MaidLookController {
    private static final Map<UUID, HeldLook> HELD_LOOKS = new HashMap<>();
    private record HeldLook(EntityMaid maid, float yaw, float pitch, long expiresAt) { }
    private MaidLookController() { }

    static void apply(Player owner, MaidLookPayload request) {
        Entity entity = owner.level().getEntity(request.maidId());
        if (!(entity instanceof EntityMaid maid) || !maid.isAlive() || !maid.isOwnedBy(owner)
                || !VisionMaidCompat.wearsEye(maid)) return;
        float yaw = request.yaw();
        float pitch = Math.clamp(request.pitch(), -90, 90);
        HELD_LOOKS.put(maid.getUUID(), new HeldLook(maid, yaw, pitch, maid.level().getGameTime() + 10));
        aim(maid, yaw, pitch);
    }
    /** Run after entity AI each server tick so movement/pathing cannot reclaim the camera yaw. */
    public static void tick(ServerTickEvent.Post event) {
        Iterator<HeldLook> looks = HELD_LOOKS.values().iterator();
        while (looks.hasNext()) {
            HeldLook held = looks.next();
            if (!held.maid().isAlive() || held.maid().isRemoved() || held.maid().level().getGameTime() > held.expiresAt()) {
                looks.remove();
            } else {
                aim(held.maid(), held.yaw(), held.pitch());
            }
        }
    }
    private static void aim(EntityMaid maid, float yaw, float pitch) {
        Vec3 aim = Vec3.directionFromRotation(pitch, yaw);
        BlockPos target = BlockPos.containing(maid.getEyePosition().add(aim.scale(32)));
        // LookAtTargetSink is TLM's own brain channel. Refreshing this brief target makes the
        // brain turn toward the borrowed view instead of competing through LookControl.
        maid.getBrain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target), 5);
        // The memory refresh has no render/network cost. Only correct physical rotation when
        // movement or another task actually displaced it.
        if (Math.abs(Mth.wrapDegrees(maid.getYRot() - yaw)) > 0.01F) maid.setYRot(yaw);
        if (Math.abs(Mth.wrapDegrees(maid.getYHeadRot() - yaw)) > 0.01F) maid.setYHeadRot(yaw);
        if (Math.abs(maid.getXRot() - pitch) > 0.01F) maid.setXRot(pitch);
    }
}
