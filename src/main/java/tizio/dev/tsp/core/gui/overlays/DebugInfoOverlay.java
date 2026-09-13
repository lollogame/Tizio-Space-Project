package tizio.dev.tsp.core.gui.overlays;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import tizio.dev.tsp.core.utils.Color;

import java.text.DecimalFormat;


public class DebugInfoOverlay {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final DecimalFormat DF  = new DecimalFormat("0.0");
    private static final DecimalFormat DF2 = new DecimalFormat("0.000");

    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (mc.options.hideGui) return;
        if (mc.level == null) return;
        drawOverlay(event.getGuiGraphics());
    }

    private static void drawOverlay(GuiGraphics g) {

        Font font   = mc.font;
        int startX  = 10;
        int startY  = 45;

        RenderSystem.enableBlend();

        int y          = startY;
        int titleColor = Color.colorHex(0, 255, 255);
        int textColor  = Color.colorHex(0, 255, 0);
        int warnColor  = Color.colorHex(255, 200, 0);

        y = drawTextWithBackground(g, font, "[Graphics Stats]", startX, y, titleColor);
        y = drawTextWithBackground(g, font, "FPS: " + DF.format(Minecraft.getInstance().getFps()), startX, y, textColor);

        RenderSystem.disableBlend();
    }

    private static int drawTextWithBackground(GuiGraphics g, Font font, String text, int x, int y, int color) {
        int padding   = 3;
        int textWidth  = font.width(text);
        int textHeight = font.lineHeight;

        int bgX1 = x - padding;
        int bgY1 = y - padding / 2;
        int bgX2 = x + textWidth + padding;
        int bgY2 = y + textHeight + padding / 2;

        g.fill(bgX1, bgY1, bgX2, bgY2, Color.colorHex(10, 10, 10, 5));
        g.drawString(font, text, x, y, color, true);

        return y + textHeight + 4;
    }
}
