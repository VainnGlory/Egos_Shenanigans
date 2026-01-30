package net.vainnglory.egoistical.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.vainnglory.egoistical.util.ModRarities;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class TrickBagItem extends Item {
    private static final String CONTENTS_KEY = "BagContents";
    private static final int MAX_USE_TIME = 72000;
    private static final int MIN_CHARGE_TIME = 15;
    private static final int COOLDOWN_TICKS = 60;
    private static final double RANGE = 6.0;

    public static final String EMPTY = "empty";
    public static final String SAND = "sand";
    public static final String ENDER_PEARL = "ender_pearl";
    public static final String BLAZE_POWDER = "blaze_powder";
    public static final String SOUL_SAND = "soul_sand";
    private final ModRarities rarity;

    public TrickBagItem(Settings settings, ModRarities rarity) {
        super(settings);
        this.rarity = rarity;
    }

    public static String getContents(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(CONTENTS_KEY)) {
            return EMPTY;
        }
        return nbt.getString(CONTENTS_KEY);
    }

    public static void setContents(ItemStack stack, String contents) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(CONTENTS_KEY, contents);
    }

    public static boolean isEmpty(ItemStack stack) {
        return getContents(stack).equals(EMPTY);
    }

    public static boolean isFilled(ItemStack stack) {
        return !isEmpty(stack);
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);

        return baseName.copy().setStyle(Style.EMPTY.withColor(rarity.color));
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        return ActionResult.PASS;
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack bagStack = user.getStackInHand(hand);

        if (isEmpty(bagStack)) {
            if (!world.isClient) {
                user.sendMessage(Text.literal("Someone forgot to fill their bag")
                        .formatted(Formatting.GOLD), true);
            }
            return TypedActionResult.fail(bagStack);
        }

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(bagStack);
        }

        user.setCurrentHand(hand);
        return TypedActionResult.consume(bagStack);
    }


    @Override
    public int getMaxUseTime(ItemStack stack) {
        return MAX_USE_TIME;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof ServerPlayerEntity player)) return;
        if (isEmpty(stack)) return;

        int chargeTime = getMaxUseTime(stack) - remainingUseTicks;
        if (chargeTime < MIN_CHARGE_TIME) {
            return;
        }

        LivingEntity target = findTarget(player, RANGE);

        if (target == null) {
            player.sendMessage(Text.literal("there's nobody here").formatted(Formatting.RED), true);
            return;
        }

        String contents = getContents(stack);
        boolean success = applyEffect(player, target, contents, (ServerWorld) world);

        if (success) {
            setContents(stack, EMPTY);
            player.getItemCooldownManager().set(this, COOLDOWN_TICKS);

            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1.0f, 0.8f);
        }
    }


    @Nullable
    private LivingEntity findTarget(PlayerEntity player, double range) {
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1.0f);
        Vec3d endPos = eyePos.add(lookVec.multiply(range));

        HitResult blockHit = player.getWorld().raycast(new RaycastContext(
                eyePos, endPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));

        double maxDist = range;
        if (blockHit.getType() != HitResult.Type.MISS) {
            maxDist = blockHit.getPos().distanceTo(eyePos);
        }

        Box searchBox = player.getBoundingBox().stretch(lookVec.multiply(maxDist)).expand(1.0);
        List<Entity> entities = player.getWorld().getOtherEntities(player, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && e != player);

        LivingEntity closestTarget = null;
        double closestDist = maxDist;

        for (Entity entity : entities) {
            Box entityBox = entity.getBoundingBox().expand(0.3);
            Optional<Vec3d> hit = entityBox.raycast(eyePos, endPos);

            if (hit.isPresent()) {
                double dist = eyePos.distanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = (LivingEntity) entity;
                }
            }
        }

        return closestTarget;
    }


    private boolean applyEffect(ServerPlayerEntity user, LivingEntity target, String contents, ServerWorld world) {
        switch (contents) {
            case SAND -> {
                applySandEffect(user, target, world);
                return true;
            }
            case ENDER_PEARL -> {
                applyEnderPearlEffect(user, target, world);
                return true;
            }
            case BLAZE_POWDER -> {
                applyBlazePowderEffect(user, target, world);
                return true;
            }
            case SOUL_SAND -> {
                applySoulSandEffect(user, target, world);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void applySandEffect(ServerPlayerEntity user, LivingEntity target, ServerWorld world) {
        for (int i = 0; i < 30; i++) {
            world.spawnParticles(ParticleTypes.WHITE_ASH,
                    target.getX() + (world.random.nextDouble() - 0.5) * 2,
                    target.getY() + 1 + world.random.nextDouble(),
                    target.getZ() + (world.random.nextDouble() - 0.5) * 2,
                    1, 0.1, 0.1, 0.1, 0.1);
        }

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0, false, true, true));

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BLOCK_SAND_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);

        if (target instanceof ServerPlayerEntity targetPlayer) {
            targetPlayer.sendMessage(Text.literal("Sand in your eyes!").formatted(Formatting.GOLD), true);
        }
        user.sendMessage(Text.literal("Blinded " + target.getName().getString() + "!").formatted(Formatting.GOLD), true);
    }

    private void applyEnderPearlEffect(ServerPlayerEntity user, LivingEntity target, ServerWorld world) {
        for (int i = 0; i < 32; i++) {
            world.spawnParticles(ParticleTypes.PORTAL,
                    target.getX() + (world.random.nextDouble() - 0.5) * 2,
                    target.getY() + world.random.nextDouble() * 2,
                    target.getZ() + (world.random.nextDouble() - 0.5) * 2,
                    1, 0, 0, 0, 0.1);
        }

        Vec3d currentPos = target.getPos();
        double angle = world.random.nextDouble() * Math.PI * 2;
        double distance = 20 + world.random.nextDouble() * 20;

        double newX = currentPos.x + Math.cos(angle) * distance;
        double newZ = currentPos.z + Math.sin(angle) * distance;

        BlockPos targetPos = new BlockPos((int) newX, (int) currentPos.y, (int) newZ);
        BlockPos safePos = findSafePosition(world, targetPos);

        if (safePos != null) {
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            target.teleport(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);

            for (int i = 0; i < 32; i++) {
                world.spawnParticles(ParticleTypes.PORTAL,
                        target.getX() + (world.random.nextDouble() - 0.5) * 2,
                        target.getY() + world.random.nextDouble() * 2,
                        target.getZ() + (world.random.nextDouble() - 0.5) * 2,
                        1, 0, 0, 0, 0.1);
            }

            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        if (target instanceof ServerPlayerEntity targetPlayer) {
            targetPlayer.sendMessage(Text.literal("You've been displaced").formatted(Formatting.GOLD), true);
        }
        user.sendMessage(Text.literal("Teleported " + target.getName().getString() + " away").formatted(Formatting.GOLD), true);
    }

    @Nullable
    private BlockPos findSafePosition(ServerWorld world, BlockPos targetPos) {
        for (int yOffset = 0; yOffset < 20; yOffset++) {
            BlockPos checkUp = targetPos.up(yOffset);
            BlockPos checkDown = targetPos.down(yOffset);

            if (isSafePosition(world, checkUp)) return checkUp;
            if (isSafePosition(world, checkDown)) return checkDown;
        }
        return null;
    }

    private boolean isSafePosition(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isAir() &&
                world.getBlockState(pos.up()).isAir() &&
                !world.getBlockState(pos.down()).isAir();
    }

    private void applyBlazePowderEffect(ServerPlayerEntity user, LivingEntity target, ServerWorld world) {
        spawnParticleLine(user, target, world, ParticleTypes.CAMPFIRE_COSY_SMOKE);

        for (int i = 0; i < 20; i++) {
            world.spawnParticles(ParticleTypes.FLAME,
                    target.getX() + (world.random.nextDouble() - 0.5),
                    target.getY() + world.random.nextDouble() * 2,
                    target.getZ() + (world.random.nextDouble() - 0.5),
                    1, 0, 0, 0, 0.05);
        }

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 160, 0, false, true, true));

        target.setOnFireFor(2);

        if (target instanceof ServerPlayerEntity targetPlayer) {
            targetPlayer.sendMessage(Text.literal("Cursed flames. Water won't save you").formatted(Formatting.GOLD), true);
        }

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        user.sendMessage(Text.literal("Set " + target.getName().getString() + " ablaze with cursed fire").formatted(Formatting.GOLD), true);
    }

    private void applySoulSandEffect(ServerPlayerEntity user, LivingEntity target, ServerWorld world) {
        for (int i = 0; i < 30; i++) {
            world.spawnParticles(ParticleTypes.SOUL,
                    target.getX() + (world.random.nextDouble() - 0.5) * 2,
                    target.getY() + world.random.nextDouble() * 2,
                    target.getZ() + (world.random.nextDouble() - 0.5) * 2,
                    1, 0, 0.05, 0, 0.02);
        }

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 9, false, false, true));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 40, 128, false, false, true)); // Negative jump boost
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 4, false, false, true));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 9, false, false, true));

        target.setVelocity(0, 0, 0);
        if (target instanceof ServerPlayerEntity targetPlayer) {
            targetPlayer.velocityModified = true;
        }

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.PLAYERS, 1.0f, 0.5f);

        if (target instanceof ServerPlayerEntity targetPlayer) {
            targetPlayer.sendMessage(Text.literal("Your soul is bound.").formatted(Formatting.GOLD), true);
        }
        user.sendMessage(Text.literal("Stunned " + target.getName().getString() + ".").formatted(Formatting.GOLD), true);
    }

    private void spawnParticleLine(ServerPlayerEntity user, LivingEntity target, ServerWorld world, net.minecraft.particle.ParticleEffect particle) {
        Vec3d start = user.getEyePos();
        Vec3d end = target.getPos().add(0, target.getHeight() / 2, 0);
        Vec3d direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);

        for (double d = 0; d < distance; d += 0.5) {
            Vec3d pos = start.add(direction.multiply(d));
            world.spawnParticles(particle, pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        String contents = getContents(stack);

        if (contents.equals(EMPTY)) {
            tooltip.add(Text.literal("Empty").formatted(Formatting.GRAY, Formatting.ITALIC));
        } else {
            tooltip.add(Text.literal("Contains: ").formatted(Formatting.GRAY)
                    .append(Text.literal(getContentsDisplayName(contents)).formatted(getContentsColor(contents))));
        }
    }

    private String getContentsDisplayName(String contents) {
        return switch (contents) {
            case SAND -> "Sand";
            case ENDER_PEARL -> "Ender Pearl";
            case BLAZE_POWDER -> "Blaze Powder";
            case SOUL_SAND -> "Soul Sand";
            default -> "Empty";
        };
    }

    private Formatting getContentsColor(String contents) {
        return switch (contents) {
            case SAND -> Formatting.GOLD;
            case ENDER_PEARL -> Formatting.DARK_PURPLE;
            case BLAZE_POWDER -> Formatting.RED;
            case SOUL_SAND -> Formatting.DARK_AQUA;
            default -> Formatting.GRAY;
        };
    }

    public static float getFilledPredicate(ItemStack stack) {
        return isFilled(stack) ? 1.0f : 0.0f;
    }
}
