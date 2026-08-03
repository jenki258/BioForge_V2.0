package net.jenkimods.bioforge.registry;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public final class BioForgeEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, BioForge.MODID);

    public static final RegistryObject<MobEffect> STRAIN_IMMUNITY = EFFECTS.register(
            "strain_immunity", StrainImmunityEffect::new);

    private BioForgeEffects() {}

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }

    private static final class StrainImmunityEffect extends MobEffect {
        private StrainImmunityEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x55D6C2);
        }
    }
}
