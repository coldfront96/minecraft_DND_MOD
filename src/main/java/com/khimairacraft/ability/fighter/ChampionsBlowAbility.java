package com.khimairacraft.ability.fighter;

import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.combat.ModDamageTypes;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Level 17 — Champion's Blow. A single front strike dealing bonus damage equal
 * to 3x the STR modifier; against a target below 25% HP it is a guaranteed
 * critical (double damage) — the guarantee bypasses the (vanilla) crit roll by
 * applying the multiplier directly.
 */
public class ChampionsBlowAbility extends FighterAbility {
    public ChampionsBlowAbility() {
        super("fighter_champions_blow", "Champion's Blow",
                "A decisive strike: 3x STR modifier bonus damage; guaranteed crit below 25% HP.",
                17, 45, 400, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        LivingEntity target = null;
        double closest = 4.0;
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(4.0))) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValidHostileTarget(player, living)) continue;
            Vec3 toEntity = entity.getEyePosition().subtract(eye).normalize();
            if (look.dot(toEntity) < 0.5) continue;
            double dist = entity.distanceTo(player);
            if (dist < closest) {
                closest = dist;
                target = living;
            }
        }

        if (target != null) {
            float dmg = 6.0f + 3.0f * data.getAbilityScores().getStrMod();
            if (target.getHealth() < target.getMaxHealth() * 0.25f) {
                dmg *= 2.0f; // guaranteed critical
            }
            target.hurt(ModDamageTypes.abilityDamage(player.level(), player), dmg);
        }
    }
}
