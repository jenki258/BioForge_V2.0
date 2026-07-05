package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.infection.symptoms.SymptomKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class StrainData {
    private UUID colonyId;
    private PathogenType pathogen;
    private final Set<InfectionType> infectionTypes = EnumSet.noneOf(InfectionType.class);
    private final Map<String, String> symptoms = new LinkedHashMap<>();

    private static Map<String, SymptomKey<?>> ALL_SYMPTOM_KEYS;

    private StrainData() {}

    private static Map<String, SymptomKey<?>> getAllSymptomKeys() {
        if (ALL_SYMPTOM_KEYS == null) {
            ALL_SYMPTOM_KEYS = BioForgeSymptoms.getAllSymptomKeys();
        }
        return ALL_SYMPTOM_KEYS;
    }

    public static StrainData createEmpty() {
        return new StrainData();
    }

    public static StrainData buildFrom(InfectionData data) {
        StrainData s = new StrainData();
        if (data.getPathogenType() != null) {
            s.colonyId = UUID.randomUUID();
            s.pathogen = data.getPathogenType();
            s.infectionTypes.addAll(data.getInfectionTypes());

            for (Map.Entry<String, SymptomKey<?>> entry : getAllSymptomKeys().entrySet()) {
                String keyId = entry.getKey();
                SymptomKey<?> key = entry.getValue();
                Object value = data.getSymptom(key);
                if (value != null) {
                    s.symptoms.put(keyId, serializeSymptomValue(value));
                }
            }
        }
        return s;
    }

    public static StrainData parse(String payload) {
        StrainData s = new StrainData();
        if (payload == null || payload.equals("CLEAN") || payload.isEmpty()) return s;

        String[] parts = payload.split(";");
        if (parts.length == 0) return s;

        String[] header = parts[0].split("\\|");
        if (header.length >= 3) {
            try { s.colonyId = UUID.fromString(header[0]); } catch (Exception ignored) {}
            s.pathogen = PathogenType.fromName(header[1]);
            s.parseTypes(header[2]);
        } else if (header.length == 2) {
            s.pathogen = PathogenType.fromName(header[0]);
            s.parseTypes(header[1]);
        } else if (header.length == 1) {
            s.pathogen = PathogenType.fromName(header[0]);
        }

        Map<String, SymptomKey<?>> allKeys = getAllSymptomKeys();
        for (int i = 1; i < parts.length; i++) {
            String[] kv = parts[i].split("=", 2);
            if (kv.length == 2 && allKeys.containsKey(kv[0])) {
                s.symptoms.put(kv[0], kv[1]);
            }
        }
        return s;
    }

    public Optional<UUID> getColonyId() { return Optional.ofNullable(colonyId); }
    public PathogenType getPathogen() { return pathogen; }
    public Set<InfectionType> getInfectionTypes() { return infectionTypes; }
    public Map<String, String> getSymptoms() { return symptoms; }
    public Optional<String> getSymptom(String key) { return Optional.ofNullable(symptoms.get(key)); }

    public void setColonyId(UUID id) { this.colonyId = id; }
    public void setPathogen(PathogenType pathogen) { this.pathogen = pathogen; }

    public static String replaceColonyId(String payload, String newColonyId) {
        int firstPipe = payload.indexOf('|');
        if (firstPipe == -1) return payload;
        int secondPipe = payload.indexOf('|', firstPipe + 1);
        if (secondPipe == -1) return payload;
        return newColonyId + payload.substring(firstPipe);
    }

    public String toPayload() {
        StringBuilder sb = new StringBuilder();
        sb.append(colonyId != null ? colonyId.toString() : "PLACEHOLDER").append("|");
        sb.append(pathogen != null ? pathogen.name() : "UNKNOWN").append("|");
        Iterator<InfectionType> iter = infectionTypes.iterator();
        while (iter.hasNext()) {
            sb.append(iter.next().name());
            if (iter.hasNext()) sb.append(",");
        }
        sb.append(";");
        for (Map.Entry<String, String> entry : symptoms.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public void applyToEntity(InfectionData data, LivingEntity target) {
        if (data == null || data.isInfected() || pathogen == null) return;
        data.setInfected(true);
        data.setPathogenType(pathogen);
        for (InfectionType type : infectionTypes) {
            data.addInfectionType(type);
        }
        Map<String, SymptomKey<?>> allKeys = getAllSymptomKeys();
        for (Map.Entry<String, String> entry : symptoms.entrySet()) {
            SymptomKey<?> key = allKeys.get(entry.getKey());
            if (key != null) {
                Object value = parseSymptomValue(entry.getValue(), key.getType());
                if (value != null) {
                    data.getSymptoms().set((SymptomKey) key, value);
                }
            }
        }
        if (target instanceof ServerPlayer sp) {
            InfectionEventHandler.syncToClient(sp, data);
        }
    }

    private static String serializeSymptomValue(Object value) {
        if (value instanceof Enum<?> e) return e.name();
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        return "";
    }

    @SuppressWarnings("unchecked")
    private static <T> T parseSymptomValue(String string, Class<T> type) {
        try {
            if (type.isEnum()) {
                return (T) Enum.valueOf((Class<Enum>) type, string);
            } else if (type == Boolean.class) {
                return (T) Boolean.valueOf(string);
            } else if (type == Float.class) {
                return (T) Float.valueOf(string);
            } else if (type == Integer.class) {
                return (T) Integer.valueOf(string);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void parseTypes(String raw) {
        infectionTypes.clear();
        if (raw == null || raw.isEmpty()) return;
        for (String t : raw.split(",")) {
            InfectionType it = InfectionType.fromName(t.trim());
            if (it != null) infectionTypes.add(it);
        }
    }
}