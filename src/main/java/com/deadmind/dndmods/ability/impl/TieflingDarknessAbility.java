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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public class TieflingDarknessAbility extends Ability {

    public TieflingDarknessAbility() {
        super("racial_tiefling_darkness", "Infernal Darkness",
                "Blind nearby hostile mobs and gain night vision",
                DnDClass.NONE, 1, 15, 400, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (data.getRace() != DnDRace.TIEFLING) return false;
        if (data.getDnDClass() != DnDClass.NONE && data.getCurrentResource() < getResourceCost()) return false;
        return !AbilityCooldownManager.isOnCooldown(player, this);
    }

    @Override
    public boolean meetsLevelRequirement(DnDPlayerData data) {
        return data.getRace() == DnDRace.TIEFLING;
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
        AABB area = player.getBoundingBox().inflate(6.0);

        for (Entity entity : player.level().getEntities(player, area)) {
            if (entity instanceof Mob mob) {
                mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, true));
            }
        }

        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 0, false, true));
    }
}
