package tizio.dev.tsp.engine.camera.post;

import net.minecraftforge.client.event.RenderLevelStageEvent;
import tizio.dev.tsp.engine.postprocess.PostProcessRenderer;
import tizio.dev.tsp.engine.postprocess.PostProcessUtil;

public final class AspectRatioBarsEffect {

    public static final float DEFAULT_ASPECT_R = 2.39F;
    public static boolean enabled = false;
    public static float targetAspect = DEFAULT_ASPECT_R;

    public static void resetToDefaults() {
        targetAspect = DEFAULT_ASPECT_R;
        enabled = false;
    }

    public static void evaluate(RenderLevelStageEvent event) {
        if (!enabled) return;

        PostProcessRenderer.queueProcess(PostProcessUtil.addShader("aspect_ratio_bars"),
                effect -> {
                PostProcessUtil.setVec2(effect, "OutSize", PostProcessUtil.getWidth(), PostProcessUtil.getHeight());
                PostProcessUtil.setFloat(effect, "TargetAspect", targetAspect);
            }
        );
    }
}
