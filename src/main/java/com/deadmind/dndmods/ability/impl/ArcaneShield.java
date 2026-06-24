package com.deadmind.dndmods.ability.impl;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class ArcaneShield extends Ability {
    public ArcaneShield() {
        super("wizard_arcane_shield", "Arcane Shield",
                "Absorb the next 3 hits (absorption hearts)",
                DnDClass.WIZARD, 7, 35, 200);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        // Absorption III = 12 absorption hearts (6 full hearts), enough to absorb ~3 moderate hits
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 2, false, true));
    }
}
