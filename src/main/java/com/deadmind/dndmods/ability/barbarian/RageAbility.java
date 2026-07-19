package com.deadmind.dndmods.ability.barbarian;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityCooldownManager;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.resource.RageSystem;
import net.minecraft.server.level.ServerPlayer;

public class RageAbility extends Ability {

    public RageAbility() {
        super("barbarian_rage", "Rage",
                "Toggle: Enter a furious rage, gaining damage and damage reduction. Ends after duration expires or manual deactivation.",
                DnDClass.BARBARIAN, 1, 0, 0, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        return data.getClassLevel(DnDClass.BARBARIAN) >= 1;
    }

    @Override
    public void execute(ServerPlayer player, DnDPlayerData data) {
        RageSystem.activate(player);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        // handled by execute override
    }
}
