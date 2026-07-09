package net.jenkimods.bioforge.world.microscope;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.StrainData;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.infection.symptoms.SymptomKey;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MicroscopeBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                syncToViewers();
            }
        }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return !MicroscopeSymptomConfig.INSTANCE.getEntriesFor(stack).isEmpty();
        }
    };

    private LazyOptional<ItemStackHandler> lazyHandler = LazyOptional.of(() -> itemHandler);

    public MicroscopeBlockEntity(BlockPos pos, BlockState state) {
        super(BioForge.MICROSCOPE_BE.get(), pos, state);
    }

    private void syncToViewers() {
        if (level == null) return;
        ItemStack stack = itemHandler.getStackInSlot(0);
        List<MicroscopeSymptomEntry> entries = MicroscopeSymptomConfig.INSTANCE.getEntriesFor(stack);
        List<CalibrationSlider> calib = MicroscopeSymptomConfig.INSTANCE.getCalibrationFor(stack);
        List<CalibrationSlider> randomised = new ArrayList<>();
        for (CalibrationSlider slider : calib) {
            if (slider.randomTarget()) {
                net.minecraft.util.RandomSource rand = level != null ? level.random : net.minecraft.util.RandomSource.create();
                float randomTarget = slider.rangeMin() + rand.nextFloat() * (slider.rangeMax() - slider.rangeMin());
                randomised.add(new CalibrationSlider(slider.nameKey(), randomTarget, slider.rangeMin(), slider.rangeMax(), true));
            } else {
                randomised.add(slider);
            }
        }
        Map<String, Object> symptoms = getCurrentSymptoms(entries);
        String visibility = getCurrentVisibility();
        MicroscopeSyncPacket packet = new MicroscopeSyncPacket(symptoms, visibility, entries, randomised);
        for (Player player : level.players()) {
            if (player.containerMenu instanceof MicroscopeMenu menu && menu.getBlockEntity() == this) {
                MicroscopeNetwork.sendToPlayer(packet, (ServerPlayer) player);
            }
        }
    }

    private Map<String, Object> getCurrentSymptoms(List<MicroscopeSymptomEntry> entries) {
        Map<String, Object> symptoms = new LinkedHashMap<>();
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (!stack.isEmpty()) {
            CompoundTag tag = stack.getOrCreateTag();
            for (MicroscopeSymptomEntry entry : entries) {
                if ("nbt".equals(entry.source())) {
                    if (entry.nbtKey() == null) continue;
                    if (tag.contains(entry.nbtKey())) {
                        int value = tag.getInt(entry.nbtKey());
                        if (entry.matchesCondition(value)) {
                            symptoms.put(entry.symptomKey(), (float) value);
                        }
                    }
                    continue;
                }
                String strainRaw = NbtObfuscator.readInfection(tag);
                if (strainRaw == null || strainRaw.isEmpty()) strainRaw = NbtObfuscator.readString(tag);
                if (strainRaw == null || strainRaw.isEmpty()) continue;
                StrainData strain = StrainData.parse(strainRaw);
                SymptomKey<?> key = BioForgeSymptoms.getAllSymptomKeys().get(entry.symptomKey());
                if (key == null) continue;
                String raw = strain.getSymptom(entry.symptomKey()).orElse(null);
                if (raw == null) continue;
                if (key.getType().isEnum()) symptoms.put(entry.symptomKey(), raw.toUpperCase());
                else if (key.getType() == Boolean.class) symptoms.put(entry.symptomKey(), Boolean.valueOf(raw));
                else if (key.getType() == Float.class) {
                    try { symptoms.put(entry.symptomKey(), Float.valueOf(raw)); } catch (Exception ignored) {}
                }
            }
        }
        return symptoms;
    }

    private String getCurrentVisibility() {
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (!stack.isEmpty()) {
            String strainRaw = NbtObfuscator.readInfection(stack.getOrCreateTag());
            if (strainRaw == null || strainRaw.isEmpty()) {
                strainRaw = NbtObfuscator.readString(stack.getOrCreateTag());
            }
            if (strainRaw != null && !strainRaw.isEmpty()) {
                StrainData strain = StrainData.parse(strainRaw);
                return strain.getSymptom("microscope_visibility").orElse("NONE");
            }
        }
        return "NONE";
    }

    @Override
    public Component getDisplayName() { return Component.translatable("block.bioforge.microscope"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (player instanceof ServerPlayer sp) {
            ItemStack stack = itemHandler.getStackInSlot(0);
            List<MicroscopeSymptomEntry> entries = MicroscopeSymptomConfig.INSTANCE.getEntriesFor(stack);
            List<CalibrationSlider> calib = MicroscopeSymptomConfig.INSTANCE.getCalibrationFor(stack);
            List<CalibrationSlider> randomised = new ArrayList<>();
            for (CalibrationSlider slider : calib) {
                if (slider.randomTarget()) {
                    net.minecraft.util.RandomSource rand = level != null ? level.random : net.minecraft.util.RandomSource.create();
                    float randomTarget = slider.rangeMin() + rand.nextFloat() * (slider.rangeMax() - slider.rangeMin());
                    randomised.add(new CalibrationSlider(slider.nameKey(), randomTarget, slider.rangeMin(), slider.rangeMax(), true));
                } else {
                    randomised.add(slider);
                }
            }
            MicroscopeNetwork.sendToPlayer(
                    new MicroscopeSyncPacket(getCurrentSymptoms(entries), getCurrentVisibility(), entries, randomised), sp);
        }
        return new MicroscopeMenu(containerId, playerInventory, this);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
    }

    @Override
    public void setRemoved() { super.setRemoved(); lazyHandler.invalidate(); }

    @Override
    public void reviveCaps() { super.reviveCaps(); lazyHandler = LazyOptional.of(() -> itemHandler); }

    public void drops() {
        if (level == null) return;
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (!stack.isEmpty()) Containers.dropItemStack(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, stack);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }
}