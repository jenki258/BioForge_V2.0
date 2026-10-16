package net.jenkimods.bioforge.mutation;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.config.BioForgeServerConfig;
import net.jenkimods.bioforge.infection.InfectionCapability;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public final class TreatmentRejection {
    private TreatmentRejection() {}
    public static boolean has(LivingEntity entity, String mutation) {
        var data = InfectionCapability.get(entity);
        return BioForgeServerConfig.isMutationEnabled(mutation) && data != null
                && data.isInfectionActive() && data.getSymptoms().hasMutation(mutation);
    }
    /** Also called at the start of vanilla instant effects, which bypass MobEffectEvent.Applicable. */
    public static boolean blocksEffect(MobEffect effect, LivingEntity entity) {
        if (effect == MobEffects.REGENERATION) return has(entity, "regeneration_rejection");
        if (effect == MobEffects.DAMAGE_BOOST) return has(entity, "stimulant_rejection");
        boolean heals = effect == MobEffects.HEAL && !entity.isInvertedHealAndHarm()
                || effect == MobEffects.HARM && entity.isInvertedHealAndHarm();
        return heals && has(entity, "instant_healing_rejection");
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void applicable(MobEffectEvent.Applicable event) {
        if (blocksEffect(event.getEffectInstance().getEffect(), event.getEntity())) event.setResult(Event.Result.DENY);
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity.tickCount % 10 != 0) return;
        if (blocksEffect(MobEffects.REGENERATION, entity)) entity.removeEffect(MobEffects.REGENERATION);
        if (blocksEffect(MobEffects.DAMAGE_BOOST, entity)) entity.removeEffect(MobEffects.DAMAGE_BOOST);
    }
}
