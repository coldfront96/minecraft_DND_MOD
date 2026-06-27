package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.combat.PerPlayerCombatState;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
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
