package com.deadmind.dndmods.systems;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public final class MobXpRewardSystem {

    private static final TagKey<EntityType<?>> BOSS_TAG =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("dndmods", "boss"));

    private static final int DEFAULT_XP = 10;

    private static final Map<EntityType<?>, Integer> XP_MAP = new HashMap<>();

    static {
        // PASSIVE — 0 XP
        XP_MAP.put(EntityType.COW, 0);
        XP_MAP.put(EntityType.SHEEP, 0);
        XP_MAP.put(EntityType.PIG, 0);
        XP_MAP.put(EntityType.CHICKEN, 0);
        XP_MAP.put(EntityType.RABBIT, 0);
        XP_MAP.put(EntityType.SQUID, 0);
        XP_MAP.put(EntityType.GLOW_SQUID, 0);
        XP_MAP.put(EntityType.HORSE, 0);
        XP_MAP.put(EntityType.DONKEY, 0);
        XP_MAP.put(EntityType.MULE, 0);
        XP_MAP.put(EntityType.MOOSHROOM, 0);
        XP_MAP.put(EntityType.PARROT, 0);
        XP_MAP.put(EntityType.BAT, 0);
        XP_MAP.put(EntityType.CAT, 0);
        XP_MAP.put(EntityType.OCELOT, 0);
        XP_MAP.put(EntityType.VILLAGER, 0);
        XP_MAP.put(EntityType.WANDERING_TRADER, 0);
        XP_MAP.put(EntityType.TURTLE, 0);
        XP_MAP.put(EntityType.FROG, 0);
        XP_MAP.put(EntityType.TADPOLE, 0);
        XP_MAP.put(EntityType.ALLAY, 0);
        XP_MAP.put(EntityType.AXOLOTL, 0);
        XP_MAP.put(EntityType.CAMEL, 0);
        XP_MAP.put(EntityType.SNIFFER, 0);
        XP_MAP.put(EntityType.ARMADILLO, 0);

        // NEUTRAL — 5 XP
        XP_MAP.put(EntityType.WOLF, 5);
        XP_MAP.put(EntityType.SPIDER, 5);
        XP_MAP.put(EntityType.ENDERMAN, 5);
        XP_MAP.put(EntityType.ZOMBIFIED_PIGLIN, 5);
        XP_MAP.put(EntityType.BEE, 5);
        XP_MAP.put(EntityType.IRON_GOLEM, 5);
        XP_MAP.put(EntityType.LLAMA, 5);
        XP_MAP.put(EntityType.PANDA, 5);
        XP_MAP.put(EntityType.POLAR_BEAR, 5);
        XP_MAP.put(EntityType.DOLPHIN, 5);
        XP_MAP.put(EntityType.GOAT, 5);
        XP_MAP.put(EntityType.PIGLIN, 5);
        XP_MAP.put(EntityType.FOX, 5);

        // HOSTILE_COMMON — 10 XP
        XP_MAP.put(EntityType.ZOMBIE, 10);
        XP_MAP.put(EntityType.SKELETON, 10);
        XP_MAP.put(EntityType.CREEPER, 10);
        XP_MAP.put(EntityType.WITCH, 10);
        XP_MAP.put(EntityType.SLIME, 10);
        XP_MAP.put(EntityType.PHANTOM, 10);
        XP_MAP.put(EntityType.ZOMBIE_VILLAGER, 10);

        // HOSTILE_UNCOMMON — 15 XP
        XP_MAP.put(EntityType.DROWNED, 15);
        XP_MAP.put(EntityType.HUSK, 15);
        XP_MAP.put(EntityType.STRAY, 15);
        XP_MAP.put(EntityType.BLAZE, 15);
        XP_MAP.put(EntityType.GHAST, 15);
        XP_MAP.put(EntityType.MAGMA_CUBE, 15);
        XP_MAP.put(EntityType.WITHER_SKELETON, 15);
        XP_MAP.put(EntityType.SILVERFISH, 15);
        XP_MAP.put(EntityType.CAVE_SPIDER, 15);
        XP_MAP.put(EntityType.HOGLIN, 15);
        XP_MAP.put(EntityType.PIGLIN_BRUTE, 15);
        XP_MAP.put(EntityType.GUARDIAN, 15);
        XP_MAP.put(EntityType.ENDERMITE, 15);
        XP_MAP.put(EntityType.BREEZE, 15);
        XP_MAP.put(EntityType.BOGGED, 15);

        // HOSTILE_RARE — 25 XP
        XP_MAP.put(EntityType.ELDER_GUARDIAN, 25);
        XP_MAP.put(EntityType.EVOKER, 25);
        XP_MAP.put(EntityType.VINDICATOR, 25);
        XP_MAP.put(EntityType.PILLAGER, 25);
        XP_MAP.put(EntityType.RAVAGER, 25);
        XP_MAP.put(EntityType.SHULKER, 25);
        XP_MAP.put(EntityType.VEX, 25);

        // BOSS — 200 XP
        XP_MAP.put(EntityType.WITHER, 200);
        XP_MAP.put(EntityType.ENDER_DRAGON, 200);
        XP_MAP.put(EntityType.WARDEN, 200);
    }

    private MobXpRewardSystem() {}

    public static int getXpForEntity(LivingEntity entity) {
        if (entity instanceof Player) {
            return 0;
        }

        if (entity.getType().is(BOSS_TAG)) {
            return 200;
        }

        int baseXp = XP_MAP.getOrDefault(entity.getType(), DEFAULT_XP);

        if (entity.hasCustomName()) {
            baseXp *= 2;
        }

        return baseXp;
    }
}
