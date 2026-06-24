package com.deadmind.dndmods.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenAbilityBarPayload(boolean open) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenAbilityBarPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("open_ability_bar"));

    public static final StreamCodec<FriendlyByteBuf, OpenAbilityBarPayload> STREAM_CODEC =
            StreamCodec.of(OpenAbilityBarPayload::write, OpenAbilityBarPayload::read);

    private static OpenAbilityBarPayload read(FriendlyByteBuf buf) {
        return new OpenAbilityBarPayload(buf.readBoolean());
    }

    private static void write(FriendlyByteBuf buf, OpenAbilityBarPayload payload) {
        buf.writeBoolean(payload.open);
    }

    public void handle(IPayloadContext context) {
        // Server acknowledgment — no-op for now, state is client-driven
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
