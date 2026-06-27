package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.core.BlockPos;
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
                DnDClass.ROGUE, 7, 30, 160, ClickBehavior.CONSUMES_CLICK);
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
            // Avoid teleporting into a wall behind the target (e.g. target
            // standing against a wall) — fall back to the target's own footprint.
            if (isSafeDestination(player, behind)) {
                player.teleportTo(behind.x, behind.y, behind.z);
            } else {
                player.teleportTo(target.getX(), target.getY(), target.getZ());
            }
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
        } else {
            Vec3 look = player.getLookAngle();
            Vec3 dest = player.position().add(look.scale(8.0));
            // Only blink forward if the destination won't suffocate the player.
            if (isSafeDestination(player, dest)) {
                player.teleportTo(dest.x, dest.y, dest.z);
            }
        }
    }

    /** A destination is safe if neither the foot nor head block is a full solid block. */
    private static boolean isSafeDestination(ServerPlayer player, Vec3 dest) {
        BlockPos foot = BlockPos.containing(dest);
        BlockPos head = foot.above();
        return !player.level().getBlockState(foot).isCollisionShapeFullBlock(player.level(), foot)
                && !player.level().getBlockState(head).isCollisionShapeFullBlock(player.level(), head);
    }
}
