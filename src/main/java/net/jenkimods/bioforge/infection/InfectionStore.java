package net.jenkimods.bioforge.infection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public void setInfection(UUID uuid, InfectionRecord record) { records.put(uuid, record); setDirty(); }
    public void clearInfection(UUID uuid) { records.remove(uuid); setDirty(); }
    @Nullable public InfectionRecord getInfection(UUID uuid) { return records.get(uuid); }
    public boolean isInfected(UUID uuid) { InfectionRecord r = records.get(uuid); return r != null && r.infected(); }

    public record InfectionRecord(
            boolean infected,
            boolean persistent,
            @Nullable PathogenType pathogenType,
            @Nullable InfectionType infectionType,
            HeartRate heartRate,
            LungSound lungSound,
            boolean temperaturePlus,
            boolean temperatureMinus,
            float redness,
            float lesions,
            float secretion,
            float swelling,
            float reflexDelay,
            float reflexStrength,
            float neuralDamage,
            float oxygenSaturation,
            float perfusionIndex,
            float infectionStrength
    ) {
        public static final InfectionRecord NONE =
                new InfectionRecord(false, false, null, null,
                        HeartRate.NORMAL, LungSound.NORMAL, false, false,
                        0.0f, 0.0f, 0.0f, 0.0f,
                        0.0f, 0.5f, 0.0f,
                        0.95f, 0.7f, 0.5f);

        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Infected", infected);
            tag.putBoolean("Persistent", persistent);
            if (pathogenType != null) tag.putString("PathogenType", pathogenType.name());
            if (infectionType != null) tag.putString("InfectionType", infectionType.name());
            tag.putString("HeartRate", heartRate.name());
            tag.putString("LungSound", lungSound.name());
            tag.putBoolean("TempPlus", temperaturePlus);
            tag.putBoolean("TempMinus", temperatureMinus);
            tag.putFloat("Redness", redness);
            tag.putFloat("Lesions", lesions);
            tag.putFloat("Secretion", secretion);
            tag.putFloat("Swelling", swelling);
            tag.putFloat("ReflexDelay", reflexDelay);
            tag.putFloat("ReflexStrength", reflexStrength);
            tag.putFloat("NeuralDamage", neuralDamage);
            tag.putFloat("OxygenSaturation", oxygenSaturation);
            tag.putFloat("PerfusionIndex", perfusionIndex);
            tag.putFloat("InfectionStrength", infectionStrength);
            return tag;
        }

        public static InfectionRecord fromNbt(CompoundTag tag) {
            boolean infected = tag.getBoolean("Infected");
            boolean persistent = tag.getBoolean("Persistent");
            PathogenType pathogenType = tag.contains("PathogenType")
                    ? PathogenType.fromName(tag.getString("PathogenType")) : null;
            InfectionType infectionType = tag.contains("InfectionType")
                    ? InfectionType.fromName(tag.getString("InfectionType")) : null;
            HeartRate heartRate = tag.contains("HeartRate")
                    ? HeartRate.fromName(tag.getString("HeartRate")) : HeartRate.NORMAL;
            LungSound lungSound = tag.contains("LungSound")
                    ? LungSound.fromName(tag.getString("LungSound")) : LungSound.NORMAL;
            boolean tempPlus = tag.getBoolean("TempPlus");
            boolean tempMinus = tag.getBoolean("TempMinus");
            float redness = tag.getFloat("Redness");
            float lesions = tag.getFloat("Lesions");
            float secretion = tag.getFloat("Secretion");
            float swelling = tag.getFloat("Swelling");
            float reflexDelay = tag.contains("ReflexDelay") ? tag.getFloat("ReflexDelay") : 0.0f;
            float reflexStrength = tag.contains("ReflexStrength") ? tag.getFloat("ReflexStrength") : 0.5f;
            float neuralDamage = tag.contains("NeuralDamage") ? tag.getFloat("NeuralDamage") : 0.0f;
            float oxygenSaturation = tag.contains("OxygenSaturation") ? tag.getFloat("OxygenSaturation") : 0.95f;
            float perfusionIndex = tag.contains("PerfusionIndex") ? tag.getFloat("PerfusionIndex") : 0.7f;
            float infectionStrength = tag.contains("InfectionStrength") ? tag.getFloat("InfectionStrength") : 0.5f;
            return new InfectionRecord(infected, persistent, pathogenType, infectionType,
                    heartRate, lungSound, tempPlus, tempMinus,
                    redness, lesions, secretion, swelling,
                    reflexDelay, reflexStrength, neuralDamage,
                    oxygenSaturation, perfusionIndex, infectionStrength);
        }
    }
}