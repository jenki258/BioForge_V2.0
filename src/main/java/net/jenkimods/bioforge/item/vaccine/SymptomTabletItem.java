package net.jenkimods.bioforge.item.vaccine;

import net.jenkimods.bioforge.config.BioForgeServerConfig;
import net.jenkimods.bioforge.crispr.CrisprDisplayNames;
import net.jenkimods.bioforge.crispr.VaccineTargetCategory;
import net.jenkimods.bioforge.vaccine.SymptomTabletProfile;
import net.jenkimods.bioforge.vaccine.SymptomTreatment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SymptomTabletItem extends Item {
    private final SymptomTabletProfile fixedProfile;
    public SymptomTabletItem() {
        this(null);
    }

    public SymptomTabletItem(SymptomTabletProfile fixedProfile) {
        super(new Properties().stacksTo(16));
        this.fixedProfile = fixedProfile;
    }

    public SymptomTabletProfile profile(ItemStack stack) {
        return fixedProfile == null ? SymptomTabletProfile.read(stack) : fixedProfile;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                  InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        SymptomTabletProfile profile = profile(stack);
        if (profile == null || !BioForgeServerConfig.isSymptomEnabled(profile.symptomId())) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable(
                        "message.bioforge.symptom_tablet.blank")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level,
                                     LivingEntity entity) {
        SymptomTabletProfile profile = profile(stack);
        if (!level.isClientSide() && profile != null) {
            int duration = SymptomTreatment.apply(entity, profile);
            if (duration == 0) return stack;
            level.playSound(null, entity.blockPosition(), net.jenkimods.bioforge.audio.BioForgeActionSounds.EVENTS.get("instrument.tablet.take").get(),
                    SoundSource.PLAYERS, 0.7F, 1.25F);
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable(
                        "message.bioforge.symptom_tablet.applied",
                        CrisprDisplayNames.target(VaccineTargetCategory.SYMPTOM,
                                profile.symptomId()), StringUtil.formatTickDuration(duration))
                        .withStyle(ChatFormatting.AQUA), true);
                player.getCooldowns().addCooldown(this, 40);
            }
            if (!(entity instanceof Player player) || !player.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    @Override public int getUseDuration(ItemStack stack) { return 24; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.EAT; }
    @Override public boolean isFoil(ItemStack stack) {
        return fixedProfile == null && profile(stack) != null || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        SymptomTabletProfile profile = profile(stack);
        if (profile == null) {
            tooltip.add(Component.translatable("item.bioforge.symptom_tablet.blank")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.bioforge.symptom_tablet.vaccine_maker")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(CrisprDisplayNames.target(VaccineTargetCategory.SYMPTOM,
                profile.symptomId()).copy().withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.bioforge.symptom_tablet.duration",
                StringUtil.formatTickDuration(profile.durationTicks()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.bioforge.symptom_tablet.not_cure")
                .withStyle(ChatFormatting.DARK_RED));
    }
}
