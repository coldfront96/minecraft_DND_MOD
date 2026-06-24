package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;

public class SecondWind extends Ability {
    public SecondWind() {
        super("fighter_second_wind", "Second Wind", DnDClass.FIGHTER, 5, 30, 200);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        float healAmount = player.getMaxHealth() * 0.3f;
        player.heal(healAmount);
    }
}
