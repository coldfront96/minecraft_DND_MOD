package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Fireball extends Ability {
    public Fireball() {
        super("wizard_fireball", "Fireball",
                "Launch an explosion at your target area, scales with INT",
                DnDClass.WIZARD, 1, 25, 80);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 center = player.getEyePosition().add(look.scale(8.0));
        double radius = 4.0;
        AABB area = new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius));

        int intMod = data.getAbilityScores().getIntMod();
        float baseDamage = 8.0f;
        float damage = baseDamage + intMod * 2.0f;

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 3, 1.0, 1.0, 1.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 30, 2.0, 2.0, 2.0, 0.05);
        }

        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof LivingEntity living) {
                living.setRemainingFireTicks(100);
                living.hurt(player.damageSources().playerAttack(player), damage);
            }
        }
    }
}
