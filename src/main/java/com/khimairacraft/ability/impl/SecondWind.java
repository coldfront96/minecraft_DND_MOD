package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;

public class SecondWind extends Ability {
    public SecondWind() {
        super("fighter_second_wind", "Second Wind",
                "Restore 30% of max HP",
                DnDClass.FIGHTER, 5, 30, 200, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        float healAmount = player.getMaxHealth() * 0.3f;
        player.heal(healAmount);
    }
}
