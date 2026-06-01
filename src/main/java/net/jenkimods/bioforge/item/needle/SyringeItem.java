package net.jenkimods.bioforge.item.needle;

import net.jenkimods.bioforge.blood.BloodCapability;
import net.jenkimods.bioforge.blood.BloodData;
import net.jenkimods.bioforge.blood.BloodType;
import net.jenkimods.bioforge.blood.knowledge.BloodKnowledge;
import net.jenkimods.bioforge.blood.knowledge.BloodKnowledgeStore;
import net.jenkimods.bioforge.item.BloodSampleUtil;
import net.jenkimods.bioforge.util.NbtObfuscator.ObfuscatedData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SyringeItem extends Item {

    private static final int BLOOD_DRAIN = 10;
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
        if (BloodSampleUtil.hasBlood(stack)) return InteractionResultHolder.pass(stack);

        BloodData selfData = BloodCapability.get(sp);
        if (selfData == null || selfData.getBlood() <= 0) return InteractionResultHolder.fail(stack);

        int newBlood = Math.max(0, selfData.getBlood() - BLOOD_DRAIN);
        selfData.setBlood(newBlood);
        if (newBlood > 0) {
            storeBlood(stack, selfData.getBlood(), selfData.getBloodType(),
                    sp.getName().getString(), sp.getUUID());
        }
        return InteractionResultHolder.success(stack);
    }

    public boolean tryExtractBlood(ItemStack stack, LivingEntity target, ServerPlayer attacker) {
        if (BloodSampleUtil.hasBlood(stack)) return false;
        if (!NeedleItem.entityHasBlood(target)) return false;

        BloodData targetData = BloodCapability.get(target);
        if (targetData == null || targetData.getBlood() <= 0) return false;

        int newBlood = Math.max(0, targetData.getBlood() - BLOOD_DRAIN);
        targetData.setBlood(newBlood);
        if (newBlood > 0) {
            storeBlood(stack, targetData.getBlood(), targetData.getBloodType(),
                    target.getName().getString(), target.getUUID());
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

    public static boolean hasBlood(ItemStack stack) {
        return BloodSampleUtil.hasBlood(stack);
    }

    public static int getUses(ItemStack stack) {
        ObfuscatedData data = BloodSampleUtil.getData(stack);
        return data != null ? data.amount() : 0;
    }

    @Nullable
    public static BloodType getBloodType(ItemStack stack) {
        ObfuscatedData data = BloodSampleUtil.getData(stack);
        return data != null ? BloodType.fromName(data.typeName()) : null;
    }

    @Nullable
    public static String getSourceName(ItemStack stack) {
        ObfuscatedData data = BloodSampleUtil.getData(stack);
        return data != null ? data.sourceName() : null;
    }

    @Nullable
    public static UUID getSubjectUUID(ItemStack stack) {
        ObfuscatedData data = BloodSampleUtil.getData(stack);
        return data != null ? data.subjectUUID() : null;
    }

    private static void storeBlood(ItemStack stack, int amount, BloodType type, String sourceName, UUID subjectUUID) {
        BloodSampleUtil.setData(stack, MAX_USES, type, sourceName, subjectUUID);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!hasBlood(stack)) {
            tooltip.add(Component.translatable("item.bioforge.syringe.empty").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.bioforge.syringe.tooltip.use_self").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("item.bioforge.syringe.tooltip.use_other").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        ObfuscatedData data = BloodSampleUtil.getData(stack);
        if (data == null) return;
        tooltip.add(Component.translatable("item.bioforge.syringe.filled").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.bioforge.syringe.source", data.sourceName()).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.bioforge.syringe.uses_left", data.amount()).withStyle(ChatFormatting.GOLD));
        appendKnowledgeLines(data, tooltip);
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