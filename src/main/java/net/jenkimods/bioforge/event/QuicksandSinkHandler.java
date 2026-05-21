package net.jenkimods.bioforge.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class QuicksandSinkHandler {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        BlockState state = entity.getBlockStateOn(); // state at entity's feet
        if (state.getBlock() instanceof InfestedBlock) {
            // Strong slow (applied each tick while standing)
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4, false, false, false));

            // Sink downward: apply a small constant downward motion
            if (!entity.onGround()) return; // only sink if on ground
            entity.setDeltaMovement(entity.getDeltaMovement().x, -0.08D, entity.getDeltaMovement().z);
            entity.hurtMarked = true; // mark velocity dirty
        }
    }
}