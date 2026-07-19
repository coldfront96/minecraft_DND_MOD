package com.deadmind.dndmods.ability.passive;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import com.deadmind.dndmods.resource.RageSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class BarbarianPassives {

    private BarbarianPassives() {}

    public static boolean hasFastMovement(DnDPlayerData data) {
        return data.getClassLevel(DnDClass.BARBARIAN) >= 1;
    }

    public static boolean hasUncannyDodge(DnDPlayerData data) {
        return data.getClassLevel(DnDClass.BARBARIAN) >= 2;
    }

    public static boolean hasTrapSense(DnDPlayerData data) {
        return data.getClassLevel(DnDClass.BARBARIAN) >= 3;
    }

    public static int getTrapSenseBonus(DnDPlayerData data) {
        int level = data.getClassLevel(DnDClass.BARBARIAN);
        if (level < 3) return 0;
        return (level - 1) / 3; // +1 at 3, +2 at 6, +3 at 9, etc.
    }

    public static boolean hasImprovedUncannyDodge(DnDPlayerData data) {
        return data.getClassLevel(DnDClass.BARBARIAN) >= 5;
    }

    public static boolean hasDamageReduction(DnDPlayerData data) {
        return data.getClassLevel(DnDClass.BARBARIAN) >= 7;
    }

    public static int getDamageReduction(DnDPlayerData data) {
        int level = data.getClassLevel(DnDClass.BARBARIAN);
        if (level < 7) return 0;
        return 1 + (level - 7) / 3; // 1 at 7, 2 at 10, 3 at 13, 4 at 16, 5 at 19
    }

    public static boolean hasIndomitableWill(DnDPlayerData data) {
        return data.getClassLevel(DnDClass.BARBARIAN) >= 14;
    }

    public static int getIndomitableWillBonus(DnDPlayerData data) {
        if (!hasIndomitableWill(data)) return 0;
        if (!RageSystem.isRaging(null)) return 0; // caller must check raging state
        return 4;
    }

    public static int getIndomitableWillBonus(DnDPlayerData data, ServerPlayer player) {
        if (data.getClassLevel(DnDClass.BARBARIAN) < 14) return 0;
        if (!RageSystem.isRaging(player.getUUID())) return 0;
        return 4;
    }

    public static boolean hasTirelessRage(DnDPlayerData data) {
        return data.getClassLevel(DnDClass.BARBARIAN) >= 17;
    }

    public static boolean hasMightyRage(DnDPlayerData data) {
        return data.getClassLevel(DnDClass.BARBARIAN) >= 20;
    }

    public static void applyBloodlustSurge(ServerPlayer player, DnDPlayerData data) {
        int barbLevel = data.getClassLevel(DnDClass.BARBARIAN);
        if (barbLevel < 12) return;
        if (!RageSystem.isRaging(player.getUUID())) return;

        float hpPercent = (float) data.getCurrentHp() / data.getMaxHp();
        if (hpPercent > 0.25f) return;

        int healAmount = 2 + data.getAbilityScores().getConMod();
        data.setCurrentHp(Math.min(data.getMaxHp(), data.getCurrentHp() + healAmount));
    }

    public static void applyFastMovement(ServerPlayer player, DnDPlayerData data) {
        if (!hasFastMovement(data)) return;
        int barbLevel = data.getClassLevel(DnDClass.BARBARIAN);
        int amplifier = barbLevel >= 10 ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, amplifier, false, false, false));
    }
}
