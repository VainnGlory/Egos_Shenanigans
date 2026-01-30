package net.vainnglory.egoistical.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrackerNetworking {
    public static final Identifier TRACKER_UPDATE_PACKET = new Identifier("egoistical", "tracker_update");
    public static final Identifier BEING_WATCHED_PACKET = new Identifier("egoistical", "being_watched");

    private static final Map<UUID, TrackedPlayerData> trackedPositions = new HashMap<>();
    private static boolean isBeingWatched = false;

    public static void registerClientPacketReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(TRACKER_UPDATE_PACKET, (client, handler, buf, responseSender) -> {
            UUID trackedUUID = buf.readUuid();
            boolean isOnline = buf.readBoolean();

            if (isOnline) {
                int x = buf.readInt();
                int y = buf.readInt();
                int z = buf.readInt();
                String dimension = buf.readString();

                client.execute(() -> {
                    trackedPositions.put(trackedUUID, new TrackedPlayerData(
                            new BlockPos(x, y, z), dimension, true
                    ));
                });
            } else {
                client.execute(() -> {
                    TrackedPlayerData data = trackedPositions.get(trackedUUID);
                    if (data != null) {
                        data.isOnline = false;
                    }
                });
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(BEING_WATCHED_PACKET, (client, handler, buf, responseSender) -> {
            boolean watched = buf.readBoolean();

            client.execute(() -> {
                isBeingWatched = watched;
            });
        });
    }

    public static void sendTrackerUpdate(ServerPlayerEntity trackedPlayer, ServerPlayerEntity trackingPlayer) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(trackedPlayer.getUuid());
        buf.writeBoolean(true);

        BlockPos pos = trackedPlayer.getBlockPos();
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeString(trackedPlayer.getWorld().getRegistryKey().getValue().toString());

        ServerPlayNetworking.send(trackingPlayer, TRACKER_UPDATE_PACKET, buf);
    }

    public static void sendPlayerOffline(UUID trackedUUID, ServerPlayerEntity trackingPlayer) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(trackedUUID);
        buf.writeBoolean(false);

        ServerPlayNetworking.send(trackingPlayer, TRACKER_UPDATE_PACKET, buf);
    }

    public static void sendBeingWatchedUpdate(ServerPlayerEntity targetPlayer, boolean isWatched) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(isWatched);

        ServerPlayNetworking.send(targetPlayer, BEING_WATCHED_PACKET, buf);
    }

    public static TrackedPlayerData getTrackedPosition(UUID uuid) {
        return trackedPositions.get(uuid);
    }

    public static boolean isBeingWatched() {
        return isBeingWatched;
    }

    public static void clearCache() {
        trackedPositions.clear();
        isBeingWatched = false;
    }

    public static class TrackedPlayerData {
        public BlockPos position;
        public String dimension;
        public boolean isOnline;

        public TrackedPlayerData(BlockPos position, String dimension, boolean isOnline) {
            this.position = position;
            this.dimension = dimension;
            this.isOnline = isOnline;
        }
    }
}
