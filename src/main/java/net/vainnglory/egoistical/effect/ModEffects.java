package net.vainnglory.egoistical.effect;

import net.vainnglory.egoistical.Egoistical;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEffects {

    public static final StatusEffect ADRENALINE = registerEffect("adrenaline", new AdrenalineEffect());

    private static StatusEffect registerEffect(String name, StatusEffect effect) {
        return Registry.register(Registries.STATUS_EFFECT, new Identifier(Egoistical.MOD_ID, name), effect);
    }

    public static void registerModEffects() {
        Egoistical.LOGGER.info("Registering Mod Effects for " + Egoistical.MOD_ID);
    }
}
