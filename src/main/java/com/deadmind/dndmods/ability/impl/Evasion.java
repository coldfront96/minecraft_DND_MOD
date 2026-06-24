package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Evasion extends Ability {
    public Evasion() {
        super("rogue_evasion", "Evasion", DnDClass.ROGUE, 4, 20, 120);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 2));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1));
    }
}
