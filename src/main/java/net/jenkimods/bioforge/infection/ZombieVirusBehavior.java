package net.jenkimods.bioforge.infection;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.config.BioForgeServerConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public final class ZombieVirusBehavior {
    public static final String MUTATION = "zombie_neurotropism";
    public static final String RABIES_MUTATION = "rabid_aggression";
    private static final Map<Mob, Boolean> INSTALLED = new WeakHashMap<>();
    private ZombieVirusBehavior() {}

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Mob mob)
                || !InfectionCapability.canHostInfection(mob)) return;
        if (INSTALLED.putIfAbsent(mob, Boolean.TRUE) == null) {
            mob.goalSelector.addGoal(-1, new InfectedAttackGoal(mob));
        }
    }

    public static boolean isAggressive(Mob mob) {
        InfectionData data = InfectionCapability.get(mob);
        return isRabid(mob) || (!(mob instanceof Enemy) && BioForgeServerConfig.symptomsEnabled()
                && BioForgeServerConfig.isMutationEnabled(MUTATION) && data != null
                && data.isInfectionActive() && data.getSymptoms().hasMutation(MUTATION));
    }

    public static boolean isRabid(Mob mob) {
        InfectionData data = InfectionCapability.get(mob);
        return BioForgeServerConfig.symptomsEnabled() && BioForgeServerConfig.isMutationEnabled(RABIES_MUTATION)
                && data != null && data.isInfectionActive() && data.getSymptoms().hasMutation(RABIES_MUTATION);
    }

    public static boolean canAttack(Mob mob, LivingEntity target) {
        if (target == mob || !target.isAlive() || target.isInvulnerable()
                || !InfectionCapability.canHostInfection(target)
                || target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        InfectionData data = InfectionCapability.get(target);
        return data != null && (target instanceof Player || isRabid(mob) || !data.isInfected()) && !InfectionInvulnerability.isEnabled(target);
    }

    private static final class InfectedAttackGoal extends Goal {
        private final Mob mob;
        private LivingEntity victim;
        private int searchDelay, attackDelay;
        private InfectedAttackGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP, Flag.TARGET));
        }
        @Override public boolean canUse() {
            if (!isAggressive(mob)) return false;
            if (searchDelay-- > 0) return false;
            searchDelay = 20;
            victim = mob.level().getEntitiesOfClass(LivingEntity.class,
                    mob.getBoundingBox().inflate(16), entity -> canAttack(mob, entity)
                            && mob.hasLineOfSight(entity)).stream()
                    .min(Comparator.comparingInt((LivingEntity entity) -> !isRabid(mob) && entity instanceof Player ? 0 : 1)
                            .thenComparingDouble(mob::distanceToSqr)).orElse(null);
            return victim != null;
        }
        @Override public boolean canContinueToUse() {
            return isAggressive(mob) && victim != null && canAttack(mob, victim)
                    && mob.distanceToSqr(victim) <= 24 * 24;
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            mob.setTarget(victim);
            mob.getLookControl().setLookAt(victim, 30, 30);
            if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(victim, 1.15);
            if (attackDelay > 0) attackDelay--;
            double reach = Math.max(1.4, (mob.getBbWidth() + victim.getBbWidth()) * 0.5 + 0.6);
            if (attackDelay == 0 && mob.distanceToSqr(victim) <= reach * reach && mob.hasLineOfSight(victim)) {
                attackDelay = 20;
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                // Passive mobs commonly have no ATTACK_DAMAGE attribute: doHurtTarget would dereference null.
                if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) mob.doHurtTarget(victim);
                else victim.hurt(mob.damageSources().mobAttack(mob), 2.0F);
            }
        }
        @Override public void stop() {
            if (mob.getTarget() == victim) mob.setTarget(null);
            mob.getNavigation().stop();
            victim = null;
        }
    }
}
