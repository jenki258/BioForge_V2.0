package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.infection.symptoms.EntitySymptoms;
import net.jenkimods.bioforge.infection.symptoms.SymptomKey;
import org.jetbrains.annotations.Nullable;

public interface InfectionData {

    boolean isInfected();
    @Nullable PathogenType getPathogenType();
    @Nullable InfectionType getInfectionType();

    void setInfected(boolean infected);
    void setPathogenType(@Nullable PathogenType pathogenType);
    void setInfectionType(@Nullable InfectionType infectionType);
    void clearInfection();

    EntitySymptoms getSymptoms();

    default <T> T getSymptom(SymptomKey<T> key) {
        return getSymptoms().get(key);
    }

    default <T> void setSymptom(SymptomKey<T> key, T value) {
        getSymptoms().set(key, value);
    }
}
