package net.jenkimods.bioforge.registry;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;

public final class BioForgeSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BioForge.MODID);

    public static final RegistryObject<SoundEvent> COUGH = register("symptom.cough");
    public static final RegistryObject<SoundEvent> SNEEZE = register("symptom.sneeze");
    public static final RegistryObject<SoundEvent> PARANOIA_VOICE =
            register("symptom.paranoia_voice");
    public static final RegistryObject<SoundEvent> DISINFECTING =
            register("machine.disinfecting");
    public static final RegistryObject<SoundEvent> GENES_COMPLETE =
            register("machine.genes_complete");
    public static final RegistryObject<SoundEvent> EMERGENCY =
            register("machine.emergency");
    public static final RegistryObject<SoundEvent> TESTING_COMPLETE =
            register("machine.testing_complete");
    public static final RegistryObject<SoundEvent> CENTRIFUGE =
            register("machine.centrifuge");
    public static final RegistryObject<SoundEvent> LIQUID_POUR =
            register("machine.liquid_pour");
    public static final RegistryObject<SoundEvent> CHEMICALS_COMPLETE =
            register("machine.chemicals_complete");
    public static final RegistryObject<SoundEvent> UI_BUTTON = register("ui.button");
    public static final RegistryObject<SoundEvent> UI_SATISFYING =
            register("ui.satisfying");

    private BioForgeSounds() {
    }

    private static RegistryObject<SoundEvent> register(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(
                Objects.requireNonNull(ResourceLocation.tryBuild(BioForge.MODID, id))));
    }
}
