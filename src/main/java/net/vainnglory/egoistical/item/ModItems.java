package net.vainnglory.egoistical.item;

import net.vainnglory.egoistical.Egoistical;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {
    public static final Item GREED_RUNE = registerItem("greed_rune",
            new Item(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));

    public static final TrackerItem TRACKER = (TrackerItem) registerItem("tracker",
            new TrackerItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(Egoistical.MOD_ID, name), item);
    }

    public static void registerModItems() {
        Egoistical.LOGGER.info("Registering Mod Items for " + Egoistical.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(GREED_RUNE);
        });
    }
}
