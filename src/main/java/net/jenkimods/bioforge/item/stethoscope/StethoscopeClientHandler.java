package net.jenkimods.bioforge.item.stethoscope;

import net.jenkimods.bioforge.infection.HeartRate;
import net.jenkimods.bioforge.infection.LungSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="bioforge",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public class StethoscopeClientHandler {

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase!=net.minecraftforge.event.TickEvent.Phase.END || !listening) return;
        var player=Minecraft.getInstance().player;
        if (player==null || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof StethoscopeItem)) stopListening();
    }

    public static HeartRate getHeartState() { return heartRate; }
    public static LungSound getLungState() { return lungSound; }

    private static AuscultationSite site=AuscultationSite.HEART;
    public static AuscultationSite getSite() { return site; }
    private static boolean listening = false;
    private static int targetEntityId = -1;
    private static HeartRate heartRate = HeartRate.NORMAL;
    private static LungSound lungSound = LungSound.NORMAL;
    private static String targetName = "";
    private static boolean hasReading = false;
    private static SoundInstance heartSoundInstance = null;
    private static SoundInstance lungSoundInstance = null;

    public static void beginListening(int entityId,AuscultationSite selected) {
        if (listening && entityId == targetEntityId && site==selected) return;
        stopSounds();
        listening = true;
        site=selected;
        targetEntityId = entityId;
        hasReading = false;
        heartRate = HeartRate.NORMAL;
        lungSound = LungSound.NORMAL;
        targetName = "";
    }

    public static void applyReading(HeartRate hr, LungSound ls, String name,AuscultationSite selected) {
        if (!listening) return;
        if (hasReading && heartRate==hr && lungSound==ls && site==selected) { targetName=name;return; }
        stopSounds();
        site=selected;
        hasReading = true;
        heartRate = hr;
        lungSound = ls;
        if (!name.isEmpty()) {
            targetName = name;
        }
        if (site.isHeart()) playHeartSound();
        else playLungSound();
    }

    private static void playHeartSound() {
        Minecraft mc = Minecraft.getInstance();
        SoundEvent event = switch (heartRate) {
            case TACHY -> StethoscopeSounds.HEARTBEAT_FAST.get();
            case BRADY -> StethoscopeSounds.HEARTBEAT_SLOW.get();
            default -> StethoscopeSounds.HEARTBEAT_NORMAL.get();
        };
        heartSoundInstance = new LoopingSoundInstance(event, SoundSource.PLAYERS, 0.8f*site.volume*net.jenkimods.bioforge.config.BioForgeClientConfig.stethoscopeVolume(), site.pitch);
        mc.getSoundManager().play(heartSoundInstance);
    }

    private static void playLungSound() {
        Minecraft mc = Minecraft.getInstance();
        SoundEvent event = lungSound == LungSound.CRACKLE
                ? StethoscopeSounds.LUNGS_CRACKLE.get()
                : StethoscopeSounds.LUNGS_NORMAL.get();
        lungSoundInstance = new LoopingSoundInstance(event, SoundSource.PLAYERS, 0.5f*site.volume*net.jenkimods.bioforge.config.BioForgeClientConfig.stethoscopeVolume(), site.pitch);
        mc.getSoundManager().play(lungSoundInstance);
    }

    private static void stopSounds() {
        Minecraft mc = Minecraft.getInstance();
        if (heartSoundInstance != null) {
            mc.getSoundManager().stop(heartSoundInstance);
            heartSoundInstance = null;
        }
        if (lungSoundInstance != null) {
            mc.getSoundManager().stop(lungSoundInstance);
            lungSoundInstance = null;
        }
    }

    public static void stopListening() {
        listening = false;
        hasReading = false;
        targetName = "";
        targetEntityId = -1;
        stopSounds();
    }

    public static boolean isListening() { return listening; }
    public static boolean hasReading() { return hasReading; }
    public static String getTargetName() { return targetName; }
    public static int getCurrentTargetId() { return targetEntityId; }

    private static class LoopingSoundInstance extends AbstractSoundInstance {
        LoopingSoundInstance(SoundEvent source, SoundSource soundSource, float volume, float pitch) {
            super(source.getLocation(), soundSource, SoundInstance.createUnseededRandom());
            this.volume = volume;
            this.pitch = pitch;
            this.looping = true;
            this.attenuation = Attenuation.NONE;
            this.relative = true;
        }
    }
}
