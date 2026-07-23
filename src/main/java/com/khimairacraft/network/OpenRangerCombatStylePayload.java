package com.khimairacraft.network;

import com.khimairacraft.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> client prompt to pick a Combat Style. Sent when a player reaches
 * Ranger level 2 with no style chosen; opens the blocking selection screen.
 */
public record OpenRangerCombatStylePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenRangerCombatStylePayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("open_ranger_combat_style"));

    public static final StreamCodec<FriendlyByteBuf, OpenRangerCombatStylePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {},
                    buf -> new OpenRangerCombatStylePayload()
            );

    public void handle(IPayloadContext context) {
        context.enqueueWork(ClientPacketHandler::openRangerCombatStyle);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
