package com.khimairacraft.network;

import com.khimairacraft.client.ClientPacketHandler;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncPlayerDataPayload(CompoundTag data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPlayerDataPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_player_data"));

    public static final StreamCodec<FriendlyByteBuf, SyncPlayerDataPayload> STREAM_CODEC =
            StreamCodec.of(SyncPlayerDataPayload::write, SyncPlayerDataPayload::read);

    private static SyncPlayerDataPayload read(FriendlyByteBuf buf) {
        return new SyncPlayerDataPayload(buf.readNbt());
    }

    private static void write(FriendlyByteBuf buf, SyncPlayerDataPayload payload) {
        buf.writeNbt(payload.data);
    }

    public static SyncPlayerDataPayload fromPlayer(DnDPlayerData playerData) {
        return new SyncPlayerDataPayload(playerData.save());
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandler.handleSyncPlayerData(data));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
