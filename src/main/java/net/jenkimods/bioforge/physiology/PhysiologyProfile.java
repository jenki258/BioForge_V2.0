package net.jenkimods.bioforge.physiology;

import net.jenkimods.bioforge.infection.HeartRate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record PhysiologyProfile(double temperatureC, HeartRate heartRate, float oxygen, float perfusion) {
    public static final PhysiologyProfile STANDARD = new PhysiologyProfile(36.6D, HeartRate.NORMAL, .95F, .7F);

    public PhysiologyProfile {
        if (!Double.isFinite(temperatureC) || temperatureC < 0 || temperatureC > 100
                || heartRate == null || !Float.isFinite(oxygen) || oxygen < 0 || oxygen > 1
                || !Float.isFinite(perfusion) || perfusion < 0 || perfusion > 1)
            throw new IllegalArgumentException("Invalid physiological baseline");
    }

    public record Entry(String selector, PhysiologyProfile profile) { }

    public static Entry parse(String text) {
        String[] parts = text.split("\\|", -1);
        if (parts.length != 5) throw new IllegalArgumentException("Expected player|temperature|heart|oxygen|perfusion");
        String selector = parts[0].trim();
        if (!selector.matches("[A-Za-z0-9_]{1,16}")) {
            UUID uuid = UUID.fromString(selector);
            if (!uuid.toString().equalsIgnoreCase(selector)) throw new IllegalArgumentException("Noncanonical UUID");
        }
        return new Entry(selector.toLowerCase(Locale.ROOT), new PhysiologyProfile(
                Double.parseDouble(parts[1].trim()), HeartRate.valueOf(parts[2].trim().toUpperCase(Locale.ROOT)),
                Float.parseFloat(parts[3].trim()), Float.parseFloat(parts[4].trim())));
    }

    public static boolean validEntry(Object value) {
        if (!(value instanceof String text)) return false;
        try { parse(text); return true; } catch (IllegalArgumentException exception) { return false; }
    }

    public static PhysiologyProfile resolve(UUID uuid, String name, List<? extends String> entries, PhysiologyProfile fallback) {
        PhysiologyProfile named = fallback, identified = null;
        for (String text : entries) {
            if (!validEntry(text)) continue;
            Entry entry = parse(text);
            if (entry.selector().equals(uuid.toString())) identified = entry.profile();
            else if (entry.selector().equalsIgnoreCase(name)) named = entry.profile();
        }
        return identified == null ? named : identified;
    }

    public double temperature(boolean fever, boolean cold) {
        return temperatureC + (fever ? 1.9D : cold ? -1.1D : 0.0D);
    }

    public HeartRate heart(HeartRate symptom) { return symptom == HeartRate.NORMAL ? heartRate : symptom; }
    public float oxygen(float symptom) { return shifted(symptom, .95F, oxygen); }
    public float perfusion(float symptom) { return shifted(symptom, .7F, perfusion); }
    private static float shifted(float symptom, float standard, float baseline) {
        return Math.max(0F, Math.min(1F, symptom + baseline - standard));
    }
}
