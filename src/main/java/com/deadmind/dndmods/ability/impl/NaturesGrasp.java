package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public class NaturesGrasp extends Ability {
    public NaturesGrasp() {
        super("ranger_natures_grasp", "Nature's Grasp", DnDClass.RANGER, 6, 25, 140);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        AABB area = player.getBoundingBox().inflate(6.0);
        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1));
    }
}
