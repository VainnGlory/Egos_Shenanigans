package net.vainnglory.egoistical;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.util.Identifier;
import net.vainnglory.egoistical.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.vainnglory.egoistical.item.TrackerItem;
import net.vainnglory.egoistical.network.TrackerNetworking;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EgoisticalClient implements ClientModInitializer {
    private static final Set<UUID> glowingPlayers = new HashSet<>();
    private static boolean wasHoldingRune = false;

    @Override
    public void onInitializeClient() {
        ModelPredicateProviderRegistry.register(
                ModItems.TRACKER,
                new Identifier("egoistical", "tracking"),
                (stack, world, entity, seed) -> {
                    return TrackerItem.hasTrackedPlayer(stack) ? 1.0f : 0.0f;
                }
        );
        TrackerNetworking.registerClientPacketReceiver();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TrackerNetworking.clearCache();
        });

        HudRenderCallback.EVENT.register(new TrackerHudRenderer());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) {
                if (!glowingPlayers.isEmpty()) {
                    glowingPlayers.clear();
                    wasHoldingRune = false;
                }
                return;
            }

            ClientPlayerEntity player = client.player;
            boolean hasRune = player.getInventory().contains(ModItems.GREED_RUNE.getDefaultStack());

            if (hasRune) {
                wasHoldingRune = true;
                for (AbstractClientPlayerEntity otherPlayer : client.world.getPlayers()) {
                    if (!otherPlayer.equals(player)) {
                        if (!otherPlayer.isGlowing()) {
                            otherPlayer.setGlowing(true);
                        }
                        glowingPlayers.add(otherPlayer.getUuid());
                    }
                }
            } else if (wasHoldingRune || !glowingPlayers.isEmpty()) {
                for (AbstractClientPlayerEntity otherPlayer : client.world.getPlayers()) {
                    if (glowingPlayers.contains(otherPlayer.getUuid())) {
                        otherPlayer.setGlowing(false);
                    }
                }
                glowingPlayers.clear();
                wasHoldingRune = false;
            }
        });
    }

    public static boolean shouldCancelStepSound(PlayerEntity player) {
        return player.getInventory().contains(ModItems.GREED_RUNE.getDefaultStack());
    }

    public static boolean shouldHideNameTag(PlayerEntity player) {
        return player.getInventory().contains(ModItems.GREED_RUNE.getDefaultStack());
    }
}
