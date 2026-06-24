package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class PowerStrike extends Ability {
    public PowerStrike() {
        super("fighter_power_strike", "Power Strike", DnDClass.FIGHTER, 1, 15, 60);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1));
    }
}
