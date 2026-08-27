package tizio.dev.tsp.core.celestial.renderer;

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
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.celestial.instance.elements.blackhole.BlackHoleRenderer;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetSurfaceRenderer;
import tizio.dev.tsp.core.celestial.instance.elements.planet.atmosphere.AtmosphereRenderer;
import tizio.dev.tsp.core.celestial.instance.elements.planet.ring.PlanetRingRenderer;
import tizio.dev.tsp.core.celestial.instance.elements.planet.ring.PlanetRingRockRenderer;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunRenderer;
import tizio.dev.tsp.core.celestial.lighting.LightShadeManager;
import tizio.dev.tsp.core.client.ClientRenderRegistries;
import tizio.dev.tsp.core.client.ClientShaderRegistry;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.utils.Utils;
import tizio.dev.tsp.core.utils.volume.VolumeRenderUtil;

import java.util.List;

@Mod.EventBusSubscriber(modid = MainClass.MODID, value = Dist.CLIENT)
public final class SpaceRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        if(!Utils.inDimension("tsp:space")) return;

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
        LightShadeManager.update(camera, minecraft.level);

        if (ClientRenderRegistries.ATMOSPHERES.isEmpty()
                && ClientRenderRegistries.BLACK_HOLES.isEmpty()
                && ClientRenderRegistries.SUNS.isEmpty()
                && ClientRenderRegistries.PLANETS_RINGS.isEmpty()
                && ClientRenderRegistries.PLANETS_SURFACES.isEmpty()
                && ClientRenderRegistries.PLANETS_RING_ROCKS.isEmpty()
        ) return;

        List<VolumeRenderUtil.RenderTask> tasks = VolumeRenderUtil.mergeSorted(
                cameraPos,
                PlanetSurfaceRenderer.buildTasks(ClientShaderRegistry.planetSurface(), camera, frustum, poseStack, bufferSource, timeSeconds),
                PlanetRingRenderer.buildTasks(ClientShaderRegistry.planetRing(), camera, frustum, poseStack, bufferSource),
                PlanetRingRockRenderer.buildTasks(ClientShaderRegistry.planetRingRocks(), camera, frustum, poseStack, bufferSource, timeSeconds),
                SunRenderer.buildTasks(ClientShaderRegistry.sunShader(), camera, frustum, poseStack, bufferSource, timeSeconds),
                AtmosphereRenderer.buildTasks(ClientShaderRegistry.atmosphereShader(), camera, frustum, poseStack, bufferSource),
                BlackHoleRenderer.buildTasks(ClientShaderRegistry.blackHoleShader(), camera, frustum, poseStack, bufferSource, timeSeconds)
        );

        for (VolumeRenderUtil.RenderTask task : tasks) {
            task.render().run();
        }
    }
}
