package net.vainnglory.egoistical.util;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.vainnglory.egoistical.Egoistical;

public class ModRecipes {

    public static final RecipeSerializer<ThornedIngotRecipe> THORNED_INGOT_RECIPE_SERIALIZER =
            RecipeSerializer.register(
                    Egoistical.MOD_ID + ":thorned_ingot",
                    new SpecialRecipeSerializer<>(ThornedIngotRecipe::new)
            );

    public static void registerRecipes() {
        Egoistical.LOGGER.info("Registering Mod Recipes for " + Egoistical.MOD_ID);
    }
}
