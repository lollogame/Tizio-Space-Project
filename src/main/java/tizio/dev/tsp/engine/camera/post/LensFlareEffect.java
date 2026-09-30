package tizio.dev.tsp.engine.camera.post;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tizio.dev.tsp.config.ConfigManager;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunInstance;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.engine.celestial.renderer.environment.PlanetSkyRenderer;
import tizio.dev.tsp.engine.client.ClientRenderRegistries;
import tizio.dev.tsp.engine.postprocess.PostProcessRenderer;
import tizio.dev.tsp.engine.postprocess.PostProcessUtil;

import java.util.ArrayList;
import java.util.List;

public final class LensFlareEffect {

    private static final ResourceLocation PIPELINE = PostProcessUtil.addShader("lens_flare");
    private static final float REFERENCE_DISTANCE = 256.0F;
    private static final float FADE_START_DIST = 0.0F;
    private static final float FADE_END_DIST = 500000.0F;
    private static final int MAX_SUNS = 4;
    private static final float SKY_SUN_RADIUS_FACTOR = 0.35F;

    public static void evaluate(RenderLevelStageEvent event) {

        if (!ConfigManager.hasPostLensFlare()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        boolean hasNormalSky = mc.level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL;
        boolean onBody = CelestialJsonLoader.isBodyDimension(mc.level.dimension().location().toString());

        List<SunInstance> allSuns = new ArrayList<>();
        if (hasNormalSky) {
            if (!onBody) {
                allSuns.addAll(ClientRenderRegistries.SUNS.instances());
            }
            allSuns.addAll(PlanetSkyRenderer.getActiveSuns());
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        List<ActiveSun> activeSuns = hasNormalSky ? collectActiveSuns(camPos, allSuns) : new ArrayList<>();

        if (activeSuns.isEmpty()) return;

        Matrix4f sunProj = new Matrix4f(event.getProjectionMatrix());
        Matrix4f sunView = new Matrix4f().rotationX((float) Math.toRadians(camera.getXRot())).rotateY((float) Math.toRadians(camera.getYRot() + 180.0F));

        Matrix4f relPosMat = new Matrix4f().zero();
        Matrix4f colorMat = new Matrix4f().zero();
        Vector4f visibilityVec = new Vector4f(0.0F);
        Vector4f scaleVec = new Vector4f(0.0F);
        Vector4f ignoreDepthVec = new Vector4f(0.0F);

        for (int i = 0; i < MAX_SUNS; i++) {
            if (i < activeSuns.size()) {
                ActiveSun active = activeSuns.get(i);
                relPosMat.setColumn(i, new Vector4f(active.relPos, 1.0F));
                colorMat.setColumn(i, new Vector4f(active.color, 1.0F));
                setComponent(visibilityVec, i, active.visibility);
                setComponent(scaleVec, i, active.scale);
                setComponent(ignoreDepthVec, i, active.ignoreDepth ? 1.0F : 0.0F);
            }
        }

        PostProcessRenderer.queueProcess(PIPELINE, effect -> {
            PostProcessUtil.setMatrix(effect, "SunProjMat", sunProj);
            PostProcessUtil.setMatrix(effect, "SunViewMat", sunView);
            PostProcessUtil.setMatrix(effect, "SunRelPosMat", relPosMat);
            PostProcessUtil.setMatrix(effect, "SunColorMat", colorMat);
            PostProcessUtil.setVec4(effect, "SunVisibility", visibilityVec);
            PostProcessUtil.setVec4(effect, "SunScale", scaleVec);
            PostProcessUtil.setVec4(effect, "SunIgnoreDepth", ignoreDepthVec);
        });
    }

    private static void setComponent(Vector4f vec, int index, float val) {
        switch (index) {
            case 0 -> vec.x = val;
            case 1 -> vec.y = val;
            case 2 -> vec.z = val;
            case 3 -> vec.w = val;
        }
    }

    private static List<ActiveSun> collectActiveSuns(Vec3 camPos, List<SunInstance> allSuns) {
        List<ActiveSun> result = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        float rainFactor = 1.0F;
        if (mc.level != null) {
            rainFactor = 1.0F - mc.level.getRainLevel(1.0F) * 0.75F;
        }

        List<SunInstance> planetSuns = PlanetSkyRenderer.getActiveSuns();

        for (SunInstance sun : allSuns) {
            Vec3 toSun = sun.position().subtract(camPos);
            float distance = (float) toSun.length();
            if (distance < 0.001F) continue;

            boolean isSkySun = planetSuns.contains(sun);

            float fadeFactor;
            float sunScale;

            if (isSkySun) {
                Vector3f dirNorm = new Vector3f((float) toSun.x, (float) toSun.y, (float) toSun.z).normalize();
                float horizonFade = Math.max(0.0F, Math.min(1.0F, (dirNorm.y + 0.05F) / 0.15F));
                fadeFactor = horizonFade * rainFactor;
                sunScale = sun.planetRadius() > 0 ? Math.max(0.7F, Math.min(1.6F, (sun.planetRadius() * SKY_SUN_RADIUS_FACTOR) / 100.0F)) : 1.0F;
            } else {
                fadeFactor = 1.0F - Math.max(0.0F, Math.min(1.0F, (distance - FADE_START_DIST) / (FADE_END_DIST - FADE_START_DIST)));
                sunScale = (float) (REFERENCE_DISTANCE / Math.max(1.0, distance));
            }

            if (fadeFactor <= 0.001F) continue;

            Vector3f relPos = new Vector3f((float) toSun.x, (float) toSun.y, (float) toSun.z);
            result.add(new ActiveSun(distance, relPos, sun.color(), fadeFactor, sunScale, false));
        }

        result.sort((a, b) -> Float.compare(a.distance, b.distance));
        return result.size() > MAX_SUNS ? result.subList(0, MAX_SUNS) : result;
    }

    private record ActiveSun(float distance, Vector3f relPos, Vector3f color, float visibility, float scale,
                             boolean ignoreDepth) {
    }
}