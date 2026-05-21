package net.jenkimods.bioforge.item.thermometer;

import net.jenkimods.bioforge.BioForgeTags;
import net.jenkimods.bioforge.infection.InfectionCapability;
import net.jenkimods.bioforge.infection.InfectionData;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.item.clipboard.ClipboardClientHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ThermometerItem extends Item {

    private static final double NORMAL_TEMP_CELSIUS = 36.6;
    private static final double HIGH_TEMP_CELSIUS   = 38.5;
    private static final double LOW_TEMP_CELSIUS    = 35.5;

    private static final String NBT_READY       = "ThermometerReady";
    private static final String NBT_LAST_C      = "LastTempC";
    private static final String NBT_LAST_F      = "LastTempF";
    private static final String NBT_LAST_K      = "LastTempK";
    private static final String NBT_LAST_USED   = "LastUsedTime";
    private static final String NBT_LAST_TARGET = "LastTargetName";
    private static final String NBT_LAST_RAW_C  = "LastTempRaw";

    private static final long COOLDOWN_MS = 3000L;

    public static final TagKey<net.minecraft.world.entity.EntityType<?>> NO_THERMOMETER_TAG =
            TagKey.create(
                    net.minecraft.core.registries.Registries.ENTITY_TYPE,
                    ResourceLocation.tryBuild("bioforge", "no_thermometer")
            );

    public ThermometerItem() {
        super(new Properties().stacksTo(1));
    }

    public static boolean isReady(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(NBT_READY);
    }

    private void setReady(ItemStack stack, boolean ready) {
        stack.getOrCreateTag().putBoolean(NBT_READY, ready);
    }

    private boolean isOnCooldown(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(NBT_LAST_USED)) return false;
        return System.currentTimeMillis() - stack.getTag().getLong(NBT_LAST_USED) < COOLDOWN_MS;
    }

    private void applyInternalCooldown(ItemStack stack) {
        stack.getOrCreateTag().putLong(NBT_LAST_USED, System.currentTimeMillis());
    }

    private void saveAllTemps(ItemStack stack, boolean tempPlus, boolean tempMinus) {
        double celsius = tempPlus ? HIGH_TEMP_CELSIUS : tempMinus ? LOW_TEMP_CELSIUS : NORMAL_TEMP_CELSIUS;
        stack.getOrCreateTag().putDouble(NBT_LAST_RAW_C, celsius);
        stack.getOrCreateTag().putString(NBT_LAST_C, String.format("%.1f\u00b0C", celsius));
        stack.getOrCreateTag().putString(NBT_LAST_F, String.format("%.1f\u00b0F", celsius * 9.0 / 5.0 + 32.0));
        stack.getOrCreateTag().putString(NBT_LAST_K, String.format("%.2fK",       celsius + 273.15));
    }

    private boolean hasReading(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(NBT_LAST_RAW_C);
    }

    private boolean isHighTemp(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(NBT_LAST_RAW_C)) return false;
        return stack.getTag().getDouble(NBT_LAST_RAW_C) >= HIGH_TEMP_CELSIUS;
    }

    private boolean isLowTemp(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(NBT_LAST_RAW_C)) return false;
        return stack.getTag().getDouble(NBT_LAST_RAW_C) <= LOW_TEMP_CELSIUS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (isOnCooldown(stack)) {
            if (level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("item.bioforge.thermometer.not_ready"));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!isReady(stack)) {
            if (level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("item.bioforge.thermometer.shake_first"));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide()) {
        } else {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                InfectionData selfData = InfectionCapability.get(player);
                boolean tPlus  = selfData != null && selfData.getSymptom(BioForgeSymptoms.TEMPERATURE_PLUS);
                boolean tMinus = selfData != null && selfData.getSymptom(BioForgeSymptoms.TEMPERATURE_MINUS);
                long    until  = System.currentTimeMillis() + COOLDOWN_MS;

                saveAllTemps(stack, tPlus, tMinus);
                stack.getOrCreateTag().remove(NBT_LAST_TARGET);

                ThermometerNetworkHandler.sendReading(serverPlayer, hand == InteractionHand.MAIN_HAND,
                        tPlus, tMinus, "", until, player.getUUID());
            }
            applyInternalCooldown(stack);
            setReady(stack, false);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();

        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        if (isOnCooldown(stack)) {
            if (level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("item.bioforge.thermometer.not_ready"));
            }
            return InteractionResult.FAIL;
        }

        if (!isReady(stack)) {
            if (level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("item.bioforge.thermometer.shake_first"));
            }
            return InteractionResult.FAIL;
        }

        if (target.getType().is(NO_THERMOMETER_TAG) || target.getType().is(BioForgeTags.NO_DIAGNOSTICS)) {
            if (level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("item.bioforge.thermometer.no_infection", target.getDisplayName()));
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) {
        } else {
            InfectionData targetData = InfectionCapability.get(target);
            if (targetData != null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                boolean tPlus  = targetData.getSymptom(BioForgeSymptoms.TEMPERATURE_PLUS);
                boolean tMinus = targetData.getSymptom(BioForgeSymptoms.TEMPERATURE_MINUS);
                String  name   = target.getDisplayName().getString();
                long    until  = System.currentTimeMillis() + COOLDOWN_MS;

                saveAllTemps(stack, tPlus, tMinus);
                stack.getOrCreateTag().putString(NBT_LAST_TARGET, name);

                ThermometerNetworkHandler.sendReading(serverPlayer, hand == InteractionHand.MAIN_HAND,
                        tPlus, tMinus, name, until, target.getUUID());
            }
            applyInternalCooldown(stack);
            setReady(stack, false);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public void applyReadingClient(ItemStack stack, boolean tempPlus, boolean tempMinus,
                                   String targetName, long cooldownUntil, UUID targetUUID) {
        saveAllTemps(stack, tempPlus, tempMinus);
        stack.getOrCreateTag().putLong(NBT_LAST_USED, cooldownUntil - COOLDOWN_MS);
        stack.getOrCreateTag().putBoolean(NBT_READY, false);
        if (targetName.isEmpty()) {
            stack.getOrCreateTag().remove(NBT_LAST_TARGET);
        } else {
            stack.getOrCreateTag().putString(NBT_LAST_TARGET, targetName);
        }

        if (ClipboardClientHandler.hasPatient()) {
            UUID clipboardUUID = ClipboardClientHandler.getSubjectUUID();
            if (targetUUID != null && clipboardUUID != null && targetUUID.equals(clipboardUUID)) {
                double celsius = tempPlus ? HIGH_TEMP_CELSIUS : tempMinus ? LOW_TEMP_CELSIUS : NORMAL_TEMP_CELSIUS;
                ClipboardClientHandler.recordTemperature((float)celsius, false);
            }
        }
    }

    public void onShake(Player player, ItemStack stack) {
        if (!isReady(stack)) {
            setReady(stack, true);
            if (player.level().isClientSide()) {
                player.sendSystemMessage(Component.translatable("item.bioforge.thermometer.shaken"));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasReading(stack)) {
            ChatFormatting tempColor = isHighTemp(stack) ? ChatFormatting.RED
                    : isLowTemp(stack) ? ChatFormatting.AQUA
                    : ChatFormatting.GREEN;

            boolean hasTarget = stack.hasTag() && stack.getTag().contains(NBT_LAST_TARGET);
            if (hasTarget) {
                tooltip.add(Component.translatable("item.bioforge.thermometer.reading_header_target",
                        stack.getTag().getString(NBT_LAST_TARGET)).withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("item.bioforge.thermometer.reading_header")
                        .withStyle(ChatFormatting.GRAY));
            }

            tooltip.add(Component.translatable("item.bioforge.thermometer.temp_c",
                    stack.getTag().getString(NBT_LAST_C)).withStyle(tempColor));
            tooltip.add(Component.translatable("item.bioforge.thermometer.temp_f",
                    stack.getTag().getString(NBT_LAST_F)).withStyle(tempColor));
            tooltip.add(Component.translatable("item.bioforge.thermometer.temp_k",
                    stack.getTag().getString(NBT_LAST_K)).withStyle(tempColor));

            tooltip.add(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY));
        }

        if (isReady(stack)) {
            tooltip.add(Component.translatable("item.bioforge.thermometer.status_ready")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("item.bioforge.thermometer.status_used")
                    .withStyle(ChatFormatting.RED));
        }

        tooltip.add(Component.literal("─────────────────").withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.translatable("item.bioforge.thermometer.tooltip.shake")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.thermometer.tooltip.use_self")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.thermometer.tooltip.use_mob")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}