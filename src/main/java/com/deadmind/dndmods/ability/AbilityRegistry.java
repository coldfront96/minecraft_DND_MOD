package com.deadmind.dndmods.ability;

import com.deadmind.dndmods.ability.impl.*;
import com.deadmind.dndmods.classes.DnDClass;

import java.util.*;

public class AbilityRegistry {
    private static final Map<DnDClass, List<Ability>> CLASS_ABILITIES = new EnumMap<>(DnDClass.class);
    private static final Map<String, Ability> ALL_ABILITIES = new LinkedHashMap<>();

    public static void init() {
        // Fighter abilities
        register(new PowerStrike());
        register(new ShieldBash());
        register(new SecondWind());

        // Rogue abilities
        register(new BackStab());
        register(new Evasion());
        register(new ShadowStep());

        // Wizard abilities
        register(new Fireball());
        register(new FrostNova());
        register(new ArcaneShield());

        // Cleric abilities
        register(new HealingWord());
        register(new SmiteUndead());
        register(new DivineShield());

        // Ranger abilities
        register(new MultiShot());
        register(new HuntersMark());
        register(new NaturesGrasp());

        // Barbarian abilities
        register(new Reckless());
        register(new GroundSlam());
        register(new BerserkerRage());
    }

    private static void register(Ability ability) {
        ALL_ABILITIES.put(ability.getId(), ability);
        CLASS_ABILITIES.computeIfAbsent(ability.getRequiredClass(), k -> new ArrayList<>())
                .add(ability);
    }

    public static List<Ability> getAbilitiesForClass(DnDClass dndClass) {
        return CLASS_ABILITIES.getOrDefault(dndClass, Collections.emptyList());
    }

    public static List<Ability> getAvailableAbilities(DnDClass dndClass, int level) {
        return getAbilitiesForClass(dndClass).stream()
                .filter(a -> a.getRequiredLevel() <= level)
                .toList();
    }

    public static Ability getAbility(String id) {
        return ALL_ABILITIES.get(id);
    }
}
