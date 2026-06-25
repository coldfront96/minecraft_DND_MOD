package com.deadmind.dndmods.enchanting;

import com.deadmind.dndmods.items.enchanting.ArcaneDust;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ArcaneEnchantingMenu extends AbstractContainerMenu {
    private final Container enchantSlots = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            ArcaneEnchantingMenu.this.slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;

    public ArcaneEnchantingMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, BlockPos.ZERO);
    }

    public ArcaneEnchantingMenu(int containerId, Inventory playerInv, BlockPos pos) {
        super(ModMenuTypes.ARCANE_ENCHANTING_TABLE.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInv.player.level(), pos);

        // Slot 0: Item to enchant
        this.addSlot(new Slot(enchantSlots, 0, 25, 34));

        // Slot 1: Arcane Dust fuel
        this.addSlot(new Slot(enchantSlots, 1, 76, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArcaneDust;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotItem = slot.getItem();
            result = slotItem.copy();

            if (index < 2) {
                if (!this.moveItemStackTo(slotItem, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotItem.getItem() instanceof ArcaneDust) {
                if (!this.moveItemStackTo(slotItem, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(slotItem, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotItem.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, enchantSlots));
    }

    public Container getEnchantSlots() {
        return enchantSlots;
    }
}
