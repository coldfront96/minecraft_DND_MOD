package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GroundSlam extends Ability {
    public GroundSlam() {
        super("barbarian_ground_slam", "Ground Slam", DnDClass.BARBARIAN, 4, 20, 100);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        AABB area = player.getBoundingBox().inflate(5.0);
        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof LivingEntity living) {
                living.hurt(player.damageSources().playerAttack(player), 6.0f);
                Vec3 knockback = entity.position().subtract(player.position()).normalize().scale(2.0);
                living.setDeltaMovement(knockback.x, 0.5, knockback.z);
            }
        }
    }
}
