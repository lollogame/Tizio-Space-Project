package tizio.dev.engine.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tizio.dev.engine.gui.widgets.UniformSlider;
import tizio.dev.engine.utils.DebugKeyBindings;
import tizio.dev.engine.utils.UniformOverrides;

import java.util.Map;

public class DebugUniformGui extends Screen {

    private static final int SLIDER_WIDTH = 300;
    private static final int SLIDER_HEIGHT = 20;
    private static final int PADDING = 4;

    public DebugUniformGui() {
        super(Component.literal("Debug Uniforms"));
    }

    @Override
    protected void init() {
        int y = 10;
        for (Map.Entry<String, UniformOverrides.Entry> entry : UniformOverrides.all().entrySet()) {
            addRenderableWidget(new UniformSlider(10, y, SLIDER_WIDTH, entry.getKey(), entry.getValue().min(), entry.getValue().max()));
            y += SLIDER_HEIGHT + PADDING;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(5, 5, SLIDER_WIDTH + 15, 10 + UniformOverrides.all().size() * (SLIDER_HEIGHT + PADDING), 0x88000000);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == DebugKeyBindings.TOGGLE_GUI.getKey().getValue()) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
