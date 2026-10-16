package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.nbt.CompoundTag;

/** Blood metadata and its separate infection channel must never be confused. */
public final class StrainPayload {
    private StrainPayload() {}
    public static StrainData read(CompoundTag tag) {
        String infection = NbtObfuscator.readInfection(tag);
        String payload = infection != null ? infection
                : NbtObfuscator.read(tag) != null ? null : NbtObfuscator.readString(tag);
        if (payload == null || payload.isBlank() || "CLEAN".equals(payload)) return null;
        try {
            StrainData strain = StrainData.parse(payload);
            return strain.getPathogenId() == null ? null : strain;
        } catch (IllegalArgumentException invalid) { return null; }
    }
}
