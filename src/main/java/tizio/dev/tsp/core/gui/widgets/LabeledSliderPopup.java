package tizio.dev.tsp.core.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tizio.dev.tsp.core.gui.theme.SystemEditorTheme;

import java.util.Locale;
import java.util.function.Consumer;

public class LabeledSliderPopup extends Screen {
    private final Screen parentScreen;
    private final String labelPrefix;
    private final double currentValue;
    private final double min;
    private final double max;
    private final Consumer<Double> onConfirm;

    private EditBox inputField;

    public LabeledSliderPopup(Screen parentScreen, String labelPrefix, double currentValue, double min, double max, Consumer<Double> onConfirm) {
        super(Component.literal("Enter Precise Value"));
        this.parentScreen = parentScreen;
        this.labelPrefix = labelPrefix;
        this.currentValue = currentValue;
        this.min = min;
        this.max = max;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int dialogW = 220;
        int dialogH = 100;
        int dialogX = (this.width - dialogW) / 2;
        int dialogY = (this.height - dialogH) / 2;

        inputField = new EditBox(this.font, dialogX + 15, dialogY + 38, dialogW - 30, 20, Component.literal("Value"));
        inputField.setValue(String.format(Locale.ROOT, "%.4f", currentValue));
        inputField.setFocused(true);
        setInitialFocus(inputField);

        addRenderableWidget(inputField);
        addRenderableWidget(Button.builder(Component.literal("Confirm"), b -> confirmValue()).bounds(dialogX + 15, dialogY + 68, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> closePrompt()).bounds(dialogX + 115, dialogY + 68, 90, 20).build());

    }

    private void confirmValue() {
        try {
            double parsed = Double.parseDouble(inputField.getValue().trim());
            double clamped = Math.max(min, Math.min(max, parsed));
            if (onConfirm != null) {
                onConfirm.accept(clamped);
            }
        } catch (NumberFormatException ignored) {}
        closePrompt();
    }

    private void closePrompt() {
        Minecraft.getInstance().setScreen(parentScreen);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            confirmValue();
            return true;
        }
        if (keyCode == 256) {
            closePrompt();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (parentScreen != null) {
            parentScreen.render(guiGraphics, -1, -1, partialTick);
        }
        guiGraphics.fill(0, 0, this.width, this.height, 0x77000000);

        int dialogW = 220;
        int dialogH = 100;
        int dialogX = (this.width - dialogW) / 2;
        int dialogY = (this.height - dialogH) / 2;

        guiGraphics.fill(dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, 0xD010121A);
        guiGraphics.fill(dialogX, dialogY, dialogX + dialogW, dialogY + 2, 0x44AAAAAA);
        guiGraphics.fill(dialogX, dialogY + dialogH - 2, dialogX + dialogW, dialogY + dialogH, 0x44AAAAAA);

        guiGraphics.drawString(this.font, "Set Value: " + labelPrefix, dialogX + 15, dialogY + 10, 0x00FFCC, SystemEditorTheme.TEXT_SHADOW);
        guiGraphics.drawString(this.font, String.format(Locale.ROOT, "Range: [%.1f - %.1f]", min, max), dialogX + 15, dialogY + 24, 0xAAAAAA, SystemEditorTheme.TEXT_SHADOW);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
