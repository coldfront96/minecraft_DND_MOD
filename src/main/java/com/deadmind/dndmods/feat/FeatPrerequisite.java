package com.deadmind.dndmods.feat;

import com.deadmind.dndmods.ability.AbilityScoreType;
import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.race.DnDRace;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public interface FeatPrerequisite {

    boolean isMet(DnDPlayerData data);

    String getDescription();

    static boolean allMet(List<FeatPrerequisite> prereqs, DnDPlayerData data) {
        for (FeatPrerequisite prereq : prereqs) {
            if (!prereq.isMet(data)) return false;
        }
        return true;
    }

    record AbilityScorePrerequisite(AbilityScoreType stat, int minimum) implements FeatPrerequisite {
        @Override
        public boolean isMet(DnDPlayerData data) {
            return stat.getScore(data.getAbilityScores()) >= minimum;
        }

        @Override
        public String getDescription() {
            return stat.name().charAt(0) + stat.name().substring(1).toLowerCase() + " " + minimum + "+";
        }
    }

    record RequiredFeatPrerequisite(String requiredFeatId, String requiredFeatName) implements FeatPrerequisite {
        @Override
        public boolean isMet(DnDPlayerData data) {
            return data.hasFeat(requiredFeatId);
        }

        @Override
        public String getDescription() {
            return "Feat: " + requiredFeatName;
        }
    }

    record ClassPrerequisite(DnDClass requiredClass) implements FeatPrerequisite {
        @Override
        public boolean isMet(DnDPlayerData data) {
            if (data.getPrimary().getDnDClass() == requiredClass) return true;
            return data.getSecondary() != null && data.getSecondary().getDnDClass() == requiredClass;
        }

        @Override
        public String getDescription() {
            return "Class: " + requiredClass.getDisplayName();
        }
    }

    record RacePrerequisite(DnDRace requiredRace) implements FeatPrerequisite {
        @Override
        public boolean isMet(DnDPlayerData data) {
            return data.getRace() == requiredRace;
        }

        @Override
        public String getDescription() {
            return "Race: " + requiredRace.getDisplayName();
        }
    }

    record LevelPrerequisite(int minimumLevel) implements FeatPrerequisite {
        @Override
        public boolean isMet(DnDPlayerData data) {
            return data.getTotalLevel() >= minimumLevel;
        }

        @Override
        public String getDescription() {
            return "Level " + minimumLevel + "+";
        }
    }

    record CasterLevelPrerequisite(int minimumCasterLevel) implements FeatPrerequisite {
        @Override
        public boolean isMet(DnDPlayerData data) {
            int casterLevel = 0;
            casterLevel = Math.max(casterLevel, getCasterLevel(data.getPrimary().getDnDClass(), data.getPrimary().getLevel()));
            if (data.getSecondary() != null) {
                casterLevel = Math.max(casterLevel, getCasterLevel(data.getSecondary().getDnDClass(), data.getSecondary().getLevel()));
            }
            return casterLevel >= minimumCasterLevel;
        }

        private static int getCasterLevel(DnDClass cls, int level) {
            return switch (cls) {
                case WIZARD, CLERIC -> level;
                case RANGER -> level / 2;
                default -> 0;
            };
        }

        @Override
        public String getDescription() {
            return "Caster Level " + minimumCasterLevel + "+";
        }
    }

    record BaseAttackBonusPrerequisite(int minimumBab) implements FeatPrerequisite {
        @Override
        public boolean isMet(DnDPlayerData data) {
            return calculateBab(data) >= minimumBab;
        }

        public static int calculateBab(DnDPlayerData data) {
            int bab = getClassBab(data.getPrimary().getDnDClass(), data.getPrimary().getLevel());
            if (data.getSecondary() != null) {
                bab += getClassBab(data.getSecondary().getDnDClass(), data.getSecondary().getLevel());
            }
            return bab;
        }

        private static int getClassBab(DnDClass cls, int level) {
            return switch (cls) {
                case FIGHTER, BARBARIAN, RANGER -> level;
                case ROGUE, CLERIC -> (int) (level * 0.75);
                case WIZARD -> level / 2;
                case NONE -> 0;
            };
        }

        @Override
        public String getDescription() {
            return "BAB +" + minimumBab;
        }
    }
}
