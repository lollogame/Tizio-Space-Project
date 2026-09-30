package tizio.dev.tsp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tizio.dev.tsp.core.gui.PhotoModeScreen;
import tizio.dev.tsp.engine.camera.post.ColorCorrectionEffect;
import tizio.dev.tsp.engine.camera.post.FilmGrainEffect;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public final class PhotoModeSettings {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File SETTINGS_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "tsp/photo_mode.json");

    public static int screenshotScale = 1;
    public static PhotoModeScreen.CompositionGrid compositionGrid = PhotoModeScreen.CompositionGrid.NONE;
    public static boolean cutAspectEnabled = false;
    public static float targetAspect = 2.39F;
    public static float photoModeFov = -1.0F;
    public static boolean panelCollapsed = false;
    public static boolean filmGrainEnabled = false;
    public static float filmGrainIntensity = FilmGrainEffect.DEFAULT_INTENSITY;
    public static float filmGrainSize = FilmGrainEffect.DEFAULT_SIZE;

    private static boolean loaded = false;

    public static synchronized void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    public static synchronized void load() {
        loaded = true;
        if (!SETTINGS_FILE.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(SETTINGS_FILE)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data != null) {
                if (data.tonemapper != null) {
                    try {
                        ColorCorrectionEffect.tonemapper = ColorCorrectionEffect.Tonemapper.valueOf(data.tonemapper);
                    } catch (Exception ignored) {
                    }
                }
                ColorCorrectionEffect.exposure = data.exposure;
                ColorCorrectionEffect.contrast = data.contrast;
                ColorCorrectionEffect.saturation = data.saturation;
                ColorCorrectionEffect.vignetteIntensity = data.vignetteIntensity;
                ColorCorrectionEffect.customTemperature = data.customTemperature;
                if (data.customTemperature) {
                    ColorCorrectionEffect.temperature = data.temperature;
                } else {
                    ColorCorrectionEffect.temperature = ColorCorrectionEffect.calculateSunColorTemperature();
                }

                screenshotScale = Math.max(1, Math.min(4, data.screenshotScale));
                if (data.compositionGrid != null) {
                    try {
                        compositionGrid = PhotoModeScreen.CompositionGrid.valueOf(data.compositionGrid);
                    } catch (Exception ignored) {
                    }
                }
                cutAspectEnabled = data.cutAspect;
                targetAspect = data.targetAspect > 0 ? data.targetAspect : 2.39F;
                photoModeFov = data.photoModeFov;
                panelCollapsed = data.panelCollapsed;
                filmGrainEnabled = data.filmGrainEnabled;
                filmGrainIntensity = Math.max(0.0F, Math.min(1.0F, data.filmGrainIntensity));
                filmGrainSize = Math.max(0.25F, Math.min(4.0F, data.filmGrainSize));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load PhotoMode settings from {}", SETTINGS_FILE.getAbsolutePath(), e);
        }
    }

    public static synchronized void save() {
        try {
            File parent = SETTINGS_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Data data = new Data();
            data.tonemapper = ColorCorrectionEffect.tonemapper != null ? ColorCorrectionEffect.tonemapper.name() : ColorCorrectionEffect.DEFAULT_TONEMAPPER.name();
            data.exposure = ColorCorrectionEffect.exposure;
            data.contrast = ColorCorrectionEffect.contrast;
            data.saturation = ColorCorrectionEffect.saturation;
            data.vignetteIntensity = ColorCorrectionEffect.vignetteIntensity;
            data.customTemperature = ColorCorrectionEffect.customTemperature;
            data.temperature = ColorCorrectionEffect.temperature;

            data.screenshotScale = screenshotScale;
            data.compositionGrid = compositionGrid != null ? compositionGrid.name() : "NONE";
            data.cutAspect = cutAspectEnabled;
            data.targetAspect = targetAspect;
            data.photoModeFov = photoModeFov;
            data.panelCollapsed = panelCollapsed;
            data.filmGrainEnabled = filmGrainEnabled;
            data.filmGrainIntensity = filmGrainIntensity;
            data.filmGrainSize = filmGrainSize;

            try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save PhotoMode settings to {}", SETTINGS_FILE.getAbsolutePath(), e);
        }
    }

    public static class Data {
        public String tonemapper = ColorCorrectionEffect.DEFAULT_TONEMAPPER.name();
        public float exposure = ColorCorrectionEffect.DEFAULT_EXPOSURE;
        public float contrast = ColorCorrectionEffect.DEFAULT_CONTRAST;
        public float saturation = ColorCorrectionEffect.DEFAULT_SATURATION;
        public float vignetteIntensity = ColorCorrectionEffect.DEFAULT_VIGNETTE_INTENSITY;
        public float temperature = ColorCorrectionEffect.DEFAULT_TEMPERATURE;
        public boolean customTemperature = false;

        public int screenshotScale = 1;
        public String compositionGrid = "NONE";
        public boolean cutAspect = false;
        public float targetAspect = 2.39F;
        public float photoModeFov = -1.0F;
        public boolean panelCollapsed = false;
        public boolean filmGrainEnabled = false;
        public float filmGrainIntensity = FilmGrainEffect.DEFAULT_INTENSITY;
        public float filmGrainSize = FilmGrainEffect.DEFAULT_SIZE;
    }
}