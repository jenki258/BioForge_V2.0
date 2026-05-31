package net.jenkimods.bioforge.item.infection;

import net.jenkimods.bioforge.BioForgeTags;
import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.*;

public class SwabItem extends Item {

    public SwabItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isContaminated(stack)) return InteractionResultHolder.fail(stack);

        if (!level.isClientSide()) {
            InfectionData data = InfectionCapability.get(player);
            String payload;
            if (data != null && data.isInfected()) {
                payload = buildStrainPayload(data);
            } else {
                payload = "CLEAN";
            }

            NbtObfuscator.writeString(stack.getOrCreateTag(), payload);
            player.setItemInHand(hand, stack);

            if ("CLEAN".equals(payload)) {
                player.sendSystemMessage(Component.translatable("item.bioforge.swab.collected_clean_self"));
            } else {
                player.sendSystemMessage(Component.translatable("item.bioforge.swab.collected_self"));
            }
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target, InteractionHand hand) {
        Level level = player.level();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (isContaminated(stack)) return InteractionResult.FAIL;

        if (target.getType().is(BioForgeTags.NO_DIAGNOSTICS)) {
            player.sendSystemMessage(Component.translatable("item.bioforge.swab.blocked",
                    target.getDisplayName()));
            return InteractionResult.FAIL;
        }

        InfectionData data = InfectionCapability.get(target);
        String payload;
        if (data != null && data.isInfected()) {
            payload = buildStrainPayload(data);
        } else {
            payload = "CLEAN";
        }

        NbtObfuscator.writeString(stack.getOrCreateTag(), payload);
        player.setItemInHand(hand, stack);

        if ("CLEAN".equals(payload)) {
            player.sendSystemMessage(Component.translatable("item.bioforge.swab.collected",
                    target.getDisplayName()));
        } else {
            player.sendSystemMessage(Component.translatable("item.bioforge.swab.collected",
                    target.getDisplayName()));
        }
        return InteractionResult.CONSUME;
    }

    private String buildStrainPayload(@Nullable InfectionData data) {
        if (data == null || !data.isInfected()) return "CLEAN";
        String colonyId = "PLACEHOLDER";
        StringBuilder sb = new StringBuilder();
        sb.append(colonyId).append("|").append(data.getPathogenType().name()).append("|");
        Iterator<InfectionType> iter = data.getInfectionTypes().iterator();
        while (iter.hasNext()) {
            sb.append(iter.next().name());
            if (iter.hasNext()) sb.append(",");
        }
        sb.append(";");
        sb.append("HeartRate=").append(data.getSymptom(BioForgeSymptoms.HEART_RATE).name()).append(";");
        sb.append("LungSound=").append(data.getSymptom(BioForgeSymptoms.LUNG_SOUND).name()).append(";");
        sb.append("TempPlus=").append(data.getSymptom(BioForgeSymptoms.TEMPERATURE_PLUS)).append(";");
        sb.append("TempMinus=").append(data.getSymptom(BioForgeSymptoms.TEMPERATURE_MINUS)).append(";");
        sb.append("Redness=").append(data.getSymptom(BioForgeSymptoms.OTOSCOPE_REDNESS)).append(";");
        sb.append("Lesions=").append(data.getSymptom(BioForgeSymptoms.OTOSCOPE_LESIONS)).append(";");
        sb.append("Secretion=").append(data.getSymptom(BioForgeSymptoms.OTOSCOPE_SECRETION)).append(";");
        sb.append("Swelling=").append(data.getSymptom(BioForgeSymptoms.OTOSCOPE_SWELLING)).append(";");
        sb.append("ReflexDelay=").append(data.getSymptom(BioForgeSymptoms.REFLEX_DELAY)).append(";");
        sb.append("ReflexStrength=").append(data.getSymptom(BioForgeSymptoms.REFLEX_STRENGTH)).append(";");
        sb.append("NeuralDamage=").append(data.getSymptom(BioForgeSymptoms.NEURAL_DAMAGE)).append(";");
        sb.append("OxygenSaturation=").append(data.getSymptom(BioForgeSymptoms.OXYGEN_SATURATION)).append(";");
        sb.append("PerfusionIndex=").append(data.getSymptom(BioForgeSymptoms.PERFUSION_INDEX)).append(";");
        sb.append("InfectionStrength=").append(data.getSymptom(BioForgeSymptoms.INFECTION_STRENGTH)).append(";");
        sb.append("ColonyRadius=").append(data.getSymptom(BioForgeSymptoms.COLONY_RADIUS)).append(";");
        sb.append("MaxInfestedBlocks=").append(data.getSymptom(BioForgeSymptoms.MAX_INFESTED_BLOCKS)).append(";");
        return sb.toString();
    }

    public static boolean isContaminated(ItemStack stack) {
        return NbtObfuscator.readString(stack.getOrCreateTag()) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        if (!isContaminated(stack)) {
            tooltip.add(Component.translatable("item.bioforge.swab.tooltip.clean")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.bioforge.swab.tooltip.usage")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("item.bioforge.swab.tooltip.contaminated")
                    .withStyle(ChatFormatting.RED));
        }
    }
}