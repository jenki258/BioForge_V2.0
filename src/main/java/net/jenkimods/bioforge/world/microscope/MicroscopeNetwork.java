package net.jenkimods.bioforge.world.microscope;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;

public class MicroscopeNetwork {
    private static final String PROTOCOL = "2";
    private static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                ResourceLocation.tryBuild(BioForge.MODID, "microscope"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
        );
        CHANNEL.registerMessage(0, MicroscopeSyncPacket.class,
                MicroscopeSyncPacket::encode, MicroscopeSyncPacket::decode,
                MicroscopeSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, MicroscopeCalibrationPacket.class,
                MicroscopeCalibrationPacket::encode,
                MicroscopeCalibrationPacket::decode,
                MicroscopeCalibrationPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToPlayer(MicroscopeSyncPacket packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendCalibration(MicroscopeCalibrationPacket packet) {
        CHANNEL.sendToServer(packet);
    }
}
