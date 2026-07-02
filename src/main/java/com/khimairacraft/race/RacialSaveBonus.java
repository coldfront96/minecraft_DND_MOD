package com.khimairacraft.race;

public final class RacialSaveBonus {

    private RacialSaveBonus() {}

    public static int getEnchantmentBonus(DnDRace race) {
        return switch (race) {
            case ELF, HALF_ELF -> 2;
            default -> 0;
        };
    }

    public static int getFearBonus(DnDRace race) {
        return switch (race) {
            case HALFLING -> 2;
            default -> 0;
        };
    }

    public static int getIllusionBonus(DnDRace race) {
        return switch (race) {
            case GNOME -> 2;
            default -> 0;
        };
    }

    public static int getAllSavesBonus(DnDRace race) {
        return switch (race) {
            case HALFLING -> 1;
            default -> 0;
        };
    }
}
