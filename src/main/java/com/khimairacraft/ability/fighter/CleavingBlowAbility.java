package com.khimairacraft.ability.fighter;

import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.combat.AbilityDamageCalculator;
import com.khimairacraft.combat.ModDamageTypes;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Level 5 — Cleaving Blow. A 90-degree frontal arc melee attack (3-block range)
 * dealing full weapon damage to every valid enemy. Uses the shared targeting
 * filter (hostile mob/player, not friendly, in line of sight).
 */
public class CleavingBlowAbility extends FighterAbility {
    public CleavingBlowAbility() {
        super("fighter_cleaving_blow", "Cleaving Blow",
                "Sweep a 90-degree arc, striking all enemies within 3 blocks in front of you.",
                5, 15, 0, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public float getBaseDamage() { return 6.0f; }

    @Override
    public AbilityScoreType getDamageScalingStat() { return AbilityScoreType.STRENGTH; }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(3.0))) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValidHostileTarget(player, living)) continue;
            Vec3 toEntity = entity.getEyePosition().subtract(eye).normalize();
            if (look.dot(toEntity) < 0.707) continue; // ~90-degree frontal arc
            float dmg = AbilityDamageCalculator.calculate(this, data, living, player);
            living.hurt(ModDamageTypes.abilityDamage(player.level(), player), dmg);
        }
    }
}
