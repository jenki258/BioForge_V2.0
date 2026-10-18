package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID, value = Dist.CLIENT)
public final class SculkEchoClientEvents {
    private SculkEchoClientEvents() {}

    @SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        net.jenkimods.bioforge.infection.InfectionClientCache.clear();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SculkEchoRenderer.tick();
            SymptomVisionRenderer.tick();
        }
    }

    @SubscribeEvent
    public static void sound(PlaySoundEvent event) { SculkEchoRenderer.sound(event.getSound()); }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            var view = new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getInverseViewRotationMatrix()).invert();
            SculkEchoRenderer.render(event.getProjectionMatrix(), view,
                    event.getCamera(), event.getPartialTick());
            SymptomVisionRenderer.render(event.getProjectionMatrix(), view,
                    event.getCamera(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog event) {
        if (!SculkEchoRenderer.active()) return;
        event.setNearPlaneDistance(-8.0F);
        event.setFarPlaneDistance(Math.max(48.0F, Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0F));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        if (SculkEchoRenderer.active()) event.setCanceled(true);
    }
}
