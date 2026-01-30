package net.vainnglory.egoistical.util;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.vainnglory.egoistical.network.TrackerNetworking;

public class BeingWatchedHudRenderer implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        if (!TrackerNetworking.isBeingWatched()) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        String message = "Someone's watching you";
        int color = 0xFF5555;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int textWidth = textRenderer.getWidth(message);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 48;

        drawContext.drawText(textRenderer, message, x, y, color, true);
    }
}
