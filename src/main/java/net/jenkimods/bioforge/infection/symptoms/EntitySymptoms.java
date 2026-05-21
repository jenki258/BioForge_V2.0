package net.jenkimods.bioforge.infection.symptoms;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class EntitySymptoms {

    private final Map<String, Object> values = new HashMap<>();

    public <T> T get(SymptomKey<T> key) {
        Object val = values.get(key.getId());
        if (val == null) return key.getDefaultValue();
        if (key.getType().isInstance(val)) return key.getType().cast(val);
        return key.getDefaultValue();
    }

    public <T> void set(SymptomKey<T> key, T value) {
        if (value == null) {
            values.remove(key.getId());
        } else {
            values.put(key.getId(), value);
        }
    }

    public <T> void clear(SymptomKey<T> key) {
        values.remove(key.getId());
    }

    public void clearAll() {
        values.clear();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Boolean b) tag.putBoolean(entry.getKey(), b);
            else if (val instanceof Integer i) tag.putInt(entry.getKey(), i);
            else if (val instanceof Float f) tag.putFloat(entry.getKey(), f);
            else if (val instanceof String s) tag.putString(entry.getKey(), s);
            else if (val instanceof Enum<?> e) tag.putString(entry.getKey(), e.name());
        }
        return tag;
    }

    public void deserializeNBT(@NotNull CompoundTag tag, SymptomDeserializer deserializer) {
        values.clear();
        for (String key : tag.getAllKeys()) {
            Object resolved = deserializer.resolve(key, tag);
            if (resolved != null) values.put(key, resolved);
        }
    }
}
