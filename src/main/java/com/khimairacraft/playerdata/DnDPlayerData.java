package com.khimairacraft.playerdata;

import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.classes.PrestigeClass;
import com.khimairacraft.player.AbilityHotbar;
import com.khimairacraft.race.AbilityScoreModifiers;
import com.khimairacraft.race.DnDRace;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DnDPlayerData {
    public static final int MAX_LEVEL = 20;

    private ClassEntry primary = new ClassEntry(DnDClass.NONE, 1);
    @Nullable
    private ClassEntry secondary = null;
    private PrestigeClass prestigeClass = PrestigeClass.NONE;
    private DnDRace race = DnDRace.NONE;
    private int xp = 0;
    private final AbilityScores abilityScores = new AbilityScores();
    private int currentHp;
    private final Map<String, Boolean> achievementFlags = new HashMap<>();
    private final AbilityHotbar abilityHotbar = new AbilityHotbar();
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private boolean levelUpAvailable = false;
    private boolean humanBonusFeatAvailable = false;
    private long undyingResolveLastUsed = -1L;
    private int racialAcBonus = 0;
    private final List<String> grantedFeats = new ArrayList<>();
    /** featId to times-taken count for repeatable feats (e.g. Toughness, Extra Rage). */
    private final Map<String, Integer> repeatableFeatStacks = new HashMap<>();
    private int featSlotsAvailable = 0;
    private int featAcBonus = 0;
    private int featInitiativeBonus = 0;
    private int featShieldBonus = 0;
    private int featWillBonus = 0;
    private int featRefBonus = 0;
    private int featFortBonus = 0;
    private int extraTurningCharges = 0;
    private int devotionDamageBonus = 0;
    private int devotionHealingBonus = 0;
    private float racialXpBonus = 0.0f;
    private int dwarvenToughnessHp = 0;
    private int halflingLuckCharges = 0;
    private boolean halfOrcFerocityAvailable = true;
    private int naturalArmorBonus = 0;
    private int bladeOfForceBonus = 0;
    private int battleHardenedHp = 0;
    private int divineCasterLevelBonus = 0;
    private int turningLevelBonus = 0;
    private int domainSaveDcBonus = 0;
    private int piousDefianceCharges = 0;
    private int holyWarriorDamageBonus = 0;
    private int spellFocusBonus = 0;
    private int spellPenetrationBonus = 0;
    private int arcaneStrikeBonus = 0;
    private int schoolCasterLevelBonus = 0;
    private int schoolMasteryCharges = 0;
    private int arcaneToughnessHp = 0;
    private int innateSpellCharges = 0;
    private int magicalTrainingCharges = 0;
    private int favoredEnemyBonus = 0;
    private boolean evasionUnlocked = false;
    private int dangerSenseInitBonus = 0;
    private int luckOfHeroesCharges = 0;
    private int fatedCharges = 0;
    private int smiteCharges = 0;
    private int smiteDamageBonus = 0;
    private int devotionHealingBonusWis = 0;
    private boolean standFirmUnlocked = false;
    private int divineImpetusCharges = 0;
    private int holyResilienceReduction = 0;
    private int zealousSurgeCharges = 0;
    private int eldritchLoreCasterBonus = 0;
    private int eldritchApexCharges = 0;
    private int automaticMetamagicCharges = 0;
    private int wardingGestureCharges = 0;
    private int spellReflectionCharges = 0;
    private int reactiveSpellCharges = 0;
    private int instantMetamagicCharges = 0;
    private int deathWardCharges = 0;
    private int shadowStrBonus = 0;
    private int eldritchInsightCharges = 0;
    private int martialDamageBonus = 0;
    private int adamantineReduction = 0;
    private int concentrationMightBonus = 0;
    private boolean concentrationMightUsedThisSecond = false;
    private int perfectClarityCharges = 0;
    private int orderForgedCharges = 0;
    private int bloodyFuryBonus = 0;
    private int ironHeartSurgeCooldown = 0;
    private int rogueSneakAttackDice = 0;
    private boolean rogueEvasionUnlocked = false;
    private int rogueTrapSenseBonus = 0;
    private int rogueSpecialAbilitySlotsAvailable = 0;
    private int rogueCripplingStrikeStacks = 0;
    private long defensiveRollLastUsed = -1L;
    // --- Fighter class ability content pass ---
    private int fighterBonusFeatSlotsAvailable = 0;
    private int currentStamina = 0;
    private int maxStamina = 0;
    private int fighterArmorAcBonus = 0;
    private int fighterWeaponAttackBonus = 0;
    private int fighterWeaponDamageBonus = 0;
    private int fighterDamageReduction = 0;

    public DnDPlayerData() {
        this.currentHp = primary.getMaxHp();
    }

    // --- Backward-compatible accessors (delegate to primary) ---

    public DnDClass getDnDClass() { return primary.getDnDClass(); }
    public int getLevel() { return primary.getLevel(); }
    public int getXp() { return xp; }
    public AbilityScores getAbilityScores() { return abilityScores; }
    public int getCurrentHp() { return currentHp; }

    public int getCurrentResource() {
        if (isUnifiedPool()) {
            return primary.getCurrentResource() + (secondary != null ? secondary.getCurrentResource() : 0);
        }
        return primary.getCurrentResource();
    }

    public int getMaxResource() {
        if (isUnifiedPool()) {
            return primary.getMaxResource() + (secondary != null ? secondary.getMaxResource() : 0);
        }
        return primary.getMaxResource();
    }

    public int getMaxHp() {
        int hp = primary.getMaxHp();
        if (secondary != null) {
            hp += secondary.getMaxHp();
        }
        hp += abilityScores.getConMod() * getTotalLevel();
        hp += getFeatStackCount("toughness") * 3;
        return hp;
    }

    public void setDnDClass(DnDClass dndClass) {
        this.primary = new ClassEntry(dndClass, 1);
        this.currentHp = getMaxHp();
    }

    public void setLevel(int level) {
        primary.setLevel(Math.max(1, Math.min(MAX_LEVEL, level)));
    }

    public void setCurrentHp(int hp) {
        this.currentHp = Math.max(0, Math.min(getMaxHp(), hp));
    }

    public void setCurrentResource(int resource) {
        primary.setCurrentResource(resource);
    }

    public boolean consumeResource(int amount) {
        if (isUnifiedPool()) {
            int total = getCurrentResource();
            if (total < amount) return false;
            int remaining = amount;
            int fromPrimary = Math.min(remaining, primary.getCurrentResource());
            primary.setCurrentResource(primary.getCurrentResource() - fromPrimary);
            remaining -= fromPrimary;
            if (remaining > 0 && secondary != null) {
                secondary.setCurrentResource(secondary.getCurrentResource() - remaining);
            }
            return true;
        }
        return primary.consumeResource(amount);
    }

    public void restoreResource(int amount) {
        if (isUnifiedPool()) {
            int remaining = amount;
            int primarySpace = primary.getMaxResource() - primary.getCurrentResource();
            int toPrimary = Math.min(remaining, primarySpace);
            primary.restoreResource(toPrimary);
            remaining -= toPrimary;
            if (remaining > 0 && secondary != null) {
                secondary.restoreResource(remaining);
            }
            return;
        }
        primary.restoreResource(amount);
    }

    // --- Multiclass accessors ---

    public ClassEntry getPrimary() { return primary; }
    @Nullable public ClassEntry getSecondary() { return secondary; }
    public PrestigeClass getPrestigeClass() { return prestigeClass; }

    public int getTotalLevel() {
        return primary.getLevel() + (secondary != null ? secondary.getLevel() : 0);
    }

    /** Total levels the player has in the given class across primary and secondary entries. */
    public int getClassLevel(DnDClass dndClass) {
        int level = 0;
        if (primary.getDnDClass() == dndClass) level += primary.getLevel();
        if (secondary != null && secondary.getDnDClass() == dndClass) level += secondary.getLevel();
        return level;
    }

    // --- Fighter class ability content pass ---

    public int getFighterBonusFeatSlotsAvailable() { return fighterBonusFeatSlotsAvailable; }
    public void setFighterBonusFeatSlotsAvailable(int slots) { this.fighterBonusFeatSlotsAvailable = Math.max(0, slots); }
    public void addFighterBonusFeatSlot() { this.fighterBonusFeatSlotsAvailable++; }
    public void spendFighterBonusFeatSlot() {
        if (fighterBonusFeatSlotsAvailable > 0) fighterBonusFeatSlotsAvailable--;
    }

    public int getCurrentStamina() { return currentStamina; }
    public void setCurrentStamina(int stamina) { this.currentStamina = Math.max(0, Math.min(maxStamina, stamina)); }
    public int getMaxStamina() { return maxStamina; }
    public void setMaxStamina(int max) {
        this.maxStamina = Math.max(0, max);
        if (currentStamina > maxStamina) currentStamina = maxStamina;
    }

    /** Spends stamina if available. Returns false (no change) if there is not enough. */
    public boolean trySpendStamina(int amount) {
        if (amount <= 0) return true;
        if (currentStamina < amount) return false;
        currentStamina -= amount;
        return true;
    }

    public int getFighterArmorAcBonus() { return fighterArmorAcBonus; }
    public void setFighterArmorAcBonus(int bonus) { this.fighterArmorAcBonus = Math.max(0, bonus); }

    public int getFighterWeaponAttackBonus() { return fighterWeaponAttackBonus; }
    public void setFighterWeaponAttackBonus(int bonus) { this.fighterWeaponAttackBonus = Math.max(0, bonus); }

    public int getFighterWeaponDamageBonus() { return fighterWeaponDamageBonus; }
    public void setFighterWeaponDamageBonus(int bonus) { this.fighterWeaponDamageBonus = Math.max(0, bonus); }

    public int getFighterDamageReduction() { return fighterDamageReduction; }
    public void setFighterDamageReduction(int reduction) { this.fighterDamageReduction = Math.max(0, reduction); }

    public boolean isUnifiedPool() {
        return prestigeClass.unifiesPools() && secondary != null;
    }

    public int getUnifiedResource() {
        return primary.getCurrentResource() + (secondary != null ? secondary.getCurrentResource() : 0);
    }

    public int getUnifiedMaxResource() {
        return primary.getMaxResource() + (secondary != null ? secondary.getMaxResource() : 0);
    }

    public boolean canMulticlass() {
        return primary.getLevel() >= 3 && secondary == null && getTotalLevel() < MAX_LEVEL;
    }

    public void setSecondaryClass(DnDClass dndClass) {
        if (!canMulticlass()) return;
        if (dndClass == DnDClass.NONE || dndClass == primary.getDnDClass()) return;
        this.secondary = new ClassEntry(dndClass, 1);
    }

    public boolean canLevelUp(boolean advancePrimary) {
        if (getTotalLevel() >= MAX_LEVEL) return false;
        if (xp < getXpForNextLevel()) return false;
        ClassEntry entry = advancePrimary ? primary : secondary;
        return entry != null && entry.getDnDClass() != DnDClass.NONE;
    }

    public void levelUp(boolean advancePrimary) {
        if (!canLevelUp(advancePrimary)) return;
        xp -= getXpForNextLevel();
        ClassEntry entry = advancePrimary ? primary : secondary;
        int oldMaxHp = getMaxHp();
        entry.setLevel(entry.getLevel() + 1);
        int newMaxHp = getMaxHp();
        currentHp += (newMaxHp - oldMaxHp);
        entry.setCurrentResource(entry.getMaxResource());
    }

    public void setPrestigeClass(PrestigeClass prestige) {
        this.prestigeClass = prestige;
    }

    // --- Race ---

    public DnDRace getRace() { return race; }

    public void setRace(DnDRace newRace) {
        if (newRace == null) newRace = DnDRace.NONE;

        if (race != DnDRace.NONE) {
            AbilityScoreModifiers oldMods = race.getModifiers();
            abilityScores.setStrength(abilityScores.getStrength() - oldMods.getStrMod());
            abilityScores.setDexterity(abilityScores.getDexterity() - oldMods.getDexMod());
            abilityScores.setConstitution(abilityScores.getConstitution() - oldMods.getConMod());
            abilityScores.setIntelligence(abilityScores.getIntelligence() - oldMods.getIntMod());
            abilityScores.setWisdom(abilityScores.getWisdom() - oldMods.getWisMod());
            abilityScores.setCharisma(abilityScores.getCharisma() - oldMods.getChaMod());
        }

        this.race = newRace;
        this.racialAcBonus = (newRace == DnDRace.SKELETON_WARRIOR) ? 1 : 0;

        if (newRace != DnDRace.NONE) {
            AbilityScoreModifiers newMods = newRace.getModifiers();
            abilityScores.setStrength(abilityScores.getStrength() + newMods.getStrMod());
            abilityScores.setDexterity(abilityScores.getDexterity() + newMods.getDexMod());
            abilityScores.setConstitution(abilityScores.getConstitution() + newMods.getConMod());
            abilityScores.setIntelligence(abilityScores.getIntelligence() + newMods.getIntMod());
            abilityScores.setWisdom(abilityScores.getWisdom() + newMods.getWisMod());
            abilityScores.setCharisma(abilityScores.getCharisma() + newMods.getChaMod());
        }
    }

    // --- Human Bonus Feat ---

    public boolean isHumanBonusFeatAvailable() { return humanBonusFeatAvailable; }
    public void setHumanBonusFeatAvailable(boolean available) { this.humanBonusFeatAvailable = available; }

    // --- Undying Resolve (Revenant) ---

    public long getUndyingResolveLastUsed() { return undyingResolveLastUsed; }
    public void setUndyingResolveLastUsed(long time) { this.undyingResolveLastUsed = time; }

    // --- Racial AC Bonus (Skeleton Warrior) ---

    public int getRacialAcBonus() { return racialAcBonus; }
    public void setRacialAcBonus(int bonus) { this.racialAcBonus = bonus; }

    // --- Feats ---

    public List<String> getGrantedFeats() { return grantedFeats; }
    public boolean hasFeat(String featId) { return grantedFeats.contains(featId); }

    public void grantFeat(String featId) {
        if (!grantedFeats.contains(featId)) {
            grantedFeats.add(featId);
        }
    }

    public void revokeFeat(String featId) {
        grantedFeats.remove(featId);
    }

    public int getFeatSlotsAvailable() { return featSlotsAvailable; }

    public void setFeatSlotsAvailable(int slots) { this.featSlotsAvailable = slots; }

    public void spendFeatSlot() {
        if (featSlotsAvailable > 0) featSlotsAvailable--;
    }

    public void addFeatSlot() { featSlotsAvailable++; }

    public int getFeatAcBonus() { return featAcBonus; }
    public void setFeatAcBonus(int bonus) { this.featAcBonus = bonus; }

    public int getFeatInitiativeBonus() { return featInitiativeBonus; }
    public void setFeatInitiativeBonus(int bonus) { this.featInitiativeBonus = bonus; }

    public int getFeatShieldBonus() { return featShieldBonus; }
    public void setFeatShieldBonus(int bonus) { this.featShieldBonus = bonus; }

    public int getFeatWillBonus() { return featWillBonus; }
    public void setFeatWillBonus(int bonus) { this.featWillBonus = bonus; }

    public int getFeatRefBonus() { return featRefBonus; }
    public void setFeatRefBonus(int bonus) { this.featRefBonus = bonus; }

    public int getFeatFortBonus() { return featFortBonus; }
    public void setFeatFortBonus(int bonus) { this.featFortBonus = bonus; }

    // ------------------------------------------------------------------
    // Repeatable feat stacks — general system for feats that may be taken
    // multiple times (Toughness, Extra Rage, ...). featId -> times-taken.
    // ------------------------------------------------------------------

    /** Times a repeatable feat has been taken; 0 if never. */
    public int getFeatStackCount(String featId) {
        return repeatableFeatStacks.getOrDefault(featId, 0);
    }

    /** Adds one stack of a repeatable feat, creating the entry at 1 if absent. */
    public void incrementFeatStack(String featId) {
        repeatableFeatStacks.merge(featId, 1, Integer::sum);
    }

    /** Sets a repeatable feat's stack count (clamped at 0; removes the entry at 0). */
    public void setFeatStackCount(String featId, int count) {
        if (count <= 0) {
            repeatableFeatStacks.remove(featId);
        } else {
            repeatableFeatStacks.put(featId, count);
        }
    }

    /**
     * True if the feat is held at all — either granted normally, or a repeatable
     * feat with at least one stack. Prerequisite checks use this so a single
     * stack of a repeatable feat satisfies a "requires feat X" prerequisite.
     */
    public boolean hasFeatAtAnyStack(String featId) {
        return grantedFeats.contains(featId) || getFeatStackCount(featId) > 0;
    }

    public Map<String, Integer> getRepeatableFeatStacks() { return repeatableFeatStacks; }

    /**
     * Rage cooldown multiplier from stacked Extra Rage. Each stack multiplies the
     * cooldown by 0.75 (25% reduction, diminishing, no hard cap). Returns 1.0 with
     * no stacks. Consumed by the Barbarian Rage system when computing cooldowns.
     */
    public float getRageCooldownMultiplier() {
        int stacks = getFeatStackCount("extra_rage");
        if (stacks <= 0) return 1.0f;
        return (float) Math.pow(0.75, stacks);
    }

    public int getExtraTurningCharges() { return extraTurningCharges; }
    public void setExtraTurningCharges(int charges) { this.extraTurningCharges = Math.max(0, charges); }

    public int getDevotionDamageBonus() { return devotionDamageBonus; }
    public void setDevotionDamageBonus(int bonus) { this.devotionDamageBonus = bonus; }

    public int getDevotionHealingBonus() { return devotionHealingBonus; }
    public void setDevotionHealingBonus(int bonus) { this.devotionHealingBonus = bonus; }

    public float getRacialXpBonus() { return racialXpBonus; }
    public void setRacialXpBonus(float bonus) { this.racialXpBonus = Math.max(0.0f, bonus); }

    public int getDwarvenToughnessHp() { return dwarvenToughnessHp; }
    public void setDwarvenToughnessHp(int hp) { this.dwarvenToughnessHp = Math.max(0, hp); }

    public int getHalflingLuckCharges() { return halflingLuckCharges; }
    public void setHalflingLuckCharges(int charges) { this.halflingLuckCharges = Math.max(0, charges); }

    public boolean isHalfOrcFerocityAvailable() { return halfOrcFerocityAvailable; }
    public void setHalfOrcFerocityAvailable(boolean available) { this.halfOrcFerocityAvailable = available; }

    public int getNaturalArmorBonus() { return naturalArmorBonus; }
    public void setNaturalArmorBonus(int bonus) { this.naturalArmorBonus = bonus; }

    // Blade of Force (Complete Warrior): flat force damage equal to INT modifier,
    // recalculated whenever the feat is granted or the INT score changes.
    public int getBladeOfForceBonus() { return bladeOfForceBonus; }
    public void setBladeOfForceBonus(int bonus) { this.bladeOfForceBonus = Math.max(0, bonus); }

    // Battle Hardened (Complete Warrior): +2 max HP per hit die, scaled with total
    // level and applied to the vanilla health bar by FeatEffectHandler.
    public int getBattleHardenedHp() { return battleHardenedHp; }
    public void setBattleHardenedHp(int hp) { this.battleHardenedHp = Math.max(0, hp); }

    // Divine Spell Power (Complete Divine): caster-level bonus the Cleric ability
    // pass will toggle; the field is stored here so saves carry it forward.
    public int getDivineCasterLevelBonus() { return divineCasterLevelBonus; }
    public void setDivineCasterLevelBonus(int bonus) { this.divineCasterLevelBonus = bonus; }

    // Improved Turning (Complete Divine): effective turning-level bonus (+4).
    public int getTurningLevelBonus() { return turningLevelBonus; }
    public void setTurningLevelBonus(int bonus) { this.turningLevelBonus = bonus; }

    // Domain Focus (Complete Divine): +2 to domain spell save DCs.
    public int getDomainSaveDcBonus() { return domainSaveDcBonus; }
    public void setDomainSaveDcBonus(int bonus) { this.domainSaveDcBonus = bonus; }

    // Pious Defiance (Complete Divine): daily auto-success charge, reset on login.
    public int getPiousDefianceCharges() { return piousDefianceCharges; }
    public void setPiousDefianceCharges(int charges) { this.piousDefianceCharges = Math.max(0, charges); }

    // Holy Warrior (Complete Divine): flat melee damage equal to WIS modifier,
    // recalculated on grant and whenever WIS changes; applied in FeatEffectHandler.
    public int getHolyWarriorDamageBonus() { return holyWarriorDamageBonus; }
    public void setHolyWarriorDamageBonus(int bonus) { this.holyWarriorDamageBonus = Math.max(0, bonus); }

    // Spell Focus / Greater Spell Focus / Earth Power (Complete Arcane): stacking
    // bonus to spell save DCs.
    public int getSpellFocusBonus() { return spellFocusBonus; }
    public void setSpellFocusBonus(int bonus) { this.spellFocusBonus = bonus; }

    // Spell Penetration / Greater Spell Penetration (Complete Arcane): stacking
    // bonus on caster-level checks to overcome spell resistance.
    public int getSpellPenetrationBonus() { return spellPenetrationBonus; }
    public void setSpellPenetrationBonus(int bonus) { this.spellPenetrationBonus = bonus; }

    // Arcane Strike (Complete Arcane): bonus melee damage from a sacrificed spell
    // slot; the active toggle is deferred to the Wizard ability pass.
    public int getArcaneStrikeBonus() { return arcaneStrikeBonus; }
    public void setArcaneStrikeBonus(int bonus) { this.arcaneStrikeBonus = bonus; }

    // School Focus / Enhanced School (Complete Arcane): effective caster-level
    // bonus for the chosen school.
    public int getSchoolCasterLevelBonus() { return schoolCasterLevelBonus; }
    public void setSchoolCasterLevelBonus(int bonus) { this.schoolCasterLevelBonus = bonus; }

    // School Mastery (Complete Arcane): daily free chosen-school cast, reset on login.
    public int getSchoolMasteryCharges() { return schoolMasteryCharges; }
    public void setSchoolMasteryCharges(int charges) { this.schoolMasteryCharges = Math.max(0, charges); }

    // Arcane Toughness (Complete Arcane): +1 HP per Wizard level, applied to the
    // vanilla health bar by FeatEffectHandler and rescaled on level up.
    public int getArcaneToughnessHp() { return arcaneToughnessHp; }
    public void setArcaneToughnessHp(int hp) { this.arcaneToughnessHp = Math.max(0, hp); }

    // Innate Spell (Complete Arcane): daily slot-free casts, reset on login.
    public int getInnateSpellCharges() { return innateSpellCharges; }
    public void setInnateSpellCharges(int charges) { this.innateSpellCharges = Math.max(0, charges); }

    // Magical Training (Complete Arcane): daily cantrip-level uses, reset on login.
    public int getMagicalTrainingCharges() { return magicalTrainingCharges; }
    public void setMagicalTrainingCharges(int charges) { this.magicalTrainingCharges = Math.max(0, charges); }

    // Improved Favored Enemy (Complete Adventurer): stacking attack/damage bonus
    // against favored enemies.
    public int getFavoredEnemyBonus() { return favoredEnemyBonus; }
    public void setFavoredEnemyBonus(int bonus) { this.favoredEnemyBonus = bonus; }

    // Evasion (Complete Adventurer): negates area-effect damage on a Reflex save,
    // checked in FeatEffectHandler.
    public boolean isEvasionUnlocked() { return evasionUnlocked; }
    public void setEvasionUnlocked(boolean unlocked) { this.evasionUnlocked = unlocked; }

    // Danger Sense (Complete Adventurer) / Quick Thinking (Complete Scoundrel):
    // initiative bonus, added alongside featInitiativeBonus in the character sheet.
    public int getDangerSenseInitBonus() { return dangerSenseInitBonus; }
    public void setDangerSenseInitBonus(int bonus) { this.dangerSenseInitBonus = bonus; }

    // Luck of Heroes / Fortunate One (Complete Scoundrel): daily reroll charges,
    // reset on login (1 base, 2 with Fortunate One).
    public int getLuckOfHeroesCharges() { return luckOfHeroesCharges; }
    public void setLuckOfHeroesCharges(int charges) { this.luckOfHeroesCharges = Math.max(0, charges); }

    // Fated (Complete Scoundrel): daily auto-natural-20 charge, reset on login.
    public int getFatedCharges() { return fatedCharges; }
    public void setFatedCharges(int charges) { this.fatedCharges = Math.max(0, charges); }

    // Smite Evil line (Complete Champion): daily smite charges (1/2/3 as the
    // line is advanced), reset on login.
    public int getSmiteCharges() { return smiteCharges; }
    public void setSmiteCharges(int charges) { this.smiteCharges = Math.max(0, charges); }

    // Improved Smite (Complete Champion): bonus radiant smite damage equal to
    // Cleric level, rescaled on level up.
    public int getSmiteDamageBonus() { return smiteDamageBonus; }
    public void setSmiteDamageBonus(int bonus) { this.smiteDamageBonus = Math.max(0, bonus); }

    // Sacred Healing Devotion (Complete Champion): extra heal equal to WIS
    // modifier, recalculated on WIS change.
    public int getDevotionHealingBonusWis() { return devotionHealingBonusWis; }
    public void setDevotionHealingBonusWis(int bonus) { this.devotionHealingBonusWis = Math.max(0, bonus); }

    // Stand Firm (Complete Champion): immunity to knockback, checked in
    // FeatEffectHandler's LivingKnockBackEvent hook.
    public boolean isStandFirmUnlocked() { return standFirmUnlocked; }
    public void setStandFirmUnlocked(boolean unlocked) { this.standFirmUnlocked = unlocked; }

    // Divine Impetus (Complete Champion): daily burst-of-speed charge, reset on login.
    public int getDivineImpetusCharges() { return divineImpetusCharges; }
    public void setDivineImpetusCharges(int charges) { this.divineImpetusCharges = Math.max(0, charges); }

    // Holy Resilience (Complete Champion): flat damage reduction equal to WIS
    // modifier (min 1), applied in FeatEffectHandler; recalculated on WIS change.
    public int getHolyResilienceReduction() { return holyResilienceReduction; }
    public void setHolyResilienceReduction(int reduction) { this.holyResilienceReduction = Math.max(0, reduction); }

    // Zealous Surge (Complete Champion): daily divine revival charge, reset on login.
    public int getZealousSurgeCharges() { return zealousSurgeCharges; }
    public void setZealousSurgeCharges(int charges) { this.zealousSurgeCharges = Math.max(0, charges); }

    // Eldritch Lore / Mastery (Complete Mage): caster-level bonus equal to the
    // INT modifier (x2 with Mastery), recalculated on INT change.
    public int getEldritchLoreCasterBonus() { return eldritchLoreCasterBonus; }
    public void setEldritchLoreCasterBonus(int bonus) { this.eldritchLoreCasterBonus = Math.max(0, bonus); }

    // Eldritch Apex (Complete Mage): daily max-caster-level cast, reset on login.
    public int getEldritchApexCharges() { return eldritchApexCharges; }
    public void setEldritchApexCharges(int charges) { this.eldritchApexCharges = Math.max(0, charges); }

    // Automatic Metamagic (Complete Mage): daily free-metamagic charge, reset on login.
    public int getAutomaticMetamagicCharges() { return automaticMetamagicCharges; }
    public void setAutomaticMetamagicCharges(int charges) { this.automaticMetamagicCharges = Math.max(0, charges); }

    // Warding Gesture / Greater Warding (Complete Mage): daily ward charges,
    // reset on login (1 base, 2 with Greater Warding).
    public int getWardingGestureCharges() { return wardingGestureCharges; }
    public void setWardingGestureCharges(int charges) { this.wardingGestureCharges = Math.max(0, charges); }

    // Spell Reflection (Complete Mage): daily reflect charge, reset on login.
    public int getSpellReflectionCharges() { return spellReflectionCharges; }
    public void setSpellReflectionCharges(int charges) { this.spellReflectionCharges = Math.max(0, charges); }

    // Reactive Spell (Complete Mage): daily immediate-action cast, reset on login.
    public int getReactiveSpellCharges() { return reactiveSpellCharges; }
    public void setReactiveSpellCharges(int charges) { this.reactiveSpellCharges = Math.max(0, charges); }

    // Instant Metamagic (Complete Mage): three daily free-action metamagic casts,
    // reset on login.
    public int getInstantMetamagicCharges() { return instantMetamagicCharges; }
    public void setInstantMetamagicCharges(int charges) { this.instantMetamagicCharges = Math.max(0, charges); }

    // Death Ward (Heroes of Horror): daily death-immunity charge, reset on login.
    public int getDeathWardCharges() { return deathWardCharges; }
    public void setDeathWardCharges(int charges) { this.deathWardCharges = Math.max(0, charges); }

    // Embrace the Dark (Tome of Magic): dynamic +STR melee bonus while in full
    // darkness; reconciled each tick by FeatEffectHandler.
    public int getShadowStrBonus() { return shadowStrBonus; }
    public void setShadowStrBonus(int bonus) { this.shadowStrBonus = Math.max(0, bonus); }

    // Eldritch Insight (Tome of Magic): daily spell-save reroll charge, reset on login.
    public int getEldritchInsightCharges() { return eldritchInsightCharges; }
    public void setEldritchInsightCharges(int charges) { this.eldritchInsightCharges = Math.max(0, charges); }

    // Shared martial melee bonus (Tome of Battle): summed flat melee damage from
    // iron_will_warrior, mountain_hammer, elder_mountain_hammer, and martial_study.
    public int getMartialDamageBonus() { return martialDamageBonus; }
    public void setMartialDamageBonus(int bonus) { this.martialDamageBonus = Math.max(0, bonus); }

    // Adamantine Body (Tome of Battle): flat physical-melee damage reduction.
    public int getAdamantineReduction() { return adamantineReduction; }
    public void setAdamantineReduction(int reduction) { this.adamantineReduction = Math.max(0, reduction); }

    // Concentration of Might (Tome of Battle): INT-based bonus applied to one
    // melee hit per second; recalculated on INT change.
    public int getConcentrationMightBonus() { return concentrationMightBonus; }
    public void setConcentrationMightBonus(int bonus) { this.concentrationMightBonus = Math.max(0, bonus); }

    public boolean isConcentrationMightUsedThisSecond() { return concentrationMightUsedThisSecond; }
    public void setConcentrationMightUsedThisSecond(boolean used) { this.concentrationMightUsedThisSecond = used; }

    // Perfect Clarity (Tome of Battle): daily INT-attack reroll charge, reset on login.
    public int getPerfectClarityCharges() { return perfectClarityCharges; }
    public void setPerfectClarityCharges(int charges) { this.perfectClarityCharges = Math.max(0, charges); }

    // Order Forged from Chaos (Tome of Battle): daily party-buff charge, reset on login.
    public int getOrderForgedCharges() { return orderForgedCharges; }
    public void setOrderForgedCharges(int charges) { this.orderForgedCharges = Math.max(0, charges); }

    // Blood of the Martyr (Tome of Battle): dynamic melee fury bonus below 50% HP;
    // reconciled each tick by FeatEffectHandler.
    public int getBloodyFuryBonus() { return bloodyFuryBonus; }
    public void setBloodyFuryBonus(int bonus) { this.bloodyFuryBonus = Math.max(0, bonus); }

    // Iron Heart Surge (Tome of Battle): cooldown ticks for the deferred active use.
    public int getIronHeartSurgeCooldown() { return ironHeartSurgeCooldown; }
    public void setIronHeartSurgeCooldown(int ticks) { this.ironHeartSurgeCooldown = Math.max(0, ticks); }

    // --- Rogue class progression ---

    // Sneak Attack dice (1d6 each): 1 at Rogue 1, +1 every odd level to 10 at 19.
    public int getRogueSneakAttackDice() { return rogueSneakAttackDice; }
    public void setRogueSneakAttackDice(int dice) { this.rogueSneakAttackDice = Math.max(0, dice); }

    // Evasion (Rogue 2): Reflex save negates area damage in light/no armor.
    public boolean isRogueEvasionUnlocked() { return rogueEvasionUnlocked; }
    public void setRogueEvasionUnlocked(boolean unlocked) { this.rogueEvasionUnlocked = unlocked; }

    // Trap Sense (Rogue 3): +1 per 3 Rogue levels to Reflex saves, stacks
    // additively with the Barbarian version (separate field).
    public int getRogueTrapSenseBonus() { return rogueTrapSenseBonus; }
    public void setRogueTrapSenseBonus(int bonus) { this.rogueTrapSenseBonus = Math.max(0, bonus); }

    // Special ability picks earned at Rogue levels 10/13/16/19.
    public int getRogueSpecialAbilitySlotsAvailable() { return rogueSpecialAbilitySlotsAvailable; }
    public void setRogueSpecialAbilitySlotsAvailable(int slots) { this.rogueSpecialAbilitySlotsAvailable = Math.max(0, slots); }

    // Crippling Strike stacks (the one repeatable special ability).
    public int getRogueCripplingStrikeStacks() { return rogueCripplingStrikeStacks; }
    public void setRogueCripplingStrikeStacks(int stacks) { this.rogueCripplingStrikeStacks = Math.max(0, stacks); }

    // Defensive Roll: once per real-world day, epoch millis of last use.
    public long getDefensiveRollLastUsed() { return defensiveRollLastUsed; }
    public void setDefensiveRollLastUsed(long time) { this.defensiveRollLastUsed = time; }

    // --- Achievement flags ---

    public void setAchievementFlag(String key, boolean value) {
        achievementFlags.put(key, value);
    }

    public boolean getAchievementFlag(String key) {
        return achievementFlags.getOrDefault(key, false);
    }

    public Map<String, Boolean> getAchievementFlags() {
        return achievementFlags;
    }

    // --- Ability Hotbar ---

    public AbilityHotbar getAbilityHotbar() { return abilityHotbar; }

    // --- Cooldowns ---

    public boolean isOnCooldown(String abilityId) {
        Integer remaining = cooldowns.get(abilityId);
        return remaining != null && remaining > 0;
    }

    public int getCooldownRemaining(String abilityId) {
        return cooldowns.getOrDefault(abilityId, 0);
    }

    public void startCooldown(String abilityId, int ticks) {
        cooldowns.put(abilityId, ticks);
    }

    public void tickCooldowns() {
        Iterator<Map.Entry<String, Integer>> it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    // --- Level Up ---

    public boolean isLevelUpAvailable() {
        return levelUpAvailable;
    }

    public void clearLevelUpAvailable() {
        this.levelUpAvailable = false;
    }

    // --- XP ---

    public void addXp(int amount) {
        if (levelUpAvailable) return;
        if (getTotalLevel() >= MAX_LEVEL) return;

        this.xp += amount;

        if (this.xp >= getXpForNextLevel()) {
            this.xp = getXpForNextLevel();
            this.levelUpAvailable = true;
        }
    }

    public int getXpForNextLevel() {
        int totalLevel = getTotalLevel();
        return totalLevel * 100 + (totalLevel * totalLevel * 50);
    }

    public void resetXpAfterLevelUp() {
        this.xp = 0;
    }

    // --- Serialization ---

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Primary", primary.save());
        if (secondary != null) {
            tag.put("Secondary", secondary.save());
        }
        tag.putString("PrestigeClass", prestigeClass.name());
        tag.putString("Race", race.name());
        tag.putInt("XP", xp);
        tag.put("AbilityScores", abilityScores.save());
        tag.putInt("CurrentHP", currentHp);

        CompoundTag flags = new CompoundTag();
        achievementFlags.forEach(flags::putBoolean);
        tag.put("Achievements", flags);

        tag.put("Hotbar", abilityHotbar.save());

        CompoundTag cdTag = new CompoundTag();
        cooldowns.forEach(cdTag::putInt);
        tag.put("Cooldowns", cdTag);

        tag.putBoolean("LevelUpAvailable", levelUpAvailable);
        tag.putBoolean("HumanBonusFeatAvailable", humanBonusFeatAvailable);
        tag.putLong("UndyingResolveLastUsed", undyingResolveLastUsed);
        tag.putInt("RacialAcBonus", racialAcBonus);

        CompoundTag featTag = new CompoundTag();
        featTag.putInt("SlotsAvailable", featSlotsAvailable);
        featTag.putInt("AcBonus", featAcBonus);
        featTag.putInt("InitiativeBonus", featInitiativeBonus);
        featTag.putInt("ShieldBonus", featShieldBonus);
        featTag.putInt("WillBonus", featWillBonus);
        featTag.putInt("RefBonus", featRefBonus);
        featTag.putInt("FortBonus", featFortBonus);
        featTag.putInt("ExtraTurningCharges", extraTurningCharges);
        featTag.putInt("DevotionDamageBonus", devotionDamageBonus);
        featTag.putInt("DevotionHealingBonus", devotionHealingBonus);
        featTag.putFloat("RacialXpBonus", racialXpBonus);
        featTag.putInt("DwarvenToughnessHp", dwarvenToughnessHp);
        featTag.putInt("HalflingLuckCharges", halflingLuckCharges);
        featTag.putBoolean("HalfOrcFerocityAvailable", halfOrcFerocityAvailable);
        featTag.putInt("NaturalArmorBonus", naturalArmorBonus);
        featTag.putInt("BladeOfForceBonus", bladeOfForceBonus);
        featTag.putInt("BattleHardenedHp", battleHardenedHp);
        featTag.putInt("DivineCasterLevelBonus", divineCasterLevelBonus);
        featTag.putInt("TurningLevelBonus", turningLevelBonus);
        featTag.putInt("DomainSaveDcBonus", domainSaveDcBonus);
        featTag.putInt("PiousDefianceCharges", piousDefianceCharges);
        featTag.putInt("HolyWarriorDamageBonus", holyWarriorDamageBonus);
        featTag.putInt("SpellFocusBonus", spellFocusBonus);
        featTag.putInt("SpellPenetrationBonus", spellPenetrationBonus);
        featTag.putInt("ArcaneStrikeBonus", arcaneStrikeBonus);
        featTag.putInt("SchoolCasterLevelBonus", schoolCasterLevelBonus);
        featTag.putInt("SchoolMasteryCharges", schoolMasteryCharges);
        featTag.putInt("ArcaneToughnessHp", arcaneToughnessHp);
        featTag.putInt("InnateSpellCharges", innateSpellCharges);
        featTag.putInt("MagicalTrainingCharges", magicalTrainingCharges);
        featTag.putInt("FavoredEnemyBonus", favoredEnemyBonus);
        featTag.putBoolean("EvasionUnlocked", evasionUnlocked);
        featTag.putInt("DangerSenseInitBonus", dangerSenseInitBonus);
        featTag.putInt("LuckOfHeroesCharges", luckOfHeroesCharges);
        featTag.putInt("FatedCharges", fatedCharges);
        featTag.putInt("SmiteCharges", smiteCharges);
        featTag.putInt("SmiteDamageBonus", smiteDamageBonus);
        featTag.putInt("DevotionHealingBonusWis", devotionHealingBonusWis);
        featTag.putBoolean("StandFirmUnlocked", standFirmUnlocked);
        featTag.putInt("DivineImpetusCharges", divineImpetusCharges);
        featTag.putInt("HolyResilienceReduction", holyResilienceReduction);
        featTag.putInt("ZealousSurgeCharges", zealousSurgeCharges);
        featTag.putInt("EldritchLoreCasterBonus", eldritchLoreCasterBonus);
        featTag.putInt("EldritchApexCharges", eldritchApexCharges);
        featTag.putInt("AutomaticMetamagicCharges", automaticMetamagicCharges);
        featTag.putInt("WardingGestureCharges", wardingGestureCharges);
        featTag.putInt("SpellReflectionCharges", spellReflectionCharges);
        featTag.putInt("ReactiveSpellCharges", reactiveSpellCharges);
        featTag.putInt("InstantMetamagicCharges", instantMetamagicCharges);
        featTag.putInt("DeathWardCharges", deathWardCharges);
        featTag.putInt("ShadowStrBonus", shadowStrBonus);
        featTag.putInt("EldritchInsightCharges", eldritchInsightCharges);
        featTag.putInt("MartialDamageBonus", martialDamageBonus);
        featTag.putInt("AdamantineReduction", adamantineReduction);
        featTag.putInt("ConcentrationMightBonus", concentrationMightBonus);
        featTag.putBoolean("ConcentrationMightUsedThisSecond", concentrationMightUsedThisSecond);
        featTag.putInt("PerfectClarityCharges", perfectClarityCharges);
        featTag.putInt("OrderForgedCharges", orderForgedCharges);
        featTag.putInt("BloodyFuryBonus", bloodyFuryBonus);
        featTag.putInt("IronHeartSurgeCooldown", ironHeartSurgeCooldown);
        featTag.putInt("FighterBonusFeatSlots", fighterBonusFeatSlotsAvailable);
        featTag.putInt("CurrentStamina", currentStamina);
        featTag.putInt("MaxStamina", maxStamina);
        featTag.putInt("FighterArmorAcBonus", fighterArmorAcBonus);
        featTag.putInt("FighterWeaponAttackBonus", fighterWeaponAttackBonus);
        featTag.putInt("FighterWeaponDamageBonus", fighterWeaponDamageBonus);
        featTag.putInt("FighterDamageReduction", fighterDamageReduction);
        net.minecraft.nbt.ListTag featList = new net.minecraft.nbt.ListTag();
        for (String featId : grantedFeats) {
            featList.add(net.minecraft.nbt.StringTag.valueOf(featId));
        }
        featTag.put("Granted", featList);

        CompoundTag stacksTag = new CompoundTag();
        repeatableFeatStacks.forEach(stacksTag::putInt);
        featTag.put("RepeatableFeatStacks", stacksTag);

        tag.put("Feats", featTag);

        CompoundTag rogueTag = new CompoundTag();
        rogueTag.putInt("SneakAttackDice", rogueSneakAttackDice);
        rogueTag.putBoolean("EvasionUnlocked", rogueEvasionUnlocked);
        rogueTag.putInt("TrapSenseBonus", rogueTrapSenseBonus);
        rogueTag.putInt("SpecialSlots", rogueSpecialAbilitySlotsAvailable);
        rogueTag.putInt("CripplingStacks", rogueCripplingStrikeStacks);
        rogueTag.putLong("DefensiveRollLastUsed", defensiveRollLastUsed);
        tag.put("Rogue", rogueTag);

        return tag;
    }

    public void load(CompoundTag tag) {
        // Support old format (single Class/Level/CurrentResource)
        if (tag.contains("Class")) {
            DnDClass cls = DnDClass.valueOf(tag.getString("Class"));
            int level = tag.getInt("Level");
            primary = new ClassEntry(cls, level);
            primary.setCurrentResource(tag.getInt("CurrentResource"));
            secondary = null;
            prestigeClass = PrestigeClass.NONE;
        } else {
            primary = new ClassEntry();
            primary.load(tag.getCompound("Primary"));
            if (tag.contains("Secondary")) {
                secondary = new ClassEntry();
                secondary.load(tag.getCompound("Secondary"));
            } else {
                secondary = null;
            }
            try {
                prestigeClass = PrestigeClass.valueOf(tag.getString("PrestigeClass"));
            } catch (IllegalArgumentException e) {
                prestigeClass = PrestigeClass.NONE;
            }
        }

        if (tag.contains("Race")) {
            try {
                race = DnDRace.valueOf(tag.getString("Race"));
            } catch (IllegalArgumentException e) {
                race = DnDRace.NONE;
            }
        } else {
            race = DnDRace.NONE;
        }

        xp = tag.getInt("XP");
        abilityScores.load(tag.getCompound("AbilityScores"));
        currentHp = tag.getInt("CurrentHP");

        achievementFlags.clear();
        if (tag.contains("Achievements")) {
            CompoundTag flags = tag.getCompound("Achievements");
            for (String key : flags.getAllKeys()) {
                achievementFlags.put(key, flags.getBoolean(key));
            }
        }

        if (tag.contains("Hotbar")) {
            abilityHotbar.load(tag.getCompound("Hotbar"));
        }

        cooldowns.clear();
        if (tag.contains("Cooldowns")) {
            CompoundTag cdTag = tag.getCompound("Cooldowns");
            for (String key : cdTag.getAllKeys()) {
                cooldowns.put(key, cdTag.getInt(key));
            }
        }

        levelUpAvailable = tag.getBoolean("LevelUpAvailable");
        humanBonusFeatAvailable = tag.getBoolean("HumanBonusFeatAvailable");
        undyingResolveLastUsed = tag.contains("UndyingResolveLastUsed") ? tag.getLong("UndyingResolveLastUsed") : -1L;
        racialAcBonus = tag.getInt("RacialAcBonus");

        grantedFeats.clear();
        repeatableFeatStacks.clear();
        if (tag.contains("Feats")) {
            CompoundTag featTag = tag.getCompound("Feats");
            featSlotsAvailable = featTag.getInt("SlotsAvailable");
            featAcBonus = featTag.getInt("AcBonus");
            featInitiativeBonus = featTag.getInt("InitiativeBonus");
            featShieldBonus = featTag.getInt("ShieldBonus");
            featWillBonus = featTag.getInt("WillBonus");
            featRefBonus = featTag.getInt("RefBonus");
            featFortBonus = featTag.getInt("FortBonus");
            extraTurningCharges = featTag.getInt("ExtraTurningCharges");
            devotionDamageBonus = featTag.getInt("DevotionDamageBonus");
            devotionHealingBonus = featTag.getInt("DevotionHealingBonus");
            racialXpBonus = featTag.getFloat("RacialXpBonus");
            dwarvenToughnessHp = featTag.getInt("DwarvenToughnessHp");
            halflingLuckCharges = featTag.getInt("HalflingLuckCharges");
            halfOrcFerocityAvailable = !featTag.contains("HalfOrcFerocityAvailable")
                    || featTag.getBoolean("HalfOrcFerocityAvailable");
            naturalArmorBonus = featTag.getInt("NaturalArmorBonus");
            bladeOfForceBonus = featTag.getInt("BladeOfForceBonus");
            battleHardenedHp = featTag.getInt("BattleHardenedHp");
            divineCasterLevelBonus = featTag.getInt("DivineCasterLevelBonus");
            turningLevelBonus = featTag.getInt("TurningLevelBonus");
            domainSaveDcBonus = featTag.getInt("DomainSaveDcBonus");
            piousDefianceCharges = featTag.getInt("PiousDefianceCharges");
            holyWarriorDamageBonus = featTag.getInt("HolyWarriorDamageBonus");
            spellFocusBonus = featTag.getInt("SpellFocusBonus");
            spellPenetrationBonus = featTag.getInt("SpellPenetrationBonus");
            arcaneStrikeBonus = featTag.getInt("ArcaneStrikeBonus");
            schoolCasterLevelBonus = featTag.getInt("SchoolCasterLevelBonus");
            schoolMasteryCharges = featTag.getInt("SchoolMasteryCharges");
            arcaneToughnessHp = featTag.getInt("ArcaneToughnessHp");
            innateSpellCharges = featTag.getInt("InnateSpellCharges");
            magicalTrainingCharges = featTag.getInt("MagicalTrainingCharges");
            favoredEnemyBonus = featTag.getInt("FavoredEnemyBonus");
            evasionUnlocked = featTag.getBoolean("EvasionUnlocked");
            dangerSenseInitBonus = featTag.getInt("DangerSenseInitBonus");
            luckOfHeroesCharges = featTag.getInt("LuckOfHeroesCharges");
            fatedCharges = featTag.getInt("FatedCharges");
            smiteCharges = featTag.getInt("SmiteCharges");
            smiteDamageBonus = featTag.getInt("SmiteDamageBonus");
            devotionHealingBonusWis = featTag.getInt("DevotionHealingBonusWis");
            standFirmUnlocked = featTag.getBoolean("StandFirmUnlocked");
            divineImpetusCharges = featTag.getInt("DivineImpetusCharges");
            holyResilienceReduction = featTag.getInt("HolyResilienceReduction");
            zealousSurgeCharges = featTag.getInt("ZealousSurgeCharges");
            eldritchLoreCasterBonus = featTag.getInt("EldritchLoreCasterBonus");
            eldritchApexCharges = featTag.getInt("EldritchApexCharges");
            automaticMetamagicCharges = featTag.getInt("AutomaticMetamagicCharges");
            wardingGestureCharges = featTag.getInt("WardingGestureCharges");
            spellReflectionCharges = featTag.getInt("SpellReflectionCharges");
            reactiveSpellCharges = featTag.getInt("ReactiveSpellCharges");
            instantMetamagicCharges = featTag.getInt("InstantMetamagicCharges");
            deathWardCharges = featTag.getInt("DeathWardCharges");
            shadowStrBonus = featTag.getInt("ShadowStrBonus");
            eldritchInsightCharges = featTag.getInt("EldritchInsightCharges");
            martialDamageBonus = featTag.getInt("MartialDamageBonus");
            adamantineReduction = featTag.getInt("AdamantineReduction");
            concentrationMightBonus = featTag.getInt("ConcentrationMightBonus");
            concentrationMightUsedThisSecond = featTag.getBoolean("ConcentrationMightUsedThisSecond");
            perfectClarityCharges = featTag.getInt("PerfectClarityCharges");
            orderForgedCharges = featTag.getInt("OrderForgedCharges");
            bloodyFuryBonus = featTag.getInt("BloodyFuryBonus");
            ironHeartSurgeCooldown = featTag.getInt("IronHeartSurgeCooldown");
            fighterBonusFeatSlotsAvailable = featTag.getInt("FighterBonusFeatSlots");
            currentStamina = featTag.getInt("CurrentStamina");
            maxStamina = featTag.getInt("MaxStamina");
            fighterArmorAcBonus = featTag.getInt("FighterArmorAcBonus");
            fighterWeaponAttackBonus = featTag.getInt("FighterWeaponAttackBonus");
            fighterWeaponDamageBonus = featTag.getInt("FighterWeaponDamageBonus");
            fighterDamageReduction = featTag.getInt("FighterDamageReduction");
            net.minecraft.nbt.ListTag featList = featTag.getList("Granted", net.minecraft.nbt.Tag.TAG_STRING);
            for (int i = 0; i < featList.size(); i++) {
                grantedFeats.add(featList.getString(i));
            }

            if (featTag.contains("RepeatableFeatStacks")) {
                CompoundTag stacksTag = featTag.getCompound("RepeatableFeatStacks");
                for (String key : stacksTag.getAllKeys()) {
                    repeatableFeatStacks.put(key, stacksTag.getInt(key));
                }
            }

            // One-time migration: Toughness used to stack via a dedicated
            // ToughnessCount field. Fold any old value into the general stack map
            // (only if this save predates the map, i.e. has no "toughness" entry).
            // Brand-new saves never had ToughnessCount, so the contains() guard
            // skips them safely.
            if (featTag.contains("ToughnessCount") && !repeatableFeatStacks.containsKey("toughness")) {
                int legacy = featTag.getInt("ToughnessCount");
                if (legacy > 0) {
                    repeatableFeatStacks.put("toughness", legacy);
                }
            }

            if (tag.contains("Rogue")) {
                CompoundTag rogueTag = tag.getCompound("Rogue");
                rogueSneakAttackDice = rogueTag.getInt("SneakAttackDice");
                rogueEvasionUnlocked = rogueTag.getBoolean("EvasionUnlocked");
                rogueTrapSenseBonus = rogueTag.getInt("TrapSenseBonus");
                rogueSpecialAbilitySlotsAvailable = rogueTag.getInt("SpecialSlots");
                rogueCripplingStrikeStacks = rogueTag.getInt("CripplingStacks");
                defensiveRollLastUsed = rogueTag.contains("DefensiveRollLastUsed")
                        ? rogueTag.getLong("DefensiveRollLastUsed") : -1L;
            } else {
                rogueSneakAttackDice = 0;
                rogueEvasionUnlocked = false;
                rogueTrapSenseBonus = 0;
                rogueSpecialAbilitySlotsAvailable = 0;
                rogueCripplingStrikeStacks = 0;
                defensiveRollLastUsed = -1L;
            }
        } else {
            featSlotsAvailable = 0;
            featAcBonus = 0;
            featInitiativeBonus = 0;
            featShieldBonus = 0;
            featWillBonus = 0;
            featRefBonus = 0;
            featFortBonus = 0;
            extraTurningCharges = 0;
            devotionDamageBonus = 0;
            devotionHealingBonus = 0;
            racialXpBonus = 0.0f;
            dwarvenToughnessHp = 0;
            halflingLuckCharges = 0;
            halfOrcFerocityAvailable = true;
            naturalArmorBonus = 0;
            bladeOfForceBonus = 0;
            battleHardenedHp = 0;
            divineCasterLevelBonus = 0;
            turningLevelBonus = 0;
            domainSaveDcBonus = 0;
            piousDefianceCharges = 0;
            holyWarriorDamageBonus = 0;
            spellFocusBonus = 0;
            spellPenetrationBonus = 0;
            arcaneStrikeBonus = 0;
            schoolCasterLevelBonus = 0;
            schoolMasteryCharges = 0;
            arcaneToughnessHp = 0;
            innateSpellCharges = 0;
            magicalTrainingCharges = 0;
            favoredEnemyBonus = 0;
            evasionUnlocked = false;
            dangerSenseInitBonus = 0;
            luckOfHeroesCharges = 0;
            fatedCharges = 0;
            smiteCharges = 0;
            smiteDamageBonus = 0;
            devotionHealingBonusWis = 0;
            standFirmUnlocked = false;
            divineImpetusCharges = 0;
            holyResilienceReduction = 0;
            zealousSurgeCharges = 0;
            eldritchLoreCasterBonus = 0;
            eldritchApexCharges = 0;
            automaticMetamagicCharges = 0;
            wardingGestureCharges = 0;
            spellReflectionCharges = 0;
            reactiveSpellCharges = 0;
            instantMetamagicCharges = 0;
            deathWardCharges = 0;
            shadowStrBonus = 0;
            eldritchInsightCharges = 0;
            martialDamageBonus = 0;
            adamantineReduction = 0;
            concentrationMightBonus = 0;
            concentrationMightUsedThisSecond = false;
            perfectClarityCharges = 0;
            orderForgedCharges = 0;
            bloodyFuryBonus = 0;
            ironHeartSurgeCooldown = 0;
            rogueSneakAttackDice = 0;
            rogueEvasionUnlocked = false;
            rogueTrapSenseBonus = 0;
            rogueSpecialAbilitySlotsAvailable = 0;
            rogueCripplingStrikeStacks = 0;
            defensiveRollLastUsed = -1L;
            fighterBonusFeatSlotsAvailable = 0;
            currentStamina = 0;
            maxStamina = 0;
            fighterArmorAcBonus = 0;
            fighterWeaponAttackBonus = 0;
            fighterWeaponDamageBonus = 0;
            fighterDamageReduction = 0;
        }
    }

    public void copyFrom(DnDPlayerData other) {
        this.primary = new ClassEntry();
        this.primary.copyFrom(other.primary);
        if (other.secondary != null) {
            this.secondary = new ClassEntry();
            this.secondary.copyFrom(other.secondary);
        } else {
            this.secondary = null;
        }
        this.prestigeClass = other.prestigeClass;
        this.race = other.race;
        this.xp = other.xp;
        this.abilityScores.copyFrom(other.abilityScores);
        this.currentHp = other.currentHp;
        this.achievementFlags.clear();
        this.achievementFlags.putAll(other.achievementFlags);
        this.abilityHotbar.copyFrom(other.abilityHotbar);
        this.cooldowns.clear();
        this.cooldowns.putAll(other.cooldowns);
        this.levelUpAvailable = other.levelUpAvailable;
        this.humanBonusFeatAvailable = other.humanBonusFeatAvailable;
        this.undyingResolveLastUsed = other.undyingResolveLastUsed;
        this.racialAcBonus = other.racialAcBonus;
        this.grantedFeats.clear();
        this.grantedFeats.addAll(other.grantedFeats);
        this.repeatableFeatStacks.clear();
        this.repeatableFeatStacks.putAll(other.repeatableFeatStacks);
        this.featSlotsAvailable = other.featSlotsAvailable;
        this.featAcBonus = other.featAcBonus;
        this.featInitiativeBonus = other.featInitiativeBonus;
        this.featShieldBonus = other.featShieldBonus;
        this.featWillBonus = other.featWillBonus;
        this.featRefBonus = other.featRefBonus;
        this.featFortBonus = other.featFortBonus;
        this.extraTurningCharges = other.extraTurningCharges;
        this.devotionDamageBonus = other.devotionDamageBonus;
        this.devotionHealingBonus = other.devotionHealingBonus;
        this.racialXpBonus = other.racialXpBonus;
        this.dwarvenToughnessHp = other.dwarvenToughnessHp;
        this.halflingLuckCharges = other.halflingLuckCharges;
        this.halfOrcFerocityAvailable = other.halfOrcFerocityAvailable;
        this.naturalArmorBonus = other.naturalArmorBonus;
        this.bladeOfForceBonus = other.bladeOfForceBonus;
        this.battleHardenedHp = other.battleHardenedHp;
        this.divineCasterLevelBonus = other.divineCasterLevelBonus;
        this.turningLevelBonus = other.turningLevelBonus;
        this.domainSaveDcBonus = other.domainSaveDcBonus;
        this.piousDefianceCharges = other.piousDefianceCharges;
        this.holyWarriorDamageBonus = other.holyWarriorDamageBonus;
        this.spellFocusBonus = other.spellFocusBonus;
        this.spellPenetrationBonus = other.spellPenetrationBonus;
        this.arcaneStrikeBonus = other.arcaneStrikeBonus;
        this.schoolCasterLevelBonus = other.schoolCasterLevelBonus;
        this.schoolMasteryCharges = other.schoolMasteryCharges;
        this.arcaneToughnessHp = other.arcaneToughnessHp;
        this.innateSpellCharges = other.innateSpellCharges;
        this.magicalTrainingCharges = other.magicalTrainingCharges;
        this.favoredEnemyBonus = other.favoredEnemyBonus;
        this.evasionUnlocked = other.evasionUnlocked;
        this.dangerSenseInitBonus = other.dangerSenseInitBonus;
        this.luckOfHeroesCharges = other.luckOfHeroesCharges;
        this.fatedCharges = other.fatedCharges;
        this.smiteCharges = other.smiteCharges;
        this.smiteDamageBonus = other.smiteDamageBonus;
        this.devotionHealingBonusWis = other.devotionHealingBonusWis;
        this.standFirmUnlocked = other.standFirmUnlocked;
        this.divineImpetusCharges = other.divineImpetusCharges;
        this.holyResilienceReduction = other.holyResilienceReduction;
        this.zealousSurgeCharges = other.zealousSurgeCharges;
        this.eldritchLoreCasterBonus = other.eldritchLoreCasterBonus;
        this.eldritchApexCharges = other.eldritchApexCharges;
        this.automaticMetamagicCharges = other.automaticMetamagicCharges;
        this.wardingGestureCharges = other.wardingGestureCharges;
        this.spellReflectionCharges = other.spellReflectionCharges;
        this.reactiveSpellCharges = other.reactiveSpellCharges;
        this.instantMetamagicCharges = other.instantMetamagicCharges;
        this.deathWardCharges = other.deathWardCharges;
        this.shadowStrBonus = other.shadowStrBonus;
        this.eldritchInsightCharges = other.eldritchInsightCharges;
        this.martialDamageBonus = other.martialDamageBonus;
        this.adamantineReduction = other.adamantineReduction;
        this.concentrationMightBonus = other.concentrationMightBonus;
        this.concentrationMightUsedThisSecond = other.concentrationMightUsedThisSecond;
        this.perfectClarityCharges = other.perfectClarityCharges;
        this.orderForgedCharges = other.orderForgedCharges;
        this.bloodyFuryBonus = other.bloodyFuryBonus;
        this.ironHeartSurgeCooldown = other.ironHeartSurgeCooldown;
        this.rogueSneakAttackDice = other.rogueSneakAttackDice;
        this.rogueEvasionUnlocked = other.rogueEvasionUnlocked;
        this.rogueTrapSenseBonus = other.rogueTrapSenseBonus;
        this.rogueSpecialAbilitySlotsAvailable = other.rogueSpecialAbilitySlotsAvailable;
        this.rogueCripplingStrikeStacks = other.rogueCripplingStrikeStacks;
        this.defensiveRollLastUsed = other.defensiveRollLastUsed;
        this.fighterBonusFeatSlotsAvailable = other.fighterBonusFeatSlotsAvailable;
        this.currentStamina = other.currentStamina;
        this.maxStamina = other.maxStamina;
        this.fighterArmorAcBonus = other.fighterArmorAcBonus;
        this.fighterWeaponAttackBonus = other.fighterWeaponAttackBonus;
        this.fighterWeaponDamageBonus = other.fighterWeaponDamageBonus;
        this.fighterDamageReduction = other.fighterDamageReduction;
    }
}
