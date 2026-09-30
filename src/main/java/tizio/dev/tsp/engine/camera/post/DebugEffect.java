package tizio.dev.tsp.engine.camera.post;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import tizio.dev.tsp.engine.postprocess.PostProcessRenderer;
import tizio.dev.tsp.engine.postprocess.PostProcessUtil;

public final class DebugEffect {

    public static boolean SHOW_DEPTH = false;

    public static void evaluate(RenderLevelStageEvent event) {
        if (SHOW_DEPTH) {
            PostProcessRenderer.queueProcess(PostProcessUtil.addShader("debug/debug_depth"),
                    effect -> {
                    PostProcessUtil.setFloat(effect, "ZNear", 0.05f);
                    PostProcessUtil.setFloat(effect, "ZFar", Minecraft.getInstance().gameRenderer.getRenderDistance() * 1.5f);
                }
            );
        }
    }
}
