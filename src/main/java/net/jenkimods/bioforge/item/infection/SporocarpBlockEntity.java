package net.jenkimods.bioforge.item.infection;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.block.MicrobialMatBlock;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SporocarpBlockEntity extends BlockEntity {

    private String strainData = null;
    public PathogenType pathogen = null;
    public UUID colonyId = null;
    @Nullable
    private BlockPos corePos = null;

    private static final int REGEN_RADIUS = 10;
    private static final int BURST_RADIUS = 5;
    private int regenCooldown = 0;
    private static final int REGEN_COOLDOWN_MAX = 1200; // 1 minute

    public SporocarpBlockEntity(BlockPos pos, BlockState state) {
        super(BioForge.SPOROCARP_BE.get(), pos, state);
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
                } else if (header.length >= 2) {
                    pathogen = PathogenType.fromName(header[0]);
                }
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setCorePos(BlockPos pos) {
        this.corePos = pos.immutable();
        setChanged();
    }

    @Nullable
    public BlockPos getCorePos() { return corePos; }

    public String getStrainData() { return strainData; }

    // Passive regeneration ticker
    public static void tick(Level level, BlockPos pos, BlockState state, SporocarpBlockEntity entity) {
        if (level.isClientSide() || entity.strainData == null) return;
        entity.regenCooldown++;
        if (entity.regenCooldown >= REGEN_COOLDOWN_MAX) {
            entity.regenCooldown = 0;
            entity.regenerateNearby((ServerLevel) level, pos);
        }
    }

    // Burst on death (sunlight)
    public void burst(ServerLevel level, BlockPos pos) {
        if (corePos == null) return;   // safety: shouldn't happen
        int count = 5 + level.random.nextInt(6); // 5-10 spores
        for (int i = 0; i < count; i++) {
            int dx = level.random.nextInt(BURST_RADIUS * 2 + 1) - BURST_RADIUS;
            int dz = level.random.nextInt(BURST_RADIUS * 2 + 1) - BURST_RADIUS;
            int dy = level.random.nextInt(3) - 1;
            BlockPos targetPos = pos.offset(dx, dy, dz);
            if (targetPos.equals(pos)) continue;
            if (!level.getBlockState(targetPos).isAir()) continue;
            BlockPos below = targetPos.below();
            if (!isSubstrate(level.getBlockState(below))) continue;

            level.setBlock(targetPos, BioForge.MICROBIAL_MAT.get().defaultBlockState()
                    .setValue(MicrobialMatBlock.GROWTH, 0)
                    .setValue(MicrobialMatBlock.HOST_CROP, false), 3);
            if (level.getBlockEntity(targetPos) instanceof MicrobialMatBlockEntity mat) {
                mat.setStrainData(strainData);
                mat.setCorePos(corePos);   // spread with core position
            }
        }
        level.destroyBlock(pos, false);
    }

    private void regenerateNearby(ServerLevel level, BlockPos pos) {
        if (corePos == null) return;
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        for (int x = -REGEN_RADIUS; x <= REGEN_RADIUS; x++) {
            for (int y = -REGEN_RADIUS; y <= REGEN_RADIUS; y++) {
                for (int z = -REGEN_RADIUS; z <= REGEN_RADIUS; z++) {
                    mPos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if (level.getBlockState(mPos).is(BioForge.MICROBIAL_MAT.get())) {
                        if (level.getBlockEntity(mPos) instanceof MicrobialMatBlockEntity mat) {
                            if (mat.colonyId != null && mat.colonyId.equals(colonyId)) {
                                int growth = level.getBlockState(mPos).getValue(MicrobialMatBlock.GROWTH);
                                if (growth < 4) {
                                    level.setBlock(mPos, level.getBlockState(mPos)
                                            .setValue(MicrobialMatBlock.GROWTH, growth + 1), 3);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isSubstrate(BlockState state) {
        return state.is(BlockTags.create(ResourceLocation.tryBuild("bioforge", "substrate/organic")))
                || state.is(BlockTags.create(ResourceLocation.tryBuild("bioforge", "substrate/wood")));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (strainData != null) NbtObfuscator.writeString(tag, strainData);
        tag.putInt("RegenCooldown", regenCooldown);
        if (corePos != null) {
            tag.putInt("CoreX", corePos.getX());
            tag.putInt("CoreY", corePos.getY());
            tag.putInt("CoreZ", corePos.getZ());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (NbtObfuscator.hasData(tag)) {
            String decrypted = NbtObfuscator.readString(tag);
            if (decrypted != null) {
                this.strainData = decrypted;
                if (!decrypted.equals("CLEAN")) {
                    String[] parts = decrypted.split(";");
                    if (parts.length > 0) {
                        String[] header = parts[0].split("\\|");
                        if (header.length >= 3) {
                            try { colonyId = UUID.fromString(header[0]); } catch (IllegalArgumentException ignored) {}
                            pathogen = PathogenType.fromName(header[1]);
                        } else if (header.length >= 2) {
                            pathogen = PathogenType.fromName(header[0]);
                        }
                    }
                }
            }
        }
        regenCooldown = tag.getInt("RegenCooldown");
        if (tag.contains("CoreX")) {
            corePos = new BlockPos(tag.getInt("CoreX"), tag.getInt("CoreY"), tag.getInt("CoreZ"));
        }
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }
    @Override public void handleUpdateTag(CompoundTag tag) { super.handleUpdateTag(tag); load(tag); }
    @Nullable @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) { if (pkt.getTag() != null) handleUpdateTag(pkt.getTag()); }
}