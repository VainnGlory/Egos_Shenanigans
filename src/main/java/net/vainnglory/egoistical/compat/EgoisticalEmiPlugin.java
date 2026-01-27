package net.vainnglory.egoistical.compat;

import net.vainnglory.egoistical.item.ModItems;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;

public class EgoisticalEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.removeEmiStacks(EmiStack.of(ModItems.GREED_RUNE));
        registry.removeEmiStacks(EmiStack.of(ModItems.TRACKER));
        registry.removeEmiStacks(EmiStack.of(ModItems.ADRENALINE_SHOT_EMPTY));
        registry.removeEmiStacks(EmiStack.of(ModItems.ADRENALINE_SHOT_FILLED));
    }
}