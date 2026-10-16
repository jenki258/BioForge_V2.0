package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.world.laboratory.LaboratoryProcessorMenu;
import net.jenkimods.bioforge.world.laboratory.LaboratoryStation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class LaboratoryProcessorScreen
        extends AbstractContainerScreen<LaboratoryProcessorMenu> {
    private static final int TEXTURE_SIZE = 256;

    public LaboratoryProcessorScreen(LaboratoryProcessorMenu menu,
                                     Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick,
                            int mouseX, int mouseY) {
        if (menu.station() == LaboratoryStation.HERBAL_MORTAR) {
            renderMortar(graphics);
            return;
        }
        ResourceLocation texture = texture();
        GuiRenderCompat.prepare(texture);
        graphics.blit(texture, leftPos, topPos, 0, 0,
                imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        if (menu.station() == LaboratoryStation.BARREL_PRESS && !menu.getSlot(0).hasItem()) {
            GuiRenderCompat.ghostItem(graphics, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE),
                    leftPos + 26, topPos + 35);
        }
        switch (menu.station()) {
            case BARREL_PRESS -> renderVerticalProgress(graphics, texture,
                    104, 31, 18, 22);
            case CHEMICAL_SYNTHESIZER, HERBAL_MORTAR -> renderHorizontalProgress(graphics, texture,
                    93, 38, 24, 11);
            case PHARMA_MIXER -> renderVerticalProgress(graphics, texture,
                    109, 25, 4, 39);
            case STERILIZATION_CHAMBER -> renderVerticalProgress(graphics, texture,
                    120, 25, 5, 34);
        }
    }

    private void renderMortar(GuiGraphics g) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF555555);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFFFFFFF);
        g.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFFC6C6C6);
        for (var slot : menu.slots) {
            g.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF373737);
            g.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFFFFFFFF);
            g.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF8B8B8B);
        }
        var furnace = ResourceLocation.tryBuild("minecraft", "textures/gui/container/furnace.png");
        GuiRenderCompat.prepare(furnace);
        g.blit(furnace, leftPos + 93, topPos + 34, 79, 34, 24, 17, 256, 256);
        int progress = menu.scaledProgress(24);
        if (progress > 0) g.blit(furnace, leftPos + 93, topPos + 34, 176, 14, progress, 16, 256, 256);
    }

    private ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(BioForge.MODID,
                "textures/gui/laboratory/" + (menu.station() == LaboratoryStation.HERBAL_MORTAR
                        ? "chemical_synthesizer" : menu.station().getSerializedName()) + ".png");
    }

    private void renderHorizontalProgress(GuiGraphics graphics, ResourceLocation texture,
                                          int x, int y, int width, int height) {
        graphics.blit(texture, leftPos + x, topPos + y, 176, 0, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
        int progress = menu.scaledProgress(width);
        if (progress > 0) graphics.blit(texture, leftPos + x, topPos + y,
                176 + width, 0, progress, height, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private void renderVerticalProgress(GuiGraphics graphics, ResourceLocation texture,
                                        int x, int y, int width, int height) {
        graphics.blit(texture, leftPos + x, topPos + y, 176, 0, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
        int progress = menu.scaledProgress(height);
        if (progress > 0) {
            int offset = height - progress;
            graphics.blit(texture, leftPos + x, topPos + y + offset,
                    176 + width, offset, width, progress, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    private int accentColor() {
        return switch (menu.station()) {
            case BARREL_PRESS -> 0xFFC98A3B;
            case CHEMICAL_SYNTHESIZER -> 0xFF4FD8E8;
            case PHARMA_MIXER -> 0xFFD67BEA;
            case STERILIZATION_CHAMBER -> 0xFF70D9A7;
            case HERBAL_MORTAR -> 0xFFDBBA80;
        };
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int color = menu.station() == LaboratoryStation.HERBAL_MORTAR ? 0x404040 : accentColor() & 0xFFFFFF;
        graphics.drawString(font, title, titleLabelX, titleLabelY, color, false);
        graphics.drawString(font, playerInventoryTitle,
                inventoryLabelX, inventoryLabelY, color, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
