package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityScoreType;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.combat.AbilityDamageCalculator;
import com.deadmind.dndmods.combat.ModDamageTypes;
import com.deadmind.dndmods.combat.PerPlayerCombatState;
import com.deadmind.dndmods.party.FriendlyFireChecker;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

public class BackStab extends Ability {
    public BackStab() {
        super("rogue_backstab", "Backstab",
                "Deal 3x damage if target isn't facing you",
                DnDClass.ROGUE, 1, 15, 40, ClickBehavior.REPLACES_ATTACK);
    }

    @Override
    public float getBaseDamage() { return 4.0f; }

    @Override
    public AbilityScoreType getDamageScalingStat() { return AbilityScoreType.DEXTERITY; }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        PerPlayerCombatState.get(player.getUUID()).setConsumeNextAttack(true);

        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        LivingEntity target = null;
        double closestDist = 5.0;

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(5.0))) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!(living instanceof Monster) && !(living instanceof ServerPlayer)) continue;
            if (FriendlyFireChecker.isFriendly(player, living)) continue;
            if (!player.hasLineOfSight(living)) continue;
            Vec3 toEntity = entity.position().subtract(eyePos).normalize();
            if (look.dot(toEntity) > 0.7) {
                double dist = entity.distanceTo(player);
                if (dist < closestDist) {
                    closestDist = dist;
                    target = living;
                }
            }
        }

        if (target != null) {
            float damage = AbilityDamageCalculator.calculate(this, data, target, player);

            Vec3 targetLook = target.getLookAngle();
            Vec3 playerToTarget = target.position().subtract(player.position()).normalize();
            boolean targetFacingAway = targetLook.dot(playerToTarget) > 0.0;

            if (targetFacingAway) {
                damage *= 3.0f;
            }

            target.hurt(ModDamageTypes.abilityDamage(player.level(), player), damage);
        }
    }
}
