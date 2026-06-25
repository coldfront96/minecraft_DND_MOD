package com.deadmind.dndmods;

import com.deadmind.dndmods.enchanting.ArcaneEnchantingTableBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, DnDMods.MOD_ID);

    public static final DeferredHolder<Block, ArcaneEnchantingTableBlock> ARCANE_ENCHANTING_TABLE =
            BLOCKS.register("arcane_enchanting_table",
                    () -> new ArcaneEnchantingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
