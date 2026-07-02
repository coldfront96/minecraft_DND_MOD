package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityCooldownManager;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.race.DnDRace;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ShadarKaiPhaseStepAbility extends Ability {

    public ShadarKaiPhaseStepAbility() {
        super("racial_phase_step", "Phase Step",
                "Teleport up to 8 blocks forward, becoming briefly invisible",
                DnDClass.NONE, 1, 25, 300, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (data.getRace() != DnDRace.SHADAR_KAI) return false;
        if (data.getDnDClass() != DnDClass.NONE && data.getCurrentResource() < getResourceCost()) return false;
        return !AbilityCooldownManager.isOnCooldown(player, this);
    }

    @Override
    public boolean meetsLevelRequirement(DnDPlayerData data) {
        return data.getRace() == DnDRace.SHADAR_KAI;
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
        Vec3 start = player.position();
        Level level = player.level();

        Vec3 bestPos = start;
        for (int dist = 8; dist >= 1; dist--) {
            Vec3 candidate = start.add(look.scale(dist));
            BlockPos blockPos = BlockPos.containing(candidate);
            BlockPos above = blockPos.above();
            if (!level.getBlockState(blockPos).isSolid() && !level.getBlockState(above).isSolid()) {
                bestPos = candidate;
                break;
            }
        }

        if (bestPos != start) {
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.0f, 1.0f);
            player.teleportTo(bestPos.x, bestPos.y, bestPos.z);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.0f, 1.0f);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false));
        }
    }
}
