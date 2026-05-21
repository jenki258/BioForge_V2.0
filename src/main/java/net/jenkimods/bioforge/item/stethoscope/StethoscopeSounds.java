package net.jenkimods.bioforge.item.stethoscope;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class StethoscopeSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BioForge.MODID);

    public static final RegistryObject<SoundEvent> HEARTBEAT_NORMAL =
            SOUNDS.register("stethoscope.heartbeat.normal",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild(BioForge.MODID, "stethoscope.heartbeat.normal")));

    public static final RegistryObject<SoundEvent> HEARTBEAT_FAST =
            SOUNDS.register("stethoscope.heartbeat.fast",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild(BioForge.MODID, "stethoscope.heartbeat.fast")));

    public static final RegistryObject<SoundEvent> HEARTBEAT_SLOW =
            SOUNDS.register("stethoscope.heartbeat.slow",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild(BioForge.MODID, "stethoscope.heartbeat.slow")));

    public static final RegistryObject<SoundEvent> LUNGS_NORMAL =
            SOUNDS.register("stethoscope.lungs.normal",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild(BioForge.MODID, "stethoscope.lungs.normal")));

    public static final RegistryObject<SoundEvent> LUNGS_CRACKLE =
            SOUNDS.register("stethoscope.lungs.crackle",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild(BioForge.MODID, "stethoscope.lungs.crackle")));
}
