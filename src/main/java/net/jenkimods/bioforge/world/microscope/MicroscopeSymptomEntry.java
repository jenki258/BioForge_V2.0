package net.jenkimods.bioforge.world.microscope;

import net.jenkimods.bioforge.infection.MicroscopeVisibility;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record MicroscopeSymptomEntry(
        String symptomKey,
        ResourceLocation icon,
        @Nullable Map<String, ResourceLocation> stateIcons,
        boolean isBoolean,
        boolean isEnum,
        MicroscopeVisibility minVisibility
) {
    public MicroscopeSymptomEntry(String symptomKey, ResourceLocation icon, boolean isBoolean,
                                  MicroscopeVisibility minVisibility) {
        this(symptomKey, icon, null, isBoolean, false, minVisibility);
    }

    public MicroscopeSymptomEntry(String symptomKey, ResourceLocation icon,
                                  Map<String, ResourceLocation> stateIcons,
                                  MicroscopeVisibility minVisibility) {
        this(symptomKey, icon, stateIcons, false, true, minVisibility);
    }
}