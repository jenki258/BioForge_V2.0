package net.jenkimods.bioforge.world.recipe;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.jenkimods.bioforge.crispr.VaccineTargetCategory;
import net.jenkimods.bioforge.world.vaccine.VaccineMakerRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record VaccineMakerDataRecipe(ResourceLocation id, JsonObject source,
                                     VaccineMakerRecipe recipe)
        implements Recipe<Container> {
    private static final Gson GSON = new Gson();

    public static VaccineMakerDataRecipe fromJson(ResourceLocation id, JsonObject json) {
        return new VaccineMakerDataRecipe(id, json.deepCopy(),
                VaccineMakerRecipe.fromJson(id, json));
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return getResultItem(registryAccess);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        Item result = recipe.fullResult();
        if (result == null) {
            for (VaccineTargetCategory category : VaccineTargetCategory.values()) {
                result = recipe.directedResult(category);
                if (result != null) break;
            }
        }
        return result == null ? ItemStack.EMPTY : new ItemStack(result);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        add(ingredients, recipe.sample());
        add(ingredients, recipe.carrier());
        add(ingredients, recipe.reagent());
        add(ingredients, recipe.report());
        add(ingredients, recipe.cartridge());
        add(ingredients, recipe.casModule());
        return ingredients;
    }

    private static void add(NonNullList<Ingredient> target, Ingredient ingredient) {
        if (ingredient != null && !ingredient.isEmpty()) target.add(ingredient);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BioForgeRecipeRegistration.VACCINE_MAKER_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return BioForgeRecipeRegistration.VACCINE_MAKER_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final class Serializer implements RecipeSerializer<VaccineMakerDataRecipe> {
        @Override
        public VaccineMakerDataRecipe fromJson(ResourceLocation id, JsonObject json) {
            return VaccineMakerDataRecipe.fromJson(id, json);
        }

        @Nullable
        @Override
        public VaccineMakerDataRecipe fromNetwork(ResourceLocation id,
                                                  FriendlyByteBuf buffer) {
            return fromJson(id, GSON.fromJson(buffer.readUtf(), JsonObject.class));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, VaccineMakerDataRecipe recipe) {
            buffer.writeUtf(GSON.toJson(recipe.source()));
        }
    }
}
