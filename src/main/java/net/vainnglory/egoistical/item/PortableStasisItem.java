package net.vainnglory.egoistical.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.vainnglory.egoistical.util.ModRarities;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class PortableStasisItem extends Item {
    private static final String CHARGE_KEY = "StasisCharge";
    private static final String LODESTONE_POS_KEY = "LodestonePos";
    private static final String LODESTONE_DIMENSION_KEY = "LodestoneDimension";
    private static final int MAX_CHARGE = 2;
    private final ModRarities rarity;

    private static final ChunkTicketType<ChunkPos> STASIS_TICKET = ChunkTicketType.create(
            "egoistical_stasis", Comparator.comparingLong(ChunkPos::toLong), 20 * 30 // 30 seconds
    );

    public PortableStasisItem(Settings settings, ModRarities rarity) {
        super(settings);
        this.rarity = rarity;
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);

        return baseName.copy().setStyle(Style.EMPTY.withColor(rarity.color));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        if (player == null) return ActionResult.PASS;

        if (world.getBlockState(pos).isOf(Blocks.LODESTONE)) {
            if (!world.isClient) {
                bindToLodestone(stack, pos, world);
                player.sendMessage(Text.literal("Stasis bound to lodestone at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                        .formatted(Formatting.GOLD), true);

                world.playSound(null, pos, SoundEvents.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            return ActionResult.success(world.isClient);
        }

        return ActionResult.PASS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            int charge = getCharge(stack);

            if (!hasBoundLodestone(stack)) {
                user.sendMessage(Text.literal("Stasis not bound to a lodestone!")
                        .formatted(Formatting.RED), true);
                return TypedActionResult.fail(stack);
            }

            if (charge < MAX_CHARGE) {
                user.sendMessage(Text.literal("Stasis not fully charged! (" + charge + "/" + MAX_CHARGE + " ender pearls)")
                        .formatted(Formatting.RED), true);
                return TypedActionResult.fail(stack);
            }

            if (teleportToLodestone(serverPlayer, stack)) {
                setCharge(stack, 0);

                clearBinding(stack);

                serverPlayer.getInventory().markDirty();
                serverPlayer.playerScreenHandler.sendContentUpdates();

                return TypedActionResult.success(stack);
            } else {
                user.sendMessage(Text.literal("Lodestone no longer exists! Binding cleared.")
                        .formatted(Formatting.RED), true);
                clearBinding(stack);
                return TypedActionResult.fail(stack);
            }
        }

        return TypedActionResult.consume(stack);
    }

    private boolean teleportToLodestone(ServerPlayerEntity player, ItemStack stack) {
        BlockPos lodestonePos = getBoundLodestonePos(stack);
        String dimensionId = getBoundLodestoneDimension(stack);

        if (lodestonePos == null || dimensionId == null) {
            return false;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return false;

        RegistryKey<World> targetDimensionKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(dimensionId));
        ServerWorld targetWorld = server.getWorld(targetDimensionKey);

        if (targetWorld == null) {
            return false;
        }

        ChunkPos chunkPos = new ChunkPos(lodestonePos);
        targetWorld.getChunkManager().addTicket(STASIS_TICKET, chunkPos, 3, chunkPos);

        if (!targetWorld.getBlockState(lodestonePos).isOf(Blocks.LODESTONE)) {
            return false;
        }

        BlockPos teleportPos = lodestonePos.up();

        double originalX = player.getX();
        double originalY = player.getY();
        double originalZ = player.getZ();
        ServerWorld originalWorld = player.getServerWorld();

        originalWorld.playSound(null, originalX, originalY, originalZ,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.5f);

        for (int i = 0; i < 32; i++) {
            originalWorld.spawnParticles(ParticleTypes.PORTAL,
                    originalX + (originalWorld.random.nextDouble() - 0.5) * 2,
                    originalY + originalWorld.random.nextDouble() * 2,
                    originalZ + (originalWorld.random.nextDouble() - 0.5) * 2,
                    1, 0, 0, 0, 0.1);
        }

        player.teleport(targetWorld,
                teleportPos.getX() + 0.5,
                teleportPos.getY(),
                teleportPos.getZ() + 0.5,
                player.getYaw(),
                player.getPitch());

        targetWorld.playSound(null, teleportPos,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);

        for (int i = 0; i < 32; i++) {
            targetWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                    teleportPos.getX() + 0.5 + (targetWorld.random.nextDouble() - 0.5) * 2,
                    teleportPos.getY() + targetWorld.random.nextDouble() * 2,
                    teleportPos.getZ() + 0.5 + (targetWorld.random.nextDouble() - 0.5) * 2,
                    1, 0, 0, 0, 0.1);
        }

        player.sendMessage(Text.literal("Warped to lodestone!")
                .formatted(Formatting.GOLD), true);

        return true;
    }

    public static int getCharge(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(CHARGE_KEY)) {
            return nbt.getInt(CHARGE_KEY);
        }
        return 0;
    }

    public static void setCharge(ItemStack stack, int charge) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putInt(CHARGE_KEY, Math.min(charge, MAX_CHARGE));
    }

    public static boolean isFullyCharged(ItemStack stack) {
        return getCharge(stack) >= MAX_CHARGE;
    }

    private void bindToLodestone(ItemStack stack, BlockPos pos, World world) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putInt(LODESTONE_POS_KEY + "X", pos.getX());
        nbt.putInt(LODESTONE_POS_KEY + "Y", pos.getY());
        nbt.putInt(LODESTONE_POS_KEY + "Z", pos.getZ());
        nbt.putString(LODESTONE_DIMENSION_KEY, world.getRegistryKey().getValue().toString());
    }

    private void clearBinding(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            nbt.remove(LODESTONE_POS_KEY + "X");
            nbt.remove(LODESTONE_POS_KEY + "Y");
            nbt.remove(LODESTONE_POS_KEY + "Z");
            nbt.remove(LODESTONE_DIMENSION_KEY);
        }
    }

    public static boolean hasBoundLodestone(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(LODESTONE_POS_KEY + "X") && nbt.contains(LODESTONE_DIMENSION_KEY);
    }

    @Nullable
    private BlockPos getBoundLodestonePos(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(LODESTONE_POS_KEY + "X")) {
            return new BlockPos(
                    nbt.getInt(LODESTONE_POS_KEY + "X"),
                    nbt.getInt(LODESTONE_POS_KEY + "Y"),
                    nbt.getInt(LODESTONE_POS_KEY + "Z")
            );
        }
        return null;
    }

    @Nullable
    private String getBoundLodestoneDimension(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(LODESTONE_DIMENSION_KEY)) {
            return nbt.getString(LODESTONE_DIMENSION_KEY);
        }
        return null;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        int charge = getCharge(stack);

        if (charge >= MAX_CHARGE) {
            tooltip.add(Text.literal("Fully Charged").formatted(Formatting.GOLD));
        } else {
            tooltip.add(Text.literal("Charge: " + charge + "/" + MAX_CHARGE + " ender pearls")
                    .formatted(Formatting.GRAY));
        }

        if (hasBoundLodestone(stack)) {
            BlockPos pos = getBoundLodestonePos(stack);
            String dim = getBoundLodestoneDimension(stack);
            if (pos != null && dim != null) {
                String dimName = dim.replace("minecraft:", "").replace("_", " ");
                dimName = dimName.substring(0, 1).toUpperCase() + dimName.substring(1);

                tooltip.add(Text.literal("Bound to: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                        .formatted(Formatting.GOLD));
                tooltip.add(Text.literal("Dimension: " + dimName)
                        .formatted(Formatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Text.literal("Not bound - Right-click a lodestone")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Click with Ender Pearl to charge")
                .formatted(Formatting.DARK_GRAY));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return false;
    }
}
