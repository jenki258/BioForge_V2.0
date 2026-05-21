package net.jenkimods.bioforge.infection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class InfectionClientCache {
    private InfectionClientCache() {}

    private static final AtomicBoolean infected = new AtomicBoolean(false);
    private static final AtomicReference<PathogenType> pathogenType = new AtomicReference<>(null);
    private static final AtomicReference<InfectionType> infectionType = new AtomicReference<>(null);
    private static final AtomicReference<HeartRate> heartRate = new AtomicReference<>(HeartRate.NORMAL);
    private static final AtomicReference<LungSound> lungSound = new AtomicReference<>(LungSound.NORMAL);
    private static final AtomicBoolean temperaturePlus = new AtomicBoolean(false);
    private static final AtomicBoolean temperatureMinus = new AtomicBoolean(false);
    private static final AtomicReference<Float> redness = new AtomicReference<>(0.0f);
    private static final AtomicReference<Float> lesions = new AtomicReference<>(0.0f);
    private static final AtomicReference<Float> secretion = new AtomicReference<>(0.0f);
    private static final AtomicReference<Float> swelling = new AtomicReference<>(0.0f);
    private static final AtomicReference<Float> reflexDelay = new AtomicReference<>(0.0f);
    private static final AtomicReference<Float> reflexStrength = new AtomicReference<>(0.5f);
    private static final AtomicReference<Float> neuralDamage = new AtomicReference<>(0.0f);
    private static final AtomicReference<Float> oxygenSaturation = new AtomicReference<>(0.95f);
    private static final AtomicReference<Float> perfusionIndex = new AtomicReference<>(0.7f);
    private static final AtomicReference<Float> infectionStrength = new AtomicReference<>(0.5f);
    private static final AtomicReference<Float> colonyRadius = new AtomicReference<>(20.0f);
    private static final AtomicReference<Float> maxInfestedBlocks = new AtomicReference<>(100.0f);

    public static void set(boolean isInfected, PathogenType pathogen, InfectionType infection,
                           HeartRate hr, LungSound ls, boolean tempPlus, boolean tempMinus,
                           float r, float l, float s, float w, float reflexDelay, float reflexStrength,
                           float neuralDamage, float o2, float perf, float infStrength,
                           float colonyRadius, float maxInfestedBlocks) {
        infected.set(isInfected);
        pathogenType.set(pathogen);
        infectionType.set(infection);
        heartRate.set(hr);
        lungSound.set(ls);
        temperaturePlus.set(tempPlus);
        temperatureMinus.set(tempMinus);
        redness.set(r);
        lesions.set(l);
        secretion.set(s);
        swelling.set(w);
        InfectionClientCache.reflexDelay.set(reflexDelay);
        InfectionClientCache.reflexStrength.set(reflexStrength);
        InfectionClientCache.neuralDamage.set(neuralDamage);
        oxygenSaturation.set(o2);
        perfusionIndex.set(perf);
        infectionStrength.set(infStrength);
        InfectionClientCache.colonyRadius.set(colonyRadius);
        InfectionClientCache.maxInfestedBlocks.set(maxInfestedBlocks);
    }

    public static boolean isInfected() { return infected.get(); }
    public static PathogenType getPathogenType() { return pathogenType.get(); }
    public static InfectionType getInfectionType() { return infectionType.get(); }
    public static HeartRate getHeartRate() { return heartRate.get(); }
    public static LungSound getLungSound() { return lungSound.get(); }
    public static boolean isTemperaturePlus() { return temperaturePlus.get(); }
    public static boolean isTemperatureMinus() { return temperatureMinus.get(); }
    public static float getRedness() { return redness.get(); }
    public static float getLesions() { return lesions.get(); }
    public static float getSecretion() { return secretion.get(); }
    public static float getSwelling() { return swelling.get(); }
    public static float getReflexDelay() { return reflexDelay.get(); }
    public static float getReflexStrength() { return reflexStrength.get(); }
    public static float getNeuralDamage() { return neuralDamage.get(); }
    public static float getOxygenSaturation() { return oxygenSaturation.get(); }
    public static float getPerfusionIndex() { return perfusionIndex.get(); }
    public static float getInfectionStrength() { return infectionStrength.get(); }
    public static float getColonyRadius() { return colonyRadius.get(); }
    public static float getMaxInfestedBlocks() { return maxInfestedBlocks.get(); }
}