package net.jenkimods.bioforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.jenkimods.bioforge.BioForge;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BioForge.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BioForgeKeyBindings {
    public static final KeyMapping REFLEX_STRIKE = new KeyMapping(
            "key.bioforge.reflex_strike",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.bioforge"
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(REFLEX_STRIKE);
    }
}