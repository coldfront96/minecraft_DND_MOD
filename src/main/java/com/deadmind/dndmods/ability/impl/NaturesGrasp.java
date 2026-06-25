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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NaturesGrasp extends Ability {
    public NaturesGrasp() {
        super("ranger_natures_grasp", "Nature's Grasp",
                "Root target in place with web effect for 3 seconds",
                DnDClass.RANGER, 6, 25, 140, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        LivingEntity target = null;
        double closestDist = 12.0;

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(12.0))) {
            if (entity instanceof LivingEntity living) {
                Vec3 toEntity = entity.position().subtract(eyePos).normalize();
                if (look.dot(toEntity) > 0.7) {
                    double dist = entity.distanceTo(player);
                    if (dist < closestDist) {
                        closestDist = dist;
                        target = living;
                    }
                }
            }
        }

        if (target != null) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 100, false, true));

            BlockPos pos = target.blockPosition();
            if (player.level().getBlockState(pos).isAir()) {
                player.level().setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
                // Schedule removal via entity persistent data
                target.getPersistentData().putLong("dndmods_web_pos", pos.asLong());
                target.getPersistentData().putLong("dndmods_web_remove_at", player.level().getGameTime() + 60);
            }
        }
    }
}
