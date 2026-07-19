package com.deadmind.dndmods.combat;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityScoreType;
import com.deadmind.dndmods.ability.passive.BarbarianPassives;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.AbilityScores;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import com.deadmind.dndmods.race.DnDRace;
import com.deadmind.dndmods.race.RacialSaveBonus;
import com.deadmind.dndmods.resource.RageSystem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public final class SavingThrowSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger("DnDMods/Saves");
    private static final Random RANDOM = new Random();
    private static final TagKey<EntityType<?>> BOSS_TAG =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("dndmods", "boss"));

    private SavingThrowSystem() {}

    public static int getPlayerSaveBonus(DnDPlayerData data, SaveType saveType) {
        AbilityScores scores = data.getAbilityScores();
        int level = data.getTotalLevel();
        DnDRace race = data.getRace();
        int racialAll = RacialSaveBonus.getAllSavesBonus(race);

        int trapSense = BarbarianPassives.getTrapSenseBonus(data);

        return switch (saveType) {
            case FORTITUDE -> scores.getConMod() + (level / 3) + racialAll
                    + RacialSaveBonus.getFearBonus(race) + data.getFeatFortBonus();
            case REFLEX -> getReflexAbilityMod(data, scores) + (level / 4) + racialAll
                    + data.getFeatRefBonus() + trapSense;
            case WILL -> {
                int base = scores.getWisMod() + (level / 3) + racialAll
                        + RacialSaveBonus.getEnchantmentBonus(race)
                        + RacialSaveBonus.getIllusionBonus(race) + data.getFeatWillBonus();
                if (data.getClassLevel(DnDClass.BARBARIAN) >= 14) {
                    base += 4; // Indomitable Will (always active at level 14+)
                }
                yield base;
            }
        };
    }

    /**
     * Reflex saves normally key off DEX. The Insightful Reflexes feat lets a
     * player use their INT modifier instead when it is higher.
     */
    private static int getReflexAbilityMod(DnDPlayerData data, AbilityScores scores) {
        int dexMod = scores.getDexMod();
        if (data.getAchievementFlag("insightful_reflexes_unlocked")) {
            return Math.max(dexMod, scores.getIntMod());
        }
        return dexMod;
    }

    public static int getMobSaveBonus(LivingEntity entity, SaveType saveType) {
        boolean isBoss = entity.getType().is(BOSS_TAG);
        return switch (saveType) {
            case FORTITUDE -> (int) (entity.getMaxHealth() / 10) + entity.getArmorValue();
            case REFLEX -> (int) (entity.getAttributeValue(Attributes.MOVEMENT_SPEED) * 20);
            case WILL -> {
                double followRange = 16.0;
                if (entity.getAttribute(Attributes.FOLLOW_RANGE) != null) {
                    followRange = entity.getAttributeValue(Attributes.FOLLOW_RANGE);
                }
                yield (int) (followRange / 8) + (isBoss ? 10 : 0);
            }
        };
    }

    public static int getDerivedSaveBonus(LivingEntity target, SaveType saveType) {
        if (target instanceof ServerPlayer player) {
            DnDPlayerData data = PlayerDataHelper.get(player);
            return getPlayerSaveBonus(data, saveType);
        }
        return getMobSaveBonus(target, saveType);
    }

    public static int getAbilityDC(Ability ability, DnDPlayerData data) {
        int spellLevel = (int) Math.ceil(ability.getRequiredLevel() / 2.0);
        int relevantMod = getRelevantModifier(ability.getRequiredClass(), data.getAbilityScores());
        return 10 + spellLevel + relevantMod;
    }

    private static int getRelevantModifier(DnDClass dndClass, AbilityScores scores) {
        return switch (dndClass) {
            case WIZARD -> scores.getIntMod();
            case CLERIC -> scores.getWisMod();
            case RANGER -> scores.getWisMod();
            case FIGHTER -> scores.getStrMod();
            case ROGUE -> scores.getDexMod();
            case BARBARIAN -> scores.getStrMod();
            case NONE -> 0;
        };
    }

    public static boolean rollSave(LivingEntity target, SaveType saveType, int dc, @Nullable ServerPlayer attacker) {
        int saveBonus = getDerivedSaveBonus(target, saveType);
        int roll = RANDOM.nextInt(20) + 1;
        int total = roll + saveBonus;
        boolean success = total >= dc;

        if (attacker != null) {
            LOGGER.debug("[DnDMods] Save: {} rolled {} + {} = {} vs DC {} — {}",
                    target.getName().getString(), roll, saveBonus, total, dc,
                    success ? "SAVED" : "FAILED");
        }

        return success;
    }

    public static float getSaveMultiplier(boolean saveSuccess, boolean halfOnSave) {
        if (saveSuccess && halfOnSave) return 0.5f;
        if (saveSuccess) return 0.0f;
        return 1.0f;
    }
}
