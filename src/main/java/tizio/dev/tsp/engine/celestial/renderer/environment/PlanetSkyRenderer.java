package tizio.dev.tsp.engine.celestial.renderer.environment;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.config.ConfigManager;
import tizio.dev.tsp.config.DataConfig;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.celestial.instance.elements.blackhole.BlackHoleInstance;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunInstance;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.utils.Utils;
import tizio.dev.tsp.engine.celestial.instance.elements.blackhole.BlackHoleRenderer;
import tizio.dev.tsp.engine.celestial.instance.elements.planet.PlanetSurfaceRenderer;
import tizio.dev.tsp.engine.celestial.instance.elements.planet.atmosphere.AtmosphereRenderer;
import tizio.dev.tsp.engine.celestial.instance.elements.planet.clouds.SkyCloudsRenderer;
import tizio.dev.tsp.engine.celestial.instance.elements.planet.ring.PlanetRingRenderer;
import tizio.dev.tsp.engine.celestial.instance.elements.sun.SunRenderer;
import tizio.dev.tsp.engine.client.ClientShaderRegistry;
import tizio.dev.tsp.engine.volume.VolumeRenderUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MainClass.MODID, value = Dist.CLIENT)
public final class PlanetSkyRenderer {

    private static final float SKY_DOME_RADIUS = 3700.0f;
    private static final List<SunInstance> ACTIVE_SUNS = new ArrayList<>();
    private static final List<SunInstance> ACTIVE_SUNS_VIEW = Collections.unmodifiableList(ACTIVE_SUNS);
    private static final List<VolumeRenderUtil.RenderTask> PENDING_SURFACE_CLOUD_TASKS = new ArrayList<>();
    private static final List<VolumeRenderUtil.RenderTask> PENDING_SUN_TASKS = new ArrayList<>();
    public static boolean RENDER_FULL_SOLAR_SYSTEM = true;

    public static List<SunInstance> getActiveSuns() {
        return ACTIVE_SUNS_VIEW;
    }

    public static void clearActiveSuns() {
        ACTIVE_SUNS.clear();
        PENDING_SURFACE_CLOUD_TASKS.clear();
        PENDING_SUN_TASKS.clear();
    }

    @SubscribeEvent
    public static void onLoggingOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        clearActiveSuns();
    }

    public static void render(ClientLevel level, Camera camera, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float partialTick) {
        if (level == null || camera == null || poseStack == null || bufferSource == null) {
            clearActiveSuns();
            return;
        }

        PENDING_SURFACE_CLOUD_TASKS.clear();
        PENDING_SUN_TASKS.clear();

        String currentDimId = level.dimension().location().toString();
        if (CelestialJsonLoader.isSpaceDimension(currentDimId)) {
            clearActiveSuns();
            return;
        }

        CelestialJsonLoader.ensureLoaded();

        SolarSystemData system = findSystemForDimension(currentDimId);
        if (system == null) {
            clearActiveSuns();
            return;
        }

        PlanetInstance.Config currentBody = findBodyForDimension(system, currentDimId);
        if (currentBody == null) {
            clearActiveSuns();
            return;
        }

        float timeSeconds = (level.getGameTime() + partialTick) / 20.0f;
        Instant now = Instant.now();
        Map<String, Vec3> bodyPositions = CelestialJsonLoader.calculateBodyPositions(system, now, false);
        float globalScale = (float) Utils.clamp(system.globalScale, DataConfig.System.SCALE.min(), DataConfig.System.SCALE.max());
        Vec3 currentBodyPos = bodyPositions.getOrDefault(currentBody.id, origin(system));
        Vec3 starPos = getStarPosition(system, bodyPositions);
        Vec3 cameraPos = camera.getPosition();

        float sunAngle = level.getSunAngle(partialTick);
        Vector3f sunDirBase = new Vector3f(-(float) Math.sin(sunAngle), (float) Math.cos(sunAngle), 0.0f).normalize();
        Quaternionf skyTilt = SkyRenderContext.tiltRotation();
        Vector3f sunDirMC = skyTilt.transform(new Vector3f(sunDirBase), new Vector3f()).normalize();
        Vector3f starDirection = new Vector3f(
                (float) (starPos.x - currentBodyPos.x),
                (float) (starPos.y - currentBodyPos.y),
                (float) (starPos.z - currentBodyPos.z)
        );

        Quaternionf celestialToSky = new Quaternionf(skyTilt).mul(buildCelestialToSky(starDirection, sunDirBase));

        List<VolumeRenderUtil.RenderTask> allTasks = new ArrayList<>();
        List<VolumeRenderUtil.RenderTask> sunTasks = new ArrayList<>();
        List<SunInstance> currentFrameSuns = new ArrayList<>();
        double starDistance = -1.0;
        float starSkyExtent = 0.0f;

        if (system.star != null && system.star.enabled) {
            Vec3 toStarVec = starPos.subtract(currentBodyPos);
            double starDist = toStarVec.length();
            float r = (float) computeSkyRadius(DataConfig.Star.toBlocks(system.star.radius) * globalScale, starDist > 1.0e-6 ? starDist : 1000.0);
            starDistance = starDist > 1.0e-6 ? starDist : -1.0;
            starSkyExtent = r * 1.95f;
            float starDomeDist = SKY_DOME_RADIUS + 50.0f;
            Vec3 skyPosStar = cameraPos.add(toVec3(sunDirMC).scale(starDomeDist));
            Vector3f starColor = CelestialJsonLoader.parseColor(system.star.colorHex, new Vector3f(1.0f, 0.9f, 0.65f));

            Quaternionf starSpaceOrient = new Quaternionf().rotateXYZ(
                    (float) Math.toRadians(system.star.pitch),
                    (float) Math.toRadians(system.star.yaw),
                    (float) Math.toRadians(system.star.roll)
            );
            Quaternionf starSkyOrient = new Quaternionf(celestialToSky).mul(starSpaceOrient);

            SunInstance skyStarInst = SunInstance.at(skyPosStar)
                    .planetRadius(r)
                    .sunRadius(r * 1.95f)
                    .quadRadius(r * 1.8f)
                    .color(starColor)
                    .orientation(starSkyOrient)
                    .build();

            currentFrameSuns.add(skyStarInst);
            Vec3 starSortPos = cameraPos.add(toVec3(sunDirMC).scale(starDist > 1.0e-6 ? starDist : starDomeDist));
            addWithSortPosition(sunTasks, starSortPos, SunRenderer.buildTasks(ClientShaderRegistry.sunShader(), camera, null, poseStack, bufferSource, timeSeconds, List.of(skyStarInst)));
        }

        if(RENDER_FULL_SOLAR_SYSTEM) {
            for (PlanetInstance.Config body : system.bodies) {
                if (body == null || body.id == null || body.id.equalsIgnoreCase(currentBody.id)) continue;
                if (!RENDER_FULL_SOLAR_SYSTEM && !isRelatedCelestialBody(body, currentBody, system)) continue;

                Vec3 bodyPos = bodyPositions.getOrDefault(body.id, currentBodyPos);
                Vector3f relativeDirection = new Vector3f(
                        (float) (bodyPos.x - currentBodyPos.x),
                        (float) (bodyPos.y - currentBodyPos.y),
                        (float) (bodyPos.z - currentBodyPos.z)
                );

                double bodyDist = relativeDirection.length();
                if (bodyDist < 1.0e-6) continue;

                relativeDirection.div((float) bodyDist);
                Vector3f dirSky = celestialToSky.transform(relativeDirection, new Vector3f()).normalize();

                Vec3 skyPosSurface = cameraPos.add(toVec3(dirSky).scale(SKY_DOME_RADIUS));
                Vec3 skyPosRing = cameraPos.add(toVec3(dirSky).scale(SKY_DOME_RADIUS - 0.02));
                Vec3 skyPosAtmos = cameraPos.add(toVec3(dirSky).scale(SKY_DOME_RADIUS - 0.05));
                Vec3 bodySortPos = cameraPos.add(toVec3(dirSky).scale(bodyDist));

                double physRadius = DataConfig.Body.toBlocks(body.radius) * globalScale;
                float r = (float) computeSkyRadius(physRadius, bodyDist);

                boolean drawAfterStar = false;
                if (starDistance > 0.0 && bodyDist < starDistance) {
                    float extent = computeBodySkyExtent(body, r) + starSkyExtent;
                    float cosSeparation = Math.max(-1.0f, Math.min(1.0f, dirSky.dot(sunDirMC)));
                    drawAfterStar = Math.acos(cosSeparation) < Math.atan(extent / SKY_DOME_RADIUS);
                }
                List<VolumeRenderUtil.RenderTask> bodyTarget = drawAfterStar ? sunTasks : allTasks;

                Vec3 bodyToStar = starPos.subtract(bodyPos);
                Vector3f lightDir = bodyToStar.lengthSqr() > 1.0e-12
                        ? celestialToSky.transform(new Vector3f((float) bodyToStar.x, (float) bodyToStar.y, (float) bodyToStar.z).normalize(), new Vector3f()).normalize()
                        : new Vector3f(sunDirMC);

                Vector3f bodyColor = CelestialJsonLoader.parseColor(body.colorHex, new Vector3f(0.55f, 0.78f, 1.0f));
                boolean isParentPlanet = currentBody.parentId != null && currentBody.parentId.equalsIgnoreCase(body.id);

                float renderPitch = body.pitch + (isParentPlanet ? 90.0f : 0.0f);
                float renderYaw = body.yaw;
                float renderRoll = body.roll;

                Quaternionf bodySpaceOrient = new Quaternionf().rotateXYZ(
                        (float) Math.toRadians(renderPitch),
                        (float) Math.toRadians(renderYaw),
                        (float) Math.toRadians(renderRoll)
                );
                Quaternionf bodySkyOrient = new Quaternionf(celestialToSky).mul(bodySpaceOrient);

                if (body.isBlackHole()) {
                    Vector3f holeColor = CelestialJsonLoader.parseColor(body.colorHex, new Vector3f(1.0f, 0.72f, 0.22f));
                    BlackHoleInstance skyBlackHole = BlackHoleInstance.at(skyPosSurface)
                            .radius(r)
                            .quadRadius(Math.max(r * 9.5f, r))
                            .diskRotationSpeed(body.diskRotationSpeed)
                            .intensity(body.intensity)
                            .color(holeColor)
                            .orientation(bodySkyOrient)
                            .build();

                    addWithSortPosition(bodyTarget, bodySortPos, BlackHoleRenderer.buildTasks(ClientShaderRegistry.blackHoleShader(), camera, null, poseStack, bufferSource, timeSeconds, List.of(skyBlackHole)));
                    continue;
                }

                boolean isStar = body.isStar() || "star".equalsIgnoreCase(body.type) || "sun".equalsIgnoreCase(body.type);
                if (isStar) {
                    SunInstance skyBodyStar = SunInstance.at(skyPosSurface)
                            .planetRadius(r)
                            .sunRadius(r * 1.95f)
                            .quadRadius(r * 1.8f)
                            .color(bodyColor)
                            .orientation(bodySkyOrient)
                            .build();

                    currentFrameSuns.add(skyBodyStar);
                    addWithSortPosition(sunTasks, bodySortPos, SunRenderer.buildTasks(ClientShaderRegistry.sunShader(), camera, null, poseStack, bufferSource, timeSeconds, List.of(skyBodyStar)));
                    continue;
                }

                boolean renderSurface = body.surfaceEnabled || (body.texture != null && !body.texture.isBlank());
                if (renderSurface) {
                    float quadR = r * 1.05f;
                    PlanetInstance.SurfaceInstance.Builder sb = PlanetInstance.SurfaceInstance.at(skyPosSurface)
                            .planetRadius(r)
                            .quadRadius(quadR)
                            .dayTexture(body.texture)
                            .color(bodyColor)
                            .lightDirection(lightDir)
                            .orientation(bodySkyOrient)
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

                    addWithSortPosition(bodyTarget, bodySortPos, PlanetSurfaceRenderer.buildTasks(ClientShaderRegistry.planetSurface(), camera, null, poseStack, bufferSource, timeSeconds, List.of(sb.build())));
                }

                if (body.ring != null && body.ring.enabled) {
                    float bodyRadiusBlocks = DataConfig.Body.toBlocks(body.radius);
                    float innerR = (body.ring.innerRadius >= bodyRadiusBlocks ? body.ring.innerRadius / bodyRadiusBlocks : body.ring.innerRadius) * r;
                    float outerR = (body.ring.outerRadius >= bodyRadiusBlocks ? body.ring.outerRadius / bodyRadiusBlocks : body.ring.outerRadius) * r;
                    float ringQuadR = outerR * 1.02f;
                    Vector3f ringColor = CelestialJsonLoader.parseColor(body.ring.colorHex, new Vector3f(1, 1, 1));
                    float ringPitch = body.ring.pitch + (isParentPlanet ? 90.0f : 0.0f);

                    Quaternionf ringSpaceOrient = new Quaternionf().rotateXYZ(
                            (float) Math.toRadians(ringPitch),
                            (float) Math.toRadians(body.ring.yaw),
                            (float) Math.toRadians(body.ring.roll)
                    );
                    Quaternionf ringSkyOrient = new Quaternionf(celestialToSky).mul(ringSpaceOrient);

                    addWithSortPosition(bodyTarget, bodySortPos, PlanetRingRenderer.buildTasks(
                            ClientShaderRegistry.planetRing(), camera, null, poseStack, bufferSource,
                            List.of(PlanetInstance.RingInstance.at(skyPosRing)
                                    .planetRadius(r)
                                    .quadRadius(ringQuadR)
                                    .ringInnerRadius(innerR)
                                    .ringOuterRadius(outerR)
                                    .ringTexture(body.ring.texture)
                                    .color(ringColor)
                                    .lightDirection(lightDir)
                                    .orientation(ringSkyOrient)
                                    .build())));
                }

                if (body.atmosphere != null && body.atmosphere.enabled) {
                    float atmosRadius = CelestialJsonLoader.resolveAtmosphereRadiusConfig(body.atmosphere, r);
                    float atmosQuadR = atmosRadius * 1.05f;
                    Vector3f atmosColor = CelestialJsonLoader.parseColor(body.atmosphere.colorHex, bodyColor);

                    addWithSortPosition(bodyTarget, bodySortPos, AtmosphereRenderer.buildTasks(ClientShaderRegistry.atmosphereShader(), camera, null, poseStack, bufferSource,
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
                                    .orientation(bodySkyOrient)
                                    .build())));
                }
            }
        }

        if (currentBody.ring != null && currentBody.ring.enabled) {
            List<VolumeRenderUtil.RenderTask> ownRingTasks = new ArrayList<>();
            buildOwnRingTasks(currentBody, globalScale, sunAngle, camera, null, poseStack, bufferSource, cameraPos, ownRingTasks);
            addWithSortPosition(allTasks, cameraPos, ownRingTasks);
        }

        if (ConfigManager.enablePlanetClouds() && Minecraft.getInstance().options.cloudStatus().get() != net.minecraft.client.CloudStatus.OFF) {
            if ((!Utils.isModLoaded("simpleclouds") || Utils.isModLoaded("betterclouds")) && currentBody.clouds != null && currentBody.clouds.enabled) {
                PENDING_SURFACE_CLOUD_TASKS.addAll(SkyCloudsRenderer.buildTasks(
                        ClientShaderRegistry.skyClouds(),
                        poseStack,
                        partialTick,
                        cameraPos.x,
                        cameraPos.y,
                        cameraPos.z,
                        currentBody.clouds
                ));
            }
        }

        ACTIVE_SUNS.clear();
        ACTIVE_SUNS.addAll(currentFrameSuns);

        List<VolumeRenderUtil.RenderTask> sortedTasks = VolumeRenderUtil.mergeSorted(cameraPos, allTasks);
        for (VolumeRenderUtil.RenderTask task : sortedTasks) {
            task.render().run();
        }

        PENDING_SUN_TASKS.addAll(VolumeRenderUtil.mergeSorted(cameraPos, sunTasks));
    }

    public static void renderSurfaceCloudsAfterAtmosphere() {
        for (VolumeRenderUtil.RenderTask task : PENDING_SURFACE_CLOUD_TASKS) {
            task.render().run();
        }
        PENDING_SURFACE_CLOUD_TASKS.clear();
    }

    public static void renderSunsAfterAtmosphere() {
        for (VolumeRenderUtil.RenderTask task : PENDING_SUN_TASKS) {
            task.render().run();
        }
        PENDING_SUN_TASKS.clear();
    }

    private static float computeBodySkyExtent(PlanetInstance.Config body, float r) {
        float extent = r * 1.05f;

        if (body.ring != null && body.ring.enabled) {
            float bodyRadiusBlocks = DataConfig.Body.toBlocks(body.radius);
            float outerR = (body.ring.outerRadius >= bodyRadiusBlocks ? body.ring.outerRadius / bodyRadiusBlocks : body.ring.outerRadius) * r;
            extent = Math.max(extent, outerR * 1.02f);
        }

        if (body.atmosphere != null && body.atmosphere.enabled) {
            float atmosRadius = CelestialJsonLoader.resolveAtmosphereRadiusConfig(body.atmosphere, r);
            extent = Math.max(extent, atmosRadius * 1.05f);
        }

        return extent;
    }

    private static void addWithSortPosition(List<VolumeRenderUtil.RenderTask> target, Vec3 sortPosition, List<VolumeRenderUtil.RenderTask> source) {
        for (VolumeRenderUtil.RenderTask task : source) {
            target.add(new VolumeRenderUtil.RenderTask(sortPosition, task.pass(), task.render()));
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

        return currentBody.parentId != null
                && !currentBody.parentId.equalsIgnoreCase(starId)
                && body.parentId != null
                && currentBody.parentId.equalsIgnoreCase(body.parentId);
    }

    private static void buildOwnRingTasks(PlanetInstance.Config body, float globalScale, float sunAngle, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 cameraPos, List<VolumeRenderUtil.RenderTask> ringTasks) {

        float physPlanetR = DataConfig.Body.toBlocks(body.radius) * globalScale;
        if (physPlanetR < 1e-6f) return;

        float bodyRadiusBlocks = DataConfig.Body.toBlocks(body.radius);
        float innerRatio = body.ring.innerRadius >= bodyRadiusBlocks ? body.ring.innerRadius / bodyRadiusBlocks : body.ring.innerRadius;
        float outerRatio = body.ring.outerRadius >= bodyRadiusBlocks ? body.ring.outerRadius / bodyRadiusBlocks : body.ring.outerRadius;

        float planetRadius = SKY_DOME_RADIUS;
        float innerR = innerRatio * planetRadius;
        float outerR = outerRatio * planetRadius;

        Vec3 planetCenter = new Vec3(cameraPos.x, cameraPos.y - (double) SKY_DOME_RADIUS, cameraPos.z);
        float quadRadius = outerR * 1.05f;

        Vector3f lightDir = SkyRenderContext.tiltRotation().transform(new Vector3f(-(float) Math.sin(sunAngle), (float) Math.cos(sunAngle), 0.0f), new Vector3f()).normalize();
        Vector3f ringColor = CelestialJsonLoader.parseColor(body.ring.colorHex, new Vector3f(1, 1, 1));

        Quaternionf orientation = new Quaternionf()
                .rotateY((float) Math.toRadians(body.ring.yaw))
                .rotateX((float) (Math.PI * 0.5 - Math.toRadians(body.ring.pitch)))
                .rotateZ((float) Math.toRadians(body.ring.roll));

        ringTasks.addAll(PlanetRingRenderer.buildTasks(ClientShaderRegistry.planetRing(), camera, frustum, poseStack, bufferSource,
                List.of(PlanetInstance.RingInstance.at(planetCenter)
                        .planetRadius(planetRadius * 0.98F)
                        .quadRadius(quadRadius)
                        .ringInnerRadius(innerR)
                        .ringOuterRadius(outerR)
                        .ringTexture(body.ring.texture)
                        .color(ringColor)
                        .lightDirection(lightDir)
                        .orientation(orientation)
                        .build())));
    }

    private static Quaternionf buildCelestialToSky(Vector3f starDirection, Vector3f sunDirMC) {
        if (starDirection.lengthSquared() <= 1.0e-8F) {
            return new Quaternionf();
        }

        Vector3f fromU = new Vector3f(starDirection).normalize();
        Vector3f fromN = new Vector3f(0.0F, 1.0F, 0.0F);
        fromN.sub(new Vector3f(fromU).mul(fromN.dot(fromU)));
        if (fromN.lengthSquared() <= 1.0e-8F) {
            return rotationTo(starDirection, sunDirMC);
        }
        fromN.normalize();
        Vector3f fromV = new Vector3f(fromU).cross(fromN).normalize();

        Vector3f toU = new Vector3f(sunDirMC).normalize();
        Vector3f toN = new Vector3f(0.0F, 0.0F, 1.0F);
        Vector3f toV = new Vector3f(toU).cross(toN).normalize();

        Matrix3f from = new Matrix3f(fromU, fromN, fromV);
        Matrix3f to = new Matrix3f(toU, toN, toV);
        Matrix3f rotation = new Matrix3f(to).mul(new Matrix3f(from).transpose());

        return new Quaternionf().setFromNormalized(rotation).normalize();
    }

    private static Quaternionf rotationTo(Vector3f from, Vector3f to) {
        if (from.lengthSquared() <= 1.0e-8F || to.lengthSquared() <= 1.0e-8F) {
            return new Quaternionf();
        }

        Vector3f source = new Vector3f(from).normalize();
        Vector3f target = new Vector3f(to).normalize();
        float dot = source.dot(target);

        if (dot > 0.999999F) {
            return new Quaternionf();
        }

        if (dot < -0.999999F) {
            Vector3f axis = Math.abs(source.x()) < 0.9F
                    ? new Vector3f(1.0F, 0.0F, 0.0F)
                    : new Vector3f(0.0F, 1.0F, 0.0F);
            axis.cross(source).normalize();
            return new Quaternionf().rotationAxis((float) Math.PI, axis.x(), axis.y(), axis.z());
        }

        return new Quaternionf().rotationTo(source, target);
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