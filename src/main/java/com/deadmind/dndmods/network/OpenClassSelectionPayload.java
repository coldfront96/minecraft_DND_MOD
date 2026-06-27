package com.deadmind.dndmods.network;

import com.deadmind.dndmods.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenClassSelectionPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenClassSelectionPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("open_class_selection"));

    public static final StreamCodec<FriendlyByteBuf, OpenClassSelectionPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {},
                    buf -> new OpenClassSelectionPayload()
            );

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandler.openClassSelection());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
