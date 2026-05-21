package net.jenkimods.bioforge.block;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.item.infection.ColonyCoreBlockEntity;
import net.jenkimods.bioforge.item.infection.MicrobialMatBlockEntity;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ContaminatedSubstrateBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    public ContaminatedSubstrateBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(0.2F)
                .noOcclusion()
                .isViewBlocking((a, b, c) -> false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    // When placed, immediately convert to microbial mat with stored strain
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;

        String strain = NbtObfuscator.readString(stack.getOrCreateTag());
        if (strain == null || strain.isEmpty()) {
            // Should not happen – fallback to air
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        // Replace MICROBIAL_MAT with COLONY_CORE
        level.setBlock(pos, BioForge.COLONY_CORE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof ColonyCoreBlockEntity core) {
            core.setStrainData(strain);
        }

    }
}