package net.vainnglory.egoistical;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.vainnglory.egoistical.effect.ModEffects;
import net.vainnglory.egoistical.item.ModItemGroups;
import net.vainnglory.egoistical.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.vainnglory.egoistical.item.TrackerItem;
import net.vainnglory.egoistical.network.TrackerNetworking;
import net.vainnglory.egoistical.util.ModRecipes;
import net.vainnglory.egoistical.util.AdrenalineManager;
import net.vainnglory.egoistical.util.InventoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Egoistical implements ModInitializer {
    public static final String MOD_ID = "egoistical";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final float LOW_HEALTH_THRESHOLD = 0.3f;

    private int trackerUpdateTick = 0;
    private static final int TRACKER_UPDATE_INTERVAL = 10;
    private static final Set<UUID> playersWithAdrenaline = new HashSet<>();

    private int thornedIngotTick = 0;
    private static final int THORNED_INGOT_DAMAGE_INTERVAL = 4;

    private final Map<UUID, Set<UUID>> playersBeingWatched = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Egoistical Mod");

        ModEffects.registerModEffects();
        LOGGER.info("Registered custom effects");

        ModItems.registerModItems();
        LOGGER.info("Registered mod items");
        ModItemGroups.registerItemGroups();

        ModRecipes.registerRecipes();
        LOGGER.info("Registered mod recipes");

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            trackerUpdateTick++;
            thornedIngotTick++;

            boolean shouldUpdateTrackers = false;
            if (trackerUpdateTick >= TRACKER_UPDATE_INTERVAL) {
                trackerUpdateTick = 0;
                shouldUpdateTrackers = true;
            }

            boolean shouldDamageThornedIngot = false;
            if (thornedIngotTick >= THORNED_INGOT_DAMAGE_INTERVAL) {
                thornedIngotTick = 0;
                shouldDamageThornedIngot = true;
            }

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (hasGreedRune(player)) {
                    handleGreedRuneEffects(player);
                }

                if (hasThornedIngot(player)) {
                    if (shouldDamageThornedIngot) {
                        handleThornedIngotDamage(player);
                    }
                }

                if (shouldUpdateTrackers) {
                    handleTrackerUpdates(player, server);
                }

                handleAdrenalineEffectTracking(player);
            }

            if (shouldUpdateTrackers) {
                updateBeingWatchedStatus(server);
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

    private void handleThornedIngotDamage(ServerPlayerEntity player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();

        boolean isHeld = mainHand.isOf(ModItems.THORNED_INGOT) || offHand.isOf(ModItems.THORNED_INGOT);

        float damage = isHeld ? 1.0f : 0.5f;

        float newHealth = player.getHealth() - damage;

        if (newHealth <= 0) {
            DamageSource magicDamage = player.getDamageSources().magic();
            player.damage(magicDamage, damage);
        } else {
            player.setHealth(newHealth);
        }
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

                        boolean sameDimension = trackingPlayer.getWorld().getRegistryKey().equals(
                                trackedPlayer.getWorld().getRegistryKey());
                        double distanceSquared = trackingPlayer.squaredDistanceTo(trackedPlayer);
                        boolean withinRange = distanceSquared <= (35 * 35);

                        if (sameDimension && withinRange) {
                            playersBeingWatched
                                    .computeIfAbsent(trackedUUID, k -> new HashSet<>())
                                    .add(trackingPlayer.getUuid());
                        } else {
                            Set<UUID> watchers = playersBeingWatched.get(trackedUUID);
                            if (watchers != null) {
                                watchers.remove(trackingPlayer.getUuid());
                            }
                        }
                    } else {
                        TrackerNetworking.sendPlayerOffline(trackedUUID, trackingPlayer);

                        Set<UUID> watchers = playersBeingWatched.get(trackedUUID);
                        if (watchers != null) {
                            watchers.remove(trackingPlayer.getUuid());
                        }
                    }
                }
            }
        }
    }

    private void updateBeingWatchedStatus(net.minecraft.server.MinecraftServer server) {
        for (Set<UUID> watchers : playersBeingWatched.values()) {
            watchers.removeIf(watcherUUID -> server.getPlayerManager().getPlayer(watcherUUID) == null);
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerUUID = player.getUuid();
            Set<UUID> watchers = playersBeingWatched.get(playerUUID);

            boolean hasWatchers = watchers != null && !watchers.isEmpty();
            TrackerNetworking.sendBeingWatchedUpdate(player, hasWatchers);
        }

        playersBeingWatched.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private boolean hasGreedRune(ServerPlayerEntity player) {
        return InventoryHelper.hasItem(player, ModItems.GREED_RUNE);
    }

    private boolean hasThornedIngot(ServerPlayerEntity player) {
        return InventoryHelper.hasItem(player, ModItems.THORNED_INGOT);
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

        if (player.isSneaking() || player.isCrawling()) {
            if (player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                player.removeStatusEffect(StatusEffects.SLOWNESS);
            }
        }
    }
}