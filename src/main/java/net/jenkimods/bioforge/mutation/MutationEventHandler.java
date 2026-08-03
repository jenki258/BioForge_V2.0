package net.jenkimods.bioforge.mutation;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.InfectionCapability;
import net.jenkimods.bioforge.infection.InfectionData;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;






@Mod.EventBusSubscriber(modid = BioForge.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MutationEventHandler {
    private MutationEventHandler() {}

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        InfectionData data = InfectionCapability.get(event.getEntity());
        if (data == null || !data.isInfected() || data.getSymptoms().getMutations().isEmpty()) return;
        MutationManager.tickMutations(data, event.getEntity());
    }
}
