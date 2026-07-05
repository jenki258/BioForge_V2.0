package net.jenkimods.bioforge.item;

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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CatalystVialItem extends Item {

    private static final int MAX_CHARGES = 1;

    public CatalystVialItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);

        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack reagent = player.getItemInHand(other);
        if (reagent.isEmpty()) return InteractionResultHolder.fail(stack);

        PathogenType pathogen = CatalystMappingManager.INSTANCE.getPathogen(reagent.getItem());
        if (pathogen == null && reagent.getItem() == net.minecraft.world.item.Items.NETHER_STAR) {
            pathogen = CatalystMappingManager.INSTANCE.getRandomPathogen();
        }
        if (pathogen != null) {
            stack.getOrCreateTag().putString("Pathogen", pathogen.name());
            stack.getOrCreateTag().putInt("Charges", MAX_CHARGES);
            if (!player.isCreative()) reagent.shrink(1);
            player.sendSystemMessage(Component.translatable("item.bioforge.catalyst_vial.set", pathogen.name()));
        } else {
            player.sendSystemMessage(Component.translatable("item.bioforge.catalyst_vial.invalid_reagent"));
        }
        return InteractionResultHolder.success(stack);
    }

    @Nullable
    public static PathogenType getPathogen(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("Pathogen")) {
            return PathogenType.fromName(stack.getTag().getString("Pathogen"));
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
        PathogenType pathogen = getPathogen(stack);
        int charges = getCharges(stack);
        if (pathogen != null && charges > 0) {
            tooltip.add(Component.translatable("item.bioforge.catalyst_vial.pathogen", pathogen.name())
                    .withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            tooltip.add(Component.translatable("item.bioforge.catalyst_vial.empty").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.bioforge.catalyst_vial.tooltip").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}