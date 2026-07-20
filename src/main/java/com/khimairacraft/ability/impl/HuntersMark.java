package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.PerPlayerCombatState;
import com.khimairacraft.party.FriendlyFireChecker;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

public class HuntersMark extends Ability {
    public HuntersMark() {
        super("ranger_hunters_mark", "Hunter's Mark",
                "Mark target, all damage to marked target +25%",
                DnDClass.RANGER, 3, 20, 100, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        LivingEntity closest = null;
        double closestDist = 20.0;

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(20.0))) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!(living instanceof Enemy) && !(living instanceof ServerPlayer)) continue;
            if (FriendlyFireChecker.isFriendly(player, living)) continue;
            if (!player.hasLineOfSight(living)) continue;
            Vec3 toEntity = entity.getEyePosition().subtract(eyePos).normalize();
            if (look.dot(toEntity) > 0.8) {
                double dist = entity.distanceTo(player);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = living;
                }
            }
        }

        if (closest != null) {
            closest.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, true));
            PerPlayerCombatState.get(player.getUUID()).setHuntersMark(closest.getUUID(), 200);
        }
    }
}
