package io.redspace.ironsspellbooks.block.alchemist_cauldron;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class AlchemistCauldronRecipeJson implements IRecipe<Inventory>, Recipe<Container> {
    // Define your recipe fields here
    private final ResourceLocation id;
    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients;

    public AlchemistCauldronRecipeJson(ResourceLocation id, ItemStack result, NonNullList<Ingredient> ingredients) {
        this.id = id;
        this.result = result;
        this.ingredients = ingredients;
    }

    @Override
    public boolean matches(Inventory inv, Level world) {
        // Add logic to check if the ingredients match
        return true;
    }

    @Override
    public ItemStack assemble(Inventory inv) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<AlchemistCauldronRecipeJson> getSerializer() {
        return RecipeSerializer<>.ALCHEMIST_CAULDRON_RECIPE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.ALCHEMIST_CAULDRON;
    }
}
