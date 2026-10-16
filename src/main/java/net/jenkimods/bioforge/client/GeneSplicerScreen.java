package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.crispr.CrisprDisplayNames;
import net.jenkimods.bioforge.crispr.StrainSampleUtil;
import net.jenkimods.bioforge.crispr.VaccineTargetCategory;
import net.jenkimods.bioforge.world.splicer.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

public final class GeneSplicerScreen extends AbstractContainerScreen<GeneSplicerMenu> {
    private final java.util.Map<Integer, Button> controls = new java.util.HashMap<>();
    private static final net.minecraft.resources.ResourceLocation BACKGROUND = net.minecraft.resources.ResourceLocation.tryBuild("bioforge", "textures/gui/vaccine_maker/background.png");
    private static final net.minecraft.resources.ResourceLocation SLOT = net.minecraft.resources.ResourceLocation.tryBuild("bioforge", "textures/gui/vaccine_maker/slot.png");

    public GeneSplicerScreen(GeneSplicerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 248; imageHeight = 266; inventoryLabelX = 43; inventoryLabelY = 172;
    }
    private void button(int id, int x, int y, int width, String key) {
        controls.put(id, addRenderableWidget(Button.builder(Component.translatable("gui.bioforge.splicer." + key), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }).bounds(leftPos + x, topPos + y, width, 18).build()));
        controls.get(id).active = false;
    }
    @Override protected void init() {
        super.init();
        controls.clear();
        button(3, 10, 104, 164, "category"); button(4, 178, 104, 28, "previous"); button(5, 210, 104, 28, "next");
        button(7, 10, 126, 72, "examine"); button(6, 86, 126, 72, "recover"); button(0, 166, 126, 72, "extract");
        button(1, 10, 146, 112, "insert"); button(2, 126, 146, 112, "remove");
    }
    @Override protected void containerTick() {
        super.containerTick();
        controls.forEach((id, button) -> button.active = id == 3 || id == 4 || id == 5
                ? menu.data.get(2) == 2 : (menu.data.get(5) & (1 << id)) != 0);
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        GuiRenderCompat.prepare(BACKGROUND);
        g.pose().pushPose(); g.pose().translate(leftPos, topPos, 0); g.pose().scale(1, 168F / 116, 1);
        g.blit(BACKGROUND, 0, 0, 0, 0, 248, 116, 248, 214); g.pose().popPose();
        g.blit(BACKGROUND, leftPos, topPos + 168, 0, 116, 248, 98, 248, 214);
        g.fill(leftPos + 8, topPos + 65, leftPos + 240, topPos + 103, 0xFF397783);
        g.fill(leftPos + 9, topPos + 66, leftPos + 239, topPos + 102, 0xFF0A2A34);
        g.fill(leftPos + 180, topPos + 43, leftPos + 200, topPos + 45, 0xFF8CE9C8);
        g.fill(leftPos + 196, topPos + 40, leftPos + 198, topPos + 48, 0xFF8CE9C8);
        g.fill(leftPos + 198, topPos + 42, leftPos + 200, topPos + 46, 0xFF8CE9C8);
        int duration = menu.data.get(1), progress = menu.data.get(0);
        MachineProgress.draw(g, leftPos + 10, topPos + 58, 228, 4, progress, duration);
        for (var slot : menu.slots) {
            if (slot instanceof net.minecraftforge.items.SlotItemHandler) {
                GuiRenderCompat.prepare(SLOT); g.blit(SLOT, leftPos + slot.x - 1, topPos + slot.y - 1, 0, 0, 18, 19, 18, 19); continue;
            }
        }
        int frame = (int) ((minecraft.level.getGameTime() / 40) % 4);
        net.minecraft.world.item.Item[] samples = {net.jenkimods.bioforge.BioForge.SWAB.get(), net.jenkimods.bioforge.BioForge.VIRUS_SAMPLE.get(), net.jenkimods.bioforge.BioForge.LIVE_CULTURE_VIAL.get(), net.jenkimods.bioforge.BioForge.PETRI_DISH.get()};
        var category = VaccineTargetCategory.values()[Math.floorMod(menu.data.get(4), 3)];
        net.minecraft.world.item.Item[] ghosts = {samples[frame], net.jenkimods.bioforge.BioForge.GENE_IMPRINT.get(), net.jenkimods.bioforge.BioForge.NUTRIENT_MEDIUM.get(), net.jenkimods.bioforge.BioForge.CRISPR_NOTES.get(), GeneSplicerBlockEntity.examinationReagent(category)};
        for (int i = 0; i < ghosts.length; i++) if (!menu.getSlot(i).hasItem()) {
            var slot = menu.getSlot(i); GuiRenderCompat.ghostItem(g, new net.minecraft.world.item.ItemStack(ghosts[i]), leftPos + slot.x, topPos + slot.y);
        }
    }
    private void fitted(GuiGraphics g, Component text, int x, int y, int width, int color) {
        String value = text.getString();
        float scale = Math.min(1F, Math.max(.85F, width / (float) Math.max(1, font.width(value))));
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(scale, scale, 1);
        g.drawString(font, font.plainSubstrByWidth(value, (int) (width / scale)), 0, 0, color, false);
        g.pose().popPose();
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        fitted(g, title, 10, 8, 228, 0xE1FFF5);
        String[] slots = {"sample", "imprint", "medium", "notes", "catalyst", "output"};
        for (int i = 0; i < 6; i++) {
            var lines = font.split(Component.translatable("gui.bioforge.splicer." + slots[i]), 42);
            g.pose().pushPose(); g.pose().translate(menu.getSlot(i).x + 8, 21, 0); g.pose().scale(.75F, .75F, 1);
            for (int row = 0; row < Math.min(2, lines.size()); row++)
                g.drawString(font, lines.get(row), -font.width(lines.get(row)) / 2, row * 9, 0xD1EDF2, false);
            g.pose().popPose();
        }
        int status = menu.data.get(2);
        fitted(g, Component.translatable("gui.bioforge.splicer.status." + status), 12, 67, 224, 0x8CE9C8);
        var sample = menu.getSlot(0).getItem();
        if (status >= 2 && SplicerCulture.analyzed(sample)) {
            var strain = StrainSampleUtil.getStrain(sample);
            var category = VaccineTargetCategory.values()[Math.floorMod(menu.data.get(4), 3)];
            var genes = SplicerCulture.genes(strain, category);
            int selection = genes.isEmpty() ? 0 : Math.floorMod(menu.data.get(3), genes.size());
            String pathogen = strain == null || strain.getPathogenId() == null ? "?" : strain.getPathogenId().getPath();
            fitted(g, Component.translatable("gui.bioforge.splicer.culture", pathogen, SplicerCulture.biomass(sample), SplicerCulture.stability(sample)), 12, 79, 224, 0xCDDEE0);
            if (!SplicerCulture.examined(sample, category)) fitted(g, Component.translatable("gui.bioforge.splicer.study_required", GeneSplicerBlockEntity.examinationReagent(category).getDescription()), 12, 91, 224, 0xF7D490);
            else {
                Component gene = genes.isEmpty() ? Component.translatable("gui.bioforge.splicer.no_genes") : CrisprDisplayNames.target(category, genes.get(selection).target());
                fitted(g, Component.translatable("gui.bioforge.splicer.selected", Component.translatable("gui.bioforge.splicer.category." + category.serializedName()), genes.isEmpty() ? 0 : selection + 1, genes.size(), gene), 12, 91, 224, 0xF7D490);
            }
        } else fitted(g, Component.translatable("gui.bioforge.splicer.analysis_hint"), 12, 84, 224, 0xCDDEE0);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        controls.forEach((id, button) -> button.active = id == 3 || id == 4 || id == 5
                ? menu.data.get(2) == 2 : (menu.data.get(5) & (1 << id)) != 0);
        renderBackground(g); super.render(g, mouseX, mouseY, partial); renderTooltip(g, mouseX, mouseY);
        var help = java.util.Map.of(7, "examine_help", 0, "extract_help", 1, "insert_help", 2, "remove_help", 6, "recover_help");
        for (var entry : help.entrySet()) {
            var button = controls.get(entry.getKey());
            if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                    && mouseY >= button.getY() && mouseY < button.getY() + button.getHeight()) {
                g.renderTooltip(font, font.split(Component.translatable("gui.bioforge.splicer." + entry.getValue()), 240), mouseX, mouseY);
                break;
            }
        }
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 6
                && hoveredSlot.index != GeneSplicerBlockEntity.NOTES) {
            String[] names = {"sample", "imprint", "medium", "notes", "catalyst", "output"};
            g.renderTooltip(font, Component.translatable("gui.bioforge.splicer." + names[hoveredSlot.index]), mouseX, mouseY);
        }
        if (hoveredSlot != null && hoveredSlot.index == GeneSplicerBlockEntity.NOTES)
            g.renderTooltip(font, font.split(Component.translatable("gui.bioforge.splicer.notes_help"), 240), mouseX, mouseY);
        if (mouseY >= topPos + 79 && mouseY < topPos + 103 && mouseX >= leftPos + 9 && mouseX < leftPos + 239 && SplicerCulture.analyzed(menu.getSlot(0).getItem())) {
            var strain = StrainSampleUtil.getStrain(menu.getSlot(0).getItem());
            var lines = new java.util.ArrayList<Component>();
            if (strain != null) {
                String pathogen = strain.getPathogenId() == null ? "?" : strain.getPathogenId().getPath();
                lines.add(Component.translatable("gui.bioforge.splicer.culture", pathogen,
                        SplicerCulture.biomass(menu.getSlot(0).getItem()), SplicerCulture.stability(menu.getSlot(0).getItem())));
                lines.add(Component.translatable("gui.bioforge.splicer.genome_hint"));
                var category = VaccineTargetCategory.values()[Math.floorMod(menu.data.get(4), 3)];
                var genes = SplicerCulture.genes(strain, category);
                lines.add(Component.translatable("gui.bioforge.splicer.category." + category.serializedName()));
                if (SplicerCulture.examined(menu.getSlot(0).getItem(), category) && !genes.isEmpty()) {
                    var gene = genes.get(Math.floorMod(menu.data.get(3), genes.size()));
                    String value = category == VaccineTargetCategory.SYMPTOM ? " = " + strain.getSymptoms().get(gene.target()) : "";
                    lines.add(CrisprDisplayNames.target(category, gene.target()).copy().append(value));
                }
                for (var group : VaccineTargetCategory.values()) lines.add(Component.translatable(
                        "gui.bioforge.splicer.category." + group.serializedName()).append(": " + (SplicerCulture.examined(menu.getSlot(0).getItem(), group) ? SplicerCulture.genes(strain, group).size() : "?")));
            }
            if (!lines.isEmpty()) g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
}
