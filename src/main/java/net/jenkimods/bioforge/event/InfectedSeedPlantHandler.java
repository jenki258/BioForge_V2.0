package net.jenkimods.bioforge.event;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.BioForgeTags;
import net.jenkimods.bioforge.infection.CropInfection;
import net.jenkimods.bioforge.infection.capability.CropInfectionCapability;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class InfectedSeedPlantHandler {

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor.isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        BlockState placedState = event.getPlacedBlock();
        if (!placedState.is(BioForgeTags.INFECTABLE_CROPS)) return;

        ItemStack stack = player.getMainHandItem();
        String strain = NbtObfuscator.readString(stack.getOrCreateTag());
        if (strain == null || strain.equals("CLEAN")) return;

        ServerLevel level = (ServerLevel) levelAccessor;
        BlockPos pos = event.getPos();
        LevelChunk chunk = level.getChunkAt(pos);
        chunk.getCapability(CropInfectionCapability.CROP_INFECTION).ifPresent(storage -> {
            if (!storage.isInfected(pos)) {
                storage.setInfection(pos, new CropInfection(strain));
                chunk.setUnsaved(true);
            }
        });
    }
}