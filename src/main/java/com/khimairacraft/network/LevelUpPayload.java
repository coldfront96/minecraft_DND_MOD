package com.khimairacraft.network;

import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.playerdata.AbilityScores;
import com.khimairacraft.playerdata.ClassEntry;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record LevelUpPayload(
        boolean advancePrimary,
        boolean addingNewClass,
        String newSecondaryClassName,
        int asiScore1,
        int asiScore2,
        boolean featAcknowledged
) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger("DnDMods/LevelUp");

    public static final Type<LevelUpPayload> TYPE =
            new Type<>(ModNetwork.id("level_up"));

    public static final StreamCodec<FriendlyByteBuf, LevelUpPayload> STREAM_CODEC =
            StreamCodec.of(LevelUpPayload::write, LevelUpPayload::read);

    private static LevelUpPayload read(FriendlyByteBuf buf) {
        return new LevelUpPayload(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(32),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    private static void write(FriendlyByteBuf buf, LevelUpPayload payload) {
        buf.writeBoolean(payload.advancePrimary);
        buf.writeBoolean(payload.addingNewClass);
        buf.writeUtf(payload.newSecondaryClassName, 32);
        buf.writeInt(payload.asiScore1);
        buf.writeInt(payload.asiScore2);
        buf.writeBoolean(payload.featAcknowledged);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);

            if (!data.isLevelUpAvailable()) {
                LOGGER.warn("Player {} attempted level up without it being available", player.getName().getString());
                return;
            }

            if (data.getTotalLevel() >= DnDPlayerData.MAX_LEVEL) {
                LOGGER.warn("Player {} attempted level up at max level", player.getName().getString());
                return;
            }

            if (addingNewClass) {
                if (!data.canMulticlass()) {
                    LOGGER.warn("Player {} cannot multiclass", player.getName().getString());
                    return;
                }
                if (data.getPrestigeClass() != com.khimairacraft.classes.PrestigeClass.NONE) {
                    LOGGER.warn("Player {} has a prestige class and cannot add secondary", player.getName().getString());
                    return;
                }

                DnDClass newClass;
                try {
                    newClass = DnDClass.valueOf(newSecondaryClassName);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Player {} sent invalid class name: {}", player.getName().getString(), newSecondaryClassName);
                    return;
                }

                if (newClass == DnDClass.NONE || newClass == data.getDnDClass()) {
                    LOGGER.warn("Player {} tried to multiclass into invalid class: {}", player.getName().getString(), newClass);
                    return;
                }

                int oldMaxHp = data.getMaxHp();
                data.setSecondaryClass(newClass);
                int newMaxHp = data.getMaxHp();
                data.setCurrentHp(data.getCurrentHp() + (newMaxHp - oldMaxHp));

                // Multiclassing into Fighter grants a level-1 Fighter bonus feat.
                if (newClass == DnDClass.FIGHTER) {
                    data.addFighterBonusFeatSlot();
                }

                data.clearLevelUpAvailable();

                LOGGER.info("Player {} added secondary class: {}", player.getName().getString(), newClass.getDisplayName());
            } else {
                ClassEntry advancing = advancePrimary ? data.getPrimary() : data.getSecondary();
                if (advancing == null || advancing.getDnDClass() == DnDClass.NONE) {
                    LOGGER.warn("Player {} tried to advance null/NONE class", player.getName().getString());
                    return;
                }

                int currentLevel = advancing.getLevel();
                int newLevel = currentLevel + 1;

                int oldMaxHp = data.getMaxHp();
                advancing.setLevel(newLevel);
                int newMaxHp = data.getMaxHp();
                data.setCurrentHp(data.getCurrentHp() + (newMaxHp - oldMaxHp));
                advancing.setCurrentResource(advancing.getMaxResource());

                if (newLevel % 4 == 0) {
                    if (asiScore1 < 0 || asiScore1 > 5) {
                        LOGGER.warn("Player {} sent invalid ASI score1: {}", player.getName().getString(), asiScore1);
                        advancing.setLevel(currentLevel);
                        return;
                    }
                    AbilityScores scores = data.getAbilityScores();
                    if (asiScore2 >= 0 && asiScore2 <= 5 && asiScore2 != asiScore1) {
                        applyAsiPoint(scores, asiScore1, 1);
                        applyAsiPoint(scores, asiScore2, 1);
                    } else {
                        applyAsiPoint(scores, asiScore1, 2);
                    }
                }

                // Rogue special ability picks at Rogue class levels 10/13/16/19.
                if (advancing.getDnDClass() == DnDClass.ROGUE
                        && (newLevel == 10 || newLevel == 13 || newLevel == 16 || newLevel == 19)) {
                    data.setRogueSpecialAbilitySlotsAvailable(data.getRogueSpecialAbilitySlotsAvailable() + 1);
                    LOGGER.info("Player {} earned a Rogue special ability slot at Rogue level {}",
                            player.getName().getString(), newLevel);
                }

                int totalLevel = data.getTotalLevel();
                int[] featLevels = {1, 3, 6, 9, 12, 15, 18, 20};
                for (int fl : featLevels) {
                    if (totalLevel == fl) {
                        data.addFeatSlot();
                        LOGGER.info("Player {} gained a feat slot at level {}", player.getName().getString(), totalLevel);
                        break;
                    }
                }

                // Fighter bonus feat at Fighter levels 1,2,4,6,8,10,12,14,16,18,20.
                if (advancing.getDnDClass() == DnDClass.FIGHTER && isFighterBonusFeatLevel(newLevel)) {
                    data.addFighterBonusFeatSlot();
                    LOGGER.info("Player {} gained a Fighter bonus feat at Fighter level {}",
                            player.getName().getString(), newLevel);
                }

                data.clearLevelUpAvailable();

                LOGGER.info("Player {} leveled up {} to level {}",
                        player.getName().getString(),
                        advancing.getDnDClass().getDisplayName(),
                        newLevel);
            }

            data.resetXpAfterLevelUp();

            // Fighter passive progression + Stamina recalc (covers CON ASI too).
            recalcFighterPassives(data);
            com.khimairacraft.resource.StaminaSystem.recalcMaxStamina(data);

            // Dwarven Toughness grants +1 HP per character level — rescale to
            // the new total level so FeatEffectHandler applies the right bonus.
            if (data.hasFeat("dwarven_toughness")) {
                data.setDwarvenToughnessHp(data.getTotalLevel());
            }

            // Battle Hardened (Complete Warrior) grants +2 max HP per hit die —
            // rescale to the new total level so FeatEffectHandler applies the
            // right bonus on the next tick.
            if (data.hasFeat("battle_hardened")) {
                data.setBattleHardenedHp(data.getTotalLevel() * 2);
            }

            // Improved Toughness (Complete Warrior) grants +1 HP per character
            // level via the shared toughness stacking system — gaining a level
            // adds one more stack so the MAX_HEALTH reconcile keeps pace.
            if (data.hasFeat("improved_toughness")) {
                data.setFeatStackCount("toughness", data.getFeatStackCount("toughness") + 1);
            }

            // Blade of Force (Complete Warrior) deals flat force damage equal to
            // the INT modifier — recalculate in case an ASI changed INT.
            if (data.hasFeat("blade_of_force")) {
                data.setBladeOfForceBonus(data.getAbilityScores().getIntMod());
            }

            // Holy Warrior (Complete Divine) deals flat melee damage equal to the
            // WIS modifier — recalculate in case an ASI changed WIS.
            if (data.hasFeat("holy_warrior")) {
                data.setHolyWarriorDamageBonus(data.getAbilityScores().getWisMod());
            }

            // Arcane Toughness (Complete Arcane) grants +1 HP per Wizard level —
            // rescale to the new Wizard level so FeatEffectHandler applies the
            // right bonus on the next tick.
            if (data.hasFeat("arcane_toughness")) {
                data.setArcaneToughnessHp(data.getClassLevel(DnDClass.WIZARD));
            }

            // Improved Smite (Complete Champion): bonus radiant damage equal to
            // Cleric level — rescale on level up.
            if (data.hasFeat("improved_smite")) {
                data.setSmiteDamageBonus(data.getClassLevel(DnDClass.CLERIC));
            }

            // Sacred Healing Devotion (Complete Champion): extra heal equal to the
            // WIS modifier — recalculate in case an ASI changed WIS.
            if (data.hasFeat("sacred_healing_devotion")) {
                data.setDevotionHealingBonusWis(data.getAbilityScores().getWisMod());
            }

            // Holy Resilience (Complete Champion): flat reduction equal to the WIS
            // modifier (min 1) — recalculate in case an ASI changed WIS.
            if (data.hasFeat("holy_resilience")) {
                data.setHolyResilienceReduction(Math.max(1, data.getAbilityScores().getWisMod()));
            }

            // Eldritch Lore / Mastery (Complete Mage): caster-level bonus equal to
            // the INT modifier, doubled with Eldritch Mastery — recalculate in case
            // an ASI changed INT.
            if (data.hasFeat("eldritch_mastery")) {
                data.setEldritchLoreCasterBonus(data.getAbilityScores().getIntMod() * 2);
            } else if (data.hasFeat("eldritch_lore")) {
                data.setEldritchLoreCasterBonus(data.getAbilityScores().getIntMod());
            }

            // Concentration of Might (Tome of Battle): INT-based melee bonus —
            // recalculate in case an ASI changed INT.
            if (data.hasFeat("concentration_of_might")) {
                data.setConcentrationMightBonus(data.getAbilityScores().getIntMod());
            }

            // Rogue level-derived fields (Sneak Attack dice, Evasion, Trap
            // Sense, Uncanny Dodge flags) reconcile from the new class level.
            com.deadmind.dndmods.ability.passive.RoguePassives.reconcile(data);

            data.getAbilityHotbar().validateAndClean(data);

            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
        });
    }

    private static boolean isFighterBonusFeatLevel(int level) {
        return switch (level) {
            case 1, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20 -> true;
            default -> false;
        };
    }

    /** Recomputes the Fighter passive scaling values from the current Fighter level. */
    private static void recalcFighterPassives(DnDPlayerData data) {
        int lvl = data.getClassLevel(DnDClass.FIGHTER);

        int armorAc = 0;
        if (lvl >= 15) armorAc = 4;
        else if (lvl >= 11) armorAc = 3;
        else if (lvl >= 7) armorAc = 2;
        else if (lvl >= 3) armorAc = 1;
        data.setFighterArmorAcBonus(armorAc);

        int weapon = 0;
        if (lvl >= 17) weapon = 4;
        else if (lvl >= 13) weapon = 3;
        else if (lvl >= 9) weapon = 2;
        else if (lvl >= 5) weapon = 1;
        data.setFighterWeaponAttackBonus(weapon);
        data.setFighterWeaponDamageBonus(weapon);

        data.setAchievementFlag("weapon_mastery_unlocked", lvl >= 19);
        data.setFighterDamageReduction(lvl >= 20 ? 5 : 0);
    }

    private static void applyAsiPoint(AbilityScores scores, int index, int amount) {
        switch (index) {
            case 0 -> scores.setStrength(scores.getStrength() + amount);
            case 1 -> scores.setDexterity(scores.getDexterity() + amount);
            case 2 -> scores.setConstitution(scores.getConstitution() + amount);
            case 3 -> scores.setIntelligence(scores.getIntelligence() + amount);
            case 4 -> scores.setWisdom(scores.getWisdom() + amount);
            case 5 -> scores.setCharisma(scores.getCharisma() + amount);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
