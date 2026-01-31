package net.vainnglory.egoistical.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.vainnglory.egoistical.util.ModRarities;

public class MarksmanProofItem extends Item {
    private final ModRarities rarity;

    public MarksmanProofItem(Settings settings, ModRarities rarity) {
        super(settings);
        this.rarity = rarity;
    }

    @Override
    public Text getName(ItemStack stack) {
        return super.getName(stack).copy().setStyle(Style.EMPTY.withColor(rarity.color));
    }
}
