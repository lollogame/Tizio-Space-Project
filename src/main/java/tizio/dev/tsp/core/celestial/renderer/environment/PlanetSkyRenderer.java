package tizio.dev.tsp.core.celestial.renderer.environment;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetSurfaceRenderer;
import tizio.dev.tsp.core.celestial.instance.elements.planet.atmosphere.AtmosphereRenderer;
import tizio.dev.tsp.core.celestial.instance.elements.planet.clouds.SkyCloudsRenderer;
import tizio.dev.tsp.core.celestial.instance.elements.planet.ring.PlanetRingRenderer;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunInstance;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunRenderer;
import tizio.dev.tsp.core.client.ClientShaderRegistry;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.utils.Utils;
import tizio.dev.tsp.core.utils.volume.VolumeRenderUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MainClass.MODID, value = Dist.CLIENT)
public final class PlanetSkyRenderer {

    private PlanetSkyRenderer() {}

    private static final float SKY_DOME_RADIUS = 2600.0f;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        String currentDimId = mc.level.dimension().location().toString();
        if ("tsp:space".equals(currentDimId)) return;

        CelestialJsonLoader.ensureLoaded();

        SolarSystemData system = findSystemForDimension(currentDimId);
        if (system == null) return;

        PlanetInstance.Config currentBody = findBodyForDimension(system, currentDimId);

        Camera camera = event.getCamera();
        Frustum frustum  = event.getFrustum();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        ClientLevel level = mc.level;
        float partialTick = event.getPartialTick();
        float timeSeconds = (level.getGameTime() + partialTick) / 20.0f;
        Instant now = Instant.now();
        Map<String, Vec3> bodyPositions = CelestialJsonLoader.calculateBodyPositions(system, now);
        float globalScale  = system.globalScale;
        Vec3 currentBodyPos = currentBody != null ? bodyPositions.getOrDefault(currentBody.id, origin(system)) : origin(system);
        Vec3 starPos = getStarPosition(system, bodyPositions);
        Vec3 cameraPos = camera.getPosition();

        // ── 1. VETTORE SOLE VANILLA ───────────────────────────────────────────────
        float sunAngle = level.getSunAngle(partialTick);
        Vector3f sunDirMC = new Vector3f(-(float) Math.sin(sunAngle), (float) Math.cos(sunAngle), 0.0f);

        List<VolumeRenderUtil.RenderTask> allTasks = new ArrayList<>();

        if (system.star != null && system.star.enabled) {

            Vec3 toStarVec  = starPos.subtract(currentBodyPos);
            double starDist = toStarVec.length();
            float r = (float) computeSkyRadius((system.star.radius * 0.35f) * globalScale, starDist > 1e-6 ? starDist : 1000.0);
            float starDomeDist = SKY_DOME_RADIUS + 50.0f;
            Vec3 skyPosStar = cameraPos.add(toVec3(sunDirMC).scale(starDomeDist));
            Vector3f starColor = CelestialJsonLoader.parseColor(system.star.colorHex, new Vector3f(1.0f, 0.9f, 0.65f));

            SunInstance skyStarInst = SunInstance.at(skyPosStar)
                    .planetRadius(r)
                    .sunRadius(r * 1.95f)
                    .quadRadius(r * 1.8f)
                    .color(starColor)
                    .eulerDegrees(0, 0, 0)
                    .build();

            allTasks.addAll(SunRenderer.buildTasks(ClientShaderRegistry.sunShader(), camera, frustum, poseStack, bufferSource, timeSeconds, List.of(skyStarInst)));
        }


        for (PlanetInstance.Config body : system.bodies) {
            if (currentBody != null && body.id.equalsIgnoreCase(currentBody.id)) continue;
            if (!isRelatedCelestialBody(body, currentBody, system)) continue;

            Vector3f dirSky = new Vector3f(-sunDirMC.x(), -sunDirMC.y(), -sunDirMC.z()).normalize();

            if (dirSky.y() < -0.2f) continue;

            Vec3 skyPosSurface = cameraPos.add(toVec3(dirSky).scale(SKY_DOME_RADIUS));
            Vec3 skyPosRing    = cameraPos.add(toVec3(dirSky).scale(SKY_DOME_RADIUS - 0.02));
            Vec3 skyPosAtmos   = cameraPos.add(toVec3(dirSky).scale(SKY_DOME_RADIUS - 0.05));

            double physRadius = body.radius * globalScale;
            Vec3 bodyPos = bodyPositions.getOrDefault(body.id, currentBodyPos);
            double bodyDist = bodyPos.subtract(currentBodyPos).length();
            if (bodyDist < 1e-6) bodyDist = 1800.0;

            float r = (float) computeSkyRadius(physRadius, bodyDist);

            Vector3f lightDir = new Vector3f(sunDirMC).normalize();
            Vector3f bodyColor = CelestialJsonLoader.parseColor(body.colorHex, new Vector3f(0.55f, 0.78f, 1.0f));

            boolean renderSurface = body.surfaceEnabled || (body.texture != null && !body.texture.isBlank());
            if (renderSurface) {
                float quadR = r * 1.05f;
                PlanetInstance.SurfaceInstance.Builder sb = PlanetInstance.SurfaceInstance.at(skyPosSurface)
                        .planetRadius(r)
                        .quadRadius(quadR)
                        .dayTexture(body.texture)
                        .color(bodyColor)
                        .lightDirection(lightDir)
                        .eulerDegrees(body.yaw, body.pitch, body.roll)
                        .spinHours(body.spinHours);

                if (body.nightTexture != null && !body.nightTexture.isBlank()) {
                    sb.nightTexture(body.nightTexture);
                }
                if (body.clouds != null && body.clouds.enabled) {
                    sb.cloudsEnabled(true)
                            .cloudTexture(body.clouds.texture)
                            .cloudHeight(body.clouds.height)
                            .cloudCoverage(body.clouds.density)
                            .cloudWindSpeed(body.clouds.windSpeed)
                            .cloudColor(CelestialJsonLoader.parseColor(body.clouds.colorHex, new Vector3f(1, 1, 1)))
                            .cloudAlpha(body.clouds.alpha)
                            .cloudNoiseScale(body.clouds.noiseScale);
                } else {
                    sb.cloudsEnabled(false);
                }

                allTasks.addAll(PlanetSurfaceRenderer.buildTasks(ClientShaderRegistry.planetSurface(), camera, frustum, poseStack, bufferSource, timeSeconds, List.of(sb.build())));
            }

            if (body.ring != null && body.ring.enabled) {

                float scaleF = r / (float) physRadius;
                float innerR = body.ring.innerRadius * globalScale * scaleF;
                float outerR = body.ring.outerRadius * globalScale * scaleF;
                float ringQuadR = outerR * 1.02f;
                Vector3f ringColor = CelestialJsonLoader.parseColor(body.ring.colorHex, new Vector3f(1, 1, 1));

                allTasks.addAll(PlanetRingRenderer.buildTasks(
                        ClientShaderRegistry.planetRing(), camera, frustum, poseStack, bufferSource,
                        List.of(PlanetInstance.RingInstance.at(skyPosRing)
                                .planetRadius(r)
                                .quadRadius(ringQuadR)
                                .ringInnerRadius(innerR)
                                .ringOuterRadius(outerR)
                                .ringTexture(body.ring.texture)
                                .color(ringColor)
                                .lightDirection(lightDir)
                                .eulerDegrees(body.ring.yaw, body.ring.pitch, body.ring.roll)
                                .build())));
            }

            if (body.atmosphere != null && body.atmosphere.enabled) {

                float atmosRadius = CelestialJsonLoader.resolveAtmosphereRadiusConfig(body.atmosphere, r);
                float atmosQuadR  = atmosRadius * 1.05f;
                Vector3f atmosColor = CelestialJsonLoader.parseColor(body.atmosphere.colorHex, bodyColor);

                allTasks.addAll(AtmosphereRenderer.buildTasks(ClientShaderRegistry.atmosphereShader(), camera, frustum, poseStack, bufferSource,
                        List.of(PlanetInstance.AtmosphereInstance.at(skyPosAtmos)
                                .planetRadius(r)
                                .quadRadius(atmosQuadR)
                                .atmosphereRadius(atmosRadius)
                                .exposure(body.atmosphere.exposure)
                                .intensity(body.atmosphere.intensity)
                                .rayleighScaleHeight(body.atmosphere.rayleighScaleHeight)
                                .rayleighStrength(body.atmosphere.rayleighStrength)
                                .color(atmosColor)
                                .waveLengths(body.atmosphere.wavelengthR, body.atmosphere.wavelengthG, body.atmosphere.wavelengthB)
                                .lightDirection(lightDir)
                                .eulerDegrees(body.yaw, body.pitch, body.roll)
                                .build())));
            }
        }

        if (currentBody != null && currentBody.ring != null && currentBody.ring.enabled) {
            buildOwnRingTasks(currentBody, globalScale, sunDirMC, camera, frustum, poseStack, bufferSource, cameraPos, allTasks);
        }

        // ── RENDERING NUVOLE PIATTE DEL PIANETA CORRENTE ──────────────────────────
        if ((!Utils.isModLoaded("simpleclouds") || Utils.isModLoaded("betterclouds")) && (currentBody != null && currentBody.clouds != null && currentBody.clouds.enabled)) {

            allTasks.addAll(SkyCloudsRenderer.buildTasks(
                    ClientShaderRegistry.skyClouds(),
                    poseStack,
                    partialTick,
                    cameraPos.x,
                    cameraPos.y,
                    cameraPos.z,
                    currentBody.clouds
            ));
        }

        List<VolumeRenderUtil.RenderTask> sortedTasks = VolumeRenderUtil.mergeSorted(cameraPos, allTasks);
        for (VolumeRenderUtil.RenderTask task : sortedTasks) {
            task.render().run();
        }
    }

    private static boolean isRelatedCelestialBody(PlanetInstance.Config body, PlanetInstance.Config currentBody, SolarSystemData system) {
        if (body == null || currentBody == null) return false;

        String starId = (system.star != null && system.star.id != null && !system.star.id.isBlank()) ? system.star.id : "sun";

        if (body.parentId != null && body.parentId.equalsIgnoreCase(currentBody.id)) {
            return true;
        }

        if (currentBody.parentId != null && currentBody.parentId.equalsIgnoreCase(body.id)) {
            return true;
        }

        if (currentBody.parentId != null
                && !currentBody.parentId.equalsIgnoreCase(starId)
                && body.parentId != null
                && currentBody.parentId.equalsIgnoreCase(body.parentId)) {
            return true;
        }

        return false;
    }

    private static void buildOwnRingTasks(PlanetInstance.Config body, float globalScale, Vector3f sunDirMC, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 cameraPos, List<VolumeRenderUtil.RenderTask> ringTasks) {

        float physPlanetR = body.radius * globalScale;
        if (physPlanetR < 1e-6f) return;

        float scaleToSky = (float) (SKY_DOME_RADIUS / physPlanetR);
        float innerR     = body.ring.innerRadius * globalScale * scaleToSky;
        float outerR     = body.ring.outerRadius * globalScale * scaleToSky;

        org.joml.Quaternionf rot = new org.joml.Quaternionf().rotationZYX((float) Math.toRadians(body.ring.roll), (float) Math.toRadians(body.ring.pitch), (float) Math.toRadians(body.ring.yaw));
        Vector3f localUp = new Vector3f(0.0f, (float) SKY_DOME_RADIUS, 0.0f);
        localUp.rotate(rot);

        Vec3 planetCenter = cameraPos.subtract(toVec3(localUp));

        float ringQuadR = (outerR + (float) SKY_DOME_RADIUS) * 1.5f;
        float ringRadiusMult = 1.8f;

        Vector3f lightDir = new Vector3f(sunDirMC).normalize();
        Vector3f ringColor = CelestialJsonLoader.parseColor(body.ring.colorHex, new Vector3f(1, 1, 1));

        ringTasks.addAll(PlanetRingRenderer.buildTasks(ClientShaderRegistry.planetRing(), camera, frustum, poseStack, bufferSource,
                List.of(PlanetInstance.RingInstance.at(planetCenter)
                        .planetRadius((float) SKY_DOME_RADIUS * ringRadiusMult)
                        .quadRadius(ringQuadR * ringRadiusMult)
                        .ringInnerRadius(innerR * ringRadiusMult)
                        .ringOuterRadius(outerR * ringRadiusMult)
                        .ringTexture(body.ring.texture)
                        .color(ringColor)
                        .lightDirection(lightDir)
                        .eulerDegrees(body.ring.yaw, body.ring.pitch, body.ring.roll)
                        .build())));
    }

    private static double computeSkyRadius(double physRadius, double realDist) {
        if (realDist <= 1e-6) return physRadius;
        return (physRadius / realDist) * SKY_DOME_RADIUS;
    }

    private static Vec3 getStarPosition(SolarSystemData system, Map<String, Vec3> bodyPositions) {
        if (system.star != null && system.star.enabled) {
            String starId = (system.star.id != null && !system.star.id.isBlank()) ? system.star.id : "sun";
            return bodyPositions.getOrDefault(starId, origin(system));
        }
        return origin(system);
    }

    private static Vec3 origin(SolarSystemData system) {
        return new Vec3(system.originX, system.originY, system.originZ);
    }

    private static Vec3 toVec3(Vector3f v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    private static SolarSystemData findSystemForDimension(String dimensionId) {
        for (SolarSystemData system : CelestialJsonLoader.getActiveSystems().values()) {
            if (dimensionId.equalsIgnoreCase(system.dimension)) return system;
            for (PlanetInstance.Config body : system.bodies) {
                if (dimensionId.equalsIgnoreCase(body.dimension)) return system;
            }
        }
        return null;
    }

    private static PlanetInstance.Config findBodyForDimension(SolarSystemData system, String dimensionId) {
        for (PlanetInstance.Config body : system.bodies) {
            if (dimensionId.equalsIgnoreCase(body.dimension)) return body;
        }
        return null;
    }
}