package net.jenkimods.bioforge.item.crispr;

import net.jenkimods.bioforge.vaccine.VaccineResearchNotes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class CrisprNotesItem extends Item {
    public CrisprNotesItem() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                   InteractionHand hand) {
        ItemStack notes = player.getItemInHand(hand);
        VaccineResearchNotes.Data data = VaccineResearchNotes.read(notes);
        if (data == null) return InteractionResultHolder.pass(notes);
        InteractionHand other = hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack target = player.getItemInHand(other);
        if (target.is(Items.WRITABLE_BOOK)) {
            if (!level.isClientSide()) VaccineResearchNotes.appendToBook(target, data);
            return InteractionResultHolder.sidedSuccess(notes, level.isClientSide());
        }
        if (!target.is(Items.PAPER)) return InteractionResultHolder.pass(notes);
        if (!level.isClientSide()) {
            ItemStack copy = notes.copy();
            copy.setCount(1);
            if (!player.getAbilities().instabuild) target.shrink(1);
            if (!player.getInventory().add(copy)) player.drop(copy, false);
        }
        return InteractionResultHolder.sidedSuccess(notes, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        VaccineResearchNotes.Data data = VaccineResearchNotes.read(stack);
        if (data == null) {
            tooltip.add(Component.translatable("item.bioforge.crispr_notes.blank")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("item.bioforge.crispr_notes.batch",
                data.sampleFingerprint()).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.bioforge.crispr_notes.quality",
                String.format(Locale.ROOT, "%.1f%%", data.quality() * 100.0f))
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal("gRNA-1  " + data.guideOne())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("gRNA-2  " + data.guideTwo())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("gRNA-3  " + data.guideThree())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("item.bioforge.crispr_notes.hint_template")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.bioforge.crispr_notes.hint_copy")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.crispr_notes.hint_book")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
