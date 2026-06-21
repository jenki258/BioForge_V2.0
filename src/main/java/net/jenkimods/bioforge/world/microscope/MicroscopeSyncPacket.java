package net.jenkimods.bioforge.world.microscope;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class MicroscopeSyncPacket {

    private final Map<String, Object> symptoms;
    private final String visibility;

    public MicroscopeSyncPacket(Map<String, Object> symptoms, String visibility) {
        this.symptoms = symptoms;
        this.visibility = visibility;
    }

    public Map<String, Object> getSymptoms() { return symptoms; }
    public String getVisibility() { return visibility; }

    public static void encode(MicroscopeSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.symptoms.size());
        for (Map.Entry<String, Object> entry : msg.symptoms.entrySet()) {
            buf.writeUtf(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Float f) {
                buf.writeByte(0); buf.writeFloat(f);
            } else if (value instanceof Boolean b) {
                buf.writeByte(1); buf.writeBoolean(b);
            } else {
                buf.writeByte(2); buf.writeUtf(value.toString());
            }
        }
        buf.writeUtf(msg.visibility);
    }

    public static MicroscopeSyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String key = buf.readUtf();
            byte type = buf.readByte();
            if (type == 0) map.put(key, buf.readFloat());
            else if (type == 1) map.put(key, buf.readBoolean());
            else if (type == 2) map.put(key, buf.readUtf());
        }
        String vis = buf.readUtf();
        return new MicroscopeSyncPacket(map, vis);
    }

    public static void handle(MicroscopeSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> MicroscopeClientData.set(msg.symptoms, msg.visibility));
        ctx.get().setPacketHandled(true);
    }
}