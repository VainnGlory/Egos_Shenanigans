package net.vainnglory.egoistical.util;

import net.vainnglory.egoistical.effect.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdrenalineManager {
    private static final Map<UUID, Float> storedDamage = new HashMap<>();
    private static final Map<UUID, DamageSource> lastDamageSource = new HashMap<>();

    public static void initializePlayer(UUID playerUuid) {
        storedDamage.put(playerUuid, 0.0f);
        lastDamageSource.remove(playerUuid);
    }

    public static void addStoredDamage(UUID playerUuid, float damage, DamageSource source) {
        float current = storedDamage.getOrDefault(playerUuid, 0.0f);
        storedDamage.put(playerUuid, current + damage);
        lastDamageSource.put(playerUuid, source);
    }

    public static float getStoredDamage(UUID playerUuid) {
        return storedDamage.getOrDefault(playerUuid, 0.0f);
    }

    public static void applyStoredDamage(LivingEntity entity) {
        UUID uuid = entity.getUuid();
        float damage = storedDamage.getOrDefault(uuid, 0.0f);

        if (damage > 0.0f) {
            DamageSource source = lastDamageSource.getOrDefault(uuid, entity.getDamageSources().generic());

            entity.damage(source, damage);

            if (entity instanceof ServerPlayerEntity player) {
                player.sendMessage(
                        Text.literal("Adrenaline wore off! Took " + String.format("%.1f", damage) + " delayed damage!")
                                .formatted(Formatting.GOLD),
                        true
                );
            }
        }

        storedDamage.remove(uuid);
        lastDamageSource.remove(uuid);
    }

    public static boolean hasAdrenalineEffect(LivingEntity entity) {
        return entity.hasStatusEffect(ModEffects.ADRENALINE);
    }

    public static void cleanup(UUID playerUuid) {
        storedDamage.remove(playerUuid);
        lastDamageSource.remove(playerUuid);
    }

    public static void clearAll() {
        storedDamage.clear();
        lastDamageSource.clear();
    }
}
