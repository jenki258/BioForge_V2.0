package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.BioForgeTags;
import net.jenkimods.bioforge.infection.network.InfectionNetworkHandler;
import net.jenkimods.bioforge.infection.network.InfectionSyncPacket;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.jenkimods.bioforge.infection.symptoms.SymptomKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

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
        if (!attackerData.getInfectionTypes().contains(InfectionType.ATTACK_BASED)) return;
        PathogenType pathogen = attackerData.getPathogenType();
        if (pathogen == null || !pathogen.allows(InfectionType.ATTACK_BASED)) return;
        InfectionData targetData = InfectionCapability.get(target);
        if (targetData == null || targetData.isInfected()) return;
        targetData.setInfected(true);
        targetData.setPathogenType(pathogen);
        targetData.addInfectionType(InfectionType.ATTACK_BASED);
        BioForgeSymptoms.applyDefaultSymptoms(targetData);
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
            for (InfectionType t : record.infectionTypes()) newData.addInfectionType(t);
            for (Map.Entry<String, Object> entry : record.symptoms().entrySet()) {
                SymptomKey<?> key = BioForgeSymptoms.getAllSymptomKeys().get(entry.getKey());
                if (key != null) {
                    newData.getSymptoms().set((SymptomKey) key, entry.getValue());
                }
            }
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

    public static void syncToClient(ServerPlayer player, InfectionData data) {
        InfectionNetworkHandler.sendToPlayer(InfectionSyncPacket.fromData(data), player);
    }
}