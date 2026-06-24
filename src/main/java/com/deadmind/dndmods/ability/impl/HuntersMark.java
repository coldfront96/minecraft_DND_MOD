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
import net.minecraft.world.phys.Vec3;

public class HuntersMark extends Ability {
    public HuntersMark() {
        super("ranger_hunters_mark", "Hunter's Mark", DnDClass.RANGER, 3, 20, 100);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        LivingEntity closest = null;
        double closestDist = 20.0;

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(20.0))) {
            if (entity instanceof LivingEntity living) {
                Vec3 toEntity = entity.position().subtract(eyePos).normalize();
                if (look.dot(toEntity) > 0.8) {
                    double dist = entity.distanceTo(player);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = living;
                    }
                }
            }
        }

        if (closest != null) {
            closest.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            closest.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        }
    }
}
