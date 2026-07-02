package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;

public class Reckless extends Ability {
    public Reckless() {
        super("barbarian_reckless", "Reckless Attack",
                "Deal +40% damage but take +20% more for 10 seconds",
                DnDClass.BARBARIAN, 1, 10, 60, ClickBehavior.ENHANCES_ATTACK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        PerPlayerCombatState.CombatState state = PerPlayerCombatState.get(player.getUUID());
        state.setPendingEnhancement(getId());
        state.setReckless(200);
    }
}
