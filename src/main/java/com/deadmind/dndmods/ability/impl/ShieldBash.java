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

public class ShieldBash extends Ability {
    public ShieldBash() {
        super("fighter_shield_bash", "Shield Bash",
                "Knockback + stun (Slowness III, 2s) to targets in front",
                DnDClass.FIGHTER, 3, 20, 80);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        AABB area = player.getBoundingBox().inflate(4.0);

        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof LivingEntity living) {
                Vec3 toEntity = entity.position().subtract(eyePos).normalize();
                if (look.dot(toEntity) > 0.5) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, true));
                    Vec3 knockDir = toEntity.normalize().scale(2.0);
                    living.setDeltaMovement(knockDir.x, 0.4, knockDir.z);
                    living.hurtMarked = true;
                    living.hurt(player.damageSources().playerAttack(player), 3.0f);
                }
            }
        }
    }
}
