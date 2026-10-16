package net.jenkimods.bioforge.item.vaccine;

import net.jenkimods.bioforge.vaccine.VaccineProfile;
import net.jenkimods.bioforge.BioForge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class FieldVaccineItem extends VaccineItem {
    public static final int COOLDOWN_TICKS = 1200;
    private static final String COOLDOWN_CHANNEL = "field_vaccine_cooldown";
    public static float effectiveness(ItemStack stack) {
        if (!(stack.getItem() instanceof FieldVaccineItem)) return 1.0F;
        return stack.is(BioForge.STABILIZED_FIELD_VACCINE.get()) ? .25F : .15F;
    }
    public static boolean canTreat(net.minecraft.world.entity.LivingEntity target, ItemStack medicine) {
        if (!(medicine.getItem() instanceof FieldVaccineItem)) return true;
        var saved = net.jenkimods.bioforge.util.NbtObfuscator.readCompound(target.getPersistentData(), COOLDOWN_CHANNEL);
        return saved == null || saved.getLong("Until") <= target.level().getGameTime();
    }
    public static void markTreatment(net.minecraft.world.entity.LivingEntity target) {
        var saved = new net.minecraft.nbt.CompoundTag();
        saved.putLong("Until", target.level().getGameTime() + COOLDOWN_TICKS);
        net.jenkimods.bioforge.util.NbtObfuscator.writeCompound(target.getPersistentData(), COOLDOWN_CHANNEL, saved);
    }
    public static void coolDown(net.minecraft.world.entity.player.Player practitioner) {
        practitioner.getCooldowns().addCooldown(BioForge.FIELD_VACCINE.get(), COOLDOWN_TICKS);
        practitioner.getCooldowns().addCooldown(BioForge.STABILIZED_FIELD_VACCINE.get(), COOLDOWN_TICKS);
    }
    @Override public Component getName(ItemStack stack) {
        return VaccineProfile.read(stack) == null
                ? Component.translatable("item.bioforge.field_vaccine.blank") : super.getName(stack);
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (VaccineProfile.read(stack) == null) {
            tooltip.add(Component.translatable("gui.bioforge.field.blank_help").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("item.bioforge.field_vaccine.weak").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
