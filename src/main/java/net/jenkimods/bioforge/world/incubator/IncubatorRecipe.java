package net.jenkimods.bioforge.world.incubator;

import net.jenkimods.bioforge.infection.StrainData;
import net.jenkimods.bioforge.item.BloodSampleUtil;
import net.jenkimods.bioforge.item.incubating.LiveCultureVialItem;
import net.jenkimods.bioforge.item.reagents.CatalystVialItem;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
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

public record IncubatorRecipe(
        ResourceLocation id,
        IncubatorIngredient primaryInput,
        IncubatorIngredient secondaryInput,
        IncubatorIngredient output,
        int outputCount,
        IncubatorOperation operation,
        int processingTime,
        int primaryItemCost,
        boolean primaryCostPerOutput,
        int catalystChargeCost,
        @Nullable IncubatorIngredient jeiInput
) implements Recipe<Container> {

    @Override
    public boolean matches(Container container, Level level) {
        if (container.getContainerSize() < 2 || !matchesPrimary(container.getItem(0))) {
            return false;
        }
        for (int slot = 1; slot < Math.min(4, container.getContainerSize()); slot++) {
            if (matchesSecondary(container.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesPrimary(ItemStack stack) {
        if (!primaryInput.test(stack) || stack.getCount() < primaryItemCost) {
            return false;
        }

        return switch (operation) {
            case CRAFT -> true;
            case GENERATE_STRAIN ->
                    stack.getItem() instanceof CatalystVialItem
                            && CatalystVialItem.isSet(stack)
                            && CatalystVialItem.getCharges(stack) >= catalystChargeCost;
            case COPY_SAMPLE_STRAIN -> isValidStrainPayload(readSampleStrain(stack));
            case COPY_BLOOD_STRAIN ->
                    BloodSampleUtil.hasBlood(stack) && isValidStrainPayload(readBloodStrain(stack));
        };
    }

    public boolean matchesSecondary(ItemStack stack) {
        if (!secondaryInput.test(stack)) {
            return false;
        }
        if ((operation == IncubatorOperation.COPY_SAMPLE_STRAIN
                || operation == IncubatorOperation.COPY_BLOOD_STRAIN)
                && stack.getItem() instanceof LiveCultureVialItem) {
            return !LiveCultureVialItem.hasStrain(stack);
        }
        return true;
    }

    @Nullable
    public String getSourceStrain(ItemStack stack) {
        return switch (operation) {
            case CRAFT, GENERATE_STRAIN -> null;
            case COPY_SAMPLE_STRAIN -> readSampleStrain(stack);
            case COPY_BLOOD_STRAIN -> readBloodStrain(stack);
        };
    }

    @Nullable
    private static String readSampleStrain(ItemStack stack) {
        String raw = NbtObfuscator.readString(stack.getOrCreateTag());
        return raw == null || raw.isEmpty() ? null : raw;
    }

    @Nullable
    private static String readBloodStrain(ItemStack stack) {
        String raw = NbtObfuscator.readInfection(stack.getOrCreateTag());
        if (raw == null || raw.isEmpty()) {
            raw = NbtObfuscator.readString(stack.getOrCreateTag());
        }
        return raw == null || raw.isEmpty() ? null : raw;
    }

    private static boolean isValidStrainPayload(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        try {
            return StrainData.parse(raw).getPathogen() != null;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return getResultItem(registryAccess);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        Item item = output.resolveItem(net.minecraft.util.RandomSource.create(0L));
        return item == null ? ItemStack.EMPTY : new ItemStack(item, Math.min(outputCount, item.getMaxStackSize()));
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
        return IncubatorRecipeRegistration.SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return IncubatorRecipeRegistration.TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
