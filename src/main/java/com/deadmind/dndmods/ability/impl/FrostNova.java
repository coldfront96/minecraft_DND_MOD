package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
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
                DnDClass.WIZARD, 4, 30, 100);
    }

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

        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4, false, true));
                living.setTicksFrozen(200);
                living.hurt(player.damageSources().freeze(), 4.0f);
            }
        }
    }
}
