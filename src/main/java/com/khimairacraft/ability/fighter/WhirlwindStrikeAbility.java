package com.khimairacraft.ability.fighter;

import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.combat.AbilityDamageCalculator;
import com.khimairacraft.combat.ModDamageTypes;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Level 13 — Whirlwind Strike. Hit every valid enemy within a 4-block radius for
 * full weapon damage, using the shared targeting filter.
 */
public class WhirlwindStrikeAbility extends FighterAbility {
    public WhirlwindStrikeAbility() {
        super("fighter_whirlwind_strike", "Whirlwind Strike",
                "Spin, striking all enemies within 4 blocks for full weapon damage.",
                13, 35, 200, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public float getBaseDamage() { return 6.0f; }

    @Override
    public AbilityScoreType getDamageScalingStat() { return AbilityScoreType.STRENGTH; }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(4.0))) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValidHostileTarget(player, living)) continue;
            float dmg = AbilityDamageCalculator.calculate(this, data, living, player);
            living.hurt(ModDamageTypes.abilityDamage(player.level(), player), dmg);
        }
    }
}
