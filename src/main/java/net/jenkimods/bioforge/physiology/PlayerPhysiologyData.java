package net.jenkimods.bioforge.physiology;

import net.jenkimods.bioforge.infection.HeartRate;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerPhysiologyData extends SavedData {
    private final Map<UUID, PhysiologyProfile> profiles = new HashMap<>();
    public static PlayerPhysiologyData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PlayerPhysiologyData::load,
                PlayerPhysiologyData::new, "bioforge_player_physiology");
    }
    public PhysiologyProfile profile(UUID player) { return profiles.get(player); }
    public void set(UUID player, PhysiologyProfile profile) { profiles.put(player,profile); setDirty(); }
    public boolean reset(UUID player) {
        if (profiles.remove(player)==null) return false;
        setDirty(); return true;
    }
    public static PlayerPhysiologyData load(CompoundTag tag) {
        var result = new PlayerPhysiologyData();
        var data = NbtObfuscator.readCompound(tag,"player_physiology");
        if (data == null) return result;
        for (String key : data.getAllKeys()) {
            try {
                var entry = data.getCompound(key);
                result.profiles.put(UUID.fromString(key),new PhysiologyProfile(entry.getDouble("Temperature"),
                        HeartRate.valueOf(entry.getString("Heart")),entry.getFloat("Oxygen"),entry.getFloat("Perfusion")));
            } catch (IllegalArgumentException ignored) { }
        }
        return result;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var data = new CompoundTag();
        profiles.forEach((uuid,profile)->{
            var entry = new CompoundTag(); entry.putDouble("Temperature",profile.temperatureC());
            entry.putString("Heart",profile.heartRate().name()); entry.putFloat("Oxygen",profile.oxygen());
            entry.putFloat("Perfusion",profile.perfusion()); data.put(uuid.toString(),entry);
        });
        NbtObfuscator.writeCompound(tag,"player_physiology",data); return tag;
    }
}
