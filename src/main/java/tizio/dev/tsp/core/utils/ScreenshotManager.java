package tizio.dev.tsp.core.utils;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.mixin.render.accessor.LevelRendererAccessor;
import tizio.dev.tsp.mixin.render.accessor.WindowAccessor;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = MainClass.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ScreenshotManager {

    private static final int SAFE_MAX_GPU_SIZE = 16384;

    private static boolean capturing = false;
    private static int warmupFramesLeft = 0;
    private static int currentScale = 1;
    private static boolean hidePlayer = false;

    private static int originalWidth = 0;
    private static int originalHeight = 0;
    private static boolean originalHideGui = false;

    private static int targetWidth = 0;
    private static int targetHeight = 0;
    private static int renderWidth = 0;
    private static int renderHeight = 0;
    private static boolean isCpuUpscaling = false;

    private static BiConsumer<String, Boolean> activeCallback = null;

    private ScreenshotManager() {
    }

    public static boolean isCapturing() {
        return capturing;
    }

    public static boolean isHidePlayer() {
        return hidePlayer;
    }

    public static void setHidePlayer(boolean hide) {
        hidePlayer = hide;
    }

    public static void takeScreenshot(int scaleFactor) {
        takeScreenshot(scaleFactor, hidePlayer, null);
    }

    public static void takeScreenshot(int scaleFactor, BiConsumer<String, Boolean> statusCallback) {
        takeScreenshot(scaleFactor, hidePlayer, statusCallback);
    }

    public static void takeScreenshot(int scaleFactor, boolean hidePlayerModel, BiConsumer<String, Boolean> statusCallback) {
        if (capturing) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getMainRenderTarget() == null) {
            if (statusCallback != null) {
                statusCallback.accept("Capture failed: No active world", true);
            }
            return;
        }

        try {
            currentScale = Math.max(1, Math.min(scaleFactor, 4));
            hidePlayer = hidePlayerModel;
            activeCallback = statusCallback;

            WindowAccessor window = (WindowAccessor) (Object) mc.getWindow();
            originalWidth = window.getFramebufferWidth();
            originalHeight = window.getFramebufferHeight();
            originalHideGui = mc.options.hideGui;

            targetWidth = originalWidth * currentScale;
            targetHeight = originalHeight * currentScale;

            int hardwareLimit = RenderSystem.maxSupportedTextureSize();
            int maxGpuSize = Math.min(hardwareLimit - 100, SAFE_MAX_GPU_SIZE);

            if (targetWidth > maxGpuSize || targetHeight > maxGpuSize) {
                double scaleRatio = Math.min((double) maxGpuSize / targetWidth, (double) maxGpuSize / targetHeight);
                renderWidth = Math.max(1, (int) (targetWidth * scaleRatio));
                renderHeight = Math.max(1, (int) (targetHeight * scaleRatio));
                isCpuUpscaling = true;
            } else {
                renderWidth = targetWidth;
                renderHeight = targetHeight;
                isCpuUpscaling = false;
            }

            capturing = true;
            mc.options.hideGui = true;
            warmupFramesLeft = (currentScale == 1 ? 2 : 4);

            resize(renderWidth, renderHeight);

        } catch (OutOfMemoryError oom) {
            handleOOM(oom);
        } catch (Exception e) {
            handleError(e);
        }
    }

    public static void cancelCapture() {
        if (capturing) {
            restoreState();
        }
        activeCallback = null;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!capturing || event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getMainRenderTarget() == null) {
            cancelCapture();
            return;
        }

        try {
            WindowAccessor window = (WindowAccessor) (Object) mc.getWindow();
            if (window.getFramebufferWidth() != renderWidth || window.getFramebufferHeight() != renderHeight) {
                resize(renderWidth, renderHeight);
            }

            if (warmupFramesLeft > 0) {
                warmupFramesLeft--;
                return;
            }

            NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            BiConsumer<String, Boolean> callback = activeCallback;
            activeCallback = null;
            int finalTargetW = targetWidth;
            int finalTargetH = targetHeight;
            boolean cpuUpscale = isCpuUpscaling;
            int finalScale = currentScale;

            restoreState();

            processAndSaveScreenshot(image, finalTargetW, finalTargetH, cpuUpscale, finalScale, callback);

        } catch (OutOfMemoryError oom) {
            handleOOM(oom);
        } catch (Exception e) {
            handleError(e);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (capturing && hidePlayer) {
            if (event.getEntity() == Minecraft.getInstance().player) {
                event.setCanceled(true);
            }
        }
    }

    private static void resize(int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        WindowAccessor window = (WindowAccessor) (Object) mc.getWindow();
        window.setFramebufferWidth(width);
        window.setFramebufferHeight(height);
        mc.getWindow().setWidth(width);
        mc.getWindow().setHeight(height);

        RenderSystem.viewport(0, 0, width, height);

        RenderTarget mainTarget = mc.getMainRenderTarget();
        if (mainTarget != null && (mainTarget.width != width || mainTarget.height != height)) {
            mainTarget.resize(width, height, Minecraft.ON_OSX);
        }

        if (mc.levelRenderer != null) {
            if (mc.levelRenderer instanceof LevelRendererAccessor lvl) {
                if (lvl.getEntityEffect() != null) lvl.getEntityEffect().resize(width, height);
                if (lvl.getTransparencyChain() != null) lvl.getTransparencyChain().resize(width, height);
            }
            if (mc.levelRenderer.entityTarget() != null)
                mc.levelRenderer.entityTarget().resize(width, height, Minecraft.ON_OSX);
            if (mc.levelRenderer.getTranslucentTarget() != null)
                mc.levelRenderer.getTranslucentTarget().resize(width, height, Minecraft.ON_OSX);
            if (mc.levelRenderer.getParticlesTarget() != null)
                mc.levelRenderer.getParticlesTarget().resize(width, height, Minecraft.ON_OSX);
            if (mc.levelRenderer.getWeatherTarget() != null)
                mc.levelRenderer.getWeatherTarget().resize(width, height, Minecraft.ON_OSX);
            if (mc.levelRenderer.getCloudsTarget() != null)
                mc.levelRenderer.getCloudsTarget().resize(width, height, Minecraft.ON_OSX);
            if (mc.levelRenderer.getItemEntityTarget() != null)
                mc.levelRenderer.getItemEntityTarget().resize(width, height, Minecraft.ON_OSX);
        }

        if (mc.gameRenderer != null) {
            mc.gameRenderer.resize(width, height);
        }
    }

    private static void restoreState() {
        capturing = false;
        Minecraft mc = Minecraft.getInstance();
        try {
            resize(originalWidth - 1, originalHeight);
            resize(originalWidth, originalHeight);

            RenderSystem.viewport(0, 0, originalWidth, originalHeight);

            if (mc.getMainRenderTarget() != null) {
                mc.getMainRenderTarget().bindWrite(true);
                RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            }

            mc.options.hideGui = originalHideGui;
        } catch (Exception e) {
            MainClass.LOGGER.error("Failed to restore display state after screenshot", e);
        }
    }

    private static void handleOOM(OutOfMemoryError oom) {
        MainClass.LOGGER.error("Out of memory during screenshot capture", oom);
        restoreState();
        System.gc();
        BiConsumer<String, Boolean> cb = activeCallback;
        activeCallback = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§cScreenshot error: Out of memory! Reduce resolution."));
        }
        if (cb != null) {
            cb.accept("Out of memory! Reduce resolution.", true);
        }
    }

    private static void handleError(Exception e) {
        MainClass.LOGGER.error("Error during screenshot capture", e);
        restoreState();
        BiConsumer<String, Boolean> cb = activeCallback;
        activeCallback = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§cScreenshot error: " + e.getMessage()));
        }
        if (cb != null) {
            cb.accept("Capture failed: " + e.getMessage(), true);
        }
    }

    private static void processAndSaveScreenshot(NativeImage image, int targetW, int targetH, boolean cpuUpscale, int scale, BiConsumer<String, Boolean> callback) {
        Minecraft mc = Minecraft.getInstance();
        File screenshotsDir = new File(mc.gameDirectory, "screenshots");
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        String baseName = timestamp + (scale > 1 ? "_" + scale + "x" : "");
        File initialFile = new File(screenshotsDir, baseName + ".png");
        File uniqueTargetFile = initialFile;
        int count = 1;
        while (uniqueTargetFile.exists()) {
            uniqueTargetFile = new File(screenshotsDir, baseName + "_" + count + ".png");
            count++;
        }
        final File outputFile = uniqueTargetFile;

        Util.ioPool().execute(() -> {
            try {
                if (cpuUpscale) {
                    BufferedImage rawBuf = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            int abgr = image.getPixelRGBA(x, y);
                            int a = (abgr >> 24) & 0xFF;
                            int b = (abgr >> 16) & 0xFF;
                            int g = (abgr >> 8) & 0xFF;
                            int r = abgr & 0xFF;
                            rawBuf.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                        }
                    }

                    double wr = (double) targetW / (double) rawBuf.getWidth();
                    double hr = (double) targetH / (double) rawBuf.getHeight();
                    AffineTransformOp op = new AffineTransformOp(
                            AffineTransform.getScaleInstance(wr, hr),
                            AffineTransformOp.TYPE_BICUBIC
                    );

                    BufferedImage scaledBuf = op.filter(rawBuf, null);
                    ImageIO.write(scaledBuf, "png", outputFile);
                } else {
                    image.writeToFile(outputFile);
                }

                mc.execute(() -> {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                    Component link = Component.literal(outputFile.getName())
                            .withStyle(ChatFormatting.UNDERLINE)
                            .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outputFile.getAbsolutePath())));
                    Component chatMsg = Component.translatable("screenshot.success", link);
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(chatMsg);
                    }

                    if (callback != null) {
                        callback.accept("Screenshot saved: " + outputFile.getName(), false);
                    }
                });

            } catch (Exception e) {
                MainClass.LOGGER.error("Failed to write screenshot file", e);
                mc.execute(() -> {
                    if (callback != null) {
                        callback.accept("Failed to save: " + e.getMessage(), true);
                    }
                });
            } finally {
                image.close();
            }
        });
    }
}