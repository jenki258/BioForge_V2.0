package net.jenkimods.bioforge.item.clipboard;

import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicalReportItem extends Item {
    public MedicalReportItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag == null || NbtObfuscator.readString(tag) == null) {
            tooltip.add(Component.translatable("item.bioforge.medical_report.blank").withStyle(ChatFormatting.GRAY));
            return;
        }

        String data = NbtObfuscator.readString(tag);
        Map<String, String> map = new HashMap<>();
        if (data != null) {
            for (String part : data.split(";")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) map.put(kv[0], kv[1]);
            }
        }

        String patient = map.getOrDefault("PatientName", "???");
        tooltip.add(Component.translatable("item.bioforge.medical_report.patient", patient)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(""));

        // Vitals
        tooltip.add(Component.translatable("clipboard.section.vital").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA));
        if (map.containsKey("TempC")) {
            float temp = Float.parseFloat(map.get("TempC"));
            String status = temp >= 38.5f ? "Fever" : (temp <= 35.5f ? "Hypothermia" : "Normal");
            String unstable = map.get("TempUnstable").equals("true") ? " (?)" : "";
            tooltip.add(Component.translatable("clipboard.entry.temperature", String.format("%.1f°C (%s)", temp, status) + unstable)
                    .withStyle(ChatFormatting.WHITE));
        } else { tooltip.add(Component.translatable("clipboard.no_data").withStyle(ChatFormatting.DARK_GRAY)); }

        if (map.containsKey("HeartRate")) {
            String rate = Component.translatable("clipboard.stethoscope." + map.get("HeartRate").toLowerCase()).getString();
            String unstable = map.get("HeartUnstable").equals("true") ? " (?)" : "";
            tooltip.add(Component.translatable("clipboard.entry.heart", rate + unstable).withStyle(ChatFormatting.WHITE));
        } else { tooltip.add(Component.translatable("clipboard.no_data").withStyle(ChatFormatting.DARK_GRAY)); }

        if (map.containsKey("OxygenSaturation")) {
            float o2 = Float.parseFloat(map.get("OxygenSaturation"));
            float pi = Float.parseFloat(map.getOrDefault("PerfusionIndex", "0.7"));
            String piDesc = pi > 0.7f ? "Strong" : (pi > 0.3f ? "Moderate" : "Weak");
            String unstable = map.get("O2Unstable").equals("true") ? " (?)" : "";
            tooltip.add(Component.translatable("clipboard.entry.oxygen", String.format("%.0f%%", o2 * 100f), piDesc + unstable).withStyle(ChatFormatting.WHITE));
        } else { tooltip.add(Component.translatable("clipboard.no_data").withStyle(ChatFormatting.DARK_GRAY)); }

        // Respiratory
        tooltip.add(Component.translatable("clipboard.section.respiratory").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA));
        if (map.containsKey("LungSound")) {
            String sound = Component.translatable("clipboard.stethoscope." + map.get("LungSound").toLowerCase()).getString();
            String unstable = map.get("LungUnstable").equals("true") ? " (?)" : "";
            tooltip.add(Component.translatable("clipboard.entry.lungs", sound + unstable).withStyle(ChatFormatting.WHITE));
        } else { tooltip.add(Component.translatable("clipboard.no_data").withStyle(ChatFormatting.DARK_GRAY)); }

        // Neurological
        tooltip.add(Component.translatable("clipboard.section.neurological").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA));
        if (map.containsKey("ReflexDelay")) {
            String delay = Component.translatable("clipboard.reflex." + map.get("ReflexDelay").toLowerCase()).getString();
            String strength = Component.translatable("clipboard.reflex." + map.get("ReflexStrength").toLowerCase()).getString();
            String unstable = map.get("ReflexUnstable").equals("true") ? " (?)" : "";
            tooltip.add(Component.translatable("clipboard.entry.reflex", delay, strength, unstable).withStyle(ChatFormatting.WHITE));
        } else { tooltip.add(Component.translatable("clipboard.no_data").withStyle(ChatFormatting.DARK_GRAY)); }

        // Visual
        tooltip.add(Component.translatable("clipboard.section.visual").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA));
        if (map.containsKey("Redness")) {
            String unstable = map.get("VisualUnstable").equals("true") ? " (?)" : "";
            tooltip.add(Component.translatable("clipboard.entry.redness", describeVisual(Float.parseFloat(map.get("Redness"))) + unstable).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("clipboard.entry.lesions", describeVisual(Float.parseFloat(map.get("Lesions"))) + unstable).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("clipboard.entry.secretion", describeVisual(Float.parseFloat(map.get("Secretion"))) + unstable).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("clipboard.entry.swelling", describeVisual(Float.parseFloat(map.get("Swelling"))) + unstable).withStyle(ChatFormatting.WHITE));
        } else { tooltip.add(Component.translatable("clipboard.no_data").withStyle(ChatFormatting.DARK_GRAY)); }

        // Blood – only show if all reagents present
        tooltip.add(Component.translatable("clipboard.section.blood").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA));
        boolean hasAntiA = map.containsKey("AntiA");
        boolean hasAntiB = map.containsKey("AntiB");
        boolean hasAntiD = map.containsKey("AntiD");
        if (!hasAntiA || !hasAntiB || !hasAntiD) {
            tooltip.add(Component.translatable("clipboard.blood.incomplete").withStyle(ChatFormatting.DARK_GRAY));
        } else if (map.containsKey("BloodType")) {
            tooltip.add(Component.translatable("clipboard.entry.blood_group", map.get("BloodType")).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("clipboard.entry.anti_a", map.get("AntiA").equals("true") ? "+" : "-").withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("clipboard.entry.anti_b", map.get("AntiB").equals("true") ? "+" : "-").withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("clipboard.entry.anti_d", map.get("AntiD").equals("true") ? "+" : "-").withStyle(ChatFormatting.WHITE));
        }
    }

    private String describeVisual(float val) {
        if (val > 0.7f) return "High";
        if (val > 0.3f) return "Moderate";
        if (val > 0f) return "Low";
        return "None";
    }
}