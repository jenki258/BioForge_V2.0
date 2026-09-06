package net.jenkimods.bioforge.item.reflex_hammer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReflexHammerItem extends Item {

    public static final int USE_DURATION = 72000;
    private static final double MAX_TARGET_DISTANCE = 4.0;
    public static final TagKey<EntityType<?>> NO_REFLEX_HAMMER_TAG =
            TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                    ResourceLocation.tryBuild("bioforge", "no_reflex_hammer"));

    public ReflexHammerItem() {
        super(new Properties().stacksTo(1).durability(0));
    }

    @Override
    public int getUseDuration(ItemStack stack) { return USE_DURATION; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ReflexHammerClientHandler.beginChargeFromPlayer(player));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (level.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> ReflexHammerClientHandler::cancelCharge);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.bioforge.reflex_hammer.tooltip.hold")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.reflex_hammer.tooltip.target")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.reflex_hammer.tooltip.strike", Component.translatable("key.bioforge.reflex_strike"))
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.reflex_hammer.tooltip.success")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
