package net.jenkimods.bioforge.world.recipe;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.crispr.StrainSampleUtil;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Transfers the complete encoded culture, including its finite splicer reserves. */
public final class CultureSampleRecipe extends CustomRecipe {
    public CultureSampleRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }
    private ItemStack culture(CraftingContainer grid) {
        ItemStack found = ItemStack.EMPTY;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack stack = grid.getItem(i);
            if (stack.isEmpty()) continue;
            if (!found.isEmpty() || !stack.is(BioForge.LIVE_CULTURE_VIAL.get())
                    || StrainSampleUtil.getStrain(stack) == null) return ItemStack.EMPTY;
            found = stack;
        }
        return found;
    }
    @Override public boolean matches(CraftingContainer grid, Level level) { return !culture(grid).isEmpty(); }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        ItemStack source = culture(grid);
        if (source.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(BioForge.VIRUS_SAMPLE.get());
        if (source.hasTag()) result.setTag(source.getTag().copy());
        return result;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width > 0 && height > 0; }
    @Override public ItemStack getResultItem(RegistryAccess access) { return new ItemStack(BioForge.VIRUS_SAMPLE.get()); }
    @Override public NonNullList<Ingredient> getIngredients() {
        var example = net.jenkimods.bioforge.infection.StrainData.createEmpty();
        example.setPathogen(net.jenkimods.bioforge.infection.PathogenType.VIRUS);
        var vial = new ItemStack(BioForge.LIVE_CULTURE_VIAL.get());
        net.jenkimods.bioforge.util.NbtObfuscator.writeString(vial.getOrCreateTag(), example.toPayload());
        return NonNullList.of(Ingredient.EMPTY, Ingredient.of(vial));
    }
    @Override public RecipeSerializer<?> getSerializer() { return BioForgeRecipeRegistration.CULTURE_SAMPLE_SERIALIZER; }
}
