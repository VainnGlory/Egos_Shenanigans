package net.vainnglory.egoistical.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class TrackerItem extends Item {
    private static final String TRACKED_UUID_KEY = "TrackedPlayerUUID";
    private static final String TRACKED_NAME_KEY = "TrackedPlayerName";

    public TrackerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (entity instanceof PlayerEntity targetPlayer) {
            if (user.isSneaking()) {
                clearTrackedPlayer(stack);
                if (!user.getWorld().isClient) {
                    user.sendMessage(Text.literal("Tracker cleared").formatted(Formatting.YELLOW), true);
                }
                return ActionResult.SUCCESS;
            } else {
                setTrackedPlayer(stack, targetPlayer);
                if (!user.getWorld().isClient) {
                    user.sendMessage(Text.literal("Now tracking: " + targetPlayer.getName().getString()).formatted(Formatting.GREEN), true);
                }
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    public static void setTrackedPlayer(ItemStack stack, PlayerEntity player) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putUuid(TRACKED_UUID_KEY, player.getUuid());
        nbt.putString(TRACKED_NAME_KEY, player.getName().getString());
    }

    public static void clearTrackedPlayer(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            nbt.remove(TRACKED_UUID_KEY);
            nbt.remove(TRACKED_NAME_KEY);
        }
    }

    public static UUID getTrackedPlayerUUID(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.containsUuid(TRACKED_UUID_KEY)) {
            return nbt.getUuid(TRACKED_UUID_KEY);
        }
        return null;
    }

    public static String getTrackedPlayerName(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(TRACKED_NAME_KEY)) {
            return nbt.getString(TRACKED_NAME_KEY);
        }
        return null;
    }

    public static boolean hasTrackedPlayer(ItemStack stack) {
        return getTrackedPlayerUUID(stack) != null;
    }
}
