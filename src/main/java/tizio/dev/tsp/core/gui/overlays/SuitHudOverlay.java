package tizio.dev.tsp.core.gui.overlays;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.gui.theme.SuitHudTheme;
import tizio.dev.tsp.core.handlers.gravity.GravityManager;
import tizio.dev.tsp.core.handlers.oxygen.OxygenManager;
import tizio.dev.tsp.core.handlers.temperature.TemperatureManager;
import tizio.dev.tsp.resources.armor.CustomArmorItem;

@Mod.EventBusSubscriber(modid = MainClass.MODID, value = Dist.CLIENT)
public final class SuitHudOverlay {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final ResourceLocation VISOR_BACKGROUND_OVERLAY = new ResourceLocation(MainClass.MODID, "textures/gui/visor_overlay_c.png");

    private SuitHudOverlay() {}

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        if (mc.options.hideGui || mc.player == null || mc.level == null) {
            return;
        }

        if (!mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        Player player = mc.player;
        if (player.isSpectator()) {
            return;
        }

        ItemStack headSlot = player.getItemBySlot(EquipmentSlot.HEAD);
        if (headSlot.isEmpty() || !(headSlot.getItem() instanceof CustomArmorItem)) {
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        drawCyberVisorHud(event.getGuiGraphics(), player, screenWidth, screenHeight);
    }

    private static void drawCyberVisorHud(GuiGraphics g, Player player, int screenW, int screenH) {
        Font font = mc.font;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float oxygen = OxygenManager.getOxygen(player);
        float temperature = TemperatureManager.getTemperature(player);
        float envTemp = TemperatureManager.getEnvironmentTemperature(player.level());
        boolean hasSuit = OxygenManager.hasFullSpaceSuit(player);

        drawVisorBackgroundOverlay(g, screenW, screenH);
        drawVisorFrame(g, screenW, screenH);
        drawTopCompassTape(g, font, player, screenW);
        drawTopLeftTelemetry(g, font, player);
        drawMidLeftVitals(g, font, player, oxygen, temperature, envTemp, hasSuit);
        drawRightAltitudeLadder(g, font, player, screenW, screenH);
        drawBottomRightArmorIntegrity(g, font, player, screenW, screenH, hasSuit);

        RenderSystem.disableBlend();
    }

    private static void drawVisorBackgroundOverlay(GuiGraphics g, int screenW, int screenH) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(VISOR_BACKGROUND_OVERLAY, 0, 0, 0.0F, 0.0F, screenW, screenH, screenW, screenH);
    }

    private static void drawVisorFrame(GuiGraphics g, int screenW, int screenH) {
        int color = SuitHudTheme.VISOR_BRACKET;
        int len = 20;

        g.fill(6, 6, 6 + len, 7, color);
        g.fill(6, 6, 7, 6 + len, color);

        g.fill(screenW - 6 - len, 6, screenW - 6, 7, color);
        g.fill(screenW - 7, 6, screenW - 6, 6 + len, color);

        g.fill(6, screenH - 7, 6 + len, screenH - 6, color);
        g.fill(6, screenH - 6 - len, 7, screenH - 6, color);

        g.fill(screenW - 6 - len, screenH - 7, screenW - 6, screenH - 6, color);
        g.fill(screenW - 7, screenH - 6 - len, screenW - 6, screenH - 6, color);
    }

    private static void drawTopCompassTape(GuiGraphics g, Font font, Player player, int screenW) {
        int tapeW = 200;
        int tapeH = 12;
        int tapeX = (screenW - tapeW) / 2;
        int tapeY = 6;

        float yaw = (player.getYRot() % 360.0F + 360.0F) % 360.0F;
        float northYaw = (yaw + 180.0F) % 360.0F;

        int centerX = tapeX + (tapeW / 2);

        String headNum = String.format("%03.0f°", northYaw);
        int hnW = font.width(headNum);

        g.drawString(font, headNum, centerX - (hnW / 2), tapeY - 1, SuitHudTheme.AMBER_BRIGHT, SuitHudTheme.TEXT_SHADOW);
        g.fill(tapeX, tapeY + 9, tapeX + tapeW, tapeY + 10, SuitHudTheme.AMBER_SUB);

        double scale = mc.getWindow().getGuiScale();
        int scissorX = (int) (tapeX * scale);
        int scissorY = (int) ((mc.getWindow().getGuiScaledHeight() - (tapeY + tapeH + 9)) * scale);
        int scissorW = (int) (tapeW * scale);
        int scissorH = (int) ((tapeH + 9) * scale);

        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

        float visibleDeg = 100.0F;
        float pxPerDeg = (float) tapeW / visibleDeg;

        for (int deg = 0; deg < 360; deg += 10) {
            float diff = deg - northYaw;
            while (diff < -180.0F) diff += 360.0F;
            while (diff > 180.0F) diff -= 360.0F;

            if (Math.abs(diff) <= visibleDeg / 2.0F) {
                int tickX = Math.round(centerX + (diff * pxPerDeg));
                boolean isMajor = (deg % 20 == 0);

                if (isMajor) {
                    g.fill(tickX, tapeY + 7, tickX + 1, tapeY + 11, SuitHudTheme.AMBER_PRIMARY);
                    String lbl = String.format("%03d", deg);
                    int tw = font.width(lbl);
                    g.drawString(font, lbl, tickX - (tw / 2), tapeY + 11, SuitHudTheme.AMBER_DIM, SuitHudTheme.TEXT_SHADOW);
                } else {
                    g.fill(tickX, tapeY + 8, tickX + 1, tapeY + 10, SuitHudTheme.AMBER_SUB);
                }
            }
        }

        RenderSystem.disableScissor();

        g.fill(centerX - 1, tapeY + 7, centerX + 1, tapeY + 8, SuitHudTheme.TEXT_HIGHLIGHT);
        g.fill(centerX, tapeY + 8, centerX + 1, tapeY + 11, SuitHudTheme.TEXT_HIGHLIGHT);
    }

    private static void drawTopLeftTelemetry(GuiGraphics g, Font font, Player player) {
        int x = 14;
        int y = 14;

        g.drawString(font, "▶ [ Telemetry ]", x, y, SuitHudTheme.AMBER_PRIMARY, SuitHudTheme.TEXT_SHADOW);
        g.fill(x, y + 9, x + 118, y + 10, SuitHudTheme.AMBER_DIM);

        int rowY = y + 12;
        int px = (int) player.getX();
        int py = (int) player.getY();
        int pz = (int) player.getZ();

        float yaw = (player.getYRot() % 360.0F + 360.0F) % 360.0F;
        float northYaw = (yaw + 180.0F) % 360.0F;
        String dir = getHeadingDir(northYaw);

        double spdKmh = player.getDeltaMovement().length() * 72.0;
        long dayTime = player.level().getDayTime();
        long hours = (dayTime / 1000 + 6) % 24;
        long mins = (dayTime % 1000) * 60 / 1000;
        long day = (dayTime / 24000) + 1;

        String dimId = player.level().dimension().location().getPath().toUpperCase();

        double gravG = GravityManager.getGravityScale(player.level());
        double gravMs2 = GravityManager.getGravityMs2(player.level());
        String gravStr = gravG < 0.05 ? "0.00G (ZERO-G)" : String.format("%.2fG (%.1f m/s²)", gravG, gravMs2);

        rowY += 1;
        drawTelemetryRow(g, font, "HDG", String.format("%s %03.0f°", dir, northYaw), x, rowY); rowY += 9;
        drawTelemetryRow(g, font, "SPD", String.format("%.1f KM/H", spdKmh), x, rowY); rowY += 9;
        drawTelemetryRow(g, font, "TIME", String.format("%02d:%02d  DAY %d", hours, mins, day), x, rowY); rowY += 9;
        drawTelemetryRow(g, font, "SECTOR", dimId, x, rowY); rowY += 9;
        drawTelemetryRow(g, font, "GRAV", gravStr, x, rowY);
    }

    private static void drawTelemetryRow(GuiGraphics g, Font font, String label, String val, int x, int y) {
        g.drawString(font, label, x, y, SuitHudTheme.AMBER_DIM, SuitHudTheme.TEXT_SHADOW);
        g.drawString(font, val, x + 44, y, SuitHudTheme.AMBER_BRIGHT, SuitHudTheme.TEXT_SHADOW);
    }

    private static String getHeadingDir(float northYaw) {
        if (northYaw >= 337.5 || northYaw < 22.5) return "N";
        if (northYaw < 67.5) return "NE";
        if (northYaw < 112.5) return "E";
        if (northYaw < 157.5) return "SE";
        if (northYaw < 202.5) return "S";
        if (northYaw < 247.5) return "SW";
        if (northYaw < 292.5) return "W";
        return "NW";
    }

    private static void drawMidLeftVitals(GuiGraphics g, Font font, Player player, float oxygen, float temperature, float envTemp, boolean hasSuit) {
        int x = 14;
        int y = 78;

        g.drawString(font, "▶ [ Life Support ]", x, y, SuitHudTheme.AMBER_PRIMARY, SuitHudTheme.TEXT_SHADOW);
        g.fill(x, y + 9, x + 118, y + 10, SuitHudTheme.AMBER_DIM);

        int rowY = y + 13;

        int o2Col = SuitHudTheme.getOxygenColor(oxygen);
        g.drawString(font, "O₂", x, rowY, SuitHudTheme.AMBER_DIM, SuitHudTheme.TEXT_SHADOW);
        drawVectorBar(g, x + 44, rowY + 1, 56, 5, oxygen, o2Col);
        String o2Pct = Math.round(oxygen * 100.0F) + "%";
        g.drawString(font, o2Pct, x + 104, rowY, o2Col, SuitHudTheme.TEXT_SHADOW);
        rowY += 9;

        float coreC = SuitHudTheme.toCelsiusBody(temperature);
        int tempCol = SuitHudTheme.getTemperatureColor(temperature);
        drawTelemetryRow(g, font, "BODY", String.format("%.1f°C", coreC), x, rowY, tempCol);
        rowY += 9;

        float ambC = SuitHudTheme.toCelsiusAmbient(envTemp);
        int ambCol = SuitHudTheme.getTemperatureColor(envTemp);
        drawTelemetryRow(g, font, "AMB", String.format("%.0f°C", ambC), x, rowY, ambCol);
        rowY += 9;

        int bpm = calculatePlayerHeartRate(player, oxygen);
        int pulseCol = SuitHudTheme.getHeartRateColor(bpm);
        drawTelemetryRow(g, font, "PULSE", bpm + " BPM", x, rowY, pulseCol);
        rowY += 9;

        String sealStr = hasSuit ? "SEALED" : "BREACH / EXPOSED";
        int sealCol = hasSuit ? SuitHudTheme.EMERALD_GREEN : SuitHudTheme.ALERT_RED;
        drawTelemetryRow(g, font, "SUIT", sealStr, x, rowY, sealCol);
    }

    private static void drawTelemetryRow(GuiGraphics g, Font font, String label, String val, int x, int y, int valColor) {
        g.drawString(font, label, x, y, SuitHudTheme.AMBER_DIM, SuitHudTheme.TEXT_SHADOW);
        g.drawString(font, val, x + 44, y, valColor, SuitHudTheme.TEXT_SHADOW);
    }

    private static void drawVectorBar(GuiGraphics g, int x, int y, int w, int h, float percent, int fgColor) {
        g.fill(x, y, x + w, y + h, SuitHudTheme.AMBER_FAINT);

        int filledW = Math.round(w * Math.max(0.0F, Math.min(1.0F, percent)));
        if (filledW > 0) {
            g.fill(x, y, x + filledW, y + h, fgColor);
        }

        for (int i = 1; i <= 4; i++) {
            int divX = x + (i * w / 5);
            g.fill(divX, y, divX + 1, y + h, SuitHudTheme.AMBER_SUB);
        }

        g.fill(x, y, x + w, y + 1, SuitHudTheme.AMBER_DIM);
        g.fill(x, y + h - 1, x + w, y + h, SuitHudTheme.AMBER_DIM);
        g.fill(x, y, x + 1, y + h, SuitHudTheme.AMBER_DIM);
        g.fill(x + w - 1, y, x + w, y + h, SuitHudTheme.AMBER_DIM);
    }

    private static void drawRightAltitudeLadder(GuiGraphics g, Font font, Player player, int screenW, int screenH) {
        int ladderX = screenW - 38;
        int ladderY = (screenH / 2) - 40;
        int ladderH = 80;

        g.drawString(font, "ALT - Y", ladderX - 14, ladderY - 12, SuitHudTheme.AMBER_DIM, SuitHudTheme.TEXT_SHADOW);

        g.fill(ladderX, ladderY, ladderX + 1, ladderY + ladderH, SuitHudTheme.AMBER_SUB);

        int playerY = (int) player.getY();
        int midY = ladderY + (ladderH / 2);

        for (int offset = -30; offset <= 30; offset += 10) {
            int tickAltitude = Math.floorDiv(playerY + offset, 10) * 10;
            int tickY = midY - (int) ((tickAltitude - player.getY()) * 1.3);

            if (tickY >= ladderY && tickY <= ladderY + ladderH) {
                g.fill(ladderX, tickY, ladderX + 3, tickY + 1, SuitHudTheme.AMBER_PRIMARY);
                String altStr = formatAltitude(tickAltitude);
                g.drawString(font, altStr, ladderX + 5, tickY - 3, SuitHudTheme.AMBER_DIM, SuitHudTheme.TEXT_SHADOW);
            }
        }

        g.fill(ladderX - 3, midY, ladderX, midY + 1, SuitHudTheme.TEXT_HIGHLIGHT);
        String curAlt = "▶" + formatAltitude(playerY);
        int curAltW = font.width(curAlt);
        g.drawString(font, curAlt, ladderX - curAltW - 3, midY - 3, SuitHudTheme.TEXT_HIGHLIGHT, SuitHudTheme.TEXT_SHADOW);
    }

    private static String formatAltitude(int altitude) {
        int abs = Math.abs(altitude);
        String sign = altitude < 0 ? "-" : "";
        if (abs >= 1_000_000) {
            double m = abs / 1_000_000.0;
            return m >= 10.0 ? String.format(java.util.Locale.ROOT, "%s%.0fM", sign, m) : String.format(java.util.Locale.ROOT, "%s%.1fM", sign, m);
        } else if (abs >= 10_000) {
            return String.format(java.util.Locale.ROOT, "%s%.0fk", sign, abs / 1000.0);
        } else if (abs >= 1_000) {
            return String.format(java.util.Locale.ROOT, "%s%.1fk", sign, abs / 1000.0);
        }
        return String.valueOf(altitude);
    }

    private static void drawBottomRightArmorIntegrity(GuiGraphics g, Font font, Player player, int screenW, int screenH, boolean hasSuit) {
        int x = screenW - 116;
        int y = screenH - 75;

        g.drawString(font, "[ Suit Integrity ] ◀", x, y, SuitHudTheme.AMBER_PRIMARY, SuitHudTheme.TEXT_SHADOW);
        g.fill(x, y + 9, x + 104, y + 10, SuitHudTheme.AMBER_DIM);

        int rowY = y + 13;

        int suitPct = calculateAverageSuitDurability(player);
        int barCol = suitPct > 50 ? SuitHudTheme.AMBER_PRIMARY : (suitPct > 20 ? SuitHudTheme.ALERT_AMBER : SuitHudTheme.ALERT_RED);
        drawVectorBar(g, x, rowY + 1, 60, 5, suitPct / 100.0F, barCol);
        g.drawString(font, suitPct + "%", x + 66, rowY, barCol, SuitHudTheme.TEXT_SHADOW);
        rowY += 9;

        drawArmorPieceRow(g, font, "HELM", player.getItemBySlot(EquipmentSlot.HEAD), x, rowY); rowY += 8;
        drawArmorPieceRow(g, font, "CHEST", player.getItemBySlot(EquipmentSlot.CHEST), x, rowY); rowY += 8;
        drawArmorPieceRow(g, font, "LEGS", player.getItemBySlot(EquipmentSlot.LEGS), x, rowY); rowY += 8;
        drawArmorPieceRow(g, font, "BOOTS", player.getItemBySlot(EquipmentSlot.FEET), x, rowY); rowY += 9;

        String sysStatus = hasSuit ? "Systems: EVA ONLINE" : "Systems: EXPOSED";
        int sysCol = hasSuit ? SuitHudTheme.EMERALD_GREEN : SuitHudTheme.ALERT_RED;
        g.drawString(font, sysStatus, x, rowY, sysCol, SuitHudTheme.TEXT_SHADOW);
    }

    private static void drawArmorPieceRow(GuiGraphics g, Font font, String name, ItemStack stack, int x, int y) {
        boolean equipped = !stack.isEmpty() && (stack.getItem() instanceof CustomArmorItem);
        int col = equipped ? SuitHudTheme.AMBER_PRIMARY : SuitHudTheme.AMBER_SUB;

        g.drawString(font, name, x, y, col, SuitHudTheme.TEXT_SHADOW);

        String durStr;
        if (!equipped) {
            durStr = "OFF";
        } else if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage();
            int cur = max - stack.getDamageValue();
            int pct = (int) ((cur / (float) max) * 100.0F);
            durStr = pct + "%";
        } else {
            durStr = "100%";
        }

        g.drawString(font, durStr, x + 44, y, equipped ? SuitHudTheme.AMBER_BRIGHT : SuitHudTheme.AMBER_FAINT, SuitHudTheme.TEXT_SHADOW);
    }

    private static int calculateAverageSuitDurability(Player player) {
        int total = 0;
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof CustomArmorItem) {
                count++;
                if (stack.isDamageableItem()) {
                    int max = stack.getMaxDamage();
                    int cur = max - stack.getDamageValue();
                    total += (int) ((cur / (float) max) * 100.0F);
                } else {
                    total += 100;
                }
            }
        }
        if (count == 0) return 0;
        return total / count;
    }

    private static int calculatePlayerHeartRate(Player player, float oxygen) {
        float healthPct = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        int baseBpm = 72;

        if (player.isSprinting()) baseBpm += 42;
        if (healthPct < 0.5F) baseBpm += Math.round((1.0F - healthPct) * 45.0F);
        if (oxygen < 0.3F) baseBpm += Math.round((0.3F - oxygen) * 75.0F);
        if (player.isOnFire() || player.getTicksFrozen() > 0) baseBpm += 25;

        int wave = (int) (Math.sin(player.tickCount * 0.1) * 2);
        return Math.max(50, Math.min(195, baseBpm + wave));
    }
}
