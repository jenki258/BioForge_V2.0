package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.infection.symptoms.EntitySymptoms;
import net.jenkimods.bioforge.infection.symptoms.SymptomKey;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.Set;

public interface InfectionData {
    boolean isInfected();
    @Nullable PathogenType getPathogenType();
    Set<InfectionType> getInfectionTypes();

    void setInfected(boolean infected);
    void setPathogenType(@Nullable PathogenType pathogenType);
    void addInfectionType(InfectionType type);
    void removeInfectionType(InfectionType type);
    void clearInfection();


    Collection<StrainImmunity> getStrainImmunities();
    boolean hasStrainImmunity(String fingerprint);
    void grantStrainImmunity(String fingerprint, String displayName, int durationTicks);
    boolean tickStrainImmunities();
    void copyStrainImmunitiesFrom(InfectionData source);

    EntitySymptoms getSymptoms();

    default <T> T getSymptom(SymptomKey<T> key) {
        return getSymptoms().get(key);
    }

    default <T> void setSymptom(SymptomKey<T> key, T value) {
        getSymptoms().set(key, value);
    }
}
