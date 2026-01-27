package net.vainnglory.egoistical.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class AdrenalineEffect extends StatusEffect {

    public AdrenalineEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xFF45A2);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
    }
}
