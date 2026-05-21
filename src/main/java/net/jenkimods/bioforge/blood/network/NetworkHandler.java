package net.jenkimods.bioforge.blood.network;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    private static final String PROTOCOL = "1";

    private static SimpleChannel CHANNEL;

    private static int id = 0;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                ResourceLocation.tryBuild(BioForge.MODID, "main"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );

        CHANNEL.registerMessage(
                id++,
                BloodSyncPacket.class,
                BloodSyncPacket::encode,
                BloodSyncPacket::decode,
                BloodSyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                id++,
                BloodReagentResultPacket.class,
                BloodReagentResultPacket::encode,
                BloodReagentResultPacket::decode,
                BloodReagentResultPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendReagentResult(ServerPlayer player, BloodReagentResultPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static <MSG> void sendToAll(MSG message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }
}
