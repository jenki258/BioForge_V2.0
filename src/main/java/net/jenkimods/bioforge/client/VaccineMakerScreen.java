package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.api.vaccine.VaccineMakerPageDefinition;
import net.jenkimods.bioforge.api.vaccine.VaccineMakerPageRegistry;
import net.jenkimods.bioforge.client.vaccine.VaccineMakerPageRenderRegistry;
import net.jenkimods.bioforge.client.vaccine.VaccineMakerPageRenderer;
import net.jenkimods.bioforge.client.vaccine.VaccineMakerTabButton;
import net.jenkimods.bioforge.crispr.StrainSampleUtil;
import net.jenkimods.bioforge.infection.StrainData;
import net.jenkimods.bioforge.item.crispr.CasModuleItem;
import net.jenkimods.bioforge.item.crispr.CrisprCartridgeItem;
import net.jenkimods.bioforge.item.crispr.GeneImprintItem;
import net.jenkimods.bioforge.vaccine.VaccineHostProfile;
import net.jenkimods.bioforge.vaccine.MedicalReportStrainBinding;
import net.jenkimods.bioforge.vaccine.VaccineResearchNotes;
import net.jenkimods.bioforge.world.vaccine.VaccineMakerBlockEntity;
import net.jenkimods.bioforge.world.vaccine.VaccineMakerMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VaccineMakerScreen extends AbstractContainerScreen<VaccineMakerMenu> {
    private static final int MAX_VISIBLE_TABS = 7;
    private static final ResourceLocation BACKGROUND_TEXTURE = guiTexture("background");
    private static final ResourceLocation CRISPR_PAGE_TEXTURE = guiTexture("page_crispr");
    private static final ResourceLocation JOURNAL_PAGE_TEXTURE = guiTexture("page_journal");
    private static final ResourceLocation SYNTHESIS_PAGE_TEXTURE = guiTexture("page_synthesis");
    private static final ResourceLocation SLOT_TEXTURE = guiTexture("slot");
    private static final ResourceLocation INFO_TEXTURE = guiTexture("info");
    private static final ResourceLocation PROGRESS_TRACK_TEXTURE = guiTexture("progress_track");
    private static final ResourceLocation PROGRESS_FILL_TEXTURE = guiTexture("progress_fill");
    private static final ResourceLocation ACTION_BUTTON_TEXTURE = guiTexture("button_action");
    private static final ResourceLocation NAVIGATION_BUTTON_TEXTURE = guiTexture("button_navigation");

    private static final int PAGE_TEXTURE_WIDTH = 234;
    private static final int PAGE_TEXTURE_HEIGHT = 102;

    private Button synthesizeButton;
    private Button researchButton;
    private Button previousTabsButton;
    private Button nextTabsButton;
    private final List<VaccineMakerTabButton> tabButtons = new ArrayList<>();
    private final List<HoverArea> pageTooltips = new ArrayList<>();
    private int tabOffset;

    private record HoverArea(int x, int y, int width, int height, Component text) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width
                    && mouseY >= y && mouseY < y + height;
        }
    }

    private record CrisprBaseHit(int cartridgeSlot, int base, int x, int y,
                                 int width, char value) {}

    public VaccineMakerScreen(VaccineMakerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 248;
        imageHeight = 214;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 43;
        inventoryLabelY = 120;
    }

    @Override
    protected void init() {
        super.init();
        synthesizeButton = addRenderableWidget(new BioForgeTexturedButton(
                leftPos + 137, topPos + 88, 99, 20,
                        Component.translatable("gui.bioforge.vaccine_maker.synthesize"),
                        button -> sendMachineButton(VaccineMakerBlockEntity.SYNTHESIZE_BUTTON),
                ACTION_BUTTON_TEXTURE));
        researchButton = addRenderableWidget(new BioForgeTexturedButton(
                leftPos + 137, topPos + 88, 99, 20,
                        Component.translatable("gui.bioforge.vaccine_maker.research"),
                        button -> sendMachineButton(VaccineMakerBlockEntity.RESEARCH_BUTTON),
                ACTION_BUTTON_TEXTURE));

        previousTabsButton = addRenderableWidget(new BioForgeTexturedButton(
                leftPos + 2, topPos - 22, 18, 20, Component.literal("<"),
                        button -> {
                            tabOffset = Math.max(0, tabOffset - 1);
                            updateTabLayout();
                        }, NAVIGATION_BUTTON_TEXTURE));
        nextTabsButton = addRenderableWidget(new BioForgeTexturedButton(
                leftPos + 228, topPos - 22, 18, 20, Component.literal(">"),
                        button -> {
                            tabOffset = Math.min(maxTabOffset(), tabOffset + 1);
                            updateTabLayout();
                        }, NAVIGATION_BUTTON_TEXTURE));

        List<VaccineMakerPageDefinition> pages = menu.getPages();
        for (int index = 0; index < pages.size(); index++) {
            VaccineMakerTabButton tab = new VaccineMakerTabButton(
                    this, pages.get(index), index, leftPos + 8 + index * 30, topPos - 22);
            tabButtons.add(addRenderableWidget(tab));
        }
        updateTabLayout();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateTabLayout();
        ResourceLocation page = menu.getActivePageId();
        ItemStack reagent = menu.getMachineStack(VaccineMakerBlockEntity.REAGENT_SLOT);
        ItemStack sample = menu.getMachineStack(VaccineMakerBlockEntity.SAMPLE_SLOT);
        ItemStack document = menu.getMachineStack(VaccineMakerBlockEntity.REPORT_SLOT);

        synthesizeButton.visible = VaccineMakerPageRegistry.CRAFT.equals(page);
        synthesizeButton.active = synthesizeButton.visible && menu.getStatus() == 1;

        boolean crispr = VaccineMakerPageRegistry.CRISPR.equals(page);
        researchButton.visible = crispr;
        if (crispr) {
            boolean completeProgram = hasCompleteCrisprProgram();
            if (VaccineResearchNotes.isTemplate(document)) {
                researchButton.setMessage(Component.translatable(
                        "gui.bioforge.vaccine_maker.apply_template"));
                researchButton.active = hasAllCartridges();
            } else if (VaccineResearchNotes.canRecord(document)) {
                researchButton.setMessage(Component.translatable(
                        "gui.bioforge.vaccine_maker.write"));
                researchButton.active = completeProgram && !sample.isEmpty();
            } else if (GeneImprintItem.isBlank(reagent)) {
                researchButton.setMessage(Component.translatable(
                        "gui.bioforge.vaccine_maker.extract"));
                researchButton.active = !sample.isEmpty();
            } else {
                researchButton.setMessage(Component.translatable(
                        "gui.bioforge.vaccine_maker.write"));
                researchButton.active = false;
            }
        } else {
            researchButton.active = false;
        }

        VaccineMakerPageRenderer renderer = VaccineMakerPageRenderRegistry.get(page);
        if (renderer != null) renderer.containerTick(this);
    }

    private void updateTabLayout() {
        int pageCount = tabButtons.size();
        boolean overflow = pageCount > MAX_VISIBLE_TABS;
        tabOffset = Mth.clamp(tabOffset, 0, maxTabOffset());
        previousTabsButton.visible = overflow;
        nextTabsButton.visible = overflow;
        previousTabsButton.active = tabOffset > 0;
        nextTabsButton.active = tabOffset < maxTabOffset();

        int startX = leftPos + (overflow ? 22 : 8);
        int tabY = Math.max(0, topPos - 22);
        previousTabsButton.setY(tabY);
        nextTabsButton.setY(tabY);
        for (int index = 0; index < pageCount; index++) {
            VaccineMakerTabButton tab = tabButtons.get(index);
            boolean visible = !overflow
                    || index >= tabOffset && index < tabOffset + MAX_VISIBLE_TABS;
            tab.visible = visible;
            if (visible) {
                tab.setX(startX + (index - tabOffset) * 29);
                tab.setY(tabY);
                tab.active = menu.getActivePageIndex() != index;
            }
        }
    }

    private int maxTabOffset() {
        return Math.max(0, tabButtons.size() - MAX_VISIBLE_TABS);
    }

    public void selectPage(int pageIndex) {
        menu.selectPageLocally(pageIndex);
        sendMachineButton(VaccineMakerMenu.PAGE_BUTTON_BASE + pageIndex);
    }

    public void sendExtensionButton(int buttonId) {
        if (buttonId < VaccineMakerMenu.EXTENSION_BUTTON_BASE) {
            throw new IllegalArgumentException("Extension button IDs must be >= "
                    + VaccineMakerMenu.EXTENSION_BUTTON_BASE);
        }
        sendMachineButton(buttonId);
    }

    private void sendMachineButton(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderPageTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        pageTooltips.clear();
        int left = leftPos;
        int top = topPos;
        if (!blitIfPresent(graphics, BACKGROUND_TEXTURE, left, top,
                imageWidth, imageHeight, imageWidth, imageHeight)) {
            renderFallbackBackground(graphics, left, top);
        }

        ResourceLocation page = menu.getActivePageId();
        if (VaccineMakerPageRegistry.CRISPR.equals(page)) {
            renderCrisprPage(graphics, left, top, mouseX, mouseY);
        } else if (VaccineMakerPageRegistry.JOURNAL.equals(page)) {
            renderJournalPage(graphics, left, top, mouseX, mouseY);
        } else if (VaccineMakerPageRegistry.CRAFT.equals(page)) {
            renderCraftPage(graphics, left, top);
        } else {
            graphics.drawString(font, menu.getActivePage().title().get(),
                    left + 10, top + 20, 0xFF75F4FF, false);
        }

        VaccineMakerPageRenderer renderer = VaccineMakerPageRenderRegistry.get(page);
        if (renderer != null) {
            renderer.renderBackground(this, graphics, left, top, mouseX, mouseY, partialTick);
        }

        boolean texturedSlots = texturePresent(SLOT_TEXTURE);
        for (Slot slot : menu.getActiveMachineSlots()) {
            int color = slot.getSlotIndex() == VaccineMakerBlockEntity.OUTPUT_SLOT
                    ? 0xFF35D99A : slot.getSlotIndex() == VaccineMakerBlockEntity.REPORT_SLOT
                    ? 0xFFFFC95C : 0xFF2F8394;
            drawSlot(graphics, left + slot.x, top + slot.y, color, texturedSlots);
            if (!slot.hasItem()) {
                Component tooltip = emptySlotTooltip(slot.getSlotIndex());
                if (tooltip != null) {
                    addPageTooltip(left + slot.x, top + slot.y,
                            16, 16, tooltip);
                }
            }
        }
    }

    private void renderCrisprPage(GuiGraphics graphics, int left, int top,
                                  int mouseX, int mouseY) {
        if (!blitPageIfPresent(graphics, CRISPR_PAGE_TEXTURE, left, top)) {
            renderFallbackSplitPage(graphics, left, top);
        }
        CrisprBaseHit hoveredBase = findCrisprBase(mouseX, mouseY);
        for (int guide = 0; guide < 3; guide++) {
            int guideColor = guide == 0 ? 0xFF7EF9FF
                    : guide == 1 ? 0xFFFFA65C : 0xFFC68CFF;
            int slotY = top + 18 + guide * 32;
            int sequenceY = top + 36 + guide * 32;
            graphics.fill(left + 6, slotY, left + 8, slotY + 17, guideColor);
            graphics.drawString(font, "g" + (guide + 1), left + 113,
                    slotY + 4, guideColor, false);
            for (int fragment = 0; fragment < 5; fragment++) {
                int cartridgeSlot = guide * 5 + fragment;
                ItemStack cartridge = menu.getMachineStack(cartridgeSlot);
                boolean installed = !cartridge.isEmpty();
                String sequence = installed
                        ? CrisprCartridgeItem.getSequence(cartridge) : "----";
                int cursor = left + 7 + fragment * 24;
                graphics.fill(cursor - 1, sequenceY - 1,
                        cursor + Math.max(22, font.width(sequence)),
                        sequenceY + font.lineHeight, 0xFF04151B);
                for (int base = 0; base < 4; base++) {
                    String letter = sequence.substring(base, base + 1);
                    int letterWidth = font.width(letter);
                    boolean hovered = hoveredBase != null
                            && hoveredBase.cartridgeSlot() == cartridgeSlot
                            && hoveredBase.base() == base;
                    if (hovered) {
                        graphics.fill(cursor - 1, sequenceY - 1,
                                cursor + letterWidth,
                                sequenceY + font.lineHeight, 0xAAE5B94F);
                    }
                    graphics.drawString(font, letter, cursor, sequenceY,
                            hovered ? 0xFFFFFFFF
                                    : installed ? guideColor : 0xFF345A62, false);
                    cursor += letterWidth;
                }
            }
        }

        float quality = Mth.clamp(menu.getQuality(), 0.0f, 1.0f);
        Component qualityText = Component.translatable(
                "gui.bioforge.vaccine_maker.quality.compact",
                formatQuality(quality));
        graphics.drawString(font, qualityText, left + 138, top + 18,
                qualityColor(quality), false);
        addPageTooltip(left + 136, top + 16, font.width(qualityText) + 4,
                font.lineHeight + 3, Component.translatable(
                        "gui.bioforge.vaccine_maker.quality.tooltip",
                        formatQuality(quality), Component.translatable(
                                "gui.bioforge.vaccine_maker.quality."
                                        + qualityGrade(quality))));

        ItemStack cas = menu.getMachineStack(VaccineMakerBlockEntity.CAS_SLOT);
        Component casLabel = Component.translatable(
                "gui.bioforge.vaccine_maker.cas.compact");
        drawFitted(graphics, casLabel, left + 138, top + 32, 43,
                cas.isEmpty() ? 0xFF708B93 : 0xFF80E8FF);
        addPageTooltip(left + 136, top + 30, 45, font.lineHeight + 4,
                casTooltip(cas));

        ItemStack imprint = menu.getMachineStack(VaccineMakerBlockEntity.REAGENT_SLOT);
        Component imprintState = Component.translatable(
                GeneImprintItem.isIdentified(imprint)
                        ? "gui.bioforge.vaccine_maker.crispr.imprint.identified"
                        : GeneImprintItem.isBlank(imprint)
                        ? "gui.bioforge.vaccine_maker.crispr.imprint.blank"
                        : "gui.bioforge.vaccine_maker.crispr.imprint.empty");
        drawFitted(graphics, imprintState, left + 158, top + 66, 26,
                GeneImprintItem.isIdentified(imprint) ? 0xFF67F5D0
                        : GeneImprintItem.isBlank(imprint) ? 0xFFFFCC66 : 0xFF708B93);
        addPageTooltip(left + 157, top + 64, 28, font.lineHeight + 4,
                geneImprintTooltip(imprint));

        ItemStack document = menu.getMachineStack(VaccineMakerBlockEntity.REPORT_SLOT);
        Component documentState = Component.translatable(
                VaccineResearchNotes.isTemplate(document)
                        ? "gui.bioforge.vaccine_maker.crispr.document.template"
                        : VaccineResearchNotes.canRecord(document)
                        ? "gui.bioforge.vaccine_maker.crispr.document.write"
                        : "gui.bioforge.vaccine_maker.crispr.document.empty");
        drawFitted(graphics, documentState, left + 184, top + 66, 52,
                VaccineResearchNotes.isTemplate(document) ? 0xFF67F5D0
                        : VaccineResearchNotes.canRecord(document)
                        ? 0xFFFFCC66 : 0xFF708B93);
        addPageTooltip(left + 183, top + 64, 54, font.lineHeight + 4,
                Component.translatable("gui.bioforge.vaccine_maker.slot.report"));

        drawInfoBadge(graphics, left + 226, top + 18, mouseX, mouseY,
                Component.translatable("gui.bioforge.vaccine_maker.crispr.hint"));

        if (hoveredBase != null) {
            int guide = hoveredBase.cartridgeSlot() / 5 + 1;
            int fragment = hoveredBase.cartridgeSlot() % 5 + 1;
            addPageTooltip(hoveredBase.x() - 1, hoveredBase.y() - 1,
                    hoveredBase.width() + 2, font.lineHeight + 2,
                    Component.translatable(
                            "gui.bioforge.vaccine_maker.crispr.base_tooltip",
                            guide, fragment, hoveredBase.base() + 1,
                            String.valueOf(hoveredBase.value())));
        }
    }

    private void renderJournalPage(GuiGraphics graphics, int left, int top,
                                   int mouseX, int mouseY) {
        if (!blitPageIfPresent(graphics, JOURNAL_PAGE_TEXTURE, left, top)) {
            graphics.fill(left + 6, top + 14, left + 240, top + 116,
                    0xFF071E27);
        }
        ItemStack report = menu.getMachineStack(VaccineMakerBlockEntity.REPORT_SLOT);
        VaccineResearchNotes.Data notes = VaccineResearchNotes.read(report);
        VaccineHostProfile clinical = VaccineHostProfile.fromMedicalReport(report);

        graphics.drawString(font, Component.translatable(
                        "gui.bioforge.vaccine_maker.journal.inputs"),
                left + 10, top + 20, 0xFF75F4FF, false);
        Component state;
        int color;
        if (notes != null) {
            state = Component.translatable(
                    "gui.bioforge.vaccine_maker.journal.readout",
                    notes.sampleFingerprint(),
                    String.format(Locale.ROOT, "%.1f%%", notes.quality() * 100.0f));
            color = 0xFF67F5D0;
        } else if (clinical != null) {
            state = Component.translatable(
                    "gui.bioforge.vaccine_maker.journal.clinical",
                    clinical.findings());
            color = 0xFF80E8FF;
        } else if (VaccineResearchNotes.canRecord(report)) {
            state = Component.translatable(
                    "gui.bioforge.vaccine_maker.journal.ready");
            color = 0xFFFFCC66;
        } else {
            state = Component.translatable(
                    "gui.bioforge.vaccine_maker.journal.empty");
            color = 0xFF708B93;
        }
        drawFitted(graphics, state, left + 10, top + 35, 220, color);
        addPageTooltip(left + 9, top + 33, 224, font.lineHeight + 4, state);
        if (notes != null) {
            drawFitted(graphics, Component.literal("gRNA-1  " + notes.guideOne()),
                    left + 10, top + 52, 218, 0xFFD8FAFF);
            drawFitted(graphics, Component.literal("gRNA-2  " + notes.guideTwo()),
                    left + 10, top + 65, 218, 0xFFD8FAFF);
            drawFitted(graphics, Component.literal("gRNA-3  " + notes.guideThree()),
                    left + 10, top + 78, 218, 0xFFD8FAFF);
        }
        drawInfoBadge(graphics, left + 10, top + 96, mouseX, mouseY,
                Component.translatable(
                        "gui.bioforge.vaccine_maker.journal.description"));
        graphics.drawString(font, Component.translatable(
                        "gui.bioforge.vaccine_maker.details"),
                left + 23, top + 98, 0xFF78C7D3, false);
    }

    private void renderCraftPage(GuiGraphics graphics, int left, int top) {
        if (!blitPageIfPresent(graphics, SYNTHESIS_PAGE_TEXTURE, left, top)) {
            renderFallbackSplitPage(graphics, left, top);
        }
        graphics.fill(left + 6, top + 18, left + 8, top + 35, 0xFF7EF9FF);
        graphics.fill(left + 6, top + 50, left + 8, top + 67, 0xFFFFA65C);
        graphics.fill(left + 6, top + 82, left + 8, top + 99, 0xFFC68CFF);

        int barWidth = 99;
        int filled = menu.getMaxProgress() <= 0 ? 0
                : Mth.clamp(Math.round(barWidth * menu.getProgress()
                / (float) menu.getMaxProgress()), 0, barWidth);
        if (!blitIfPresent(graphics, PROGRESS_TRACK_TEXTURE,
                left + 137, top + 111, 99, 4, 99, 4)) {
            graphics.fill(left + 137, top + 111, left + 236, top + 115,
                    0xFF173842);
        }
        if (filled > 0 && texturePresent(PROGRESS_FILL_TEXTURE)) {
            graphics.blit(PROGRESS_FILL_TEXTURE, left + 137, top + 111,
                    0, 0, filled, 4, 99, 4);
        } else if (filled > 0) {
            graphics.fill(left + 137, top + 111, left + 137 + filled,
                    top + 115, 0xFF60F5E5);
        }

        float quality = Mth.clamp(menu.getQuality(), 0.0f, 1.0f);
        String grade = qualityGrade(quality);
        int qualityColor = qualityColor(quality);
        graphics.drawString(font, Component.translatable(
                        "gui.bioforge.vaccine_maker.quality.compact",
                        formatQuality(quality)),
                left + 137, top + 77, qualityColor, false);
        Component gradeText = Component.translatable(
                "gui.bioforge.vaccine_maker.quality." + grade);
        addPageTooltip(left + 136, top + 75, 52, font.lineHeight + 4,
                Component.translatable("gui.bioforge.vaccine_maker.quality.tooltip",
                        formatQuality(quality), gradeText));

        ItemStack report = menu.getMachineStack(VaccineMakerBlockEntity.REPORT_SLOT);
        StrainData strain = StrainSampleUtil.getStrain(
                menu.getMachineStack(VaccineMakerBlockEntity.SAMPLE_SLOT));
        boolean exactReport = strain != null && MedicalReportStrainBinding.matchesSample(
                report, strain.toPayload());
        VaccineHostProfile profile = exactReport
                ? VaccineHostProfile.fromMedicalReport(report) : null;
        Component researchSummary;
        Component researchDetails;
        int reportColor;
        if (VaccineResearchNotes.isTemplate(report)) {
            boolean exactTemplate = strain != null && VaccineResearchNotes.matchesSample(
                    report, strain.toPayload());
            researchSummary = Component.translatable(exactTemplate
                    ? "gui.bioforge.vaccine_maker.craft.notes.short"
                    : "gui.bioforge.vaccine_maker.craft.notes.mismatch.short");
            researchDetails = Component.translatable(exactTemplate
                    ? "gui.bioforge.vaccine_maker.craft.notes"
                    : "gui.bioforge.vaccine_maker.craft.notes.mismatch");
            reportColor = exactTemplate ? 0xFF67F5D0 : 0xFFFF6B6B;
        } else if (report.isEmpty()) {
            researchSummary = Component.translatable(
                    "gui.bioforge.vaccine_maker.craft.no_report.short");
            researchDetails = Component.translatable(
                    "gui.bioforge.vaccine_maker.craft.no_report");
            reportColor = 0xFFFFCC66;
        } else if (VaccineResearchNotes.canRecord(report)) {
            researchSummary = Component.translatable(
                    "gui.bioforge.vaccine_maker.craft.document.short");
            researchDetails = Component.translatable(
                    "gui.bioforge.vaccine_maker.craft.document");
            reportColor = 0xFFFFCC66;
        } else if (MedicalReportStrainBinding.fingerprint(report) == null) {
            researchSummary = Component.translatable(
                    "gui.bioforge.vaccine_maker.craft.report.unbound.short");
            researchDetails = Component.translatable(
                    "gui.bioforge.vaccine_maker.craft.report.unbound");
            reportColor = 0xFFFFCC66;
        } else if (!exactReport) {
            researchSummary = Component.translatable(
                    "gui.bioforge.vaccine_maker.craft.report.mismatch.short");
            researchDetails = Component.translatable(
                    "gui.bioforge.vaccine_maker.craft.report.mismatch");
            reportColor = 0xFFFF6B6B;
        } else {
            int findings = profile == null ? 0 : profile.findings();
            researchSummary = Component.translatable(
                    "gui.bioforge.vaccine_maker.craft.report.short", findings);
            researchDetails = Component.translatable(
                    "gui.bioforge.vaccine_maker.craft.report", findings);
            reportColor = 0xFF67F5D0;
        }
        drawFitted(graphics, researchSummary, left + 190, top + 77,
                46, reportColor);
        addPageTooltip(left + 188, top + 75, 49,
                font.lineHeight + 4, researchDetails);
    }

    private static String qualityGrade(float quality) {
        return quality >= 0.95f ? "optimal"
                : quality >= 0.80f ? "high"
                : quality >= 0.55f ? "viable"
                : quality >= 0.35f ? "low" : "unstable";
    }

    private static int qualityColor(float quality) {
        return quality >= 0.80f ? 0xFF55F59A
                : quality >= 0.35f ? 0xFFFFCC66 : 0xFFFF6B6B;
    }

    private static String formatQuality(float quality) {
        return String.format(Locale.ROOT, "%.1f%%", quality * 100.0f);
    }

    private void drawCentered(GuiGraphics graphics, String text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private void drawFitted(GuiGraphics graphics, Component component,
                            int x, int y, int maxWidth, int color) {
        String text = component.getString();
        if (font.width(text) > maxWidth) {
            text = font.plainSubstrByWidth(text, Math.max(0, maxWidth - 6)) + "...";
        }
        graphics.drawString(font, text, x, y, color, false);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y, int color,
                                 boolean textured) {
        if (textured) {


            graphics.blit(SLOT_TEXTURE, x - 1, y - 1, 0, 0, 18, 19, 18, 19);
        } else {
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF31505A);
            graphics.fill(x, y, x + 16, y + 16, 0xFF0A222A);
            graphics.fill(x - 1, y + 17, x + 17, y + 18, 0xFF02090D);
        }

        graphics.fill(x - 2, y, x - 1, y + 17, color);
    }

    private void drawInfoBadge(GuiGraphics graphics, int x, int y,
                               int mouseX, int mouseY, Component tooltip) {
        boolean hovered = mouseX >= x && mouseX < x + 10
                && mouseY >= y && mouseY < y + 10;
        if (texturePresent(INFO_TEXTURE)) {
            graphics.blit(INFO_TEXTURE, x, y, hovered ? 10 : 0, 0,
                    10, 10, 20, 10);
            addPageTooltip(x, y, 10, 10, tooltip);
            return;
        }
        int border = hovered ? 0xFF75F4FF : 0xFF397783;
        graphics.fill(x, y, x + 10, y + 10, 0xFF02090D);
        graphics.fill(x + 1, y + 1, x + 9, y + 9,
                hovered ? 0xFF174650 : 0xFF0B2B33);
        graphics.drawString(font, "i", x + 4, y + 1, border, false);
        addPageTooltip(x, y, 10, 10, tooltip);
    }

    private static ResourceLocation guiTexture(String name) {
        return ResourceLocation.tryBuild("bioforge", "textures/gui/vaccine_maker/"
                + name + ".png");
    }

    private boolean texturePresent(ResourceLocation texture) {
        return minecraft != null
                && minecraft.getResourceManager().getResource(texture).isPresent();
    }

    private boolean blitIfPresent(GuiGraphics graphics, ResourceLocation texture,
                                  int x, int y, int width, int height,
                                  int textureWidth, int textureHeight) {
        if (!texturePresent(texture)) return false;
        graphics.blit(texture, x, y, 0, 0, width, height,
                textureWidth, textureHeight);
        return true;
    }

    private boolean blitPageIfPresent(GuiGraphics graphics,
                                      ResourceLocation texture, int left, int top) {
        return blitIfPresent(graphics, texture, left + 6, top + 14,
                PAGE_TEXTURE_WIDTH, PAGE_TEXTURE_HEIGHT,
                PAGE_TEXTURE_WIDTH, PAGE_TEXTURE_HEIGHT);
    }

    private static void renderFallbackBackground(GuiGraphics graphics,
                                                 int left, int top) {
        graphics.fill(left, top, left + 248, top + 214, 0xFF08151C);
        graphics.fill(left + 3, top + 3, left + 245, top + 211, 0xFF102932);
        graphics.fill(left + 6, top + 14, left + 240, top + 116, 0xFF071E27);
        graphics.fill(left + 39, top + 127, left + 209, top + 211, 0xFF071E27);
    }

    private static void renderFallbackSplitPage(GuiGraphics graphics,
                                                int left, int top) {
        graphics.fill(left + 6, top + 14, left + 130, top + 112, 0xFF071E27);
        graphics.fill(left + 134, top + 14, left + 240, top + 112, 0xFF0A222A);
    }

    private void addPageTooltip(int x, int y, int width, int height,
                                Component text) {
        pageTooltips.add(new HoverArea(x, y, Math.max(1, width),
                Math.max(1, height), text));
    }

    private void renderPageTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int index = pageTooltips.size() - 1; index >= 0; index--) {
            HoverArea area = pageTooltips.get(index);
            if (area.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, font.split(area.text(), 210),
                        mouseX, mouseY);
                return;
            }
        }
    }

    private Component emptySlotTooltip(int logicalSlot) {
        if (logicalSlot >= 0 && logicalSlot < 15) {
            return Component.translatable(
                    "gui.bioforge.vaccine_maker.slot.cartridge",
                    logicalSlot / 5 + 1, logicalSlot % 5 + 1);
        }
        return switch (logicalSlot) {
            case VaccineMakerBlockEntity.CAS_SLOT -> Component.translatable(
                    "gui.bioforge.vaccine_maker.slot.cas");
            case VaccineMakerBlockEntity.SAMPLE_SLOT -> Component.translatable(
                    "gui.bioforge.vaccine_maker.slot.sample");
            case VaccineMakerBlockEntity.CARRIER_SLOT -> Component.translatable(
                    "gui.bioforge.vaccine_maker.slot.carrier");
            case VaccineMakerBlockEntity.REAGENT_SLOT -> Component.translatable(
                    "gui.bioforge.vaccine_maker.slot.reagent");
            case VaccineMakerBlockEntity.OUTPUT_SLOT -> Component.translatable(
                    "gui.bioforge.vaccine_maker.slot.output");
            case VaccineMakerBlockEntity.REPORT_SLOT -> Component.translatable(
                    "gui.bioforge.vaccine_maker.slot.report");
            default -> null;
        };
    }

    private CrisprBaseHit findCrisprBase(double mouseX, double mouseY) {
        for (int guide = 0; guide < 3; guide++) {
            int textY = topPos + 36 + guide * 32;
            if (mouseY < textY - 1 || mouseY >= textY + font.lineHeight + 1) {
                continue;
            }
            for (int fragment = 0; fragment < 5; fragment++) {
                int cursor = leftPos + 7 + fragment * 24;
                int cartridgeSlot = guide * 5 + fragment;
                ItemStack cartridge = menu.getMachineStack(cartridgeSlot);
                String sequence = cartridge.isEmpty()
                        ? "----" : CrisprCartridgeItem.getSequence(cartridge);
                for (int base = 0; base < 4; base++) {
                    char value = sequence.charAt(base);
                    int width = Math.max(1, font.width(String.valueOf(value)));
                    if (!cartridge.isEmpty()
                            && mouseX >= cursor && mouseX < cursor + width) {
                        return new CrisprBaseHit(cartridgeSlot, base, cursor,
                                textY, width, value);
                    }
                    cursor += width;
                }
            }
        }
        return null;
    }

    private boolean hasCompleteCrisprProgram() {
        for (int slot = VaccineMakerBlockEntity.CARTRIDGE_START;
             slot < VaccineMakerBlockEntity.CARTRIDGE_END; slot++) {
            if (!(menu.getMachineStack(slot).getItem() instanceof CrisprCartridgeItem)) {
                return false;
            }
        }
        return !menu.getMachineStack(VaccineMakerBlockEntity.CAS_SLOT).isEmpty();
    }

    private boolean hasAllCartridges() {
        for (int slot = VaccineMakerBlockEntity.CARTRIDGE_START;
             slot < VaccineMakerBlockEntity.CARTRIDGE_END; slot++) {
            if (!(menu.getMachineStack(slot).getItem() instanceof CrisprCartridgeItem)) {
                return false;
            }
        }
        return true;
    }

    private Component casTooltip(ItemStack stack) {
        if (stack.isEmpty()) {
            return Component.translatable("gui.bioforge.vaccine_maker.cas.empty");
        }
        ResourceLocation id = CasModuleItem.getModuleId(stack);
        CasModuleItem.DisplayData display = CasModuleItem.getDisplayData(stack);
        if (display != null) {
            return Component.translatable(
                        "gui.bioforge.vaccine_maker.cas.details",
                        CasModuleItem.getModuleName(stack), display.pam(),
                        String.format(Locale.ROOT, "%.0f%%",
                                display.efficiency() * 100.0f),
                        display.pathogens().isBlank()
                                ? Component.translatable(
                                "item.bioforge.cas_module.pathogens.any")
                                : display.pathogens());
        }
        return Component.translatable(
                "gui.bioforge.vaccine_maker.cas.unknown",
                CasModuleItem.getModuleName(stack));
    }

    private Component geneImprintTooltip(ItemStack stack) {
        GeneImprintItem.Data data = GeneImprintItem.read(stack);
        if (data == null) {
            return Component.translatable(
                    "gui.bioforge.vaccine_maker.crispr.imprint.help");
        }
        if (!data.identified()) {
            return Component.translatable(
                    "gui.bioforge.vaccine_maker.crispr.imprint.unidentified");
        }
        return Component.translatable(
                "gui.bioforge.vaccine_maker.crispr.imprint.details",
                Component.translatable("vaccine.category."
                        + data.category().serializedName()),
                net.jenkimods.bioforge.crispr.CrisprDisplayNames.target(
                        data.category(), data.target()));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF75F4FF, false);
        Component pageTitle = menu.getActivePage().title().get();
        drawFitted(graphics, pageTitle, 142, titleLabelY, 96, 0xFF60C6D6);
        addPageTooltip(leftPos + 141, topPos + titleLabelY - 1,
                98, font.lineHeight + 2, pageTitle);
        graphics.drawString(font, playerInventoryTitle,
                inventoryLabelX, inventoryLabelY, 0xFF75F4FF, false);

        Component status = pageStatus();
        Component shortStatus = pageStatusShort();
        int statusColor = menu.getStatus() == 3 || menu.getStatus() == 4
                || menu.getStatus() == 5 ? 0xFFFF6868 : 0xFF78C7D3;
        drawFitted(graphics, shortStatus, 150, inventoryLabelY,
                88, statusColor);
        addPageTooltip(leftPos + 148, topPos + inventoryLabelY - 2,
                92,
                font.lineHeight + 4, status);

        VaccineMakerPageRenderer renderer =
                VaccineMakerPageRenderRegistry.get(menu.getActivePageId());
        if (renderer != null) renderer.renderLabels(this, graphics, mouseX, mouseY);
    }

    private Component pageStatus() {
        ResourceLocation page = menu.getActivePageId();
        ItemStack reagent = menu.getMachineStack(VaccineMakerBlockEntity.REAGENT_SLOT);
        ItemStack report = menu.getMachineStack(VaccineMakerBlockEntity.REPORT_SLOT);
        if (VaccineMakerPageRegistry.CRISPR.equals(page)) {
            if (VaccineResearchNotes.isTemplate(report)) {
                return Component.translatable(
                        "gui.bioforge.vaccine_maker.status.template");
            }
            if (VaccineResearchNotes.canRecord(report)) {
                return Component.translatable(
                        "gui.bioforge.vaccine_maker.status.write");
            }
            return GeneImprintItem.isBlank(reagent)
                    ? Component.translatable("gui.bioforge.vaccine_maker.status.extract")
                    : Component.translatable("gui.bioforge.vaccine_maker.status.crispr");
        }
        if (VaccineMakerPageRegistry.JOURNAL.equals(page)) {
            return VaccineResearchNotes.canRecord(report)
                    ? Component.translatable("gui.bioforge.vaccine_maker.status.write")
                    : Component.translatable("gui.bioforge.vaccine_maker.status.journal");
        }
        if (VaccineMakerPageRegistry.CRAFT.equals(page)) {
            return Component.translatable(
                    "gui.bioforge.vaccine_maker.status." + menu.getStatus());
        }
        return menu.getActivePage().title().get();
    }

    private Component pageStatusShort() {
        ResourceLocation page = menu.getActivePageId();
        ItemStack reagent = menu.getMachineStack(VaccineMakerBlockEntity.REAGENT_SLOT);
        ItemStack report = menu.getMachineStack(VaccineMakerBlockEntity.REPORT_SLOT);
        if (VaccineMakerPageRegistry.CRISPR.equals(page)) {
            if (VaccineResearchNotes.isTemplate(report)) {
                return Component.translatable(
                        "gui.bioforge.vaccine_maker.status.short.template");
            }
            if (VaccineResearchNotes.canRecord(report)) {
                return Component.translatable(
                        "gui.bioforge.vaccine_maker.status.short.write");
            }
            return Component.translatable(GeneImprintItem.isBlank(reagent)
                    ? "gui.bioforge.vaccine_maker.status.short.extract"
                    : "gui.bioforge.vaccine_maker.status.short.crispr");
        }
        if (VaccineMakerPageRegistry.JOURNAL.equals(page)) {
            return Component.translatable(VaccineResearchNotes.canRecord(report)
                    ? "gui.bioforge.vaccine_maker.status.short.write"
                    : "gui.bioforge.vaccine_maker.status.short.journal");
        }
        if (VaccineMakerPageRegistry.CRAFT.equals(page)) {
            return Component.translatable(
                    "gui.bioforge.vaccine_maker.status.short." + menu.getStatus());
        }
        return menu.getActivePage().title().get();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        VaccineMakerPageRenderer renderer =
                VaccineMakerPageRenderRegistry.get(menu.getActivePageId());
        if (renderer != null && renderer.mouseClicked(this, mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        VaccineMakerPageRenderer renderer =
                VaccineMakerPageRenderRegistry.get(menu.getActivePageId());
        if (renderer != null && renderer.mouseScrolled(this, mouseX, mouseY, delta)) {
            return true;
        }
        if (VaccineMakerPageRegistry.CRISPR.equals(menu.getActivePageId())) {
            CrisprBaseHit hit = findCrisprBase(mouseX, mouseY);
            if (hit != null) {
                int buttonId = hit.cartridgeSlot() * 4 + hit.base()
                        + (delta < 0 ? 64 : 0);
                sendMachineButton(buttonId);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    public VaccineMakerMenu getMenuView() {
        return menu;
    }

    public Font getFontView() {
        return font;
    }

    public int getGuiLeftView() {
        return leftPos;
    }

    public int getGuiTopView() {
        return topPos;
    }
}
