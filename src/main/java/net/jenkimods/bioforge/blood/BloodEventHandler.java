package net.jenkimods.bioforge.blood;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.blood.network.BloodSyncPacket;
import net.jenkimods.bioforge.blood.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class BloodEventHandler {

    private static final int REGEN_INTERVAL_NORMAL   = 100;
    private static final int REGEN_INTERVAL_WEAKNESS = 200;
    private static final int REGEN_INTERVAL_SEVERE   = 300;
    private static final int REGEN_INTERVAL_CRITICAL = 400;
    private static final int REGEN_AMOUNT            = 1;

    private static final int EFFECT_DURATION          = 260;
    private static final int EFFECT_REAPPLY_THRESHOLD = 80;
    private static final int POTION_SAFE_THRESHOLD    = EFFECT_DURATION + 10;

    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide()) return;
        if (!living.isAlive()) return;

        BloodData data = BloodCapability.get(living);
        if (data == null) return;

        if (data.needsInit()) {
            data.clearNeedsInit();
            if (!data.isInitialized()) {
                BloodType assigned = (living instanceof Player || living.getType().is(net.minecraft.tags.TagKey.create(
                        net.minecraftforge.registries.ForgeRegistries.Keys.ENTITY_TYPES,
                        net.minecraft.resources.ResourceLocation.tryBuild(BioForge.MODID, "player_blood_give"))))
                        ? BloodType.randomHuman(RNG)
                        : BloodType.randomNonHuman(RNG);
                data.setBloodType(assigned);
                data.setBlood(BloodData.MAX_BLOOD);
            }
        }

        if (!(living instanceof ServerPlayer player)) return;

        int tick  = player.tickCount;
        BloodData.BloodPhase phase = data.getPhase();

        int regenInterval = switch (phase) {
            case NORMAL   -> REGEN_INTERVAL_NORMAL;
            case WEAKNESS -> REGEN_INTERVAL_WEAKNESS;
            case SEVERE   -> REGEN_INTERVAL_SEVERE;
            case CRITICAL -> REGEN_INTERVAL_CRITICAL;
        };

        boolean didRegen = false;
        if (tick % regenInterval == 0 && data.getBlood() < BloodData.MAX_BLOOD) {
            data.addBlood(REGEN_AMOUNT);
            didRegen = true;
        }

        if (tick % 100 != 0) return;
        applyDebuffs(player, data.getPhase());
        if (!didRegen) syncToClient(player, data);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player newPlayer = event.getEntity();
        Player oldPlayer = event.getOriginal();

        oldPlayer.reviveCaps();

        BloodData oldData = BloodCapability.get(oldPlayer);
        BloodData newData = BloodCapability.get(newPlayer);

        oldPlayer.invalidateCaps();

        if (oldData == null || newData == null) return;

        newData.setBloodType(oldData.getBloodType());
        newData.setBlood(oldData.getBlood());
    }

    private static void applyDebuffs(ServerPlayer player, BloodData.BloodPhase phase) {
        switch (phase) {
            case NORMAL   -> {}
            case WEAKNESS -> applyBloodEffect(player, MobEffects.WEAKNESS, 0);
            case SEVERE   -> {
                    applyBloodEffect(player, MobEffects.WEAKNESS, 1);
                    applyBloodEffect(player, MobEffects.MOVEMENT_SLOWDOWN, 0);
            }
            case CRITICAL -> {
                applyBloodEffect(player, MobEffects.WEAKNESS, 1);
                applyBloodEffect(player, MobEffects.MOVEMENT_SLOWDOWN, 1);
                applyBloodEffect(player, MobEffects.BLINDNESS, 0);
            }
        }
    }

    private static void applyBloodEffect(ServerPlayer player, MobEffect effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null) {
            if (current.getDuration() > POTION_SAFE_THRESHOLD) return;
            if (current.getAmplifier() >= amplifier
                    && current.getDuration() > EFFECT_REAPPLY_THRESHOLD) return;
        }
        player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, amplifier, false, false));
    }

    public static void syncToClient(ServerPlayer player, BloodData data) {
        NetworkHandler.sendToPlayer(
                new BloodSyncPacket(data.getBlood(), data.getBloodType()),
                player
        );
    }
}