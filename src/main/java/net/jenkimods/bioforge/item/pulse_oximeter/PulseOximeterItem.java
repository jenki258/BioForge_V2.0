package net.jenkimods.bioforge.item.pulse_oximeter;

import net.jenkimods.bioforge.BioForgeTags;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

public class PulseOximeterItem extends Item {
    public static final int USE_DURATION = 72000;
    public static final int HOLD_TICKS_REQUIRED = 40;   // faster than stethoscope

    public PulseOximeterItem() {
        super(new Properties().stacksTo(1).durability(0));
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
        if (usedTicks < HOLD_TICKS_REQUIRED) return;
        if ((usedTicks - HOLD_TICKS_REQUIRED) % 20 != 0) return;   // update every second

        if (level.isClientSide()) {
            Entity target = pickTargetEntity(player);
            int targetId = -1;
            if (target instanceof LivingEntity
                    && !target.getType().is(BioForgeTags.NO_DIAGNOSTICS)
                    && !target.getType().is(PulseOximeterItem.NO_PULSE_OXIMETER_TAG)) {
                targetId = target.getId();
            }
            PulseOximeterNetworkHandler.sendRequest(targetId);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (level.isClientSide()) PulseOximeterClientHandler.stopInspection();
    }

    private Entity pickTargetEntity(Player player) {
        if (player.level().isClientSide()) {
            HitResult hit = Minecraft.getInstance().hitResult;
            if (hit instanceof EntityHitResult ehr) return ehr.getEntity();
        }
        return null;
    }

    public static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> NO_PULSE_OXIMETER_TAG =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                    net.minecraft.resources.ResourceLocation.tryBuild("bioforge", "no_pulse_oximeter"));

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.bioforge.pulse_oximeter.tooltip.hold")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.pulse_oximeter.tooltip.target")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.pulse_oximeter.tooltip.stabilize")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}