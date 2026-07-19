package com.khimairacraft.ability.fighter;

import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Level 15 — Unbreakable. For 6 seconds take 50% less damage and become immune
 * to knockback (both applied in FighterPassives while the flag is active). Stun
 * immunity is a flag-only note — there is no stun mechanic in this combat model.
 */
public class UnbreakableAbility extends FighterAbility {
    public UnbreakableAbility() {
        super("fighter_unbreakable", "Unbreakable",
                "For 6 seconds take 50% less damage and ignore knockback and stun.",
                15, 40, 1200, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        PerPlayerCombatState.get(player.getUUID()).setUnbreakable(120);
        player.sendSystemMessage(Component.literal("§6[DnDMods] §eYou brace — Unbreakable!"));
    }
}
