package tizio.dev.tsp.config;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tizio.dev.tsp.core.handlers.gravity.GravityManager;

import java.util.Set;

public final class DataConfig {

    private DataConfig() {}

    public record Slider(double def, double min, double max) {
        public float defF() { return (float) def; }
        public float minF() { return (float) min; }
        public float maxF() { return (float) max; }
        public int defI() { return (int) def; }
        public int minI() { return (int) min; }
        public int maxI() { return (int) max; }
    }

    public static final class System {
        private System() {}
        public static final String DEFAULT_SPACE_DIMENSION = "tsp:space";
        public static final Set<String> VANILLA_DIMENSION_BLACKLIST = Set.of(
            "minecraft:overworld",
            "minecraft:the_nether",
            "minecraft:the_end"
        );
        public static final String DEFAULT_SYSTEM_ID = "new_system";
        public static final String NEW_SYSTEM_ID_PREFIX = "system_";
        public static final Vec3 ORIGIN_DEF = new Vec3(0.0, 500.0, 0.0);
        public static final Slider ORIGIN_X = new Slider(0.0, -100000.0, 100000.0);
        public static final Slider ORIGIN_Y = new Slider(500.0, -100000.0, 100000.0);
        public static final Slider ORIGIN_Z = new Slider(0.0, -100000.0, 100000.0);
        public static final Slider SCALE = new Slider(5.50, 5.50, 15.0);
        public static final double GRAVITY_DEF = 0.0D;
        public static final String EXPORT_DIR = "config/tsp/exported_solar_systems/";
    }

    public static final class Star {
        private Star() {}
        public static final String ID_DEF = "sun";
        public static final String TYPE_DEF = "star";
        public static final String COLOR_HEX_DEF = "#ffd48a";
        public static final boolean ENABLED_DEF = true;
        public static final Slider RADIUS = new Slider(2000.0, 100.0, 10000.0);
        public static final Slider DISK_ROTATION_SPEED = new Slider(0.20, 0.0, 5.0);
        public static final Slider INTENSITY = new Slider(1.0, 0.0, 2.0);
        public static final Slider YAW = new Slider(0.0, -180.0, 180.0);
        public static final Slider PITCH = new Slider(0.0, -180.0, 180.0);
        public static final Slider ROLL = new Slider(0.0, -180.0, 180.0);
        public static final float MAX_RADIUS = 100_000.0F;
        public static final float SUN_RADIUS_FACTOR_DEF = 1.3F;
        public static final float SCATTERING_STRENGTH_DEF = 0.5F;
        public static final float DENSITY_FALLOFF_DEF = 5.0F;
        public static final float BUILDER_PLANET_RADIUS_DEF = 1.0F;
        public static final float BUILDER_SUN_RADIUS_DEF = 1.95F;
        public static final Vector3f BUILDER_COLOR_DEF = new Vector3f(1.0F, 0.90F, 0.72F);
        public static final float BUILDER_QUAD_SCALE_DEF = 1.2F;
    }

    public static final class BlackHole {
        private BlackHole() {}
        public static final String ID_DEF = "blackhole";
        public static final String PARENT_ID_DEF = "sun";
        public static final float RADIUS_DEF = 100.0F;
        public static final String COLOR_HEX_DEF = "#000000";
        public static final float YAW_DEF = 0.0F;
        public static final float PITCH_DEF = 0.0F;
        public static final float ROLL_DEF = 0.0F;
        public static final float DISK_ROTATION_SPEED_DEF = 0.20F;
        public static final float INTENSITY_DEF = 1.0F;
        public static final float BUILDER_RADIUS_DEF = 1.0F;
    }

    public static final class Body {
        private Body() {}
        public static final String ID_DEF = "body";
        public static final String UNNAMED_BODY_ID = "unnamed_body";
        public static final String NEW_BODY_ID = "new_planet";
        public static final String TYPE_DEF = "planet";
        public static final String PARENT_ID_DEF = "sun";
        public static final String TEXTURE_DEF = "earth_mat_0";
        public static final String TEXTURE_NONE = "";
        public static final String NIGHT_TEXTURE_DEF = "";
        public static final String COLOR_HEX_DEF = "#7fb8ff";
        public static final String DIMENSION_DEF = "";
        public static final boolean OXYGEN_DEF = false;
        public static final Slider RADIUS = new Slider(150.0, 60.0, 1800.0);
        public static final float FALLBACK_RADIUS = 60.0F;
        public static final float DEFAULT_SYSTEM_BODY_RADIUS = 250.0F;
        public static final float NEW_SYSTEM_BODY_RADIUS = 200.0F;
        public static final float MOON_RADIUS = 60.0F;
        public static final float BLACKHOLE_RADIUS = 300.0F;
        public static final float PLANET_RADIUS = 150.0F;
        public static final float PLANET_INSTANCE_RADIUS = 100.0F;
        public static final float RESET_RADIUS = 150.0F;
        public static final float MIN_PHYSICAL_RADIUS = 0.05F;
        public static final float MAX_RADIUS = 10_000.0F;
        public static final Slider GRAVITY = new Slider(GravityManager.EARTH_GRAVITY_MS2, 0.0, 50.0);
        public static final double GRAVITY_FALLBACK = 9.81D;
        public static final Slider TEMPERATURE = new Slider(0.0, -1.0, 1.0);
        public static final Slider DISK_ROTATION_SPEED = new Slider(0.20, 0.0, 5.0);
        public static final Slider INTENSITY = new Slider(1.0, 0.0, 10.0);
        public static final Slider SPIN_HOURS = new Slider(24.0, 0.0, 1000.0);
        public static final Slider ROT_YAW = new Slider(0.0, -180.0, 180.0);
        public static final Slider ROT_PITCH = new Slider(0.0, -180.0, 180.0);
        public static final Slider ROT_ROLL = new Slider(0.0, -180.0, 180.0);
        public static final boolean SURFACE_ENABLED_DEF = true;
        public static final Vector3f SURFACE_COLOR_RGB_DEF = new Vector3f(0.55F, 0.78F, 1.0F);
        public static final String COPY_SUFFIX = "_copy";
        public static final double COPY_ORBIT_OFFSET = 1000.0;
    }

    public static final class Orbit {
        private Orbit() {}
        public static final boolean ENABLED_DEF = true;
        public static final String EPOCH_UTC_DEF = "2000-01-01T12:00:00Z";
        public static final Slider RADIUS = new Slider(16000.0, 0.0, 200000.0);
        public static final double RADIUS_FALLBACK = 0.0;
        public static final double NEW_SYSTEM_RADIUS = 12000.0;
        public static final double MOON_RADIUS = 1200.0;
        public static final double BODY_STEP_RADIUS = 8000.0;
        public static final Slider PERIOD_DAYS = new Slider(365.25, 0.1, 60000.0);
        public static final double PERIOD_DAYS_FALLBACK = 0.0;
        public static final double MOON_PERIOD_DAYS = 27.0;
        public static final double BODY_STEP_PERIOD_DAYS = 100.0;
        public static final Slider INCLINATION = new Slider(0.0, -90.0, 90.0);
        public static final Slider ASCENDING_NODE = new Slider(0.0, 0.0, 360.0);
        public static final Slider EPOCH_ANGLE = new Slider(0.0, 0.0, 360.0);
        public static final Slider VERTICAL_OFFSET = new Slider(0.0, -2000.0, 2000.0);
        public static final double MAX_SCALED_RADIUS = 30_000_000.0;
    }

    public static final class Clouds {
        private Clouds() {}
        public static final boolean ENABLED_DEF = false;
        public static final String TEXTURE_DEF = "noise1";
        public static final String COLOR_HEX_DEF = "#ffffff";
        public static final Slider HEIGHT = new Slider(0.03, 0.001, 0.2);
        public static final Slider DENSITY = new Slider(0.5, 0.0, 1.0);
        public static final Slider NOISE_SCALE = new Slider(1.0, 0.1, 2.5);
        public static final Slider WIND_SPEED = new Slider(0.5, -1.0, 1.0);
        public static final float ALPHA_DEF = 1.0F;
        public static final float WIND_SPEED_INSTANCE_DEF = 0.0025F;
        public static final float SURFACE_WIND_SPEED_DEF = 0.02F;
        public static final Vector3f COLOR_RGB_DEF = new Vector3f(1.0F, 1.0F, 1.0F);
    }

    public static final class Atmosphere {
        private Atmosphere() {}
        public static final boolean ENABLED_DEF = false;
        public static final String COLOR_HEX_DEF = "#ffffff";
        public static final String JSON_COLOR_HEX_DEF = "#7fb8ff";
        public static final Slider THICKNESS = new Slider(0.23, 0.04, 0.35);
        public static final float THICKNESS_LOADER_FALLBACK = 0.07F;
        public static final Slider EXPOSURE = new Slider(3.25, 1.0, 3.25);
        public static final Slider INTENSITY = new Slider(0.34, 0.15, 0.7);
        public static final Slider RAYLEIGH_SCALE_HEIGHT = new Slider(0.0913, 0.0913, 0.20);
        public static final Slider RAYLEIGH_STRENGTH = new Slider(0.0856, 0.0035, 0.0942);
        public static final Slider WAVELENGTH_R = new Slider(1000.0, 380.0, 2000.0);
        public static final Slider WAVELENGTH_G = new Slider(1000.0, 380.0, 2000.0);
        public static final Slider WAVELENGTH_B = new Slider(1000.0, 380.0, 2000.0);
        public static final Vector3f WAVELENGTHS_DEF = new Vector3f(1000.0F, 1000.0F, 1000.0F);
        public static final float BUILDER_PLANET_RADIUS_DEF = 1.0F;
        public static final float BUILDER_ATMOSPHERE_RADIUS_DEF = 1.3F;
        public static final float BUILDER_RAYLEIGH_SCALE_HEIGHT_DEF = 0.1142F;
        public static final float BUILDER_RAYLEIGH_STRENGTH_DEF = 2.56F;
    }

    public static final class Ring {
        private Ring() {}
        public static final boolean ENABLED_DEF = false;
        public static final String TEXTURE_DEF = "saturn_ring";
        public static final String ROCK_TEXTURE_DEF = "rock_texture";
        public static final String ROCK_TEXTURE_INSTANCE_DEF = "debug";
        public static final String COLOR_HEX_DEF = "#ffffff";
        public static final Slider INNER_RADIUS = new Slider(1.2, 1.01, 3.0);
        public static final Slider OUTER_RADIUS = new Slider(2.0, 1.05, 10.0);
        public static final float INNER_RADIUS_INSTANCE_DEF = 1.0F;
        public static final float OUTER_RADIUS_INSTANCE_DEF = 3.0F;
        public static final Slider YAW = new Slider(0.0, -180.0, 180.0);
        public static final Slider PITCH = new Slider(0.0, -180.0, 180.0);
        public static final Slider ROLL = new Slider(0.0, -180.0, 180.0);
        public static final boolean ROCKS_ENABLED_DEF = false;
        public static final Slider ROCK_COUNT = new Slider(4000.0, 0.0, 150000.0);
        public static final Slider ROCK_MIN_SIZE = new Slider(0.05, 0.05, 0.50);
        public static final Slider ROCK_MAX_SIZE = new Slider(0.05, 0.05, 0.50);
        public static final Slider ROCK_HEIGHT = new Slider(1.0, 0.0, 3.0);
        public static final float ROCK_HEIGHT_FALLBACK = 0.0F;
        public static final Slider ROCK_ORBIT_SPEED = new Slider(0.0, -0.10, 0.10);
        public static final float ROCK_ORBIT_SPEED_FALLBACK = 0.015F;
        public static final float ROCKS_BUILDER_INNER_RADIUS_DEF = 120.0F;
        public static final float ROCKS_BUILDER_OUTER_RADIUS_DEF = 200.0F;
        public static final int ROCKS_BUILDER_ROCK_COUNT_DEF = 1000;
        public static final float ROCKS_BUILDER_MIN_SIZE_DEF = 1.0F;
        public static final float ROCKS_BUILDER_MAX_SIZE_DEF = 3.0F;
        public static final float ROCKS_BUILDER_HEIGHT_DEF = 2.0F;
        public static final float ROCKS_BUILDER_ORBIT_SPEED_DEF = 0.01F;
        public static final float ROCKS_BUILDER_SEED_DEF = 42.0F;
    }

    public static final class Sky {
        private Sky() {}
        public static final boolean SKYBOX_ROTATION_DEF = false;
        public static final boolean SKYBOX_CONSTANT_DEF = false;
        public static final String SKYBOX_TEXTURE_DEF = "space_skybox";
        public static final String SKYBOX_TEXTURE_LOADER_DEF = "milky_way";
        public static final boolean STARS_ENABLED_DEF = false;
        public static final String STARS_COLOR_HEX_DEF = "#ffffff";
        public static final boolean GROUND_MODE_DEF = false;
        public static final Slider STARS_AMOUNT = new Slider(5000.0, 0.0, 20000.0);
        public static final Slider STARS_SEED = new Slider(0.0, 0.0, 10000.0);
    }

    public static final class Fog {
        private Fog() {}
        public static final boolean ENABLED_DEF = true;
        public static final boolean SURFACE_FOG_ENABLED_DEF = false;
        public static final String COLOR_HEX_DEF = "#000000";
        public static final String SHAPE_DEF = "CYLINDER";
        public static final boolean USE_RENDER_DISTANCE_DEF = false;
        public static final Slider START_PERCENT = new Slider(0.0, -50.0, 100.0);
        public static final Slider END_PERCENT = new Slider(50.0, 1.0, 300.0);
        public static final Slider START_DISTANCE = new Slider(0.0, -100.0, 300.0);
        public static final Slider END_DISTANCE = new Slider(48.0, 2.0, 500.0);
        public static final float END_DISTANCE_LOADER_FALLBACK = 192.0F;
    }
}
