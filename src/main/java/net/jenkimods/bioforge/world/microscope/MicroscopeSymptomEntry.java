package net.jenkimods.bioforge.world.microscope;

import net.jenkimods.bioforge.infection.MicroscopeVisibility;
import net.minecraft.network.FriendlyByteBuf;
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

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(symptomKey);
        buf.writeUtf(icon.toString());
        buf.writeBoolean(isBoolean);
        buf.writeBoolean(isEnum);
        buf.writeEnum(minVisibility);
        if (stateIcons != null) {
            buf.writeBoolean(true);
            buf.writeMap(stateIcons, FriendlyByteBuf::writeUtf, (b, rl) -> b.writeUtf(rl.toString()));
        } else {
            buf.writeBoolean(false);
        }
    }

    public static MicroscopeSymptomEntry decode(FriendlyByteBuf buf) {
        String key = buf.readUtf();
        ResourceLocation icon = ResourceLocation.tryParse(buf.readUtf());
        boolean isBool = buf.readBoolean();
        boolean isEnum = buf.readBoolean();
        MicroscopeVisibility vis = buf.readEnum(MicroscopeVisibility.class);
        Map<String, ResourceLocation> states = null;
        if (buf.readBoolean()) {
            states = buf.readMap(FriendlyByteBuf::readUtf, b -> ResourceLocation.tryParse(b.readUtf()));
        }
        return new MicroscopeSymptomEntry(key, icon, states, isBool, isEnum, vis);
    }
}