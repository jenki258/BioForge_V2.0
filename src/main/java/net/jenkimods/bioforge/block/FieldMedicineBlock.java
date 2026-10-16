package net.jenkimods.bioforge.block;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.world.medicine.FieldMedicineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public final class FieldMedicineBlock extends BaseEntityBlock {
    public FieldMedicineBlock() {
        this(false);
    }
    public FieldMedicineBlock(boolean fullCube) {
        super(properties(fullCube));
    }
    private static BlockBehaviour.Properties properties(boolean fullCube) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD);
        return fullCube ? properties : properties.noOcclusion();
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FieldMedicineBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, BioForge.FIELD_MEDICINE_BE.get(), FieldMedicineBlockEntity::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof FieldMedicineBlockEntity machine)
            NetworkHooks.openScreen(serverPlayer, machine, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock() && level.getBlockEntity(pos) instanceof FieldMedicineBlockEntity machine) machine.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }
}
