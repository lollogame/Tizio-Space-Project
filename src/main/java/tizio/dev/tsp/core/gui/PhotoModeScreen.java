package tizio.dev.tsp.core.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tizio.dev.tsp.config.ConfigManager;
import tizio.dev.tsp.config.PhotoModeSettings;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.gui.theme.SystemEditorTheme;
import tizio.dev.tsp.core.gui.widgets.Button;
import tizio.dev.tsp.core.gui.widgets.LabeledSlider;
import tizio.dev.tsp.core.utils.ScreenshotManager;
import tizio.dev.tsp.engine.camera.post.AspectRatioBarsEffect;
import tizio.dev.tsp.engine.camera.post.ColorCorrectionEffect;
import tizio.dev.tsp.engine.camera.post.FilmGrainEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PhotoModeScreen extends Screen {

    private static final int ROW_H = 18;
    private static final int HEADER_H = 24;
    private final List<InspectorEntry> inspectorEntries = new ArrayList<>();
    private boolean hideUI = false;
    private boolean panelCollapsed = false;
    private boolean prevHideGuiState;
    private boolean prevDebugState;
    private int screenshotScale = 1;
    private CompositionGrid compositionGrid = CompositionGrid.NONE;
    private int gameplayFov = 70;
    private int scrollOffset = 0;
    private int totalInspectorHeight = 0;
    private String statusMessage = "";
    private long statusMessageTime = 0;
    private boolean statusIsError = false;

    public PhotoModeScreen() {
        super(Component.literal("Photo Mode"));
    }

    private int getPanelWidth() {
        return Math.min(195, Math.max(140, (this.width / 2) - 95));
    }

    @Override
    protected void init() {
        PhotoModeSettings.ensureLoaded();
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && mc.options.fov() != null) {
            this.gameplayFov = mc.options.fov().get();
            if (PhotoModeSettings.photoModeFov > 0) {
                mc.options.fov().set((int) PhotoModeSettings.photoModeFov);
            }
        }

        this.prevHideGuiState = mc.options.hideGui;
        this.prevDebugState = mc.options.renderDebug;

        mc.options.hideGui = true;
        mc.options.renderDebug = false;
        mc.gui.getChat().clearMessages(false);

        this.screenshotScale = PhotoModeSettings.screenshotScale;
        this.compositionGrid = PhotoModeSettings.compositionGrid;
        this.panelCollapsed = PhotoModeSettings.panelCollapsed;
        AspectRatioBarsEffect.enabled = PhotoModeSettings.cutAspectEnabled;
        AspectRatioBarsEffect.targetAspect = PhotoModeSettings.targetAspect;
        FilmGrainEffect.enabled = PhotoModeSettings.filmGrainEnabled;
        FilmGrainEffect.intensity = PhotoModeSettings.filmGrainIntensity;
        FilmGrainEffect.size = PhotoModeSettings.filmGrainSize;

        buildLayout();
    }

    private void showStatus(String msg, boolean error) {
        this.statusMessage = msg;
        this.statusIsError = error;
        this.statusMessageTime = System.currentTimeMillis() + 2500;
    }

    private void cycleResolutionScale() {
        screenshotScale++;
        if (screenshotScale > 4) {
            screenshotScale = 1;
        }
        PhotoModeSettings.screenshotScale = screenshotScale;
        showStatus("Native Res: " + screenshotScale + "x", false);
        buildLayout();
    }

    private void cycleCompositionGrid() {
        int next = (compositionGrid.ordinal() + 1) % CompositionGrid.values().length;
        compositionGrid = CompositionGrid.values()[next];
        PhotoModeSettings.compositionGrid = compositionGrid;
        showStatus("Grid: " + compositionGrid.getLabel(), false);
        buildLayout();
    }

    private void buildLayout() {
        clearWidgets();
        inspectorEntries.clear();
        if (hideUI) return;

        buildHeaderToolbar();

        if (!panelCollapsed) {
            buildPanel();
        }
    }

    private void buildHeaderToolbar() {
        int x = 4;
        int y = 2;

        addRenderableWidget(new Button(x, y, 80, 20, Component.literal("Photo Mode"), b -> {
        }).accentColor(SystemEditorTheme.ACCENT));
        x += 83;
        addRenderableWidget(new Button(x, y, 70, 20, Component.literal("Take Shot"), b -> triggerScreenshot()).accentColor(SystemEditorTheme.ACCENT));
        x += 73;
        addRenderableWidget(new Button(x, y, 55, 20, Component.literal("Res: " + screenshotScale + "x"), b -> cycleResolutionScale()).accentColor(SystemEditorTheme.AMBER));
        x += 58;
        addRenderableWidget(new Button(x, y, 55, 20, Component.literal("Reset"), b -> resetAllSettings()).accentColor(SystemEditorTheme.RED));
        addRenderableWidget(new Button(this.width - 70, y, 66, 20, Component.literal("Hide UI"), b -> toggleHideUI()).accentColor(SystemEditorTheme.AMBER));

        int panelW = getPanelWidth();
        String toggleLabel = panelCollapsed ? " Controls > " : " < ";
        addRenderableWidget(new Button(panelCollapsed ? 4 : panelW - 24, HEADER_H + 2, panelCollapsed ? 70 : 20, 16, Component.literal(toggleLabel), b -> {
            panelCollapsed = !panelCollapsed;
            PhotoModeSettings.panelCollapsed = panelCollapsed;
            buildLayout();
        }).compact(true));
    }

    private void buildPanel() {
        int panelW = getPanelWidth();
        int panelX = 4;
        int startY = HEADER_H + 20;
        int width = panelW - 8;

        buildTabHeader(panelX, startY, width);

        int formY = startY + ROW_H + 4;
        buildPhotoTab(panelX, formY, width);

        updateInspectorScrollPositions();
    }

    private void buildTabHeader(int x, int y, int width) {
        addRenderableWidget(new Button(x, y, width, ROW_H, Component.literal("Photo"), b -> {
        }).activeTab(true).compact(true));
    }

    private void addInspectorWidget(AbstractWidget widget, int rawY, int height, String label) {
        addWidget(widget);
        inspectorEntries.add(new InspectorEntry(widget, rawY, height, label));
        totalInspectorHeight = Math.max(totalInspectorHeight, rawY + height);
    }

    private void updateInspectorScrollPositions() {
        int formY = HEADER_H + 44;
        int visibleTop = formY;
        int visibleBottom = this.height - 10;
        int maxScroll = getMaxInspectorScroll();
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

        for (InspectorEntry entry : inspectorEntries) {
            int widgetY = formY + entry.rawY - scrollOffset;
            entry.widget.setY(widgetY);
            entry.widget.visible = (widgetY + entry.height >= visibleTop && widgetY <= visibleBottom);
        }
    }

    private int getMaxInspectorScroll() {
        int formY = HEADER_H + 44;
        int visibleH = this.height - formY - 10;
        return Math.max(0, totalInspectorHeight - visibleH);
    }

    private LabeledSlider addCustomSlider(int x, int relativeY, int width, String label, double current, double def, double min, double max, Consumer<Double> onChange) {
        int formY = HEADER_H + 44;
        int sliderW = width - 24;
        LabeledSlider slider = new LabeledSlider(x, formY + relativeY, sliderW, ROW_H, label, current, def, min, max, onChange);
        addInspectorWidget(slider, relativeY, ROW_H, null);

        Button resetBtn = new Button(x + sliderW + 2, formY + relativeY, 22, ROW_H, Component.literal("R"), b -> slider.resetToDefault()).compact(true).accentColor(SystemEditorTheme.RED);
        addInspectorWidget(resetBtn, relativeY, ROW_H, null);
        return slider;
    }

    private void buildPhotoTab(int x, int y, int width) {
        totalInspectorHeight = 0;
        int formY = HEADER_H + 44;
        int relY = 0;

        relY = buildCameraControls(x, formY, width, relY);
        if (ConfigManager.hasPostColorCorrection() && CelestialJsonLoader.isSpaceDimension(Minecraft.getInstance().player.level())) {
            relY = buildColorControls(x, formY, width, relY);
        }
        relY = buildCinematicControls(x, formY, width, relY);
    }

    private int buildCameraControls(int x, int formY, int width, int relY) {
        Minecraft mc = Minecraft.getInstance();
        float currentFov = (float) (double) mc.options.fov().get();

        addCustomSlider(x, relY, width, "FOV / Zoom", currentFov, this.gameplayFov, 30.0, 110.0, val -> {
            mc.options.fov().set((int) (double) val);
            PhotoModeSettings.photoModeFov = val.floatValue();
        });
        relY += 22;

        Button resBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Native Res: " + screenshotScale + "x"), btn -> cycleResolutionScale()).compact(true);
        addInspectorWidget(resBtn, relY, ROW_H, null);
        relY += 22;

        Button gridBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Grid: " + compositionGrid.getLabel()), btn -> cycleCompositionGrid()).compact(true);
        addInspectorWidget(gridBtn, relY, ROW_H, null);
        relY += 22;

        return relY;
    }

    private int buildColorControls(int x, int formY, int width, int relY) {
        Button tonemapBtn = new Button(x, formY + relY, width, ROW_H,
                Component.literal("Tonemap: " + ColorCorrectionEffect.tonemapper.getLabel()),
                btn -> {
                    int next = (ColorCorrectionEffect.tonemapper.ordinal() + 1) % ColorCorrectionEffect.Tonemapper.values().length;
                    ColorCorrectionEffect.tonemapper = ColorCorrectionEffect.Tonemapper.values()[next];
                    btn.setMessage(Component.literal("Tonemap: " + ColorCorrectionEffect.tonemapper.getLabel()));
                }).compact(true);
        addInspectorWidget(tonemapBtn, relY, ROW_H, null);
        relY += 22;

        addCustomSlider(x, relY, width, "Exposure", ColorCorrectionEffect.exposure, ColorCorrectionEffect.DEFAULT_EXPOSURE, 0.0, 3.0, val -> ColorCorrectionEffect.exposure = val.floatValue());
        relY += 22;
        addCustomSlider(x, relY, width, "Contrast", ColorCorrectionEffect.contrast, ColorCorrectionEffect.DEFAULT_CONTRAST, 0.0, 2.0, val -> ColorCorrectionEffect.contrast = val.floatValue());
        relY += 22;
        addCustomSlider(x, relY, width, "Saturation", ColorCorrectionEffect.saturation, ColorCorrectionEffect.DEFAULT_SATURATION, 0.0, 2.0, val -> ColorCorrectionEffect.saturation = val.floatValue());
        relY += 22;
        addCustomSlider(x, relY, width, "Vignette", ColorCorrectionEffect.vignetteIntensity, ColorCorrectionEffect.DEFAULT_VIGNETTE_INTENSITY, 0.0, 2.0, val -> ColorCorrectionEffect.vignetteIntensity = val.floatValue());
        relY += 22;
        float defaultTemp = ColorCorrectionEffect.calculateSunColorTemperature();
        addCustomSlider(x, relY, width, "Temperature", ColorCorrectionEffect.getEffectiveTemperature(), defaultTemp, -1.0, 1.0, val -> {
            ColorCorrectionEffect.temperature = val.floatValue();
            ColorCorrectionEffect.customTemperature = Math.abs(val.floatValue() - defaultTemp) > 0.001F;
        });
        return relY + 22;
    }

    private int buildCinematicControls(int x, int formY, int width, int relY) {
        Button grainBtn = new Button(x, formY + relY, width, ROW_H,
                Component.literal("Film Grain: " + (FilmGrainEffect.enabled ? "ON" : "OFF")),
                btn -> {
                    FilmGrainEffect.enabled = !FilmGrainEffect.enabled;
                    PhotoModeSettings.filmGrainEnabled = FilmGrainEffect.enabled;
                    btn.setMessage(Component.literal("Film Grain: " + (FilmGrainEffect.enabled ? "ON" : "OFF")));
                }).compact(true);
        addInspectorWidget(grainBtn, relY, ROW_H, null);
        relY += 22;
        addCustomSlider(x, relY, width, "Grain Intensity", FilmGrainEffect.intensity, FilmGrainEffect.DEFAULT_INTENSITY, 0.0, 1.0, val -> {
            FilmGrainEffect.intensity = val.floatValue();
            PhotoModeSettings.filmGrainIntensity = val.floatValue();
        });
        relY += 22;
        addCustomSlider(x, relY, width, "Grain Size", FilmGrainEffect.size, FilmGrainEffect.DEFAULT_SIZE, 0.25, 4.0, val -> {
            FilmGrainEffect.size = val.floatValue();
            PhotoModeSettings.filmGrainSize = val.floatValue();
        });
        relY += 22;

        Button cutAspectBtn = new Button(x, formY + relY, width, ROW_H,
                Component.literal("Cut Aspect: " + (AspectRatioBarsEffect.enabled ? "ON" : "OFF")),
                btn -> {
                    AspectRatioBarsEffect.enabled = !AspectRatioBarsEffect.enabled;
                    PhotoModeSettings.cutAspectEnabled = AspectRatioBarsEffect.enabled;
                    btn.setMessage(Component.literal("Cut Aspect: " + (AspectRatioBarsEffect.enabled ? "ON" : "OFF")));
                }).compact(true);

        addInspectorWidget(cutAspectBtn, relY, ROW_H, null);
        relY += 22;
        addCustomSlider(x, relY, width, "Aspect Ratio", AspectRatioBarsEffect.targetAspect, 2.39, 1.33, 3.0, val -> {
            AspectRatioBarsEffect.targetAspect = val.floatValue();
            PhotoModeSettings.targetAspect = val.floatValue();
        });
        relY += 22;
        Button shotBtn = new Button(x, formY + relY, width, ROW_H, Component.literal("Take Shot (" + screenshotScale + "x Native)"), b -> triggerScreenshot()).compact(true).accentColor(SystemEditorTheme.ACCENT);
        addInspectorWidget(shotBtn, relY, ROW_H, null);
        return relY + 22;
    }

    private void triggerScreenshot() {
        ScreenshotManager.takeScreenshot(this.screenshotScale, this::showStatus);
    }

    private void resetAllSettings() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && mc.options.fov() != null) {
            mc.options.fov().set(this.gameplayFov);
        }

        ColorCorrectionEffect.resetToDefaults();
        AspectRatioBarsEffect.resetToDefaults();
        FilmGrainEffect.resetToDefaults();

        this.screenshotScale = 1;
        this.compositionGrid = CompositionGrid.NONE;

        PhotoModeSettings.screenshotScale = 1;
        PhotoModeSettings.compositionGrid = CompositionGrid.NONE;
        PhotoModeSettings.cutAspectEnabled = false;
        PhotoModeSettings.targetAspect = 2.39F;
        PhotoModeSettings.photoModeFov = -1.0F;
        PhotoModeSettings.filmGrainEnabled = false;
        PhotoModeSettings.filmGrainIntensity = FilmGrainEffect.DEFAULT_INTENSITY;
        PhotoModeSettings.filmGrainSize = FilmGrainEffect.DEFAULT_SIZE;
        PhotoModeSettings.save();

        showStatus("Reset all settings", false);
        buildLayout();
    }

    private void toggleHideUI() {
        this.hideUI = !this.hideUI;
        buildLayout();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int panelW = getPanelWidth();

        if (!panelCollapsed && mouseX >= 0 && mouseX <= panelW && mouseY >= HEADER_H + 40 && mouseY <= this.height - 10) {
            int maxScroll = getMaxInspectorScroll();
            this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int) (amount * 18)));
            updateInspectorScrollPositions();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (ScreenshotManager.isCapturing()) {
            return;
        }

        renderCompositionGrid(g);

        if (hideUI) {
            g.fill(this.width - 240, 4, this.width - 4, 20, SystemEditorTheme.PREVIEW_BANNER_BG);
            g.drawString(this.font, "Preview Mode [ TAB/H | G Grid ]", this.width - 234, 8, SystemEditorTheme.PREVIEW_BANNER_TEXT, SystemEditorTheme.TEXT_SHADOW);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        int panelW = getPanelWidth();

        g.fill(0, 0, this.width, HEADER_H, SystemEditorTheme.HEADER_BG);
        g.fill(0, HEADER_H - 1, this.width, HEADER_H, SystemEditorTheme.HEADER_BORDER_BOT);

        if (!panelCollapsed) {
            g.fill(0, HEADER_H, panelW, this.height, SystemEditorTheme.PANEL_BG);
            g.fill(panelW - 1, HEADER_H, panelW, this.height, SystemEditorTheme.PANEL_BORDER);

            String panelHeader = "Photo Settings";
            g.drawString(this.font, panelHeader, 6, HEADER_H + 5, SystemEditorTheme.PANEL_TITLE_TEXT, SystemEditorTheme.TEXT_SHADOW);
        }

        int formY = HEADER_H + 42;
        int visibleH = this.height - formY - 10;
        int scissorBottom = this.height - 10;

        if (!panelCollapsed) {
            g.enableScissor(0, formY, panelW, scissorBottom);

            for (InspectorEntry entry : inspectorEntries) {
                if (!entry.widget.visible) continue;

                if (entry.label != null) {
                    int labelY = formY + entry.rawY - scrollOffset + (ROW_H - 8) / 2;
                    g.drawString(this.font, entry.label, 6, labelY, SystemEditorTheme.INSPECTOR_LABEL_TEXT, SystemEditorTheme.TEXT_SHADOW);
                }
                entry.widget.render(g, mouseX, mouseY, partialTick);
            }

            g.disableScissor();
        }

        g.flush();
        super.render(g, mouseX, mouseY, partialTick);
        g.flush();

        if (!panelCollapsed) {
            if (totalInspectorHeight > visibleH && visibleH > 0) {
                int maxScroll = getMaxInspectorScroll();
                int scrollbarH = Math.max(15, (visibleH * visibleH) / totalInspectorHeight);
                int scrollbarY = formY + (scrollOffset * (visibleH - scrollbarH)) / Math.max(1, maxScroll);
                int sbX = panelW - 4;

                g.fill(sbX, formY, sbX + 3, formY + visibleH, SystemEditorTheme.INSPECTOR_SCROLLBAR_BG);
                g.fill(sbX, scrollbarY, sbX + 3, scrollbarY + scrollbarH, SystemEditorTheme.INSPECTOR_SCROLLBAR_THUMB);
            }
        }

        if (System.currentTimeMillis() < statusMessageTime) {
            int textWidth = this.font.width(statusMessage);
            int toastWidth = textWidth + 16;
            int toastHeight = 15;

            int x1 = (this.width - toastWidth) / 2;
            int y1 = HEADER_H + 4;
            int x2 = x1 + toastWidth;
            int y2 = y1 + toastHeight;

            int bgColor = 0xE6141414;
            int accentColor = statusIsError ? SystemEditorTheme.TOAST_ERROR_TEXT : SystemEditorTheme.TOAST_SUCCESS_TEXT;
            int textColor = statusIsError ? SystemEditorTheme.TOAST_ERROR_TEXT : SystemEditorTheme.TEXT_HI;

            g.fill(x1, y1, x2, y2, bgColor);
            g.renderOutline(x1, y1, toastWidth, toastHeight, accentColor);
            g.drawString(this.font, statusMessage, x1 + 8, y1 + 4, textColor, SystemEditorTheme.TEXT_SHADOW);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int formY = HEADER_H + 44;
        int scissorBottom = this.height - 10;
        List<AbstractWidget> disabledForClick = new ArrayList<>();
        for (InspectorEntry entry : inspectorEntries) {
            if (entry.widget.visible && (entry.widget.getY() < formY || entry.widget.getY() + entry.height > scissorBottom)) {
                entry.widget.active = false;
                disabledForClick.add(entry.widget);
            }
        }

        boolean result = super.mouseClicked(mouseX, mouseY, button);

        for (AbstractWidget w : disabledForClick) {
            w.active = true;
        }

        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft mc = Minecraft.getInstance();

        if (keyCode == 72 || keyCode == 258) {
            toggleHideUI();
            return true;
        }

        if (keyCode == 71) {
            cycleCompositionGrid();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderCompositionGrid(GuiGraphics g) {
        if (compositionGrid == CompositionGrid.NONE) {
            return;
        }

        int left = 0;
        int right = this.width;
        int top = 0;
        int bottom = this.height;

        if (AspectRatioBarsEffect.enabled) {
            float currentAspect = (float) this.width / (float) this.height;
            if (currentAspect < AspectRatioBarsEffect.targetAspect) {
                float barHeight = (1.0f - (currentAspect / AspectRatioBarsEffect.targetAspect)) * 0.5f;
                int barPixels = Math.round(barHeight * this.height);
                top = barPixels;
                bottom = this.height - barPixels;
            } else if (currentAspect > AspectRatioBarsEffect.targetAspect) {
                float barWidth = (1.0f - (AspectRatioBarsEffect.targetAspect / currentAspect)) * 0.5f;
                int barPixels = Math.round(barWidth * this.width);
                left = barPixels;
                right = this.width - barPixels;
            }
        }

        int viewW = right - left;
        int viewH = bottom - top;
        if (viewW <= 0 || viewH <= 0) return;

        int lineColor = 0x80FFFFFF;
        int shadowColor = 0x33000000;

        switch (compositionGrid) {
            case RULE_OF_THIRDS -> {
                int x1 = left + viewW / 3;
                int x2 = left + (viewW * 2) / 3;
                int y1 = top + viewH / 3;
                int y2 = top + (viewH * 2) / 3;

                drawGridVLine(g, x1, top, bottom, lineColor, shadowColor);
                drawGridVLine(g, x2, top, bottom, lineColor, shadowColor);
                drawGridHLine(g, left, right, y1, lineColor, shadowColor);
                drawGridHLine(g, left, right, y2, lineColor, shadowColor);

                drawReticlePoint(g, x1, y1);
                drawReticlePoint(g, x1, y2);
                drawReticlePoint(g, x2, y1);
                drawReticlePoint(g, x2, y2);
            }
            case GOLDEN_RATIO -> {
                int x1 = left + Math.round(viewW * 0.381966f);
                int x2 = left + Math.round(viewW * 0.618034f);
                int y1 = top + Math.round(viewH * 0.381966f);
                int y2 = top + Math.round(viewH * 0.618034f);

                drawGridVLine(g, x1, top, bottom, lineColor, shadowColor);
                drawGridVLine(g, x2, top, bottom, lineColor, shadowColor);
                drawGridHLine(g, left, right, y1, lineColor, shadowColor);
                drawGridHLine(g, left, right, y2, lineColor, shadowColor);

                drawReticlePoint(g, x1, y1);
                drawReticlePoint(g, x1, y2);
                drawReticlePoint(g, x2, y1);
                drawReticlePoint(g, x2, y2);
            }
            case CENTER_CROSS -> {
                int cx = left + viewW / 2;
                int cy = top + viewH / 2;

                drawGridVLine(g, cx, top, bottom, lineColor, shadowColor);
                drawGridHLine(g, left, right, cy, lineColor, shadowColor);

                drawReticleBox(g, cx, cy, 8);
            }
            case SAFE_AREAS -> {
                int aMarginX = Math.round(viewW * 0.05f);
                int aMarginY = Math.round(viewH * 0.05f);
                drawOutlineWithShadow(g, left + aMarginX, top + aMarginY, viewW - 2 * aMarginX, viewH - 2 * aMarginY, lineColor, shadowColor);

                int tMarginX = Math.round(viewW * 0.10f);
                int tMarginY = Math.round(viewH * 0.10f);
                drawOutlineWithShadow(g, left + tMarginX, top + tMarginY, viewW - 2 * tMarginX, viewH - 2 * tMarginY, 0x60FFFFFF, shadowColor);

                int cx = left + viewW / 2;
                int cy = top + viewH / 2;
                drawGridHLine(g, cx - 6, cx + 7, cy, lineColor, shadowColor);
                drawGridVLine(g, cx, cy - 6, cy + 7, lineColor, shadowColor);
            }
            case DIAGONALS -> {
                drawLine(g, left, top, right, bottom, lineColor);
                drawLine(g, right, top, left, bottom, lineColor);
                drawOutlineWithShadow(g, left, top, viewW, viewH, 0x50FFFFFF, shadowColor);
            }
            default -> {
            }
        }
    }

    private void drawGridVLine(GuiGraphics g, int x, int minY, int maxY, int color, int shadow) {
        g.fill(x - 1, minY, x, maxY, shadow);
        g.fill(x, minY, x + 1, maxY, color);
        g.fill(x + 1, minY, x + 2, maxY, shadow);
    }

    private void drawGridHLine(GuiGraphics g, int minX, int maxX, int y, int color, int shadow) {
        g.fill(minX, y - 1, maxX, y, shadow);
        g.fill(minX, y, maxX, y + 1, color);
        g.fill(minX, y + 1, maxX, y + 2, shadow);
    }

    private void drawReticlePoint(GuiGraphics g, int x, int y) {
        g.fill(x - 3, y - 1, x + 4, y + 2, 0x33000000);
        g.fill(x - 1, y - 3, x + 2, y + 4, 0x33000000);
        g.fill(x - 2, y, x + 3, y + 1, 0xCCFFFFFF);
        g.fill(x, y - 2, x + 1, y + 3, 0xCCFFFFFF);
    }

    private void drawReticleBox(GuiGraphics g, int cx, int cy, int r) {
        int len = 4;
        int color = 0xCCFFFFFF;
        int shadow = 0x33000000;
        drawGridHLine(g, cx - r, cx - r + len, cy - r, color, shadow);
        drawGridVLine(g, cx - r, cy - r, cy - r + len, color, shadow);
        drawGridHLine(g, cx + r - len, cx + r, cy - r, color, shadow);
        drawGridVLine(g, cx + r, cy - r, cy - r + len, color, shadow);
        drawGridHLine(g, cx - r, cx - r + len, cy + r, color, shadow);
        drawGridVLine(g, cx - r, cy + r - len, cy + r, color, shadow);
        drawGridHLine(g, cx + r - len, cx + r, cy + r, color, shadow);
        drawGridVLine(g, cx + r, cy + r - len, cy + r, color, shadow);
    }

    private void drawOutlineWithShadow(GuiGraphics g, int x, int y, int w, int h, int color, int shadow) {
        drawGridHLine(g, x, x + w, y, color, shadow);
        drawGridHLine(g, x, x + w, y + h, color, shadow);
        drawGridVLine(g, x, y, y + h, color, shadow);
        drawGridVLine(g, x + w, y, y + h, color, shadow);
    }

    private void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    @Override
    public void removed() {
        if (this.minecraft != null && this.minecraft.options != null) {
            this.minecraft.options.hideGui = this.prevHideGuiState;
            this.minecraft.options.renderDebug = this.prevDebugState;

            if (this.minecraft.options.fov() != null) {
                PhotoModeSettings.photoModeFov = this.minecraft.options.fov().get().floatValue();
                this.minecraft.options.fov().set(this.gameplayFov);
            }
        }
        PhotoModeSettings.screenshotScale = this.screenshotScale;
        PhotoModeSettings.compositionGrid = this.compositionGrid;
        PhotoModeSettings.panelCollapsed = this.panelCollapsed;
        PhotoModeSettings.cutAspectEnabled = AspectRatioBarsEffect.enabled;
        PhotoModeSettings.targetAspect = AspectRatioBarsEffect.targetAspect;
        PhotoModeSettings.filmGrainEnabled = FilmGrainEffect.enabled;
        PhotoModeSettings.filmGrainIntensity = FilmGrainEffect.intensity;
        PhotoModeSettings.filmGrainSize = FilmGrainEffect.size;

        PhotoModeSettings.save();

        AspectRatioBarsEffect.enabled = false;
        FilmGrainEffect.enabled = false;
        super.removed();
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public enum CompositionGrid {
        NONE("OFF"),
        RULE_OF_THIRDS("Rule of Thirds"),
        GOLDEN_RATIO("Golden Ratio"),
        CENTER_CROSS("Center Cross"),
        SAFE_AREAS("Safe Areas"),
        DIAGONALS("Diagonals");

        private final String label;

        CompositionGrid(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static class InspectorEntry {
        AbstractWidget widget;
        int rawY;
        int height;
        String label;

        InspectorEntry(AbstractWidget widget, int rawY, int height, String label) {
            this.widget = widget;
            this.rawY = rawY;
            this.height = height;
            this.label = label;
        }
    }
}