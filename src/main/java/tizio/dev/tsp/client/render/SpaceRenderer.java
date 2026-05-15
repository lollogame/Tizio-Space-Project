package tizio.dev.tsp.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.engine.elements.atmosphere.AtmosphereRenderer;
import tizio.dev.engine.elements.blackhole.BlackHoleRenderer;
import tizio.dev.engine.elements.planet.PlanetSurfaceRenderer;
import tizio.dev.engine.elements.ring.PlanetRingRenderer;
import tizio.dev.engine.elements.sun.SunRenderer;
import tizio.dev.engine.instance.ClientRenderRegistries;
import tizio.dev.engine.instance.ClientShaderRegistry;
import tizio.dev.engine.volume.VolumeRenderUtil;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.client.render.celestial.CelestialJsonLoader;

import java.util.List;

@Mod.EventBusSubscriber(modid = MainClass.MODID, value = Dist.CLIENT)
public final class SpaceRenderer {

    private SpaceRenderer() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        Camera camera = event.getCamera();
        Frustum frustum = event.getFrustum();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Vec3 cameraPos = camera.getPosition();
        float timeSeconds = (minecraft.level.getGameTime() + event.getPartialTick()) / 20.0F;

        CelestialJsonLoader.setCurrentDimension(minecraft.level.dimension().location());
        CelestialJsonLoader.ensureLoaded();
        CelestialJsonLoader.refreshDynamicPositions();

        if (ClientRenderRegistries.ATMOSPHERES.isEmpty()
                && ClientRenderRegistries.BLACK_HOLES.isEmpty()
                && ClientRenderRegistries.SUNS.isEmpty()
                && ClientRenderRegistries.PLANETS_RINGS.isEmpty()
                && ClientRenderRegistries.PLANETS_SURFACES.isEmpty()
        ) return;

        List<VolumeRenderUtil.RenderTask> tasks = VolumeRenderUtil.mergeSorted(
                cameraPos,
                PlanetSurfaceRenderer.buildTasks(ClientShaderRegistry.planetSurface(), camera, frustum, poseStack, bufferSource),
                PlanetRingRenderer.buildTasks(ClientShaderRegistry.planetRing(), camera, frustum, poseStack, bufferSource),
                SunRenderer.buildTasks(ClientShaderRegistry.sunShader(), camera, frustum, poseStack, bufferSource, timeSeconds),
                AtmosphereRenderer.buildTasks(ClientShaderRegistry.atmosphereShader(), camera, frustum, poseStack, bufferSource),
                BlackHoleRenderer.buildTasks(ClientShaderRegistry.blackHoleShader(), camera, frustum, poseStack, bufferSource, timeSeconds)
        );

        for (VolumeRenderUtil.RenderTask task : tasks) {
            task.render().run();
        }
    }
}
