package com.coolpick.tlmvision.network;

import com.coolpick.tlmvision.TlmVisionHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** A player's requested head direction while borrowing one of their maid's views. */
public record MaidLookPayload(int maidId, float yaw, float pitch) implements CustomPacketPayload {
    public static final Type<MaidLookPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TlmVisionHelper.MOD_ID, "maid_look"));
    public static final StreamCodec<ByteBuf, MaidLookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MaidLookPayload::maidId,
            ByteBufCodecs.FLOAT, MaidLookPayload::yaw,
            ByteBufCodecs.FLOAT, MaidLookPayload::pitch,
            MaidLookPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> MaidLookController.apply(context.player(), payload)));
    }
}
