package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class ArcaneShield extends Ability {
    public ArcaneShield() {
        super("wizard_arcane_shield", "Arcane Shield", DnDClass.WIZARD, 7, 35, 200);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 2));
    }
}
