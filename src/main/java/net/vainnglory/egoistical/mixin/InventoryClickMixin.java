package net.vainnglory.egoistical.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.vainnglory.egoistical.util.EMPManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class InventoryClickMixin {
    private static final String STORED_ENCHANTS_KEY = "EgoisticalStoredEnchants";

    @Inject(method = "internalOnSlotClick", at = @At("RETURN"))
    private void restoreEnchantsOnSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (EMPManager.isAffected(player.getUuid())) {
            return;
        }

        ItemStack cursorStack = player.currentScreenHandler.getCursorStack();
        restoreIfNeeded(cursorStack);

        if (slotIndex >= 0 && slotIndex < player.currentScreenHandler.slots.size()) {
            ItemStack slotStack = player.currentScreenHandler.getSlot(slotIndex).getStack();
            restoreIfNeeded(slotStack);
        }
    }

    private void restoreIfNeeded(ItemStack stack) {
        if (stack.isEmpty()) return;

        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(STORED_ENCHANTS_KEY)) return;

        NbtList storedEnchants = nbt.getList(STORED_ENCHANTS_KEY, 10);
        nbt.put("Enchantments", storedEnchants);
        nbt.remove(STORED_ENCHANTS_KEY);
    }
}
