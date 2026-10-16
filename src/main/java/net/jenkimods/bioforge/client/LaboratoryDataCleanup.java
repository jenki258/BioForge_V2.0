package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.network.LaboratoryDataNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID, value = Dist.CLIENT)
public final class LaboratoryDataCleanup {
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() == null) LaboratoryDataNetwork.clearRemote();
    }
}
