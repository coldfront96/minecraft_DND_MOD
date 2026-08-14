package com.khimairacraft.ranger;

import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import com.khimairacraft.race.DnDRace;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The Ranger's Favored Enemy categories. Nineteen picks: seven creature-type
 * groups and twelve humanoid race groups. Each value carries a predicate that
 * answers "does this living entity belong to my category?".
 *
 * <p>Creature-type categories test the entity's {@link EntityType} (or a type
 * tag); humanoid race categories test whether the target is a player whose
 * {@link DnDRace} matches. Categories can legitimately overlap (e.g. a Drowned
 * counts as both AQUATIC and UNDEAD) — the Favored Enemy damage hook resolves
 * overlaps by taking the highest bonus among the categories the Ranger has
 * actually selected, never stacking them.
 */
public enum FavoredEnemyType {

    // --- Creature-type categories (7) ---
    UNDEAD("Undead", t -> t.getType().is(EntityTypeTags.UNDEAD)
            || raceIn(t, DnDRace.REVENANT, DnDRace.DHAMPIR, DnDRace.SHADAR_KAI,
                    DnDRace.VAMPIRE_SPAWN, DnDRace.SKELETON_WARRIOR)),

    ILLAGERS("Illagers", t -> typeIn(t,
            EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
            EntityType.RAVAGER, EntityType.WITCH, EntityType.ILLUSIONER)),

    ARTHROPODS("Arthropods", t -> t.getType().is(EntityTypeTags.ARTHROPOD)
            || typeIn(t, EntityType.SPIDER, EntityType.CAVE_SPIDER,
                    EntityType.SILVERFISH, EntityType.ENDERMITE)),

    AQUATIC("Aquatic", t -> typeIn(t,
            EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN, EntityType.DROWNED)),

    NETHER_BORN("Nether-born", t -> typeIn(t,
            EntityType.BLAZE, EntityType.GHAST, EntityType.MAGMA_CUBE,
            EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.HOGLIN,
            EntityType.ZOMBIFIED_PIGLIN)),

    END_BORN("End-born", t -> typeIn(t,
            EntityType.ENDERMAN, EntityType.SHULKER, EntityType.ENDER_DRAGON)),

    // Wolves, polar bears, and every other vanilla Animal not otherwise
    // categorised. Overlaps with the other categories are resolved by the
    // "highest bonus" rule at damage time, so a broad Animal match is safe.
    BEASTS("Beasts", t -> t instanceof Animal),

    // --- Humanoid race categories (12) ---
    HUMAN("Human", t -> raceIn(t, DnDRace.HUMAN)),
    ELF("Elf", t -> raceIn(t, DnDRace.ELF)),
    DWARF("Dwarf", t -> raceIn(t, DnDRace.DWARF)),
    HALFLING("Halfling", t -> raceIn(t, DnDRace.HALFLING)),
    GNOME("Gnome", t -> raceIn(t, DnDRace.GNOME)),
    HALF_ELF("Half-Elf", t -> raceIn(t, DnDRace.HALF_ELF)),
    HALF_ORC("Half-Orc", t -> raceIn(t, DnDRace.HALF_ORC)),
    DRAGONBORN("Dragonborn", t -> raceIn(t, DnDRace.DRAGONBORN)),
    GOLIATH("Goliath", t -> raceIn(t, DnDRace.GOLIATH)),
    WARFORGED("Warforged", t -> raceIn(t, DnDRace.WARFORGED)),
    TIEFLING("Tiefling", t -> raceIn(t, DnDRace.TIEFLING)),
    AASIMAR("Aasimar", t -> raceIn(t, DnDRace.AASIMAR));

    private final String displayName;
    private final Predicate<LivingEntity> predicate;

    FavoredEnemyType(String displayName, Predicate<LivingEntity> predicate) {
        this.displayName = displayName;
        this.predicate = predicate;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** True if the given living entity belongs to this favored-enemy category. */
    public boolean matches(LivingEntity target) {
        if (target == null) return false;
        return predicate.test(target);
    }

    private static boolean typeIn(LivingEntity target, EntityType<?>... types) {
        EntityType<?> type = target.getType();
        for (EntityType<?> t : types) {
            if (type == t) return true;
        }
        return false;
    }

    private static boolean raceIn(LivingEntity target, DnDRace... races) {
        if (!(target instanceof ServerPlayer player)) return false;
        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data == null) return false;
        DnDRace race = data.getRace();
        for (DnDRace r : races) {
            if (race == r) return true;
        }
        return false;
    }

    /** All categories a target currently matches (used by the damage hook / tooling). */
    public static Set<FavoredEnemyType> matching(LivingEntity target) {
        Set<FavoredEnemyType> result = EnumSet.noneOf(FavoredEnemyType.class);
        for (FavoredEnemyType type : values()) {
            if (type.matches(target)) result.add(type);
        }
        return result;
    }

    /** Safe enum lookup by name; returns null instead of throwing. */
    public static FavoredEnemyType byName(String name) {
        if (name == null) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
