package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.BioForgeTags;
import net.jenkimods.bioforge.infection.network.InfectionNetworkHandler;
import net.jenkimods.bioforge.infection.network.InfectionSyncPacket;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class InfectionEventHandler {

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        LivingEntity target = event.getEntity();
        if (target.getType().is(BioForgeTags.NO_INFECTION_ATTACK_SPREAD)) return;
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity le ? le : null;
        if (attacker == null) return;
        InfectionData attackerData = InfectionCapability.get(attacker);
        if (attackerData == null || !attackerData.isInfected()) return;
        if (attackerData.getInfectionType() != InfectionType.ATTACK_BASED) return;
        if (attackerData.getPathogenType() == null) return;
        if (!attackerData.getPathogenType().allows(InfectionType.ATTACK_BASED)) return;
        InfectionData targetData = InfectionCapability.get(target);
        if (targetData == null || targetData.isInfected()) return;
        targetData.setInfected(true);
        targetData.setPathogenType(attackerData.getPathogenType());
        targetData.setInfectionType(InfectionType.ATTACK_BASED);
        applyDefaultSymptoms(targetData);
        if (target instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer, targetData);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player newPlayer = event.getEntity();
        Player oldPlayer = event.getOriginal();
        oldPlayer.reviveCaps();
        InfectionData oldData = InfectionCapability.get(oldPlayer);
        InfectionData newData = InfectionCapability.get(newPlayer);
        oldPlayer.invalidateCaps();
        if (oldData == null || newData == null) return;
        ServerPlayer serverPlayer = (ServerPlayer) newPlayer;
        InfectionStore store = InfectionStore.get(serverPlayer.serverLevel());
        InfectionStore.InfectionRecord record = store.getInfection(serverPlayer.getUUID());
        if (record != null && record.persistent()) {
            newData.setInfected(true);
            newData.setPathogenType(record.pathogenType());
            newData.setInfectionType(record.infectionType());
            newData.setSymptom(BioForgeSymptoms.HEART_RATE, record.heartRate());
            newData.setSymptom(BioForgeSymptoms.LUNG_SOUND, record.lungSound());
            newData.setSymptom(BioForgeSymptoms.TEMPERATURE_PLUS, record.temperaturePlus());
            newData.setSymptom(BioForgeSymptoms.TEMPERATURE_MINUS, record.temperatureMinus());
            newData.setSymptom(BioForgeSymptoms.OTOSCOPE_REDNESS, record.redness());
            newData.setSymptom(BioForgeSymptoms.OTOSCOPE_LESIONS, record.lesions());
            newData.setSymptom(BioForgeSymptoms.OTOSCOPE_SECRETION, record.secretion());
            newData.setSymptom(BioForgeSymptoms.OTOSCOPE_SWELLING, record.swelling());
            newData.setSymptom(BioForgeSymptoms.REFLEX_DELAY, record.reflexDelay());
            newData.setSymptom(BioForgeSymptoms.REFLEX_STRENGTH, record.reflexStrength());
            newData.setSymptom(BioForgeSymptoms.NEURAL_DAMAGE, record.neuralDamage());
            newData.setSymptom(BioForgeSymptoms.COLONY_RADIUS, record.colonyRadius());
            newData.setSymptom(BioForgeSymptoms.MAX_INFESTED_BLOCKS, record.maxInfestedBlocks());
        } else {
            newData.clearInfection();
            if (record != null) store.clearInfection(serverPlayer.getUUID());
        }
        syncToClient(serverPlayer, newData);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        InfectionData data = InfectionCapability.get(player);
        if (data == null) return;
        syncToClient(player, data);
    }

    public static void applyDefaultSymptoms(InfectionData data) {
        PathogenType pathogen = data.getPathogenType();
        if (pathogen == null) pathogen = PathogenType.UNIVERSAL;
        java.util.Random rand = new java.util.Random();

        float redness, lesions, secretion, swelling;
        switch (pathogen) {
            case BACTERIA:
                redness   = 0.7f + rand.nextFloat() * 0.3f;
                secretion = 0.6f + rand.nextFloat() * 0.4f;
                lesions   = 0.3f + rand.nextFloat() * 0.3f;
                swelling  = 0.2f + rand.nextFloat() * 0.2f;
                break;
            case FUNGI:
                redness   = 0.1f + rand.nextFloat() * 0.2f;
                secretion = 0.3f + rand.nextFloat() * 0.3f;
                lesions   = 0.7f + rand.nextFloat() * 0.3f;
                swelling  = 0.0f + rand.nextFloat() * 0.1f;
                break;
            case VIRUS:
                redness   = 0.3f + rand.nextFloat() * 0.3f;
                secretion = 0.1f + rand.nextFloat() * 0.2f;
                lesions   = 0.6f + rand.nextFloat() * 0.4f;
                swelling  = 0.3f + rand.nextFloat() * 0.3f;
                break;
            case PRION:
                redness   = 0.0f;
                secretion = 0.0f;
                lesions   = 0.0f;
                swelling  = 0.0f;
                break;
            default:
                redness   = rand.nextFloat();
                lesions   = rand.nextFloat();
                secretion = rand.nextFloat();
                swelling  = rand.nextFloat();
                break;
        }
        data.setSymptom(BioForgeSymptoms.OTOSCOPE_REDNESS,   Math.min(1.0f, redness));
        data.setSymptom(BioForgeSymptoms.OTOSCOPE_LESIONS,   Math.min(1.0f, lesions));
        data.setSymptom(BioForgeSymptoms.OTOSCOPE_SECRETION, Math.min(1.0f, secretion));
        data.setSymptom(BioForgeSymptoms.OTOSCOPE_SWELLING,  Math.min(1.0f, swelling));


        float oxygen, perfusion;
        switch (pathogen) {
            case BACTERIA:
                oxygen   = 0.75f + rand.nextFloat() * 0.15f;
                perfusion = 0.5f + rand.nextFloat() * 0.3f;
                break;
            case FUNGI:
                oxygen   = 0.85f + rand.nextFloat() * 0.1f;
                perfusion = 0.4f + rand.nextFloat() * 0.4f;
                break;
            case VIRUS:
                oxygen   = 0.8f + rand.nextFloat() * 0.15f;
                perfusion = 0.3f + rand.nextFloat() * 0.3f;
                break;
            case PRION:
                oxygen   = 0.95f + rand.nextFloat() * 0.05f;
                perfusion = 0.1f + rand.nextFloat() * 0.2f;
                break;
            case PARASITE:
                oxygen   = 0.6f + rand.nextFloat() * 0.2f;
                perfusion = 0.4f + rand.nextFloat() * 0.3f;
                break;
            default:
                oxygen   = rand.nextFloat() * 0.3f + 0.7f;
                perfusion = rand.nextFloat();
                break;
        }
        data.setSymptom(BioForgeSymptoms.OXYGEN_SATURATION, Math.min(1.0f, oxygen));
        data.setSymptom(BioForgeSymptoms.PERFUSION_INDEX,   Math.min(1.0f, perfusion));


        float reflexDelay = 0.1f, reflexStrength = 0.8f, neuralDamage = 0.0f;
        switch (pathogen) {
            case VIRUS:
                reflexDelay = 0.08f + rand.nextFloat() * 0.1f;
                reflexStrength = 0.9f;
                neuralDamage = 0.0f;
                break;
            case PRION:
                reflexDelay = 0.5f + rand.nextFloat() * 0.3f;
                reflexStrength = 0.1f;
                neuralDamage = 0.9f;
                break;
            case PARASITE:
                reflexDelay = 0.2f + rand.nextFloat() * 0.3f;
                reflexStrength = 0.5f;
                neuralDamage = 0.3f;
                break;
            case BACTERIA:
                reflexDelay = 0.15f + rand.nextFloat() * 0.2f;
                reflexStrength = 0.7f;
                neuralDamage = 0.1f;
                break;
            case FUNGI:
                reflexDelay = 0.25f + rand.nextFloat() * 0.2f;
                reflexStrength = 0.3f;
                neuralDamage = 0.4f;
                break;
            default:
                reflexDelay = rand.nextFloat() * 0.3f;
                reflexStrength = rand.nextFloat();
                neuralDamage = rand.nextFloat() * 0.5f;
                break;
        }

        float infectionStrength;
        switch (pathogen) {
            case BACTERIA -> infectionStrength = 0.4f + rand.nextFloat() * 0.3f;
            case VIRUS    -> infectionStrength = 0.3f + rand.nextFloat() * 0.5f;
            case FUNGI    -> infectionStrength = 0.5f + rand.nextFloat() * 0.4f;
            case PARASITE -> infectionStrength = 0.6f + rand.nextFloat() * 0.3f;
            case PRION    -> infectionStrength = 0.1f + rand.nextFloat() * 0.2f;
            default       -> infectionStrength = 0.5f;
        }


        float radius = switch (pathogen) {
            case FUNGI -> 25.0f;
            case VIRUS -> 30.0f;
            default -> 20.0f;
        };
        float maxBlocks = switch (pathogen) {
            case FUNGI -> 150.0f;
            case VIRUS -> 120.0f;
            case BACTERIA -> 80.0f;
            default -> 100.0f;
        };

        data.setSymptom(BioForgeSymptoms.COLONY_RADIUS, radius);
        data.setSymptom(BioForgeSymptoms.MAX_INFESTED_BLOCKS, maxBlocks);
        data.setSymptom(BioForgeSymptoms.INFECTION_STRENGTH, Math.min(1.0f, infectionStrength));
        data.setSymptom(BioForgeSymptoms.REFLEX_DELAY, reflexDelay);
        data.setSymptom(BioForgeSymptoms.REFLEX_STRENGTH, reflexStrength);
        data.setSymptom(BioForgeSymptoms.NEURAL_DAMAGE, neuralDamage);

        data.setSymptom(BioForgeSymptoms.HEART_RATE,       HeartRate.TACHY);
        data.setSymptom(BioForgeSymptoms.LUNG_SOUND,       LungSound.CRACKLE);
        data.setSymptom(BioForgeSymptoms.TEMPERATURE_PLUS, true);
        data.setSymptom(BioForgeSymptoms.TEMPERATURE_MINUS, false);
    }

    public static void syncToClient(ServerPlayer player, InfectionData data) {
        InfectionNetworkHandler.sendToPlayer(InfectionSyncPacket.fromData(data), player);
    }
}