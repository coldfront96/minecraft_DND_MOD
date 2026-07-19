package com.khimairacraft;

import com.khimairacraft.block.ArcaneOreBlock;
import com.khimairacraft.block.ArcanePhylacteryBlock;
import com.khimairacraft.block.ArcanePhylacteryTier;
import com.khimairacraft.enchanting.ArcaneEnchantingTableBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
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

    public static final DeferredHolder<Block, ArcanePhylacteryBlock> ARCANE_PHYLACTERY =
            BLOCKS.register("arcane_phylactery",
                    () -> new ArcanePhylacteryBlock(BlockBehaviour.Properties.of()
                            .strength(3.0f, 6.0f)
                            .sound(SoundType.GLASS)
                            .lightLevel(state -> ArcanePhylacteryTier.fromTier(state.getValue(ArcanePhylacteryBlock.TIER)).getLightLevel())
                            .noOcclusion()));

    public static final DeferredHolder<Block, ArcaneOreBlock> ARCANE_ORE =
            BLOCKS.register("arcane_ore",
                    () -> new ArcaneOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)));

    public static final DeferredHolder<Block, ArcaneOreBlock> DEEPSLATE_ARCANE_ORE =
            BLOCKS.register("deepslate_arcane_ore",
                    () -> new ArcaneOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_GOLD_ORE)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
