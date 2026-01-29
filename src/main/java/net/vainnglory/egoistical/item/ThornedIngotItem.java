package net.vainnglory.egoistical.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.vainnglory.egoistical.util.ModRarities;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ThornedIngotItem extends Item {
    private final ModRarities rarity;

    public ThornedIngotItem(Settings settings, ModRarities rarity) {
        super(settings);
        this.rarity = rarity;
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);

        return baseName.copy().setStyle(Style.EMPTY.withColor(rarity.color));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("A cursed gold alloy covered in magic thorns.")
                .formatted(Formatting.GOLD, Formatting.ITALIC));
        tooltip.add(Text.empty());
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return false;
    }
}
