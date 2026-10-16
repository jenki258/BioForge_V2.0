package net.jenkimods.bioforge.world.incubator;

import java.util.Locale;

public enum IncubatorOperation {
    CRAFT,
    GENERATE_STRAIN,
    COPY_SAMPLE_STRAIN,
    COPY_BLOOD_STRAIN,
    CONVERT_SAMPLE;

    public static IncubatorOperation parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unknown incubator operation '" + value
                            + "'. Expected craft, generate_strain, copy_sample_strain, copy_blood_strain, or convert_sample"
            );
        }
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
