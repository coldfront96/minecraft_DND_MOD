package com.deadmind.dndmods.ability.barbarian;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.ClickBehavior;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.network.SyncPlayerDataPayload;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import com.deadmind.dndmods.resource.RageSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class SecondWindAbility extends Ability {

    public SecondWindAbility() {
        super("barbarian_second_wind", "Second Wind",
                "Once per Rage, heal for 20% of max HP when below 25% HP. Requires Rage.",
                DnDClass.BARBARIAN, 18, 0, 0, ClickBehavior.CONSUMES_CLICK);
    }

    @Override
    public boolean canUse(ServerPlayer player, DnDPlayerData data) {
        if (data.getClassLevel(DnDClass.BARBARIAN) < 18) return false;

        if (!RageSystem.isRaging(player.getUUID())) {
            player.displayClientMessage(
                    Component.literal("You must be Raging to use Second Wind!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return false;
        }

        RageSystem.RageState state = RageSystem.getRageState(player.getUUID());
        if (state != null && state.isUsedSecondWind()) {
            player.displayClientMessage(
                    Component.literal("Second Wind already used this Rage!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return false;
        }

        float hpPercent = (float) data.getCurrentHp() / data.getMaxHp();
        if (hpPercent > 0.25f) {
            player.displayClientMessage(
                    Component.literal("Second Wind requires HP below 25%!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return false;
        }

        return true;
    }

    @Override
    public void execute(ServerPlayer player, DnDPlayerData data) {
        onUse(player, data);
    }

    @Override
    protected void onUse(ServerPlayer player, DnDPlayerData data) {
        RageSystem.RageState state = RageSystem.getRageState(player.getUUID());
        if (state == null) return;

        state.setUsedSecondWind(true);

        int healAmount = data.getMaxHp() / 5;
        data.setCurrentHp(Math.min(data.getMaxHp(), data.getCurrentHp() + healAmount));

        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));

        player.displayClientMessage(
                Component.literal("Second Wind! Healed " + healAmount + " HP!")
                        .withStyle(s -> s.withColor(0x55FF55).withBold(true)), true);
    }
}
