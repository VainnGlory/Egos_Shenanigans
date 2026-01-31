package net.vainnglory.egoistical.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.vainnglory.egoistical.item.ModItems;
import net.vainnglory.egoistical.util.InventoryHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class PlayerDeathMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void dropHeadOnDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity victim = (ServerPlayerEntity) (Object) this;

        if (damageSource.getAttacker() instanceof PlayerEntity killer) {
            if (InventoryHelper.hasItem(killer, ModItems.MARKSMANS_PROOF)) {
                ItemStack headStack = new ItemStack(Items.PLAYER_HEAD);

                GameProfile profile = victim.getGameProfile();
                NbtCompound nbt = headStack.getOrCreateNbt();
                NbtCompound skullOwner = new NbtCompound();
                NbtHelper.writeGameProfile(skullOwner, profile);
                nbt.put("SkullOwner", skullOwner);

                victim.dropStack(headStack);
            }
        }
    }
}
