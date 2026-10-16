package net.jenkimods.bioforge.vaccine;

import net.jenkimods.bioforge.config.BioForgeServerConfig;
import net.jenkimods.bioforge.infection.InfectionCapability;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.infection.symptoms.SymptomSuppression;
import net.jenkimods.bioforge.item.vaccine.SymptomTabletItem;
import net.jenkimods.bioforge.mutation.MutationLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Shared by oral medicine and the dispenser so both use the same resistance rules. */
public final class SymptomTreatment {
    private SymptomTreatment() {}

    public static SymptomTabletProfile profile(ItemStack stack) {
        return stack.getItem() instanceof SymptomTabletItem tablet ? tablet.profile(stack) : null;
    }

    public static boolean needed(LivingEntity entity, SymptomTabletProfile profile) {
        if (profile == null || !BioForgeServerConfig.isSymptomEnabled(profile.symptomId())
                || SymptomSuppression.isSuppressed(entity, profile.symptomId())) return false;
        var data = InfectionCapability.get(entity);
        if (data == null || !data.isInfectionActive()) return false;
        var key = BioForgeSymptoms.getAllSymptomKeys().get(profile.symptomId());
        if (key != null && !data.getSymptom(key).equals(key.getDefaultValue())) return true;
        return data.getSymptoms().getMutations().stream()
                .filter(BioForgeServerConfig::isMutationEnabled)
                .anyMatch(id -> MutationLoader.INSTANCE.getMutation(id)
                        .map(d -> d.tags().contains("suppressible:" + profile.symptomId())).orElse(false));
    }

    public static int apply(LivingEntity entity, SymptomTabletProfile profile) {
        if (entity.level().isClientSide || profile == null
                || !BioForgeServerConfig.isSymptomEnabled(profile.symptomId())) return 0;
        var data = InfectionCapability.get(entity);
        if (data == null) return 0;
        float strength = data.isInfected() ? Math.max(0, data.getSymptom(BioForgeSymptoms.INFECTION_STRENGTH)) : 0;
        float resistance = data.isInfected() && data.getSymptoms().hasMutation("analgesic_resistance") ? .35F : 1;
        int duration = Math.max(20, Math.round(profile.durationTicks()
                * (.55F + profile.quality() * .45F) * resistance / (1 + strength * .65F)));
        SymptomSuppression.suppress(entity, profile.symptomId(), duration);
        return duration;
    }
}
