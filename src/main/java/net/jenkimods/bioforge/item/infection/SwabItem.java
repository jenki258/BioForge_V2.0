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

import java.util.List;
import java.util.UUID;

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
            if (data == null || !data.isInfected()) {
                player.sendSystemMessage(Component.translatable("item.bioforge.swab.not_infected"));
                return InteractionResultHolder.fail(stack);
            }
            String payload = buildStrainPayload(data);
            NbtObfuscator.writeString(stack.getOrCreateTag(), payload);
            player.setItemInHand(hand, stack);
            player.sendSystemMessage(Component.translatable("item.bioforge.swab.collected_self"));
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
        if (data == null || !data.isInfected()) {
            player.sendSystemMessage(Component.translatable("item.bioforge.swab.target_not_infected"));
            return InteractionResult.FAIL;
        }

        String payload = buildStrainPayload(data);
        NbtObfuscator.writeString(stack.getOrCreateTag(), payload);
        player.setItemInHand(hand, stack);
        player.sendSystemMessage(Component.translatable("item.bioforge.swab.collected",
                target.getDisplayName()));
        return InteractionResult.CONSUME;
    }

    private String buildStrainPayload(@Nullable InfectionData data) {
        if (data == null || !data.isInfected()) return "CLEAN";
        String colonyId = UUID.randomUUID().toString();
        StringBuilder sb = new StringBuilder();
        sb.append(colonyId).append("|").append(data.getPathogenType().name())
                .append("|").append(data.getInfectionType().name());
        sb.append(";").append("HeartRate=").append(data.getSymptom(BioForgeSymptoms.HEART_RATE).name());
        sb.append(";").append("LungSound=").append(data.getSymptom(BioForgeSymptoms.LUNG_SOUND).name());
        sb.append(";").append("TempPlus=").append(data.getSymptom(BioForgeSymptoms.TEMPERATURE_PLUS));
        sb.append(";").append("TempMinus=").append(data.getSymptom(BioForgeSymptoms.TEMPERATURE_MINUS));
        sb.append(";").append("Redness=").append(data.getSymptom(BioForgeSymptoms.OTOSCOPE_REDNESS));
        sb.append(";").append("Lesions=").append(data.getSymptom(BioForgeSymptoms.OTOSCOPE_LESIONS));
        sb.append(";").append("Secretion=").append(data.getSymptom(BioForgeSymptoms.OTOSCOPE_SECRETION));
        sb.append(";").append("Swelling=").append(data.getSymptom(BioForgeSymptoms.OTOSCOPE_SWELLING));
        sb.append(";").append("ReflexDelay=").append(data.getSymptom(BioForgeSymptoms.REFLEX_DELAY));
        sb.append(";").append("ReflexStrength=").append(data.getSymptom(BioForgeSymptoms.REFLEX_STRENGTH));
        sb.append(";").append("NeuralDamage=").append(data.getSymptom(BioForgeSymptoms.NEURAL_DAMAGE));
        sb.append(";").append("OxygenSaturation=").append(data.getSymptom(BioForgeSymptoms.OXYGEN_SATURATION));
        sb.append(";").append("PerfusionIndex=").append(data.getSymptom(BioForgeSymptoms.PERFUSION_INDEX));
        sb.append(";").append("InfectionStrength=").append(data.getSymptom(BioForgeSymptoms.INFECTION_STRENGTH));
        sb.append(";").append("ColonyRadius=").append(data.getSymptom(BioForgeSymptoms.COLONY_RADIUS));
        sb.append(";").append("MaxInfestedBlocks=").append(data.getSymptom(BioForgeSymptoms.MAX_INFESTED_BLOCKS));
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