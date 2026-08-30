package net.jenkimods.bioforge.world.recipe;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.jenkimods.bioforge.world.decalcification.DecalcificationRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.StreamSupport;

public record DecalcificationDataRecipe(ResourceLocation id, JsonObject source,
                                        DecalcificationRecipe recipe)
        implements Recipe<Container> {
    private static final Gson GSON = new Gson();

    public static DecalcificationDataRecipe fromJson(ResourceLocation id, JsonObject json) {
        JsonArray keysJson = GsonHelper.getAsJsonArray(json, "copy_nbt_keys", new JsonArray());
        List<String> keys = StreamSupport.stream(keysJson.spliterator(), false)
                .map(JsonElement::getAsString).toList();
        DecalcificationRecipe recipe = new DecalcificationRecipe(
                GsonHelper.getAsString(json, "input"),
                GsonHelper.getAsString(json, "output"),
                GsonHelper.getAsBoolean(json, "copy_blood_data", true),
                GsonHelper.getAsBoolean(json, "copy_nbt", false), keys);
        return new DecalcificationDataRecipe(id, json.deepCopy(), recipe);
    }

    @Override
    public boolean matches(Container container, Level level) {
        return container.getContainerSize() > 0 && matches(recipe.input(), container.getItem(0));
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
        Item item = resolveFirst(recipe.output());
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
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
        return BioForgeRecipeRegistration.DECALCIFICATION_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return BioForgeRecipeRegistration.DECALCIFICATION_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    private static boolean matches(String value, ItemStack stack) {
        ResourceLocation id = ResourceLocation.tryParse(value.startsWith("#")
                ? value.substring(1) : value);
        if (id == null) return false;
        return value.startsWith("#")
                ? stack.is(ItemTags.create(id))
                : stack.is(ForgeRegistries.ITEMS.getValue(id));
    }

    private static Item resolveFirst(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value.startsWith("#")
                ? value.substring(1) : value);
        if (id == null) return null;
        if (!value.startsWith("#")) return ForgeRegistries.ITEMS.getValue(id);
        return ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(id)).stream()
                .findFirst().orElse(null);
    }

    public static final class Serializer implements RecipeSerializer<DecalcificationDataRecipe> {
        @Override
        public DecalcificationDataRecipe fromJson(ResourceLocation id, JsonObject json) {
            return DecalcificationDataRecipe.fromJson(id, json);
        }

        @Nullable
        @Override
        public DecalcificationDataRecipe fromNetwork(ResourceLocation id,
                                                      FriendlyByteBuf buffer) {
            return fromJson(id, GSON.fromJson(buffer.readUtf(), JsonObject.class));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, DecalcificationDataRecipe recipe) {
            buffer.writeUtf(GSON.toJson(recipe.source()));
        }
    }
}
