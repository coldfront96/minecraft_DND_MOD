package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.AbilityDamageCalculator;
import com.khimairacraft.combat.ModDamageTypes;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.combat.SaveType;
import com.khimairacraft.party.FriendlyFireChecker;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.phys.Vec3;

public class SmiteUndead extends Ability {
    public SmiteUndead() {
        super("cleric_smite_undead", "Smite Undead",
                "Deal holy damage (extra to undead) to mobs in front",
                DnDClass.CLERIC, 3, 25, 80, ClickBehavior.REPLACES_ATTACK);
    }

    @Override
    public float getBaseDamage() { return 8.0f; }

    @Override
    public AbilityScoreType getDamageScalingStat() { return AbilityScoreType.WISDOM; }

    @Override
    public SaveType getRequiredSave() { return SaveType.WILL; }

    @Override
    public boolean isHalfOnSave() { return true; }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        PerPlayerCombatState.get(player.getUUID()).setConsumeNextAttack(true);

        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 particlePos = eyePos.add(look.scale(3.0));
            serverLevel.sendParticles(ParticleTypes.END_ROD, particlePos.x, particlePos.y, particlePos.z, 15, 1.0, 1.0, 1.0, 0.05);
        }

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(6.0))) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!(living instanceof Enemy) && !(living instanceof ServerPlayer)) continue;
            if (FriendlyFireChecker.isFriendly(player, living)) continue;
            if (!player.hasLineOfSight(living)) continue;
            Vec3 toEntity = entity.getEyePosition().subtract(eyePos).normalize();
            if (look.dot(toEntity) > 0.5) {
                // Holy damage hits foes in front; undead take extra and are set
                // alight by the holy fire (matches the ability text).
                boolean isUndead = living.getType().is(EntityTypeTags.UNDEAD);

                float damage = AbilityDamageCalculator.calculate(this, data, living, player);
                if (isUndead) {
                    damage *= 1.5f;
                }
                living.hurt(ModDamageTypes.abilityDamage(player.level(), player), damage);
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                if (isUndead) {
                    living.setRemainingFireTicks(60);
                }
            }
        }
    }
}
