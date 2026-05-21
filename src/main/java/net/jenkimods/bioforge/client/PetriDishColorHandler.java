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

@Mod.EventBusSubscriber(modid = BioForge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PetriDishColorHandler {

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, reader, pos, tintIndex) -> {
                    if (tintIndex == 1 && pos != null && reader != null) {
                        BlockEntity be = reader.getBlockEntity(pos);
                        if (be instanceof PetriDishBlockEntity dish) {
                            if (dish.pathogen != null) {
                                return switch (dish.pathogen) {
                                    case VIRUS    -> 0xFF6666;
                                    case BACTERIA -> 0x66FF66;
                                    case FUNGI    -> 0xFFFF66;
                                    case PARASITE -> 0xCC66FF;
                                    case PRION    -> 0xFFFFFF;
                                    default       -> 0xAAAAAA;
                                };
                            } else {
                                return 0xAAAAAA;
                            }
                        }
                        return 0xAAAAAA;
                    }
                    return 0xFFFFFFFF;
                },
                BioForge.PETRI_DISH_BLOCK.get()
        );

        event.register(
                (state, reader, pos, tintIndex) -> {
                    if (tintIndex == 1 && pos != null && reader != null) {
                        BlockEntity be = reader.getBlockEntity(pos);
                        if (be instanceof MicrobialMatBlockEntity mat) {
                            if (mat.pathogen != null) {
                                return switch (mat.pathogen) {
                                    case VIRUS    -> 0xFF6666;
                                    case BACTERIA -> 0x66FF66;
                                    case FUNGI    -> 0xFFFF66;
                                    case PARASITE -> 0xCC66FF;
                                    case PRION    -> 0xFFFFFF;
                                    default       -> 0xAAAAAA;
                                };
                            }
                            return 0xAAAAAA; // default grey
                        }
                    }
                    return 0xFFFFFFFF;
                },
                BioForge.MICROBIAL_MAT.get()
        );

        event.register(
                (state, reader, pos, tintIndex) -> {
                    if (tintIndex == 1 && pos != null && reader != null) {
                        BlockEntity be = reader.getBlockEntity(pos);
                        if (be instanceof InfestedBlockEntity mat) {
                            if (mat.pathogen != null) {
                                return switch (mat.pathogen) {
                                    case VIRUS    -> 0xFF6666;
                                    case BACTERIA -> 0x66FF66;
                                    case FUNGI    -> 0xFFFF66;
                                    case PARASITE -> 0xCC66FF;
                                    case PRION    -> 0xFFFFFF;
                                    default       -> 0xAAAAAA;
                                };
                            }
                            return 0xAAAAAA;
                        }
                    }
                    return 0xFFFFFFFF;
                },
                BioForge.INFESTED_BLOCK.get()
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
                            if (parts.length > 0) {
                                String[] header = parts[0].split("\\|");
                                if (header.length >= 2) {
                                    PathogenType pt = PathogenType.fromName(header[0]);
                                    if (pt != null) {
                                        return switch (pt) {
                                            case VIRUS    -> 0xFF6666;
                                            case BACTERIA -> 0x66FF66;
                                            case FUNGI    -> 0xFFFF66;
                                            case PARASITE -> 0xCC66FF;
                                            case PRION    -> 0xFFFFFF;
                                            default       -> 0xAAAAAA;
                                        };
                                    }
                                }
                            }
                        }
                        return 0xAAAAAA;
                    }
                    return 0xFFFFFFFF;
                },
                BioForge.PETRI_DISH.get()
        );
    }
}