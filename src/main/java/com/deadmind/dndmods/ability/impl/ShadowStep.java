package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class ShadowStep extends Ability {
    public ShadowStep() {
        super("rogue_shadow_step", "Shadow Step",
                "Teleport behind the nearest hostile mob within 16 blocks",
                DnDClass.ROGUE, 7, 30, 160);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Mob target = null;
        double closestDist = 16.0;

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(16.0))) {
            if (entity instanceof Mob mob) {
                double dist = entity.distanceTo(player);
                if (dist < closestDist) {
                    closestDist = dist;
                    target = mob;
                }
            }
        }

        if (target != null) {
            Vec3 targetLook = target.getLookAngle().normalize();
            Vec3 behind = target.position().subtract(targetLook.scale(2.0));
            player.teleportTo(behind.x, behind.y, behind.z);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
        } else {
            Vec3 look = player.getLookAngle();
            player.teleportTo(
                    player.getX() + look.x * 8.0,
                    player.getY() + look.y * 8.0,
                    player.getZ() + look.z * 8.0
            );
        }
    }
}
