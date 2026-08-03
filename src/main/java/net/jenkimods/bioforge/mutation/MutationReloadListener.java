package net.jenkimods.bioforge.mutation;

import net.jenkimods.bioforge.BioForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class MutationReloadListener {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MutationLoader.INSTANCE);
    }
}