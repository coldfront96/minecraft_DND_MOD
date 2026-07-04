package com.khimairacraft.ability.fighter;

import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.party.Party;
import com.khimairacraft.party.PartyManager;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Level 20 — Warlord's Presence. For 15 seconds all online party members within
 * 20 blocks (and the caster) gain the Warlord buff: +3 damage (applied in
 * FighterPassives) and fear immunity (flag/note — no fear mechanic here). Uses
 * {@link PartyManager} directly rather than the friendly-fire filter, since this
 * is a party buff, not a hostile-targeting decision. With no party it still
 * buffs the caster.
 */
public class WarlordsPresenceAbility extends FighterAbility {
    public WarlordsPresenceAbility() {
        super("fighter_warlords_presence", "Warlord's Presence",
                "For 15 seconds, party members within 20 blocks gain +3 attack, +3 damage, and fear immunity.",
                20, 50, 600, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        int duration = 300;
        // The caster is always buffed (solo fallback).
        PerPlayerCombatState.get(player.getUUID()).setWarlordsPresence(duration);

        MinecraftServer server = player.getServer();
        Party party = PartyManager.getInstance().getPartyOf(player.getUUID());
        if (party != null && server != null) {
            for (UUID memberId : party.getMembers().keySet()) {
                if (memberId.equals(player.getUUID())) continue;
                ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                if (member == null) continue;
                if (member.level() != player.level()) continue;
                if (member.distanceTo(player) > 20.0) continue;
                PerPlayerCombatState.get(memberId).setWarlordsPresence(duration);
                member.sendSystemMessage(Component.literal("§6[DnDMods] §e" + player.getName().getString()
                        + "'s Warlord's Presence empowers you!"));
            }
        }
        player.sendSystemMessage(Component.literal("§6[DnDMods] §eWarlord's Presence!"));
    }
}
