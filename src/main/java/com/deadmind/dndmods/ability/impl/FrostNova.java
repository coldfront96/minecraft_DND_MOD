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

public class FrostNova extends Ability {
    public FrostNova() {
        super("wizard_frost_nova", "Frost Nova", DnDClass.WIZARD, 4, 30, 100);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        AABB area = player.getBoundingBox().inflate(5.0);
        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                living.hurt(player.damageSources().freeze(), 4.0f);
            }
        }
    }
}
