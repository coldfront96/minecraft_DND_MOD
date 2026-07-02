package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.AbilityDamageCalculator;
import com.khimairacraft.combat.ModDamageTypes;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.combat.SaveType;
import com.khimairacraft.combat.SavingThrowSystem;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class GroundSlam extends Ability {
    public GroundSlam() {
        super("barbarian_ground_slam", "Ground Slam",
                "AOE knockback + damage to all mobs within 4 blocks",
                DnDClass.BARBARIAN, 4, 20, 100, ClickBehavior.REPLACES_ATTACK);
    }

    @Override
    public float getBaseDamage() { return 5.0f; }

    @Override
    public AbilityScoreType getDamageScalingStat() { return AbilityScoreType.STRENGTH; }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        PerPlayerCombatState.get(player.getUUID()).setConsumeNextAttack(true);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 5, 2.0, 0.5, 2.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY(), player.getZ(), 20, 3.0, 0.2, 3.0, 0.01);
        }

        int dc = SavingThrowSystem.getAbilityDC(this, data);

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(4.0))) {
            if (entity instanceof LivingEntity living) {
                boolean saved = SavingThrowSystem.rollSave(living, SaveType.FORTITUDE, dc, player);
                float damage = AbilityDamageCalculator.calculate(this, data, living, player);
                float multiplier = SavingThrowSystem.getSaveMultiplier(saved, false);
                damage *= multiplier;

                if (damage > 0) {
                    living.hurt(ModDamageTypes.abilityDamage(player.level(), player), damage);
                }

                if (!saved) {
                    Vec3 knockback = entity.position().subtract(player.position()).normalize().scale(2.0);
                    living.setDeltaMovement(knockback.x, 0.5, knockback.z);
                    living.hurtMarked = true;
                }
            }
        }
    }
}
