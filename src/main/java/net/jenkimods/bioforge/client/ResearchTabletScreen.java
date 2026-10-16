package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.api.guide.ResearchJournalPageView;
import net.jenkimods.bioforge.api.guide.ResearchNavigation;
import net.minecraft.client.gui.components.EditBox;
import java.util.HashSet;
import java.util.Set;
import net.jenkimods.bioforge.api.guide.ResearchJournalRecipeView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ResearchTabletScreen extends Screen {
    private static final ResourceLocation TABLET_TEXTURE = ResourceLocation.tryBuild(
            BioForge.MODID, "textures/gui/research_journal/tablet.png");
    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.tryBuild(
            BioForge.MODID, "textures/gui/vaccine_maker/slot.png");
    private static final int TABLET_WIDTH = 360;
    private static final int TABLET_HEIGHT = 230;
    private static final int SIDEBAR_WIDTH = 136;
    private static final int HEADER_HEIGHT = 30;
    private static final int ENTRY_HEIGHT = 20;
    private static final int TAB_HEIGHT = 14;
    private static final int RECIPE_AREA_HEIGHT = 91;
    private static final int SCROLL_BAR_WIDTH = 5;
    private static final int PANEL = 0xFF071E27;
    private static final int PANEL_INNER = 0xFF0A2A34;
    private static final int PANEL_HOVER = 0xFF0D3A45;
    private static final int PANEL_SELECTED = 0xFF10505E;
    private static final int BORDER = 0xFF397783;
    private static final int CYAN = 0xFF75F4FF;
    private static final int TEXT_CYAN = 0xFFA8F9FF;
    private static final int MUTED_CYAN = 0xFF62B5C0;
    private static final int DISABLED_CYAN = 0xFF39727A;
    private final List<ResearchJournalPageView> pages;
    private int selectedPage;
    private ResearchNavigation.Section section = ResearchNavigation.Section.ALL;
    private static final Set<ResourceLocation> favorites = new HashSet<>();
    private final List<Integer> history = new ArrayList<>();
    private int historyPosition;
    private String query = "";
    private EditBox search;
    private Component navigationTooltip;
    private int navigationScroll;
    private int contentScroll;
    private int navigationMaxScroll;
    private int contentMaxScroll;
    private int recipeScroll;
    private int recipeMaxScroll;
    private ItemStack recipeTooltipStack = ItemStack.EMPTY;
    private int navigationVisibleRows = 1;
    private int contentVisibleLines = 1;
    private ScrollTarget draggedScroll = ScrollTarget.NONE;

    public ResearchTabletScreen(List<ResearchJournalPageView> pages) {
        super(Component.translatable("gui.bioforge.research_journal.title"));
        this.pages = List.copyOf(pages);
        for (int index = 0; index < pages.size(); index++) {
            if (pages.get(index).unlocked()) {
                selectedPage = index;
                break;
            }
        }
    }

    @Override
    protected void init() {
        int left = (width - TABLET_WIDTH) / 2;
        int top = (height - TABLET_HEIGHT) / 2;
        search = addRenderableWidget(new EditBox(font, left + 16, top + 54,
                SIDEBAR_WIDTH - 34, 18, Component.translatable("gui.bioforge.research_journal.search")));
        search.setMaxLength(96);
        search.setHint(Component.translatable("gui.bioforge.research_journal.search"));
        search.setTextColor(TEXT_CYAN);
        search.setValue(query);
        search.setResponder(value -> { query = value; navigationScroll = 0; });
        if (history.isEmpty()) history.add(selectedPage);
    }

    private void selectPage(int index) {
        if (index == selectedPage) return;
        selectedPage = index;
        contentScroll = recipeScroll = 0;
        while (history.size() > historyPosition + 1) history.remove(history.size() - 1);
        history.add(index);
        historyPosition = history.size() - 1;
    }

    private void browseHistory(int direction) {
        int next = historyPosition + direction;
        if (next < 0 || next >= history.size()) return;
        historyPosition = next;
        selectedPage = history.get(next);
        section = ResearchNavigation.Section.ALL;
        search.setValue("");
        contentScroll = recipeScroll = 0;
        navigationScroll = Math.max(0, selectedPage - navigationVisibleRows / 2);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        navigationTooltip = null;
        int left = (width - TABLET_WIDTH) / 2;
        int top = (height - TABLET_HEIGHT) / 2;
        renderTablet(graphics, left, top);
        renderResearchTabs(graphics, left, top, mouseX, mouseY);
        renderNavigation(graphics, left, top, mouseX, mouseY);
        renderPage(graphics, left, top, mouseX, mouseY);
        renderRecipes(graphics, left, top, mouseX, mouseY);
        renderCloseButton(graphics, left, top, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (navigationTooltip != null && !search.isHovered())
            graphics.renderTooltip(font, font.split(navigationTooltip, 230), mouseX, mouseY);
    }

    private void renderTablet(GuiGraphics graphics, int left, int top) {
        boolean hasTexture = minecraft != null && minecraft.getResourceManager()
                .getResource(TABLET_TEXTURE).isPresent();
        if (hasTexture) {
            GuiRenderCompat.prepare(TABLET_TEXTURE);
            graphics.blit(TABLET_TEXTURE, left, top, 0, 0,
                    TABLET_WIDTH, TABLET_HEIGHT, TABLET_WIDTH, TABLET_HEIGHT);
        } else {
            graphics.fill(left, top, left + TABLET_WIDTH, top + TABLET_HEIGHT, 0xFF10171D);
            graphics.fill(left + 4, top + 4, left + TABLET_WIDTH - 4,
                    top + TABLET_HEIGHT - 4, 0xFF26343C);
            graphics.fill(left + 10, top + 10, left + TABLET_WIDTH - 10,
                    top + TABLET_HEIGHT - 10, 0xFFF0F3EC);
            graphics.fill(left + 10, top + 10, left + TABLET_WIDTH - 10,
                    top + HEADER_HEIGHT, 0xFF173F4A);
            graphics.fill(left + SIDEBAR_WIDTH, top + HEADER_HEIGHT,
                    left + SIDEBAR_WIDTH + 1, top + TABLET_HEIGHT - 10, 0xFF8AA0A3);
        }
        graphics.fill(left + 10, top + 10, left + TABLET_WIDTH - 10,
                top + HEADER_HEIGHT, PANEL);
        drawPanel(graphics, left + 10, top + HEADER_HEIGHT + 4,
                left + SIDEBAR_WIDTH, top + TABLET_HEIGHT - 10);
        drawPanel(graphics, left + SIDEBAR_WIDTH + 5, top + HEADER_HEIGHT + 4,
                left + TABLET_WIDTH - 10, top + TABLET_HEIGHT - 10);
        graphics.drawString(font, trim(title, TABLET_WIDTH - 124), left + 18, top + 17, CYAN, false);
    }

    private void renderNavigation(GuiGraphics graphics, int left, int top,
                                  int mouseX, int mouseY) {
        List<ResearchJournalPageView> visiblePages = visiblePages();
        int listTop = navigationListTop(top);
        int listBottom = top + TABLET_HEIGHT - 26;
        int visible = Math.max(1, (listBottom - listTop) / ENTRY_HEIGHT);
        navigationVisibleRows = visible;
        navigationMaxScroll = Math.max(0, visiblePages.size() - visible);
        navigationScroll = Mth.clamp(navigationScroll, 0, navigationMaxScroll);
        for (int row = 0; row < visible; row++) {
            int index = navigationScroll + row;
            if (index >= visiblePages.size()) break;
            ResearchJournalPageView page = visiblePages.get(index);
            int globalIndex = pages.indexOf(page);
            int y = listTop + row * ENTRY_HEIGHT;
            boolean hovered = mouseX >= left + 14 && mouseX < left + SIDEBAR_WIDTH - 5
                    && mouseY >= y && mouseY < y + ENTRY_HEIGHT - 2;
            int background = globalIndex == selectedPage ? PANEL_SELECTED
                    : hovered ? PANEL_HOVER : PANEL_INNER;
            graphics.fill(left + 14, y, left + SIDEBAR_WIDTH - 5,
                    y + ENTRY_HEIGHT - 2, BORDER);
            graphics.fill(left + 15, y + 1, left + SIDEBAR_WIDTH - 6,
                    y + ENTRY_HEIGHT - 3, background);
            Component label = page.title();
            if (hovered) navigationTooltip = page.title().copy().append(page.unlocked() ? Component.empty()
                    : Component.literal(" — ").append(Component.translatable("gui.bioforge.research_journal.locked_short")));
            graphics.drawString(font, trim(label, SIDEBAR_WIDTH - 40),
                    left + 19, y + 5, page.unlocked() ? CYAN : DISABLED_CYAN, false);
        }
        if (visiblePages.isEmpty()) graphics.drawWordWrap(font,
                Component.translatable("gui.bioforge.research_journal.no_results"), left + 19, listTop + 6,
                SIDEBAR_WIDTH - 37, MUTED_CYAN);
        graphics.drawString(font, Component.translatable("gui.bioforge.research_journal.results", visiblePages.size()),
                left + 18, top + TABLET_HEIGHT - 23, MUTED_CYAN, false);
        if (mouseX >= left + 14 && mouseX < left + SIDEBAR_WIDTH && mouseY >= top + TABLET_HEIGHT - 25
                && mouseY < top + TABLET_HEIGHT - 12) navigationTooltip = Component.translatable(
                "gui.bioforge.research_journal.progress", pages.stream().filter(ResearchJournalPageView::unlocked).count(), pages.size());
        renderScrollBar(graphics, navigationScrollX(left), listTop,
                listBottom - listTop, navigationScroll, navigationMaxScroll,
                navigationVisibleRows);
    }

    private void renderResearchTabs(GuiGraphics graphics, int left, int top,
                                    int mouseX, int mouseY) {
        int y = top + HEADER_HEIGHT + 6;
        int x = left + 14, tabWidth = SIDEBAR_WIDTH - 28;
        boolean hovered = mouseX >= x && mouseX < x + tabWidth && mouseY >= y && mouseY < y + TAB_HEIGHT;
        graphics.fill(x, y, x + tabWidth, y + TAB_HEIGHT, BORDER);
        graphics.fill(x + 1, y + 1, x + tabWidth - 1, y + TAB_HEIGHT - 1, hovered ? PANEL_SELECTED : PANEL);
        graphics.drawCenteredString(font, trim(Component.translatable(section.key()), tabWidth - 22), x + tabWidth / 2, y + 3, CYAN);
        graphics.drawString(font, "<", x + 3, y + 3, MUTED_CYAN, false);
        graphics.drawString(font, ">", x + tabWidth - 9, y + 3, MUTED_CYAN, false);
        if (hovered) navigationTooltip = Component.translatable("gui.bioforge.research_journal.category_hint");
        String[] labels = {"<", ">", "H", "*"};
        String[] hints = {"back", "forward", "home", "favorite"};
        for (int i = 0; i < labels.length; i++) {
            int bx = navigationControlsLeft(left) + i * 18;
            boolean enabled = i == 0 ? historyPosition > 0 : i == 1 ? historyPosition + 1 < history.size() : !pages.isEmpty();
            boolean bookmarked = i == 3 && !pages.isEmpty() && favorites.contains(pages.get(selectedPage).id());
            if (bookmarked) {
                graphics.fill(bx - 1, top + 13, bx + 15, top + 28, 0x55FFD577);
                graphics.fill(bx, top + 14, bx + 14, top + 27, 0xFFFFD577);
                graphics.fill(bx + 1, top + 15, bx + 13, top + 26, 0xFF526549);
                graphics.drawString(font, "*", bx + (14 - font.width("*")) / 2 + 1, top + 16, 0xFFFFE49C, false);
            } else renderRecipeNavigationButton(graphics, bx, top + 14, labels[i], enabled, mouseX, mouseY);
            if (mouseX >= bx && mouseX < bx + 14 && mouseY >= top + 14 && mouseY < top + 27)
                navigationTooltip = Component.translatable("gui.bioforge.research_journal." + hints[i]);
        }
    }

    private void renderPage(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        if (pages.isEmpty()) {
            graphics.drawString(font,
                    Component.translatable("gui.bioforge.research_journal.empty"),
                    left + SIDEBAR_WIDTH + 14, top + HEADER_HEIGHT + 14,
                    TEXT_CYAN, false);
            return;
        }
        selectedPage = Mth.clamp(selectedPage, 0, pages.size() - 1);
        ResearchJournalPageView page = pages.get(selectedPage);
        int contentLeft = left + SIDEBAR_WIDTH + 14;
        int contentTop = top + HEADER_HEIGHT + 9;
        int contentWidth = TABLET_WIDTH - SIDEBAR_WIDTH - 34;
        graphics.drawString(font, trim(page.title(), contentWidth), contentLeft,
                contentTop, page.unlocked() ? CYAN : DISABLED_CYAN, false);
        List<FormattedCharSequence> lines = formattedLines(page.body(), contentWidth);
        int recipeArea = !page.recipes().isEmpty()
                ? RECIPE_AREA_HEIGHT : 0;
        int visibleLines = Math.max(1,
                (TABLET_HEIGHT - HEADER_HEIGHT - 42 - recipeArea) / 11);
        contentVisibleLines = visibleLines;
        contentMaxScroll = Math.max(0, lines.size() - visibleLines);
        contentScroll = Mth.clamp(contentScroll, 0, contentMaxScroll);
        int bodyTop = contentTop + 18;
        graphics.enableScissor(contentLeft, bodyTop,
                contentScrollX(left) - 2, bodyTop + visibleLines * 11);
        for (int row = 0; row < visibleLines; row++) {
            int index = contentScroll + row;
            if (index >= lines.size()) break;
            graphics.drawString(font, lines.get(index), contentLeft,
                    bodyTop + row * 11, TEXT_CYAN, false);
        }
        graphics.disableScissor();
        if (lines.size() > visibleLines) {
            Component scroll = Component.translatable(
                    "gui.bioforge.research_journal.scroll_hint");
            graphics.drawString(font, scroll, contentLeft,
                    top + TABLET_HEIGHT - 20 - recipeArea, MUTED_CYAN, false);
        }
        var style = pageStyleAt(mouseX, mouseY);
        if (style != null && style.getClickEvent() != null) {
            var id = ResourceLocation.tryParse(style.getClickEvent().getValue());
            pages.stream().filter(target -> target.id().equals(id)).findFirst()
                    .ifPresent(target -> navigationTooltip = target.title());
        }
        renderScrollBar(graphics, contentScrollX(left), contentTop + 18,
                visibleLines * 11, contentScroll, contentMaxScroll, contentVisibleLines);
    }

    private void renderRecipes(GuiGraphics graphics, int left, int top,
                               int mouseX, int mouseY) {
        recipeMaxScroll = 0;
        recipeTooltipStack = ItemStack.EMPTY;
        if (pages.isEmpty() || minecraft == null || minecraft.level == null) return;
        ResearchJournalPageView page = pages.get(Mth.clamp(selectedPage, 0, pages.size() - 1));
        if (page.recipes().isEmpty()) return;

        List<ResearchJournalRecipeView> recipes = page.recipes();
        if (recipes.isEmpty()) return;

        int x = left + SIDEBAR_WIDTH + 14;
        int y = top + TABLET_HEIGHT - RECIPE_AREA_HEIGHT;
        graphics.fill(x - 5, y - 5, left + TABLET_WIDTH - 14,
                top + TABLET_HEIGHT - 14, BORDER);
        graphics.fill(x - 4, y - 4, left + TABLET_WIDTH - 15,
                top + TABLET_HEIGHT - 15, PANEL);
        graphics.drawString(font, Component.translatable(page.unlocked()
                ? "gui.bioforge.research_journal.recipes"
                : "gui.bioforge.research_journal.unlock_recipe"), x, y, CYAN, false);
        recipeMaxScroll = Math.max(0, recipes.size() - 1);
        recipeScroll = Mth.clamp(recipeScroll, 0, recipeMaxScroll);
        ResearchJournalRecipeView recipe = recipes.get(recipeScroll);
        graphics.drawString(font, trim(recipe.station(), 135), x, y + 10,
                MUTED_CYAN, false);
        int gridX = x + 31;
        int gridY = y + 22;
        renderRecipeGrid(graphics, recipe, gridX, gridY, mouseX, mouseY);
        int arrowX = gridX + 61;
        int arrowY = gridY + 23;
        graphics.fill(arrowX, arrowY + 3, arrowX + 14, arrowY + 5, CYAN);
        graphics.fill(arrowX + 10, arrowY, arrowX + 12, arrowY + 8, CYAN);
        graphics.fill(arrowX + 12, arrowY + 2, arrowX + 14, arrowY + 6, CYAN);

        int resultX = gridX + 82;
        int visibleResults = Math.min(3, recipe.results().size());
        int resultY = gridY + Math.max(0, (54 - visibleResults * 18) / 2);
        for (int resultIndex = 0; resultIndex < visibleResults; resultIndex++) {
            ItemStack result = recipe.results().get(resultIndex);
            int slotY = resultY + resultIndex * 18;
            renderRecipeSlot(graphics, result, resultX, slotY, mouseX, mouseY);
            if (!result.isEmpty()) {
                graphics.drawString(font, trim(result.getHoverName(), TABLET_WIDTH - SIDEBAR_WIDTH - 167), resultX + 22,
                        slotY + 5, TEXT_CYAN, false);
            }
        }
        if (recipeMaxScroll > 0) {
            int previousX = recipePreviousX(left);
            int nextX = recipeNextX(left);
            int navigationY = recipeNavigationY(top);
            renderRecipeNavigationButton(graphics, previousX, navigationY, "<",
                    recipeScroll > 0, mouseX, mouseY);
            renderRecipeNavigationButton(graphics, nextX, navigationY, ">",
                    recipeScroll < recipeMaxScroll, mouseX, mouseY);
            Component counter = Component.literal((recipeScroll + 1) + "/"
                    + (recipeMaxScroll + 1));
            graphics.drawString(font, counter,
                    previousX - 5 - font.width(counter), navigationY + 2,
                    MUTED_CYAN, false);
        }
        if (!recipeTooltipStack.isEmpty()) {
            graphics.renderTooltip(font, recipeTooltipStack, mouseX, mouseY);
        }
    }

    private void renderRecipeGrid(GuiGraphics graphics, ResearchJournalRecipeView recipe, int gridX,
                                  int gridY, int mouseX, int mouseY) {
        List<List<ItemStack>> ingredients = recipe.ingredients();
        int width = Mth.clamp(recipe.width(), 1, 3);
        int height = Mth.clamp(recipe.height(), 1, 3);
        int offsetX = (3 - width) / 2;
        int offsetY = (3 - height) / 2;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                ItemStack display = ItemStack.EMPTY;
                int ingredientIndex = shapedIngredientIndex(
                        column, row, width, height, offsetX, offsetY);
                if (ingredientIndex >= 0 && ingredientIndex < ingredients.size()) {
                    display = cyclingStack(ingredients.get(ingredientIndex),
                            ingredientIndex);
                }
                renderRecipeSlot(graphics, display, gridX + column * 18,
                        gridY + row * 18, mouseX, mouseY);
            }
        }
    }

    private int shapedIngredientIndex(int column, int row, int width, int height,
                                      int offsetX, int offsetY) {
        if (column < offsetX || column >= offsetX + width
                || row < offsetY || row >= offsetY + height) return -1;
        return (row - offsetY) * width + column - offsetX;
    }

    private ItemStack cyclingStack(List<ItemStack> choices, int offset) {
        if (choices.isEmpty()) return ItemStack.EMPTY;
        long cycle = minecraft == null || minecraft.level == null
                ? 0L : minecraft.level.getGameTime() / 30L;
        return choices.get(Math.floorMod((int) cycle + offset, choices.size())).copy();
    }

    private void renderRecipeSlot(GuiGraphics graphics, ItemStack stack, int x, int y,
                                  int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
        if (minecraft != null && minecraft.getResourceManager().getResource(SLOT_TEXTURE).isPresent()) {
            GuiRenderCompat.prepare(SLOT_TEXTURE);
            graphics.blit(SLOT_TEXTURE, x - 1, y - 1, 0, 0, 18, 19, 18, 19);
        } else {
            graphics.fill(x - 1, y - 1, x + 17, y + 18, BORDER);
            graphics.fill(x, y, x + 16, y + 16, PANEL_INNER);
        }
        if (hovered) graphics.fill(x, y, x + 16, y + 16, 0x55FFFFFF);
        if (stack.isEmpty()) return;
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
        if (hovered) recipeTooltipStack = stack;
    }

    private void renderRecipeNavigationButton(GuiGraphics graphics, int x, int y,
                                              String label, boolean enabled,
                                              int mouseX, int mouseY) {
        boolean hovered = enabled && mouseX >= x && mouseX < x + 14
                && mouseY >= y && mouseY < y + 13;
        graphics.fill(x, y, x + 14, y + 13, BORDER);
        graphics.fill(x + 1, y + 1, x + 13, y + 12,
                enabled ? hovered ? PANEL_SELECTED : PANEL_INNER : PANEL);
        graphics.drawString(font, label, x + (14 - font.width(label)) / 2,
                y + 2, enabled ? CYAN : DISABLED_CYAN, false);
    }

    private void renderCloseButton(GuiGraphics graphics, int left, int top,
                                   int mouseX, int mouseY) {
        int x = left + TABLET_WIDTH - 15;
        int y = top + 5;
        boolean hovered = mouseX >= x - 6 && mouseX <= x + 6
                && mouseY >= y && mouseY <= y + 12;
        graphics.drawString(font, "×", x - 3, y + 1,
                hovered ? 0xFFFF8A7A : 0xFFD7E5E2, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int left = (width - TABLET_WIDTH) / 2;
        int top = (height - TABLET_HEIGHT) / 2;
        int closeX = left + TABLET_WIDTH - 15;
        if (mouseX >= closeX - 6 && mouseX <= closeX + 6
                && mouseY >= top + 5 && mouseY <= top + 17) {
            onClose();
            return true;
        }
        var style = pageStyleAt(mouseX, mouseY);
        if (style != null && style.getClickEvent() != null
                && style.getClickEvent().getAction() == net.minecraft.network.chat.ClickEvent.Action.CHANGE_PAGE) {
            var id = ResourceLocation.tryParse(style.getClickEvent().getValue());
            for (int index = 0; index < pages.size(); index++) if (pages.get(index).id().equals(id)) {
                selectPage(index);
                section = ResearchNavigation.Section.ALL; search.setValue("");
                navigationScroll = Math.max(0, index - navigationVisibleRows / 2);
                return true;
            }
        }
        int recipeY = recipeNavigationY(top);
        int previousX = recipePreviousX(left);
        int nextX = recipeNextX(left);
        if (mouseY >= recipeY && mouseY < recipeY + 13) {
            if (mouseX >= previousX && mouseX < previousX + 14 && recipeScroll > 0) {
                recipeScroll--;
                return true;
            }
            if (mouseX >= nextX && mouseX < nextX + 14
                    && recipeScroll < recipeMaxScroll) {
                recipeScroll++;
                return true;
            }
        }
        if (mouseY >= top + 14 && mouseY < top + 27) {
            int start = navigationControlsLeft(left);
            for (int i = 0; i < 4; i++) if (mouseX >= start + i * 18 && mouseX < start + i * 18 + 14) {
                if (i < 2) browseHistory(i == 0 ? -1 : 1);
                else if (i == 2) {
                    section = ResearchNavigation.Section.ALL; search.setValue(""); navigationScroll = 0;
                    for (int j = 0; j < pages.size(); j++) if (pages.get(j).id().getPath().equals("contents")) { selectPage(j); break; }
                } else if (!pages.isEmpty()) {
                    ResourceLocation id = pages.get(selectedPage).id();
                    if (!favorites.remove(id)) favorites.add(id);
                }
                return true;
            }
        }
        int tabY = top + HEADER_HEIGHT + 6;
        int tabX = left + 14, tabWidth = SIDEBAR_WIDTH - 28;
        if (mouseY >= tabY && mouseY < tabY + TAB_HEIGHT && mouseX >= tabX && mouseX < tabX + tabWidth) {
            var sections = ResearchNavigation.Section.values();
            section = sections[Math.floorMod(section.ordinal() + (mouseX < tabX + 16 || hasShiftDown() ? -1 : 1), sections.length)];
            navigationScroll = 0;
            return true;
        }
        List<ResearchJournalPageView> visiblePages = visiblePages();
        int listTop = navigationListTop(top);
        int listBottom = top + TABLET_HEIGHT - 26;
        if (isInsideScrollBar(mouseX, mouseY, navigationScrollX(left), listTop,
                listBottom - listTop)) {
            draggedScroll = ScrollTarget.NAVIGATION;
            navigationScroll = scrollFromPointer(mouseY, listTop, listBottom - listTop,
                    navigationMaxScroll, navigationVisibleRows);
            return true;
        }
        int contentTop = top + HEADER_HEIGHT + 27;
        int contentHeight = contentVisibleLines * 11;
        if (isInsideScrollBar(mouseX, mouseY, contentScrollX(left), contentTop,
                contentHeight)) {
            draggedScroll = ScrollTarget.CONTENT;
            contentScroll = scrollFromPointer(mouseY, contentTop, contentHeight,
                    contentMaxScroll, contentVisibleLines);
            return true;
        }
        if (mouseX >= left + 14 && mouseX < left + SIDEBAR_WIDTH - 5
                && mouseY >= listTop && mouseY < listBottom) {
            int row = ((int) mouseY - listTop) / ENTRY_HEIGHT;
            int index = navigationScroll + row;
            if (row < navigationVisibleRows && index >= 0 && index < visiblePages.size()) {
                selectPage(pages.indexOf(visiblePages.get(index)));
                search.setFocused(false);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int left = (width - TABLET_WIDTH) / 2;
        int top = (height - TABLET_HEIGHT) / 2;
        int direction = delta > 0.0D ? -1 : delta < 0.0D ? 1 : 0;
        if (mouseX < left + SIDEBAR_WIDTH) navigationScroll += direction;
        else if (!pages.isEmpty() && !pages.get(selectedPage).recipes().isEmpty()
                && mouseY >= top + TABLET_HEIGHT - RECIPE_AREA_HEIGHT - 5) {
            recipeScroll += direction;
        }
        else contentScroll += direction;
        return direction != 0 || super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (button == 0 && draggedScroll != ScrollTarget.NONE) {
            int left = (width - TABLET_WIDTH) / 2;
            int top = (height - TABLET_HEIGHT) / 2;
            if (draggedScroll == ScrollTarget.NAVIGATION) {
                int trackTop = navigationListTop(top);
                int trackHeight = top + TABLET_HEIGHT - 26 - trackTop;
                navigationScroll = scrollFromPointer(mouseY, trackTop, trackHeight,
                        navigationMaxScroll, navigationVisibleRows);
            } else {
                int trackTop = top + HEADER_HEIGHT + 27;
                int trackHeight = contentVisibleLines * 11;
                contentScroll = scrollFromPointer(mouseY, trackTop, trackHeight,
                        contentMaxScroll, contentVisibleLines);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggedScroll != ScrollTarget.NONE) {
            draggedScroll = ScrollTarget.NONE;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderScrollBar(GuiGraphics graphics, int x, int y, int height,
                                 int scroll, int maximum, int visible) {
        graphics.fill(x, y, x + SCROLL_BAR_WIDTH, y + height, PANEL);
        int thumbHeight = scrollThumbHeight(height, maximum, visible);
        int travel = Math.max(0, height - thumbHeight);
        int thumbY = maximum == 0 ? y : y + Math.round(travel * (scroll / (float) maximum));
        graphics.fill(x - 1, thumbY, x + SCROLL_BAR_WIDTH + 1,
                thumbY + thumbHeight, maximum == 0 ? DISABLED_CYAN : CYAN);
    }

    private void drawPanel(GuiGraphics graphics, int left, int top, int right, int bottom) {
        graphics.fill(left, top, right, bottom, BORDER);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, PANEL_INNER);
    }

    private boolean isInsideScrollBar(double mouseX, double mouseY, int x, int y,
                                      int height) {
        return mouseX >= x - 2 && mouseX <= x + SCROLL_BAR_WIDTH + 2
                && mouseY >= y && mouseY <= y + height;
    }

    private int scrollFromPointer(double mouseY, int trackTop, int trackHeight,
                                  int maximum, int visible) {
        if (maximum <= 0) return 0;
        int thumbHeight = scrollThumbHeight(trackHeight, maximum, visible);
        int travel = Math.max(1, trackHeight - thumbHeight);
        double position = Mth.clamp(mouseY - trackTop - thumbHeight / 2.0D,
                0.0D, travel);
        return Mth.clamp((int) Math.round(position / travel * maximum), 0, maximum);
    }

    private int scrollThumbHeight(int trackHeight, int maximum, int visible) {
        int total = maximum + visible;
        if (maximum <= 0 || total <= 0) return trackHeight;
        return Mth.clamp(Math.round(trackHeight * (visible / (float) total)),
                12, trackHeight);
    }

    private int navigationScrollX(int left) {
        return left + SIDEBAR_WIDTH - 10;
    }

    private int navigationListTop(int top) {
        return top + 78;
    }
    private int navigationControlsLeft(int left) { return left + TABLET_WIDTH - 95; }

    private List<ResearchJournalPageView> visiblePages() {
        return ResearchNavigation.filter(pages, section, query, favorites);
    }

    private int contentScrollX(int left) {
        return left + TABLET_WIDTH - 18;
    }

    private int recipePreviousX(int left) {
        return left + TABLET_WIDTH - 57;
    }

    private int recipeNextX(int left) {
        return left + TABLET_WIDTH - 39;
    }

    private int recipeNavigationY(int top) {
        return top + TABLET_HEIGHT - 81;
    }

    private List<FormattedCharSequence> formattedLines(Component component, int maxWidth) {
        var normalized = Component.empty();
        component.visit((style, text) -> {
            normalized.append(Component.literal(normalizeText(text)).withStyle(
                    net.jenkimods.bioforge.api.guide.TabletTextPalette.readable(style)));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return font.split(normalized, maxWidth);
    }
    private net.minecraft.network.chat.Style pageStyleAt(double mouseX, double mouseY) {
        if (pages.isEmpty()) return null;
        int left = (width - TABLET_WIDTH) / 2, top = (height - TABLET_HEIGHT) / 2;
        int contentLeft = left + SIDEBAR_WIDTH + 14, bodyTop = top + HEADER_HEIGHT + 27;
        if (mouseX < contentLeft || mouseX >= contentScrollX(left) - 2
                || mouseY < bodyTop || mouseY >= bodyTop + contentVisibleLines * 11) return null;
        var lines = formattedLines(pages.get(selectedPage).body(), TABLET_WIDTH - SIDEBAR_WIDTH - 34);
        int row = (int)(mouseY - bodyTop) / 11 + contentScroll;
        return row < lines.size() ? font.getSplitter().componentStyleAtWidth(lines.get(row), (int)(mouseX - contentLeft)) : null;
    }
    private String normalizeText(String value) {
        return value.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\\n", "\n")
                .replace("\\t", "    ")
                .replace("\t", "    ");
    }

    private Component singleLine(Component component) {
        return Component.literal(normalizeText(component.getString())
                .replace("\n", " ").replaceAll("\\s{2,}", " ").trim());
    }

    private Component trim(Component component, int maxWidth) {
        Component singleLine = singleLine(component);
        if (font.width(singleLine) <= maxWidth) return singleLine;
        return Component.literal(font.substrByWidth(singleLine, maxWidth - font.width("…"))
                .getString() + "…");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum ScrollTarget {
        NONE,
        NAVIGATION,
        CONTENT
    }

}
