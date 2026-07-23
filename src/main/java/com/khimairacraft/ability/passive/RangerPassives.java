package com.khimairacraft.ability.passive;

import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.feat.Feat;
import com.khimairacraft.feat.FeatRegistry;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.DnDPlayerData.RangerCombatStyle;
import com.khimairacraft.ranger.FavoredEnemyType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * All level-derived Ranger passives that are trigger- or flag-based rather than
 * resource-driven. Ranger has no resource pool — Combat Style, Favored Enemy,
 * and the detection passives are automatic bonus-feat grants or passive triggers,
 * mirroring the Rogue design philosophy in {@link RoguePassives}.
 *
 * <p><b>Combat Style "treated as having a feat".</b> Per RAW a Ranger is treated
 * as having the combat-style feats without actually gaining them for prerequisite
 * purposes. We honour that by setting <em>distinct</em> {@code ranger_style_*}
 * achievement flags (never adding the real feat ids to {@code grantedFeats}, and
 * never touching the real {@code *_unlocked} feat flags, which would clobber a
 * separately-earned feat). Any current or future consumer of a combat-style
 * feat's effect should OR the real feat flag with the matching helper here
 * (e.g. {@link #isTreatedAsHavingRapidShot}). The base feat {@code *_unlocked}
 * flags currently have no gameplay consumer either, so these flags are
 * representational in exactly the same way the real feats are.
 */
public final class RangerPassives {

    private RangerPassives() {}

    // Combat Style tier unlock levels.
    public static final int TIER1_LEVEL = 2;   // Combat Style
    public static final int TIER2_LEVEL = 6;   // Improved Combat Style
    public static final int TIER3_LEVEL = 11;  // Combat Style Mastery

    // Distinct combat-style flags — never collide with the real feat *_unlocked flags.
    public static final String FLAG_RAPID_SHOT = "ranger_style_rapid_shot";
    public static final String FLAG_MANYSHOT = "ranger_style_manyshot";
    public static final String FLAG_IMPROVED_PRECISE_SHOT = "ranger_style_improved_precise_shot";
    public static final String FLAG_TWO_WEAPON_FIGHTING = "ranger_style_two_weapon_fighting";
    public static final String FLAG_IMPROVED_TWO_WEAPON = "ranger_style_improved_two_weapon_fighting";
    public static final String FLAG_GREATER_TWO_WEAPON = "ranger_style_greater_two_weapon_fighting";

    /** Wild Empathy tame bonus is capped at 50%. */
    public static final double WILD_EMPATHY_CAP = 0.50;

    // Detection passive reduction fractions.
    public static final float CAMOUFLAGE_REDUCTION = 0.30f;         // level 13
    public static final float HIDE_IN_PLAIN_SIGHT_REDUCTION = 0.50f; // level 17

    public static int getRangerLevel(DnDPlayerData data) {
        return data.getClassLevel(DnDClass.RANGER);
    }

    public static boolean isRanger(DnDPlayerData data) {
        return getRangerLevel(data) > 0;
    }

    // ------------------------------------------------------------------
    // Reconcile — self-healing level-derived flags and bonus feats
    // ------------------------------------------------------------------

    /**
     * Reconciles all non-armor level-derived Ranger flags and bonus feats from
     * the current Ranger class level. Called on level up and periodically from
     * the tick handler so existing characters self-heal without a migration.
     * Combat Style flags are armor-gated and therefore handled separately in
     * {@link #applyCombatStyleFlags} where the player (and equipment) is available.
     */
    public static void reconcile(DnDPlayerData data) {
        int level = getRangerLevel(data);
        if (level <= 0) return;

        // Track (Ranger 1) and Endurance (Ranger 3): auto-granted bonus feats.
        grantBonusFeat(data, "track", level >= 1);
        grantBonusFeat(data, "endurance", level >= 3);

        // Passive detection / movement flags.
        data.setAchievementFlag("ranger_woodland_stride", level >= 7);
        data.setAchievementFlag("ranger_swift_tracker", level >= 8);
        data.setAchievementFlag("ranger_camouflage", level >= 13);
        data.setAchievementFlag("ranger_hide_in_plain_sight", level >= 17);
    }

    /** Grants a bonus feat (and applies its effect) once the threshold is met; idempotent. */
    private static void grantBonusFeat(DnDPlayerData data, String featId, boolean shouldHave) {
        if (!shouldHave) return;
        if (data.hasFeat(featId)) return;
        Feat feat = FeatRegistry.get(featId);
        if (feat == null) return;
        data.grantFeat(featId);
        feat.applyGrant(data);
    }

    // ------------------------------------------------------------------
    // Combat Style
    // ------------------------------------------------------------------

    public static boolean hasCombatStyleTier(DnDPlayerData data, int tier) {
        int level = getRangerLevel(data);
        return switch (tier) {
            case 1 -> level >= TIER1_LEVEL;
            case 2 -> level >= TIER2_LEVEL;
            case 3 -> level >= TIER3_LEVEL;
            default -> false;
        };
    }

    /**
     * Combat Style bonuses only apply while wearing light or no armor. Reuses the
     * exact classification used for the Rogue's Evasion: light = no chestplate, or
     * a leather / chainmail chestplate; anything heavier denies it.
     */
    public static boolean isWearingLightOrNoArmor(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) return true;
        if (!(chest.getItem() instanceof ArmorItem)) return true;
        return chest.is(Items.LEATHER_CHESTPLATE) || chest.is(Items.CHAINMAIL_CHESTPLATE);
    }

    /**
     * Sets the six representational combat-style flags from the Ranger's chosen
     * style, unlocked tier, and the light/no-armor gate. Called every tick-scan
     * from the combat handler so the flags follow armor changes immediately.
     */
    public static void applyCombatStyleFlags(ServerPlayer player, DnDPlayerData data) {
        RangerCombatStyle style = data.getRangerCombatStyle();
        boolean armorOk = isWearingLightOrNoArmor(player);
        boolean archery = style == RangerCombatStyle.ARCHERY;
        boolean twoWeapon = style == RangerCombatStyle.TWO_WEAPON;

        boolean tier1 = armorOk && hasCombatStyleTier(data, 1);
        boolean tier2 = armorOk && hasCombatStyleTier(data, 2);
        boolean tier3 = armorOk && hasCombatStyleTier(data, 3);

        data.setAchievementFlag(FLAG_RAPID_SHOT, archery && tier1);
        data.setAchievementFlag(FLAG_MANYSHOT, archery && tier2);
        data.setAchievementFlag(FLAG_IMPROVED_PRECISE_SHOT, archery && tier3);

        data.setAchievementFlag(FLAG_TWO_WEAPON_FIGHTING, twoWeapon && tier1);
        data.setAchievementFlag(FLAG_IMPROVED_TWO_WEAPON, twoWeapon && tier2);
        data.setAchievementFlag(FLAG_GREATER_TWO_WEAPON, twoWeapon && tier3);
    }

    // "Treated as having" helpers: OR the real feat flag with the combat-style
    // flag so a future consumer of the feat's effect fires for either source.
    public static boolean isTreatedAsHavingRapidShot(DnDPlayerData data) {
        return data.getAchievementFlag("rapid_shot_unlocked") || data.getAchievementFlag(FLAG_RAPID_SHOT);
    }

    public static boolean isTreatedAsHavingManyshot(DnDPlayerData data) {
        return data.getAchievementFlag("manyshot_unlocked") || data.getAchievementFlag(FLAG_MANYSHOT);
    }

    public static boolean isTreatedAsHavingImprovedPreciseShot(DnDPlayerData data) {
        return data.getAchievementFlag("improved_precise_shot_unlocked")
                || data.getAchievementFlag(FLAG_IMPROVED_PRECISE_SHOT);
    }

    public static boolean isTreatedAsHavingTwoWeaponFighting(DnDPlayerData data) {
        return data.getAchievementFlag("two_weapon_fighting_unlocked")
                || data.getAchievementFlag(FLAG_TWO_WEAPON_FIGHTING);
    }

    public static boolean isTreatedAsHavingImprovedTwoWeaponFighting(DnDPlayerData data) {
        return data.getAchievementFlag("improved_two_weapon_fighting_unlocked")
                || data.getAchievementFlag(FLAG_IMPROVED_TWO_WEAPON);
    }

    public static boolean isTreatedAsHavingGreaterTwoWeaponFighting(DnDPlayerData data) {
        return data.getAchievementFlag("greater_two_weapon_fighting_unlocked")
                || data.getAchievementFlag(FLAG_GREATER_TWO_WEAPON);
    }

    // ------------------------------------------------------------------
    // Favored Enemy
    // ------------------------------------------------------------------

    /**
     * Highest matching Favored Enemy bonus against the target among the
     * categories the Ranger has selected. Overlapping categories never stack
     * (RAW): the single highest bonus is used.
     */
    public static int getFavoredEnemyBonus(DnDPlayerData data, LivingEntity target) {
        if (target == null) return 0;
        int best = 0;
        for (Map.Entry<FavoredEnemyType, Integer> entry : data.getFavoredEnemies().entrySet()) {
            if (entry.getValue() > best && entry.getKey().matches(target)) {
                best = entry.getValue();
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Wild Empathy (level 1)
    // ------------------------------------------------------------------

    /**
     * Wild Empathy tame-success bonus: (Ranger level + CHA modifier) x 0.01,
     * clamped to [0, 0.50]. Applied as an additive success chance per taming
     * interaction by the combat handler.
     */
    public static double getWildEmpathyBonus(DnDPlayerData data) {
        int level = getRangerLevel(data);
        if (level <= 0) return 0.0;
        double bonus = (level * 0.01) + (data.getAbilityScores().getChaMod() * 0.01);
        if (bonus < 0.0) return 0.0;
        return Math.min(WILD_EMPATHY_CAP, bonus);
    }

    // ------------------------------------------------------------------
    // Camouflage / Hide in Plain Sight (levels 13 / 17)
    // ------------------------------------------------------------------

    /** Detection-range reduction fraction from the Ranger's current tier (0 if none). */
    public static float getDetectionReduction(DnDPlayerData data) {
        if (data.getAchievementFlag("ranger_hide_in_plain_sight")) return HIDE_IN_PLAIN_SIGHT_REDUCTION;
        if (data.getAchievementFlag("ranger_camouflage")) return CAMOUFLAGE_REDUCTION;
        return 0.0f;
    }
}
