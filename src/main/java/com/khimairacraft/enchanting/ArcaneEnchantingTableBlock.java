package com.khimairacraft.enchanting;

import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ArcaneEnchantingTableBlock extends Block implements EntityBlock {
    private static final Component TITLE = Component.translatable("container.khimairacraft.arcane_enchanting_table");

    public ArcaneEnchantingTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            DnDPlayerData data = serverPlayer.getData(ModAttachments.PLAYER_DATA);
            DnDClass dndClass = data.getDnDClass();

            if (dndClass == DnDClass.FIGHTER || dndClass == DnDClass.ROGUE
                    || dndClass == DnDClass.BARBARIAN || dndClass == DnDClass.NONE) {
                serverPlayer.sendSystemMessage(Component.literal("You lack the arcane knowledge to use this."));
                return InteractionResult.CONSUME;
            }

            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, playerInv, p) -> new ArcaneEnchantingMenu(containerId, playerInv, pos),
                    TITLE
            ), buf -> buf.writeBlockPos(pos));
        }

        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneEnchantingTableBlockEntity(pos, state);
    }
}
