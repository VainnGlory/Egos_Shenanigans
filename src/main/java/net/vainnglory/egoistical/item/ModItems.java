package net.vainnglory.egoistical.item;

import net.minecraft.item.PotionItem;
import net.vainnglory.egoistical.Egoistical;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.vainnglory.egoistical.util.ModRarities;

public class ModItems {
    public static final Item GREED_RUNE = registerItem("greed_rune",
            new Item(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));

    public static final TrackerItem TRACKER = (TrackerItem) registerItem("tracker",
            new TrackerItem(new FabricItemSettings().maxCount(1), ModRarities.GOLDEN));

    public static final AdrenalineShotItem ADRENALINE_SHOT_EMPTY = (AdrenalineShotItem) registerItem("adrenaline_shot_empty",
            new AdrenalineShotItem(new FabricItemSettings().maxCount(16), ModRarities.GOLDEN, false));

    public static final AdrenalineShotItem ADRENALINE_SHOT_FILLED = (AdrenalineShotItem) registerItem("adrenaline_shot_filled",
            new AdrenalineShotItem (new FabricItemSettings().maxCount(1), ModRarities.GOLDEN, true));

    public static final PortableStasisItem PORTABLE_STASIS = (PortableStasisItem) registerItem("portable_stasis",
            new PortableStasisItem(new FabricItemSettings().maxCount(1), ModRarities.ENDER));



    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(Egoistical.MOD_ID, name), item);
    }

    public static void registerModItems() {
        Egoistical.LOGGER.info("Registering Mod Items for " + Egoistical.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(GREED_RUNE);
            content.add(TRACKER);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> {
            content.add(ADRENALINE_SHOT_EMPTY);
            content.add(ADRENALINE_SHOT_FILLED);
        });
    }
}
