package net.jenkimods.bioforge.crispr;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.jenkimods.bioforge.infection.PathogenType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

import java.util.LinkedHashSet;
import java.util.Set;

public record CrisprCasModuleDefinition(
        ResourceLocation id,
        String displayName,
        String pam,
        float efficiency,
        Set<ResourceLocation> compatibleGuideProfiles,
        Set<PathogenType> compatiblePathogens
) {
    public CrisprCasModuleDefinition {
        if (displayName == null || displayName.isBlank()) {
            displayName = CrisprDisplayNames.humanize(id.getPath());
        }
        if (pam == null || pam.isBlank()) {
            throw new IllegalArgumentException("Cas module PAM cannot be empty");
        }
        efficiency = Mth.clamp(efficiency, 0.0f, 1.0f);
        compatibleGuideProfiles = Set.copyOf(compatibleGuideProfiles);
        compatiblePathogens = Set.copyOf(compatiblePathogens);
    }

    public static CrisprCasModuleDefinition fromJson(ResourceLocation id, JsonObject json) {
        Set<ResourceLocation> profiles = new LinkedHashSet<>();
        if (json.has("compatible_guide_profiles")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(
                    json, "compatible_guide_profiles")) {
                ResourceLocation profile = ResourceLocation.tryParse(element.getAsString());
                if (profile == null) {
                    throw new IllegalArgumentException(
                            "Invalid compatible guide profile: " + element.getAsString());
                }
                profiles.add(profile);
            }
        }
        Set<PathogenType> pathogens = new LinkedHashSet<>();
        if (json.has("compatible_pathogens")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(
                    json, "compatible_pathogens")) {
                try {
                    pathogens.add(PathogenType.valueOf(
                            element.getAsString().trim().toUpperCase(java.util.Locale.ROOT)));
                } catch (IllegalArgumentException invalid) {
                    throw new IllegalArgumentException(
                            "Invalid compatible pathogen: " + element.getAsString());
                }
            }
        }
        return new CrisprCasModuleDefinition(
                id,
                GsonHelper.getAsString(json, "display_name",
                        CrisprDisplayNames.humanize(id.getPath())),
                GsonHelper.getAsString(json, "pam", "NGG"),
                GsonHelper.getAsFloat(json, "efficiency", 1.0f),
                profiles,
                pathogens
        );
    }

    public boolean isCompatible(ResourceLocation profile) {
        return compatibleGuideProfiles.isEmpty()
                || compatibleGuideProfiles.contains(profile);
    }

    public boolean isCompatible(ResourceLocation profile, PathogenType pathogen) {
        return isCompatible(profile) && (pathogen == PathogenType.UNIVERSAL
                || compatiblePathogens.isEmpty()
                || compatiblePathogens.contains(pathogen));
    }
}
