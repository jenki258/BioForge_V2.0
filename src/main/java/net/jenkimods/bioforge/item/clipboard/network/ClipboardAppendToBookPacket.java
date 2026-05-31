package net.jenkimods.bioforge.item.clipboard.network;

import net.jenkimods.bioforge.item.clipboard.ClipboardItem;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class ClipboardAppendToBookPacket {

    private final String data;
    private static final int MAX_CHARS_PER_PAGE = 130;

    public ClipboardAppendToBookPacket(String data) {
        this.data = data;
    }

    public static void encode(ClipboardAppendToBookPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.data);
    }

    public static ClipboardAppendToBookPacket decode(FriendlyByteBuf buf) {
        return new ClipboardAppendToBookPacket(buf.readUtf());
    }

    public static void handle(ClipboardAppendToBookPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack clipboard = player.getMainHandItem();
            if (!(clipboard.getItem() instanceof ClipboardItem)) return;

            InteractionHand mainHand = player.getUsedItemHand();
            InteractionHand otherHand = mainHand == InteractionHand.MAIN_HAND
                    ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack bookStack = player.getItemInHand(otherHand);
            if (!bookStack.is(Items.WRITABLE_BOOK)) return;

            CompoundTag bookTag = bookStack.getOrCreateTag();
            ListTag pages = bookTag.getList("pages", 8);

            String reportText = buildPlainTextReport(msg.data, player);
            List<String> allPages = new ArrayList<>();
            if (!pages.isEmpty()) {
                for (int i = 0; i < pages.size(); i++) {
                    allPages.add(pages.getString(i));
                }
                int last = allPages.size() - 1;
                allPages.set(last, allPages.get(last) + "\n" + reportText);
            } else {
                allPages.add(reportText);
            }

            List<String> finalPages = new ArrayList<>();
            for (String full : allPages) {
                finalPages.addAll(splitIntoPages(full, MAX_CHARS_PER_PAGE));
            }

            ListTag newPages = new ListTag();
            for (String p : finalPages) {
                newPages.add(StringTag.valueOf(p));
            }
            bookTag.put("pages", newPages);

            NbtObfuscator.clear(clipboard.getOrCreateTag());
            clipboard.getOrCreateTag().remove("SessionToken");
        });
        ctx.get().setPacketHandled(true);
    }

    private static List<String> splitIntoPages(String text, int maxChars) {
        List<String> pages = new ArrayList<>();
        if (text == null || text.isEmpty()) return pages;

        String[] lines = text.split("\n", -1);
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            while (line.length() > maxChars) {
                if (current.length() > 0) {
                    pages.add(current.toString());
                    current = new StringBuilder();
                }
                pages.add(line.substring(0, maxChars));
                line = line.substring(maxChars);
            }

            int addLength = line.length() + (current.length() > 0 ? 1 : 0);
            if (current.length() + addLength > maxChars && current.length() > 0) {
                pages.add(current.toString());
                current = new StringBuilder();
            }

            if (current.length() > 0) {
                current.append("\n");
            }
            current.append(line);
        }

        if (current.length() > 0) {
            pages.add(current.toString());
        }
        return pages;
    }

    private static String buildPlainTextReport(String raw, ServerPlayer player) {
        Map<String, String> map = parseRawData(raw);
        StringBuilder sb = new StringBuilder();

        String patient = map.getOrDefault("PatientName", "???");
        sb.append("Patient: ").append(patient).append("\n\n");

        sb.append(translate(player, "clipboard.section.vital")).append("\n");
        if (map.containsKey("TempC")) {
            float temp = Float.parseFloat(map.get("TempC"));
            String statusKey = temp >= 38.5f ? "clipboard.stethoscope.tachy" : (temp <= 35.5f ? "clipboard.stethoscope.brady" : "clipboard.stethoscope.normal");
            String status = translate(player, statusKey);
            String unstable = map.getOrDefault("TempUnstable", "false").equals("true") ? " (?)" : "";
            String tempStr = translate(player, "clipboard.entry.temperature",
                    String.format("%.1f°C", temp) + " (" + status + ")" + unstable);
            sb.append(tempStr).append("\n");
        } else {
            sb.append("Temperature: --").append("\n");
        }

        if (map.containsKey("HeartRate")) {
            String rateKey = "clipboard.stethoscope." + map.get("HeartRate").toLowerCase();
            String rate = translate(player, rateKey);
            String unstable = map.getOrDefault("HeartUnstable", "false").equals("true") ? " (?)" : "";
            String heartStr = translate(player, "clipboard.entry.heart", rate + unstable);
            sb.append(heartStr).append("\n");
        } else {
            sb.append("Heart Rate: --").append("\n");
        }

        if (map.containsKey("OxygenSaturation")) {
            float o2 = Float.parseFloat(map.get("OxygenSaturation"));
            float pi = Float.parseFloat(map.getOrDefault("PerfusionIndex", "0.7"));
            String piKey = pi > 0.7f ? "clipboard.pi.strong" : (pi > 0.3f ? "clipboard.pi.moderate" : "clipboard.pi.weak");
            String piDesc = translate(player, piKey);
            String unstable = map.getOrDefault("O2Unstable", "false").equals("true") ? " (?)" : "";
            String o2Str = translate(player, "clipboard.entry.oxygen",
                    String.format("%.0f%%", o2 * 100), piDesc + unstable);
            sb.append(o2Str).append("\n");
        } else {
            sb.append("SpO₂: --").append("\n");
        }

        sb.append("\n").append(translate(player, "clipboard.section.respiratory")).append("\n");
        if (map.containsKey("LungSound")) {
            String soundKey = "clipboard.stethoscope." + map.get("LungSound").toLowerCase();
            String sound = translate(player, soundKey);
            String unstable = map.getOrDefault("LungUnstable", "false").equals("true") ? " (?)" : "";
            String lungStr = translate(player, "clipboard.entry.lungs", sound + unstable);
            sb.append(lungStr).append("\n");
        } else {
            sb.append("Lung Sounds: --").append("\n");
        }

        sb.append("\n").append(translate(player, "clipboard.section.neurological")).append("\n");
        if (map.containsKey("ReflexDelay")) {
            String delayKey = "clipboard.reflex." + map.get("ReflexDelay").toLowerCase();
            String strengthKey = "clipboard.reflex." + map.getOrDefault("ReflexStrength", "moderate").toLowerCase();
            String delay = translate(player, delayKey);
            String strength = translate(player, strengthKey);
            String unstable = map.getOrDefault("ReflexUnstable", "false").equals("true") ? " (?)" : "";
            String reflexStr = translate(player, "clipboard.entry.reflex", delay, strength, unstable);
            sb.append(reflexStr).append("\n");
        } else {
            sb.append("Reflex: --").append("\n");
        }

        sb.append("\n").append(translate(player, "clipboard.section.visual")).append("\n");
        if (map.containsKey("Redness")) {
            String unstable = map.getOrDefault("VisualUnstable", "false").equals("true") ? " (?)" : "";
            sb.append(translate(player, "clipboard.entry.redness", visualDesc(map.get("Redness")) + unstable)).append("\n");
            sb.append(translate(player, "clipboard.entry.lesions", visualDesc(map.get("Lesions")) + unstable)).append("\n");
            sb.append(translate(player, "clipboard.entry.secretion", visualDesc(map.get("Secretion")) + unstable)).append("\n");
            sb.append(translate(player, "clipboard.entry.swelling", visualDesc(map.get("Swelling")) + unstable)).append("\n");
        } else {
            sb.append("Redness: --").append("\n");
            sb.append("Lesions: --").append("\n");
            sb.append("Secretion: --").append("\n");
            sb.append("Swelling: --").append("\n");
        }

        sb.append("\n").append(translate(player, "clipboard.section.blood")).append("\n");
        boolean reagentA = map.getOrDefault("ReagentA", "false").equals("true");
        boolean reagentB = map.getOrDefault("ReagentB", "false").equals("true");
        boolean reagentD = map.getOrDefault("ReagentD", "false").equals("true");
        if (!reagentA || !reagentB || !reagentD) {
            sb.append(translate(player, "clipboard.blood.incomplete")).append("\n");
        } else if (map.containsKey("BloodType")) {
            sb.append(translate(player, "clipboard.entry.blood_group", map.get("BloodType"))).append("\n");
            String antiA = map.get("AntiA").equals("true") ? "+" : "-";
            String antiB = map.get("AntiB").equals("true") ? "+" : "-";
            String antiD = map.get("AntiD").equals("true") ? "+" : "-";
            sb.append(translate(player, "clipboard.entry.anti_a", antiA)).append("\n");
            sb.append(translate(player, "clipboard.entry.anti_b", antiB)).append("\n");
            sb.append(translate(player, "clipboard.entry.anti_d", antiD)).append("\n");
        }

        return sb.toString();
    }

    private static String translate(ServerPlayer player, String key, String... args) {
        Language language = Language.getInstance();
        String pattern = language.getOrDefault(key);
        if (args.length > 0) {
            String result = pattern;
            for (String arg : args) {
                result = result.replaceFirst("%s", arg);
            }
            return result;
        }
        return pattern;
    }

    private static String visualDesc(String value) {
        try {
            float val = Float.parseFloat(value);
            if (val > 0.7f) return translate(null, "clipboard.visual.high");
            if (val > 0.3f) return translate(null, "clipboard.visual.moderate");
            if (val > 0.0f) return translate(null, "clipboard.visual.low");
            return translate(null, "clipboard.visual.none");
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private static Map<String, String> parseRawData(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null) return map;
        for (String part : raw.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }
}