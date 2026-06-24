package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Reckless extends Ability {
    public Reckless() {
        super("barbarian_reckless", "Reckless Attack", DnDClass.BARBARIAN, 1, 10, 60);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 2));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
    }
}
