package tizio.dev.tsp.engine.camera.post;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import tizio.dev.tsp.core.gui.PhotoModeScreen;
import tizio.dev.tsp.engine.postprocess.PostProcessRenderer;
import tizio.dev.tsp.engine.postprocess.PostProcessUtil;

public final class FilmGrainEffect {

    public static final float DEFAULT_INTENSITY = 0.35F;
    public static final float DEFAULT_SIZE = 1.0F;
    public static boolean enabled = false;
    public static float intensity = DEFAULT_INTENSITY;
    public static float size = DEFAULT_SIZE;

    private FilmGrainEffect() {
    }

    public static void resetToDefaults() {
        enabled = false;
        intensity = DEFAULT_INTENSITY;
        size = DEFAULT_SIZE;
    }

    public static void evaluate(RenderLevelStageEvent event) {
        if (!enabled || intensity <= 0.0F) return;
        if (!(Minecraft.getInstance().screen instanceof PhotoModeScreen)) return;

        float time = (float) ((System.nanoTime() / 1_000_000L) % 100000L) / 1000.0F;

        PostProcessRenderer.queueProcess(PostProcessUtil.addShader("film_grain"),
                effect -> {
                PostProcessUtil.setVec2(effect, "OutSize", PostProcessUtil.getWidth(), PostProcessUtil.getHeight());
                PostProcessUtil.setFloat(effect, "Intensity", intensity);
                PostProcessUtil.setFloat(effect, "GrainSize", size);
                PostProcessUtil.setFloat(effect, "GrainTime", time);
            }
        );
    }
}