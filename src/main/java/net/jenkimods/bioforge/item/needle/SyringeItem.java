package net.jenkimods.bioforge.item.needle;

import net.jenkimods.bioforge.blood.*;
import net.jenkimods.bioforge.blood.knowledge.BloodKnowledge;
import net.jenkimods.bioforge.blood.knowledge.BloodKnowledgeStore;
import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.item.BloodSampleUtil;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.jenkimods.bioforge.util.NbtObfuscator.ObfuscatedData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SyringeItem extends Item {

    private static final int BLOOD_DRAIN = 10;
    private static final int BLOOD_TRANSFER = 2;
    private static final int MAX_USES = 4;

    public SyringeItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        EquipmentSlot oppositeSlot = (hand == InteractionHand.MAIN_HAND) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        if (!player.getItemBySlot(oppositeSlot).isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide()) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        if (BloodSampleUtil.hasBlood(stack)) {
            BloodData selfData = BloodCapability.get(sp);
            if (selfData == null) return InteractionResultHolder.fail(stack);

            selfData.addBlood(BLOOD_TRANSFER);
            applyStoredInfection(stack, player);
            consumeUse(stack);

            level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8f, 1.2f);
            return InteractionResultHolder.success(stack);
        }

        BloodData selfData = BloodCapability.get(sp);
        if (selfData == null || selfData.getBlood() <= 0) return InteractionResultHolder.fail(stack);
        applyStoredInfection(stack, player);

        int newBlood = Math.max(0, selfData.getBlood() - BLOOD_DRAIN);
        selfData.setBlood(newBlood);
        if (newBlood > 0) {
            storeBlood(stack, selfData.getBlood(), selfData.getBloodType(),
                    sp.getName().getString(), sp.getUUID());
            captureInfection(stack, sp);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.FAIL;

        if (!BloodSampleUtil.hasBlood(stack)) return InteractionResult.PASS;

        BloodData targetData = BloodCapability.get(target);
        if (targetData == null) return InteractionResult.FAIL;

        targetData.addBlood(BLOOD_TRANSFER);
        applyStoredInfection(stack, target);
        consumeUse(stack);
        player.setItemInHand(hand, stack);

        level.playSound(null, target.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8f, 1.2f);
        return InteractionResult.SUCCESS;
    }

    public boolean tryExtractBlood(ItemStack stack, LivingEntity target, ServerPlayer attacker) {
        if (BloodSampleUtil.hasBlood(stack)) return false;
        if (!NeedleItem.entityHasBlood(target)) return false;

        BloodData targetData = BloodCapability.get(target);
        if (targetData == null || targetData.getBlood() <= 0) return false;
        applyStoredInfection(stack, target);
        int newBlood = Math.max(0, targetData.getBlood() - BLOOD_DRAIN);
        targetData.setBlood(newBlood);
        if (newBlood > 0) {
            storeBlood(stack, targetData.getBlood(), targetData.getBloodType(),
                    target.getName().getString(), target.getUUID());
            captureInfection(stack, target);
            return true;
        }
        return false;
    }

    public static void consumeUse(ItemStack stack) {
        ObfuscatedData data = BloodSampleUtil.getData(stack);
        if (data == null) return;
        int uses = data.amount() - 1;
        if (uses <= 0) {
            BloodSampleUtil.clear(stack);
        } else {
            BloodType type = BloodType.fromName(data.typeName());
            BloodSampleUtil.setData(stack, uses, type, data.sourceName(), data.subjectUUID());
        }
    }

    private static void storeBlood(ItemStack stack, int amount, BloodType type, String sourceName, UUID subjectUUID) {
        BloodSampleUtil.setData(stack, MAX_USES, type, sourceName, subjectUUID);
    }

    private static void captureInfection(ItemStack stack, LivingEntity entity) {
        InfectionData inf = InfectionCapability.get(entity);
        if (inf == null || !inf.isInfected()) return;
        Set<InfectionType> types = inf.getInfectionTypes();
        if (!types.contains(InfectionType.BLOOD)) return;
        String strain = buildStrainPayload(inf);
        NbtObfuscator.writeInfection(stack.getOrCreateTag(), strain);
    }

    private static String buildStrainPayload(InfectionData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("PLACEHOLDER|").append(data.getPathogenType().name()).append("|");
        Iterator<InfectionType> iter = data.getInfectionTypes().iterator();
        while (iter.hasNext()) {
            sb.append(iter.next().name());
            if (iter.hasNext()) sb.append(",");
        }
        sb.append(";");
        appendSymptom(sb, "HeartRate", data.getSymptom(BioForgeSymptoms.HEART_RATE).name());
        appendSymptom(sb, "LungSound", data.getSymptom(BioForgeSymptoms.LUNG_SOUND).name());
        appendSymptom(sb, "TempPlus", String.valueOf(data.getSymptom(BioForgeSymptoms.TEMPERATURE_PLUS)));
        appendSymptom(sb, "TempMinus", String.valueOf(data.getSymptom(BioForgeSymptoms.TEMPERATURE_MINUS)));
        appendSymptom(sb, "Redness", String.valueOf(data.getSymptom(BioForgeSymptoms.OTOSCOPE_REDNESS)));
        appendSymptom(sb, "Lesions", String.valueOf(data.getSymptom(BioForgeSymptoms.OTOSCOPE_LESIONS)));
        appendSymptom(sb, "Secretion", String.valueOf(data.getSymptom(BioForgeSymptoms.OTOSCOPE_SECRETION)));
        appendSymptom(sb, "Swelling", String.valueOf(data.getSymptom(BioForgeSymptoms.OTOSCOPE_SWELLING)));
        appendSymptom(sb, "ReflexDelay", String.valueOf(data.getSymptom(BioForgeSymptoms.REFLEX_DELAY)));
        appendSymptom(sb, "ReflexStrength", String.valueOf(data.getSymptom(BioForgeSymptoms.REFLEX_STRENGTH)));
        appendSymptom(sb, "NeuralDamage", String.valueOf(data.getSymptom(BioForgeSymptoms.NEURAL_DAMAGE)));
        appendSymptom(sb, "OxygenSaturation", String.valueOf(data.getSymptom(BioForgeSymptoms.OXYGEN_SATURATION)));
        appendSymptom(sb, "PerfusionIndex", String.valueOf(data.getSymptom(BioForgeSymptoms.PERFUSION_INDEX)));
        appendSymptom(sb, "InfectionStrength", String.valueOf(data.getSymptom(BioForgeSymptoms.INFECTION_STRENGTH)));
        appendSymptom(sb, "ColonyRadius", String.valueOf(data.getSymptom(BioForgeSymptoms.COLONY_RADIUS)));
        appendSymptom(sb, "MaxInfestedBlocks", String.valueOf(data.getSymptom(BioForgeSymptoms.MAX_INFESTED_BLOCKS)));
        return sb.toString();
    }

    private static void appendSymptom(StringBuilder sb, String key, String value) {
        sb.append(key).append("=").append(value).append(";");
    }

    private static void applyStoredInfection(ItemStack stack, LivingEntity target) {
        String strain = NbtObfuscator.readInfection(stack.getOrCreateTag());
        if (strain == null || strain.isEmpty()) return;
        InfectionData data = InfectionCapability.get(target);
        if (data == null || data.isInfected()) return;
        String[] parts = strain.split(";");
        if (parts.length == 0) return;
        String[] header = parts[0].split("\\|");
        if (header.length < 2) return;
        PathogenType pt = PathogenType.fromName(header[1]);
        if (pt == null) return;
        data.setInfected(true);
        data.setPathogenType(pt);
        String typesRaw = header.length >= 3 ? header[2] : header[1];
        for (String t : typesRaw.split(",")) {
            InfectionType it = InfectionType.fromName(t.trim());
            if (it != null) data.addInfectionType(it);
        }
        for (int i = 1; i < parts.length; i++) {
            String[] kv = parts[i].split("=", 2);
            if (kv.length == 2) {
                switch (kv[0]) {
                    case "HeartRate" -> data.setSymptom(BioForgeSymptoms.HEART_RATE, HeartRate.fromName(kv[1]));
                    case "LungSound" -> data.setSymptom(BioForgeSymptoms.LUNG_SOUND, LungSound.fromName(kv[1]));
                    case "TempPlus" -> data.setSymptom(BioForgeSymptoms.TEMPERATURE_PLUS, Boolean.parseBoolean(kv[1]));
                    case "TempMinus" -> data.setSymptom(BioForgeSymptoms.TEMPERATURE_MINUS, Boolean.parseBoolean(kv[1]));
                    case "Redness" -> data.setSymptom(BioForgeSymptoms.OTOSCOPE_REDNESS, Float.parseFloat(kv[1]));
                    case "Lesions" -> data.setSymptom(BioForgeSymptoms.OTOSCOPE_LESIONS, Float.parseFloat(kv[1]));
                    case "Secretion" -> data.setSymptom(BioForgeSymptoms.OTOSCOPE_SECRETION, Float.parseFloat(kv[1]));
                    case "Swelling" -> data.setSymptom(BioForgeSymptoms.OTOSCOPE_SWELLING, Float.parseFloat(kv[1]));
                    case "ReflexDelay" -> data.setSymptom(BioForgeSymptoms.REFLEX_DELAY, Float.parseFloat(kv[1]));
                    case "ReflexStrength" -> data.setSymptom(BioForgeSymptoms.REFLEX_STRENGTH, Float.parseFloat(kv[1]));
                    case "NeuralDamage" -> data.setSymptom(BioForgeSymptoms.NEURAL_DAMAGE, Float.parseFloat(kv[1]));
                    case "OxygenSaturation" -> data.setSymptom(BioForgeSymptoms.OXYGEN_SATURATION, Float.parseFloat(kv[1]));
                    case "PerfusionIndex" -> data.setSymptom(BioForgeSymptoms.PERFUSION_INDEX, Float.parseFloat(kv[1]));
                    case "InfectionStrength" -> data.setSymptom(BioForgeSymptoms.INFECTION_STRENGTH, Float.parseFloat(kv[1]));
                    case "ColonyRadius" -> data.setSymptom(BioForgeSymptoms.COLONY_RADIUS, Float.parseFloat(kv[1]));
                    case "MaxInfestedBlocks" -> data.setSymptom(BioForgeSymptoms.MAX_INFESTED_BLOCKS, Float.parseFloat(kv[1]));
                }
            }
        }
        if (target instanceof ServerPlayer sp) {
            InfectionEventHandler.syncToClient(sp, data);
        }
    }

    public static void clearInfection(ItemStack stack) {
        NbtObfuscator.clearInfection(stack.getOrCreateTag());
    }

    public static boolean hasBlood(ItemStack stack) { return BloodSampleUtil.hasBlood(stack); }
    public static int getUses(ItemStack stack) {
        ObfuscatedData data = BloodSampleUtil.getData(stack);
        return data != null ? data.amount() : 0;
    }

    @Nullable public static BloodType getBloodType(ItemStack stack) {
        ObfuscatedData data = BloodSampleUtil.getData(stack);
        return data != null ? BloodType.fromName(data.typeName()) : null;
    }
    @Nullable public static String getSourceName(ItemStack stack) {
        ObfuscatedData data = BloodSampleUtil.getData(stack);
        return data != null ? data.sourceName() : null;
    }
    @Nullable public static UUID getSubjectUUID(ItemStack stack) {
        ObfuscatedData data = BloodSampleUtil.getData(stack);
        return data != null ? data.subjectUUID() : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!hasBlood(stack)) {
            tooltip.add(Component.translatable("item.bioforge.syringe.empty").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal(" "));
            tooltip.add(Component.translatable("item.bioforge.syringe.tooltip.use_self").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("item.bioforge.syringe.tooltip.use_other").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("item.bioforge.syringe.tooltip.warning_blood").withStyle(ChatFormatting.DARK_RED));
            return;
        }

        ObfuscatedData data = BloodSampleUtil.getData(stack);
        if (data == null) return;
        tooltip.add(Component.translatable("item.bioforge.syringe.filled").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.bioforge.syringe.source", data.sourceName()).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.bioforge.syringe.uses_left", data.amount()).withStyle(ChatFormatting.GOLD));
        appendKnowledgeLines(data, tooltip);
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.translatable("item.bioforge.syringe.tooltip.use_self").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.syringe.tooltip.use_other").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bioforge.syringe.tooltip.warning_blood").withStyle(ChatFormatting.DARK_RED));
    }

    private static void appendKnowledgeLines(ObfuscatedData data, List<Component> tooltip) {
        if (data.subjectUUID() == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return;
        BloodKnowledgeStore store = BloodKnowledgeStore.get(server);
        Optional<BloodKnowledge> knowledge = store.find(mc.player.getUUID(), data.subjectUUID());
        if (knowledge.isEmpty()) return;
        BloodKnowledge k = knowledge.get();
        if (k.getAntiA() != null && k.getAntiB() != null && k.getAntiD() != null) {
            BloodType type = BloodType.fromName(data.typeName());
            tooltip.add(Component.translatable("item.bioforge.syringe.blood_type",
                    type.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
        }
        tooltip.add(Component.translatable("item.bioforge.syringe.reactions").withStyle(ChatFormatting.DARK_GREEN));
        if (k.getAntiA() != null) {
            tooltip.add(Component.translatable("item.bioforge.syringe.anti_a",
                    k.getAntiA() ? "+" : "-").withStyle(k.getAntiA() ? ChatFormatting.RED : ChatFormatting.GREEN));
        }
        if (k.getAntiB() != null) {
            tooltip.add(Component.translatable("item.bioforge.syringe.anti_b",
                    k.getAntiB() ? "+" : "-").withStyle(k.getAntiB() ? ChatFormatting.RED : ChatFormatting.GREEN));
        }
        if (k.getAntiD() != null) {
            tooltip.add(Component.translatable("item.bioforge.syringe.anti_d",
                    k.getAntiD() ? "+" : "-").withStyle(k.getAntiD() ? ChatFormatting.RED : ChatFormatting.GREEN));
        }
    }
}