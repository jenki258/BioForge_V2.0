package net.jenkimods.bioforge.config;

import net.jenkimods.bioforge.vaccine.VaccineRules;
import net.minecraftforge.common.ForgeConfigSpec;





public final class BioForgeServerConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.DoubleValue MINIMUM_SIMILARITY;
    private static final ForgeConfigSpec.DoubleValue SIMILARITY_CURVE_FLOOR;
    private static final ForgeConfigSpec.DoubleValue BASE_POTENCY;
    private static final ForgeConfigSpec.DoubleValue MAXIMUM_CURE_CHANCE;
    private static final ForgeConfigSpec.DoubleValue STRENGTH_RESISTANCE;
    private static final ForgeConfigSpec.DoubleValue DEFENSE_CURE_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue DEFENSE_RISK_STRENGTH_SCALE;
    private static final ForgeConfigSpec.DoubleValue DEFENSE_RISK_MISMATCH_SCALE;
    private static final ForgeConfigSpec.DoubleValue EXACT_BLOOD_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue SAME_RH_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue RH_MISMATCH_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue UNKNOWN_HOST_MULTIPLIER;
    private static final ForgeConfigSpec.ConfigValue<String> DEFENSE_MUTATION;
    private static final ForgeConfigSpec.IntValue STRAIN_IMMUNITY_DURATION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment(
                "BioForge per-world server balance.",
                "This file is created in the world's serverconfig directory.")
                .push("vaccines");

        MINIMUM_SIMILARITY = probability(builder,
                "minimumSimilarity", 0.45,
                "Minimum strain similarity required for a full vaccine to have any cure chance.");
        SIMILARITY_CURVE_FLOOR = probability(builder,
                "similarityCurveFloor", 0.40,
                "Similarity value treated as the bottom of the quadratic cure curve.");
        BASE_POTENCY = nonNegative(builder,
                "basePotency", 0.95,
                "Base full-vaccine potency before quality, similarity, strength and blood modifiers.");
        MAXIMUM_CURE_CHANCE = probability(builder,
                "maximumCureChance", 0.85,
                "Hard cap for full-vaccine cure chance after every modifier.");
        STRENGTH_RESISTANCE = nonNegative(builder,
                "strengthResistance", 2.25,
                "How strongly infection_strength reduces full and directed vaccine success.");
        DEFENSE_CURE_MULTIPLIER = probability(builder,
                "defenseMutationCureMultiplier", 0.35,
                "Cure-chance multiplier while a vaccine-defense/immune-escape mutation is active.");
        DEFENSE_RISK_STRENGTH_SCALE = nonNegative(builder,
                "defenseRiskStrengthScale", 0.75,
                "How infection strength increases the chance of selecting vaccine defense.");
        DEFENSE_RISK_MISMATCH_SCALE = nonNegative(builder,
                "defenseRiskMismatchScale", 0.50,
                "How strain mismatch increases the chance of selecting vaccine defense.");
        STRAIN_IMMUNITY_DURATION = builder.comment(
                        "Duration in ticks of exact-strain immunity granted by a successful full vaccine.",
                        "20 ticks = 1 second; the default is 10 minutes of loaded game time.")
                .defineInRange("strainImmunityDurationTicks", 12_000, 20, 2_147_483_647);

        builder.pop().push("bloodCompatibility");
        EXACT_BLOOD_MULTIPLIER = nonNegative(builder,
                "exactBloodTypeMultiplier", 1.10,
                "Multiplier when the vaccine's researched ABO/Rh profile exactly matches the host.");
        SAME_RH_MULTIPLIER = nonNegative(builder,
                "sameRhMultiplier", 0.95,
                "Multiplier for a different ABO group with the same Rh sign.");
        RH_MISMATCH_MULTIPLIER = nonNegative(builder,
                "rhMismatchMultiplier", 0.65,
                "Multiplier for an Rh/category mismatch.");
        UNKNOWN_HOST_MULTIPLIER = nonNegative(builder,
                "unknownHostMultiplier", 0.85,
                "Multiplier when either the host or vaccine has no verified blood profile.");

        builder.pop().push("mutations");
        DEFENSE_MUTATION = builder
                .comment("Mutation ID selected after a failed vaccine when possible.")
                .define("defenseMutation", VaccineRules.DEFAULT_DEFENSE_MUTATION_ID,
                        value -> value instanceof String string && !string.isBlank());
        builder.pop();
        SPEC = builder.build();
    }

    private BioForgeServerConfig() {}

    public static VaccineRules vaccineRules() {
        return new VaccineRules(
                MINIMUM_SIMILARITY.get().floatValue(),
                SIMILARITY_CURVE_FLOOR.get().floatValue(),
                BASE_POTENCY.get().floatValue(),
                MAXIMUM_CURE_CHANCE.get().floatValue(),
                STRENGTH_RESISTANCE.get().floatValue(),
                DEFENSE_CURE_MULTIPLIER.get().floatValue(),
                DEFENSE_RISK_STRENGTH_SCALE.get().floatValue(),
                DEFENSE_RISK_MISMATCH_SCALE.get().floatValue(),
                EXACT_BLOOD_MULTIPLIER.get().floatValue(),
                SAME_RH_MULTIPLIER.get().floatValue(),
                RH_MISMATCH_MULTIPLIER.get().floatValue(),
                UNKNOWN_HOST_MULTIPLIER.get().floatValue(),
                DEFENSE_MUTATION.get());
    }

    public static int strainImmunityDurationTicks() {
        return STRAIN_IMMUNITY_DURATION.get();
    }

    private static ForgeConfigSpec.DoubleValue probability(
            ForgeConfigSpec.Builder builder, String name, double fallback, String comment) {
        return builder.comment(comment).defineInRange(name, fallback, 0.0, 1.0);
    }

    private static ForgeConfigSpec.DoubleValue nonNegative(
            ForgeConfigSpec.Builder builder, String name, double fallback, String comment) {
        return builder.comment(comment).defineInRange(name, fallback, 0.0, 100.0);
    }
}
