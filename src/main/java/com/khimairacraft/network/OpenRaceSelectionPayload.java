package com.khimairacraft.network;

import com.khimairacraft.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenRaceSelectionPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenRaceSelectionPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("open_race_selection"));

    public static final StreamCodec<FriendlyByteBuf, OpenRaceSelectionPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {},
                    buf -> new OpenRaceSelectionPayload()
            );

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandler.openRaceSelection());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
