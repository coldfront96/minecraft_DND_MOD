package com.deadmind.dndmods.network;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import com.deadmind.dndmods.race.DnDRace;
import com.deadmind.dndmods.race.RaceConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SelectRacePayload(String raceName) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger("DnDMods/RaceSelect");

    public static final CustomPacketPayload.Type<SelectRacePayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("select_race"));

    public static final StreamCodec<FriendlyByteBuf, SelectRacePayload> STREAM_CODEC =
            StreamCodec.of(SelectRacePayload::write, SelectRacePayload::read);

    private static SelectRacePayload read(FriendlyByteBuf buf) {
        return new SelectRacePayload(buf.readUtf(64));
    }

    private static void write(FriendlyByteBuf buf, SelectRacePayload payload) {
        buf.writeUtf(payload.raceName, 64);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;

            DnDRace race;
            try {
                race = DnDRace.valueOf(raceName);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Player {} sent invalid race name: {}", serverPlayer.getName().getString(), raceName);
                return;
            }

            if (race == DnDRace.NONE) {
                LOGGER.warn("Player {} attempted to select NONE race", serverPlayer.getName().getString());
                return;
            }

            if (!RaceConfig.isSourceEnabled(race.getSource())) {
                LOGGER.warn("Player {} attempted to select disabled race source: {}", serverPlayer.getName().getString(), race.getSource());
                return;
            }

            DnDPlayerData data = PlayerDataHelper.get(serverPlayer);
            data.setRace(race);

            LOGGER.info("Player {} selected race: {}", serverPlayer.getName().getString(), race.getDisplayName());

            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));

            if (data.getDnDClass() == DnDClass.NONE) {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenClassSelectionPayload());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
