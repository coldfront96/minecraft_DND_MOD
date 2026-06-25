package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Evasion extends Ability {
    public Evasion() {
        super("rogue_evasion", "Evasion",
                "50% chance to dodge incoming damage for 5 seconds",
                DnDClass.ROGUE, 4, 20, 120, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        // Resistance II provides 40% damage reduction; combined with speed for dodge flavor
        // CombatEventHandler checks for this tag to implement 50% dodge
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, true));
        player.getPersistentData().putLong("dndmods_evasion_until", player.level().getGameTime() + 100);
    }
}
