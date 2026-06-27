package com.deadmind.dndmods.playerdata;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.classes.PrestigeClass;
import com.deadmind.dndmods.player.AbilityHotbar;
import com.deadmind.dndmods.race.AbilityScoreModifiers;
import com.deadmind.dndmods.race.DnDRace;
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
    private int featSlotsAvailable = 0;
    private int featAcBonus = 0;
    private int featInitiativeBonus = 0;
    private int featShieldBonus = 0;
    private int featWillBonus = 0;

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
        net.minecraft.nbt.ListTag featList = new net.minecraft.nbt.ListTag();
        for (String featId : grantedFeats) {
            featList.add(net.minecraft.nbt.StringTag.valueOf(featId));
        }
        featTag.put("Granted", featList);
        tag.put("Feats", featTag);

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
        if (tag.contains("Feats")) {
            CompoundTag featTag = tag.getCompound("Feats");
            featSlotsAvailable = featTag.getInt("SlotsAvailable");
            featAcBonus = featTag.getInt("AcBonus");
            featInitiativeBonus = featTag.getInt("InitiativeBonus");
            featShieldBonus = featTag.getInt("ShieldBonus");
            featWillBonus = featTag.getInt("WillBonus");
            net.minecraft.nbt.ListTag featList = featTag.getList("Granted", net.minecraft.nbt.Tag.TAG_STRING);
            for (int i = 0; i < featList.size(); i++) {
                grantedFeats.add(featList.getString(i));
            }
        } else {
            featSlotsAvailable = 0;
            featAcBonus = 0;
            featInitiativeBonus = 0;
            featShieldBonus = 0;
            featWillBonus = 0;
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
        this.featSlotsAvailable = other.featSlotsAvailable;
        this.featAcBonus = other.featAcBonus;
        this.featInitiativeBonus = other.featInitiativeBonus;
        this.featShieldBonus = other.featShieldBonus;
        this.featWillBonus = other.featWillBonus;
    }
}
