package com.deadmind.dndmods.race;

public class AbilityScoreModifiers {
    private final int strMod;
    private final int dexMod;
    private final int conMod;
    private final int intMod;
    private final int wisMod;
    private final int chaMod;

    public AbilityScoreModifiers(int strMod, int dexMod, int conMod, int intMod, int wisMod, int chaMod) {
        this.strMod = strMod;
        this.dexMod = dexMod;
        this.conMod = conMod;
        this.intMod = intMod;
        this.wisMod = wisMod;
        this.chaMod = chaMod;
    }

    public AbilityScoreModifiers() {
        this(0, 0, 0, 0, 0, 0);
    }

    public int getStrMod() { return strMod; }
    public int getDexMod() { return dexMod; }
    public int getConMod() { return conMod; }
    public int getIntMod() { return intMod; }
    public int getWisMod() { return wisMod; }
    public int getChaMod() { return chaMod; }
}
