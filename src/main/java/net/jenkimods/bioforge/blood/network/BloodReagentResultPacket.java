package net.jenkimods.bioforge.blood.network;

import net.jenkimods.bioforge.item.clipboard.ClipboardClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class BloodReagentResultPacket {

    private final String sourceName;
    private final String bloodType;
    private final Boolean antiA, antiB, antiD;
    private final UUID subjectUUID;

    public BloodReagentResultPacket(String sourceName, String bloodType,
                                    Boolean antiA, Boolean antiB, Boolean antiD,
                                    UUID subjectUUID) {
        this.sourceName = sourceName;
        this.bloodType = bloodType;
        this.antiA = antiA;
        this.antiB = antiB;
        this.antiD = antiD;
        this.subjectUUID = subjectUUID;
    }

    public static void encode(BloodReagentResultPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.sourceName);
        buf.writeUtf(msg.bloodType);
        buf.writeBoolean(msg.antiA != null);
        if (msg.antiA != null) buf.writeBoolean(msg.antiA);
        buf.writeBoolean(msg.antiB != null);
        if (msg.antiB != null) buf.writeBoolean(msg.antiB);
        buf.writeBoolean(msg.antiD != null);
        if (msg.antiD != null) buf.writeBoolean(msg.antiD);


        buf.writeBoolean(msg.subjectUUID != null);
        if (msg.subjectUUID != null) {
            buf.writeUUID(msg.subjectUUID);
        }
    }

    public static BloodReagentResultPacket decode(FriendlyByteBuf buf) {
        String sourceName = buf.readUtf();
        String bloodType = buf.readUtf();
        Boolean antiA = buf.readBoolean() ? buf.readBoolean() : null;
        Boolean antiB = buf.readBoolean() ? buf.readBoolean() : null;
        Boolean antiD = buf.readBoolean() ? buf.readBoolean() : null;

        UUID uuid = null;
        if (buf.readBoolean()) {
            uuid = buf.readUUID();
        }
        return new BloodReagentResultPacket(sourceName, bloodType, antiA, antiB, antiD, uuid);
    }

    public static void handle(BloodReagentResultPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ClipboardClientHandler.hasPatient()) {

                boolean match = false;
                if (msg.subjectUUID != null && ClipboardClientHandler.getSubjectUUID() != null) {
                    match = msg.subjectUUID.equals(ClipboardClientHandler.getSubjectUUID());
                } else {
                    match = ClipboardClientHandler.getPatientName().equalsIgnoreCase(msg.sourceName);
                }
                if (match) {
                    ClipboardClientHandler.recordBloodData(msg.bloodType, msg.antiA, msg.antiB, msg.antiD);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}