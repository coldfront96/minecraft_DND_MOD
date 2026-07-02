package com.khimairacraft.ability.impl;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.ClickBehavior;
import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class ArcaneShield extends Ability {
    public ArcaneShield() {
        super("wizard_arcane_shield", "Arcane Shield",
                "Absorb the next 3 hits (absorption hearts)",
                DnDClass.WIZARD, 7, 35, 200, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        // Absorption III = 12 absorption hearts (6 full hearts), enough to absorb ~3 moderate hits
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 2, false, true));
    }
}
