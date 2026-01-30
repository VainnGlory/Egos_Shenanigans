package net.vainnglory.egoistical.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.vainnglory.egoistical.item.PortableStasisItem;
import net.vainnglory.egoistical.item.TrickBagItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void onSlotClickForItemCharging(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (button != 1 || actionType != SlotActionType.PICKUP) {
            return;
        }

        ScreenHandler self = (ScreenHandler) (Object) this;

        if (slotIndex < 0 || slotIndex >= self.slots.size()) {
            return;
        }

        Slot slot = self.slots.get(slotIndex);
        ItemStack cursorStack = self.getCursorStack();
        ItemStack slotStack = slot.getStack();

        if (cursorStack.isEmpty() || slotStack.isEmpty()) {
            return;
        }

        if (cursorStack.isOf(Items.ENDER_PEARL) && slotStack.getItem() instanceof PortableStasisItem) {
            handleStasisCharging(player, slot, cursorStack, slotStack, ci);
            return;
        }

        if (slotStack.getItem() instanceof TrickBagItem && TrickBagItem.isEmpty(slotStack)) {
            handleTrickBagFilling(player, slot, cursorStack, slotStack, ci);
            return;
        }
    }

    private void handleStasisCharging(PlayerEntity player, Slot slot, ItemStack cursorStack, ItemStack slotStack, CallbackInfo ci) {
        int currentCharge = PortableStasisItem.getCharge(slotStack);

        if (currentCharge >= 2) {
            player.sendMessage(Text.literal("Stasis is already fully charged")
                    .formatted(Formatting.GOLD), true);
            ci.cancel();
            return;
        }

        cursorStack.decrement(1);
        PortableStasisItem.setCharge(slotStack, currentCharge + 1);
        slot.markDirty();

        int newCharge = currentCharge + 1;
        if (newCharge >= 2) {
            player.sendMessage(Text.literal("Stasis fully charged.")
                    .formatted(Formatting.GOLD), true);
            player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        } else {
            player.sendMessage(Text.literal("Stasis charged. (" + newCharge + "/2)")
                    .formatted(Formatting.GOLD), true);
            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.getInventory().markDirty();
            serverPlayer.playerScreenHandler.sendContentUpdates();
        }

        ci.cancel();
    }

    private void handleTrickBagFilling(PlayerEntity player, Slot slot, ItemStack cursorStack, ItemStack slotStack, CallbackInfo ci) {
        String fillType = null;

        if (cursorStack.isOf(Items.SAND)) {
            fillType = TrickBagItem.SAND;
        } else if (cursorStack.isOf(Items.ENDER_PEARL)) {
            fillType = TrickBagItem.ENDER_PEARL;
        } else if (cursorStack.isOf(Items.BLAZE_POWDER)) {
            fillType = TrickBagItem.BLAZE_POWDER;
        } else if (cursorStack.isOf(Items.SOUL_SAND)) {
            fillType = TrickBagItem.SOUL_SAND;
        }

        if (fillType == null) {
            return;
        }

        cursorStack.decrement(1);
        TrickBagItem.setContents(slotStack, fillType);
        slot.markDirty();

        String displayName = getDisplayName(fillType);
        player.sendMessage(Text.literal("Filled bag with " + displayName + ".")
                .formatted(Formatting.GOLD), true);
        player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.2f);

        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.getInventory().markDirty();
            serverPlayer.playerScreenHandler.sendContentUpdates();
        }

        ci.cancel();
    }

    private String getDisplayName(String fillType) {
        return switch (fillType) {
            case TrickBagItem.SAND -> "Sand";
            case TrickBagItem.ENDER_PEARL -> "Ender Pearl";
            case TrickBagItem.BLAZE_POWDER -> "Blaze Powder";
            case TrickBagItem.SOUL_SAND -> "Soul Sand";
            default -> "Unknown";
        };
    }
}