package net.jenkimods.bioforge.infection.naming;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StrainNamePromptPacket(String fingerprint) {
    public static void encode(StrainNamePromptPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.fingerprint(), 64);
    }

    public static StrainNamePromptPacket decode(FriendlyByteBuf buffer) {
        return new StrainNamePromptPacket(buffer.readUtf(64));
    }

    public static void handle(StrainNamePromptPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> StrainNameClientHandler.open(packet.fingerprint())));
        context.setPacketHandled(true);
    }
}
