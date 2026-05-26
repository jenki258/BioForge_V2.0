package net.jenkimods.bioforge.event;

import net.jenkimods.bioforge.block.InfestedBlock;
import net.jenkimods.bioforge.block.MicrobialMatBlock;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "bioforge", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MatSlownessHandler {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        BlockState state = entity.getBlockStateOn();
        if (state.getBlock() instanceof MicrobialMatBlock || state.getBlock() instanceof InfestedBlock) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0, false, false, false));
        }
    }
}