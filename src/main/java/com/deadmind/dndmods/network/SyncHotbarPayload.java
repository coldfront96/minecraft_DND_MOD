package com.deadmind.dndmods.network;

import com.deadmind.dndmods.player.AbilityHotbar;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncHotbarPayload(String[] slotIds) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncHotbarPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("sync_hotbar"));

    public static final StreamCodec<FriendlyByteBuf, SyncHotbarPayload> STREAM_CODEC =
            StreamCodec.of(SyncHotbarPayload::write, SyncHotbarPayload::read);

    private static SyncHotbarPayload read(FriendlyByteBuf buf) {
        String[] ids = new String[AbilityHotbar.SLOTS];
        for (int i = 0; i < AbilityHotbar.SLOTS; i++) {
            String id = buf.readUtf(64);
            ids[i] = id.isEmpty() ? null : id;
        }
        return new SyncHotbarPayload(ids);
    }

    private static void write(FriendlyByteBuf buf, SyncHotbarPayload payload) {
        for (int i = 0; i < AbilityHotbar.SLOTS; i++) {
            buf.writeUtf(payload.slotIds[i] != null ? payload.slotIds[i] : "", 64);
        }
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                DnDPlayerData data = PlayerDataHelper.get(serverPlayer);
                AbilityHotbar hotbar = data.getAbilityHotbar();
                for (int i = 0; i < AbilityHotbar.SLOTS; i++) {
                    if (slotIds[i] != null) {
                        hotbar.slotAbility(i, slotIds[i]);
                    } else {
                        hotbar.clearSlot(i);
                    }
                }
                PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
