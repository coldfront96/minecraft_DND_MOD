package com.khimairacraft.combat;

import com.khimairacraft.ability.Ability;
import com.khimairacraft.ability.AbilityScoreType;
import com.khimairacraft.playerdata.AbilityScores;
import com.khimairacraft.playerdata.DnDPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class AbilityDamageCalculator {

    private AbilityDamageCalculator() {}

    public static float calculate(Ability ability, DnDPlayerData data, LivingEntity target, ServerPlayer attacker) {
        float base = ability.getBaseDamage();
        if (base == 0) return 0;

        AbilityScoreType scalingStat = ability.getDamageScalingStat();
        int modifier = 0;
        if (scalingStat != null) {
            AbilityScores scores = data.getAbilityScores();
            modifier = scalingStat.getModifier(scores);
        }

        int level = data.getTotalLevel();
        float scaled = base + modifier + (level * 0.25f);

        SaveType requiredSave = ability.getRequiredSave();
        if (requiredSave != null) {
            int dc = SavingThrowSystem.getAbilityDC(ability, data);
            boolean saved = SavingThrowSystem.rollSave(target, requiredSave, dc, attacker);
            float multiplier = SavingThrowSystem.getSaveMultiplier(saved, ability.isHalfOnSave());
            scaled *= multiplier;
        }

        return scaled;
    }
}
