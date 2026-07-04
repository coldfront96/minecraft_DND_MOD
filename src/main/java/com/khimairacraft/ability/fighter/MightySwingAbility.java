package com.khimairacraft.ability.fighter;

import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.combat.ModDamageTypes;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Level 9 — Mighty Swing. A single powerful front strike dealing bonus damage
 * equal to twice the STR modifier and knocking the target back ~2 blocks.
 */
public class MightySwingAbility extends FighterAbility {
    public MightySwingAbility() {
        super("fighter_mighty_swing", "Mighty Swing",
                "A heavy blow: bonus damage equal to 2x STR modifier and knockback.",
                9, 20, 0, ClickBehavior.CONSUMES_CLICK);
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
            Vec3 toEntity = entity.position().subtract(eye).normalize();
            if (look.dot(toEntity) < 0.5) continue;
            double dist = entity.distanceTo(player);
            if (dist < closest) {
                closest = dist;
                target = living;
            }
        }

        if (target != null) {
            float dmg = 6.0f + 2.0f * data.getAbilityScores().getStrMod();
            target.hurt(ModDamageTypes.abilityDamage(player.level(), player), dmg);
            target.knockback(1.5, player.getX() - target.getX(), player.getZ() - target.getZ());
        }
    }
}
