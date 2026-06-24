package com.deadmind.dndmods.network;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectClassPayload(int classOrdinal) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SelectClassPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("select_class"));

    public static final StreamCodec<FriendlyByteBuf, SelectClassPayload> STREAM_CODEC =
            StreamCodec.of(SelectClassPayload::write, SelectClassPayload::read);

    private static SelectClassPayload read(FriendlyByteBuf buf) {
        return new SelectClassPayload(buf.readVarInt());
    }

    private static void write(FriendlyByteBuf buf, SelectClassPayload payload) {
        buf.writeVarInt(payload.classOrdinal);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                DnDPlayerData data = PlayerDataHelper.get(serverPlayer);
                if (data.getDnDClass() != DnDClass.NONE) return;

                DnDClass[] classes = DnDClass.values();
                if (classOrdinal >= 0 && classOrdinal < classes.length && classes[classOrdinal] != DnDClass.NONE) {
                    data.setDnDClass(classes[classOrdinal]);
                    PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
