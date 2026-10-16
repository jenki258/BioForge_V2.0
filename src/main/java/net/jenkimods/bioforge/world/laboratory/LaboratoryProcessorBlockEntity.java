package net.jenkimods.bioforge.world.laboratory;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.config.BioForgeServerConfig;
import net.jenkimods.bioforge.block.LaboratoryProcessorBlock;
import net.jenkimods.bioforge.infection.spread.ItemStrainData;
import net.jenkimods.bioforge.registry.BioForgeSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

public final class LaboratoryProcessorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_MACHINE_SLOTS = 8;
    private final ItemStackHandler items = new ItemStackHandler(MAX_MACHINE_SLOTS) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < 0 || slot >= station().inputSlots()) return false;
            if (station() != LaboratoryStation.BARREL_PRESS) return true;
            return stack.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation
                    .tryBuild("bioforge", slot == 0 ? "ingredients/minecraft/glass_bottle"
                            : "ingredients/minecraft/pressable_fruits")));
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            CompoundTag fixedSize = nbt.copy();
            fixedSize.putInt("Size", MAX_MACHINE_SLOTS);
            super.deserializeNBT(fixedSize);
        }
    };
    private LazyOptional<ItemStackHandler> itemCapability = LazyOptional.of(() -> items);
    private LazyOptional<net.minecraftforge.items.IItemHandler> automationCapability = LazyOptional.of(this::automationHandler);

    private net.minecraftforge.items.IItemHandler automationHandler() {
        return new net.minecraftforge.items.IItemHandler() {
            public int getSlots() { return station().machineSlots(); }
            public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
            public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
            public boolean isItemValid(int slot, ItemStack stack) { return items.isItemValid(slot, stack); }
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (station() == LaboratoryStation.BARREL_PRESS && slot < station().inputSlots()) return ItemStack.EMPTY;
                return items.extractItem(slot, amount, simulate);
            }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (station() != LaboratoryStation.BARREL_PRESS || slot == 0
                        || !items.isItemValid(slot, stack)) return items.insertItem(slot, stack, simulate);
                ItemStack[] projected = new ItemStack[3];
                for (int i = 0; i < 3; i++) projected[i] = items.getStackInSlot(i + 1).copy();
                ItemStack remainder = stack.copy();
                while (!remainder.isEmpty()) {
                    int best = -1;
                    for (int i = 0; i < 3; i++) {
                        ItemStack candidate = projected[i];
                        if (!candidate.isEmpty() && (!ItemStack.isSameItemSameTags(candidate, stack)
                                || candidate.getCount() >= Math.min(items.getSlotLimit(i + 1), stack.getMaxStackSize()))) continue;
                        if (best < 0 || candidate.getCount() < projected[best].getCount()) best = i;
                    }
                    if (best < 0) break;
                    if (projected[best].isEmpty()) projected[best] = stack.copyWithCount(1);
                    else projected[best].grow(1);
                    remainder.shrink(1);
                }
                if (!simulate) for (int i = 0; i < 3; i++) items.setStackInSlot(i + 1, projected[i]);
                return remainder;
            }
        };
    }

    private void balanceBarrel() {
        // Recover old inventories whose bottle occupied one of the former generic slots.
        if (!items.getStackInSlot(0).isEmpty() && !items.isItemValid(0, items.getStackInSlot(0))) {
            for (int slot = 1; slot < 4; slot++) {
                if (items.getStackInSlot(slot).isEmpty() || items.isItemValid(0, items.getStackInSlot(slot))) {
                    ItemStack old = items.getStackInSlot(0);
                    items.setStackInSlot(0, items.getStackInSlot(slot));
                    items.setStackInSlot(slot, old);
                    break;
                }
            }
        } else if (items.getStackInSlot(0).isEmpty()) {
            for (int slot = 1; slot < 4; slot++) if (items.isItemValid(0, items.getStackInSlot(slot))) {
                items.setStackInSlot(0, items.getStackInSlot(slot));
                items.setStackInSlot(slot, ItemStack.EMPTY);
                break;
            }
        }
        for (int pass = 0; pass < 192; pass++) {
            int from = -1, to = -1;
            for (int a = 1; a < 4; a++) for (int b = 1; b < 4; b++) {
                ItemStack source = items.getStackInSlot(a), target = items.getStackInSlot(b);
                if (a != b && items.isItemValid(a, source) && source.getCount() > target.getCount() + 1
                        && (target.isEmpty() || ItemStack.isSameItemSameTags(source, target))) { from = a; to = b; }
            }
            if (from < 0) break;
            ItemStack one = items.extractItem(from, 1, false);
            items.insertItem(to, one, false);
        }
    }
    private final int[] progress = new int[MAX_MACHINE_SLOTS];
    private final int[] maxProgress = new int[MAX_MACHINE_SLOTS];
    private boolean working;

    public boolean isWorking() { return working; }

    private void syncWorking() {
        boolean next = station() == LaboratoryStation.HERBAL_MORTAR && progress[0] > 0;
        if (working == next) return;
        working = next;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Working", working);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        working = tag.getBoolean("Working");
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            int slot = index / 2;
            if (slot < 0 || slot >= MAX_MACHINE_SLOTS) return 0;
            return (index & 1) == 0 ? progress[slot] : maxProgress[slot];
        }
        @Override public void set(int index, int value) {
            int slot = index / 2;
            if (slot < 0 || slot >= MAX_MACHINE_SLOTS) return;
            if ((index & 1) == 0) progress[slot] = value;
            else maxProgress[slot] = value;
        }
        @Override public int getCount() { return MAX_MACHINE_SLOTS * 2; }
    };

    public LaboratoryProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(BioForge.LABORATORY_PROCESSOR_BE.get(), pos, state);
    }

    public LaboratoryStation station() {
        return getBlockState().getBlock() instanceof LaboratoryProcessorBlock block
                ? block.station() : LaboratoryStation.CHEMICAL_SYNTHESIZER;
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            LaboratoryProcessorBlockEntity processor) {
        if (level.isClientSide()) return;
        if (processor.station().processesInPlace()) processor.tickSterilization();
        else processor.tickCrafting();
        processor.syncWorking();
    }

    private void tickCrafting() {
        LaboratoryStation station = station();
        if (station == LaboratoryStation.BARREL_PRESS) balanceBarrel();
        var recipe = LaboratoryProcessRecipeManager.INSTANCE.find(level, station, items);
        if (recipe.isEmpty() || !canAccept(station.resultSlot(), recipe.get().result())
                || !canAccept(station.wasteSlot(), recipe.get().waste())) {
            resetProgress(0);
            return;
        }
        LaboratoryProcessRecipe active = recipe.get();
        maxProgress[0] = BioForgeServerConfig
                .laboratoryProcessingTime(station, active.processingTime());
        net.jenkimods.bioforge.audio.ActionAudio.work(this,station.getSerializedName());
        progress[0]++;
        if (progress[0] < maxProgress[0]) {
            setChanged();
            return;
        }
        active.consume(items);
        insertOutput(station.resultSlot(), active.result());
        insertOutput(station.wasteSlot(), active.waste());
        playCompletionSound(station);
        progress[0] = 0;
        setChanged();
    }

    private void tickSterilization() {
        boolean changed = false;
        boolean completed = false;
        for (int slot = 0; slot < LaboratoryStation.STERILIZATION_CHAMBER.inputSlots(); slot++) {
            ItemStack input = items.getStackInSlot(slot);
            var recipe = LaboratoryProcessRecipeManager.INSTANCE.findSingle(
                    level, LaboratoryStation.STERILIZATION_CHAMBER, input);
            boolean contaminated = ItemStrainData.read(input) != null;
            if (input.isEmpty() || (recipe.isEmpty() && !contaminated)) {
                if (progress[slot] != 0) {
                    progress[slot] = 0;
                    changed = true;
                }
                continue;
            }
            int baseProcessingTime = recipe.map(LaboratoryProcessRecipe::processingTime)
                    .orElse(100);
            maxProgress[slot] = BioForgeServerConfig.laboratoryProcessingTime(
                    LaboratoryStation.STERILIZATION_CHAMBER, baseProcessingTime);
            net.jenkimods.bioforge.audio.ActionAudio.work(this,"sterilization_chamber");
            progress[slot]++;
            changed = true;
            if (progress[slot] < maxProgress[slot]) continue;

            ItemStack output;
            if (recipe.isPresent()) {
                LaboratoryProcessRecipe active = recipe.get();
                output = active.result().copy();
                output.setCount(Math.min(output.getMaxStackSize(),
                        input.getCount() * Math.max(1, active.result().getCount())));
                if (active.copyNbt() && input.hasTag()) output.setTag(input.getTag().copy());
            } else {
                output = input.copy();
            }
            ItemStrainData.clear(output);
            items.setStackInSlot(slot, output);
            progress[slot] = 0;
            completed = true;
        }
        if (completed) playCompletionSound(LaboratoryStation.STERILIZATION_CHAMBER);
        if (changed) setChanged();
    }

    private void playCompletionSound(LaboratoryStation station) {
        if (level == null) return;
        net.jenkimods.bioforge.audio.ActionAudio.complete(this,station.getSerializedName());
    }

    private boolean canAccept(int slot, ItemStack result) {
        if (slot < 0 || result.isEmpty()) return true;
        ItemStack output = items.getStackInSlot(slot);
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void insertOutput(int slot, ItemStack result) {
        if (slot < 0 || result.isEmpty()) return;
        ItemStack output = items.getStackInSlot(slot);
        if (output.isEmpty()) items.setStackInSlot(slot, result.copy());
        else output.grow(result.getCount());
    }

    private void resetProgress(int slot) {
        if (progress[slot] == 0) return;
        progress[slot] = 0;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bioforge." + station().getSerializedName());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new LaboratoryProcessorMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("InventoryVersion", 2);
        tag.putIntArray("Progress", progress);
        tag.putIntArray("MaxProgress", maxProgress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.getInt("InventoryVersion") < 2) migrateLegacyInventory();
        int[] savedProgress = tag.getIntArray("Progress");
        int[] savedMaxProgress = tag.getIntArray("MaxProgress");
        if (savedProgress.length == 0 && tag.contains("Progress")) {
            progress[0] = tag.getInt("Progress");
            maxProgress[0] = Math.max(1, tag.getInt("MaxProgress"));
        } else {
            for (int slot = 0; slot < MAX_MACHINE_SLOTS; slot++) {
                progress[slot] = slot < savedProgress.length ? savedProgress[slot] : 0;
                maxProgress[slot] = slot < savedMaxProgress.length
                        ? Math.max(1, savedMaxProgress[slot]) : 160;
            }
        }
    }

    private void migrateLegacyInventory() {
        if (station() == LaboratoryStation.PHARMA_MIXER
                && items.getStackInSlot(5).isEmpty()
                && !items.getStackInSlot(4).isEmpty()) {
            items.setStackInSlot(5, items.getStackInSlot(4));
            items.setStackInSlot(4, ItemStack.EMPTY);
        } else if (station() == LaboratoryStation.CHEMICAL_SYNTHESIZER
                && items.getStackInSlot(3).isEmpty()
                && !items.getStackInSlot(4).isEmpty()) {
            items.setStackInSlot(3, items.getStackInSlot(4));
            items.setStackInSlot(4, ItemStack.EMPTY);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        itemCapability.invalidate();
        automationCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> items);
        automationCapability = LazyOptional.of(this::automationHandler);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability,
                                                       @Nullable net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return side == null
                ? itemCapability.cast() : automationCapability.cast();
        return super.getCapability(capability, side);
    }

    public void dropContents() {
        if (level == null) return;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D,
                    items.getStackInSlot(slot));
        }
    }
}
