package net.jenkimods.bioforge.item.infection;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ContaminatedSubstrateItem extends BlockItem {

    public ContaminatedSubstrateItem() {
        super(BioForge.CONTAMINATED_SUBSTRATE.get(), new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack substrateStack = player.getItemInHand(hand);

        // Already inoculated → allow placement (handled by useOn normally)
        if (isInoculated(substrateStack)) {
            return super.use(level, player, hand);
        }

        // Try to inoculate from swab in other hand
        InteractionHand otherHand = (hand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);
        if (otherStack.getItem() instanceof SwabItem && SwabItem.isContaminated(otherStack)) {
            if (!level.isClientSide()) {
                String payload = NbtObfuscator.readString(otherStack.getOrCreateTag());
                if (payload == null) return InteractionResultHolder.fail(substrateStack);

                NbtObfuscator.writeString(substrateStack.getOrCreateTag(), payload);

                level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8f, 1.2f);
                player.sendSystemMessage(Component.translatable("item.bioforge.contaminated_substrate.inoculated"));
            }
            return InteractionResultHolder.success(substrateStack);
        }

        // Not inoculated and no swab → cannot place
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.translatable("item.bioforge.contaminated_substrate.not_inoculated"));
        }
        return InteractionResultHolder.fail(substrateStack);
    }

    // Prevent right‑click‑on‑block placement if not inoculated
    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!isInoculated(stack)) {
            // Only show message on server
            if (!context.getLevel().isClientSide()) {
                context.getPlayer().sendSystemMessage(Component.translatable("item.bioforge.contaminated_substrate.not_inoculated"));
            }
            return InteractionResult.FAIL;
        }
        return super.useOn(context);
    }

    // In creative, clear the NBT after placing to prevent reuse
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        InteractionResult result = super.place(context);
        if (result.consumesAction() && !context.getLevel().isClientSide()) {
            if (context.getPlayer() != null && context.getPlayer().isCreative()) {
                NbtObfuscator.clear(stack.getOrCreateTag());
                if (context.getPlayer() instanceof ServerPlayer sp) {
                    sp.inventoryMenu.broadcastChanges();
                }
            }
        }
        return result;
    }

    public static boolean isInoculated(ItemStack stack) {
        return NbtObfuscator.readString(stack.getOrCreateTag()) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!isInoculated(stack)) {
            tooltip.add(Component.translatable("item.bioforge.contaminated_substrate.tooltip.clean")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.bioforge.contaminated_substrate.tooltip.usage")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("item.bioforge.contaminated_substrate.tooltip.inoculated")
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }
}