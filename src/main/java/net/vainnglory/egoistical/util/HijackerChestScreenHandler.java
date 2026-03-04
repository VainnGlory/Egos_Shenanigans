package net.vainnglory.egoistical.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

public class HijackerChestScreenHandler extends ScreenHandler {

    private final Inventory enderChest;
    private boolean itemTaken = false;

    public HijackerChestScreenHandler(int syncId, PlayerInventory playerInventory, Inventory enderChest) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        checkSize(enderChest, 27);
        this.enderChest = enderChest;
        enderChest.onOpen(playerInventory.player);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(enderChest, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < 27) {
            if (itemTaken) return;

            if (actionType != SlotActionType.PICKUP && actionType != SlotActionType.QUICK_MOVE) return;

            if (!getCursorStack().isEmpty()) return;

            if (this.slots.get(slotIndex).getStack().isEmpty()) return;

            int countBefore = this.slots.get(slotIndex).getStack().getCount();
            super.onSlotClick(slotIndex, button, actionType, player);
            int countAfter = this.slots.get(slotIndex).getStack().getCount();

            if (countBefore > countAfter) {
                itemTaken = true;
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.closeHandledScreen();
                }
            }
            return;
        }

        if (actionType == SlotActionType.QUICK_MOVE) return;

        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        if (slot >= 27) return ItemStack.EMPTY;

        Slot source = this.slots.get(slot);
        if (!source.hasStack()) return ItemStack.EMPTY;

        ItemStack stack = source.getStack();
        ItemStack original = stack.copy();

        if (!this.insertItem(stack, 27, this.slots.size(), false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            source.setStack(ItemStack.EMPTY);
        } else {
            source.markDirty();
        }

        return original;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.enderChest.onClose(player);
    }
}


