package com.deadmind.dndmods.block;

import com.deadmind.dndmods.ModItems;
import com.deadmind.dndmods.block.entity.ArcanePhylacteryBlockEntity;
import com.deadmind.dndmods.items.enchanting.ArcaneDust;
import com.deadmind.dndmods.items.enchanting.DustTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ArcanePhylacteryBlock extends BaseEntityBlock {

    public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, 4);

    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0);

    public ArcanePhylacteryBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TIER, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcanePhylacteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof ArcanePhylacteryBlockEntity phylactery) {
                phylactery.serverTick();
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ArcanePhylacteryBlockEntity phylactery)) return InteractionResult.PASS;

        long stored = phylactery.getStoredXp();
        if (stored > 0) {
            serverPlayer.giveExperiencePoints((int) stored);
            phylactery.setStoredXp(0);
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f);
            serverPlayer.sendSystemMessage(Component.literal(
                    "§6[DnDMods] §eExtracted §f" + stored + "§e XP from the Arcane Phylactery."));
        } else {
            serverPlayer.sendSystemMessage(Component.literal(
                    "§6[DnDMods] §eThe Phylactery has no stored XP."));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ArcanePhylacteryBlockEntity phylactery)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (stack.is(Items.GLASS_BOTTLE)) {
            return handleGlassBottle(serverPlayer, phylactery, stack, level, pos);
        }

        if (stack.getItem() instanceof ArcaneDust dust) {
            return handleDustUpgrade(serverPlayer, phylactery, dust, stack, state, level, pos);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private ItemInteractionResult handleGlassBottle(ServerPlayer player, ArcanePhylacteryBlockEntity phylactery,
                                                     ItemStack bottle, Level level, BlockPos pos) {
        if (phylactery.getStoredXp() >= 50) {
            bottle.shrink(1);
            phylactery.drainXp(50);
            player.getInventory().add(new ItemStack(Items.EXPERIENCE_BOTTLE));
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f);
        } else {
            player.sendSystemMessage(Component.literal(
                    "§6[DnDMods] §eNot enough stored XP to fill a bottle. Needs at least 50 XP."));
        }
        return ItemInteractionResult.CONSUME;
    }

    private ItemInteractionResult handleDustUpgrade(ServerPlayer player, ArcanePhylacteryBlockEntity phylactery,
                                                     ArcaneDust dust, ItemStack stack, BlockState state,
                                                     Level level, BlockPos pos) {
        int currentTier = phylactery.getTier();
        if (currentTier >= 4) {
            player.sendSystemMessage(Component.literal(
                    "§6[DnDMods] §eThis Phylactery is already at maximum tier."));
            return ItemInteractionResult.CONSUME;
        }

        int requiredDustTier = getRequiredDustTier(currentTier);
        int dustTier = dust.getTier().getTier();

        if (dustTier != requiredDustTier) {
            String needed = getDustTierName(requiredDustTier);
            player.sendSystemMessage(Component.literal(
                    "§6[DnDMods] §eUpgrade requires 4x " + needed + " Arcane Dust."));
            return ItemInteractionResult.CONSUME;
        }

        int totalDust = countDustInInventory(player, dust.getTier());
        if (totalDust < 4) {
            String needed = getDustTierName(requiredDustTier);
            player.sendSystemMessage(Component.literal(
                    "§6[DnDMods] §eNeed 4x " + needed + " Arcane Dust (have " + totalDust + ")."));
            return ItemInteractionResult.CONSUME;
        }

        consumeDustFromInventory(player, dust.getTier(), 4);
        int newTier = currentTier + 1;
        phylactery.setTier(newTier);
        level.setBlock(pos, state.setValue(TIER, newTier), 3);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal(
                "§6[DnDMods] §ePhylactery upgraded to Tier " + newTier + "!"));

        return ItemInteractionResult.CONSUME;
    }

    private int getRequiredDustTier(int currentPhylacteryTier) {
        return switch (currentPhylacteryTier) {
            case 1 -> 3;  // Lapis (Tier 3)
            case 2 -> 6;  // Blaze (Tier 6)
            case 3 -> 9;  // Netherite (Tier 9)
            default -> -1;
        };
    }

    private String getDustTierName(int dustTier) {
        for (DustTier dt : DustTier.values()) {
            if (dt.getTier() == dustTier) return dt.getMaterialName();
        }
        return "Unknown";
    }

    private int countDustInInventory(Player player, DustTier tier) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.getItem() instanceof ArcaneDust d && d.getTier() == tier) {
                count += invStack.getCount();
            }
        }
        return count;
    }

    private void consumeDustFromInventory(Player player, DustTier tier, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.getItem() instanceof ArcaneDust d && d.getTier() == tier) {
                int take = Math.min(remaining, invStack.getCount());
                invStack.shrink(take);
                remaining -= take;
            }
        }
    }
}
