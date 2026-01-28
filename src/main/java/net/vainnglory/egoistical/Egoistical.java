package net.vainnglory.egoistical;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.item.ItemStack;
import net.vainnglory.egoistical.effect.ModEffects;
import net.vainnglory.egoistical.item.ModItemGroups;
import net.vainnglory.egoistical.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.vainnglory.egoistical.item.TrackerItem;
import net.vainnglory.egoistical.network.TrackerNetworking;
import net.vainnglory.egoistical.util.AdrenalineManager;
import net.vainnglory.egoistical.util.InventoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Egoistical implements ModInitializer {
    public static final String MOD_ID = "egoistical";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final float LOW_HEALTH_THRESHOLD = 0.3f;

    private int trackerUpdateTick = 0;
    private static final int TRACKER_UPDATE_INTERVAL = 10;
    private static final Set<UUID> playersWithAdrenaline = new HashSet<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Egoistical Mod");

        ModEffects.registerModEffects();
        LOGGER.info("Registered custom effects");

        ModItems.registerModItems();
        LOGGER.info("Registered mod items");
        ModItemGroups.registerItemGroups();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            trackerUpdateTick++;
            boolean shouldUpdateTrackers = false;
            if (trackerUpdateTick >= TRACKER_UPDATE_INTERVAL) {
                trackerUpdateTick = 0;
                shouldUpdateTrackers = true;
            }

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (hasGreedRune(player)) {
                    handleGreedRuneEffects(player);
                }

                if (shouldUpdateTrackers) {
                    handleTrackerUpdates(player, server);
                }

                handleAdrenalineEffectTracking(player);
            }
        });


        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            UUID uuid = newPlayer.getUuid();
            AdrenalineManager.cleanup(uuid);
            playersWithAdrenaline.remove(uuid);
            LOGGER.info("Cleaned up adrenaline data for player: {}", newPlayer.getName().getString());
        });

        LOGGER.info("Egoistical Mod initialized successfully");
    }

    private void handleAdrenalineEffectTracking(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean hasEffect = player.hasStatusEffect(ModEffects.ADRENALINE);
        boolean wasTracked = playersWithAdrenaline.contains(uuid);

        if (hasEffect && !wasTracked) {
            playersWithAdrenaline.add(uuid);
            AdrenalineManager.initializePlayer(uuid);
            LOGGER.info("Player {} started adrenaline effect", player.getName().getString());
        } else if (!hasEffect && wasTracked) {
            playersWithAdrenaline.remove(uuid);
            float storedDamage = AdrenalineManager.getStoredDamage(uuid);

            if (storedDamage > 0) {
                LOGGER.info("Player {} adrenaline ended, applying {} stored damage",
                        player.getName().getString(), storedDamage);
                AdrenalineManager.applyStoredDamage(player);
            }
        }
    }

    private void handleTrackerUpdates(ServerPlayerEntity trackingPlayer, net.minecraft.server.MinecraftServer server) {
        for (int i = 0; i < trackingPlayer.getInventory().size(); i++) {
            ItemStack stack = trackingPlayer.getInventory().getStack(i);
            if (stack.getItem() == ModItems.TRACKER && TrackerItem.hasTrackedPlayer(stack)) {
                UUID trackedUUID = TrackerItem.getTrackedPlayerUUID(stack);
                if (trackedUUID != null) {
                    ServerPlayerEntity trackedPlayer = server.getPlayerManager().getPlayer(trackedUUID);

                    if (trackedPlayer != null) {
                        TrackerNetworking.sendTrackerUpdate(trackedPlayer, trackingPlayer);
                    } else {
                        TrackerNetworking.sendPlayerOffline(trackedUUID, trackingPlayer);
                    }
                }
            }
        }
    }

    private boolean hasGreedRune(ServerPlayerEntity player) {
        return InventoryHelper.hasItem(player, ModItems.GREED_RUNE);
    }

    private void handleGreedRuneEffects(ServerPlayerEntity player) {
        float healthPercentage = player.getHealth() / player.getMaxHealth();
        boolean isLowHealth = healthPercentage <= LOW_HEALTH_THRESHOLD;

        if (isLowHealth) {
            StatusEffectInstance currentInvis = player.getStatusEffect(StatusEffects.INVISIBILITY);
            if (currentInvis == null || currentInvis.getDuration() < 20) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 60, 0, false, false, false));
            }

            StatusEffectInstance currentHunger = player.getStatusEffect(StatusEffects.HUNGER);
            if (currentHunger == null || currentHunger.getDuration() < 20 || currentHunger.getAmplifier() < 1) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 60, 1, false, false, false));
            }
        } else {
            if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                StatusEffectInstance invisEffect = player.getStatusEffect(StatusEffects.INVISIBILITY);
                if (invisEffect != null && invisEffect.getAmplifier() == 0 && invisEffect.getDuration() <= 60) {
                    player.removeStatusEffect(StatusEffects.INVISIBILITY);
                }
            }

            if (player.hasStatusEffect(StatusEffects.HUNGER)) {
                StatusEffectInstance hungerEffect = player.getStatusEffect(StatusEffects.HUNGER);
                if (hungerEffect != null && hungerEffect.getAmplifier() == 1 && hungerEffect.getDuration() <= 60) {
                    player.removeStatusEffect(StatusEffects.HUNGER);
                }
            }
        }

        if (player.isTouchingWater() || player.isSubmergedInWater()) {
            if (player.hasStatusEffect(StatusEffects.REGENERATION)) {
                player.removeStatusEffect(StatusEffects.REGENERATION);
            }
        }
    }
}