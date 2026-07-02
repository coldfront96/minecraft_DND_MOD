package com.khimairacraft.network;

import com.khimairacraft.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AbilityAssignRejectedPayload(int slotIndex, String reason) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AbilityAssignRejectedPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("ability_assign_rejected"));

    public static final StreamCodec<FriendlyByteBuf, AbilityAssignRejectedPayload> STREAM_CODEC =
            StreamCodec.of(AbilityAssignRejectedPayload::write, AbilityAssignRejectedPayload::read);

    private static AbilityAssignRejectedPayload read(FriendlyByteBuf buf) {
        return new AbilityAssignRejectedPayload(buf.readVarInt(), buf.readUtf(256));
    }

    private static void write(FriendlyByteBuf buf, AbilityAssignRejectedPayload payload) {
        buf.writeVarInt(payload.slotIndex);
        buf.writeUtf(payload.reason, 256);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandler.handleAbilityAssignRejected(slotIndex, reason));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
