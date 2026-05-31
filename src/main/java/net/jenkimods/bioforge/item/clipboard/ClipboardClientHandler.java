package net.jenkimods.bioforge.item.clipboard;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ClipboardClientHandler {
    private static int patientId = Integer.MIN_VALUE;
    private static String patientName = "";
    private static UUID subjectUUID = null;

    private static Float temperatureC = null;
    private static String heartRate = null;
    private static String lungSound = null;
    private static Float oxygenSaturation = null;
    private static Float perfusionIndex = null;
    private static Float redness = null, lesions = null, secretion = null, swelling = null;
    private static String reflexDelay = null, reflexStrength = null;

    private static boolean tempUnstable, heartUnstable, lungUnstable, o2Unstable, visualUnstable, reflexUnstable;

    private static String bloodType = null;
    private static Boolean antiA = null, antiB = null, antiD = null;
    private static boolean reagentA = false, reagentB = false, reagentD = false;

    private static UUID sessionToken = null;

    private static ItemStack findActiveClipboard() {
        Player player = Minecraft.getInstance().player;
        if (player == null || sessionToken == null) return ItemStack.EMPTY;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ClipboardItem) {
                CompoundTag tag = stack.getOrCreateTag();
                if (tag.contains("SessionToken") && tag.getUUID("SessionToken").equals(sessionToken)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static void saveToClipboardNBT() {
        if (sessionToken == null || !hasPatient()) return;

        ItemStack target = findActiveClipboard();
        if (target.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("PatientName=").append(patientName);
        sb.append(";SubjectUUID=").append(subjectUUID != null ? subjectUUID.toString() : "");
        if (temperatureC != null) sb.append(";TempC=").append(temperatureC).append(";TempUnstable=").append(tempUnstable);
        if (heartRate != null)    sb.append(";HeartRate=").append(heartRate).append(";HeartUnstable=").append(heartUnstable);
        if (lungSound != null)    sb.append(";LungSound=").append(lungSound).append(";LungUnstable=").append(lungUnstable);
        if (oxygenSaturation != null) {
            sb.append(";OxygenSaturation=").append(oxygenSaturation)
                    .append(";PerfusionIndex=").append(perfusionIndex != null ? perfusionIndex : 0.7f)
                    .append(";O2Unstable=").append(o2Unstable);
        }
        if (redness != null) {
            sb.append(";Redness=").append(redness).append(";Lesions=").append(lesions != null ? lesions : 0)
                    .append(";Secretion=").append(secretion != null ? secretion : 0)
                    .append(";Swelling=").append(swelling != null ? swelling : 0)
                    .append(";VisualUnstable=").append(visualUnstable);
        }
        if (reflexDelay != null) sb.append(";ReflexDelay=").append(reflexDelay).append(";ReflexStrength=").append(reflexStrength).append(";ReflexUnstable=").append(reflexUnstable);
        if (bloodType != null) {
            sb.append(";BloodType=").append(bloodType);
            if (antiA != null) sb.append(";AntiA=").append(antiA);
            if (antiB != null) sb.append(";AntiB=").append(antiB);
            if (antiD != null) sb.append(";AntiD=").append(antiD);
        }
        sb.append(";ReagentA=").append(reagentA);
        sb.append(";ReagentB=").append(reagentB);
        sb.append(";ReagentD=").append(reagentD);

        NbtObfuscator.writeString(target.getOrCreateTag(), sb.toString());
    }

    public static void assignPatient(int id, String name, UUID uuid, ItemStack clipboardStack) {
        patientId = id;
        patientName = name;
        subjectUUID = uuid;
        temperatureC = null;
        heartRate = null;
        lungSound = null;
        oxygenSaturation = null;
        perfusionIndex = null;
        redness = lesions = secretion = swelling = null;
        reflexDelay = null;
        reflexStrength = null;
        tempUnstable = heartUnstable = lungUnstable = o2Unstable = visualUnstable = reflexUnstable = false;
        bloodType = null;
        antiA = antiB = antiD = null;
        reagentA = reagentB = reagentD = false;

        if (id != Integer.MIN_VALUE && clipboardStack != null && !clipboardStack.isEmpty()) {
            sessionToken = UUID.randomUUID();
            clipboardStack.getOrCreateTag().putUUID("SessionToken", sessionToken);
        } else {
            sessionToken = null;
        }
        if (id != Integer.MIN_VALUE) {
            saveToClipboardNBT();
        }
    }

    public static boolean reactivateSession(ItemStack clipboard) {
        CompoundTag tag = clipboard.getOrCreateTag();
        String data = NbtObfuscator.readString(tag);
        if (data == null) return false;

        Map<String, String> map = new HashMap<>();
        for (String part : data.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }

        String name = map.getOrDefault("PatientName", "???");
        String uuidStr = map.get("SubjectUUID");
        UUID uuid = null;
        if (uuidStr != null && !uuidStr.isEmpty()) {
            try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException ignored) {}
        }

        patientName = name;
        subjectUUID = uuid;

        Player player = Minecraft.getInstance().player;
        int resolvedId = -1;
        if (uuid != null && player != null) {
            for (Player online : player.level().players()) {
                if (online.getUUID().equals(uuid)) {
                    resolvedId = online.getId();
                    break;
                }
            }
        }
        patientId = resolvedId;

        temperatureC = map.containsKey("TempC") ? Float.parseFloat(map.get("TempC")) : null;
        heartRate = map.get("HeartRate");
        lungSound = map.get("LungSound");
        oxygenSaturation = map.containsKey("OxygenSaturation") ? Float.parseFloat(map.get("OxygenSaturation")) : null;
        perfusionIndex = map.containsKey("PerfusionIndex") ? Float.parseFloat(map.get("PerfusionIndex")) : null;
        redness = map.containsKey("Redness") ? Float.parseFloat(map.get("Redness")) : null;
        lesions = map.containsKey("Lesions") ? Float.parseFloat(map.get("Lesions")) : null;
        secretion = map.containsKey("Secretion") ? Float.parseFloat(map.get("Secretion")) : null;
        swelling = map.containsKey("Swelling") ? Float.parseFloat(map.get("Swelling")) : null;
        reflexDelay = map.get("ReflexDelay");
        reflexStrength = map.get("ReflexStrength");
        bloodType = map.get("BloodType");
        antiA = map.containsKey("AntiA") ? Boolean.parseBoolean(map.get("AntiA")) : null;
        antiB = map.containsKey("AntiB") ? Boolean.parseBoolean(map.get("AntiB")) : null;
        antiD = map.containsKey("AntiD") ? Boolean.parseBoolean(map.get("AntiD")) : null;
        reagentA = map.getOrDefault("ReagentA", "false").equals("true");
        reagentB = map.getOrDefault("ReagentB", "false").equals("true");
        reagentD = map.getOrDefault("ReagentD", "false").equals("true");
        tempUnstable = map.getOrDefault("TempUnstable", "false").equals("true");
        heartUnstable = map.getOrDefault("HeartUnstable", "false").equals("true");
        lungUnstable = map.getOrDefault("LungUnstable", "false").equals("true");
        o2Unstable = map.getOrDefault("O2Unstable", "false").equals("true");
        visualUnstable = map.getOrDefault("VisualUnstable", "false").equals("true");
        reflexUnstable = map.getOrDefault("ReflexUnstable", "false").equals("true");

        if (tag.contains("SessionToken")) {
            sessionToken = tag.getUUID("SessionToken");
        } else {
            sessionToken = UUID.randomUUID();
            tag.putUUID("SessionToken", sessionToken);
        }

        return true;
    }

    public static void clearClipboardItem() {
        ItemStack active = findActiveClipboard();
        if (active != null && !active.isEmpty()) {
            CompoundTag tag = active.getOrCreateTag();
            NbtObfuscator.clear(tag);
            tag.remove("SessionToken");
        }
        sessionToken = null;
        patientId = Integer.MIN_VALUE;
        patientName = "";
        subjectUUID = null;
        temperatureC = null;
        heartRate = null;
        lungSound = null;
        oxygenSaturation = null;
        perfusionIndex = null;
        redness = lesions = secretion = swelling = null;
        reflexDelay = null;
        reflexStrength = null;
        tempUnstable = heartUnstable = lungUnstable = o2Unstable = visualUnstable = reflexUnstable = false;
        bloodType = null;
        antiA = antiB = antiD = null;
        reagentA = reagentB = reagentD = false;
    }

    public static boolean hasPatient() { return patientId != Integer.MIN_VALUE; }
    public static String getPatientName() { return patientName; }
    public static int getPatientId() { return patientId; }
    public static Float getTemperatureC() { return temperatureC; }
    public static String getHeartRate() { return heartRate; }
    public static String getLungSound() { return lungSound; }
    public static Float getOxygenSaturation() { return oxygenSaturation; }
    public static Float getPerfusionIndex() { return perfusionIndex; }
    public static Float getRedness() { return redness; }
    public static Float getLesions() { return lesions; }
    public static Float getSecretion() { return secretion; }
    public static Float getSwelling() { return swelling; }
    public static String getReflexDelay() { return reflexDelay; }
    public static String getReflexStrength() { return reflexStrength; }
    public static String getBloodType() { return bloodType; }
    public static Boolean getAntiA() { return antiA; }
    public static Boolean getAntiB() { return antiB; }
    public static Boolean getAntiD() { return antiD; }
    public static boolean isReagentA() { return reagentA; }
    public static boolean isReagentB() { return reagentB; }
    public static boolean isReagentD() { return reagentD; }
    public static boolean isBloodComplete() { return reagentA && reagentB && reagentD; }
    public static boolean isTempUnstable() { return tempUnstable; }
    public static boolean isHeartUnstable() { return heartUnstable; }
    public static boolean isLungUnstable() { return lungUnstable; }
    public static boolean isO2Unstable() { return o2Unstable; }
    public static boolean isVisualUnstable() { return visualUnstable; }
    public static boolean isReflexUnstable() { return reflexUnstable; }

    public static int getRecordedFindingsCount() {
        int count = 0;
        if (temperatureC != null) count++;
        if (heartRate != null) count++;
        if (lungSound != null) count++;
        if (oxygenSaturation != null) count++;
        if (redness != null) count++;
        if (reflexDelay != null) count++;
        if (isBloodComplete()) count++;
        return count;
    }

    public static void recordTemperature(float celsius, boolean unstable) {
        temperatureC = celsius; tempUnstable = unstable;
        saveToClipboardNBT();
    }
    public static void recordHeart(String rate, boolean unstable) {
        heartRate = rate; heartUnstable = unstable;
        saveToClipboardNBT();
    }
    public static void recordLungs(String sound, boolean unstable) {
        lungSound = sound; lungUnstable = unstable;
        saveToClipboardNBT();
    }
    public static void recordOxygenSat(float o2, float perf, boolean unstable) {
        oxygenSaturation = o2; perfusionIndex = perf; o2Unstable = unstable;
        saveToClipboardNBT();
    }
    public static void recordVisual(float red, float les, float sec, float swe, boolean unstable) {
        redness = red; lesions = les; secretion = sec; swelling = swe; visualUnstable = unstable;
        saveToClipboardNBT();
    }
    public static void recordReflex(String delay, String strength, boolean unstable) {
        reflexDelay = delay; reflexStrength = strength; reflexUnstable = unstable;
        saveToClipboardNBT();
    }
    public static void recordBloodData(String type, Boolean a, Boolean b, Boolean d) {
        bloodType = type;
        if (a != null) { antiA = a; reagentA = true; }
        if (b != null) { antiB = b; reagentB = true; }
        if (d != null) { antiD = d; reagentD = true; }
        saveToClipboardNBT();
    }

    private static void clearInMemoryFields() {
        patientId = Integer.MIN_VALUE;
        patientName = "";
        subjectUUID = null;
        temperatureC = null;
        heartRate = null;
        lungSound = null;
        oxygenSaturation = null;
        perfusionIndex = null;
        redness = lesions = secretion = swelling = null;
        reflexDelay = null;
        reflexStrength = null;
        tempUnstable = heartUnstable = lungUnstable = o2Unstable = visualUnstable = reflexUnstable = false;
        bloodType = null;
        antiA = antiB = antiD = null;
        reagentA = reagentB = reagentD = false;
    }

    public static void recordBloodData(String bloodType, Boolean antiA, Boolean antiB, Boolean antiD, UUID subjectUUID) {
        if (bloodType != null) ClipboardClientHandler.bloodType = bloodType;
        if (antiA != null) {
            ClipboardClientHandler.antiA = antiA;
            ClipboardClientHandler.reagentA = true;
        }
        if (antiB != null) {
            ClipboardClientHandler.antiB = antiB;
            ClipboardClientHandler.reagentB = true;
        }
        if (antiD != null) {
            ClipboardClientHandler.antiD = antiD;
            ClipboardClientHandler.reagentD = true;
        }

        updateBloodDataOnClipboard(subjectUUID, bloodType, antiA, antiB, antiD);
    }

    public static void updateBloodDataOnClipboard(UUID subjectUUID, String bloodType,
                                                  Boolean antiA, Boolean antiB, Boolean antiD) {
        Player player = Minecraft.getInstance().player;
        if (player == null || subjectUUID == null) return;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ClipboardItem)) continue;

            String data = NbtObfuscator.readString(stack.getOrCreateTag());
            if (data == null) continue;

            Map<String, String> map = new HashMap<>();
            for (String part : data.split(";")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) map.put(kv[0], kv[1]);
            }

            String storedUuid = map.get("SubjectUUID");
            if (storedUuid == null || !storedUuid.equals(subjectUUID.toString())) continue;

            if (bloodType != null) ClipboardClientHandler.bloodType = bloodType;
            if (antiA != null) {
                ClipboardClientHandler.antiA = antiA;
                ClipboardClientHandler.reagentA = true;
            }
            if (antiB != null) {
                ClipboardClientHandler.antiB = antiB;
                ClipboardClientHandler.reagentB = true;
            }
            if (antiD != null) {
                ClipboardClientHandler.antiD = antiD;
                ClipboardClientHandler.reagentD = true;
            }

            if (bloodType != null) map.put("BloodType", bloodType);
            if (antiA != null) { map.put("AntiA", antiA.toString()); map.put("ReagentA", "true"); }
            if (antiB != null) { map.put("AntiB", antiB.toString()); map.put("ReagentB", "true"); }
            if (antiD != null) { map.put("AntiD", antiD.toString()); map.put("ReagentD", "true"); }

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
            }
            NbtObfuscator.writeString(stack.getOrCreateTag(), sb.toString());
            break;
        }
    }

    public static UUID getSubjectUUID() {
        return subjectUUID;
    }
}