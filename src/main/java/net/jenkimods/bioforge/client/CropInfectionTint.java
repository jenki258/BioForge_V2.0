package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.BioForgeTags;
import net.jenkimods.bioforge.infection.CropInfection;
import net.jenkimods.bioforge.infection.PathogenType;
import net.jenkimods.bioforge.infection.capability.CropInfectionCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BioForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CropInfectionTint {
    private static final Map<PathogenType, Integer> BASE_COLORS = new HashMap<>();
    static {
        BASE_COLORS.put(PathogenType.FUNGI, 0xFFCCCC66);
        BASE_COLORS.put(PathogenType.BACTERIA, 0xFF66CC66);
        BASE_COLORS.put(PathogenType.VIRUS, 0xFFCC6666);
        BASE_COLORS.put(PathogenType.PARASITE, 0xFFCC66CC);
        BASE_COLORS.put(PathogenType.PRION, 0xFFCCCCCC);
    }

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        var tag = ForgeRegistries.BLOCKS.tags().getTag(BioForgeTags.INFECTABLE_CROPS);
        if (tag == null) return;
        for (Block block : tag) {
            event.register((state, reader, pos, tintIndex) -> {
                if (pos != null && reader instanceof Level level) {
                    LevelChunk chunk = level.getChunkAt(pos);
                    var storage = chunk.getCapability(CropInfectionCapability.CROP_INFECTION).orElse(null);
                    if (storage != null) {
                        CropInfection infection = storage.getInfection(pos);
                        if (infection != null && infection.getPathogen() != null) {
                            int base = BASE_COLORS.getOrDefault(infection.getPathogen(), 0xFFFFFFFF);
                            return PetriDishColorHandler.applyVariation(base, pos, infection.getColonyId());
                        }
                    }
                }
                return 0xFFFFFFFF;
            }, block);
        }
    }
}