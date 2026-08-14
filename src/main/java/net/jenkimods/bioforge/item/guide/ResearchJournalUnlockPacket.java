package net.jenkimods.bioforge.item.guide;

import net.jenkimods.bioforge.client.ResearchJournalClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ResearchJournalUnlockPacket(ResourceLocation pageId, Component title,
                                          boolean activated) {
    public static void encode(ResearchJournalUnlockPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.pageId());
        buffer.writeComponent(packet.title());
        buffer.writeBoolean(packet.activated());
    }

    public static ResearchJournalUnlockPacket decode(FriendlyByteBuf buffer) {
        return new ResearchJournalUnlockPacket(buffer.readResourceLocation(),
                buffer.readComponent(), buffer.readBoolean());
    }

    public static void handle(ResearchJournalUnlockPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ResearchJournalClient.showUnlock(packet.title(), packet.activated())));
        context.setPacketHandled(true);
    }
}
