package tizio.dev.tsp.core.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import tizio.dev.tsp.core.gui.theme.SystemEditorTheme;

public class Button extends net.minecraft.client.gui.components.Button {

    private boolean activeTab = false;
    private boolean compact   = false;
    private Integer customAccent = null;

    public Button(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public Button activeTab(boolean active) {
        this.activeTab = active;
        return this;
    }

    public Button compact(boolean compact) {
        this.compact = compact;
        return this;
    }

    public Button accentColor(int color) {
        this.customAccent = color;
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;

        boolean hovered = this.isHoveredOrFocused();

        int bgColor     = activeTab ? SystemEditorTheme.TAB_ACTIVE_BG : (hovered ? SystemEditorTheme.BTN_BG_HOVER : SystemEditorTheme.BTN_BG_NORMAL);
        int borderColor = customAccent != null ? customAccent : (hovered || activeTab ? SystemEditorTheme.BTN_BORDER_HOVER : SystemEditorTheme.BTN_BORDER_NORMAL);
        int textColor   = activeTab ? SystemEditorTheme.TAB_ACTIVE_ACCENT : (hovered ? SystemEditorTheme.BTN_TEXT_HOVER : SystemEditorTheme.BTN_TEXT_NORMAL);

        g.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

        g.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
        g.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);


        Font font = Minecraft.getInstance().font;
        String textStr = getMessage().getString();
        int textW = font.width(textStr);

        g.enableScissor(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1);
        int textX = compact ? getX() + (width - textW) / 2 : getX() + (width - textW) / 2;
        int textY = getY() + (height - 8) / 2;
        g.drawString(font, textStr, textX, textY, textColor, SystemEditorTheme.TEXT_SHADOW);
        g.disableScissor();
    }
}
