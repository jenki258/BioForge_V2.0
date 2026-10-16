package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.BioForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HazmatColorHandler {
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 0 ? ((net.minecraft.world.item.DyeableLeatherItem) stack.getItem()).getColor(stack) : 0xFFFFFF,
                BioForge.HAZCURE_HELMET.get(), BioForge.HAZCURE_CHESTPLATE.get(), BioForge.HAZCURE_LEGGINGS.get(), BioForge.HAZCURE_BOOTS.get());
    }
}
