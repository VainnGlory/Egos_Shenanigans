package net.vainnglory.egoistical.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.vainnglory.egoistical.effect.ModEffects;
import net.vainnglory.egoistical.util.AdrenalineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void preventDamageWithAdrenaline(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.hasStatusEffect(ModEffects.ADRENALINE)) {
            AdrenalineManager.addStoredDamage(entity.getUuid(), amount, source);

            cir.setReturnValue(false);
        }
    }
}
