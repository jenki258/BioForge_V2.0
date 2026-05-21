package net.jenkimods.bioforge.infection.network;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class InfectionNetworkHandler {

    private static final String PROTOCOL = "1";
    private static SimpleChannel CHANNEL;
    private static int id = 0;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                ResourceLocation.tryBuild(BioForge.MODID, "infection"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );
        CHANNEL.registerMessage(id++,
                InfectionSyncPacket.class,
                InfectionSyncPacket::encode,
                InfectionSyncPacket::decode,
                InfectionSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}