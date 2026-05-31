package net.jenkimods.bioforge.item.clipboard.network;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.item.clipboard.ClipboardItem;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClipboardCreateReportPacket {

    private final String data;

    public ClipboardCreateReportPacket(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public static void encode(ClipboardCreateReportPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.data);
    }

    public static ClipboardCreateReportPacket decode(FriendlyByteBuf buf) {
        return new ClipboardCreateReportPacket(buf.readUtf());
    }

    public static void handle(ClipboardCreateReportPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            String reportData = msg.getData();
            if (reportData.isEmpty()) return;

            ItemStack offhand = player.getOffhandItem();
            if (!offhand.is(Items.PAPER)) return;

            ItemStack report = new ItemStack(BioForge.MEDICAL_REPORT.get());
            CompoundTag reportTag = report.getOrCreateTag();
            NbtObfuscator.writeString(reportTag, reportData);

            ItemStack clipboard = player.getMainHandItem();
            if (clipboard.getItem() instanceof ClipboardItem) {
                NbtObfuscator.clear(clipboard.getOrCreateTag());
                clipboard.getOrCreateTag().remove("SessionToken");
            }

            offhand.shrink(1);
            if (!player.getInventory().add(report)) {
                player.drop(report, false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}