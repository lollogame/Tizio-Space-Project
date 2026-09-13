package tizio.dev.tsp.core.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import tizio.dev.tsp.core.gui.theme.SystemEditorTheme;

import java.util.function.Consumer;

public class ColorPreviewWidget extends AbstractWidget {

    private static final int POPUP_W = 140;
    private static final int POPUP_H = 138;
    private static final int FIELD_H = 76;
    private static final int FIELD_STEP = 1;
    private static final int TEXT_COLOR = 0xFFE0E0E0;

    private float currentHue = 0.0f;
    private float currentSat = 1.0f;
    private float currentVal = 1.0f;

    private int currentColor = 0xFFFFFFFF;
    private final Consumer<String> onColorSelected;

    private boolean paletteOpen = false;

    private final LabeledSlider vSlider;
    private boolean updatingSlider = false;
    private boolean isDraggingSlider = false;

    public ColorPreviewWidget(int x, int y, int width, int height, String initialHex, Consumer<String> onColorSelected) {

        super(x, y, width, height, Component.literal("Color Swatch"));
        this.onColorSelected = onColorSelected;
        this.vSlider = new LabeledSlider(0, 0, 124, 16, "V", 1.0, 0.0, 1.0, this::onVSliderChanged);

        setHexColor(initialHex);
    }

    public void setHexColor(String hex) {
        if (hex != null && hex.startsWith("#") && hex.length() == 7) {
            try {
                int rgb = Integer.parseInt(hex.substring(1), 16);
                this.currentColor = 0xFF000000 | rgb;

                float[] hsb = rgbToHsb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
                if (hsb[1] > 0f || hsb[2] > 0f) {
                    this.currentHue = hsb[0];
                    this.currentSat = hsb[1];
                }
                this.currentVal = hsb[2];

                syncSliderWithColor();
            } catch (NumberFormatException ignored) {}
        }
    }

    private void syncSliderWithColor() {
        if (vSlider == null) return;
        updatingSlider = true;
        vSlider.setValue(this.currentVal);
        updatingSlider = false;
    }

    private void onVSliderChanged(double newV) {
        if (updatingSlider) return;

        this.currentVal = (float) newV;
        int rgb = hsbToRgb(this.currentHue, this.currentSat, this.currentVal);
        this.currentColor = rgb;

        String hex = toHex(rgb);
        if (onColorSelected != null) {
            onColorSelected.accept(hex);
        }
    }

    public boolean isPaletteOpen() {
        return paletteOpen;
    }

    public void closePalette() {
        this.paletteOpen = false;
        this.isDraggingSlider = false;
    }

    public void handleMouseReleased() {
        this.isDraggingSlider = false;
    }

    public boolean isMouseOverSwatch(double mouseX, double mouseY) {
        return this.visible && mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
    }

    public boolean isMouseOverPopup(double mouseX, double mouseY) {
        if (!paletteOpen) return false;
        int[] pb = getPopupBounds();
        return mouseX >= pb[0] && mouseX <= pb[0] + pb[2] && mouseY >= pb[1] && mouseY <= pb[1] + pb[3];
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.visible && isMouseOverSwatch(mouseX, mouseY) && button == 0) {
            this.paletteOpen = !this.paletteOpen;
            return true;
        }
        return false;
    }

    public int[] getPopupBounds() {
        int popX = getX() - POPUP_W + width;
        int popY = getY() + height + 2;
        return new int[]{popX, popY, POPUP_W, POPUP_H};
    }

    public int getPopupBottomOffset() {
        return this.height + 2 + POPUP_H;
    }

    public int[] getFieldBounds() {
        int[] pb = getPopupBounds();
        int fx = pb[0] + 8;
        int fy = pb[1] + 8;
        int fw = pb[2] - 16;
        return new int[]{fx, fy, fw, FIELD_H};
    }

    public int[] getSliderBounds() {
        int[] pb = getPopupBounds();
        int[] fb = getFieldBounds();
        int sx = fb[0];
        int sy = fb[1] + fb[3] + 6;
        int sw = fb[2];
        int sh = 16;
        return new int[]{sx, sy, sw, sh};
    }

    private void updateSliderBounds() {
        int[] sb = getSliderBounds();
        vSlider.setX(sb[0]);
        vSlider.setY(sb[1]);
        vSlider.setWidth(sb[2]);
    }

    public boolean handlePaletteClick(double mouseX, double mouseY) {
        return handlePaletteClick(mouseX, mouseY, 0);
    }

    public boolean handlePaletteClick(double mouseX, double mouseY, int button) {
        if (!paletteOpen) return false;

        if (isMouseOverSwatch(mouseX, mouseY)) {
            return false;
        }

        if (isMouseOverPopup(mouseX, mouseY)) {
            updateSliderBounds();
            int[] sb = getSliderBounds();
            if (mouseX >= sb[0] && mouseX <= sb[0] + sb[2] && mouseY >= sb[1] && mouseY <= sb[1] + sb[3]) {
                boolean res = vSlider.mouseClicked(mouseX, mouseY, button);
                if (res) isDraggingSlider = true;
                return res;
            }

            pickFromField(mouseX, mouseY);
            return true;
        }

        this.paletteOpen = false;
        return false;
    }

    public boolean handlePaletteDrag(double mouseX, double mouseY) {
        if (!paletteOpen) return false;
        if (isDraggingSlider) {
            vSlider.mouseDragged(mouseX, mouseY, 0, 0, 0);
            return true;
        }
        if (isMouseOverPopup(mouseX, mouseY)) {
            updateSliderBounds();
            int[] sb = getSliderBounds();
            if (mouseY >= sb[1] - 4 && mouseY <= sb[1] + sb[3] + 4 && mouseX >= sb[0] && mouseX <= sb[0] + sb[2]) {
                vSlider.mouseDragged(mouseX, mouseY, 0, 0, 0);
                isDraggingSlider = true;
                return true;
            }
            return pickFromField(mouseX, mouseY);
        }
        return false;
    }

    private boolean pickFromField(double mouseX, double mouseY) {
        int[] fb = getFieldBounds();
        int fx = fb[0], fy = fb[1], fw = fb[2], fh = fb[3];
        if (mouseX < fx || mouseX > fx + fw || mouseY < fy || mouseY > fy + fh) return false;

        float hue = Mth.clamp((float) ((mouseX - fx) / (double) fw), 0.0f, 0.999999f);
        float ty = Mth.clamp((float) ((mouseY - fy) / (double) fh), 0.0f, 1.0f);

        float s, v;
        if (ty <= 0.5f) {
            float t = ty / 0.5f;
            s = t;
            v = 1f;
        } else {
            float t = (ty - 0.5f) / 0.5f;
            s = 1f;
            v = 1f - t;
        }

        this.currentHue = hue;
        this.currentSat = s;
        this.currentVal = v;

        int rgb = hsbToRgb(hue, s, v);
        this.currentColor = rgb;
        syncSliderWithColor();

        String hex = toHex(rgb);
        if (onColorSelected != null) {
            onColorSelected.accept(hex);
        }
        return true;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;

        boolean hovered = isMouseOverSwatch(mouseX, mouseY);

        g.fill(getX(), getY(), getX() + width, getY() + height, currentColor);

        int borderColor = hovered ? SystemEditorTheme.COLOR_PREVIEW_BORDER : SystemEditorTheme.PALETTE_SWATCH_BORDER;
        g.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
        g.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);
    }

    public void renderPalettePopupOverlay(GuiGraphics g, int mouseX, int mouseY) {
        int[] pb = getPopupBounds();
        int popX = pb[0], popY = pb[1], popW = pb[2], popH = pb[3];

        g.fill(popX, popY, popX + popW, popY + popH, SystemEditorTheme.PALETTE_POPUP_BG);
        g.fill(popX, popY, popX + popW, popY + 1, SystemEditorTheme.PALETTE_POPUP_BORDER);
        g.fill(popX, popY + popH - 1, popX + popW, popY + popH, SystemEditorTheme.PALETTE_SWATCH_BORDER);
        g.fill(popX, popY, popX + 1, popY + popH, SystemEditorTheme.PALETTE_SWATCH_BORDER);
        g.fill(popX + popW - 1, popY, popX + popW, popY + popH, SystemEditorTheme.PALETTE_SWATCH_BORDER);

        int[] fb = getFieldBounds();
        int fx = fb[0], fy = fb[1], fw = fb[2], fh = fb[3];

        for (int px = 0; px < fw; px += FIELD_STEP) {
            int blockW = Math.min(FIELD_STEP, fw - px);
            float hue = (px + blockW / 2f) / (float) fw;

            for (int py = 0; py < fh; py += FIELD_STEP) {
                int blockH = Math.min(FIELD_STEP, fh - py);
                float ty = (py + blockH / 2f) / (float) fh;

                float s, v;
                if (ty <= 0.5f) {
                    float t = ty / 0.5f;
                    s = t;
                    v = 1f;
                } else {
                    float t = (ty - 0.5f) / 0.5f;
                    s = 1f;
                    v = 1f - t;
                }

                int col = hsbToRgb(hue, s, v);
                g.fill(fx + px, fy + py, fx + px + blockW, fy + py + blockH, col);
            }
        }

        g.fill(fx - 1, fy - 1, fx + fw + 1, fy, SystemEditorTheme.PALETTE_SWATCH_BORDER);
        g.fill(fx - 1, fy + fh, fx + fw + 1, fy + fh + 1, SystemEditorTheme.PALETTE_SWATCH_BORDER);
        g.fill(fx - 1, fy - 1, fx, fy + fh + 1, SystemEditorTheme.PALETTE_SWATCH_BORDER);
        g.fill(fx + fw, fy - 1, fx + fw + 1, fy + fh + 1, SystemEditorTheme.PALETTE_SWATCH_BORDER);

        updateSliderBounds();
        vSlider.render(g, mouseX, mouseY, 0);

        float[] hsb = rgbToHsb((currentColor >> 16) & 0xFF, (currentColor >> 8) & 0xFF, currentColor & 0xFF);
        float markerTy = this.currentVal >= 0.999f ? this.currentSat * 0.5f : 0.5f + (1f - this.currentVal) * 0.5f;
        int markerX = fx + Mth.clamp(Math.round(this.currentHue * fw), 0, fw);
        int markerY = fy + Mth.clamp(Math.round(markerTy * fh), 0, fh);
        drawMarker(g, markerX, markerY);

        int[] sb = getSliderBounds();
        int rowY = sb[1] + sb[3] + 8;
        int swW = 14;
        g.fill(fx, rowY, fx + swW, rowY + swW, currentColor);
        g.fill(fx, rowY, fx + swW, rowY + 1, SystemEditorTheme.PALETTE_SWATCH_BORDER);
        g.fill(fx, rowY + swW - 1, fx + swW, rowY + swW, SystemEditorTheme.PALETTE_SWATCH_BORDER);
        g.fill(fx, rowY, fx + 1, rowY + swW, SystemEditorTheme.PALETTE_SWATCH_BORDER);
        g.fill(fx + swW - 1, rowY, fx + swW, rowY + swW, SystemEditorTheme.PALETTE_SWATCH_BORDER);

        Font font = Minecraft.getInstance().font;
        g.drawString(font, toHex(currentColor), fx + swW + 6, rowY + 3, TEXT_COLOR, SystemEditorTheme.TEXT_SHADOW);
    }

    private void drawMarker(GuiGraphics g, int cx, int cy) {
        int r = 3;
        g.fill(cx - r, cy - 1, cx + r + 1, cy, 0xFF000000);
        g.fill(cx - r, cy + 1, cx + r + 1, cy + 2, 0xFF000000);
        g.fill(cx - r, cy - 1, cx - r + 1, cy + 2, 0xFF000000);
        g.fill(cx + r, cy - 1, cx + r + 1, cy + 2, 0xFF000000);
        g.fill(cx - 1, cy, cx + 2, cy + 1, 0xFFFFFFFF);
    }

    private static String toHex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    private static int hsbToRgb(float h, float s, float v) {
        float r, g, b;
        if (s <= 0f) {
            r = g = b = v;
        } else {
            float hh = (h % 1.0f) * 6f;
            if (hh < 0f) hh += 6f;
            int i = (int) hh;
            float f = hh - i;
            float p = v * (1f - s);
            float q = v * (1f - s * f);
            float t = v * (1f - s * (1f - f));
            switch (i % 6) {
                case 0 -> { r = v; g = t; b = p; }
                case 1 -> { r = q; g = v; b = p; }
                case 2 -> { r = p; g = v; b = t; }
                case 3 -> { r = p; g = q; b = v; }
                case 4 -> { r = t; g = p; b = v; }
                default -> { r = v; g = p; b = q; }
            }
        }
        int ri = Mth.clamp(Math.round(r * 255f), 0, 255);
        int gi = Mth.clamp(Math.round(g * 255f), 0, 255);
        int bi = Mth.clamp(Math.round(b * 255f), 0, 255);
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    private static float[] rgbToHsb(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h;
        if (delta == 0f) {
            h = 0f;
        } else if (max == rf) {
            h = (((gf - bf) / delta) % 6f) / 6f;
        } else if (max == gf) {
            h = (((bf - rf) / delta) + 2f) / 6f;
        } else {
            h = (((rf - gf) / delta) + 4f) / 6f;
        }
        if (h < 0f) h += 1f;

        float s = max == 0f ? 0f : delta / max;
        float v = max;
        return new float[]{h, s, v};
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}
}
