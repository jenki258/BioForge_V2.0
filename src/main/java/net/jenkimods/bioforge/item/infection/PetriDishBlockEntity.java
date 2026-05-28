package net.jenkimods.bioforge.item.infection;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.block.PetriDishBlock;
import net.jenkimods.bioforge.infection.InfectionType;
import net.jenkimods.bioforge.infection.PathogenType;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PetriDishBlockEntity extends BlockEntity {

    private String strainData = null;
    public boolean preventDrop = false;

    public int growthStage = 0;
    public PathogenType pathogen = null;
    public Set<InfectionType> infectionTypes = EnumSet.noneOf(InfectionType.class);

    public PetriDishBlockEntity(BlockPos pos, BlockState state) {
        super(BioForge.PETRI_DISH_BE.get(), pos, state);
    }

    public boolean isInoculated() {
        return strainData != null;
    }

    public void setStrainData(String encrypted) {
        this.strainData = encrypted;
        if (encrypted != null && !encrypted.equals("CLEAN")) {
            String[] parts = encrypted.split(";");
            if (parts.length > 0) {
                String[] header = parts[0].split("\\|");
                if (header.length >= 3) {
                    pathogen = PathogenType.fromName(header[1]);
                    infectionTypes.clear();
                    parseTypes(header[2], infectionTypes);
                } else if (header.length >= 2) {
                    pathogen = PathogenType.fromName(header[0]);
                    infectionTypes.clear();
                    parseTypes(header[1], infectionTypes);
                } else {
                    pathogen = null;
                    infectionTypes.clear();
                }
            }
        } else {
            pathogen = null;
            infectionTypes.clear();
        }
        growthStage = 0;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public String getStrainData() {
        return strainData;
    }

    public boolean attemptGrowth(RandomSource random) {
        if (!isInoculated() || pathogen == null || growthStage >= 4) return false;

        double chance = switch (pathogen) {
            case BACTERIA -> 0.3;
            case VIRUS    -> 0.2;
            case FUNGI    -> 0.25;
            case PARASITE -> 0.15;
            case PRION    -> 0.08;
            default       -> 0.2;
        };
        if (random.nextFloat() < chance) {
            growthStage++;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.setBlock(worldPosition, getBlockState().setValue(PetriDishBlock.GROWTH, growthStage), 3);
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
            return true;
        }
        return false;
    }

    public boolean harvest(ItemStack swab, Player player) {
        if (!isInoculated() || strainData == null) return false;
        if (growthStage < 3) return false;
        if (SwabItem.isContaminated(swab)) return false;

        NbtObfuscator.writeString(swab.getOrCreateTag(), strainData);

        if (growthStage == 3) {
            strainData = null;
            pathogen = null;
            infectionTypes.clear();
            growthStage = 0;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.setBlock(worldPosition, getBlockState().setValue(PetriDishBlock.GROWTH, 0), 3);
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        if (player != null) {
            player.sendSystemMessage(Component.translatable("item.bioforge.petri_dish.harvested"));
        }
        return true;
    }

    public void saveToStack(ItemStack stack) {
        if (strainData != null) {
            CompoundTag tag = stack.getOrCreateTag();
            NbtObfuscator.writeString(tag, strainData);
            tag.putInt("Growth", growthStage);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        load(tag);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (strainData != null) {
            NbtObfuscator.writeString(tag, strainData);
        }
        tag.putInt("Growth", growthStage);
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
                            pathogen = PathogenType.fromName(header[1]);
                            infectionTypes.clear();
                            parseTypes(header[2], infectionTypes);
                        } else if (header.length >= 2) {
                            pathogen = PathogenType.fromName(header[0]);
                            infectionTypes.clear();
                            parseTypes(header[1], infectionTypes);
                        } else {
                            pathogen = null;
                            infectionTypes.clear();
                        }
                    }
                } else {
                    pathogen = null;
                    infectionTypes.clear();
                }
            }
        }
        growthStage = tag.getInt("Growth");
    }

    private void parseTypes(String raw, Set<InfectionType> target) {
        if (raw == null || raw.isEmpty()) return;
        for (String part : raw.split(",")) {
            InfectionType it = InfectionType.fromName(part.trim());
            if (it != null) target.add(it);
        }
    }
}