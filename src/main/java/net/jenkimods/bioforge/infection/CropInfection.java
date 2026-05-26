package net.jenkimods.bioforge.infection;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class CropInfection {
    private String strainData;
    private PathogenType pathogen;
    private InfectionType infectionType;
    private UUID colonyId;
    private float infectionStrength;

    public CropInfection(String strainData) {
        this.strainData = strainData;
        parseStrainData();
    }

    private void parseStrainData() {
        if (strainData == null || strainData.equals("CLEAN")) return;
        String[] parts = strainData.split(";");
        if (parts.length > 0) {
            String[] header = parts[0].split("\\|");
            if (header.length >= 3) {
                try { colonyId = UUID.fromString(header[0]); } catch (Exception ignored) {}
                pathogen = PathogenType.fromName(header[1]);
                infectionType = InfectionType.fromName(header[2]);
            }
        }
        for (String p : parts) {
            if (p.startsWith("InfectionStrength=")) {
                try { infectionStrength = Float.parseFloat(p.substring(18)); } catch (Exception ignored) {}
            }
        }
    }

    public String getStrainData() { return strainData; }
    public PathogenType getPathogen() { return pathogen; }
    public InfectionType getInfectionType() { return infectionType; }
    public UUID getColonyId() { return colonyId; }
    public float getInfectionStrength() { return infectionStrength; }

    public CompoundTag writeToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("strainData", strainData);
        return tag;
    }

    public static CropInfection readFromNBT(CompoundTag tag) {
        return new CropInfection(tag.getString("strainData"));
    }
}