package tizio.dev.tsp.core.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tizio.dev.tsp.core.gui.theme.SystemEditorTheme;

public class ConfirmationPopup extends Screen {
    private final Screen parentScreen;
    private final String targetName;
    private final Runnable onConfirm;

    public ConfirmationPopup(Screen parentScreen, String targetName, Runnable onConfirm) {
        super(Component.literal("Confirm Deletion | Reset"));
        this.parentScreen = parentScreen;
        this.targetName = targetName;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int dialogW = 230;
        int dialogH = 100;
        int dialogX = (this.width - dialogW) / 2;
        int dialogY = (this.height - dialogH) / 2;

        addRenderableWidget(Button.builder(Component.literal("Proceed"), b -> confirmDelete()).bounds(dialogX + 15, dialogY + 65, 95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> closePrompt()).bounds(dialogX + 120, dialogY + 65, 95, 20).build());
    }

    private void confirmDelete() {
        if (onConfirm != null) {
            onConfirm.run();
        }
        closePrompt();
    }

    private void closePrompt() {
        Minecraft.getInstance().setScreen(parentScreen);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            confirmDelete();
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
        guiGraphics.fill(0, 0, this.width, this.height, SystemEditorTheme.DELETE_OVERLAY_BG);

        int dialogW = 230;
        int dialogH = 100;
        int dialogX = (this.width - dialogW) / 2;
        int dialogY = (this.height - dialogH) / 2;

        guiGraphics.fill(dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, SystemEditorTheme.DELETE_POPUP_BG);
        guiGraphics.fill(dialogX, dialogY, dialogX + dialogW, dialogY + 2, SystemEditorTheme.DELETE_POPUP_TOP_BAR);
        guiGraphics.fill(dialogX, dialogY + dialogH - 2, dialogX + dialogW, dialogY + dialogH, SystemEditorTheme.DELETE_POPUP_BORDER);
        guiGraphics.fill(dialogX, dialogY, dialogX + 2, dialogY + dialogH, SystemEditorTheme.DELETE_POPUP_BORDER);
        guiGraphics.fill(dialogX + dialogW - 2, dialogY, dialogX + dialogW, dialogY + dialogH, SystemEditorTheme.DELETE_POPUP_BORDER);

        guiGraphics.drawString(this.font, "Confirm Deletion | Reset", dialogX + 15, dialogY + 12, SystemEditorTheme.DELETE_TITLE_TEXT, SystemEditorTheme.TEXT_SHADOW);
        guiGraphics.drawString(this.font, "Are you sure you want to delete/reset?", dialogX + 15, dialogY + 30, SystemEditorTheme.DELETE_BODY_TEXT, SystemEditorTheme.TEXT_SHADOW);
        guiGraphics.drawString(this.font, "\"" + targetName + "\"?", dialogX + 15, dialogY + 44, SystemEditorTheme.DELETE_TARGET_TEXT, SystemEditorTheme.TEXT_SHADOW);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
