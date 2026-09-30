package tizio.dev.tsp.engine.camera.post;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import tizio.dev.tsp.config.ConfigManager;
import tizio.dev.tsp.config.DataConfig;
import tizio.dev.tsp.config.PhotoModeSettings;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunInstance;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.utils.Utils;
import tizio.dev.tsp.engine.celestial.renderer.environment.PlanetSkyRenderer;
import tizio.dev.tsp.engine.client.ClientRenderRegistries;
import tizio.dev.tsp.engine.postprocess.PostProcessRenderer;
import tizio.dev.tsp.engine.postprocess.PostProcessUtil;

import java.util.ArrayList;
import java.util.List;

public final class ColorCorrectionEffect {

    public static final Tonemapper DEFAULT_TONEMAPPER = Tonemapper.ACES;
    public static final float DEFAULT_EXPOSURE = 0.90F;
    public static final float DEFAULT_CONTRAST = 0.85F;
    public static final float DEFAULT_SATURATION = 0.90F;
    public static final float DEFAULT_VIGNETTE_INTENSITY = 0.45F;
    public static final float DEFAULT_TEMPERATURE = 0.20F;
    public static Tonemapper tonemapper = DEFAULT_TONEMAPPER;
    public static float exposure = DEFAULT_EXPOSURE;
    public static float contrast = DEFAULT_CONTRAST;
    public static float saturation = DEFAULT_SATURATION;
    public static float vignetteIntensity = DEFAULT_VIGNETTE_INTENSITY;
    public static float temperature = DEFAULT_TEMPERATURE;
    public static boolean customTemperature = false;

    public static float colorToTemperature(Vector3f color) {
        if (color == null) return DEFAULT_TEMPERATURE;
        float max = Math.max(color.x(), Math.max(color.y(), color.z()));
        if (max <= 1e-5F) return 0.0F;
        float r = color.x() / max;
        float g = color.y() / max;
        float b = color.z() / max;
        float temp = (r - b) * 0.45F;
        return Utils.clamp(temp, -1.0F, 1.0F);
    }

    public static List<Vector3f> collectSunColors() {
        List<Vector3f> sunColors = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        if (CelestialJsonLoader.isSpaceDimension(mc.level)) {
            if (ClientRenderRegistries.SUNS != null) {
                for (SunInstance sun : ClientRenderRegistries.SUNS.instances()) {
                    if (sun != null && sun.color() != null) {
                        sunColors.add(sun.color());
                    }
                }
            }
        } else {
            for (SunInstance sun : PlanetSkyRenderer.getActiveSuns()) {
                if (sun != null && sun.color() != null) {
                    sunColors.add(sun.color());
                }
            }
        }

        if (sunColors.isEmpty()) {
            SolarSystemData system = null;
            if (mc.level != null) {
                String dimId = mc.level.dimension().location().toString();
                system = CelestialJsonLoader.getSolarSystemForSpaceDimension(dimId);
                if (system == null) {
                    system = CelestialJsonLoader.getSolarSystemForBodyDimension(dimId);
                }
            }
            if (system == null) {
                String activeId = CelestialJsonLoader.getActiveSelectedSystemId();
                if (activeId != null) {
                    system = CelestialJsonLoader.getActiveSystem(activeId);
                }
            }
            if (system == null && !CelestialJsonLoader.getActiveSystems().isEmpty()) {
                system = CelestialJsonLoader.getActiveSystems().values().iterator().next();
            }

            if (system != null) {
                if (system.star != null && system.star.enabled && !system.star.isBlackHole()) {
                    sunColors.add(CelestialJsonLoader.parseColor(system.star.colorHex, new Vector3f(DataConfig.Star.BUILDER_COLOR_DEF)));
                }
                if (system.bodies != null) {
                    for (PlanetInstance.Config body : system.bodies) {
                        if (body != null && !body.isBlackHole() && (body.isStar() || "star".equalsIgnoreCase(body.type) || "sun".equalsIgnoreCase(body.type))) {
                            sunColors.add(CelestialJsonLoader.parseColor(body.colorHex, new Vector3f(DataConfig.Star.BUILDER_COLOR_DEF)));
                        }
                    }
                }
            }
        }

        return sunColors;
    }

    public static Vector3f calculateAverageSunColor() {
        List<Vector3f> sunColors = collectSunColors();
        if (sunColors.isEmpty()) {
            return new Vector3f(DataConfig.Star.BUILDER_COLOR_DEF);
        }
        float r = 0.0F, g = 0.0F, b = 0.0F;
        for (Vector3f c : sunColors) {
            float max = Math.max(c.x(), Math.max(c.y(), c.z()));
            if (max > 1e-5F) {
                r += c.x() / max;
                g += c.y() / max;
                b += c.z() / max;
            } else {
                r += c.x();
                g += c.y();
                b += c.z();
            }
        }
        int n = sunColors.size();
        return new Vector3f(r / n, g / n, b / n);
    }

    public static float calculateSunColorTemperature() {
        List<Vector3f> sunColors = collectSunColors();
        if (sunColors.isEmpty()) {
            return DEFAULT_TEMPERATURE;
        }

        float totalTemp = 0.0F;
        for (Vector3f color : sunColors) {
            totalTemp += colorToTemperature(color);
        }
        return Utils.clamp(totalTemp / sunColors.size(), -1.0F, 1.0F);
    }

    public static float getDefaultTemperature() {
        return calculateSunColorTemperature();
    }

    public static float getEffectiveTemperature() {
        if (!customTemperature) {
            temperature = calculateSunColorTemperature();
        }
        return temperature;
    }

    public static void resetToDefaults() {
        tonemapper = DEFAULT_TONEMAPPER;
        exposure = DEFAULT_EXPOSURE;
        contrast = DEFAULT_CONTRAST;
        saturation = DEFAULT_SATURATION;
        vignetteIntensity = DEFAULT_VIGNETTE_INTENSITY;
        customTemperature = false;
        temperature = calculateSunColorTemperature();
    }

    public static void evaluate(RenderLevelStageEvent event) {

        if (!CelestialJsonLoader.isSpaceDimension(Minecraft.getInstance().player.level())) return;
        if (!ConfigManager.hasPostColorCorrection()) return;

        PhotoModeSettings.ensureLoaded();

        PostProcessRenderer.queueProcess(PostProcessUtil.addShader("color_correction"),
                effect -> {
                PostProcessUtil.setVec2(effect, "OutSize", PostProcessUtil.getWidth(), PostProcessUtil.getHeight());
                PostProcessUtil.setInt(effect, "TonemapperMode", tonemapper.getId());
                PostProcessUtil.setFloat(effect, "Exposure", exposure);
                PostProcessUtil.setFloat(effect, "Contrast", contrast);
                PostProcessUtil.setFloat(effect, "Saturation", saturation);
                PostProcessUtil.setFloat(effect, "VignetteIntensity", vignetteIntensity);
                PostProcessUtil.setFloat(effect, "Temperature", getEffectiveTemperature());
            }
        );
    }

    public enum Tonemapper {
        HABLE(0, "Hable"),
        ACES(1, "ACES"),
        UNREAL(2, "Unreal"),
        AGX(3, "AgX");

        private final int id;
        private final String label;

        Tonemapper(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public int getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }
    }

}