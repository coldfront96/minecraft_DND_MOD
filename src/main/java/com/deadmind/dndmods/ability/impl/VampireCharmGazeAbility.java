package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityCooldownManager;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.race.DnDRace;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class VampireCharmGazeAbility extends Ability {

    public VampireCharmGazeAbility() {
        super("racial_charm_gaze", "Charm Gaze",
                "Charm the nearest hostile mob, causing it to stop attacking you",
                DnDClass.NONE, 1, 30, 600, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (data.getRace() != DnDRace.VAMPIRE_SPAWN) return false;
        if (data.getDnDClass() != DnDClass.NONE && data.getCurrentResource() < getResourceCost()) return false;
        return !AbilityCooldownManager.isOnCooldown(player, this);
    }

    @Override
    public boolean meetsLevelRequirement(DnDPlayerData data) {
        return data.getRace() == DnDRace.VAMPIRE_SPAWN;
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
        AABB area = player.getBoundingBox().inflate(6.0);

        Mob nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : player.level().getEntities(player, area)) {
            if (!(entity instanceof Mob mob)) continue;
            if (mob.getTarget() != player) continue;

            Vec3 toEntity = entity.position().subtract(eyePos).normalize();
            double dot = look.normalize().dot(toEntity);
            if (dot < 0.5) continue;

            double dist = entity.position().distanceTo(eyePos);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = mob;
            }
        }

        if (nearest != null) {
            // Custom AI goals are too complex for clean implementation — use Confusion effect
            // to disorient the mob and clear its target, plus Glowing for visual feedback
            nearest.setTarget(null);
            nearest.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, true));
            nearest.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, true));
        }
    }
}
