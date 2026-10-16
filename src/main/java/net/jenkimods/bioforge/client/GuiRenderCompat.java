package net.jenkimods.bioforge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public final class GuiRenderCompat {
    private GuiRenderCompat() {}

    public static void prepare(ResourceLocation texture) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, texture);
    }

    public static void ghostItem(net.minecraft.client.gui.GuiGraphics graphics,
                                 net.minecraft.world.item.ItemStack stack, int x, int y) {
        var sprite = net.minecraft.client.Minecraft.getInstance().getItemRenderer()
                .getModel(stack, null, null, 0).getParticleIcon();
        // Draw an atlas sprite with vertex alpha. renderItem uses opaque item
        // buffers, so tinting/covering it afterwards does not make a true ghost.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(x, y, 0, 16, 16, sprite, .7F, .85F, 1F, .25F);
    }
}
