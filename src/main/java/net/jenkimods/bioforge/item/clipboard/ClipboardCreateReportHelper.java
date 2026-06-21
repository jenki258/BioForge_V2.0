package net.jenkimods.bioforge.item.clipboard;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ClipboardCreateReportHelper {

    public static void createReport(CompoundTag tag, Player player) {
        if (player == null) return;

        if (tag.isEmpty()) return;

        ItemStack offhand = player.getOffhandItem();
        if (!offhand.is(Items.PAPER)) return;

        ItemStack report = new ItemStack(BioForge.MEDICAL_REPORT.get());
        report.setTag(tag);

        ItemStack clipboard = player.getMainHandItem();
        if (clipboard.getItem() instanceof ClipboardItem) {
            NbtObfuscator.clear(clipboard.getOrCreateTag());
            clipboard.getOrCreateTag().remove("SessionToken");
        }

        offhand.shrink(1);
        if (!player.getInventory().add(report)) {
            player.drop(report, false);
        }
    }
}