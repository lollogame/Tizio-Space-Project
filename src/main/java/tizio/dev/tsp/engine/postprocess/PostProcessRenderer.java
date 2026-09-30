package tizio.dev.tsp.engine.postprocess;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.MainClass;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = MainClass.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PostProcessRenderer {

    private static final List<PostProcessEvaluator> EVALUATORS = new ArrayList<>();
    private static final List<RenderRequest> REQUEST_QUEUE = new ArrayList<>();

    public static void registerEvaluator(PostProcessEvaluator evaluator) {
        if (evaluator != null && !EVALUATORS.contains(evaluator)) {
            EVALUATORS.add(evaluator);
        }
    }

    public static void queueProcess(ResourceLocation shaderLoc, Consumer<EffectInstance> uniformSetup) {
        if (shaderLoc != null) {
            REQUEST_QUEUE.add(new RenderRequest(shaderLoc, uniformSetup));
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        for (PostProcessEvaluator evaluator : EVALUATORS) {
            evaluator.evaluate(event);
        }

        if (REQUEST_QUEUE.isEmpty()) return;

        try {
            for (RenderRequest request : REQUEST_QUEUE) {
                PostChain chain = PostProcessRegistry.getOrLoad(request.shaderLoc());
                if (chain == null) {
                    continue;
                }

                if (request.uniformSetup() != null) {
                    for (PostPass pass : PostProcessUtil.getPasses(chain)) {
                        EffectInstance effect = PostProcessUtil.getEffect(pass);
                        if (effect != null) {
                            request.uniformSetup().accept(effect);
                        }
                    }
                }
                chain.process(event.getPartialTick());
                mc.getMainRenderTarget().bindWrite(true);
            }
        } finally {
            REQUEST_QUEUE.clear();
        }
    }

    @FunctionalInterface
    public interface PostProcessEvaluator {
        void evaluate(RenderLevelStageEvent event);
    }

    private record RenderRequest(ResourceLocation shaderLoc, Consumer<EffectInstance> uniformSetup) {
    }
}