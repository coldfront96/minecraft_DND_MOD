package com.khimairacraft.playerdata;

import net.minecraft.nbt.CompoundTag;

public class AbilityScores {
    private int strength = 10;
    private int dexterity = 10;
    private int constitution = 10;
    private int intelligence = 10;
    private int wisdom = 10;
    private int charisma = 10;

    public int getStrength() { return strength; }
    public int getDexterity() { return dexterity; }
    public int getConstitution() { return constitution; }
    public int getIntelligence() { return intelligence; }
    public int getWisdom() { return wisdom; }
    public int getCharisma() { return charisma; }

    public void setStrength(int value) { this.strength = clamp(value); }
    public void setDexterity(int value) { this.dexterity = clamp(value); }
    public void setConstitution(int value) { this.constitution = clamp(value); }
    public void setIntelligence(int value) { this.intelligence = clamp(value); }
    public void setWisdom(int value) { this.wisdom = clamp(value); }
    public void setCharisma(int value) { this.charisma = clamp(value); }

    public int getModifier(int score) {
        return (score - 10) / 2;
    }

    public int getStrMod() { return getModifier(strength); }
    public int getDexMod() { return getModifier(dexterity); }
    public int getConMod() { return getModifier(constitution); }
    public int getIntMod() { return getModifier(intelligence); }
    public int getWisMod() { return getModifier(wisdom); }
    public int getChaMod() { return getModifier(charisma); }

    private static int clamp(int value) {
        return Math.max(1, Math.min(30, value));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("STR", strength);
        tag.putInt("DEX", dexterity);
        tag.putInt("CON", constitution);
        tag.putInt("INT", intelligence);
        tag.putInt("WIS", wisdom);
        tag.putInt("CHA", charisma);
        return tag;
    }

    public void load(CompoundTag tag) {
        strength = tag.getInt("STR");
        dexterity = tag.getInt("DEX");
        constitution = tag.getInt("CON");
        intelligence = tag.getInt("INT");
        wisdom = tag.getInt("WIS");
        charisma = tag.getInt("CHA");
    }

    public void copyFrom(AbilityScores other) {
        this.strength = other.strength;
        this.dexterity = other.dexterity;
        this.constitution = other.constitution;
        this.intelligence = other.intelligence;
        this.wisdom = other.wisdom;
        this.charisma = other.charisma;
    }
}
