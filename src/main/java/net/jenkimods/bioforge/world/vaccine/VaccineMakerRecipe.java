package net.jenkimods.bioforge.world.vaccine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.jenkimods.bioforge.crispr.VaccineTargetCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumMap;
import java.util.Map;

public record VaccineMakerRecipe(
        ResourceLocation id,
        VaccineMakerOperation operation,
        Ingredient sample,
        Ingredient carrier,
        Ingredient reagent,
        Ingredient report,
        Ingredient cartridge,
        Ingredient casModule,
        ResourceLocation guideProfile,
        int processingTime,
        boolean requiresProgram,
        boolean consumeSample,
        float minimumQuality,
        float[] guideWeights,
        float casWeight,
        float sampleWeight,
        float carrierWeight,
        float reagentWeight,
        int uses,
        float defenseRisk,
        Item fullResult,
        Map<VaccineTargetCategory, Item> directedResults,
        Map<VaccineTargetCategory, ResourceLocation> directedActions,
        VaccineTargetCategory fixedDirectedCategory,
        float baseQualityCap,
        float findingBonus,
        float completeBloodBonus,
        float identifiedImprintBonus,
        boolean consumeReagent,
        boolean consumeReport,
        float errorMutationChance,
        float errorMutationThreshold,
        boolean consumeReagentOnMutation
) {
    public VaccineMakerRecipe {
        processingTime = Math.max(1, processingTime);
        minimumQuality = Mth.clamp(minimumQuality, 0.0f, 1.0f);
        if (guideWeights.length != 3) {
            throw new IllegalArgumentException("guide_weights must contain exactly three values");
        }
        for (float weight : guideWeights) {
            if (!Float.isFinite(weight) || weight < 0.0f) {
                throw new IllegalArgumentException("Quality weights must be finite and non-negative");
            }
        }
        if (!Float.isFinite(casWeight) || !Float.isFinite(sampleWeight)
                || !Float.isFinite(carrierWeight) || !Float.isFinite(reagentWeight)
                || casWeight < 0 || sampleWeight < 0
                || carrierWeight < 0 || reagentWeight < 0) {
            throw new IllegalArgumentException(
                    "Quality weights must be finite and non-negative");
        }
        guideWeights = guideWeights.clone();
        directedResults = Map.copyOf(directedResults);
        directedActions = Map.copyOf(directedActions);
        uses = Mth.clamp(uses, 1, 64);
        defenseRisk = Mth.clamp(defenseRisk, 0.0f, 1.0f);
        if (!Float.isFinite(baseQualityCap) || !Float.isFinite(findingBonus)
                || !Float.isFinite(completeBloodBonus)
                || !Float.isFinite(identifiedImprintBonus)
                || !Float.isFinite(errorMutationChance)
                || !Float.isFinite(errorMutationThreshold)) {
            throw new IllegalArgumentException(
                    "Research and failure values must be finite");
        }
        baseQualityCap = Mth.clamp(baseQualityCap, 0.0f, 1.0f);
        findingBonus = Mth.clamp(findingBonus, 0.0f, 1.0f);
        completeBloodBonus = Mth.clamp(completeBloodBonus, 0.0f, 1.0f);
        identifiedImprintBonus = Mth.clamp(identifiedImprintBonus, 0.0f, 1.0f);
        errorMutationChance = Mth.clamp(errorMutationChance, 0.0f, 1.0f);
        errorMutationThreshold = Mth.clamp(errorMutationThreshold, 0.0f, 1.0f);
        if ((operation == VaccineMakerOperation.FULL
                || operation == VaccineMakerOperation.RANDOM_MUTATION)
                && fullResult == null) {
            throw new IllegalArgumentException("Vaccine recipe requires result.item");
        }
        if (operation == VaccineMakerOperation.DIRECTED
                && (directedResults.isEmpty() || directedActions.isEmpty())) {
            throw new IllegalArgumentException(
                    "Directed vaccine recipe requires category results and actions");
        }
    }

    public static VaccineMakerRecipe fromJson(ResourceLocation id, JsonObject json) {
        VaccineMakerOperation operation = VaccineMakerOperation.fromName(
                GsonHelper.getAsString(json, "operation"));
        JsonObject inputs = GsonHelper.getAsJsonObject(json, "inputs");
        Ingredient sample = ingredient(inputs, "sample");
        Ingredient carrier = ingredient(inputs, "carrier");
        Ingredient reagent = ingredient(inputs, "reagent");
        Ingredient report = inputs.has("report")
                ? ingredient(inputs, "report") : Ingredient.EMPTY;
        Ingredient cartridge = inputs.has("cartridge")
                ? ingredient(inputs, "cartridge") : Ingredient.EMPTY;
        Ingredient cas = inputs.has("cas_module")
                ? ingredient(inputs, "cas_module") : Ingredient.EMPTY;

        JsonObject quality = json.has("quality")
                ? GsonHelper.getAsJsonObject(json, "quality") : new JsonObject();
        float[] guideWeights = readGuideWeights(quality);
        JsonObject result = GsonHelper.getAsJsonObject(json, "result");
        Item fullResult = result.has("item") ? resolveItem(
                GsonHelper.getAsString(result, "item")) : null;
        EnumMap<VaccineTargetCategory, Item> directedResults =
                new EnumMap<>(VaccineTargetCategory.class);
        EnumMap<VaccineTargetCategory, ResourceLocation> directedActions =
                new EnumMap<>(VaccineTargetCategory.class);
        if (result.has("items")) {
            JsonObject items = GsonHelper.getAsJsonObject(result, "items");
            for (VaccineTargetCategory category : VaccineTargetCategory.values()) {
                if (items.has(category.serializedName())) {
                    directedResults.put(category, resolveItem(
                            GsonHelper.getAsString(items, category.serializedName())));
                }
            }
        }
        if (result.has("actions")) {
            JsonObject actions = GsonHelper.getAsJsonObject(result, "actions");
            for (VaccineTargetCategory category : VaccineTargetCategory.values()) {
                if (actions.has(category.serializedName())) {
                    ResourceLocation action = ResourceLocation.tryParse(
                            GsonHelper.getAsString(actions, category.serializedName()));
                    if (action == null) {
                        throw new IllegalArgumentException(
                                "Invalid action ID for " + category.serializedName());
                    }
                    directedActions.put(category, action);
                }
            }
        }
        ResourceLocation profile = ResourceLocation.tryParse(
                GsonHelper.getAsString(json, "guide_profile", "bioforge:default"));
        if (profile == null) throw new IllegalArgumentException("Invalid guide_profile");
        VaccineTargetCategory fixedCategory = json.has("directed_category")
                ? VaccineTargetCategory.fromName(
                GsonHelper.getAsString(json, "directed_category")) : null;
        if (json.has("directed_category") && fixedCategory == null) {
            throw new IllegalArgumentException("Invalid directed_category");
        }
        JsonObject research = json.has("research")
                ? GsonHelper.getAsJsonObject(json, "research") : new JsonObject();
        JsonObject failure = json.has("failure")
                ? GsonHelper.getAsJsonObject(json, "failure") : new JsonObject();

        return new VaccineMakerRecipe(
                id, operation, sample, carrier, reagent, report, cartridge, cas, profile,
                GsonHelper.getAsInt(json, "processing_time", 200),
                GsonHelper.getAsBoolean(json, "requires_program",
                        operation != VaccineMakerOperation.CLONE),
                GsonHelper.getAsBoolean(json, "consume_sample",
                        operation != VaccineMakerOperation.CLONE),
                GsonHelper.getAsFloat(json, "minimum_quality", 0.0f),
                guideWeights,
                GsonHelper.getAsFloat(quality, "cas_module", 0.10f),
                GsonHelper.getAsFloat(quality, "sample", 0.05f),
                GsonHelper.getAsFloat(quality, "carrier", 0.05f),
                GsonHelper.getAsFloat(quality, "reagent", 0.05f),
                GsonHelper.getAsInt(result, "uses", 1),
                GsonHelper.getAsFloat(result, "defense_risk", 0.18f),
                fullResult, directedResults, directedActions, fixedCategory,
                GsonHelper.getAsFloat(research, "base_quality_cap", 1.0f),
                GsonHelper.getAsFloat(research, "finding_bonus", 0.0f),
                GsonHelper.getAsFloat(research, "complete_blood_bonus", 0.0f),
                GsonHelper.getAsFloat(research, "identified_imprint_bonus", 0.0f),
                GsonHelper.getAsBoolean(json, "consume_reagent", true),
                GsonHelper.getAsBoolean(json, "consume_report", false),
                GsonHelper.getAsFloat(failure, "mutation_chance", 0.0f),
                GsonHelper.getAsFloat(failure, "mutation_below_quality", 0.95f),
                GsonHelper.getAsBoolean(failure, "consume_reagent", true)
        );
    }

    private static Ingredient ingredient(JsonObject inputs, String key) {
        JsonElement element = inputs.get(key);
        if (element == null) {
            throw new IllegalArgumentException("Vaccine Maker recipe is missing input '" + key + "'");
        }
        return Ingredient.fromJson(element);
    }

    private static float[] readGuideWeights(JsonObject quality) {
        if (!quality.has("guides")) return new float[]{0.25f, 0.25f, 0.25f};
        var array = GsonHelper.getAsJsonArray(quality, "guides");
        if (array.size() != 3) {
            throw new IllegalArgumentException("quality.guides must contain exactly three weights");
        }
        return new float[]{
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat()
        };
    }

    private static Item resolveItem(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        Item item = location == null ? null : ForgeRegistries.ITEMS.getValue(location);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            throw new IllegalArgumentException("Unknown item: " + id);
        }
        return item;
    }

    public boolean matches(ItemStack sampleStack, ItemStack carrierStack,
                           ItemStack reagentStack, ItemStack reportStack,
                           ItemStack casStack) {
        return sample.test(sampleStack) && carrier.test(carrierStack)
                && reagent.test(reagentStack)
                && (report.isEmpty() ? reportStack.isEmpty() : report.test(reportStack))
                && (!requiresProgram || casModule.test(casStack));
    }

    public boolean requiresReport() {
        return !report.isEmpty();
    }

    public boolean matchesWithoutReagent(ItemStack sampleStack, ItemStack carrierStack,
                                         ItemStack casStack) {
        return sample.test(sampleStack) && carrier.test(carrierStack)
                && (!requiresProgram || casModule.test(casStack));
    }

    public Item directedResult(VaccineTargetCategory category) {
        return directedResults.get(category);
    }

    public ResourceLocation directedAction(VaccineTargetCategory category) {
        return directedActions.get(category);
    }

    public float totalWeight() {
        return guideWeights[0] + guideWeights[1] + guideWeights[2]
                + casWeight + sampleWeight + carrierWeight + reagentWeight;
    }
}
