package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.AbilityDamageCalculator;
import com.khimairacraft.combat.ModDamageTypes;
import com.khimairacraft.combat.SaveType;
import com.khimairacraft.combat.SavingThrowSystem;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public class FrostNova extends Ability {
    public FrostNova() {
        super("wizard_frost_nova", "Frost Nova",
                "Freeze all mobs within 5 blocks (Slowness V + particles)",
                DnDClass.WIZARD, 4, 30, 100, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public float getBaseDamage() { return 3.0f; }

    @Override
    public AbilityScoreType getDamageScalingStat() { return AbilityScoreType.INTELLIGENCE; }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        AABB area = player.getBoundingBox().inflate(5.0);

        if (player.level() instanceof ServerLevel serverLevel) {
            for (double angle = 0; angle < Math.PI * 2; angle += 0.3) {
                double px = player.getX() + Math.cos(angle) * 5.0;
                double pz = player.getZ() + Math.sin(angle) * 5.0;
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, px, player.getY() + 0.5, pz, 3, 0.2, 0.5, 0.2, 0.01);
            }
        }

        int dc = SavingThrowSystem.getAbilityDC(this, data);

        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof LivingEntity living) {
                boolean saved = SavingThrowSystem.rollSave(living, SaveType.REFLEX, dc, player);
                float damage = AbilityDamageCalculator.calculate(this, data, living, player);
                float multiplier = SavingThrowSystem.getSaveMultiplier(saved, false);
                damage *= multiplier;

                if (damage > 0) {
                    living.hurt(ModDamageTypes.abilityDamage(player.level(), player), damage);
                }

                if (!saved) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, true));
                    living.setTicksFrozen(200);
                }
            }
        }
    }
}
