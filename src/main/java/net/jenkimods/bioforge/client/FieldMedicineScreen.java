package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.world.medicine.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;

public final class FieldMedicineScreen extends AbstractContainerScreen<FieldMedicineMenu> {
    public FieldMedicineScreen(FieldMedicineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 176; imageHeight = 188; inventoryLabelX = 8; inventoryLabelY = 94;
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        var background = net.minecraft.resources.ResourceLocation.tryBuild("bioforge", "textures/gui/vaccine_maker/background.png");
        GuiRenderCompat.prepare(background);
        g.pose().pushPose(); g.pose().translate(leftPos, topPos, 0); g.pose().scale(176F / 248F, 90F / 116F, 1);
        g.blit(background, 0, 0, 0, 0, 248, 116, 248, 214); g.pose().popPose();
        // Center crop keeps the baked vanilla inventory aligned with its existing slots.
        g.blit(background, leftPos, topPos + 90, 36, 116, 176, 98, 248, 214);
        var slotTexture = net.minecraft.resources.ResourceLocation.tryBuild("bioforge", "textures/gui/vaccine_maker/slot.png");
        GuiRenderCompat.prepare(slotTexture);
        for (var slot : menu.slots) if (slot instanceof net.minecraftforge.items.SlotItemHandler)
            g.blit(slotTexture, leftPos + slot.x - 1, topPos + slot.y - 1, 0, 0, 18, 19, 18, 19);
        int frame = (int) ((minecraft.level.getGameTime() / 40) % 4);
        Item[] medicines = {BioForge.COUGH_TABLET.get(), BioForge.SNEEZE_TABLET.get(), BioForge.FEVER_TABLET.get(), BioForge.NAUSEA_TABLET.get(), BioForge.VACCINE.get(), BioForge.FIELD_VACCINE.get(), BioForge.STABILIZED_FIELD_VACCINE.get()};
        Item[] ghosts = menu.machine.isDiffuser() ? new Item[]{medicines[(int) ((minecraft.level.getGameTime() / 40) % medicines.length)], Items.WATER_BUCKET}
                : new Item[]{new Item[]{BioForge.SWAB.get(), BioForge.VIRUS_SAMPLE.get(), BioForge.LIVE_CULTURE_VIAL.get(), BioForge.PETRI_DISH.get()}[frame], BioForge.FIELD_VACCINE.get(), frame % 2 == 0 ? Items.SUGAR : Items.GLISTERING_MELON_SLICE};
        for (int i = 0; i < ghosts.length; i++) if (!menu.getSlot(i).hasItem()) {
            var slot = menu.getSlot(i);
            GuiRenderCompat.ghostItem(g, new ItemStack(ghosts[i]), leftPos + slot.x, topPos + slot.y);
        }
        int amount = menu.machine.isDiffuser() ? menu.data.get(4) * 144 / Math.max(1, menu.data.get(5))
                : menu.data.get(0) * 144 / FieldMedicineBlockEntity.CRAFT_TICKS;
        int barY = menu.machine.isDiffuser() ? 73 : 68;
        MachineProgress.draw(g, leftPos + 16, topPos + barY, 144, 4, amount, 144);
    }
    private void label(GuiGraphics g, String key, int x, int y, int width) {
        g.drawString(font, font.plainSubstrByWidth(Component.translatable(key).getString(), width), x, y, 0xD3FFFF, false);
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        g.drawString(font, font.plainSubstrByWidth(title.getString(), 160), 8, 8, 0xD3FFFF, false);
        boolean diffuser = menu.machine.isDiffuser();
        String[] labels = diffuser ? new String[]{"medicine", "water"} : new String[]{"sample", "blank", "sugar"};
        for (int i = 0; i < labels.length; i++) label(g, "gui.bioforge.field." + labels[i], menu.getSlot(i).x - 4, 30, 30);
        label(g, "gui.bioforge.field." + (diffuser ? "bucket" : "result"), menu.getSlot(diffuser ? 2 : 3).x - 4, 30, 36);
        label(g, "gui.bioforge.field.status." + menu.data.get(1), 26, diffuser ? 81 : 79, 142);
        if (diffuser && menu.data.get(4) > 0) g.drawString(font,
                Component.translatable("gui.bioforge.field.cycle", (menu.data.get(4) + 19) / 20), 12, 62, 0xD3FFFF, false);
        g.drawString(font, "?", 8, 80, 0x8CE9C8, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        renderBackground(g); super.render(g, x, y, partial); renderTooltip(g, x, y);
        if (x >= leftPos + 6 && x < leftPos + 20 && y >= topPos + 77 && y < topPos + 91)
            g.renderTooltip(font, font.split(Component.translatable("gui.bioforge.field." + (menu.machine.isDiffuser() ? "diffuser_hint" : "maker_hint")), 220), x, y);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < (menu.machine.isDiffuser() ? 3 : 4)) {
            String key = menu.machine.isDiffuser() ? switch (hoveredSlot.index) { case 0 -> "medicine_help"; case 1 -> "water_help"; default -> "output_help"; }
                    : switch (hoveredSlot.index) { case 0 -> "sample_help"; case 1 -> "blank_help"; case 2 -> "sugar_help"; default -> "output_help"; };
            g.renderTooltip(font, font.split(Component.translatable("gui.bioforge.field." + key), 220), x, y);
        }
    }
}
