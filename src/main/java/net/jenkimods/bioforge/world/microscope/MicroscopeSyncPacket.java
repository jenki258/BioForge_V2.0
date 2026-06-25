package net.jenkimods.bioforge.world.microscope;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class MicroscopeSyncPacket {

    private final Map<String, Object> symptoms;
    private final String visibility;
    private final List<MicroscopeSymptomEntry> entries;

    public MicroscopeSyncPacket(Map<String, Object> symptoms, String visibility, List<MicroscopeSymptomEntry> entries) {
        this.symptoms = symptoms;
        this.visibility = visibility;
        this.entries = entries;
    }

    public static void encode(MicroscopeSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.symptoms.size());
        for (Map.Entry<String, Object> e : msg.symptoms.entrySet()) {
            buf.writeUtf(e.getKey());
            Object v = e.getValue();
            if (v instanceof Float f) { buf.writeByte(0); buf.writeFloat(f); }
            else if (v instanceof Boolean b) { buf.writeByte(1); buf.writeBoolean(b); }
            else { buf.writeByte(2); buf.writeUtf(v.toString()); }
        }
        buf.writeUtf(msg.visibility);
        buf.writeCollection(msg.entries, (buf2, entry) -> entry.encode(buf2));
    }

    public static MicroscopeSyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String key = buf.readUtf();
            byte type = buf.readByte();
            if (type == 0) map.put(key, buf.readFloat());
            else if (type == 1) map.put(key, buf.readBoolean());
            else map.put(key, buf.readUtf());
        }
        String vis = buf.readUtf();
        List<MicroscopeSymptomEntry> entries = buf.readList(MicroscopeSymptomEntry::decode);
        return new MicroscopeSyncPacket(map, vis, entries);
    }

    public static void handle(MicroscopeSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> MicroscopeClientData.set(msg.symptoms, msg.visibility, msg.entries));
        ctx.get().setPacketHandled(true);
    }
}