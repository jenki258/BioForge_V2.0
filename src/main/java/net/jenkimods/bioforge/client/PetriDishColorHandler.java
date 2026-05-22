package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.PathogenType;
import net.jenkimods.bioforge.item.infection.InfestedBlockEntity;
import net.jenkimods.bioforge.item.infection.MicrobialMatBlockEntity;
import net.jenkimods.bioforge.item.infection.PetriDishBlockEntity;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = BioForge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PetriDishColorHandler {

    private static final int VARIATION_RANGE = 20;

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, reader, pos, tintIndex) -> {
                    if (tintIndex == 1 && pos != null && reader != null) {
                        BlockEntity be = reader.getBlockEntity(pos);
                        if (be instanceof PetriDishBlockEntity dish) {
                            return applyVariation(getBaseColor(dish.pathogen), pos, null);
                        }
                    }
                    return 0xFFFFFFFF;
                }, BioForge.PETRI_DISH_BLOCK.get()
        );

        event.register(
                (state, reader, pos, tintIndex) -> {
                    if (tintIndex == 1 && pos != null && reader != null) {
                        BlockEntity be = reader.getBlockEntity(pos);
                        if (be instanceof MicrobialMatBlockEntity mat) {
                            return applyVariation(getBaseColor(mat.pathogen), pos, mat.colonyId);
                        }
                    }
                    return 0xFFFFFFFF;
                }, BioForge.MICROBIAL_MAT.get()
        );

        event.register(
                (state, reader, pos, tintIndex) -> {
                    if (tintIndex == 1 && pos != null && reader != null) {
                        BlockEntity be = reader.getBlockEntity(pos);
                        if (be instanceof InfestedBlockEntity infested) {
                            return applyVariation(getBaseColor(infested.pathogen), pos, infested.colonyId);
                        }
                    }
                    return 0xFFFFFFFF;
                }, BioForge.INFESTED_BLOCK.get()
        );
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 1) {
                        String data = NbtObfuscator.readString(stack.getOrCreateTag());
                        if (data != null && !data.equals("CLEAN")) {
                            String[] parts = data.split(";");
                            String[] header = parts[0].split("\\|");
                            PathogenType pathogen = PathogenType.fromName(header.length >= 2 ? header[0] : "");
                            return getBaseColor(pathogen);
                        }
                    }
                    return 0xFFFFFFFF;
                }, BioForge.PETRI_DISH.get()
        );
    }

    private static int getBaseColor(@Nullable PathogenType pathogen) {
        if (pathogen == null) return 0xFFAAAAAA;
        return switch (pathogen) {
            case VIRUS    -> 0xFFCC6666;
            case BACTERIA -> 0xFF66CC66;
            case FUNGI    -> 0xFFCCCC66;
            case PARASITE -> 0xFFCC66CC;
            case PRION    -> 0xFFCCCCCC;
            default      -> 0xFFAAAAAA;
        };
    }

    private static int applyVariation(int baseColor, BlockPos pos, @Nullable UUID colonyId) {
        long seed;
        if (colonyId != null) {
            seed = colonyId.getMostSignificantBits() ^ colonyId.getLeastSignificantBits();
        } else {
            seed = (long) pos.getX() * 31 + (long) pos.getY() * 17 + (long) pos.getZ() * 13;
        }

        int rOffset = (int) (seed % (VARIATION_RANGE + 1));
        int gOffset = (int) ((seed >> 8) % (VARIATION_RANGE + 1));
        int bOffset = (int) ((seed >> 16) % (VARIATION_RANGE + 1));

        int a = (baseColor >> 24) & 0xFF;
        int r = Math.min(255, Math.max(0, ((baseColor >> 16) & 0xFF) + rOffset));
        int g = Math.min(255, Math.max(0, ((baseColor >> 8) & 0xFF) + gOffset));
        int b = Math.min(255, Math.max(0, (baseColor & 0xFF) + bOffset));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}