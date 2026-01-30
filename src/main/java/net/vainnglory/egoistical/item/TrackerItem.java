package net.vainnglory.egoistical.item;

import java.util.UUID;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.vainnglory.egoistical.util.ModRarities;

public class TrackerItem extends Item {
    private static final String TRACKED_UUID_KEY = "TrackedPlayerUUID";
    private static final String TRACKED_NAME_KEY = "TrackedPlayerName";
    private final ModRarities rarity;

    public TrackerItem(Settings settings, ModRarities rarity) {
        super(settings);
        this.rarity = rarity;
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);

        return baseName.copy().setStyle(Style.EMPTY.withColor(rarity.color));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient && user.isSneaking() && hasTrackedPlayer(stack)) {
            clearTrackedPlayer(stack);
            user.sendMessage(Text.literal("Tracker cleared").formatted(Formatting.GOLD), true);

            if (user instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.getInventory().markDirty();
                serverPlayer.playerScreenHandler.sendContentUpdates();
            }

            return TypedActionResult.success(stack);
        }

        return TypedActionResult.pass(stack);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (user.getWorld().isClient) {
            return ActionResult.PASS;
        }

        if (entity instanceof PlayerEntity targetPlayer) {
            if (user.isSneaking()) {
                return ActionResult.PASS;
            }

            ItemStack heldStack = user.getStackInHand(hand);
            setTrackedPlayer(heldStack, targetPlayer);
            user.sendMessage(Text.literal("Now tracking: " + targetPlayer.getName().getString()).formatted(Formatting.GOLD), true);

            user.setStackInHand(hand, heldStack);

            if (user instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.getInventory().markDirty();
                serverPlayer.playerScreenHandler.sendContentUpdates();
            }

            return ActionResult.SUCCESS;
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
            if (nbt.isEmpty()) {
                stack.setNbt(null);
            }
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
