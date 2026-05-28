package net.jenkimods.bioforge.event;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class InfectedFoodHandler {

    @SubscribeEvent
    public static void onEatFinish(LivingEntityUseItemEvent.Finish event) {
        ItemStack item = event.getItem();
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        String strain = NbtObfuscator.readString(item.getOrCreateTag());
        if (strain == null || strain.equals("CLEAN")) return;

        String[] parts = strain.split(";");
        if (parts.length == 0) return;
        String[] header = parts[0].split("\\|");

        PathogenType pathogen = null;
        Set<InfectionType> strainTypes = EnumSet.noneOf(InfectionType.class);

        if (header.length == 2) {
            pathogen = PathogenType.fromName(header[0]);
            parseTypes(header[1], strainTypes);
        } else if (header.length >= 3) {
            pathogen = PathogenType.fromName(header[1]);
            parseTypes(header[2], strainTypes);
        } else {
            return;
        }

        if (strainTypes.contains(InfectionType.FOOD_BORNE) && pathogen != null) {
            InfectionData data = InfectionCapability.get(entity);
            if (data != null && !data.isInfected()) {
                data.setInfected(true);
                data.setPathogenType(pathogen);
                for (InfectionType t : strainTypes) data.addInfectionType(t);

                applyStrainSymptoms(data, strain);

                if (entity instanceof ServerPlayer sp) {
                    InfectionEventHandler.syncToClient(sp, data);
                }
            }
        }
    }

    private static void parseTypes(String raw, Set<InfectionType> target) {
        if (raw == null || raw.isEmpty()) return;
        for (String part : raw.split(",")) {
            InfectionType it = InfectionType.fromName(part.trim());
            if (it != null) target.add(it);
        }
    }

    private static void applyStrainSymptoms(InfectionData data, String strain) {
        String[] parts = strain.split(";");
        for (int i = 1; i < parts.length; i++) {
            String[] kv = parts[i].split("=", 2);
            if (kv.length != 2) continue;

            String key = kv[0];
            String value = kv[1];

            try {
                switch (key) {
                    case "HeartRate":
                        data.setSymptom(BioForgeSymptoms.HEART_RATE, HeartRate.fromName(value));
                        break;
                    case "LungSound":
                        data.setSymptom(BioForgeSymptoms.LUNG_SOUND, LungSound.fromName(value));
                        break;
                    case "TempPlus":
                        data.setSymptom(BioForgeSymptoms.TEMPERATURE_PLUS, Boolean.parseBoolean(value));
                        break;
                    case "TempMinus":
                        data.setSymptom(BioForgeSymptoms.TEMPERATURE_MINUS, Boolean.parseBoolean(value));
                        break;
                    case "Redness":
                        data.setSymptom(BioForgeSymptoms.OTOSCOPE_REDNESS, Float.parseFloat(value));
                        break;
                    case "Lesions":
                        data.setSymptom(BioForgeSymptoms.OTOSCOPE_LESIONS, Float.parseFloat(value));
                        break;
                    case "Secretion":
                        data.setSymptom(BioForgeSymptoms.OTOSCOPE_SECRETION, Float.parseFloat(value));
                        break;
                    case "Swelling":
                        data.setSymptom(BioForgeSymptoms.OTOSCOPE_SWELLING, Float.parseFloat(value));
                        break;
                    case "ReflexDelay":
                        data.setSymptom(BioForgeSymptoms.REFLEX_DELAY, Float.parseFloat(value));
                        break;
                    case "ReflexStrength":
                        data.setSymptom(BioForgeSymptoms.REFLEX_STRENGTH, Float.parseFloat(value));
                        break;
                    case "NeuralDamage":
                        data.setSymptom(BioForgeSymptoms.NEURAL_DAMAGE, Float.parseFloat(value));
                        break;
                    case "OxygenSaturation":
                        data.setSymptom(BioForgeSymptoms.OXYGEN_SATURATION, Float.parseFloat(value));
                        break;
                    case "PerfusionIndex":
                        data.setSymptom(BioForgeSymptoms.PERFUSION_INDEX, Float.parseFloat(value));
                        break;
                    case "InfectionStrength":
                        data.setSymptom(BioForgeSymptoms.INFECTION_STRENGTH, Float.parseFloat(value));
                        break;
                    case "ColonyRadius":
                        data.setSymptom(BioForgeSymptoms.COLONY_RADIUS, Float.parseFloat(value));
                        break;
                    case "MaxInfestedBlocks":
                        data.setSymptom(BioForgeSymptoms.MAX_INFESTED_BLOCKS, Float.parseFloat(value));
                        break;
                }
            } catch (Exception ignored) {}
        }
    }
}