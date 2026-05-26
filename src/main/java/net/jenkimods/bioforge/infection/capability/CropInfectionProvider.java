package net.jenkimods.bioforge.infection.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CropInfectionProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final CropInfectionStorage storage = new CropInfectionStorage();
    private final LazyOptional<ICropInfectionStorage> optional = LazyOptional.of(() -> storage);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CropInfectionCapability.CROP_INFECTION) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return storage.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        storage.deserializeNBT(nbt);
    }
}