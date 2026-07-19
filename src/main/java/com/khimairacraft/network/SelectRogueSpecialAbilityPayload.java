package com.khimairacraft.network;

import com.khimairacraft.playerdata.DnDPlayerData;
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

import java.util.Map;

/**
 * Spends a Rogue special ability slot (earned at Rogue 10/13/16/19) on one of
 * the six options. Crippling Strike is the only repeatable pick; selecting an
 * already-owned non-repeatable is rejected WITHOUT consuming the slot. The
 * "general bonus feat" alternative goes through SelectFeatPayload with
 * isRogueSpecialSlot instead of this payload.
 */
public record SelectRogueSpecialAbilityPayload(String optionId) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger("DnDMods/RogueSpecial");

    public static final CustomPacketPayload.Type<SelectRogueSpecialAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("select_rogue_special"));

    public static final StreamCodec<FriendlyByteBuf, SelectRogueSpecialAbilityPayload> STREAM_CODEC =
            StreamCodec.of(SelectRogueSpecialAbilityPayload::write, SelectRogueSpecialAbilityPayload::read);

    // Non-repeatable option id -> achievement flag it sets.
    public static final Map<String, String> NON_REPEATABLE_FLAGS = Map.of(
            "defensive_roll", "defensive_roll_unlocked",
            "improved_evasion", "improved_evasion_rogue_unlocked",
            "opportunist", "opportunist_unlocked",
            "slippery_mind", "slippery_mind_rogue_unlocked",
            "practiced_professional", "practiced_professional_unlocked"
    );

    private static SelectRogueSpecialAbilityPayload read(FriendlyByteBuf buf) {
        return new SelectRogueSpecialAbilityPayload(buf.readUtf(48));
    }

    private static void write(FriendlyByteBuf buf, SelectRogueSpecialAbilityPayload payload) {
        buf.writeUtf(payload.optionId, 48);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
            if (data == null) return;

            if (data.getRogueSpecialAbilitySlotsAvailable() <= 0) {
                LOGGER.warn("Player {} has no Rogue special ability slots", player.getName().getString());
                return;
            }

            if ("crippling_strike".equals(optionId)) {
                data.setRogueCripplingStrikeStacks(data.getRogueCripplingStrikeStacks() + 1);
            } else {
                String flag = NON_REPEATABLE_FLAGS.get(optionId);
                if (flag == null) {
                    LOGGER.warn("Player {} sent invalid Rogue special ability: {}",
                            player.getName().getString(), optionId);
                    return;
                }
                if (data.getAchievementFlag(flag)) {
                    // Already owned — refuse rather than burn the slot on nothing.
                    player.displayClientMessage(
                            Component.literal("You already have that special ability.")
                                    .withStyle(s -> s.withColor(0xFF5555)), false);
                    return;
                }
                data.setAchievementFlag(flag, true);
            }

            data.setRogueSpecialAbilitySlotsAvailable(data.getRogueSpecialAbilitySlotsAvailable() - 1);
            LOGGER.info("Player {} selected Rogue special ability: {}", player.getName().getString(), optionId);

            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
