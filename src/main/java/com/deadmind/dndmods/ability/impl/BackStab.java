package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class BackStab extends Ability {
    public BackStab() {
        super("rogue_backstab", "Backstab", DnDClass.ROGUE, 1, 15, 40);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1));
    }
}
