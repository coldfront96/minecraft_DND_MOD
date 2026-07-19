package com.deadmind.dndmods.ability.barbarian;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.combat.PerPlayerCombatState;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.resource.RageSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RecklessAttackAbility extends Ability {

    public RecklessAttackAbility() {
        super("barbarian_reckless_attack", "Reckless Attack",
                "Your next attack deals bonus damage equal to 2x STR modifier, but you take +20% damage for 10 seconds. Requires Rage.",
                DnDClass.BARBARIAN, 2, 0, 100, ClickBehavior.ENHANCES_ATTACK);
    }

    @Override
    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (data.getClassLevel(DnDClass.BARBARIAN) < 2) return false;
        if (!RageSystem.isRaging(player.getUUID())) {
            player.displayClientMessage(
                    Component.literal("You must be Raging to use Reckless Attack!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return false;
        }
        return !data.isOnCooldown("barbarian_reckless_attack");
    }

    @Override
    public void execute(ServerPlayer player, DnDPlayerData data) {
        data.startCooldown("barbarian_reckless_attack", getCooldownTicks());
        onUse(player, data);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        PerPlayerCombatState.CombatState state = PerPlayerCombatState.get(player.getUUID());
        state.setPendingEnhancement("barbarian_reckless_attack");
        state.setReckless(200); // 10 seconds of +20% incoming damage

        int strMod = data.getAbilityScores().getStrMod();
        player.displayClientMessage(
                Component.literal("Reckless Attack! (+" + (strMod * 2) + " damage, +20% incoming)")
                        .withStyle(s -> s.withColor(0xFF8800)), true);
    }
}
