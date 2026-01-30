package net.vainnglory.egoistical.mixin;

import net.vainnglory.egoistical.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "canFoodHeal", at = @At("HEAD"), cancellable = true)
    private void preventHealingInWater(CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (player.getInventory().contains(ModItems.GREED_RUNE.getDefaultStack())) {
            if (player.isTouchingWater() || player.isSubmergedInWater()) {
                cir.setReturnValue(false);
            }
        }
    }
}
