package net.jenkimods.bioforge.physiology;

import net.jenkimods.bioforge.config.BioForgeServerConfig;
import net.jenkimods.bioforge.infection.HeartRate;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class PhysiologyReadings {
    private PhysiologyReadings() { }
    public static PhysiologyProfile profile(LivingEntity entity) {
        if (!(entity instanceof Player player) || !(entity.level() instanceof net.minecraft.server.level.ServerLevel level))
            return PhysiologyProfile.STANDARD;
        var override = PlayerPhysiologyData.get(level.getServer()).profile(player.getUUID());
        return override != null ? override : PhysiologyProfile.resolve(player.getUUID(),player.getGameProfile().getName(),
                BioForgeServerConfig.playerPhysiologyProfiles(),BioForgeServerConfig.defaultPlayerPhysiology());
    }

    public static void record(CompoundTag report, String key, float clinicalValue) {
        CompoundTag readings = NbtObfuscator.readCompound(report, "physiology_readings");
        if (readings == null) readings = new CompoundTag();
        readings.putFloat(key, clinicalValue);
        NbtObfuscator.writeCompound(report, "physiology_readings", readings);
    }

    public static void recordHeart(CompoundTag report, HeartRate clinical) {
        CompoundTag readings = NbtObfuscator.readCompound(report, "physiology_readings");
        if (readings == null) readings = new CompoundTag();
        readings.putString("HeartRate", clinical.name());
        NbtObfuscator.writeCompound(report, "physiology_readings", readings);
    }

    public static float clinical(CompoundTag report, String key) {
        CompoundTag readings = NbtObfuscator.readCompound(report, "physiology_readings");
        return readings != null && readings.contains(key) ? readings.getFloat(key) : report.getFloat(key);
    }

    public static HeartRate clinicalHeart(CompoundTag report) {
        CompoundTag readings = NbtObfuscator.readCompound(report, "physiology_readings");
        return HeartRate.fromName(readings != null && readings.contains("HeartRate")
                ? readings.getString("HeartRate") : report.getString("HeartRate"));
    }

    public static String temperatureStatus(CompoundTag report) {
        float value = clinical(report, "TemperatureC");
        // Account for float rounding at the exact legacy fever/cold boundaries.
        return value >= 38.499F ? "clipboard.temperature.fever"
                : value <= 35.501F ? "clipboard.temperature.hypothermia" : "clipboard.temperature.normal";
    }
}
