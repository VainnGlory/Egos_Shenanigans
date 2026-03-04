package net.vainnglory.egoistical.util;

import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoundLodestoneCache {

    private static final Map<BlockPos, String> boundLodestones = new HashMap<>();
    private static final Map<UUID, BlockPos> playerBindings = new HashMap<>();

    public static void add(BlockPos pos, String dimension, UUID playerUUID) {
        boundLodestones.put(pos.toImmutable(), dimension);
        playerBindings.put(playerUUID, pos.toImmutable());
    }

    public static void remove(BlockPos pos) {
        boundLodestones.remove(pos);
        playerBindings.values().removeIf(p -> p.equals(pos));
    }

    public static void removeByPlayer(UUID playerUUID) {
        BlockPos pos = playerBindings.remove(playerUUID);
        if (pos != null) boundLodestones.remove(pos);
    }

    public static boolean hasBinding(UUID playerUUID) {
        return playerBindings.containsKey(playerUUID);
    }

    public static UUID getPlayerForPos(BlockPos pos) {
        for (Map.Entry<UUID, BlockPos> entry : playerBindings.entrySet()) {
            if (entry.getValue().equals(pos)) return entry.getKey();
        }
        return null;
    }

    public static Map<BlockPos, String> getAll() {
        return Collections.unmodifiableMap(boundLodestones);
    }
}


