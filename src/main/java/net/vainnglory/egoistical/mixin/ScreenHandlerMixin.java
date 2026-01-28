package net.vainnglory.egoistical.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.vainnglory.egoistical.item.PortableStasisItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void onSlotClickForStasisCharging(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
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

        if (!cursorStack.isOf(Items.ENDER_PEARL)) {
            return;
        }

        if (!(slotStack.getItem() instanceof PortableStasisItem)) {
            return;
        }

        int currentCharge = PortableStasisItem.getCharge(slotStack);

        if (currentCharge >= 2) {
            if (!player.getWorld().isClient) {
                player.sendMessage(Text.literal("Stasis is already fully charged!")
                        .formatted(Formatting.GOLD), true);
            }
            ci.cancel();
            return;
        }

        cursorStack.decrement(1);
        PortableStasisItem.setCharge(slotStack, currentCharge + 1);
        slot.markDirty();

        if (!player.getWorld().isClient) {
            int newCharge = currentCharge + 1;
            if (newCharge >= 2) {
                player.sendMessage(Text.literal("Stasis fully charged!")
                        .formatted(Formatting.GOLD), true);
                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.5f);
            } else {
                player.sendMessage(Text.literal("Stasis charged! (" + newCharge + "/2)")
                        .formatted(Formatting.GOLD), true);
                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }

            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.getInventory().markDirty();
                serverPlayer.playerScreenHandler.sendContentUpdates();
            }
        }

        ci.cancel();
    }
}
