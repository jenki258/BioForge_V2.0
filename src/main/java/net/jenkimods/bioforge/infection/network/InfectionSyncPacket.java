package net.jenkimods.bioforge.infection.network;

import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.infection.symptoms.SymptomKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class InfectionSyncPacket {
    private final boolean infected;
    private final String pathogenType;
    private final List<String> infectionTypes;
    private final Map<String, Object> symptoms;

    public InfectionSyncPacket(boolean infected, String pathogenType, List<String> infectionTypes,
                               Map<String, Object> symptoms) {
        this.infected = infected;
        this.pathogenType = pathogenType;
        this.infectionTypes = infectionTypes;
        this.symptoms = symptoms;
    }

    public static InfectionSyncPacket fromData(InfectionData data) {
        List<String> types = data.getInfectionTypes().stream().map(InfectionType::name).toList();

        Map<String, Object> symptomMap = new LinkedHashMap<>();
        for (Map.Entry<String, SymptomKey<?>> entry : BioForgeSymptoms.getAllSymptomKeys().entrySet()) {
            SymptomKey<?> key = entry.getValue();
            Object value = data.getSymptom(key);
            if (value != null) {
                symptomMap.put(entry.getKey(), value);
            }
        }

        return new InfectionSyncPacket(
                data.isInfected(),
                data.getPathogenType() != null ? data.getPathogenType().name() : "",
                types,
                symptomMap
        );
    }

    public static void encode(InfectionSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.infected);
        buf.writeUtf(pkt.pathogenType);
        buf.writeCollection(pkt.infectionTypes, FriendlyByteBuf::writeUtf);

        Set<Map.Entry<String, Object>> entries = pkt.symptoms.entrySet();
        buf.writeInt(entries.size());
        for (Map.Entry<String, Object> entry : entries) {
            buf.writeUtf(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Enum<?> e) {
                buf.writeByte(0);
                buf.writeUtf(e.name());
            } else if (value instanceof Boolean b) {
                buf.writeByte(1);
                buf.writeBoolean(b);
            } else if (value instanceof Float f) {
                buf.writeByte(2);
                buf.writeFloat(f);
            } else {
                buf.writeByte(3);
            }
        }
    }

    public static InfectionSyncPacket decode(FriendlyByteBuf buf) {
        boolean infected = buf.readBoolean();
        String pathogenType = buf.readUtf();
        List<String> types = buf.readList(FriendlyByteBuf::readUtf);

        int symptomCount = buf.readInt();
        Map<String, Object> symptoms = new LinkedHashMap<>();
        for (int i = 0; i < symptomCount; i++) {
            String keyId = buf.readUtf();
            byte type = buf.readByte();
            switch (type) {
                case 0 -> symptoms.put(keyId, buf.readUtf());
                case 1 -> symptoms.put(keyId, buf.readBoolean());
                case 2 -> symptoms.put(keyId, buf.readFloat());
            }
        }

        return new InfectionSyncPacket(infected, pathogenType, types, symptoms);
    }

    public static void handle(InfectionSyncPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            PathogenType pt = pkt.pathogenType.isEmpty() ? null : PathogenType.fromName(pkt.pathogenType);
            List<InfectionType> types = new ArrayList<>();
            for (String s : pkt.infectionTypes) {
                types.add(InfectionType.fromName(s));
            }

            Map<String, Object> symptoms = pkt.symptoms;
            InfectionClientCache.set(pkt.infected, pt, types, symptoms);
        });
        ctx.setPacketHandled(true);
    }
}