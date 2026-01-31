package net.vainnglory.egoistical;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.vainnglory.egoistical.item.ModItems;
import net.vainnglory.egoistical.item.PortableStasisItem;
import net.vainnglory.egoistical.item.TrackerItem;
import net.vainnglory.egoistical.item.TrickBagItem;
import net.vainnglory.egoistical.network.TrackerNetworking;
import net.vainnglory.egoistical.util.BeingWatchedHudRenderer;
import net.vainnglory.egoistical.util.InventoryHelper;

public class EgoisticalClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelPredicateProviderRegistry.register(
                ModItems.TRACKER,
                new Identifier("egoistical", "tracking"),
                (stack, world, entity, seed) -> {
                    return TrackerItem.hasTrackedPlayer(stack) ? 1.0f : 0.0f;
                }
        );

        ModelPredicateProviderRegistry.register(
                ModItems.PORTABLE_STASIS,
                new Identifier("egoistical", "charged"),
                (stack, world, entity, seed) -> {
                    return PortableStasisItem.isFullyCharged(stack) ? 1.0f : 0.0f;
                }
        );

        ModelPredicateProviderRegistry.register(
                ModItems.TRICK_BAG,
                new Identifier("egoistical", "filled"),
                (stack, world, entity, seed) -> {
                    return TrickBagItem.isFilled(stack) ? 1.0f : 0.0f;
                }
        );

        TrackerNetworking.registerClientPacketReceiver();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TrackerNetworking.clearCache();
        });

        HudRenderCallback.EVENT.register(new TrackerHudRenderer());
        HudRenderCallback.EVENT.register(new BeingWatchedHudRenderer());
    }

    public static boolean shouldCancelStepSound(PlayerEntity player) {
        return InventoryHelper.hasItem(player, ModItems.GREED_RUNE);
    }

    public static boolean shouldHideNameTag(PlayerEntity player) {
        return InventoryHelper.hasItem(player, ModItems.GREED_RUNE);
    }
}
