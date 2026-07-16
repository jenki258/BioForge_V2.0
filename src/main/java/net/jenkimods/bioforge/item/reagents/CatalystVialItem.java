package net.jenkimods.bioforge.item.reagents;

import net.jenkimods.bioforge.infection.PathogenType;
import net.jenkimods.bioforge.world.incubator.CatalystMappingManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CatalystVialItem extends Item {

    private static final int MAX_CHARGES = 1;

    public CatalystVialItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);

        if (isSet(stack)) {
            player.sendSystemMessage(Component.translatable("item.bioforge.catalyst_vial.already_set"));
            return InteractionResultHolder.fail(stack);
        }

        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack reagent = player.getItemInHand(other);
        if (reagent.isEmpty()) return InteractionResultHolder.fail(stack);

        PathogenType pathogen = CatalystMappingManager.INSTANCE.getPathogen(reagent.getItem());
        String pathogenName;

        if (reagent.getItem() == net.minecraft.world.item.Items.NETHER_STAR) {
            pathogenName = "RANDOM";
            if (!player.isCreative()) reagent.shrink(1);
            player.sendSystemMessage(Component.translatable("item.bioforge.catalyst_vial.set_random"));
        } else if (pathogen != null) {
            pathogenName = pathogen.name();
            if (!player.isCreative()) reagent.shrink(1);
            player.sendSystemMessage(Component.translatable("item.bioforge.catalyst_vial.set", pathogenName));
        } else {
            player.sendSystemMessage(Component.translatable("item.bioforge.catalyst_vial.invalid_reagent"));
            return InteractionResultHolder.fail(stack);
        }

        stack.getOrCreateTag().putString("Pathogen", pathogenName);
        stack.getOrCreateTag().putInt("Charges", MAX_CHARGES);
        return InteractionResultHolder.success(stack);
    }

    public static boolean isSet(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("Pathogen");
    }

    @Nullable
    public static PathogenType getPathogen(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("Pathogen")) {
            String name = stack.getTag().getString("Pathogen");
            if ("RANDOM".equals(name)) return null;
            return PathogenType.fromName(name);
        }
        return null;
    }

    @Nullable
    public static PathogenType getPathogenOrRandom(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("Pathogen")) {
            String name = stack.getTag().getString("Pathogen");
            if ("RANDOM".equals(name)) {
                return CatalystMappingManager.INSTANCE.getRandomPathogen();
            }
            return PathogenType.fromName(name);
        }
        return null;
    }

    public static int getCharges(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("Charges") : 0;
    }

    public static void consumeCharge(ItemStack stack) {
        if (stack.hasTag()) {
            int charges = stack.getTag().getInt("Charges") - 1;
            if (charges <= 0) {
                stack.shrink(1);
            } else {
                stack.getTag().putInt("Charges", charges);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!isSet(stack)) {
            tooltip.add(Component.translatable("item.bioforge.catalyst_vial.empty").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(" "));
            tooltip.add(Component.translatable("item.bioforge.catalyst_vial.tooltip").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal(" "));
            tooltip.add(Component.translatable("item.bioforge.catalyst_vial.mappings_header").withStyle(ChatFormatting.GOLD));

            Map<Item, PathogenType> mappings = CatalystMappingManager.INSTANCE.getAllMappings();
            for (Map.Entry<Item, PathogenType> entry : mappings.entrySet()) {
                String itemName = Component.translatable(entry.getKey().getDescriptionId()).getString();
                String pathogenName = Component.translatable("pathogen.bioforge." + entry.getValue().name().toLowerCase()).getString();
                tooltip.add(Component.literal("  " + itemName + " → " + pathogenName).withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip.add(Component.translatable("item.bioforge.catalyst_vial.nether_star_hint").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }

        String pathogenName = stack.getTag().getString("Pathogen");
        int charges = getCharges(stack);
        if ("RANDOM".equals(pathogenName)) {
            tooltip.add(Component.translatable("item.bioforge.catalyst_vial.pathogen_random").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("item.bioforge.catalyst_vial.pathogen", pathogenName).withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (charges > 0) {
            tooltip.add(Component.translatable("item.bioforge.catalyst_vial.charges", charges).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.translatable("item.bioforge.catalyst_vial.place_in_incubator").withStyle(ChatFormatting.DARK_GRAY));
    }
}