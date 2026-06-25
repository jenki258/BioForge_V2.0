package net.jenkimods.bioforge.world.microscope;

import java.util.*;

public class MicroscopeClientData {
    private static Map<String, Object> symptoms = new LinkedHashMap<>();
    private static String visibility = "NONE";
    private static List<MicroscopeSymptomEntry> entries = new ArrayList<>();

    public static void set(Map<String, Object> sym, String vis, List<MicroscopeSymptomEntry> ent) {
        symptoms = sym;
        visibility = vis;
        entries = ent;
    }

    public static Map<String, Object> getSymptoms() { return symptoms; }
    public static String getVisibility() { return visibility; }
    public static List<MicroscopeSymptomEntry> getEntries() { return entries; }
    public static void clear() { symptoms.clear(); visibility = "NONE"; entries.clear(); }
}