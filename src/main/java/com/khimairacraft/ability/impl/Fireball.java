package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.AbilityDamageCalculator;
import com.khimairacraft.combat.ModDamageTypes;
import com.khimairacraft.combat.SaveType;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Fireball extends Ability {
    public Fireball() {
        super("wizard_fireball", "Fireball",
                "Launch an explosion at your target area, scales with INT",
                DnDClass.WIZARD, 1, 25, 80, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public float getBaseDamage() { return 6.0f; }

    @Override
    public AbilityScoreType getDamageScalingStat() { return AbilityScoreType.INTELLIGENCE; }

    @Override
    public SaveType getRequiredSave() { return SaveType.REFLEX; }

    @Override
    public boolean isHalfOnSave() { return true; }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 center = player.getEyePosition().add(look.scale(8.0));
        double radius = 5.0;
        AABB area = new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius));

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 3, 1.0, 1.0, 1.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 30, 2.0, 2.0, 2.0, 0.05);
        }

        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof LivingEntity living) {
                float damage = AbilityDamageCalculator.calculate(this, data, living, player);
                if (damage > 0) {
                    living.setRemainingFireTicks(100);
                    living.hurt(ModDamageTypes.abilityDamage(player.level(), player), damage);
                }
            }
        }
    }
}
