package net.jenkimods.bioforge.item.thermometer;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;

public class ThermometerNetworkHandler {

    private static final String PROTOCOL = "1";
    private static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                ResourceLocation.tryBuild(BioForge.MODID, "thermometer"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );
        CHANNEL.registerMessage(0,
                ThermometerShakePacket.class,
                ThermometerShakePacket::encode,
                ThermometerShakePacket::decode,
                ThermometerShakePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    public static void sendShake(boolean mainHand) {
        CHANNEL.sendToServer(new ThermometerShakePacket(mainHand));
    }
}