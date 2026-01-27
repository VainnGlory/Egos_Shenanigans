package net.vainnglory.egoistical;

import net.vainnglory.egoistical.item.ModItems;
import net.vainnglory.egoistical.item.TrackerItem;
import net.vainnglory.egoistical.network.TrackerNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class TrackerHudRenderer implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        ClientPlayerEntity player = client.player;
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();

        ItemStack trackerStack = null;
        if (mainHand.getItem() == ModItems.TRACKER) {
            trackerStack = mainHand;
        } else if (offHand.getItem() == ModItems.TRACKER) {
            trackerStack = offHand;
        }

        if (trackerStack == null) {
            return;
        }

        UUID trackedUUID = TrackerItem.getTrackedPlayerUUID(trackerStack);
        String trackedName = TrackerItem.getTrackedPlayerName(trackerStack);

        if (trackedUUID == null || trackedName == null) {
            renderText(drawContext, client, "Tracker: No target", 0xF5C271);
            return;
        }

        AbstractClientPlayerEntity trackedPlayer = null;
        for (AbstractClientPlayerEntity worldPlayer : client.world.getPlayers()) {
            if (worldPlayer.getUuid().equals(trackedUUID)) {
                trackedPlayer = worldPlayer;
                break;
            }
        }

        if (trackedPlayer != null) {
            BlockPos pos = trackedPlayer.getBlockPos();
            int distance = (int) Math.sqrt(player.squaredDistanceTo(trackedPlayer));

            renderText(drawContext, client, "Tracking: " + trackedName, 0xF571AF);
            renderText(drawContext, client, String.format("X: %d  Y: %d  Z: %d", pos.getX(), pos.getY(), pos.getZ()), 0xFFFFFF, 10);
            renderText(drawContext, client, "Distance: " + distance + "m", 0xF5C271, 20);
        } else {
            TrackerNetworking.TrackedPlayerData cachedData = TrackerNetworking.getTrackedPosition(trackedUUID);

            if (cachedData != null && cachedData.isOnline) {
                BlockPos pos = cachedData.position;
                BlockPos playerPos = player.getBlockPos();
                int distance = (int) Math.sqrt(playerPos.getSquaredDistance(pos));

                String currentDimension = client.world.getRegistryKey().getValue().toString();
                boolean sameDimension = currentDimension.equals(cachedData.dimension);

                if (sameDimension) {
                    renderText(drawContext, client, "Tracking: " + trackedName, 0xF571AF);
                    renderText(drawContext, client, String.format("X: %d  Y: %d  Z: %d", pos.getX(), pos.getY(), pos.getZ()), 0xFFFFFF, 10);
                    renderText(drawContext, client, "Distance: ~" + distance + "m (Far)", 0xF5C271, 20);
                } else {
                    renderText(drawContext, client, "Tracking: " + trackedName, 0xF571AF);
                    renderText(drawContext, client, "Status: Different Dimension", 0xDEB983, 10);
                    renderText(drawContext, client, "Last known: " + cachedData.dimension, 0xAAAAAA, 20);
                }
            } else {
                renderText(drawContext, client, "Tracking: " + trackedName, 0xF571AF);
                renderText(drawContext, client, "Status: Offline", 0xF571AF, 10);
            }
        }
    }

    private void renderText(DrawContext drawContext, MinecraftClient client, String text, int color) {
        renderText(drawContext, client, text, color, 0);
    }

    private void renderText(DrawContext drawContext, MinecraftClient client, String text, int color, int yOffset) {
        TextRenderer textRenderer = client.textRenderer;

        int x = 10;
        int y = 10 + yOffset;

        drawContext.drawText(textRenderer, text, x, y, color, true);
    }
}
