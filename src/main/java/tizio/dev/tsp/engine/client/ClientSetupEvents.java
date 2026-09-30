package tizio.dev.tsp.engine.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.engine.camera.post.AspectRatioBarsEffect;
import tizio.dev.tsp.engine.camera.post.ColorCorrectionEffect;
import tizio.dev.tsp.engine.camera.post.FilmGrainEffect;
import tizio.dev.tsp.engine.camera.post.LensFlareEffect;
import tizio.dev.tsp.engine.postprocess.PostProcessRenderer;

public final class ClientSetupEvents {

    @Mod.EventBusSubscriber(modid = MainClass.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModBusEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                PostProcessRenderer.registerEvaluator(LensFlareEffect::evaluate);       //0
                PostProcessRenderer.registerEvaluator(ColorCorrectionEffect::evaluate); //1
                PostProcessRenderer.registerEvaluator(FilmGrainEffect::evaluate);       //2
                PostProcessRenderer.registerEvaluator(AspectRatioBarsEffect::evaluate); //3
            });
        }
    }

}