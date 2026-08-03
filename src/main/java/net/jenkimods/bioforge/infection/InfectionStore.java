package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.infection.symptoms.SymptomKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InfectionStore extends SavedData {
    private static final String DATA_NAME = "bioforge_infections";
    private final Map<UUID, InfectionRecord> records = new HashMap<>();

    public static InfectionStore get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(InfectionStore::load, InfectionStore::new, DATA_NAME);
    }

    private static InfectionStore load(CompoundTag tag) {
        InfectionStore store = new InfectionStore();
        CompoundTag playersTag = tag.getCompound("Players");
        for (String key : playersTag.getAllKeys()) {
            UUID uuid = UUID.fromString(key);
            store.records.put(uuid, InfectionRecord.fromNbt(playersTag.getCompound(key)));
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<UUID, InfectionRecord> entry : records.entrySet()) {
            playersTag.put(entry.getKey().toString(), entry.getValue().toNbt());
        }
        tag.put("Players", playersTag);
        return tag;
    }

    public void setInfection(UUID uuid, InfectionRecord record) {
        records.put(uuid, record);
        setDirty();
    }

    public void clearInfection(UUID uuid) {
        records.remove(uuid);
        setDirty();
    }

    @Nullable
    public InfectionRecord getInfection(UUID uuid) {
        return records.get(uuid);
    }

    public boolean isInfected(UUID uuid) {
        InfectionRecord r = records.get(uuid);
        return r != null && r.infected();
    }

    public record InfectionRecord(
            boolean infected,
            boolean persistent,
            @Nullable PathogenType pathogenType,
            List<InfectionType> infectionTypes,
            Map<String, Object> symptoms,
            List<String> mutations
    ) {
        public static final InfectionRecord NONE = new InfectionRecord(
                false, false, null, List.of(), Map.of(), List.of()
        );

        public InfectionRecord(boolean infected, boolean persistent, @Nullable PathogenType pathogenType,
                               List<InfectionType> infectionTypes, Map<String, Object> symptoms) {
            this(infected, persistent, pathogenType, infectionTypes, symptoms, List.of());
        }

        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Infected", infected);
            tag.putBoolean("Persistent", persistent);
            if (pathogenType != null) tag.putString("PathogenType", pathogenType.name());
            StringJoiner joiner = new StringJoiner(",");
            for (InfectionType t : infectionTypes) joiner.add(t.name());
            tag.putString("InfectionTypes", joiner.toString());

            CompoundTag symptomTag = new CompoundTag();
            for (Map.Entry<String, Object> entry : symptoms.entrySet()) {
                SymptomKey<?> key = BioForgeSymptoms.getAllSymptomKeys().get(entry.getKey());
                if (key != null) {
                    writeSymptomToNbt(symptomTag, entry.getKey(), entry.getValue(), key);
                }
            }
            tag.put("Symptoms", symptomTag);

            if (!mutations.isEmpty()) {
                tag.putString("Mutations", String.join(",", mutations));
            }
            return tag;
        }

        private static void writeSymptomToNbt(CompoundTag tag, String keyId, Object value, SymptomKey<?> key) {
            if (value instanceof Enum<?> e) {
                tag.putString(keyId, e.name());
            } else if (value instanceof Boolean b) {
                tag.putBoolean(keyId, b);
            } else if (value instanceof Float f) {
                tag.putFloat(keyId, f);
            }
        }

        public static InfectionRecord fromNbt(CompoundTag tag) {
            boolean infected = tag.getBoolean("Infected");
            boolean persistent = tag.getBoolean("Persistent");
            PathogenType pt = tag.contains("PathogenType") ? PathogenType.fromName(tag.getString("PathogenType")) : null;
            List<InfectionType> types = new ArrayList<>();
            if (tag.contains("InfectionTypes")) {
                String raw = tag.getString("InfectionTypes");
                for (String s : raw.split(",")) {
                    if (!s.isEmpty()) types.add(InfectionType.fromName(s));
                }
            }

            Map<String, Object> symptoms = new LinkedHashMap<>();
            CompoundTag symptomTag = tag.getCompound("Symptoms");
            for (Map.Entry<String, SymptomKey<?>> entry : BioForgeSymptoms.getAllSymptomKeys().entrySet()) {
                String keyId = entry.getKey();
                SymptomKey<?> key = entry.getValue();
                if (symptomTag.contains(keyId)) {
                    Object value = BioForgeSymptoms.deserializeSymptom(keyId, symptomTag);
                    if (value != null) symptoms.put(keyId, value);
                }
            }

            List<String> mutations = new ArrayList<>();
            if (tag.contains("Mutations")) {
                String raw = tag.getString("Mutations");
                if (!raw.isEmpty()) {
                    for (String s : raw.split(",")) {
                        if (!s.isEmpty()) mutations.add(s);
                    }
                }
            }

            return new InfectionRecord(infected, persistent, pt, types, symptoms, mutations);
        }
    }
}