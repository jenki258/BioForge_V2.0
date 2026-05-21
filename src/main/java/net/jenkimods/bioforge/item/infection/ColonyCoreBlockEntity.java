package net.jenkimods.bioforge.item.infection;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.block.MicrobialMatBlock;
import net.jenkimods.bioforge.infection.InfectionType;
import net.jenkimods.bioforge.infection.PathogenType;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ColonyCoreBlockEntity extends BlockEntity {

    private String strainData = null;
    public UUID colonyId = null;
    public PathogenType pathogen = null;
    public InfectionType infectionType = null;
    private int resources = 25;
    private int infectedBlockCount = 0;
    private static final int MAX_INFESTED_BLOCKS = 100;
    private static final int MAT_SPAWN_COST = 1;

    public ColonyCoreBlockEntity(BlockPos pos, BlockState state) {
        super(BioForge.COLONY_CORE_BE.get(), pos, state);
    }

    public void setStrainData(String encrypted) {
        this.strainData = encrypted;
        if (encrypted != null && !encrypted.equals("CLEAN")) {
            String[] parts = encrypted.split(";");
            if (parts.length > 0) {
                String[] header = parts[0].split("\\|");
                if (header.length >= 3) {
                    try { colonyId = UUID.fromString(header[0]); } catch (IllegalArgumentException ignored) {}
                    pathogen = PathogenType.fromName(header[1]);
                    infectionType = InfectionType.fromName(header[2]);
                }
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public String getStrainData() { return strainData; }

    public void addResources(int amount) {
        resources += amount;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean consumeResources(int amount) {
        if (resources >= amount) {
            resources -= amount;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
            return true;
        }
        return false;
    }

    public int getResources() { return resources; }

    public boolean canCreateInfestedBlock() { return infectedBlockCount < MAX_INFESTED_BLOCKS; }

    public void incrementInfestedCount() { infectedBlockCount++; setChanged(); }
    public void decrementInfestedCount() { infectedBlockCount--; setChanged(); }

    public void randomTick(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (strainData == null) return;

        int attempts = 1 + random.nextInt(3);
        for (int i = 0; i < attempts; i++) {
            if (!consumeResources(MAT_SPAWN_COST)) break;  // no points → no spawn

            int dx = random.nextInt(7) - 3;
            int dz = random.nextInt(7) - 3;
            int dy = random.nextFloat() < 0.2f ? 1 : 0;
            BlockPos target = pos.offset(dx, dy, dz);
            if (target.equals(pos)) continue;
            if (!level.getBlockState(target).isAir()) continue;
            BlockPos below = target.below();
            if (!isValidSubstrate(level.getBlockState(below))) continue;

            level.setBlock(target, BioForge.MICROBIAL_MAT.get().defaultBlockState()
                    .setValue(MicrobialMatBlock.GROWTH, 0)
                    .setValue(MicrobialMatBlock.HOST_CROP, false), 3);
            if (level.getBlockEntity(target) instanceof MicrobialMatBlockEntity mat) {
                mat.setStrainData(strainData);
                mat.setCorePos(pos);
            }
        }
    }

    private boolean isValidSubstrate(BlockState state) {
        return state.is(BlockTags.create(ResourceLocation.tryBuild("bioforge", "substrate/organic")))
                || state.is(BlockTags.create(ResourceLocation.tryBuild("bioforge", "substrate/wood")))
                || state.is(BlockTags.create(ResourceLocation.tryBuild("bioforge", "substrate/stone")))
                || state.is(BlockTags.create(ResourceLocation.tryBuild("bioforge", "substrate/moisture")));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (strainData != null) NbtObfuscator.writeString(tag, strainData);
        tag.putInt("Resources", resources);
        tag.putInt("InfectedCount", infectedBlockCount);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (NbtObfuscator.hasData(tag)) {
            String data = NbtObfuscator.readString(tag);
            if (data != null) setStrainData(data);
        }
        resources = tag.getInt("Resources");
        infectedBlockCount = tag.getInt("InfectedCount");
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }
    @Override public void handleUpdateTag(CompoundTag tag) { super.handleUpdateTag(tag); load(tag); }
    @Nullable @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) { if (pkt.getTag() != null) handleUpdateTag(pkt.getTag()); }
}