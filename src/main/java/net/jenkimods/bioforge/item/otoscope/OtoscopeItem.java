package net.jenkimods.bioforge.item.otoscope;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.BioForgeTags;
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

public class OtoscopeItem extends Item {

    public static final int USE_DURATION = 72000;
    public static final int HOLD_TICKS_REQUIRED = 60;

    public static final TagKey<EntityType<?>> NO_OTOSCOPE_TAG =
            TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                    ResourceLocation.tryBuild(BioForge.MODID, "no_otoscope"));

    public OtoscopeItem() {
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
        if (usedTicks != HOLD_TICKS_REQUIRED) return;

        if (level.isClientSide()) {
            boolean selfMode = player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof MirrorItem;
            if (selfMode) {
                OtoscopeNetworkHandler.sendRequest(-1);
            } else {
                Entity target = pickTargetEntity(player, level);
                if (target instanceof LivingEntity
                        && !target.getType().is(NO_OTOSCOPE_TAG)
                        && !target.getType().is(BioForgeTags.NO_DIAGNOSTICS)) {
                    OtoscopeNetworkHandler.sendRequest(target.getId());
                }
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (level.isClientSide()) {
            OtoscopeClientHandler.stopInspection();
        }
    }

    private Entity pickTargetEntity(Player player, Level level) {
        if (level.isClientSide()) {
            HitResult hit = net.minecraft.client.Minecraft.getInstance().hitResult;
            if (hit instanceof EntityHitResult ehr) return ehr.getEntity();
        } else {
            HitResult hit = player.pick(4.5D, 0.0F, false);
            if (hit instanceof EntityHitResult ehr) return ehr.getEntity();
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.bioforge.otoscope.tooltip.hold")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.otoscope.tooltip.target")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.otoscope.tooltip.move")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.otoscope.tooltip.exit")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}