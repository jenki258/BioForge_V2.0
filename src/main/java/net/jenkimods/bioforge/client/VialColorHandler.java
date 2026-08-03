package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.item.vaccine.VaccineItem;
import net.jenkimods.bioforge.vaccine.VaccineProfile;
import net.jenkimods.bioforge.vaccine.DirectedVaccineProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = BioForge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class VialColorHandler {

    private static final int COLOR_UNUSED          = 0xD4C19F;
    private static final int COLOR_POSITIVE_HUMAN  = 0x91160D;
    private static final int COLOR_NEGATIVE_HUMAN  = 0xF5A7A2;
    private static final int COLOR_ANIMAL          = 0xFCF74B;

    private static final int COLOR_FULL_DEFAULT         = 0x52CFC4;
    private static final int COLOR_MUTATION_DEFAULT     = 0xA66AE3;
    private static final int COLOR_TRANSMISSION_DEFAULT = 0xE69245;
    private static final int COLOR_SYMPTOM_DEFAULT      = 0x66C878;
    private static final int COLOR_RANDOM_MUTATION_DEFAULT = 0xD43A78;

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                VialColorHandler::getVialColor,
                BioForge.ANTI_A_VIAL.get(),
                BioForge.ANTI_B_VIAL.get(),
                BioForge.ANTI_D_VIAL.get()
        );
        event.register(VialColorHandler::getVaccineColor,
                BioForge.VACCINE.get(),
                BioForge.MUTATION_VACCINE.get(),
                BioForge.TRANSMISSION_VACCINE.get(),
                BioForge.SYMPTOM_VACCINE.get(),
                BioForge.RANDOM_MUTATION_VACCINE.get());
    }

    private static int getVialColor(ItemStack stack, int tintIndex) {
        if (tintIndex == 0) return 0xFFFFFF;

        CompoundTag tag = stack.getOrCreateTag();
        boolean used    = tag.getBoolean("VialUsed");
        boolean reacted = tag.getBoolean("VialReacted");

        if (!used) return COLOR_UNUSED;

        String category = tag.getString("VialBloodCategory");
        if ("NON_HUMAN".equals(category)) return COLOR_ANIMAL;

        return reacted ? COLOR_POSITIVE_HUMAN : COLOR_NEGATIVE_HUMAN;
    }

    private static int getVaccineColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) return 0xFFFFFF;

        VaccineProfile profile = VaccineProfile.read(stack);
        DirectedVaccineProfile directed = DirectedVaccineProfile.read(stack);
        VaccineItem.Kind kind = visualKind(stack, directed);
        if (profile == null && directed == null) return defaultColor(kind);

        String payload = profile != null ? profile.strainPayload() : directed.strainPayload();
        float quality = profile != null ? profile.quality() : directed.quality();
        int hash = canonicalVisualPayload(payload).hashCode();



        float hue = baseHue(kind) + unitByte(hash) * 0.08f - 0.04f;
        float saturation = clamp(0.62f + unitByte(hash >>> 8) * 0.20f, 0.0f, 1.0f);
        float brightness = clamp(0.48f + quality * 0.42f
                + (unitByte(hash >>> 16) - 0.5f) * 0.08f, 0.0f, 1.0f);
        return hsvToRgb(hue, saturation, brightness);
    }

    private static VaccineItem.Kind visualKind(ItemStack stack,
                                                DirectedVaccineProfile directed) {
        if (directed != null) {
            return switch (directed.category()) {
                case MUTATION -> VaccineItem.Kind.MUTATION;
                case TRANSMISSION -> VaccineItem.Kind.TRANSMISSION;
                case SYMPTOM -> VaccineItem.Kind.SYMPTOM;
            };
        }
        if (stack.getItem() instanceof VaccineItem vaccine) return vaccine.getKind();
        return VaccineItem.Kind.FULL;
    }

    private static int defaultColor(VaccineItem.Kind kind) {
        return switch (kind) {
            case FULL -> COLOR_FULL_DEFAULT;
            case MUTATION -> COLOR_MUTATION_DEFAULT;
            case TRANSMISSION -> COLOR_TRANSMISSION_DEFAULT;
            case SYMPTOM -> COLOR_SYMPTOM_DEFAULT;
            case RANDOM_MUTATION -> COLOR_RANDOM_MUTATION_DEFAULT;
        };
    }

    private static float baseHue(VaccineItem.Kind kind) {
        return switch (kind) {
            case FULL -> 0.49f;
            case MUTATION -> 0.76f;
            case TRANSMISSION -> 0.075f;
            case SYMPTOM -> 0.36f;
            case RANDOM_MUTATION -> 0.94f;
        };
    }

    private static float unitByte(int value) {
        return (value & 0xFF) / 255.0f;
    }


    private static String canonicalVisualPayload(String payload) {
        if (payload == null || payload.isBlank()) return "";
        String[] sections = payload.split(";");
        String[] header = sections[0].split("\\|", -1);
        String pathogen = header.length >= 3 ? header[header.length - 2]
                : header.length > 0 ? header[0] : "";
        String transmissions = header.length >= 2 ? sortedCsv(header[header.length - 1]) : "";
        List<String> genes = new ArrayList<>();
        for (int index = 1; index < sections.length; index++) {
            String gene = sections[index];
            if (gene.startsWith("mutations=")) {
                gene = "mutations=" + sortedCsv(gene.substring("mutations=".length()));
            }
            genes.add(gene);
        }
        genes.sort(String::compareTo);
        return pathogen + "|" + transmissions + ";" + String.join(";", genes);
    }

    private static String sortedCsv(String value) {
        if (value == null || value.isBlank()) return "";
        String[] parts = value.split(",");
        Arrays.sort(parts);
        return String.join(",", parts);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int hsvToRgb(float hue, float saturation, float brightness) {
        hue = hue - (float) Math.floor(hue);
        float scaled = hue * 6.0f;
        int sector = (int) Math.floor(scaled);
        float fraction = scaled - sector;
        float p = brightness * (1.0f - saturation);
        float q = brightness * (1.0f - saturation * fraction);
        float t = brightness * (1.0f - saturation * (1.0f - fraction));
        float red;
        float green;
        float blue;
        switch (sector % 6) {
            case 0 -> { red = brightness; green = t; blue = p; }
            case 1 -> { red = q; green = brightness; blue = p; }
            case 2 -> { red = p; green = brightness; blue = t; }
            case 3 -> { red = p; green = q; blue = brightness; }
            case 4 -> { red = t; green = p; blue = brightness; }
            default -> { red = brightness; green = p; blue = q; }
        }
        return (Math.round(red * 255.0f) << 16)
                | (Math.round(green * 255.0f) << 8)
                | Math.round(blue * 255.0f);
    }
}
