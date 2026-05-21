package net.jenkimods.bioforge.compat.jei;

import net.jenkimods.bioforge.world.decalcification.DecalcificationRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class DecalcificationRecipeWrapper {

    private final DecalcificationRecipe recipe;
    private final List<ItemStack> input;
    private final List<ItemStack> output;

    public DecalcificationRecipeWrapper(DecalcificationRecipe recipe) {
        this.recipe = recipe;
        this.input = resolveStacks(recipe.input());
        this.output = resolveStacks(recipe.output());
    }

    private static List<ItemStack> resolveStacks(String str) {
        List<ItemStack> result = new ArrayList<>();
        if (str.startsWith("#")) {
            ResourceLocation loc = ResourceLocation.tryParse(str.substring(1));
            if (loc == null) return result;
            ForgeRegistries.ITEMS.tags()
                    .getTag(ItemTags.create(loc))
                    .stream()
                    .map(ItemStack::new)
                    .forEach(result::add);
        } else {
            ResourceLocation loc = ResourceLocation.tryParse(str);
            if (loc == null) return result;
            Item item = ForgeRegistries.ITEMS.getValue(loc);
            if (item != null) result.add(new ItemStack(item));
        }
        return result;
    }

    public List<ItemStack> getInputs() {
        return input;
    }

    public List<ItemStack> getOutputs() {
        return output;
    }

    public boolean isCopyBloodData() {
        return recipe.copyBloodData();
    }

    public DecalcificationRecipe getRecipe() {
        return recipe;
    }
}