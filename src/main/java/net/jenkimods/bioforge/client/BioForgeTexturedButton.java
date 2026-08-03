package net.jenkimods.bioforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;


public final class BioForgeTexturedButton extends Button {
    private final ResourceLocation texture;

    public BioForgeTexturedButton(int x, int y, int width, int height,
                                  Component message, OnPress onPress,
                                  ResourceLocation texture) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.texture = texture;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                float partialTick) {
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty()) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            return;
        }
        int frame = !active ? 2 : isHovered() || isFocused() ? 1 : 0;
        graphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        graphics.blit(texture, getX(), getY(), frame * width, 0,
                width, height, width * 3, height);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        renderString(graphics, Minecraft.getInstance().font,
                active ? 0xFFFFFF : 0xA0A0A0);
    }
}
