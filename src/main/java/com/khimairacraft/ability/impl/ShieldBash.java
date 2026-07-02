package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.combat.SaveType;
import com.khimairacraft.playerdata.DnDPlayerData;
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
