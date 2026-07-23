package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityCooldownManager;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.ModDamageTypes;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.race.DnDRace;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DragonBreathAbility extends Ability {

    public DragonBreathAbility() {
        super("racial_dragon_breath", "Dragon Breath",
                "Breathe a cone of fire, dealing damage to all enemies ahead",
                DnDClass.NONE, 1, 20, 200, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (data.getRace() != DnDRace.DRAGONBORN) return false;
        if (data.getDnDClass() != DnDClass.NONE && data.getCurrentResource() < getResourceCost()) return false;
        return !AbilityCooldownManager.isOnCooldown(player, this);
    }

    @Override
    public boolean meetsLevelRequirement(DnDPlayerData data) {
        return data.getRace() == DnDRace.DRAGONBORN;
    }

    @Override
    public void execute(ServerPlayer player, DnDPlayerData data) {
        if (data.getDnDClass() != DnDClass.NONE) {
            data.consumeResource(getResourceCost());
        }
        AbilityCooldownManager.setCooldown(player, this, getCooldownTicks());
        onUse(player, data);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        float damage = 4.0f + (data.getTotalLevel() * 0.5f);

        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 1; i <= 8; i++) {
                Vec3 particlePos = eyePos.add(look.scale(i));
                serverLevel.sendParticles(ParticleTypes.FLAME, particlePos.x, particlePos.y, particlePos.z,
                        3, 0.3 * i * 0.2, 0.3 * i * 0.2, 0.3 * i * 0.2, 0.01);
            }
        }

        AABB area = new AABB(
                eyePos.subtract(1, 1, 1),
                eyePos.add(look.scale(8)).add(1, 1, 1)
        ).inflate(2);

        for (Entity entity : player.level().getEntities(player, area)) {
            if (!(entity instanceof LivingEntity living)) continue;

            Vec3 toEntity = entity.getEyePosition().subtract(eyePos).normalize();
            double dot = look.normalize().dot(toEntity);
            if (dot < 0.707) continue;

            double dist = entity.position().distanceTo(eyePos);
            if (dist > 8.0) continue;

            living.setRemainingFireTicks(60);
            living.hurt(ModDamageTypes.abilityDamage(player.level(), player), damage);
        }
    }
}
