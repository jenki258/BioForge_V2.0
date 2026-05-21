package net.jenkimods.bioforge.world.centrifuge;

import net.jenkimods.bioforge.BioForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class CentrifugeReloadListener {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(CentrifugeRecipeManager.INSTANCE);
    }
}
