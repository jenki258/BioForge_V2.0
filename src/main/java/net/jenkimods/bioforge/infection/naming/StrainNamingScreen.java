package net.jenkimods.bioforge.infection.naming;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class StrainNamingScreen extends Screen {
    private final String fingerprint;
    private EditBox nameField;

    public StrainNamingScreen(String fingerprint) {
        super(Component.translatable("gui.bioforge.strain_naming.title"));
        this.fingerprint = fingerprint;
    }

    @Override
    protected void init() {
        int center = width / 2;
        nameField = new EditBox(font, center - 100, height / 2 - 2, 200, 20,
                Component.translatable("gui.bioforge.strain_naming.field"));
        nameField.setMaxLength(StrainNamingManager.MAX_NAME_LENGTH);
        nameField.setHint(Component.translatable("gui.bioforge.strain_naming.field"));
        addRenderableWidget(nameField);
        addRenderableWidget(Button.builder(
                Component.translatable("gui.bioforge.strain_naming.save"), button -> submit())
                .bounds(center - 100, height / 2 + 28, 96, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.bioforge.strain_naming.later"),
                button -> onClose()).bounds(center + 4, height / 2 + 28, 96, 20).build());
        setInitialFocus(nameField);
    }

    private void submit() {
        String value = nameField.getValue();
        if (StrainNamingManager.sanitizeName(value) == null) return;
        StrainNameNetworkHandler.submit(fingerprint, value);
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && nameField.isFocused()) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int center = width / 2;
        graphics.drawCenteredString(font, title, center, height / 2 - 56, 0xFF75F4FF);
        graphics.drawCenteredString(font,
                Component.translatable("gui.bioforge.strain_naming.description"),
                center, height / 2 - 36, 0xFFC6DCE3);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
