package net.jenkimods.bioforge.infection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class EchoRayTracer {
    public record Reflection(Vec3 position, double pathLength) {}

    private EchoRayTracer() {}

    public static List<Reflection> trace(Level level, Entity listener, Vec3 origin,
                                         Vec3 direction, double range, int bounces) {
        List<Reflection> reflections = new ArrayList<>();
        Vec3 start = origin;
        Vec3 ray = direction.normalize();
        double travelled = 0.0D;
        for (int bounce = 0; bounce < Math.min(3, bounces); bounce++) {
            double remaining = range - travelled;
            if (remaining < 0.1D) break;
            Vec3 end = start.add(ray.scale(remaining));
            if (!level.hasChunksAt(BlockPos.containing(start), BlockPos.containing(end))) break;
            var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, listener));
            if (hit.getType() != HitResult.Type.BLOCK) break;
            double segment = start.distanceTo(hit.getLocation());
            if (segment < 0.01D) break;
            travelled += segment;
            if (level.getBlockState(hit.getBlockPos()).is(net.minecraft.tags.BlockTags.OCCLUDES_VIBRATION_SIGNALS)) break;
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            reflections.add(new Reflection(hit.getLocation().add(normal.scale(0.03D)), travelled));
            ray = ray.subtract(normal.scale(2.0D * ray.dot(normal))).normalize();
            start = hit.getLocation().add(ray.scale(0.03D));
        }
        return reflections;
    }
}
