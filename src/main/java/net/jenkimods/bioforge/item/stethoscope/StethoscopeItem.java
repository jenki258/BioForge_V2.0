package net.jenkimods.bioforge.item.stethoscope;

import net.jenkimods.bioforge.BioForgeTags;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StethoscopeItem extends Item {

    public static final int USE_DURATION = 52000;
    public static final int LISTEN_TICKS_REQUIRED = 20;
    public static final int RESEND_INTERVAL = 40;
    public static final TagKey<EntityType<?>> NO_STETHOSCOPE_TAG =
            TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                    ResourceLocation.tryBuild("bioforge", "no_stethoscope"));

    public StethoscopeItem() {
        super(new Properties().stacksTo(1).durability(0));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.bioforge.stethoscope.tooltip.hold")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.stethoscope.tooltip.look")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.stethoscope.tooltip.self")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override
    public int getUseDuration(ItemStack stack) { return USE_DURATION; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) return;
        int usedTicks = USE_DURATION - remainingUseDuration;
        if (usedTicks < LISTEN_TICKS_REQUIRED) return;
        if ((usedTicks - LISTEN_TICKS_REQUIRED) % RESEND_INTERVAL != 0) return;

        if (level.isClientSide()) {
            Entity target = pickTargetEntity(player);
            int targetId = -1;
            if (target instanceof LivingEntity
                    && !target.getType().is(StethoscopeItem.NO_STETHOSCOPE_TAG)
                    && !target.getType().is(BioForgeTags.NO_DIAGNOSTICS)) {
                targetId = target.getId();
            }

            if (!StethoscopeClientHandler.isListening() || targetId != StethoscopeClientHandler.getCurrentTargetId()) {
                StethoscopeClientHandler.beginListening(targetId);
            }
            StethoscopeNetworkHandler.sendRequest(targetId, player.getUsedItemHand() == InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (level.isClientSide()) StethoscopeClientHandler.stopListening();
    }

    private Entity pickTargetEntity(Player player) {
        if (player.level().isClientSide()) {
            HitResult hit = Minecraft.getInstance().hitResult;
            if (hit instanceof EntityHitResult ehr) return ehr.getEntity();
        }
        return null;
    }
}