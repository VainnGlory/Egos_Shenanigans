package net.vainnglory.egoistical.item;

import net.minecraft.item.Item;
import net.vainnglory.egoistical.effect.ModEffects;
import net.vainnglory.egoistical.util.AdrenalineManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class AdrenalineShotItem extends Item {
    private final boolean filled;


    public AdrenalineShotItem(Settings settings, boolean filled) {
        super(settings);
        this.filled = filled;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!filled) {
            if (!world.isClient) {
                user.sendMessage(Text.literal("The adrenaline shot is empty!").formatted(Formatting.GOLD), true);
            }
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            applyAdrenalineEffect(serverPlayer, serverPlayer);

            ItemStack emptyShot = new ItemStack(ModItems.ADRENALINE_SHOT_EMPTY, stack.getCount());
            return TypedActionResult.success(emptyShot);
        }

        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!filled) {
            if (!user.getWorld().isClient) {
                user.sendMessage(Text.literal("The adrenaline shot is empty!").formatted(Formatting.GOLD), true);
            }
            return ActionResult.FAIL;
        }

        if (!user.getWorld().isClient && entity instanceof ServerPlayerEntity target && user instanceof ServerPlayerEntity serverUser) {
            applyAdrenalineEffect(serverUser, target);

            stack.decrement(1);

            ItemStack emptyShot = new ItemStack(ModItems.ADRENALINE_SHOT_EMPTY);
            if (!user.getInventory().insertStack(emptyShot)) {
                user.dropItem(emptyShot, false);
            }

            return ActionResult.SUCCESS;
        }

        return user.getWorld().isClient ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    private void applyAdrenalineEffect(ServerPlayerEntity user, LivingEntity target) {
        AdrenalineManager.initializePlayer(target.getUuid());

        target.addStatusEffect(new StatusEffectInstance(ModEffects.ADRENALINE, 600, 0, false, true, true));

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 1, false, false, true));

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 600, 0, false, false, true));

        target.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f);

        if (target instanceof ServerPlayerEntity targetPlayer) {
            if (target.equals(user)) {
                targetPlayer.sendMessage(Text.literal("Adrenaline rush activated!").formatted(Formatting.GOLD), true);
            } else {
                targetPlayer.sendMessage(Text.literal(user.getName().getString() + " injected you with adrenaline!").formatted(Formatting.GOLD), true);
                user.sendMessage(Text.literal("Injected " + targetPlayer.getName().getString() + " with adrenaline!").formatted(Formatting.GOLD), true);
            }
        }
    }

    public boolean isFilled() {
        return filled;
    }
}
