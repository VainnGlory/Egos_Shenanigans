package net.vainnglory.egoistical.util;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.vainnglory.egoistical.Egoistical;

import java.util.*;

public class EMPManager {
    public static final String STORED_ENCHANTS_KEY = "EgoisticalStoredEnchants";

    private static final Map<UUID, Long> empExpireTimes = new HashMap<>();

    private static long currentWorldTime = 0;

    public static void disableEnchantments(ServerPlayerEntity player, int durationTicks) {
        UUID uuid = player.getUuid();
        int itemsAffected = 0;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (disableStackEnchantments(stack)) {
                itemsAffected++;
            }
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getEquippedStack(slot);
            if (disableStackEnchantments(stack)) {
                itemsAffected++;
            }
        }

        if (itemsAffected > 0) {
            empExpireTimes.put(uuid, currentWorldTime + durationTicks);
            Egoistical.LOGGER.info("EMP disabled enchantments on {} items for player: {}", itemsAffected, player.getName().getString());
        }
    }

    private static boolean disableStackEnchantments(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasEnchantments()) {
            return false;
        }

        if (stack.getNbt() != null && stack.getNbt().contains(STORED_ENCHANTS_KEY)) {
            return false;
        }

        NbtList enchants = stack.getEnchantments().copy();
        if (enchants.isEmpty()) {
            return false;
        }

        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.put(STORED_ENCHANTS_KEY, enchants);

        stack.removeSubNbt("Enchantments");

        return true;
    }

    public static void restoreEnchantments(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        empExpireTimes.remove(uuid);

        int itemsRestored = 0;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (restoreStackEnchantments(stack)) {
                itemsRestored++;
            }
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getEquippedStack(slot);
            if (restoreStackEnchantments(stack)) {
                itemsRestored++;
            }
        }

        if (itemsRestored > 0) {
            Egoistical.LOGGER.info("EMP effect ended, restored enchantments on {} items for player: {}", itemsRestored, player.getName().getString());
        }
    }

    private static boolean restoreStackEnchantments(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(STORED_ENCHANTS_KEY)) {
            return false;
        }

        NbtList storedEnchants = nbt.getList(STORED_ENCHANTS_KEY, 10); // 10 = NbtCompound type
        nbt.put("Enchantments", storedEnchants);

        nbt.remove(STORED_ENCHANTS_KEY);

        return true;
    }

    public static void tick(long worldTime) {
        currentWorldTime = worldTime;
    }

    public static boolean needsRestoration(UUID uuid, long worldTime) {
        Long expireTime = empExpireTimes.get(uuid);
        return expireTime != null && worldTime >= expireTime;
    }

    public static boolean isAffected(UUID uuid) {
        return empExpireTimes.containsKey(uuid);
    }

    public static void cleanup(UUID uuid) {
        empExpireTimes.remove(uuid);
    }
    public static void restoreAnyStoredEnchantments(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            restoreStackEnchantments(stack);
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getEquippedStack(slot);
            restoreStackEnchantments(stack);
        }
    }
}
