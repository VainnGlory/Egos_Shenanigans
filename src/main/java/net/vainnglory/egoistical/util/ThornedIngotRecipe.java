package net.vainnglory.egoistical.util;

import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.vainnglory.egoistical.item.ModItems;

public class ThornedIngotRecipe extends SpecialCraftingRecipe {

    public ThornedIngotRecipe(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        if (inventory.getWidth() < 3 || inventory.getHeight() < 3) {
            return false;
        }

        boolean foundPotion = false;
        int goldCount = 0;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                ItemStack stack = inventory.getStack(col + row * inventory.getWidth());

                if (row == 1 && col == 1) {
                    if (!isHarmingPotion(stack)) {
                        return false;
                    }
                    foundPotion = true;
                } else {
                    if (!stack.isOf(Items.GOLD_INGOT)) {
                        return false;
                    }
                    goldCount++;
                }
            }
        }

        return foundPotion && goldCount == 8;
    }

    private boolean isHarmingPotion(ItemStack stack) {
        if (!stack.isOf(Items.POTION)) {
            return false;
        }
        var potion = PotionUtil.getPotion(stack);
        return potion == Potions.HARMING || potion == Potions.STRONG_HARMING;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        return new ItemStack(ModItems.THORNED_INGOT);
    }

    @Override
    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return new ItemStack(ModItems.THORNED_INGOT);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.THORNED_INGOT_RECIPE_SERIALIZER;
    }
}
