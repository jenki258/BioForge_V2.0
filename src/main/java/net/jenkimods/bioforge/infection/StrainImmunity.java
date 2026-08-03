package net.jenkimods.bioforge.infection;

import java.util.Locale;







public record StrainImmunity(String fingerprint, String displayName, int remainingTicks) {
    public static final int MAX_NAME_LENGTH = 96;

    public StrainImmunity {
        fingerprint = normalizeFingerprint(fingerprint);
        displayName = sanitizeName(displayName, fingerprint);
        remainingTicks = Math.max(0, remainingTicks);
    }

    public boolean isActive() {
        return !fingerprint.isEmpty() && remainingTicks > 0;
    }

    public StrainImmunity tick() {
        return new StrainImmunity(fingerprint, displayName, remainingTicks - 1);
    }

    public static String normalizeFingerprint(String fingerprint) {
        if (fingerprint == null) return "";
        String normalized = fingerprint.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 64) normalized = normalized.substring(0, 64);
        return normalized.replaceAll("[^A-Z0-9_-]", "");
    }

    public static String sanitizeName(String name, String fallbackFingerprint) {
        String cleaned = name == null ? "" : name.strip();
        if (cleaned.length() > MAX_NAME_LENGTH) cleaned = cleaned.substring(0, MAX_NAME_LENGTH);
        return cleaned.isEmpty() ? fallbackFingerprint : cleaned;
    }
}
