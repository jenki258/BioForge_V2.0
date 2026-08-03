package net.jenkimods.bioforge.mutation.network;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class MutationNetworkHandler {
    private static final String PROTOCOL = "2";
    private static SimpleChannel CHANNEL;
    private static int id = 0;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                ResourceLocation.tryBuild(BioForge.MODID, "mutation"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );
        CHANNEL.registerMessage(id++,
                MutationSlotPacket.class,
                MutationSlotPacket::encode,
                MutationSlotPacket::decode,
                MutationSlotPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendToPlayer(MutationSlotPacket packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
