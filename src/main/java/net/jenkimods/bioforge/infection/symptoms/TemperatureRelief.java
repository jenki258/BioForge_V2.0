package net.jenkimods.bioforge.infection.symptoms;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class TemperatureRelief {
    private TemperatureRelief() {}

    public static boolean isCooling(LivingEntity entity) {
        return entity.isInWaterOrBubble()
                || entity.level().getBlockState(entity.getOnPos()).is(BlockTags.ICE);
    }

    public static boolean isWarming(LivingEntity entity) {
        if (entity.isOnFire() || entity.isInLava()) return true;
        BlockPos origin = entity.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -3, -3), origin.offset(3, 3, 3))) {
            if (!entity.level().hasChunkAt(pos) || !pos.closerToCenterThan(entity.position(), 3.5D)) continue;
            BlockState state = entity.level().getBlockState(pos);
            if (state.is(BlockTags.FIRE) || state.getFluidState().is(FluidTags.LAVA)
                    || state.is(Blocks.LAVA_CAULDRON) || CampfireBlock.isLitCampfire(state)
                    || (state.getBlock() instanceof AbstractFurnaceBlock
                    && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT))) return true;
        }
        return false;
    }

    public static void hurt(ServerLevel level, LivingEntity entity, boolean fever, float damage) {
        if (fever ? isCooling(entity) : isWarming(entity)) return;
        if (damage > 0.0F) entity.hurt(fever ? level.damageSources().onFire() : level.damageSources().freeze(), damage);
    }
}
