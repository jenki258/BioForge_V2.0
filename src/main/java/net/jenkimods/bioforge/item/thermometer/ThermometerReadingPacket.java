package net.jenkimods.bioforge.item.thermometer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ThermometerReadingPacket {

    private final boolean mainHand;
    private final boolean tempPlus;
    private final boolean tempMinus;
    private final String targetName;
    private final long cooldownUntil;
    private final UUID targetUUID;               // ← added

    public ThermometerReadingPacket(boolean mainHand, boolean tempPlus, boolean tempMinus,
                                    String targetName, long cooldownUntil, UUID targetUUID) {
        this.mainHand      = mainHand;
        this.tempPlus      = tempPlus;
        this.tempMinus     = tempMinus;
        this.targetName    = targetName;
        this.cooldownUntil = cooldownUntil;
        this.targetUUID    = targetUUID;
    }

    public static void encode(ThermometerReadingPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.mainHand);
        buf.writeBoolean(msg.tempPlus);
        buf.writeBoolean(msg.tempMinus);
        buf.writeUtf(msg.targetName);
        buf.writeLong(msg.cooldownUntil);
        // Write UUID (nullable, empty UUID for self? We'll use hasUUID flag)
        buf.writeBoolean(msg.targetUUID != null);
        if (msg.targetUUID != null) {
            buf.writeUUID(msg.targetUUID);
        }
    }

    public static ThermometerReadingPacket decode(FriendlyByteBuf buf) {
        boolean mainHand = buf.readBoolean();
        boolean tempPlus = buf.readBoolean();
        boolean tempMinus = buf.readBoolean();
        String targetName = buf.readUtf();
        long cooldownUntil = buf.readLong();
        UUID uuid = null;
        if (buf.readBoolean()) {
            uuid = buf.readUUID();
        }
        return new ThermometerReadingPacket(mainHand, tempPlus, tempMinus, targetName, cooldownUntil, uuid);
    }

    public static void handle(ThermometerReadingPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            ItemStack stack = msg.mainHand
                    ? player.getItemInHand(InteractionHand.MAIN_HAND)
                    : player.getItemInHand(InteractionHand.OFF_HAND);

            if (!(stack.getItem() instanceof ThermometerItem item)) return;

            item.applyReadingClient(stack, msg.tempPlus, msg.tempMinus, msg.targetName, msg.cooldownUntil, msg.targetUUID);
        });
        ctx.get().setPacketHandled(true);
    }
}