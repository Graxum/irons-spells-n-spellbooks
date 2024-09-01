package io.redspace.ironsspellbooks.block.alchemist_cauldron;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class AlchemistCauldronRecipeSerializer implements RecipeSerializer<AlchemistCauldronRecipeJson> {
    @Override
    public AlchemistCauldronRecipeJson fromJson(ResourceLocation recipeId, JsonObject json) {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(2, Ingredient.EMPTY);  // Adjust the size accordingly

        JsonArray ingredientsArray = GsonHelper.getAsJsonArray(json, "ingredients");
        for (int i = 0; i < ingredientsArray.size(); i++) {
            ingredients.set(i, Ingredient.fromJson(ingredientsArray.get(i)));
        }

        ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));

        return new AlchemistCauldronRecipeJson(recipeId, result, ingredients);
    }

    @Override
    public AlchemistCauldronRecipeJson fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(buffer.readVarInt(), Ingredient.EMPTY);

        for (int i = 0; i < ingredients.size(); i++) {
            ingredients.set(i, Ingredient.fromNetwork(buffer));
        }

        ItemStack result = buffer.readItem();

        return new AlchemistCauldronRecipeJson(recipeId, result, ingredients);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, AlchemistCauldronRecipeJson recipe) {
        buffer.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ingredient : recipe.getIngredients()) {
            ingredient.toNetwork(buffer);
        }
        buffer.writeItem(recipe.getResultItem());
    }
}
