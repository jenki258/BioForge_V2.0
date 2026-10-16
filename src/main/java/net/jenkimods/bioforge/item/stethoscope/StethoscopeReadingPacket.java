package net.jenkimods.bioforge.item.stethoscope;

import net.jenkimods.bioforge.infection.HeartRate;
import net.jenkimods.bioforge.infection.LungSound;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StethoscopeReadingPacket {

    private final HeartRate heartRate;
    private final LungSound lungSound;
    private final String targetName;
    private final boolean mainHand;
    private final AuscultationSite site;
    private final int targetId;

    public StethoscopeReadingPacket(HeartRate heartRate, LungSound lungSound,
                                    String targetName, boolean mainHand, AuscultationSite site, int targetId) {
        this.heartRate = heartRate;
        this.lungSound = lungSound;
        this.targetName = targetName;
        this.mainHand = mainHand;
        this.site=site;this.targetId=targetId;
    }

    public static void encode(StethoscopeReadingPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.heartRate.name());
        buf.writeUtf(msg.lungSound.name());
        buf.writeUtf(msg.targetName);
        buf.writeBoolean(msg.mainHand);
        buf.writeEnum(msg.site);buf.writeInt(msg.targetId);
    }

    public static StethoscopeReadingPacket decode(FriendlyByteBuf buf) {
        HeartRate heartRate = HeartRate.fromName(buf.readUtf());
        LungSound lungSound = LungSound.fromName(buf.readUtf());
        String targetName = buf.readUtf();
        boolean mainHand = buf.readBoolean();
        return new StethoscopeReadingPacket(heartRate, lungSound, targetName, mainHand, buf.readEnum(AuscultationSite.class),buf.readInt());
    }

    public static void handle(StethoscopeReadingPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> ClientHandler.handle(msg)
            );
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public class ClientHandler {

        public static void handle(StethoscopeReadingPacket msg) {
            Player player = Minecraft.getInstance().player;
            if (player == null || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof StethoscopeItem)
                    || player.getUsedItemHand()!=(msg.mainHand?net.minecraft.world.InteractionHand.MAIN_HAND:net.minecraft.world.InteractionHand.OFF_HAND)
                    || !StethoscopeClientHandler.isListening()
                    || msg.targetId!=StethoscopeClientHandler.getCurrentTargetId()) return;

            StethoscopeClientHandler.applyReading(
                    msg.heartRate,
                    msg.lungSound,
                    msg.targetName,msg.site
            );
        }
    }
}
