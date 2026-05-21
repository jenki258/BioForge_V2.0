package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.infection.symptoms.EntitySymptoms;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class InfectionDataImpl implements InfectionData {

    private boolean infected = false;
    @Nullable private PathogenType pathogenType = null;
    @Nullable private InfectionType infectionType = null;
    private final EntitySymptoms symptoms = new EntitySymptoms();

    @Override public boolean isInfected() { return infected; }
    @Override public @Nullable PathogenType getPathogenType() { return pathogenType; }
    @Override public @Nullable InfectionType getInfectionType() { return infectionType; }
    @Override public EntitySymptoms getSymptoms() { return symptoms; }

    @Override
    public void setInfected(boolean infected) {
        this.infected = infected;
        if (!infected) {
            this.pathogenType = null;
            this.infectionType = null;
            symptoms.clearAll();
        }
    }

    @Override
    public void setPathogenType(@Nullable PathogenType pathogenType) {
        this.pathogenType = pathogenType;
    }

    @Override
    public void setInfectionType(@Nullable InfectionType infectionType) {
        this.infectionType = infectionType;
    }

    @Override
    public void clearInfection() {
        setInfected(false);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Infected", infected);
        if (pathogenType != null) tag.putString("PathogenType", pathogenType.name());
        if (infectionType != null) tag.putString("InfectionType", infectionType.name());
        tag.put("Symptoms", symptoms.serializeNBT());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        infected = tag.getBoolean("Infected");
        pathogenType = tag.contains("PathogenType")
                ? PathogenType.fromName(tag.getString("PathogenType")) : null;
        infectionType = tag.contains("InfectionType")
                ? InfectionType.fromName(tag.getString("InfectionType")) : null;
        if (tag.contains("Symptoms")) {
            symptoms.deserializeNBT(tag.getCompound("Symptoms"), BioForgeSymptoms.deserializer());
        }
    }
}
