package net.jenkimods.bioforge.world.incubator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

public final class IncubatorRecipeSerializer implements RecipeSerializer<IncubatorRecipe> {

    @Override
    public IncubatorRecipe fromJson(ResourceLocation id, JsonObject json) {
        IncubatorIngredient primary = IncubatorIngredient.parse(
                GsonHelper.getAsString(json, "primary_input")
        );
        IncubatorIngredient secondary = IncubatorIngredient.parse(
                GsonHelper.getAsString(json, "secondary_input")
        );
        IncubatorIngredient output = IncubatorIngredient.parse(
                GsonHelper.getAsString(json, "output")
        );
        if (output.isAny() || output.isDirectAir()) {
            throw new JsonParseException("'output' must be an item or tag containing non-air items");
        }
        int outputCount = GsonHelper.getAsInt(json, "output_count", 1);
        if (outputCount < 1 || outputCount > 64) {
            throw new JsonParseException("'output_count' must be between 1 and 64");
        }

        IncubatorOperation operation = IncubatorOperation.parse(
                GsonHelper.getAsString(json, "operation")
        );
        int processingTime = GsonHelper.getAsInt(json, "processing_time", 200);
        if (processingTime < 1) {
            throw new JsonParseException("'processing_time' must be at least 1");
        }

        int defaultPrimaryCost = operation == IncubatorOperation.GENERATE_STRAIN ? 0 : 1;
        int primaryItemCost = GsonHelper.getAsInt(json, "primary_item_cost", defaultPrimaryCost);
        String defaultCostMode = operation == IncubatorOperation.CRAFT ? "per_output" : "per_batch";
        String costMode = GsonHelper.getAsString(json, "primary_cost_mode", defaultCostMode);
        boolean primaryCostPerOutput;
        if ("per_output".equals(costMode)) {
            primaryCostPerOutput = true;
        } else if ("per_batch".equals(costMode)) {
            primaryCostPerOutput = false;
        } else {
            throw new JsonParseException("'primary_cost_mode' must be 'per_output' or 'per_batch'");
        }
        int defaultChargeCost = operation == IncubatorOperation.GENERATE_STRAIN ? 1 : 0;
        int catalystChargeCost = GsonHelper.getAsInt(json, "catalyst_charge_cost", defaultChargeCost);
        if (primaryItemCost < 0 || catalystChargeCost < 0) {
            throw new JsonParseException("Incubator costs cannot be negative");
        }
        if (operation == IncubatorOperation.GENERATE_STRAIN && catalystChargeCost < 1) {
            throw new JsonParseException("'generate_strain' requires at least one catalyst charge");
        }
        if (operation != IncubatorOperation.GENERATE_STRAIN && catalystChargeCost != 0) {
            throw new JsonParseException(
                    "'catalyst_charge_cost' can only be used by 'generate_strain'"
            );
        }

        IncubatorIngredient jeiInput = null;
        if (json.has("jei_input")) {
            jeiInput = IncubatorIngredient.parse(GsonHelper.getAsString(json, "jei_input"));
            if (jeiInput.isAny()) {
                throw new JsonParseException("'jei_input' cannot be the wildcard '*'");
            }
        } else if (primary.isAny()) {
            throw new JsonParseException("Wildcard 'primary_input' requires a concrete 'jei_input'");
        }

        return new IncubatorRecipe(
                id,
                primary,
                secondary,
                output,
                outputCount,
                operation,
                processingTime,
                primaryItemCost,
                primaryCostPerOutput,
                catalystChargeCost,
                jeiInput
        );
    }

    @Nullable
    @Override
    public IncubatorRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        IncubatorIngredient primary = IncubatorIngredient.parse(buffer.readUtf());
        IncubatorIngredient secondary = IncubatorIngredient.parse(buffer.readUtf());
        IncubatorIngredient output = IncubatorIngredient.parse(buffer.readUtf());
        int outputCount = buffer.readVarInt();
        IncubatorOperation operation = IncubatorOperation.parse(buffer.readUtf());
        int processingTime = buffer.readVarInt();
        int primaryItemCost = buffer.readVarInt();
        boolean primaryCostPerOutput = buffer.readBoolean();
        int catalystChargeCost = buffer.readVarInt();
        IncubatorIngredient jeiInput = buffer.readBoolean()
                ? IncubatorIngredient.parse(buffer.readUtf())
                : null;
        return new IncubatorRecipe(
                id,
                primary,
                secondary,
                output,
                outputCount,
                operation,
                processingTime,
                primaryItemCost,
                primaryCostPerOutput,
                catalystChargeCost,
                jeiInput
        );
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, IncubatorRecipe recipe) {
        buffer.writeUtf(recipe.primaryInput().toString());
        buffer.writeUtf(recipe.secondaryInput().toString());
        buffer.writeUtf(recipe.output().toString());
        buffer.writeVarInt(recipe.outputCount());
        buffer.writeUtf(recipe.operation().serializedName());
        buffer.writeVarInt(recipe.processingTime());
        buffer.writeVarInt(recipe.primaryItemCost());
        buffer.writeBoolean(recipe.primaryCostPerOutput());
        buffer.writeVarInt(recipe.catalystChargeCost());
        buffer.writeBoolean(recipe.jeiInput() != null);
        if (recipe.jeiInput() != null) {
            buffer.writeUtf(recipe.jeiInput().toString());
        }
    }
}
