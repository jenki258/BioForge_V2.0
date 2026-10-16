package net.jenkimods.bioforge.world.splicer;

import net.jenkimods.bioforge.api.definition.BioForgeIds;
import net.jenkimods.bioforge.config.BioForgeServerConfig;
import net.jenkimods.bioforge.crispr.StrainSampleUtil;
import net.jenkimods.bioforge.crispr.VaccineTargetCategory;
import net.jenkimods.bioforge.definition.BioForgeDefinitionManager;
import net.jenkimods.bioforge.infection.StrainData;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.item.crispr.GeneImprintItem;
import net.jenkimods.bioforge.mutation.MutationLoader;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Culture reserves live on the sample, so moving it cannot reset the cost of editing. */
public final class SplicerCulture {
    private static final String TAG = "BioForgeSplicerCulture";
    public static final int MAX_BIOMASS = 3;
    private SplicerCulture() {}
    public record Gene(VaccineTargetCategory category, String target) {}
    private static CompoundTag read(ItemStack stack) {
        CompoundTag encoded = NbtObfuscator.readCompound(stack.getOrCreateTag(), "splicer_culture");
        return encoded != null ? encoded : stack.getOrCreateTag().getCompound(TAG).copy();
    }
    private static void write(ItemStack stack, CompoundTag state) {
        NbtObfuscator.writeCompound(stack.getOrCreateTag(), "splicer_culture", state);
        stack.getOrCreateTag().remove(TAG);
    }
    public static int biomass(ItemStack stack) {
        var state = read(stack);
        return state.contains("Biomass") ? Math.max(0, Math.min(MAX_BIOMASS, state.getInt("Biomass"))) : MAX_BIOMASS;
    }
    public static int stability(ItemStack stack) {
        var state = read(stack);
        return state.contains("Stability") ? Math.max(0, Math.min(100, state.getInt("Stability"))) : 100;
    }
    public static void reserves(ItemStack stack, int biomass, int stability) {
        CompoundTag state = read(stack);
        state.putInt("Biomass", Math.max(0, Math.min(MAX_BIOMASS, biomass)));
        state.putInt("Stability", Math.max(0, Math.min(100, stability)));
        write(stack, state);
    }
    public static boolean analyzed(ItemStack stack) {
        StrainData strain = StrainSampleUtil.getStrain(stack);
        return strain != null && strain.toCanonicalGeneticPayload().equals(read(stack).getString("Analyzed"));
    }
    public static void analyze(ItemStack stack) {
        StrainData strain = StrainSampleUtil.getStrain(stack);
        if (strain == null) return;
        reserves(stack, biomass(stack), stability(stack));
        var state = read(stack); state.putString("Analyzed", strain.toCanonicalGeneticPayload()); write(stack, state);
    }
    public static boolean examined(ItemStack stack, VaccineTargetCategory category) {
        return analyzed(stack) && (read(stack).getInt("Examined") & (1 << category.ordinal())) != 0;
    }
    public static void examine(ItemStack stack, VaccineTargetCategory category) {
        var state = read(stack); state.putInt("Examined", state.getInt("Examined") | (1 << category.ordinal())); write(stack, state);
    }
    public static List<Gene> genes(StrainData strain, VaccineTargetCategory category) {
        return GeneImprintItem.availableGenes(strain, category).stream()
                .map(target -> new Gene(category, target)).toList();
    }

    public static boolean insert(StrainData strain, GeneImprintItem.Data imprint) {
        if (strain == null || imprint == null || !imprint.identified()) return false;
        StrainData donor = StrainData.parse(imprint.strainPayload());
        if (strain.getPathogenId() == null || !strain.getPathogenId().equals(donor.getPathogenId())
                || !genes(donor, imprint.category()).contains(new Gene(imprint.category(), imprint.target()))) return false;
        switch (imprint.category()) {
            case TRANSMISSION -> {
                var id = BioForgeIds.parse(imprint.target());
                // Imprints use legacy enum names for built-in transmissions.
                var legacy = BioForgeIds.legacyTransmission(BioForgeIds.parse(imprint.target()));
                if (legacy != null) id = BioForgeIds.transmission(legacy);
                if (!BioForgeDefinitionManager.allowsTransmission(strain.getPathogenId(), id)
                        || legacy != null && !BioForgeServerConfig.isTransmissionEnabled(legacy)) return false;
                return strain.getTransmissionIds().add(id);
            }
            case SYMPTOM -> {
                String value = donor.getSymptoms().get(imprint.target());
                if (!BioForgeServerConfig.isSymptomEnabled(imprint.target()) || value == null
                        || value.equals(strain.getSymptoms().get(imprint.target()))) return false;
                strain.getSymptoms().put(imprint.target(), value);
                return true;
            }
            case MUTATION -> {
                var definition = MutationLoader.INSTANCE.getMutation(imprint.target()).orElse(null);
                if (definition == null || !definition.enabled() || !definition.isCompatible(strain.getPathogenId())
                        || !strain.getMutationIds().containsAll(definition.requiredMutations())
                        || definition.conflictingMutations().stream().anyMatch(strain.getMutationIds()::contains)) return false;
                for (String owned : strain.getMutationIds()) {
                    if (MutationLoader.INSTANCE.getMutation(owned).map(d -> d.conflictingMutations().contains(definition.id())).orElse(false)) return false;
                }
                return strain.applyMutationInVitro(definition);
            }
        }
        return false;
    }
    public static boolean remove(StrainData strain, Gene gene) {
        if (!genes(strain, gene.category()).contains(gene)) return false;
        switch (gene.category()) {
            case SYMPTOM -> { return strain.getSymptoms().remove(gene.target()) != null; }
            case TRANSMISSION -> {
                var legacy = BioForgeIds.legacyTransmission(BioForgeIds.parse(gene.target()));
                var id = legacy == null ? BioForgeIds.parse(gene.target()) : BioForgeIds.transmission(legacy);
                strain.getTransmissionIds().remove(id);
                if (legacy != null) strain.getInfectionTypes().remove(legacy);
                return true;
            }
            case MUTATION -> {
                for (String owned : strain.getMutationIds()) {
                    if (MutationLoader.INSTANCE.getMutation(owned).map(d -> d.requiredMutations().contains(gene.target())).orElse(false)) return false;
                }
                return strain.getMutationIds().remove(gene.target());
            }
        }
        return false;
    }
    public static void writeStrain(ItemStack sample, StrainData strain) {
        var tag = sample.getOrCreateTag();
        if (NbtObfuscator.readInfection(tag) != null || NbtObfuscator.read(tag) != null)
            NbtObfuscator.writeInfection(tag, strain.toPayload());
        else NbtObfuscator.writeString(tag, strain.toPayload());
        var state = read(sample); state.remove("Analyzed"); state.remove("Examined"); write(sample, state);
    }
}
