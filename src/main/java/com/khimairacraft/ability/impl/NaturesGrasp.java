package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.AbilityDamageCalculator;
import com.khimairacraft.combat.ModDamageTypes;
import com.khimairacraft.combat.SaveType;
import com.khimairacraft.combat.SavingThrowSystem;
import com.khimairacraft.party.FriendlyFireChecker;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class NaturesGrasp extends Ability {
    public NaturesGrasp() {
        super("ranger_natures_grasp", "Nature's Grasp",
                "Root target in place with web effect for 3 seconds",
                DnDClass.RANGER, 6, 25, 140, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public float getBaseDamage() { return 1.0f; }

    @Override
    public AbilityScoreType getDamageScalingStat() { return AbilityScoreType.WISDOM; }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        LivingEntity target = null;
        double closestDist = 12.0;

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(12.0))) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!(living instanceof Enemy) && !(living instanceof ServerPlayer)) continue;
            if (FriendlyFireChecker.isFriendly(player, living)) continue;
            if (!player.hasLineOfSight(living)) continue;
            Vec3 toEntity = entity.getEyePosition().subtract(eyePos).normalize();
            if (look.dot(toEntity) > 0.7) {
                double dist = entity.distanceTo(player);
                if (dist < closestDist) {
                    closestDist = dist;
                    target = living;
                }
            }
        }

        if (target != null) {
            int dc = SavingThrowSystem.getAbilityDC(this, data);
            boolean saved = SavingThrowSystem.rollSave(target, SaveType.REFLEX, dc, player);
            float damage = AbilityDamageCalculator.calculate(this, data, target, player);
            float multiplier = SavingThrowSystem.getSaveMultiplier(saved, false);
            damage *= multiplier;

            if (damage > 0) {
                target.hurt(ModDamageTypes.abilityDamage(player.level(), player), damage);
            }

            if (!saved) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 2, false, true));

                BlockPos pos = target.blockPosition();
                if (player.level().getBlockState(pos).isAir()) {
                    player.level().setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
                    target.getPersistentData().putLong("khimairacraft_web_pos", pos.asLong());
                    target.getPersistentData().putLong("khimairacraft_web_remove_at", player.level().getGameTime() + 60);
                }
            }

            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.5, 1.0, 0.5, 0.02);
            }
        }
    }
}
