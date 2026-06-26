package com.deadmind.dndmods;

import com.deadmind.dndmods.block.entity.ArcanePhylacteryBlockEntity;
import com.deadmind.dndmods.enchanting.ArcaneEnchantingTableBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DnDMods.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneEnchantingTableBlockEntity>> ARCANE_ENCHANTING_TABLE_BE =
            BLOCK_ENTITIES.register("arcane_enchanting_table",
                    () -> BlockEntityType.Builder.of(ArcaneEnchantingTableBlockEntity::new, ModBlocks.ARCANE_ENCHANTING_TABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcanePhylacteryBlockEntity>> ARCANE_PHYLACTERY_BE =
            BLOCK_ENTITIES.register("arcane_phylactery",
                    () -> BlockEntityType.Builder.of(ArcanePhylacteryBlockEntity::new, ModBlocks.ARCANE_PHYLACTERY.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
