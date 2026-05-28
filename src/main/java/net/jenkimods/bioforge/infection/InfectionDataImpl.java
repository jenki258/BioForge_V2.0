package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.infection.symptoms.EntitySymptoms;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class InfectionDataImpl implements InfectionData {
    private boolean infected = false;
    @Nullable private PathogenType pathogenType = null;
    private final Set<InfectionType> infectionTypes = EnumSet.noneOf(InfectionType.class);
    private final EntitySymptoms symptoms = new EntitySymptoms();

    @Override public boolean isInfected() { return infected; }
    @Override public @Nullable PathogenType getPathogenType() { return pathogenType; }
    @Override public Set<InfectionType> getInfectionTypes() { return infectionTypes; }
    @Override public EntitySymptoms getSymptoms() { return symptoms; }

    @Override
    public void setInfected(boolean infected) {
        this.infected = infected;
        if (!infected) {
            pathogenType = null;
            infectionTypes.clear();
            symptoms.clearAll();
        }
    }

    @Override public void setPathogenType(@Nullable PathogenType pathogenType) { this.pathogenType = pathogenType; }
    @Override public void addInfectionType(InfectionType type) { infectionTypes.add(type); }
    @Override public void removeInfectionType(InfectionType type) { infectionTypes.remove(type); }

    @Override public void clearInfection() { setInfected(false); }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Infected", infected);
        if (pathogenType != null) tag.putString("PathogenType", pathogenType.name());
        StringJoiner joiner = new StringJoiner(",");
        for (InfectionType t : infectionTypes) joiner.add(t.name());
        tag.putString("InfectionTypes", joiner.toString());
        tag.put("Symptoms", symptoms.serializeNBT());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        infected = tag.getBoolean("Infected");
        pathogenType = tag.contains("PathogenType") ? PathogenType.fromName(tag.getString("PathogenType")) : null;
        infectionTypes.clear();
        if (tag.contains("InfectionTypes")) {
            String raw = tag.getString("InfectionTypes");
            if (!raw.isEmpty()) {
                for (String part : raw.split(",")) {
                    InfectionType it = InfectionType.fromName(part);
                    if (it != null) infectionTypes.add(it);
                }
            }
        }
        if (tag.contains("Symptoms")) {
            symptoms.deserializeNBT(tag.getCompound("Symptoms"), BioForgeSymptoms.deserializer());
        }
    }
}