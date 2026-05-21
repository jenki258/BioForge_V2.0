package net.jenkimods.bioforge.world.centrifuge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.jenkimods.bioforge.BioForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class CentrifugeRecipeManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    public static final CentrifugeRecipeManager INSTANCE = new CentrifugeRecipeManager();
    private final List<CentrifugeRecipe> recipes = new ArrayList<>();

    private CentrifugeRecipeManager() {
        super(GSON, "centrifuge");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        recipes.clear();

        elements.forEach((id, element) -> {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(element, "centrifuge recipe");

                String inputStr = GsonHelper.getAsString(json, "input");
                String outputStr = GsonHelper.getAsString(json, "output");

                CentrifugeIngredient input = CentrifugeIngredient.parse(inputStr);
                CentrifugeIngredient output = CentrifugeIngredient.parse(outputStr);

                boolean copyBloodData = GsonHelper.getAsBoolean(json, "copy_blood_data", true);
                boolean copyNbt = GsonHelper.getAsBoolean(json, "copy_nbt", false);
                int processingTime = Math.max(1, GsonHelper.getAsInt(json, "processing_time", 100));

                JsonArray keysArray = GsonHelper.getAsJsonArray(json, "copy_nbt_keys", new JsonArray());
                List<String> keys = StreamSupport.stream(keysArray.spliterator(), false)
                        .map(JsonElement::getAsString)
                        .toList();

                recipes.add(new CentrifugeRecipe(input, output, copyBloodData, copyNbt, keys, processingTime));
            } catch (Exception ex) {
                BioForge.LOGGER.error("Failed to parse centrifuge recipe {}: {}", id, ex.getMessage());
            }
        });

        BioForge.LOGGER.info("Loaded {} centrifuge recipes", recipes.size());
    }

    public Optional<CentrifugeRecipe> getRecipe(ItemStack inputStack) {
        if (inputStack.isEmpty()) return Optional.empty();
        for (CentrifugeRecipe recipe : recipes) {
            if (recipe.input().test(inputStack)) return Optional.of(recipe);
        }
        return Optional.empty();
    }

    public List<CentrifugeRecipe> getRecipes() {
        return java.util.Collections.unmodifiableList(recipes);
    }
}