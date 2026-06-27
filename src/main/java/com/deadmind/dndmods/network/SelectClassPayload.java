package com.deadmind.dndmods.network;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectClassPayload(String className) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SelectClassPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("select_class"));

    public static final StreamCodec<FriendlyByteBuf, SelectClassPayload> STREAM_CODEC =
            StreamCodec.of(SelectClassPayload::write, SelectClassPayload::read);

    private static SelectClassPayload read(FriendlyByteBuf buf) {
        return new SelectClassPayload(buf.readUtf(32));
    }

    private static void write(FriendlyByteBuf buf, SelectClassPayload payload) {
        buf.writeUtf(payload.className, 32);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                DnDPlayerData data = PlayerDataHelper.get(serverPlayer);

                if (data.getDnDClass() != DnDClass.NONE) {
                    DnDMods.LOGGER.warn("{} tried to select class but already has {}",
                            serverPlayer.getName().getString(), data.getDnDClass());
                    return;
                }

                DnDClass chosen;
                try {
                    chosen = DnDClass.valueOf(className);
                } catch (IllegalArgumentException e) {
                    DnDMods.LOGGER.warn("{} sent invalid class name: {}", serverPlayer.getName().getString(), className);
                    return;
                }

                if (chosen == DnDClass.NONE) {
                    DnDMods.LOGGER.warn("{} tried to select NONE as class", serverPlayer.getName().getString());
                    return;
                }

                data.setDnDClass(chosen);
                PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));
                DnDMods.LOGGER.info("{} selected class {}", serverPlayer.getName().getString(), chosen);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
