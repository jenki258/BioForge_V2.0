package net.jenkimods.bioforge.mutation;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.api.behavior.BioForgeBehaviorRegistry;
import net.jenkimods.bioforge.api.behavior.MutationEffectContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MinecraftMutationBehaviors {
    private static final String NEXT_TELEPORT = "BioForgeEnderDisplacementNext";
    private static final int TELEPORT_COOLDOWN = 6000;

    private MinecraftMutationBehaviors() {}

    public static void register() {
        BioForgeBehaviorRegistry.registerMutationEffect(
                ResourceLocation.tryBuild(BioForge.MODID, "safe_random_teleport"),
                MinecraftMutationBehaviors::teleport);

    }

    private static void teleport(MutationEffectContext context) {
        LivingEntity entity = context.entity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        var persistent = entity.getPersistentData();
        if (context.effect().booleanValue("reset", false)) {
            persistent.remove(NEXT_TELEPORT);
            return;
        }
        long now = level.getGameTime();
        long next = persistent.getLong(NEXT_TELEPORT);
        if (!persistent.contains(NEXT_TELEPORT) || next > now + TELEPORT_COOLDOWN) {
            persistent.putLong(NEXT_TELEPORT, now + TELEPORT_COOLDOWN);
            return;
        }
        if (now < next) return;
        persistent.putLong(NEXT_TELEPORT, now + TELEPORT_COOLDOWN);
        if (!entity.isAlive() || entity.isPassenger() || entity.isVehicle()
                || entity.isSleeping() || entity.isFallFlying()) return;

        Vec3 origin = entity.position();
        BlockPos base = entity.blockPosition();
        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = entity.getRandom().nextInt(21) - 10;
            int dz = entity.getRandom().nextInt(21) - 10;
            int firstHeight = entity.getRandom().nextInt(9);
            for (int height = 0; height < 9; height++) {
                int dy = (firstHeight + height) % 9 - 4;
                Vec3 target = Vec3.atBottomCenterOf(base.offset(dx, dy, dz));
                double distance = target.distanceToSqr(origin);
                if (distance < 4.0D || distance > 100.0D || !isSafeDestination(entity, level, target)) continue;
                if (entity.randomTeleport(target.x, target.y, target.z, true)) {
                    entity.fallDistance = 0.0F;
                    entity.setDeltaMovement(Vec3.ZERO);
                    level.playSound(null, origin.x, origin.y, origin.z,
                            SoundEvents.ENDERMAN_TELEPORT, entity.getSoundSource(), 0.6F, 1.0F);
                    entity.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.6F, 1.0F);
                    return;
                }
            }
        }
    }

    public static boolean isSafeDestination(LivingEntity entity, ServerLevel level, Vec3 target) {
        AABB bounds = entity.getBoundingBox().move(target.subtract(entity.position()));
        AABB probe = bounds.inflate(0.1D);
        BlockPos min = BlockPos.containing(probe.minX, probe.minY - 1.0D, probe.minZ);
        BlockPos max = BlockPos.containing(probe.maxX, probe.maxY, probe.maxZ);
        if (min.getY() < level.getMinBuildHeight() || max.getY() >= level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(probe)
                || !level.hasChunksAt(min, max)) return false;
        BlockPos floor = BlockPos.containing(target).below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                || !level.noCollision(entity, bounds) || level.containsAnyLiquid(probe)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            var state = level.getBlockState(pos);
            if (state.is(BlockTags.FIRE) || state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK)
                    || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                    || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.SWEET_BERRY_BUSH)
                    || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.POINTED_DRIPSTONE)
                    || !state.getFluidState().isEmpty()) return false;
        }
        return true;
    }
}
