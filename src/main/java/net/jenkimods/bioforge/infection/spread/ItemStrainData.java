package net.jenkimods.bioforge.infection.spread;

import net.jenkimods.bioforge.infection.StrainData;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.UseAnim;
import net.jenkimods.bioforge.infection.InfectionType;
import net.jenkimods.bioforge.api.definition.BioForgeIds;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class ItemStrainData {
    private static final String CONTAMINATION_CHANNEL = "item_contamination";

    private ItemStrainData() {}

    @Nullable
    public static StrainData read(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return null;
        var tag = stack.getOrCreateTag();
        StrainData named = parse(NbtObfuscator.readString(tag, CONTAMINATION_CHANNEL));
        if (named != null) return forItem(stack, named);
        StrainData infection = parse(NbtObfuscator.readInfection(tag));
        if (infection != null) return forItem(stack, infection);
        return forItem(stack, parse(NbtObfuscator.readString(tag)));
    }

    public static void write(ItemStack stack, StrainData strain) {
        if (stack == null || stack.isEmpty() || strain == null || strain.getPathogenId() == null) return;
        NbtObfuscator.writeStringDeterministic(stack.getOrCreateTag(),
                CONTAMINATION_CHANNEL, forItem(stack, strain).toPayload());
    }

    @Nullable
    private static StrainData forItem(ItemStack stack, @Nullable StrainData strain) {
        if (strain == null) return null;
        if (stack.getItem() instanceof PotionItem || stack.getUseAnimation() == UseAnim.DRINK)
            return strain.withTransmissions(Set.of(BioForgeIds.transmission(InfectionType.WATER_BORNE)));
        if (stack.isEdible())
            return strain.withTransmissions(Set.of(BioForgeIds.transmission(InfectionType.FOOD_BORNE)));
        return strain;
    }

    public static boolean clear(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return false;
        var tag = stack.getOrCreateTag();
        boolean contaminated = read(stack) != null;
        NbtObfuscator.clear(tag, CONTAMINATION_CHANNEL);
        NbtObfuscator.clearInfection(tag);
        if (parse(NbtObfuscator.readString(tag)) != null) {
            NbtObfuscator.clear(tag);
        }
        if (tag.isEmpty()) stack.setTag(null);
        return contaminated;
    }

    @Nullable
    private static StrainData parse(@Nullable String payload) {
        if (payload == null || payload.isBlank() || "CLEAN".equals(payload)) return null;
        StrainData strain = StrainData.parse(payload);
        return strain.getPathogenId() == null ? null : strain;
    }

    public static StrainData stronger(@Nullable StrainData first, @Nullable StrainData second) {
        if (first == null) return second;
        if (second == null) return first;
        float firstStrength = strength(first);
        float secondStrength = strength(second);
        if (secondStrength > firstStrength) return second;
        if (firstStrength > secondStrength) return first;
        return second.toCanonicalGeneticPayload().compareTo(first.toCanonicalGeneticPayload()) < 0
                ? second : first;
    }

    private static float strength(StrainData strain) {
        return strain.getSymptom("infection_strength")
                .map(value -> {
                    try {
                        return Float.parseFloat(value);
                    } catch (NumberFormatException ignored) {
                        return 0.5F;
                    }
                }).orElse(0.5F);
    }
}
