package net.jenkimods.bioforge.world.recipe;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.jenkimods.bioforge.world.centrifuge.CentrifugeIngredient;
import net.jenkimods.bioforge.world.centrifuge.CentrifugeOutput;
import net.jenkimods.bioforge.world.centrifuge.CentrifugeRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record CentrifugeDataRecipe(ResourceLocation id, JsonObject source,
                                   CentrifugeRecipe recipe) implements Recipe<Container> {
    private static final Gson GSON = new Gson();

    public static CentrifugeDataRecipe fromJson(ResourceLocation id, JsonObject json) {
        String inputValue = GsonHelper.getAsString(json, "input");
        CentrifugeIngredient input = CentrifugeIngredient.parse(inputValue);
        List<CentrifugeOutput> outputs = new ArrayList<>();
        CentrifugeIngredient output = null;
        if (json.has("outputs")) {
            JsonArray values = GsonHelper.getAsJsonArray(json, "outputs");
            for (JsonElement element : values) {
                JsonObject value = GsonHelper.convertToJsonObject(element, "centrifuge output");
                outputs.add(new CentrifugeOutput(CentrifugeIngredient.parse(
                        GsonHelper.getAsString(value, "item")),
                        GsonHelper.getAsInt(value, "weight", 1)));
            }
        } else if (json.has("output")) {
            output = CentrifugeIngredient.parse(GsonHelper.getAsString(json, "output"));
        } else {
            throw new JsonParseException("Recipe must have 'output' or 'outputs'");
        }
        JsonArray keysJson = GsonHelper.getAsJsonArray(json, "copy_nbt_keys", new JsonArray());
        List<String> keys = new ArrayList<>();
        keysJson.forEach(element -> keys.add(element.getAsString()));
        CentrifugeRecipe recipe = new CentrifugeRecipe(input, output, outputs,
                GsonHelper.getAsBoolean(json, "copy_blood_data", false),
                GsonHelper.getAsBoolean(json, "copy_nbt", false), keys,
                GsonHelper.getAsBoolean(json, "copy_infection", false),
                Math.max(1, GsonHelper.getAsInt(json, "processing_time", 100)));
        return new CentrifugeDataRecipe(id, json.deepCopy(), recipe);
    }

    @Override
    public boolean matches(Container container, Level level) {
        return container.getContainerSize() > 0 && recipe.input().test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return getResultItem(registryAccess);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        CentrifugeIngredient output = recipe.output();
        if (output == null && !recipe.outputs().isEmpty()) {
            output = recipe.outputs().get(0).ingredient();
        }
        Item item = output == null ? null : output.resolveItem(RandomSource.create(0L));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BioForgeRecipeRegistration.CENTRIFUGE_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return BioForgeRecipeRegistration.CENTRIFUGE_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final class Serializer implements RecipeSerializer<CentrifugeDataRecipe> {
        @Override
        public CentrifugeDataRecipe fromJson(ResourceLocation id, JsonObject json) {
            return CentrifugeDataRecipe.fromJson(id, json);
        }

        @Nullable
        @Override
        public CentrifugeDataRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return fromJson(id, GSON.fromJson(buffer.readUtf(), JsonObject.class));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CentrifugeDataRecipe recipe) {
            buffer.writeUtf(GSON.toJson(recipe.source()));
        }
    }
}
