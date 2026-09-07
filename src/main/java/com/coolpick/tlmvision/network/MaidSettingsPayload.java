package com.coolpick.tlmvision.network;

import com.coolpick.tlmvision.TlmVisionHelper;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatSerializable;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Send saved chat selection/settings to the owner when the maid enters client tracking. */
public record MaidSettingsPayload(int maidId, CompoundTag settings) implements CustomPacketPayload {
    public static final Type<MaidSettingsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TlmVisionHelper.MOD_ID, "maid_settings"));
    public static final StreamCodec<ByteBuf, MaidSettingsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MaidSettingsPayload::maidId,
            ByteBufCodecs.COMPOUND_TAG, MaidSettingsPayload::settings, MaidSettingsPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) ->
                context.enqueueWork(() -> com.coolpick.tlmvision.client.MaidSettingsSync.receive(payload)));
    }

    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getTarget() instanceof EntityMaid maid && maid.isOwnedBy(player)) {
            MaidAIChatSerializable data = new MaidAIChatSerializable();
            data.copyFrom(maid.getAiChatManager());
            PacketDistributor.sendToPlayer(player, new MaidSettingsPayload(
                    maid.getId(), data.writeToTag(new CompoundTag())));
        }
    }
}
