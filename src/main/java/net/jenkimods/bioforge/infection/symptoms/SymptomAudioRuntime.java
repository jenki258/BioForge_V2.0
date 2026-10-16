package net.jenkimods.bioforge.infection.symptoms;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.config.BioForgeServerConfig;
import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.registry.BioForgeSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=BioForge.MODID)
public final class SymptomAudioRuntime {
    public static boolean pulseEligible(InfectionData data) {
        return data!=null && data.isInfectionActive() && BioForgeServerConfig.isSymptomEnabled("heart_rate")
                && data.getSymptom(BioForgeSymptoms.HEART_RATE)==HeartRate.TACHY;
    }
    public static boolean scratchingEligible(InfectionData data) {
        return data!=null && data.isInfectionActive() && data.getPathogenType()==PathogenType.PARASITE
                && BioForgeServerConfig.isSymptomEnabled("neural_damage")
                && data.getSymptom(BioForgeSymptoms.NEURAL_DAMAGE)>0;
    }
    @SubscribeEvent public static void onTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !BioForgeServerConfig.symptomsEnabled()
                || !BioForgeServerConfig.immersiveAudioEnabled()
                || (player.tickCount+player.getId())%200!=0) return;
        var data=InfectionCapability.get(player);
        if (pulseEligible(data) && !SymptomSuppression.isSuppressed(player,"heart_rate")
                && player.getRandom().nextFloat()<BioForgeServerConfig.earPulseChance())
            player.playNotifySound(BioForgeSounds.EAR_PULSE.get(),SoundSource.PLAYERS,.45F,1F);
        else if (scratchingEligible(data) && !SymptomSuppression.isSuppressed(player,"neural_damage")
                && player.getRandom().nextFloat()<BioForgeServerConfig.earScratchingChance())
            player.playNotifySound(BioForgeSounds.EAR_SCRATCHING.get(),SoundSource.PLAYERS,.32F,.95F+player.getRandom().nextFloat()*.1F);
    }
}
