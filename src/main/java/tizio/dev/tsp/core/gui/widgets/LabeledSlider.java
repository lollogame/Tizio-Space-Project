package tizio.dev.tsp.core.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tizio.dev.tsp.core.gui.theme.SystemEditorTheme;
import tizio.dev.tsp.core.utils.Utils;

import java.util.Locale;
import java.util.function.Consumer;

public class LabeledSlider extends AbstractSliderButton {
    private final String labelPrefix;
    private final double min;
    private final double max;
    private final double defaultValue;
    private final Consumer<Double> onChange;

    public LabeledSlider(int x, int y, int width, int height, String labelPrefix, double initialValue, double defaultValue, double min, double max, Consumer<Double> onChange) {
        super(x, y, width, height, Component.empty(), (initialValue - min) / (max - min));
        this.labelPrefix = labelPrefix;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        updateMessage();
    }

    public LabeledSlider(int x, int y, int width, int height, String labelPrefix, double initialValue, double min, double max, Consumer<Double> onChange) {
        this(x, y, width, height, labelPrefix, initialValue, initialValue, min, max, onChange);
    }

    private int getTrackX1() {
        Font font = Minecraft.getInstance().font;
        int labelWidth = font.width(labelPrefix);
        return getX() + labelWidth + 8;
    }

    private int getTrackX2() {
        Font font = Minecraft.getInstance().font;
        String valStr = String.format(Locale.ROOT, "%.2f", getValue());
        String maxStr = String.format(Locale.ROOT, "%.2f", max);
        int valWidth = Math.max(font.width(valStr), font.width(maxStr));
        return getX() + width - valWidth - 8;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.clicked(mouseX, mouseY)) {
            if (button == 1) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                Screen currentScreen = Minecraft.getInstance().screen;
                Minecraft.getInstance().setScreen(new LabeledSliderPopup(currentScreen, labelPrefix, getValue(), min, max, this::setValue));
                return true;
            } else if (button == 0) {
                onClick(mouseX, mouseY);
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        updateValueFromMouse(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        updateValueFromMouse(mouseX);
    }

    private void updateValueFromMouse(double mouseX) {
        int t1 = getTrackX1();
        int t2 = getTrackX2();
        if (t2 > t1) {
            double newValue = (mouseX - t1) / (double) (t2 - t1);
            this.value = Utils.clamp(newValue, 0.0, 1.0);
            updateMessage();
            applyValue();
        }
    }

    @Override
    protected void updateMessage() {
        double val = getValue();
        setMessage(Component.literal(String.format(Locale.ROOT, "%s: %.2f", labelPrefix, val)));
    }

    @Override
    protected void applyValue() {
        if (onChange != null) {
            onChange.accept(getValue());
        }
    }

    public double getValue() {
        return min + this.value * (max - min);
    }

    public void setValue(double newValue) {
        double clamped = Utils.clamp(newValue, min, max);
        this.value = (clamped - min) / (max - min);
        updateMessage();
        applyValue();
    }

    public void resetToDefault() {
        setValue(defaultValue);
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;

        boolean hovered = this.isHoveredOrFocused();
        int borderColor = hovered ? SystemEditorTheme.SLIDER_BORDER_HOVER : SystemEditorTheme.SLIDER_BORDER_NORMAL;
        int bgColor     = hovered ? SystemEditorTheme.SLIDER_CONTAINER_HOVER_BG : SystemEditorTheme.SLIDER_CONTAINER_BG;

        g.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
        g.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
        g.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);

        Font font = Minecraft.getInstance().font;

        g.drawString(font, labelPrefix, getX() + 4, getY() + (height - 8) / 2, SystemEditorTheme.SLIDER_LABEL_TEXT, SystemEditorTheme.TEXT_SHADOW);

        String valStr = String.format(Locale.ROOT, "%.2f", getValue());
        int valWidth = font.width(valStr);
        int valX = getX() + width - valWidth - 4;
        g.drawString(font, valStr, valX, getY() + (height - 8) / 2, SystemEditorTheme.SLIDER_VALUE_TEXT, SystemEditorTheme.TEXT_SHADOW);

        int trackX1 = getTrackX1();
        int trackX2 = getTrackX2();

        if (trackX2 > trackX1 + 10) {
            int trackY = getY() + height / 2;

            g.fill(trackX1, trackY - 1, trackX2, trackY + 1, SystemEditorTheme.SLIDER_TRACK_BG);
            int fillX = (int) (trackX1 + this.value * (trackX2 - trackX1));
            g.fill(trackX1, trackY - 1, Math.max(trackX1, fillX), trackY + 1, SystemEditorTheme.SLIDER_TRACK_FILL);

            int handleW = 8;
            int handleX = Math.max(trackX1 - 2, Math.min(trackX2 - handleW + 2, fillX - handleW / 2));
            int handleY1 = getY() + 2;
            int handleY2 = getY() + height - 2;

            int handleBg     = hovered ? SystemEditorTheme.SLIDER_HANDLE_HOVER : SystemEditorTheme.SLIDER_HANDLE_NORMAL;
            int borderCol    = 0xFF101216;
            int highlightCol = 0x40FFFFFF;

            g.fill(handleX, handleY1, handleX + handleW, handleY2, borderCol);
            g.fill(handleX + 1, handleY1 + 1, handleX + handleW - 1, handleY2 - 1, handleBg);
            g.fill(handleX + 1, handleY1 + 1, handleX + handleW - 1, handleY1 + 2, highlightCol);

        }
    }
}
