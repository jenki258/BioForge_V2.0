package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = BioForge.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InfectionCapability {

    public static final Capability<InfectionData> INFECTION_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation INFECTION_CAP_ID =
            ResourceLocation.tryBuild(BioForge.MODID, "infection");

    @Mod.EventBusSubscriber(modid = BioForge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(InfectionData.class);
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {

        if (!(event.getObject() instanceof LivingEntity)) return;

        if (event.getCapabilities().containsKey(INFECTION_CAP_ID)) return;

        event.addCapability(INFECTION_CAP_ID, new Provider());
    }

    @Nullable
    public static InfectionData get(Entity entity) {

        if (!(entity instanceof LivingEntity)) return null;

        return entity.getCapability(INFECTION_CAP).resolve().orElse(null);
    }

    private static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final InfectionDataImpl impl = new InfectionDataImpl();
        private final LazyOptional<InfectionData> optional = LazyOptional.of(() -> impl);

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return INFECTION_CAP.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return impl.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            impl.deserializeNBT(nbt);
        }
    }
}