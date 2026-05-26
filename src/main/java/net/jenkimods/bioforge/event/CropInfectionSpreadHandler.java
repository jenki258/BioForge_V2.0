package net.jenkimods.bioforge.event;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.BioForgeTags;
import net.jenkimods.bioforge.infection.CropInfection;
import net.jenkimods.bioforge.infection.capability.CropInfectionCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class CropInfectionSpreadHandler {
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter % 20 != 0) return;

        ServerLevel level = event.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (level == null) return;

        int randomTickSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        double chance = (randomTickSpeed * 2.0) / 4096.0;

        RandomSource rand = level.random;

        for (LevelChunk chunk : LoadedChunksTracker.getLoadedChunks()) {
            if (chunk.getLevel() != level) continue;
            chunk.getCapability(CropInfectionCapability.CROP_INFECTION).ifPresent(storage -> {
                List<Map.Entry<BlockPos, CropInfection>> entries = new ArrayList<>(storage.getAllInfections().entrySet());
                for (Map.Entry<BlockPos, CropInfection> entry : entries) {
                    BlockPos pos = entry.getKey();
                    BlockState state = chunk.getBlockState(pos);
                    if (!isMature(state) || !state.is(BioForgeTags.INFECTABLE_CROPS)) continue;

                    for (int attempt = 0; attempt < 3; attempt++) {
                        if (rand.nextFloat() >= chance) continue;

                        BlockPos neighbor = pos.offset(rand.nextInt(3) - 1, 0, rand.nextInt(3) - 1);
                        if (neighbor.equals(pos)) continue;

                        LevelChunk neighborChunk = level.getChunkAt(neighbor);
                        BlockState neighborState = neighborChunk.getBlockState(neighbor);
                        if (!isMature(neighborState) || !neighborState.is(BioForgeTags.INFECTABLE_CROPS)) continue;

                        var neighborStorage = neighborChunk.getCapability(CropInfectionCapability.CROP_INFECTION).orElse(null);
                        if (neighborStorage == null || neighborStorage.isInfected(neighbor)) continue;

                        CropInfection newInfection = new CropInfection(entry.getValue().getStrainData());
                        neighborStorage.setInfection(neighbor, newInfection);
                        neighborChunk.setUnsaved(true);
                        break;
                    }
                }
            });
        }
    }

    private static boolean isMature(BlockState state) {
        for (var prop : state.getProperties()) {
            if (prop.getName().equals("age") && prop instanceof IntegerProperty ageProp) {
                int max = ageProp.getPossibleValues().stream().max(Integer::compare).orElse(7);
                return state.getValue(ageProp) == max;
            }
        }
        return false;
    }
}