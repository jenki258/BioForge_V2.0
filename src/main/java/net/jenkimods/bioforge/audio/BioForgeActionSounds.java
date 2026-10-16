package net.jenkimods.bioforge.audio;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BioForgeActionSounds {
    public static final String[] MACHINES={"barrel_press","chemical_synthesizer","sterilization_chamber","pharma_mixer",
            "herbal_mortar","incubator","centrifuge","vaccine_maker","field_vaccine_maker","medical_diffuser","gene_splicer"};
    public static final String[] ACTIONS={"machine.microscope.complete","machine.microscope.adjust","machine.air_filter.work",
            "machine.viral_scanner.work","machine.viral_scanner.alert","ui.machine.open","ui.machine.close",
            "instrument.thermometer.shake","instrument.thermometer.measure","instrument.stethoscope.select",
            "instrument.stethoscope.start","instrument.stethoscope.stop","instrument.otoscope.start","instrument.otoscope.stop",
            "instrument.otoscope.measure","instrument.pulse_oximeter.start","instrument.pulse_oximeter.stop","instrument.pulse_oximeter.measure",
            "instrument.reflex_hammer.tap","instrument.area_scanner.scan","instrument.bone_saw.collect",
            "instrument.swab.collect","instrument.needle.extract","instrument.needle.inject","instrument.syringe.extract",
            "instrument.syringe.inject","instrument.vaccine.inject","instrument.field_vaccine.inject","instrument.vaccine.success","instrument.field_vaccine.success","instrument.vaccine.load","instrument.mutation.inject","instrument.mutation.upgrade","instrument.tablet.take",
            "instrument.resistance_pill.take","instrument.tube.fill","instrument.tube.empty","instrument.blood_slide.prepare",
            "instrument.reagent.apply","instrument.wipe.clean","instrument.ethanol.clean","instrument.decontamination.spray",
            "instrument.decalcification.apply","instrument.petri.feed","instrument.petri.inoculate","instrument.petri.collect",
            "ui.research_tablet.open","ui.clipboard.write",
            "instrument.protection.equip","instrument.protection.unequip"};
    public static final Map<String,RegistryObject<SoundEvent>> EVENTS=create();
    private static Map<String,RegistryObject<SoundEvent>> create() {
        var events=new LinkedHashMap<String,RegistryObject<SoundEvent>>();
        for(String machine:MACHINES)for(String phase:new String[]{"work","complete"})register(events,"machine."+machine+"."+phase);
        for(String id:ACTIONS)register(events,id);
        return java.util.Collections.unmodifiableMap(events);
    }
    private static void register(Map<String,RegistryObject<SoundEvent>> events,String id) {
        events.put(id,net.jenkimods.bioforge.registry.BioForgeSounds.SOUNDS.register(id,
                ()->SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild(BioForge.MODID,id))));
    }
    public static void initialize() { }
}
