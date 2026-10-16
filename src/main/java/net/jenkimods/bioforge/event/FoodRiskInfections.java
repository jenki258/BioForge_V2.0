package net.jenkimods.bioforge.event;

import net.jenkimods.bioforge.config.BioForgeServerConfig;
import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.infection.spread.ItemStrainData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** Natural food risks are separate from deliberately contaminated food. */
public final class FoodRiskInfections {
    public static final TagKey<Item> PARASITE_FOODS = tag("foods/parasite_risk");
    public static final TagKey<Item> PRION_FOODS = tag("foods/prion_risk");
    private FoodRiskInfections() {}
    private static TagKey<Item> tag(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.tryBuild("bioforge", id));
    }
    public static boolean tryInfect(LivingEntity entity, ItemStack food, float roll) {
        if (entity.level().isClientSide() || !food.isEdible() || ItemStrainData.read(food) != null
                || !BioForgeServerConfig.naturalInfectionsEnabled()
                || !BioForgeServerConfig.isTransmissionEnabled(InfectionType.FOOD_BORNE)
                || !InfectionCapability.canHostInfection(entity)
                || entity instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        InfectionData data = InfectionCapability.get(entity);
        if (data == null || data.isInfected()) return false;
        // Explicit prion tags take priority if a datapack assigns both risks to a food.
        boolean prion = food.is(PRION_FOODS);
        if (!prion && !food.is(PARASITE_FOODS)) return false;
        float chance = prion ? BioForgeServerConfig.rottenFoodPrionChance() : BioForgeServerConfig.rawFoodParasiteChance();
        if (!(roll >= 0 && roll < chance) || !prion && !BioForgeServerConfig.isMutationEnabled("hunger")) return false;
        StrainData strain = StrainData.createEmpty();
        strain.setPathogen(prion ? PathogenType.PRION : PathogenType.PARASITE);
        strain.getInfectionTypes().add(InfectionType.FOOD_BORNE);
        strain.getSymptoms().put("infection_strength", prion ? "0.35" : "0.15");
        if (prion) {
            strain.setLifecycleProfileId(ResourceLocation.tryBuild("bioforge", "food_prion"));
            strain.getSymptoms().put("neural_damage", "0.45");
            strain.getSymptoms().put("reflex_delay", "0.35");
            strain.setTimingOverrides(600, 6000);
        } else {
            strain.setLifecycleProfileId(ResourceLocation.tryBuild("bioforge", "food_parasite"));
            strain.setTimingOverrides(0, 1200);
            strain.addMutation("hunger");
        }
        return strain.applyToEntity(data, entity);
    }
}
