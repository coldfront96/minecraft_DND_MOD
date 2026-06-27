package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityScoreType;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.combat.PerPlayerCombatState;
import com.deadmind.dndmods.combat.SaveType;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;

public class ShieldBash extends Ability {
    public ShieldBash() {
        super("fighter_shield_bash", "Shield Bash",
                "Knockback + stun (Slowness III, 2s) to targets in front",
                DnDClass.FIGHTER, 3, 20, 80, ClickBehavior.ENHANCES_ATTACK);
    }

    @Override
    public float getBaseDamage() { return 3.0f; }

    @Override
    public AbilityScoreType getDamageScalingStat() { return AbilityScoreType.STRENGTH; }

    @Override
    public SaveType getRequiredSave() { return SaveType.FORTITUDE; }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        PerPlayerCombatState.get(player.getUUID()).setPendingEnhancement(getId());
    }
}
