package com.khimairacraft.ability;

import com.khimairacraft.playerdata.AbilityScores;

public enum AbilityScoreType {
    STRENGTH,
    DEXTERITY,
    CONSTITUTION,
    INTELLIGENCE,
    WISDOM,
    CHARISMA;

    public int getScore(AbilityScores scores) {
        return switch (this) {
            case STRENGTH -> scores.getStrength();
            case DEXTERITY -> scores.getDexterity();
            case CONSTITUTION -> scores.getConstitution();
            case INTELLIGENCE -> scores.getIntelligence();
            case WISDOM -> scores.getWisdom();
            case CHARISMA -> scores.getCharisma();
        };
    }

    public int getModifier(AbilityScores scores) {
        return scores.getModifier(getScore(scores));
    }
}
