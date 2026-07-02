package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;

public class PowerStrike extends Ability {
    public PowerStrike() {
        super("fighter_power_strike", "Power Strike",
                "Next attack deals +50% damage",
                DnDClass.FIGHTER, 1, 15, 60, ClickBehavior.ENHANCES_ATTACK);
    }

    @Override
    public float getBaseDamage() { return 0; }

    @Override
    public AbilityScoreType getDamageScalingStat() { return AbilityScoreType.STRENGTH; }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        PerPlayerCombatState.get(player.getUUID()).setPendingEnhancement(getId());
    }
}
