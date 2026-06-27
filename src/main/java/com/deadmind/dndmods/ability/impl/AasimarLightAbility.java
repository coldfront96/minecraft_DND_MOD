package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityCooldownManager;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.combat.ModDamageTypes;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.race.DnDRace;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public class AasimarLightAbility extends Ability {

    public AasimarLightAbility() {
        super("racial_aasimar_light", "Radiant Burst",
                "Emit radiant light, revealing and damaging nearby foes",
                DnDClass.NONE, 1, 15, 400, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (data.getRace() != DnDRace.AASIMAR) return false;
        if (data.getDnDClass() != DnDClass.NONE && data.getCurrentResource() < getResourceCost()) return false;
        return !AbilityCooldownManager.isOnCooldown(player, this);
    }

    @Override
    public boolean meetsLevelRequirement(DnDPlayerData data) {
        return data.getRace() == DnDRace.AASIMAR;
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
        AABB area = player.getBoundingBox().inflate(8.0);
        float baseDamage = 3.0f + data.getAbilityScores().getWisMod();

        for (Entity entity : player.level().getEntities(player, area)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living instanceof ServerPlayer) continue;

            if (living instanceof Mob mob) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, true));

                float damage = baseDamage;
                if (mob.getMobType() == MobType.UNDEAD) {
                    damage *= 2.0f;
                }
                mob.hurt(ModDamageTypes.abilityDamage(player.level(), player), damage);
            }
        }

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0, false, true));
    }
}
