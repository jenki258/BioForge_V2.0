package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.config.BioForgeClientConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=BioForge.MODID,value=Dist.CLIENT)
public final class BioForgeAudioPreferences {
    @SubscribeEvent public static void onSound(PlaySoundEvent event) {
        var sound=event.getSound();
        if (sound!=null && sound.getLocation().getNamespace().equals(BioForge.MODID)
                && !BioForgeClientConfig.allows(sound.getLocation().getPath())) event.setSound(null);
    }
}
