package net.jenkimods.bioforge.registry;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class BFCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BioForge.MODID);

    public static final RegistryObject<CreativeModeTab> TOOLS_TAB = TABS.register("tools_tab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bioforge.tools_tab"))
                    .icon(() -> new ItemStack(BioForge.THERMOMETER_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(BioForge.CLIPBOARD.get());
                        output.accept(BioForge.SWAB.get());
                        output.accept(BioForge.PETRI_DISH.get());
                        output.accept(BioForge.CONTAMINATED_SUBSTRATE_ITEM.get());
                        output.accept(BioForge.THERMOMETER_ITEM.get());
                        output.accept(BioForge.STETHOSCOPE.get());
                        output.accept(BioForge.OTOSCOPE.get());
                        output.accept(BioForge.MIRROR.get());
                        output.accept(BioForge.REFLEX_HAMMER.get());
                        output.accept(BioForge.PULSE_OXIMETER.get());
                        output.accept(BioForge.BONE_SAW.get());
                        output.accept(BioForge.WOODEN_NEEDLE.get());
                        output.accept(BioForge.IRON_NEEDLE.get());
                        output.accept(BioForge.HARDENED_NEEDLE.get());
                        output.accept(BioForge.SYRINGE.get());
                        output.accept(BioForge.BLOOD_SLIDE.get());
                        output.accept(BioForge.TUBE.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> REAGENTS_TAB = TABS.register("reagents_tab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bioforge.reagents_tab"))
                    .icon(() -> new ItemStack(BioForge.ANTI_A_VIAL.get()))
                    .displayItems((params, output) -> {
                        output.accept(BioForge.ANTI_A_VIAL.get());
                        output.accept(BioForge.ANTI_B_VIAL.get());
                        output.accept(BioForge.ANTI_D_VIAL.get());
                        output.accept(BioForge.DECALCIFICATION_FLUID.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> MACHINERY_TAB = TABS.register("machinery_tab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bioforge.machinery_tab"))
                    .icon(() -> new ItemStack(BioForge.CENTRIFUGE_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(BioForge.CENTRIFUGE_ITEM.get());
                        output.accept(BioForge.MICROSCOPE_ITEM.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> SAMPLES_TAB = TABS.register("samples_tab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bioforge.samples_tab"))
                    .icon(() -> new ItemStack(BioForge.BONE_MARROW.get()))
                    .displayItems((params, output) -> {
                        output.accept(BioForge.SPLIT_BONE.get());
                        output.accept(BioForge.BONE_MARROW.get());
                        output.accept(BioForge.WITHERED_SPLIT_BONE.get());
                        output.accept(BioForge.WITHERED_BONE_MARROW.get());
                        output.accept(BioForge.PLASMA_SAMPLE.get());
                        output.accept(BioForge.CELL_PELLET.get());
                        output.accept(BioForge.MEDICAL_REPORT.get());
                    })
                    .build());
}