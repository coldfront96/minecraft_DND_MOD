package com.khimairacraft.network;

import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.DnDPlayerData.RangerCombatStyle;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Locks in the Ranger's Combat Style at level 2. The choice is one-time and
 * permanent: the server only accepts it when the player is a Ranger of level 2+
 * and their style is still NONE.
 */
public record SelectRangerCombatStylePayload(String choice) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger("DnDMods/RangerCombatStyle");

    public static final Type<SelectRangerCombatStylePayload> TYPE =
            new Type<>(ModNetwork.id("select_ranger_combat_style"));

    public static final StreamCodec<FriendlyByteBuf, SelectRangerCombatStylePayload> STREAM_CODEC =
            StreamCodec.of(SelectRangerCombatStylePayload::write, SelectRangerCombatStylePayload::read);

    private static SelectRangerCombatStylePayload read(FriendlyByteBuf buf) {
        return new SelectRangerCombatStylePayload(buf.readUtf(24));
    }

    private static void write(FriendlyByteBuf buf, SelectRangerCombatStylePayload payload) {
        buf.writeUtf(payload.choice, 24);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
            if (data == null) return;

            if (data.getRangerCombatStyle() != RangerCombatStyle.NONE) {
                LOGGER.warn("Player {} already has a Ranger Combat Style", player.getName().getString());
                return;
            }
            if (data.getClassLevel(DnDClass.RANGER) < 2) {
                LOGGER.warn("Player {} tried to pick a Combat Style below Ranger 2", player.getName().getString());
                return;
            }

            RangerCombatStyle style;
            try {
                style = RangerCombatStyle.valueOf(choice);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Player {} sent invalid Combat Style: {}", player.getName().getString(), choice);
                return;
            }
            if (style != RangerCombatStyle.ARCHERY && style != RangerCombatStyle.TWO_WEAPON) {
                LOGGER.warn("Player {} sent non-selectable Combat Style: {}", player.getName().getString(), choice);
                return;
            }

            data.setRangerCombatStyle(style);
            player.displayClientMessage(
                    Component.literal("Combat Style locked in: "
                            + (style == RangerCombatStyle.ARCHERY ? "Archery" : "Two-Weapon Combat"))
                            .withStyle(s -> s.withColor(0x55FF55)), false);
            LOGGER.info("Player {} chose Ranger Combat Style: {}", player.getName().getString(), style);

            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
