package com.khimairacraft.network;

import com.khimairacraft.feat.Feat;
import com.khimairacraft.feat.FeatRegistry;
import com.khimairacraft.feat.FeatSourceConfig;
import com.khimairacraft.feat.FeatPrerequisite;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SelectFeatPayload(String featId, boolean isRacialBonusSlot, boolean isFighterBonusSlot) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger("DnDMods/FeatSelect");

    public static final CustomPacketPayload.Type<SelectFeatPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("select_feat"));

    public static final StreamCodec<FriendlyByteBuf, SelectFeatPayload> STREAM_CODEC =
            StreamCodec.of(SelectFeatPayload::write, SelectFeatPayload::read);

    private static SelectFeatPayload read(FriendlyByteBuf buf) {
        return new SelectFeatPayload(buf.readUtf(64), buf.readBoolean(), buf.readBoolean());
    }

    private static void write(FriendlyByteBuf buf, SelectFeatPayload payload) {
        buf.writeUtf(payload.featId, 64);
        buf.writeBoolean(payload.isRacialBonusSlot);
        buf.writeBoolean(payload.isFighterBonusSlot);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
            if (data == null) return;

            Feat feat = FeatRegistry.get(featId);
            if (feat == null) {
                LOGGER.warn("Player {} sent invalid feat ID: {}", player.getName().getString(), featId);
                return;
            }

            if (!FeatSourceConfig.isEnabled(feat.getSource())) {
                LOGGER.warn("Player {} attempted to select disabled feat source: {}", player.getName().getString(), feat.getSource());
                return;
            }

            if (data.hasFeat(featId)) {
                LOGGER.warn("Player {} already has feat: {}", player.getName().getString(), featId);
                return;
            }

            if (!feat.allPrerequisitesMet(data)) {
                LOGGER.warn("Player {} does not meet prerequisites for feat: {}", player.getName().getString(), featId);
                return;
            }

            if (isFighterBonusSlot) {
                if (!feat.isFighterBonusFeat()) {
                    LOGGER.warn("Player {} tried to use a Fighter bonus slot on a non-combat feat: {}",
                            player.getName().getString(), featId);
                    return;
                }
                if (data.getFighterBonusFeatSlotsAvailable() <= 0) {
                    LOGGER.warn("Player {} has no Fighter bonus feat slots available", player.getName().getString());
                    return;
                }
                data.spendFighterBonusFeatSlot();
            } else if (isRacialBonusSlot) {
                if (!data.isHumanBonusFeatAvailable()) {
                    LOGGER.warn("Player {} does not have racial bonus feat slot", player.getName().getString());
                    return;
                }
                data.setHumanBonusFeatAvailable(false);
            } else {
                if (data.getFeatSlotsAvailable() <= 0) {
                    LOGGER.warn("Player {} has no feat slots available", player.getName().getString());
                    return;
                }
                data.spendFeatSlot();
            }

            data.grantFeat(featId);
            feat.applyGrant(data);

            LOGGER.info("Player {} selected feat: {}", player.getName().getString(), feat.getDisplayName());

            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
