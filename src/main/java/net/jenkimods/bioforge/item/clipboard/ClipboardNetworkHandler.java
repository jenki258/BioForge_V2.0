package net.jenkimods.bioforge.item.clipboard;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.item.clipboard.network.ClipboardAppendToBookPacket;
import net.jenkimods.bioforge.item.clipboard.network.ClipboardCreateReportPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ClipboardNetworkHandler {
    private static final String PROTOCOL = "1";
    private static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                ResourceLocation.tryBuild(BioForge.MODID, "clipboard"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );
        int id = 0;
        CHANNEL.registerMessage(id++,
                ClipboardCreateReportPacket.class,
                ClipboardCreateReportPacket::encode,
                ClipboardCreateReportPacket::decode,
                ClipboardCreateReportPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(id++,
                ClipboardAppendToBookPacket.class,
                ClipboardAppendToBookPacket::encode,
                ClipboardAppendToBookPacket::decode,
                ClipboardAppendToBookPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    public static void sendCreateReport(String data) {
        CHANNEL.sendToServer(new ClipboardCreateReportPacket(data));
    }

    public static void sendAppendToBook(String data) {
        CHANNEL.sendToServer(new ClipboardAppendToBookPacket(data));
    }
}