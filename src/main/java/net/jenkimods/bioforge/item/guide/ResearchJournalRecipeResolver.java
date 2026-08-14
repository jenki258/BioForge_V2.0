package net.jenkimods.bioforge.item.guide;

import net.jenkimods.bioforge.api.guide.ResearchJournalRecipeReference;
import net.jenkimods.bioforge.api.guide.ResearchJournalRecipeView;
import net.jenkimods.bioforge.crispr.BioForgeResearchData;
import net.jenkimods.bioforge.world.laboratory.LaboratoryProcessRecipe;
import net.jenkimods.bioforge.world.laboratory.LaboratoryProcessRecipeManager;
import net.jenkimods.bioforge.world.vaccine.VaccineMakerRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ResearchJournalRecipeResolver {
    private ResearchJournalRecipeResolver() {}

    public static List<ResearchJournalRecipeView> resolve(
            ServerPlayer player, List<ResearchJournalRecipeReference> references) {
        Map<String, ResearchJournalRecipeView> resolved = new LinkedHashMap<>();
        for (ResearchJournalRecipeReference reference : references) {
            resolve(player, reference).ifPresent(view -> resolved.putIfAbsent(
                    reference.type().serializedName() + '|' + reference.id(), view));
        }
        return List.copyOf(resolved.values());
    }

    private static Optional<ResearchJournalRecipeView> resolve(
            ServerPlayer player, ResearchJournalRecipeReference reference) {
        return switch (reference.type()) {
            case CRAFTING -> player.serverLevel().getRecipeManager()
                    .byKey(reference.id()).map(recipe -> vanilla(player, recipe));
            case LABORATORY -> LaboratoryProcessRecipeManager.INSTANCE.recipes().stream()
                    .filter(recipe -> recipe.id().equals(reference.id()))
                    .findFirst().map(ResearchJournalRecipeResolver::laboratory);
            case VACCINE_MAKER -> BioForgeResearchData.recipes().stream()
                    .filter(recipe -> recipe.id().equals(reference.id()))
                    .findFirst().map(ResearchJournalRecipeResolver::vaccineMaker);
        };
    }

    private static ResearchJournalRecipeView vanilla(ServerPlayer player, Recipe<?> recipe) {
        int width = recipe instanceof ShapedRecipe shaped
                ? Math.max(1, shaped.getWidth()) : 3;
        int height = recipe instanceof ShapedRecipe shaped
                ? Math.max(1, shaped.getHeight())
                : Math.max(1, (recipe.getIngredients().size() + 2) / 3);
        ItemStack result = recipe.getResultItem(
                player.serverLevel().registryAccess()).copy();
        return new ResearchJournalRecipeView(recipe.getId(), Component.translatable(
                "gui.bioforge.research_journal.station.crafting"), width, height,
                choices(recipe.getIngredients()), List.of(result));
    }

    private static ResearchJournalRecipeView laboratory(LaboratoryProcessRecipe recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        outputs.add(recipe.result());
        if (!recipe.waste().isEmpty()) outputs.add(recipe.waste());
        int width = Math.min(3, Math.max(1, recipe.ingredients().size()));
        int height = Math.max(1, (recipe.ingredients().size() + width - 1) / width);
        return new ResearchJournalRecipeView(recipe.id(), Component.translatable(
                "jei.bioforge.category." + recipe.station().getSerializedName()),
                width, height, choices(recipe.ingredients()), outputs);
    }

    private static ResearchJournalRecipeView vaccineMaker(VaccineMakerRecipe recipe) {
        List<Ingredient> inputs = new ArrayList<>();
        add(inputs, recipe.sample());
        add(inputs, recipe.carrier());
        add(inputs, recipe.reagent());
        add(inputs, recipe.report());
        add(inputs, recipe.cartridge());
        add(inputs, recipe.casModule());

        List<ItemStack> outputs = new ArrayList<>();
        if (recipe.fullResult() != null) outputs.add(new ItemStack(recipe.fullResult()));
        recipe.directedResults().values().stream().distinct()
                .forEach(item -> outputs.add(new ItemStack(item)));
        if (outputs.isEmpty()) {
            ItemStack[] carrierChoices = recipe.carrier().getItems();
            if (carrierChoices.length > 0) outputs.add(carrierChoices[0].copy());
        }
        return new ResearchJournalRecipeView(recipe.id(), Component.translatable(
                "jei.bioforge.category.vaccine_maker"), 3,
                Math.max(1, (inputs.size() + 2) / 3), choices(inputs), outputs);
    }

    private static void add(List<Ingredient> ingredients, Ingredient ingredient) {
        if (ingredient != null && !ingredient.isEmpty()) ingredients.add(ingredient);
    }

    private static List<List<ItemStack>> choices(List<Ingredient> ingredients) {
        List<List<ItemStack>> result = new ArrayList<>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            List<ItemStack> alternatives = new ArrayList<>();
            for (ItemStack choice : ingredient.getItems()) {
                ItemStack display = choice.copy();
                display.setCount(1);
                alternatives.add(display);
            }
            result.add(List.copyOf(alternatives));
        }
        return List.copyOf(result);
    }
}
