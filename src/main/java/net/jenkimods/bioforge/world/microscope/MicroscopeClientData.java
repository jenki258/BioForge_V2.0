package net.jenkimods.bioforge.world.microscope;

import java.util.LinkedHashMap;
import java.util.Map;

public class MicroscopeClientData {
    private static Map<String, Object> currentSymptoms = new LinkedHashMap<>();
    private static String currentVisibility = "NONE";

    public static void set(Map<String, Object> data, String visibility) {
        currentSymptoms = data;
        currentVisibility = visibility;
    }

    public static Map<String, Object> get() { return currentSymptoms; }
    public static String getVisibility() { return currentVisibility; }
    public static void clear() { currentSymptoms.clear(); currentVisibility = "NONE"; }
}