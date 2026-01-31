package net.vainnglory.egoistical.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.vainnglory.egoistical.Egoistical;


public class ModItemGroups {
    public static final ItemGroup EGO = Registry.register(Registries.ITEM_GROUP,
            new Identifier(Egoistical.MOD_ID, "ego"),
            FabricItemGroup.builder().displayName(Text.translatable("itemgroup.ego"))
                    .icon(() -> new ItemStack(ModItems.TRACKER)).entries((displayContext, entries) -> {
                        entries.add(ModItems.TRACKER);
                        entries.add(ModItems.GREED_RUNE);
                        entries.add(ModItems.ADRENALINE_SHOT_EMPTY);
                        entries.add(ModItems.ADRENALINE_SHOT_FILLED);
                        entries.add(ModItems.PORTABLE_STASIS);
                        entries.add(ModItems.EMP);
                        entries.add(ModItems.THORNED_INGOT);
                        entries.add(ModItems.TRICK_BAG);
                        entries.add(ModItems.MARKSMANS_PROOF);

                    }).build());

    public static void registerItemGroups() {
        Egoistical.LOGGER.info("Registering Item Groups for " +Egoistical.MOD_ID);
    }
}
