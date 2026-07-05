package net.jenkimods.bioforge.item;

import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class VirusSampleItem extends Item {

    public VirusSampleItem() {
        super(new Properties().stacksTo(1));
    }

    public static StrainData getStrain(ItemStack stack) {
        String raw = NbtObfuscator.readString(stack.getOrCreateTag());
        if (raw != null && !raw.isEmpty()) {
            return StrainData.parse(raw);
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        StrainData strain = getStrain(stack);
        if (strain != null) {
            tooltip.add(Component.translatable("item.bioforge.virus_sample.pathogen", strain.getPathogen().name()).withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.translatable("item.bioforge.virus_sample.stats").withStyle(ChatFormatting.GRAY));
            for (Map.Entry<String, String> entry : strain.getSymptoms().entrySet()) {
                tooltip.add(Component.literal(entry.getKey() + ": " + entry.getValue()).withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.translatable("item.bioforge.virus_sample.empty").withStyle(ChatFormatting.GRAY));
        }
    }
}