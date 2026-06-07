package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.infection.symptoms.SymptomKey;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class InfectionClientCache {
    private InfectionClientCache() {}

    private static final AtomicBoolean infected = new AtomicBoolean(false);
    private static final AtomicReference<PathogenType> pathogenType = new AtomicReference<>(null);
    private static final AtomicReference<List<InfectionType>> infectionTypes = new AtomicReference<>(List.of());
    private static final AtomicReference<Map<String, Object>> symptomMap = new AtomicReference<>(Map.of());

    public static void set(boolean isInfected, PathogenType pathogen, List<InfectionType> types,
                           Map<String, Object> symptoms) {
        infected.set(isInfected);
        pathogenType.set(pathogen);
        infectionTypes.set(Collections.unmodifiableList(new ArrayList<>(types)));
        symptomMap.set(Collections.unmodifiableMap(new LinkedHashMap<>(symptoms)));
    }

    public static boolean isInfected() { return infected.get(); }
    public static PathogenType getPathogenType() { return pathogenType.get(); }
    public static List<InfectionType> getInfectionTypes() { return infectionTypes.get(); }

    @SuppressWarnings("unchecked")
    public static <T> T getSymptom(SymptomKey<T> key) {
        Object value = symptomMap.get().get(key.getId());
        if (value != null && key.getType().isInstance(value)) {
            return (T) value;
        }
        return key.getDefaultValue();
    }
}