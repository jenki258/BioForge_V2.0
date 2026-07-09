package net.jenkimods.bioforge.world.incubator;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.infection.symptoms.SymptomKey;
import net.jenkimods.bioforge.item.CatalystVialItem;
import net.jenkimods.bioforge.item.NutrientMediumItem;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
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
                return stack.getItem() instanceof CatalystVialItem
                        && CatalystVialItem.getPathogenOrRandom(stack) != null
                        && CatalystVialItem.getCharges(stack) > 0;
            }
            return stack.getItem() instanceof NutrientMediumItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return (slot >= 1 && slot <= 3) ? 1 : super.getSlotLimit(slot);
        }
    };

    private LazyOptional<ItemStackHandler> lazyHandler = LazyOptional.of(() -> items);
    private int progress = 0;
    private int maxProgress = 800;

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

        ItemStack catalyst = be.items.getStackInSlot(0);
        PathogenType pathogen = CatalystVialItem.getPathogenOrRandom(catalyst);
        if (pathogen == null || CatalystVialItem.getCharges(catalyst) <= 0) {
            be.progress = 0;
            return;
        }

        int nutrientCount = 0;
        for (int i = 1; i <= 3; i++) {
            if (be.items.getStackInSlot(i).getItem() instanceof NutrientMediumItem) {
                nutrientCount++;
            }
        }
        if (nutrientCount == 0) {
            be.progress = 0;
            return;
        }

        be.progress++;
        if (be.progress >= be.maxProgress) {
            be.progress = 0;

            for (int i = 1; i <= 3; i++) {
                ItemStack stack = be.items.getStackInSlot(i);
                if (stack.getItem() instanceof NutrientMediumItem) {
                    stack.shrink(1);
                    ItemStack output = new ItemStack(BioForge.VIRUS_SAMPLE.get());
                    StrainData strain = generateRandomStrain(pathogen);
                    NbtObfuscator.writeString(output.getOrCreateTag(), strain.toPayload());
                    be.items.setStackInSlot(i, output);
                }
            }

            CatalystVialItem.consumeCharge(catalyst);
        }
        be.setChanged();
    }

    private static StrainData generateRandomStrain(PathogenType pathogen) {
        StrainData strain = StrainData.createEmpty();
        strain.setPathogen(pathogen);
        strain.setColonyId(UUID.randomUUID());

        List<InfectionType> allowed = new ArrayList<>(pathogen.getAllowedTransmissions());
        if (!allowed.isEmpty()) {
            Collections.shuffle(allowed);
            int count = 1 + new Random().nextInt(allowed.size());
            for (int i = 0; i < count; i++) {
                strain.getInfectionTypes().add(allowed.get(i));
            }
        }

        Random rand = new Random();
        Map<SymptomKey<?>, float[]> ranges = BioForgeSymptoms.getDefaultRanges(pathogen);

        for (Map.Entry<String, SymptomKey<?>> entry : BioForgeSymptoms.getAllSymptomKeys().entrySet()) {
            SymptomKey<?> key = entry.getValue();
            String keyId = entry.getKey();

            if (key.getType() == Float.class) {
                float[] minMax = ranges.get(key);
                if (minMax != null) {
                    float value = minMax[0] + rand.nextFloat() * (minMax[1] - minMax[0]);
                    strain.getSymptoms().put(keyId, String.valueOf(value));
                }
            } else if (key.getType() == Boolean.class) {
                strain.getSymptoms().put(keyId, String.valueOf(rand.nextBoolean()));
            } else if (key.getType().isEnum()) {
                Object[] constants = key.getType().getEnumConstants();
                if (constants != null && constants.length > 0) {
                    int idx = rand.nextInt(constants.length);
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

    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("inv", items.serializeNBT()); tag.putInt("progress", progress); }
    @Override public void load(CompoundTag tag) { super.load(tag); items.deserializeNBT(tag.getCompound("inv")); progress = tag.getInt("progress"); }
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