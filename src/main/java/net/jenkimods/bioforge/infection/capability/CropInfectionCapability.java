package net.jenkimods.bioforge.infection.capability;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class CropInfectionCapability {
    public static final Capability<ICropInfectionStorage> CROP_INFECTION =
            CapabilityManager.get(new CapabilityToken<>(){});

    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(ICropInfectionStorage.class);
    }

    @SubscribeEvent
    public static void attachToChunk(AttachCapabilitiesEvent<LevelChunk> event) {
        event.addCapability(ResourceLocation.tryBuild(BioForge.MODID, "crop_infection"),
                new CropInfectionProvider());
    }
}