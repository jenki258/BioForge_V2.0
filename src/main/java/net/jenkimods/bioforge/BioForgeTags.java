package net.jenkimods.bioforge;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class BioForgeTags {

    public static final TagKey<EntityType<?>> NO_INFECTION_ATTACK_SPREAD =
            TagKey.create(Registries.ENTITY_TYPE,
                    ResourceLocation.tryBuild("bioforge", "no_infection_attack_spread"));

    public static final TagKey<EntityType<?>> NO_DIAGNOSTICS =
            TagKey.create(Registries.ENTITY_TYPE,
                    ResourceLocation.tryBuild("bioforge", "no_diagnostics"));
}