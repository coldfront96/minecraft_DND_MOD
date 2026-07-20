package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityCooldownManager;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.combat.ModDamageTypes;
import com.khimairacraft.party.FriendlyFireChecker;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.race.DnDRace;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.monster.Enemy;
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
            if (!(living instanceof Enemy) && !(living instanceof ServerPlayer)) continue;
            if (FriendlyFireChecker.isFriendly(player, living)) continue;
            if (!player.hasLineOfSight(living)) continue;

            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, true));

            float damage = baseDamage;
            if (living.getType().is(EntityTypeTags.UNDEAD)) {
                damage *= 2.0f;
            }
            living.hurt(ModDamageTypes.abilityDamage(player.level(), player), damage);
        }

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0, false, true));
    }
}
