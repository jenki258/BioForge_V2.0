package net.jenkimods.bioforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.jenkimods.bioforge.BioForge;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = BioForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SculkEchoShaders {
    private SculkEchoShaders() {}

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.tryBuild(BioForge.MODID, "sculk_echo"), DefaultVertexFormat.POSITION_TEX),
                SculkEchoRenderer::setShader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.tryBuild(BioForge.MODID, "symptom_vision"), DefaultVertexFormat.POSITION_TEX),
                SymptomVisionRenderer::setShader);
    }
}
