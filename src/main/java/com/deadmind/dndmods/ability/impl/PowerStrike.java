package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityScoreType;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.combat.PerPlayerCombatState;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
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
