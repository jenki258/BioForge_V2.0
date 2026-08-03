package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.infection.symptoms.EntitySymptoms;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class InfectionDataImpl implements InfectionData {
    private boolean infected = false;
    @Nullable private PathogenType pathogenType = null;
    private final Set<InfectionType> infectionTypes = EnumSet.noneOf(InfectionType.class);
    private final EntitySymptoms symptoms = new EntitySymptoms();
    private final Map<String, StrainImmunity> strainImmunities = new LinkedHashMap<>();

    @Override public boolean isInfected() { return infected; }
    @Override public @Nullable PathogenType getPathogenType() { return pathogenType; }
    @Override public Set<InfectionType> getInfectionTypes() { return infectionTypes; }
    @Override public EntitySymptoms getSymptoms() { return symptoms; }
    @Override public Collection<StrainImmunity> getStrainImmunities() {
        return Collections.unmodifiableCollection(strainImmunities.values());
    }

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

    @Override
    public boolean hasStrainImmunity(String fingerprint) {
        StrainImmunity immunity = strainImmunities.get(
                StrainImmunity.normalizeFingerprint(fingerprint));
        return immunity != null && immunity.isActive();
    }

    @Override
    public void grantStrainImmunity(String fingerprint, String displayName, int durationTicks) {
        StrainImmunity incoming = new StrainImmunity(fingerprint, displayName, durationTicks);
        if (!incoming.isActive()) return;
        StrainImmunity current = strainImmunities.get(incoming.fingerprint());
        if (current != null && current.remainingTicks() > incoming.remainingTicks()) {

            strainImmunities.put(incoming.fingerprint(), new StrainImmunity(
                    current.fingerprint(), incoming.displayName(), current.remainingTicks()));
        } else {
            strainImmunities.put(incoming.fingerprint(), incoming);
        }
    }

    @Override
    public boolean tickStrainImmunities() {
        boolean expired = false;
        Iterator<Map.Entry<String, StrainImmunity>> iterator = strainImmunities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, StrainImmunity> entry = iterator.next();
            StrainImmunity next = entry.getValue().tick();
            if (!next.isActive()) {
                iterator.remove();
                expired = true;
            } else {
                entry.setValue(next);
            }
        }
        return expired;
    }

    @Override
    public void copyStrainImmunitiesFrom(InfectionData source) {
        strainImmunities.clear();
        if (source == null) return;
        for (StrainImmunity immunity : source.getStrainImmunities()) {
            grantStrainImmunity(immunity.fingerprint(), immunity.displayName(),
                    immunity.remainingTicks());
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Infected", infected);
        if (pathogenType != null) tag.putString("PathogenType", pathogenType.name());
        StringJoiner joiner = new StringJoiner(",");
        for (InfectionType t : infectionTypes) joiner.add(t.name());
        tag.putString("InfectionTypes", joiner.toString());
        tag.put("Symptoms", symptoms.serializeNBT());
        ListTag immunities = new ListTag();
        for (StrainImmunity immunity : strainImmunities.values()) {
            if (!immunity.isActive()) continue;
            CompoundTag immunityTag = new CompoundTag();
            immunityTag.putString("Fingerprint", immunity.fingerprint());
            immunityTag.putString("Name", immunity.displayName());
            immunityTag.putInt("RemainingTicks", immunity.remainingTicks());
            immunities.add(immunityTag);
        }
        tag.put("StrainImmunities", immunities);
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
        strainImmunities.clear();
        if (tag.contains("StrainImmunities", Tag.TAG_LIST)) {
            ListTag immunities = tag.getList("StrainImmunities", Tag.TAG_COMPOUND);
            for (int i = 0; i < immunities.size(); i++) {
                CompoundTag immunityTag = immunities.getCompound(i);
                grantStrainImmunity(immunityTag.getString("Fingerprint"),
                        immunityTag.getString("Name"),
                        immunityTag.getInt("RemainingTicks"));
            }
        }
    }
}
