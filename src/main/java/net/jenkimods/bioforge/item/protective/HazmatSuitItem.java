package net.jenkimods.bioforge.item.protective;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Vanilla dye storage keeps crafting, armor rendering and water cauldrons interoperable. */
public final class HazmatSuitItem extends ProtectiveGearItem implements DyeableLeatherItem {
    public static final int DEFAULT_COLOR = 0xFFFFFF;
    public HazmatSuitItem(Type type) {
        super(BioForgeArmorMaterial.HAZCURE, type, new Properties(), "item.bioforge.protective_gear.hazcure");
    }
    @Override public int getColor(ItemStack stack) {
        return hasCustomColor(stack) ? DyeableLeatherItem.super.getColor(stack) : DEFAULT_COLOR;
    }
    @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, @Nullable String type) {
        return "overlay".equals(type) ? "bioforge:textures/models/armor/hazmat_overlay.png"
                : "bioforge:textures/models/armor/hazcure_layer_" + (slot == EquipmentSlot.LEGS ? "2" : "1") + ".png";
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.bioforge.hazmat.dye_hint").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
