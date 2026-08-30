package net.jenkimods.bioforge.world.recipe;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.jenkimods.bioforge.world.laboratory.LaboratoryProcessRecipe;
import net.jenkimods.bioforge.world.laboratory.LaboratoryProcessRecipeManager;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record LaboratoryDataRecipe(ResourceLocation id, JsonObject source,
                                   LaboratoryProcessRecipe recipe)
        implements Recipe<Container> {
    private static final Gson GSON = new Gson();

    public static LaboratoryDataRecipe fromJson(ResourceLocation id, JsonObject json) {
        return new LaboratoryDataRecipe(id, json.deepCopy(),
                LaboratoryProcessRecipeManager.parseRecipe(id, json));
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return recipe.result().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= recipe.ingredients().size();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return recipe.result().copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.addAll(recipe.ingredients());
        return ingredients;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BioForgeRecipeRegistration.LABORATORY_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return BioForgeRecipeRegistration.LABORATORY_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final class Serializer implements RecipeSerializer<LaboratoryDataRecipe> {
        @Override
        public LaboratoryDataRecipe fromJson(ResourceLocation id, JsonObject json) {
            return LaboratoryDataRecipe.fromJson(id, json);
        }

        @Nullable
        @Override
        public LaboratoryDataRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return fromJson(id, GSON.fromJson(buffer.readUtf(), JsonObject.class));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, LaboratoryDataRecipe recipe) {
            buffer.writeUtf(GSON.toJson(recipe.source()));
        }
    }
}
