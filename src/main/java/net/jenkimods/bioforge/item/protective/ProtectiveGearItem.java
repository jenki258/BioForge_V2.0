package net.jenkimods.bioforge.item.protective;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ProtectiveGearItem extends ArmorItem {
    public enum WearableStyle {
        VANILLA,
        THERMAL_BAG,
        MEDICAL_MASK,
        PROTECTIVE_GLOVES
    }

    private final String tooltipKey;
    private final WearableStyle wearableStyle;
    @Nullable
    private final String wornTexture;

    public ProtectiveGearItem(ArmorMaterial material, Type type,
                              Properties properties, String tooltipKey) {
        this(material, type, properties, tooltipKey, WearableStyle.VANILLA, null);
    }

    public ProtectiveGearItem(ArmorMaterial material, Type type,
                              Properties properties, String tooltipKey,
                              WearableStyle wearableStyle,
                              @Nullable String wornTexture) {
        super(material, type, properties);
        this.tooltipKey = tooltipKey;
        this.wearableStyle = wearableStyle;
        this.wornTexture = wornTexture;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(net.jenkimods.bioforge.client.render.ProtectiveGearClientExtensions
                .create(() -> wearableStyle));
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity,
                                  EquipmentSlot slot, @Nullable String type) {
        return wornTexture == null
                ? super.getArmorTexture(stack, entity, slot, type)
                : wornTexture;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey)
                .withStyle(ChatFormatting.AQUA));
        if (tooltipKey.endsWith(".hazcure")) {
            tooltip.add(Component.translatable(
                    "item.bioforge.protective_gear.hazcure_set")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
