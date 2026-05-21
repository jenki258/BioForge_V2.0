package net.jenkimods.bioforge.block;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.item.infection.InfestedBlockEntity;
import net.jenkimods.bioforge.item.infection.SwabItem;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class InfestedBlock extends BaseEntityBlock {

    public static final IntegerProperty GROWTH = IntegerProperty.create("growth", 0, 4);

    public InfestedBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(1.5F)
                .noOcclusion()
                .randomTicks());
        registerDefaultState(stateDefinition.any().setValue(GROWTH, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GROWTH);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfestedBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState();
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof InfestedBlockEntity entity) entity.randomTick(level, pos, state, random);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof InfestedBlockEntity mat)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof SwabItem && !SwabItem.isContaminated(held)) {
            if (mat.getStrainData() != null) {
                NbtObfuscator.writeString(held.getOrCreateTag(), mat.getStrainData());
                player.setItemInHand(hand, held);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8f, 1.2f);
                player.sendSystemMessage(Component.translatable("item.bioforge.swab.collected_mat"));
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof InfestedBlockEntity mat)) return;
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
        if (mat.pathogen == null || mat.pathogen != net.jenkimods.bioforge.infection.PathogenType.FUNGI) return;

        net.jenkimods.bioforge.infection.InfectionData data =
                net.jenkimods.bioforge.infection.InfectionCapability.get(living);
        if (data == null) return;

        String strain = mat.getStrainData();
        float newStrength = mat.infectionStrength;

        if (strain == null || strain.equals("CLEAN")) return;

        if (!data.isInfected()) {
            applyStrainToEntity(living, strain);
        } else {
            float oldStrength = data.getSymptom(
                    net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.INFECTION_STRENGTH);
            float r1 = 0.8f + level.random.nextFloat() * 0.4f;
            float r2 = 0.8f + level.random.nextFloat() * 0.4f;
            float ns = newStrength * r1;
            float os = oldStrength * r2;

            if (ns > os * 1.5f) {
                applyStrainToEntity(living, strain);
            } else if (ns > os * 1.0f) {
                float avgStr = (oldStrength + newStrength) / 2f;
                data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.INFECTION_STRENGTH, avgStr);
            } else if (ns > os * 0.7f) {
                data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.INFECTION_STRENGTH,
                        Math.min(1.0f, oldStrength + 0.05f));
            }
        }
    }

    private void applyStrainToEntity(LivingEntity entity, String strain) {
        net.jenkimods.bioforge.infection.InfectionData data =
                net.jenkimods.bioforge.infection.InfectionCapability.get(entity);
        if (data == null) return;
        String[] parts = strain.split(";");
        if (parts.length < 2) return;
        String[] header = parts[0].split("\\|");
        if (header.length < 2) return;

        net.jenkimods.bioforge.infection.PathogenType pt =
                net.jenkimods.bioforge.infection.PathogenType.fromName(header[1]);
        net.jenkimods.bioforge.infection.InfectionType it =
                net.jenkimods.bioforge.infection.InfectionType.fromName(header[2]);

        data.setInfected(true);
        data.setPathogenType(pt);
        data.setInfectionType(it);
        for (int i = 1; i < parts.length; i++) {
            String[] kv = parts[i].split("=");
            if (kv.length == 2) {
                try {
                    switch (kv[0]) {
                        case "HeartRate" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.HEART_RATE,
                                net.jenkimods.bioforge.infection.HeartRate.fromName(kv[1]));
                        case "LungSound" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.LUNG_SOUND,
                                net.jenkimods.bioforge.infection.LungSound.fromName(kv[1]));
                        case "TempPlus" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.TEMPERATURE_PLUS,
                                Boolean.parseBoolean(kv[1]));
                        case "TempMinus" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.TEMPERATURE_MINUS,
                                Boolean.parseBoolean(kv[1]));
                        case "Redness" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.OTOSCOPE_REDNESS,
                                Float.parseFloat(kv[1]));
                        case "Lesions" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.OTOSCOPE_LESIONS,
                                Float.parseFloat(kv[1]));
                        case "Secretion" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.OTOSCOPE_SECRETION,
                                Float.parseFloat(kv[1]));
                        case "Swelling" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.OTOSCOPE_SWELLING,
                                Float.parseFloat(kv[1]));
                        case "ReflexDelay" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.REFLEX_DELAY,
                                Float.parseFloat(kv[1]));
                        case "ReflexStrength" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.REFLEX_STRENGTH,
                                Float.parseFloat(kv[1]));
                        case "NeuralDamage" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.NEURAL_DAMAGE,
                                Float.parseFloat(kv[1]));
                        case "OxygenSaturation" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.OXYGEN_SATURATION,
                                Float.parseFloat(kv[1]));
                        case "PerfusionIndex" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.PERFUSION_INDEX,
                                Float.parseFloat(kv[1]));
                        case "InfectionStrength" -> data.setSymptom(net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms.INFECTION_STRENGTH,
                                Float.parseFloat(kv[1]));
                    }
                } catch (Exception ignored) {}
            }
        }
        if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.jenkimods.bioforge.infection.InfectionEventHandler.syncToClient(sp, data);
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity be, ItemStack tool) {
        if (!level.isClientSide() && be instanceof InfestedBlockEntity mat) {
            if (!player.isCreative()) {

            }
        }
        level.removeBlockEntity(pos);

        super.playerDestroy(level, player, pos, state, be, tool);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof InfestedBlockEntity mat && mat.hostState != null) {
                level.setBlock(pos, mat.hostState, 3);
                return;
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}