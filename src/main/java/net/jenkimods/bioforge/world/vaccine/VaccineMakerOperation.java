package net.jenkimods.bioforge.world.vaccine;

import java.util.Locale;

public enum VaccineMakerOperation {
    FULL,
    DIRECTED,
    RANDOM_MUTATION,
    CLONE;

    public static VaccineMakerOperation fromName(String name) {
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unknown Vaccine Maker operation: " + name);
        }
    }
}
