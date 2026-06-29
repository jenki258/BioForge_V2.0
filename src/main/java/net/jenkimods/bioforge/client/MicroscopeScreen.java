package net.jenkimods.bioforge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.MicroscopeVisibility;
import net.jenkimods.bioforge.world.microscope.MicroscopeClientData;
import net.jenkimods.bioforge.world.microscope.MicroscopeMenu;
import net.jenkimods.bioforge.world.microscope.MicroscopeSymptomConfig;
import net.jenkimods.bioforge.world.microscope.MicroscopeSymptomEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Map;

public class MicroscopeScreen extends AbstractContainerScreen<MicroscopeMenu> {

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.tryBuild(BioForge.MODID, "textures/gui/microscope.png");
    private static final ResourceLocation EMPTY_ICON =
            ResourceLocation.tryBuild(BioForge.MODID, "textures/gui/microscope/empty.png");

    private static final int GRID_X = 8, GRID_Y = 17, CELL_W = 19, CELL_H = 28, COLS = 3, ICON_SIZE = 16;
    private static final int GRID_W = COLS * CELL_W, GRID_H = 2 * CELL_H;

    private int scrollOffset = 0;
    private boolean isScrolling = false;

    public MicroscopeScreen(MicroscopeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverScrollbar(mouseX, mouseY)) {
            isScrolling = true;
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isScrolling) {
            isScrolling = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (isScrolling) return true;
        if (isMouseInGrid(mx, my)) {
            List<MicroscopeSymptomEntry> entries = MicroscopeClientData.getEntries();
            int totalRows = (int) Math.ceil((double) entries.size() / COLS);
            int visibleRows = GRID_H / CELL_H;
            int maxScroll = Math.max(0, totalRows - visibleRows);
            scrollOffset = (int) Math.min(Math.max(scrollOffset - delta, 0), maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private boolean isMouseOverScrollbar(double mx, double my) {
        int totalRows = (int) Math.ceil((double) MicroscopeClientData.getEntries().size() / COLS);
        int visibleRows = GRID_H / CELL_H;
        if (totalRows <= visibleRows) return false;
        int sx = leftPos + GRID_X;
        int sy = topPos + GRID_Y;
        int sbx = sx + GRID_W + 4;
        int sbw = 2;
        return mx >= sbx && mx <= sbx + sbw && my >= sy && my <= sy + GRID_H;
    }

    private void updateScrollFromMouse(double mouseY) {
        int totalRows = (int) Math.ceil((double) MicroscopeClientData.getEntries().size() / COLS);
        int visibleRows = GRID_H / CELL_H;
        int maxScroll = Math.max(0, totalRows - visibleRows);
        int sy = topPos + GRID_Y;
        int sbh = GRID_H;
        double fraction = (mouseY - sy) / (double) sbh;
        fraction = Math.min(1.0, Math.max(0.0, fraction));
        scrollOffset = (int) Math.round(fraction * maxScroll);
    }

    private boolean isMouseInGrid(double mx, double my) {
        return mx >= leftPos + GRID_X && mx <= leftPos + GRID_X + GRID_W
                && my >= topPos  + GRID_Y && my <= topPos  + GRID_Y + GRID_H;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderSymptomGrid(g, mx, my);
        renderTooltip(g, mx, my);

        if (isScrolling) {
            updateScrollFromMouse(my);
        }
    }

    private void renderSymptomGrid(GuiGraphics g, int mouseX, int mouseY) {
        List<MicroscopeSymptomEntry> entries = MicroscopeClientData.getEntries();
        Map<String, Object> values = MicroscopeClientData.getSymptoms();
        MicroscopeVisibility slideVis = MicroscopeVisibility.fromName(MicroscopeClientData.getVisibility());

        int visibleRows = GRID_H / CELL_H;
        int sx = leftPos + GRID_X, sy = topPos + GRID_Y, ey = sy + GRID_H;

        g.enableScissor(sx, sy, sx + GRID_W, ey);

        for (int row = scrollOffset; row < scrollOffset + visibleRows; row++) {
            int y = sy + (row - scrollOffset) * CELL_H;
            for (int col = 0; col < COLS; col++) {
                int idx = row * COLS + col;
                if (idx >= entries.size()) break;

                MicroscopeSymptomEntry entry = entries.get(idx);
                int cx = sx + col * CELL_W;

                boolean visible = slideVis.ordinal() >= entry.minVisibility().ordinal();
                ResourceLocation icon = EMPTY_ICON;

                if (visible) {
                    Object value = values.get(entry.symptomKey());
                    if (value != null) {
                        if (entry.isEnum() && entry.stateIcons() != null) {
                            String stateName = value.toString().toLowerCase();
                            icon = entry.stateIcons().getOrDefault(stateName, entry.icon());
                        } else if (entry.isBoolean()) {
                            icon = (Boolean) value ? entry.icon() : EMPTY_ICON;
                        } else {
                            icon = entry.icon();
                        }
                    }
                }

                RenderSystem.setShaderTexture(0, icon);
                g.blit(icon, cx + 3, y + 1, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
        }

        g.disableScissor();

        int totalRows = (int) Math.ceil((double) entries.size() / COLS);
        if (totalRows > visibleRows) {
            int sbx = sx + GRID_W + 4;
            int sbh = GRID_H;
            int hh = Math.max(6, sbh * visibleRows / totalRows);
            int hy = sy + (sbh - hh) * scrollOffset / (totalRows - visibleRows);
            g.fill(sbx, sy, sbx + 2, sy + sbh, 0x44FFFFFF);
            g.fill(sbx, hy, sbx + 2, hy + hh, 0xFFFFFFFF);
        }

        if (isMouseInGrid(mouseX, mouseY)) {
            int col = (mouseX - sx) / CELL_W;
            int row = scrollOffset + (mouseY - sy) / CELL_H;
            int idx = row * COLS + col;
            if (idx >= 0 && idx < entries.size()) {
                MicroscopeSymptomEntry entry = entries.get(idx);
                boolean visible = slideVis.ordinal() >= entry.minVisibility().ordinal();
                String tipText = null;

                if (!visible) {
                    tipText = "-";
                } else {
                    Object value = values.get(entry.symptomKey());
                    if (value == null || (entry.isBoolean() && !(Boolean) value)) {
                        tipText = "-";
                    } else {
                        String nameKey = "microscope.symptom." + entry.symptomKey();
                        String translatedName = Component.translatable(nameKey).getString();
                        if (translatedName.equals(nameKey)) {
                            translatedName = entry.symptomKey();
                        }
                        String stateText = "";
                        if (entry.isEnum()) {
                            String state = value.toString().toLowerCase();
                            String stateKey = "microscope.symptom." + entry.symptomKey() + "." + state;
                            stateText = Component.translatable(stateKey).getString();
                            if (stateText.equals(stateKey)) {
                                stateText = state;
                            }
                        } else if (value instanceof Float f) {
                            if (entry.displayPercentage()) {
                                stateText = String.format("%.0f%%", f * 100);
                            } else {
                                stateText = String.format("%.0f", f);
                            }
                        }
                        tipText = translatedName + ": " + stateText;
                    }
                }

                if (tipText != null) {
                    g.renderTooltip(font, Component.literal(tipText), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        g.blit(GUI_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}