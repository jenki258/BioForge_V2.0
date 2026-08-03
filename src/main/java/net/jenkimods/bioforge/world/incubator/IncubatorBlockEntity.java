package net.jenkimods.bioforge.world.incubator;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.infection.symptoms.SymptomKey;
import net.jenkimods.bioforge.item.*;
import net.jenkimods.bioforge.item.reagents.CatalystVialItem;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class IncubatorBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler items = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) {
                return hasPrimaryRecipe(stack);
            }
            return hasSecondaryRecipe(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return (slot >= 1 && slot <= 3) ? 1 : super.getSlotLimit(slot);
        }
    };

    private LazyOptional<ItemStackHandler> lazyHandler = LazyOptional.of(() -> items);
    private int progress = 0;
    private int maxProgress = 200;
    @Nullable
    private ResourceLocation activeRecipeId;
    private String activePrimarySignature = "";

    protected final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return index == 0 ? progress : maxProgress; }
        @Override public void set(int index, int value) { if (index == 0) progress = value; else maxProgress = value; }
        @Override public int getCount() { return 2; }
    };

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(BioForge.INCUBATOR_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IncubatorBlockEntity be) {
        if (level.isClientSide()) return;

        Optional<IncubatorRecipe> recipeOptional = be.findRecipe();
        if (recipeOptional.isEmpty()) {
            be.resetProgress();
            return;
        }

        IncubatorRecipe recipe = recipeOptional.get();
        String primarySignature = createPrimarySignature(be.items.getStackInSlot(0));
        if (!recipe.id().equals(be.activeRecipeId)
                || !primarySignature.equals(be.activePrimarySignature)) {
            be.progress = 0;
            be.activeRecipeId = recipe.id();
            be.activePrimarySignature = primarySignature;
        }
        be.maxProgress = recipe.processingTime();
        be.progress++;
        if (be.progress >= be.maxProgress) {
            be.progress = 0;
            be.process(recipe, level.random);
        }
        be.setChanged();
    }

    private Optional<IncubatorRecipe> findRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        ItemStack primary = items.getStackInSlot(0);
        return getRecipes().stream()
                .filter(recipe -> recipe.matchesPrimary(primary))
                .filter(recipe -> {
                    for (int slot = 1; slot <= 3; slot++) {
                        if (recipe.matchesSecondary(items.getStackInSlot(slot))) {
                            return true;
                        }
                    }
                    return false;
                })
                .sorted(Comparator
                        .comparingInt((IncubatorRecipe recipe) -> recipe.primaryInput().specificity())
                        .thenComparingInt(recipe -> recipe.secondaryInput().specificity())
                        .reversed()
                        .thenComparing(recipe -> recipe.id().toString()))
                .findFirst();
    }

    private List<IncubatorRecipe> getRecipes() {
        if (level == null) {
            return List.of();
        }
        return level.getRecipeManager().getAllRecipesFor(IncubatorRecipeRegistration.TYPE);
    }

    private boolean hasPrimaryRecipe(ItemStack stack) {
        return getRecipes().stream().anyMatch(recipe -> recipe.matchesPrimary(stack));
    }

    private boolean hasSecondaryRecipe(ItemStack stack) {
        return getRecipes().stream().anyMatch(recipe -> recipe.matchesSecondary(stack));
    }

    private void resetProgress() {
        if (progress != 0 || activeRecipeId != null) {
            progress = 0;
            activeRecipeId = null;
            activePrimarySignature = "";
            setChanged();
        }
    }

    private static String createPrimarySignature(ItemStack stack) {
        return stack.save(new CompoundTag()).toString();
    }

    private void process(IncubatorRecipe recipe, RandomSource random) {
        ItemStack primary = items.getStackInSlot(0);
        String sourceStrain = recipe.getSourceStrain(primary);
        PathogenType generatedPathogen = null;
        if (recipe.operation() == IncubatorOperation.GENERATE_STRAIN) {
            generatedPathogen = CatalystVialItem.getPathogenOrRandom(primary);
            if (generatedPathogen == null) {
                return;
            }
        }

        boolean produced = false;
        int producedSlots = 0;
        int affordableSlots = recipe.primaryCostPerOutput() && recipe.primaryItemCost() > 0
                ? primary.getCount() / recipe.primaryItemCost()
                : 3;
        for (int slot = 1; slot <= 3; slot++) {
            if (producedSlots >= affordableSlots) {
                break;
            }
            ItemStack secondary = items.getStackInSlot(slot);
            if (!recipe.matchesSecondary(secondary)) {
                continue;
            }

            Item outputItem = recipe.output().resolveItem(random);
            if (outputItem == null) {
                continue;
            }
            int outputCount = Math.min(recipe.outputCount(), outputItem.getMaxStackSize());
            ItemStack output = new ItemStack(outputItem, outputCount);
            if (recipe.operation() == IncubatorOperation.GENERATE_STRAIN) {
                StrainData strain = generateRandomStrain(generatedPathogen, random);
                NbtObfuscator.writeString(output.getOrCreateTag(), strain.toPayload());
            } else if (sourceStrain != null) {
                NbtObfuscator.writeString(output.getOrCreateTag(), sourceStrain);
            } else if (recipe.operation() != IncubatorOperation.CRAFT) {
                continue;
            }

            items.setStackInSlot(slot, output);
            produced = true;
            producedSlots++;
        }

        if (!produced) {
            return;
        }

        if (recipe.catalystChargeCost() > 0) {
            for (int charge = 0; charge < recipe.catalystChargeCost() && !primary.isEmpty(); charge++) {
                CatalystVialItem.consumeCharge(primary);
            }
        }
        if (recipe.primaryItemCost() > 0) {
            int primaryCost = recipe.primaryCostPerOutput()
                    ? recipe.primaryItemCost() * producedSlots
                    : recipe.primaryItemCost();
            primary.shrink(primaryCost);
        }
    }

    private static StrainData generateRandomStrain(PathogenType pathogen, RandomSource random) {
        StrainData strain = StrainData.createEmpty();
        strain.setPathogen(pathogen);
        strain.setColonyId(UUID.randomUUID());
        List<InfectionType> allowed = new ArrayList<>(pathogen.getAllowedTransmissions());
        if (!allowed.isEmpty()) {
            for (int index = allowed.size() - 1; index > 0; index--) {
                Collections.swap(allowed, index, random.nextInt(index + 1));
            }
            int count = 1 + random.nextInt(allowed.size());
            for (int i = 0; i < count; i++) strain.getInfectionTypes().add(allowed.get(i));
        }
        Map<SymptomKey<?>, float[]> ranges = BioForgeSymptoms.getDefaultRanges(pathogen);
        for (Map.Entry<String, SymptomKey<?>> entry : BioForgeSymptoms.getAllSymptomKeys().entrySet()) {
            SymptomKey<?> key = entry.getValue();
            String keyId = entry.getKey();
            if (key.getType() == Float.class) {
                float[] minMax = ranges.get(key);
                if (minMax != null) {
                    float value = minMax[0] + random.nextFloat() * (minMax[1] - minMax[0]);
                    strain.getSymptoms().put(keyId, String.valueOf(value));
                }
            } else if (key.getType() == Boolean.class) {
                strain.getSymptoms().put(keyId, String.valueOf(random.nextBoolean()));
            } else if (key.getType().isEnum()) {
                Object[] constants = key.getType().getEnumConstants();
                if (constants != null && constants.length > 0) {
                    int idx = random.nextInt(constants.length);
                    strain.getSymptoms().put(keyId, ((Enum<?>) constants[idx]).name());
                }
            }
        }
        return strain;
    }

    @Override public Component getDisplayName() { return Component.translatable("block.bioforge.incubator"); }

    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new IncubatorMenu(id, inv, this, data);
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inv", items.serializeNBT());
        tag.putInt("progress", progress);
        if (activeRecipeId != null) {
            tag.putString("active_recipe", activeRecipeId.toString());
        }
        tag.putString("active_primary", activePrimarySignature);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("inv"));
        progress = tag.getInt("progress");
        activeRecipeId = ResourceLocation.tryParse(tag.getString("active_recipe"));
        activePrimarySignature = tag.getString("active_primary");
    }
    @Override public void setRemoved() { super.setRemoved(); lazyHandler.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); lazyHandler = LazyOptional.of(() -> items); }

    public void drops() {
        if (level == null) return;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) Containers.dropItemStack(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, stack);
        }
    }
}
