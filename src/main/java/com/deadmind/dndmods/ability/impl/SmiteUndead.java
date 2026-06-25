package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SmiteUndead extends Ability {
    public SmiteUndead() {
        super("cleric_smite_undead", "Smite Undead",
                "Deal holy damage (extra to undead) to mobs in front",
                DnDClass.CLERIC, 3, 25, 80, ClickBehavior.REPLACES_ATTACK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        AABB area = player.getBoundingBox().inflate(6.0);

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 particlePos = eyePos.add(look.scale(3.0));
            serverLevel.sendParticles(ParticleTypes.END_ROD, particlePos.x, particlePos.y, particlePos.z, 15, 1.0, 1.0, 1.0, 0.05);
        }

        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof LivingEntity living) {
                Vec3 toEntity = entity.position().subtract(eyePos).normalize();
                if (look.dot(toEntity) > 0.5) {
                    boolean isUndead = living.getMobType() == MobType.UNDEAD;
                    float damage = isUndead ? 16.0f : 6.0f;
                    living.hurt(player.damageSources().playerAttack(player), damage);
                    if (isUndead) {
                        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                        living.setRemainingFireTicks(60);
                    }
                }
            }
        }
    }
}
