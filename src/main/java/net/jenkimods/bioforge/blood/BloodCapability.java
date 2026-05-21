package net.jenkimods.bioforge.blood;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class BloodCapability {

    public static final Capability<BloodData> BLOOD_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation BLOOD_CAP_ID =
            ResourceLocation.tryBuild(BioForge.MODID, "blood");

    @Mod.EventBusSubscriber(modid = BioForge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(BloodData.class);
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof LivingEntity)) return;
        if (event.getCapabilities().containsKey(BLOOD_CAP_ID)) return;
        event.addCapability(BLOOD_CAP_ID, new Provider());
    }

    @Nullable
    public static BloodData get(Entity entity) {
        if (!(entity instanceof LivingEntity)) return null;
        return entity.getCapability(BLOOD_CAP).resolve().orElse(null);
    }

    private static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final BloodDataImpl data = new BloodDataImpl();
        private final LazyOptional<BloodData> optional = LazyOptional.of(() -> data);

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return BLOOD_CAP.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.deserializeNBT(tag);
        }
    }
}