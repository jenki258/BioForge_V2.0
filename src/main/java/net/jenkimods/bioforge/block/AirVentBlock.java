package net.jenkimods.bioforge.block;

import net.jenkimods.bioforge.infection.InfectionType;
import net.jenkimods.bioforge.infection.spread.SurfaceContaminationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = net.jenkimods.bioforge.BioForge.MODID)
public final class AirVentBlock extends Block {
    public static final int CLEAN_RADIUS = 10;
    private static final Map<ServerLevel, Set<Long>> ACTIVE_VENTS = new WeakHashMap<>();

    public AirVentBlock() {
        super(BlockBehaviour.Properties.of().strength(3.5F, 8.0F)
                .requiresCorrectToolForDrops().sound(SoundType.METAL));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                        boolean moving) {
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            register(serverLevel, pos);
            serverLevel.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean moving) {
        if (level instanceof ServerLevel serverLevel && !newState.is(this)) {
            unregister(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.is(this)) {
            unregister(level, pos);
            return;
        }
        register(level, pos);
        if (level.getGameTime()%60<20) net.jenkimods.bioforge.audio.ActionAudio.play(level,pos,"machine.air_filter.work",net.minecraft.sounds.SoundSource.BLOCKS,.25F);
        SurfaceContaminationData.get(level).cleanTransmission(pos, CLEAN_RADIUS,
                InfectionType.AIR_BORNE, level.getGameTime());
        level.scheduleTick(pos, this, 20);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onChunkLoad(net.minecraftforge.event.level.ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var chunk = event.getChunk();
        level.getServer().execute(() -> {
            if (!level.hasChunk(chunk.getPos().x, chunk.getPos().z)) return;
            for (int sectionIndex = 0; sectionIndex < chunk.getSections().length; sectionIndex++) {
                var section = chunk.getSections()[sectionIndex];
                if (section.hasOnlyAir() || !section.getStates().maybeHas(
                        state -> state.getBlock() instanceof AirVentBlock)) continue;
                int yBase = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                    if (section.getBlockState(x, y, z).getBlock() instanceof AirVentBlock vent) {
                        BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + x,
                                yBase + y, chunk.getPos().getMinBlockZ() + z);
                        register(level, pos);
                        level.scheduleTick(pos, vent, 1);
                    }
                }
            }
        });
    }

    public static boolean isProtected(ServerLevel level, BlockPos pos) {
        Set<Long> vents = ACTIVE_VENTS.get(level);
        if (vents == null || vents.isEmpty()) return false;
        long radiusSquared = (long) CLEAN_RADIUS * CLEAN_RADIUS;
        Iterator<Long> iterator = vents.iterator();
        while (iterator.hasNext()) {
            long packed = iterator.next();
            BlockPos vent = BlockPos.of(packed);
            if (!level.hasChunkAt(vent)) continue;
            if (!(level.getBlockState(vent).getBlock() instanceof AirVentBlock)) {
                iterator.remove();
                continue;
            }
            long dx = BlockPos.getX(packed) - pos.getX();
            long dy = BlockPos.getY(packed) - pos.getY();
            long dz = BlockPos.getZ(packed) - pos.getZ();
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) return true;
        }
        if (vents.isEmpty()) ACTIVE_VENTS.remove(level);
        return false;
    }

    public static void clear(ServerLevel level) {
        ACTIVE_VENTS.remove(level);
    }

    private static void register(ServerLevel level, BlockPos pos) {
        ACTIVE_VENTS.computeIfAbsent(level, ignored -> new HashSet<>()).add(pos.asLong());
    }

    private static void unregister(ServerLevel level, BlockPos pos) {
        Set<Long> vents = ACTIVE_VENTS.get(level);
        if (vents == null) return;
        vents.remove(pos.asLong());
        if (vents.isEmpty()) ACTIVE_VENTS.remove(level);
    }
}
