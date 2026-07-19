package com.khimairacraft.ability.fighter;

import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Level 1 — Power Strike. The next melee hit within 3 seconds deals bonus damage
 * equal to the STR modifier (applied in FighterPassives). The buff is consumed
 * on the next successful hit or expires after 3 seconds.
 */
public class PowerStrikeAbility extends FighterAbility {
    public PowerStrikeAbility() {
        super("fighter_power_strike", "Power Strike",
                "Your next melee attack within 3 seconds deals bonus damage equal to your STR modifier.",
                1, 10, 0, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        PerPlayerCombatState.get(player.getUUID()).setPowerStrike(60);
    }
}
