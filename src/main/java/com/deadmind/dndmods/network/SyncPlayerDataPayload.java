package com.deadmind.dndmods.network;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.client.ClassSelectionScreen;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.minecraft.client.Minecraft;
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
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                DnDPlayerData playerData = mc.player.getData(ModAttachments.PLAYER_DATA);
                playerData.load(data);

                if (playerData.getDnDClass() == DnDClass.NONE && !(mc.screen instanceof ClassSelectionScreen)) {
                    mc.setScreen(new ClassSelectionScreen());
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
