package net.jenkimods.bioforge.infection.network;

import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class InfectionSyncPacket {

    private final boolean infected;
    private final String pathogenType;
    private final String infectionType;
    private final String heartRate;
    private final String lungSound;
    private final boolean temperaturePlus;
    private final boolean temperatureMinus;
    private final float redness;
    private final float lesions;
    private final float secretion;
    private final float swelling;
    private final float reflexDelay;
    private final float reflexStrength;
    private final float neuralDamage;
    private final float oxygenSaturation;
    private final float perfusionIndex;
    private final float infectionStrength;

    public InfectionSyncPacket(boolean infected, String pathogenType, String infectionType,
                               String heartRate, String lungSound,
                               boolean temperaturePlus, boolean temperatureMinus,
                               float redness, float lesions, float secretion, float swelling,
                               float reflexDelay, float reflexStrength, float neuralDamage,
                               float oxygenSaturation, float perfusionIndex,
                               float infectionStrength) {
        this.infected = infected;
        this.pathogenType = pathogenType;
        this.infectionType = infectionType;
        this.heartRate = heartRate;
        this.lungSound = lungSound;
        this.temperaturePlus = temperaturePlus;
        this.temperatureMinus = temperatureMinus;
        this.redness = redness;
        this.lesions = lesions;
        this.secretion = secretion;
        this.swelling = swelling;
        this.reflexDelay = reflexDelay;
        this.reflexStrength = reflexStrength;
        this.neuralDamage = neuralDamage;
        this.oxygenSaturation = oxygenSaturation;
        this.perfusionIndex = perfusionIndex;
        this.infectionStrength = infectionStrength;
    }

    public static InfectionSyncPacket fromData(InfectionData data) {
        return new InfectionSyncPacket(
                data.isInfected(),
                data.getPathogenType() != null ? data.getPathogenType().name() : "",
                data.getInfectionType() != null ? data.getInfectionType().name() : "",
                data.getSymptom(BioForgeSymptoms.HEART_RATE).name(),
                data.getSymptom(BioForgeSymptoms.LUNG_SOUND).name(),
                data.getSymptom(BioForgeSymptoms.TEMPERATURE_PLUS),
                data.getSymptom(BioForgeSymptoms.TEMPERATURE_MINUS),
                data.getSymptom(BioForgeSymptoms.OTOSCOPE_REDNESS),
                data.getSymptom(BioForgeSymptoms.OTOSCOPE_LESIONS),
                data.getSymptom(BioForgeSymptoms.OTOSCOPE_SECRETION),
                data.getSymptom(BioForgeSymptoms.OTOSCOPE_SWELLING),
                data.getSymptom(BioForgeSymptoms.REFLEX_DELAY),
                data.getSymptom(BioForgeSymptoms.REFLEX_STRENGTH),
                data.getSymptom(BioForgeSymptoms.NEURAL_DAMAGE),
                data.getSymptom(BioForgeSymptoms.OXYGEN_SATURATION),
                data.getSymptom(BioForgeSymptoms.PERFUSION_INDEX),
                data.getSymptom(BioForgeSymptoms.INFECTION_STRENGTH)
        );
    }

    public static void encode(InfectionSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.infected);
        buf.writeUtf(pkt.pathogenType);
        buf.writeUtf(pkt.infectionType);
        buf.writeUtf(pkt.heartRate);
        buf.writeUtf(pkt.lungSound);
        buf.writeBoolean(pkt.temperaturePlus);
        buf.writeBoolean(pkt.temperatureMinus);
        buf.writeFloat(pkt.redness);
        buf.writeFloat(pkt.lesions);
        buf.writeFloat(pkt.secretion);
        buf.writeFloat(pkt.swelling);
        buf.writeFloat(pkt.reflexDelay);
        buf.writeFloat(pkt.reflexStrength);
        buf.writeFloat(pkt.neuralDamage);
        buf.writeFloat(pkt.oxygenSaturation);
        buf.writeFloat(pkt.perfusionIndex);
        buf.writeFloat(pkt.infectionStrength);
    }

    public static InfectionSyncPacket decode(FriendlyByteBuf buf) {
        boolean infected = buf.readBoolean();
        String pathogenType = buf.readUtf();
        String infectionType = buf.readUtf();
        String heartRate = buf.readUtf();
        String lungSound = buf.readUtf();
        boolean temperaturePlus = buf.readBoolean();
        boolean temperatureMinus = buf.readBoolean();
        float redness = buf.readFloat();
        float lesions = buf.readFloat();
        float secretion = buf.readFloat();
        float swelling = buf.readFloat();
        float reflexDelay = buf.readFloat();
        float reflexStrength = buf.readFloat();
        float neuralDamage = buf.readFloat();
        float oxygenSaturation = buf.readFloat();
        float perfusionIndex = buf.readFloat();
        float infectionStrength = buf.readFloat();
        return new InfectionSyncPacket(infected, pathogenType, infectionType,
                heartRate, lungSound, temperaturePlus, temperatureMinus,
                redness, lesions, secretion, swelling,
                reflexDelay, reflexStrength, neuralDamage,
                oxygenSaturation, perfusionIndex,
                infectionStrength);
    }

    public static void handle(InfectionSyncPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            PathogenType pt = pkt.pathogenType.isEmpty() ? null : PathogenType.fromName(pkt.pathogenType);
            InfectionType it = pkt.infectionType.isEmpty() ? null : InfectionType.fromName(pkt.infectionType);
            HeartRate hr = HeartRate.fromName(pkt.heartRate);
            LungSound ls = LungSound.fromName(pkt.lungSound);
            InfectionClientCache.set(pkt.infected, pt, it, hr, ls,
                    pkt.temperaturePlus, pkt.temperatureMinus,
                    pkt.redness, pkt.lesions, pkt.secretion, pkt.swelling,
                    pkt.reflexDelay, pkt.reflexStrength, pkt.neuralDamage,
                    pkt.oxygenSaturation, pkt.perfusionIndex,
                    pkt.infectionStrength);
        });
        ctx.setPacketHandled(true);
    }
}