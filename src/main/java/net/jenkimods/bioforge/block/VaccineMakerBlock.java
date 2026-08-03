package net.jenkimods.bioforge.block;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.world.vaccine.VaccineMakerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class VaccineMakerBlock extends BaseEntityBlock {
    public VaccineMakerBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof VaccineMakerBlockEntity maker
                && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, maker, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VaccineMakerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BioForge.VACCINE_MAKER_BE.get(),
                VaccineMakerBlockEntity::tick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof VaccineMakerBlockEntity maker) maker.drops();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
