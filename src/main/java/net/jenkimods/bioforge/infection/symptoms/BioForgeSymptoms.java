package net.jenkimods.bioforge.infection.symptoms;

import net.jenkimods.bioforge.infection.HeartRate;
import net.jenkimods.bioforge.infection.LungSound;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public final class BioForgeSymptoms {

    private BioForgeSymptoms() {}

    public static final SymptomKey<HeartRate> HEART_RATE =
            SymptomKey.create("heart_rate", HeartRate.class, HeartRate.NORMAL);

    public static final SymptomKey<LungSound> LUNG_SOUND =
            SymptomKey.create("lung_sound", LungSound.class, LungSound.NORMAL);

    public static final SymptomKey<Boolean> TEMPERATURE_PLUS =
            SymptomKey.create("temperature_plus", Boolean.class, false);

    public static final SymptomKey<Boolean> TEMPERATURE_MINUS =
            SymptomKey.create("temperature_minus", Boolean.class, false);

    public static final SymptomKey<Float> OTOSCOPE_REDNESS =
            SymptomKey.create("otoscope_redness", Float.class, 0.0f);

    public static final SymptomKey<Float> OTOSCOPE_LESIONS =
            SymptomKey.create("otoscope_lesions", Float.class, 0.0f);

    public static final SymptomKey<Float> OTOSCOPE_SECRETION =
            SymptomKey.create("otoscope_secretion", Float.class, 0.0f);

    public static final SymptomKey<Float> OTOSCOPE_SWELLING =
            SymptomKey.create("otoscope_swelling", Float.class, 0.0f);

    public static final SymptomKey<Float> REFLEX_DELAY =
            SymptomKey.create("reflex_delay", Float.class, 0.0f);

    public static final SymptomKey<Float> REFLEX_STRENGTH =
            SymptomKey.create("reflex_strength", Float.class, 0.5f);

    public static final SymptomKey<Float> NEURAL_DAMAGE =
            SymptomKey.create("neural_damage", Float.class, 0.0f);

    public static final SymptomKey<Float> OXYGEN_SATURATION =
            SymptomKey.create("oxygen_saturation", Float.class, 0.95f);

    public static final SymptomKey<Float> PERFUSION_INDEX =
            SymptomKey.create("perfusion_index", Float.class, 0.7f);

    public static final SymptomKey<Float> INFECTION_STRENGTH =
            SymptomKey.create("infection_strength", Float.class, 0.5f);

    public static final SymptomKey<Float> COLONY_RADIUS =
            SymptomKey.create("colony_radius", Float.class, 20.0f);

    public static final SymptomKey<Float> MAX_INFESTED_BLOCKS =
            SymptomKey.create("max_infested_blocks", Float.class, 100.0f);

    public static SymptomDeserializer deserializer() {
        return BioForgeSymptoms::resolveKey;
    }

    @Nullable
    private static Object resolveKey(String keyId, CompoundTag tag) {
        return switch (keyId) {
            case "heart_rate" -> safeEnum(HeartRate.class, tag.getString(keyId), HeartRate.NORMAL);
            case "lung_sound" -> safeEnum(LungSound.class, tag.getString(keyId), LungSound.NORMAL);
            case "temperature_plus" -> tag.getBoolean(keyId);
            case "temperature_minus" -> tag.getBoolean(keyId);
            case "otoscope_redness" -> tag.getFloat(keyId);
            case "otoscope_lesions" -> tag.getFloat(keyId);
            case "otoscope_secretion" -> tag.getFloat(keyId);
            case "otoscope_swelling" -> tag.getFloat(keyId);
            case "reflex_delay" -> tag.getFloat(keyId);
            case "reflex_strength" -> tag.getFloat(keyId);
            case "neural_damage" -> tag.getFloat(keyId);
            case "oxygen_saturation" -> tag.getFloat(keyId);
            case "perfusion_index" -> tag.getFloat(keyId);
            case "infection_strength" -> tag.getFloat(keyId);
            case "colony_radius" -> tag.getFloat(keyId);
            case "max_infested_blocks" -> tag.getFloat(keyId);
            default -> null;
        };
    }

    private static <E extends Enum<E>> E safeEnum(Class<E> cls, String name, E fallback) {
        try { return Enum.valueOf(cls, name); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}