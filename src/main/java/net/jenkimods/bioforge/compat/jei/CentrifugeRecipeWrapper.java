package net.jenkimods.bioforge.compat.jei;

import net.jenkimods.bioforge.world.centrifuge.CentrifugeIngredient;
import net.jenkimods.bioforge.world.centrifuge.CentrifugeRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class CentrifugeRecipeWrapper {

    private final CentrifugeRecipe recipe;
    private final List<ItemStack> inputs;
    private final List<ItemStack> outputs;

    public CentrifugeRecipeWrapper(CentrifugeRecipe recipe) {
        this.recipe = recipe;
        this.inputs = resolveIngredient(recipe.input());
        this.outputs = resolveIngredient(recipe.output());
    }

    private static List<ItemStack> resolveIngredient(CentrifugeIngredient ingredient) {
        List<ItemStack> result = new ArrayList<>();
        if (ingredient.isTag()) {
            ForgeRegistries.ITEMS.tags()
                    .getTag(net.minecraft.tags.ItemTags.create(
                            net.minecraft.resources.ResourceLocation.tryParse(
                                    ingredient.toString().substring(1))))
                    .stream()
                    .map(ItemStack::new)
                    .forEach(result::add);
        } else {
            Item item = ingredient.resolveItem(net.minecraft.util.RandomSource.create());
            if (item != null) result.add(new ItemStack(item));
        }
        return result;
    }

    public List<ItemStack> getInputs() {
        return inputs;
    }

    public List<ItemStack> getOutputs() {
        return outputs;
    }

    public int getProcessingTime() {
        return recipe.processingTime();
    }

    public boolean isCopyBloodData() {
        return recipe.copyBloodData();
    }

    public CentrifugeRecipe getRecipe() {
        return recipe;
    }
}
