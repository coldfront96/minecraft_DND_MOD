package com.khimairacraft.network;

import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import com.khimairacraft.ranger.FavoredEnemyType;
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
 * Spends a Favored Enemy slot (earned at Ranger 1/5/10/15/20) on a category.
 * A brand-new category is added at +2; re-picking an existing one raises its
 * bonus by another +2.
 */
public record SelectFavoredEnemyPayload(String categoryName) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger("DnDMods/FavoredEnemy");

    public static final Type<SelectFavoredEnemyPayload> TYPE =
            new Type<>(ModNetwork.id("select_favored_enemy"));

    public static final StreamCodec<FriendlyByteBuf, SelectFavoredEnemyPayload> STREAM_CODEC =
            StreamCodec.of(SelectFavoredEnemyPayload::write, SelectFavoredEnemyPayload::read);

    private static SelectFavoredEnemyPayload read(FriendlyByteBuf buf) {
        return new SelectFavoredEnemyPayload(buf.readUtf(32));
    }

    private static void write(FriendlyByteBuf buf, SelectFavoredEnemyPayload payload) {
        buf.writeUtf(payload.categoryName, 32);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
            if (data == null) return;

            if (data.getFavoredEnemySlotsAvailable() <= 0) {
                LOGGER.warn("Player {} has no Favored Enemy slots", player.getName().getString());
                return;
            }

            FavoredEnemyType type = FavoredEnemyType.byName(categoryName);
            if (type == null) {
                LOGGER.warn("Player {} sent invalid Favored Enemy: {}", player.getName().getString(), categoryName);
                return;
            }

            data.selectFavoredEnemy(type);
            data.spendFavoredEnemySlot();
            int bonus = data.getFavoredEnemyBonusFor(type);
            player.displayClientMessage(
                    Component.literal("Favored Enemy: " + type.getDisplayName() + " (+" + bonus + ")")
                            .withStyle(s -> s.withColor(0x55FF55)), false);
            LOGGER.info("Player {} selected Favored Enemy {} (now +{})",
                    player.getName().getString(), type, bonus);

            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
