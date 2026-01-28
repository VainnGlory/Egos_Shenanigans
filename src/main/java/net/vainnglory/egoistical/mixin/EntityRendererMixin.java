package net.vainnglory.egoistical.mixin;

import net.vainnglory.egoistical.util.InventoryHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.vainnglory.egoistical.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void hideNameTag(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof PlayerEntity player) {
            if (InventoryHelper.hasItem(player, ModItems.GREED_RUNE)) {
                cir.setReturnValue(false);
            }
        }
    }
}