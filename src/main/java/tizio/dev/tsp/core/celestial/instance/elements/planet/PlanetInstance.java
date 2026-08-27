package tizio.dev.tsp.core.celestial.instance.elements.planet;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tizio.dev.tsp.core.utils.volume.OrientedVolumeInstance;

public final class PlanetInstance {

    private final Config config;
    private final SurfaceInstance surface;
    private final AtmosphereInstance atmosphere;
    private final RingInstance ring;

    public PlanetInstance(Config config, SurfaceInstance surface, AtmosphereInstance atmosphere, RingInstance ring) {
        this.config = config;
        this.surface = surface;
        this.atmosphere = atmosphere;
        this.ring = ring;
    }

    public Config config() {
        return this.config;
    }

    public SurfaceInstance surface() {
        return this.surface;
    }

    public AtmosphereInstance atmosphere() {
        return this.atmosphere;
    }

    public RingInstance ring() {
        return this.ring;
    }

    public static final class Config {
        public String id = "unnamed_body";
        public String type = "planet";
        public String parentId = "sun";
        public String dimension = "";
        public float radius = 100.0F;
        public String texture = "";
        public String nightTexture = "";
        public String colorHex = "#7fb8ff";
        public float yaw = 0.0F;
        public float pitch = 0.0F;
        public float roll = 0.0F;
        public boolean surfaceEnabled = true;
        public float diskRotationSpeed = 0.20F;
        public float intensity = 1.0F;
        public double spinHours = 24.0;
        public float gravity = 9.81F;
        public boolean oxygen = false;
        public float temperature = 0.0F;

        public Orbit orbit = new Orbit();
        public Atmosphere atmosphere = new Atmosphere();
        public Ring ring = new Ring();
        public Clouds clouds = new Clouds();
        public Sky sky = new Sky();
        public SurfaceInstance.SurfaceFog fog = new SurfaceInstance.SurfaceFog();

        public Config() {}

        public Config(String id, String type, String parentId, float radius, String texture, String colorHex) {
            this.id = id;
            this.type = type;
            this.parentId = parentId;
            this.radius = radius;
            this.texture = texture;
            this.colorHex = colorHex;
        }

        public boolean isBlackHole() {
            return "blackhole".equalsIgnoreCase(this.type);
        }

        public Config copy() {
            Config copy = new Config();
            copy.id = this.id;
            copy.type = this.type;
            copy.parentId = this.parentId;
            copy.dimension = this.dimension;
            copy.radius = this.radius;
            copy.texture = this.texture;
            copy.nightTexture = this.nightTexture;
            copy.colorHex = this.colorHex;
            copy.yaw = this.yaw;
            copy.pitch = this.pitch;
            copy.roll = this.roll;
            copy.surfaceEnabled = this.surfaceEnabled;
            copy.diskRotationSpeed = this.diskRotationSpeed;
            copy.intensity = this.intensity;
            copy.spinHours = this.spinHours;
            copy.gravity = this.gravity;
            copy.oxygen = this.oxygen;
            copy.temperature = this.temperature;
            copy.orbit = this.orbit != null ? this.orbit.copy() : new Orbit();
            copy.atmosphere = this.atmosphere != null ? this.atmosphere.copy() : new Atmosphere();
            copy.ring = this.ring != null ? this.ring.copy() : new Ring();
            copy.clouds = this.clouds != null ? this.clouds.copy() : new Clouds();
            copy.sky = this.sky != null ? this.sky.copy() : new Sky();
            copy.fog = this.fog != null ? this.fog.copy() : new SurfaceInstance.SurfaceFog();
            return copy;
        }
    }

    public static final class Sky {
        public boolean skyboxRotation = false;
        public boolean skyboxConstant = false;
        public String skyboxTexture = "milky_way";
        public boolean starsEnabled = false;
        public int starsAmount = 5000;
        public int starsSeed = 0;
        public String starsColorHex = "#ffffff";
        public boolean groundMode = false;

        public Sky() {}

        public Sky copy() {
            Sky copy = new Sky();
            copy.skyboxRotation = this.skyboxRotation;
            copy.skyboxConstant = this.skyboxConstant;
            copy.skyboxTexture = this.skyboxTexture;
            copy.starsEnabled = this.starsEnabled;
            copy.starsAmount = this.starsAmount;
            copy.starsSeed = this.starsSeed;
            copy.starsColorHex = this.starsColorHex;
            copy.groundMode = this.groundMode;
            return copy;
        }
    }

    public static final class Clouds {
        public boolean enabled = false;
        public String texture = "noise1";
        public float height = 0.03F;
        public float density = 0.5F;
        public float windSpeed = 0.02F;
        public String colorHex = "#ffffff";
        public float alpha = 1.0F;
        public float noiseScale = 1.0F;

        public Clouds() {}

        public Clouds(boolean enabled, String texture, float height, float density, float windSpeed, String colorHex, float alpha, float noiseScale) {
            this.enabled = enabled;
            this.texture = texture;
            this.height = height;
            this.density = density;
            this.windSpeed = windSpeed;
            this.colorHex = colorHex;
            this.alpha = alpha;
            this.noiseScale = noiseScale;
        }

        public Clouds copy() {
            Clouds copy = new Clouds();
            copy.enabled = this.enabled;
            copy.texture = this.texture;
            copy.height = this.height;
            copy.density = this.density;
            copy.windSpeed = this.windSpeed;
            copy.colorHex = this.colorHex;
            copy.alpha = this.alpha;
            copy.noiseScale = this.noiseScale;
            return copy;
        }
    }

    public static final class Orbit {
        public boolean enabled = true;
        public double radius = 0.0;
        public double periodDays = 0.0;
        public double epochAngle = 0.0;
        public String epochUtc = "2000-01-01T12:00:00Z";
        public double inclination = 0.0;
        public double ascendingNode = 0.0;
        public double verticalOffset = 0.0;

        public Orbit() {}

        public Orbit(double radius, double periodDays, double epochAngle, String epochUtc, double inclination, double ascendingNode) {
            this.radius = radius;
            this.periodDays = periodDays;
            this.epochAngle = epochAngle;
            this.epochUtc = epochUtc != null ? epochUtc : "2000-01-01T12:00:00Z";
            this.inclination = inclination;
            this.ascendingNode = ascendingNode;
        }

        public Orbit copy() {
            Orbit copy = new Orbit();
            copy.enabled = this.enabled;
            copy.radius = this.radius;
            copy.periodDays = this.periodDays;
            copy.epochAngle = this.epochAngle;
            copy.epochUtc = this.epochUtc;
            copy.inclination = this.inclination;
            copy.ascendingNode = this.ascendingNode;
            copy.verticalOffset = this.verticalOffset;
            return copy;
        }
    }

    public static final class Atmosphere {
        public boolean enabled = false;
        public float thickness = 0.23F;
        public float exposure = 3.25F;
        public float intensity = 0.34F;
        public float rayleighScaleHeight = 0.0913F;
        public float rayleighStrength = 0.0856F;
        public float wavelengthR = 1000.0F;
        public float wavelengthG = 1000.0F;
        public float wavelengthB = 1000.0F;
        public String colorHex = "#ffffff";

        public Atmosphere() {}

        public Atmosphere copy() {
            Atmosphere copy = new Atmosphere();
            copy.enabled = this.enabled;
            copy.thickness = this.thickness;
            copy.exposure = this.exposure;
            copy.intensity = this.intensity;
            copy.rayleighScaleHeight = this.rayleighScaleHeight;
            copy.rayleighStrength = this.rayleighStrength;
            copy.wavelengthR = this.wavelengthR;
            copy.wavelengthG = this.wavelengthG;
            copy.wavelengthB = this.wavelengthB;
            copy.colorHex = this.colorHex;
            return copy;
        }
    }

    public static final class Ring {
        public boolean enabled = false;
        public float innerRadius = 180.0F;
        public float outerRadius = 300.0F;
        public String texture = "saturn_ring";
        public String rockTexture = "debug";
        public String colorHex = "#ffffff";
        public float yaw = 0.0F;
        public float pitch = 0.0F;
        public float roll = 0.0F;
        public boolean rocksEnabled = false;
        public int rockCount = 4000;
        public float rockMinSize = -1.0F;
        public float rockMaxSize = -1.0F;
        public float rockHeight = -1.0F;
        public float rockOrbitSpeed = 0.015F;

        public Ring() {}

        public Ring copy() {
            Ring copy = new Ring();
            copy.enabled = this.enabled;
            copy.innerRadius = this.innerRadius;
            copy.outerRadius = this.outerRadius;
            copy.texture = this.texture;
            copy.rockTexture = this.rockTexture;
            copy.colorHex = this.colorHex;
            copy.yaw = this.yaw;
            copy.pitch = this.pitch;
            copy.roll = this.roll;
            copy.rocksEnabled = this.rocksEnabled;
            copy.rockCount = this.rockCount;
            copy.rockMinSize = this.rockMinSize;
            copy.rockMaxSize = this.rockMaxSize;
            copy.rockHeight = this.rockHeight;
            copy.rockOrbitSpeed = this.rockOrbitSpeed;
            return copy;
        }
    }

    public static final class SurfaceInstance extends OrientedVolumeInstance {

        private final float planetRadius;
        private final String dayTexture;
        private final String nightTexture;
        private final float emissiveStrength;
        private final boolean cloudsEnabled;
        private final String cloudTexture;
        private final float cloudHeight;
        private final float cloudCoverage;
        private final float cloudWindSpeed;
        private final Vector3f cloudColor;
        private final float cloudAlpha;
        private final float cloudNoiseScale;
        private double spinHours;

        private SurfaceInstance(Builder builder) {
            super(builder);
            this.planetRadius = builder.planetRadius;
            this.dayTexture = builder.dayTexture;
            this.nightTexture = builder.nightTexture;
            this.emissiveStrength = builder.emissiveStrength;
            this.cloudsEnabled = builder.cloudsEnabled;
            this.cloudTexture = builder.cloudTexture;
            this.cloudHeight = builder.cloudHeight;
            this.cloudCoverage = builder.cloudCoverage;
            this.cloudWindSpeed = builder.cloudWindSpeed;
            this.cloudColor = new Vector3f(builder.cloudColor);
            this.cloudAlpha = builder.cloudAlpha;
            this.cloudNoiseScale = builder.cloudNoiseScale;
            this.spinHours = builder.spinHours;
        }

        public float planetRadius() {
            return this.planetRadius;
        }

        public String dayTexture() {
            return finalizeTexture(this.dayTexture);
        }

        public String nightTexture() {
            return finalizeTexture(this.nightTexture);
        }

        public boolean hasNightTexture() {
            return !nightTexture().isBlank();
        }

        public float emissiveStrength() {
            return this.emissiveStrength;
        }

        public boolean hasClouds() {
            return this.cloudsEnabled;
        }

        public boolean cloudsEnabled() {
            return this.cloudsEnabled;
        }

        public String cloudTexture() {
            return finalizeTexture(this.cloudTexture);
        }

        public float cloudHeight() {
            return this.cloudHeight;
        }

        public float cloudCoverage() {
            return this.cloudCoverage;
        }

        public float cloudDensity() {
            return this.cloudCoverage;
        }

        public float cloudWindSpeed() {
            return this.cloudWindSpeed;
        }

        public Vector3f cloudColor() {
            return new Vector3f(this.cloudColor);
        }

        public float cloudAlpha() {
            return this.cloudAlpha;
        }

        public float cloudNoiseScale() {
            return this.cloudNoiseScale;
        }

        public double spinHours() {
            return this.spinHours;
        }

        public void setSpinHours(double spinHours) {
            this.spinHours = spinHours;
        }

        public double spinPeriodDays() {
            return this.spinHours / 24.0;
        }

        public void setSpinPeriodDays(double spinPeriodDays) {
            this.spinHours = spinPeriodDays * 24.0;
        }

        /** Returns surface rotation angle in radians for the given time in seconds. */
        public float surfaceRotationAngle(float timeSeconds) {
            if (this.spinHours == 0.0) return 0.0F;
            // 1 in-game day (24.0 hours) = 1 Minecraft day cycle (1200 seconds / 24000 ticks = 50s per hour)
            double periodSeconds = this.spinHours * 50.0;
            return (float) (((timeSeconds / periodSeconds) * 2.0 * Math.PI) % (2.0 * Math.PI));
        }

        @Override
        protected float getBaseRadius() {
            return 1.0F;
        }

        public static Builder at(Vec3 position) {
            return new Builder(position);
        }

        public static final class Builder extends OrientedVolumeInstance.Builder<SurfaceInstance, Builder> {
            private float planetRadius;
            private String dayTexture = TEXTURE_BASE_PATH;
            private String nightTexture = TEXTURE_BASE_PATH;
            private float emissiveStrength;
            private boolean cloudsEnabled = false;
            private String cloudTexture = TEXTURE_BASE_PATH + "noise1";
            private float cloudHeight = 0.03F;
            private float cloudCoverage = 0.5F;
            private float cloudWindSpeed = 0.02F;
            private Vector3f cloudColor = new Vector3f(1.0F, 1.0F, 1.0F);
            private float cloudAlpha = 1.0F;
            private float cloudNoiseScale = 1.0F;
            private double spinHours = 24.0;

            private Builder(Vec3 position) {
                super(position);
                this.color = new Vector3f(0.55F, 0.78F, 1.0F);
            }

            public Builder planetRadius(float planetRadius) {
                this.planetRadius = planetRadius;
                return this;
            }

            public Builder dayTexture(String name) {
                this.dayTexture = resolveTexturePath(name);
                return this;
            }

            public Builder nightTexture(String name) {
                this.nightTexture = resolveTexturePath(name);
                return this;
            }

            public Builder emissiveStrength(float emissiveStrength) {
                this.emissiveStrength = emissiveStrength;
                return this;
            }

            public Builder cloudsEnabled(boolean cloudsEnabled) {
                this.cloudsEnabled = cloudsEnabled;
                return this;
            }

            public Builder cloudTexture(String name) {
                this.cloudTexture = resolveTexturePath(name);
                return this;
            }

            public Builder cloudHeight(float cloudHeight) {
                this.cloudHeight = cloudHeight;
                return this;
            }

            public Builder cloudCoverage(float cloudCoverage) {
                this.cloudCoverage = cloudCoverage;
                return this;
            }

            public Builder cloudDensity(float cloudDensity) {
                this.cloudCoverage = cloudDensity;
                return this;
            }

            public Builder cloudWindSpeed(float cloudWindSpeed) {
                this.cloudWindSpeed = cloudWindSpeed;
                return this;
            }

            public Builder cloudColor(Vector3f cloudColor) {
                this.cloudColor = new Vector3f(cloudColor);
                return this;
            }

            public Builder cloudColor(float r, float g, float b) {
                this.cloudColor = new Vector3f(r, g, b);
                return this;
            }

            public Builder cloudAlpha(float cloudAlpha) {
                this.cloudAlpha = cloudAlpha;
                return this;
            }

            public Builder cloudNoiseScale(float cloudNoiseScale) {
                this.cloudNoiseScale = cloudNoiseScale;
                return this;
            }

            public Builder spinHours(double spinHours) {
                this.spinHours = spinHours;
                return this;
            }

            public Builder spinPeriodDays(double spinPeriodDays) {
                this.spinHours = spinPeriodDays * 24.0;
                return this;
            }

            @Override
            public SurfaceInstance build() {
                return new SurfaceInstance(this);
            }
        }

        public static final class SurfaceFog {
            public boolean enabled = false;
            public String colorHex = "#000000";
            public String shape = "CYLINDER";
            public float startDistance = 0.0F;
            public float endDistance = 192.0F;
            public boolean useRenderDistance = false;

            public SurfaceFog() {}

            public SurfaceFog(boolean enabled, String colorHex, String shape, float startDistance, float endDistance, boolean useRenderDistance) {
                this.enabled = enabled;
                this.colorHex = colorHex != null ? colorHex : "#000000";
                this.shape = shape != null ? shape : "CYLINDER";
                this.startDistance = startDistance;
                this.endDistance = endDistance;
                this.useRenderDistance = useRenderDistance;
            }

            public SurfaceFog copy() {
                SurfaceFog copy = new SurfaceFog();
                copy.enabled = this.enabled;
                copy.colorHex = this.colorHex;
                copy.shape = this.shape;
                copy.startDistance = this.startDistance;
                copy.endDistance = this.endDistance;
                copy.useRenderDistance = this.useRenderDistance;
                return copy;
            }
        }
    }

    public static final class AtmosphereInstance extends OrientedVolumeInstance {

        private final float planetRadius;
        private final float atmosphereRadius;
        private final float intensity;
        private final float exposure;
        private final float rayleighScaleHeight;
        private final float rayleighStrength;
        private final Vector3f waveLengths;

        private AtmosphereInstance(Builder builder) {
            super(builder);
            this.planetRadius = builder.planetRadius;
            this.atmosphereRadius = builder.atmosphereRadius;
            this.intensity = builder.intensity;
            this.exposure = builder.exposure;
            this.rayleighScaleHeight = builder.rayleighScaleHeight;
            this.rayleighStrength = builder.rayleighStrength;
            this.waveLengths = new Vector3f(builder.waveLengths);
        }

        public float planetRadius() {
            return this.planetRadius;
        }

        public float atmosphereRadius() {
            return this.atmosphereRadius;
        }

        public float intensity() {
            return this.intensity;
        }

        public float exposure() {
            return this.exposure;
        }

        public float rayleighScaleHeight() {
            return this.rayleighScaleHeight;
        }

        public float rayleighStrength() {
            return this.rayleighStrength;
        }

        public Vector3f waveLengths() {
            return new Vector3f(this.waveLengths);
        }

        @Override
        protected float getBaseRadius() {
            return this.atmosphereRadius;
        }

        public static Builder at(Vec3 position) {
            return new Builder(position);
        }

        public static final class Builder extends OrientedVolumeInstance.Builder<AtmosphereInstance, Builder> {
            private float planetRadius = 1.0F;
            private float atmosphereRadius = 1.3F;
            private float intensity = 0.34F;
            private float exposure = 3.25F;
            private float rayleighScaleHeight = 0.1142F;
            private float rayleighStrength = 2.56F;
            private Vector3f waveLengths = new Vector3f(1000F, 1000F, 1000F);

            private Builder(Vec3 position) {
                super(position);
            }

            public Builder planetRadius(float planetRadius) {
                this.planetRadius = planetRadius;
                return this;
            }

            public Builder atmosphereRadius(float atmosphereRadius) {
                this.atmosphereRadius = atmosphereRadius;
                return this;
            }

            public Builder intensity(float intensity) {
                this.intensity = intensity;
                return this;
            }

            public Builder exposure(float exposure) {
                this.exposure = exposure;
                return this;
            }

            public Builder rayleighScaleHeight(float rayleighScaleHeight) {
                this.rayleighScaleHeight = rayleighScaleHeight;
                return this;
            }

            public Builder rayleighStrength(float rayleighStrength) {
                this.rayleighStrength = rayleighStrength;
                return this;
            }

            public Builder waveLengths(float redNm, float greenNm, float blueNm) {
                this.waveLengths = new Vector3f(redNm, greenNm, blueNm);
                return this;
            }

            public Builder waveLengths(Vector3f color) {
                this.waveLengths = new Vector3f(color);
                return this;
            }

            @Override
            public AtmosphereInstance build() {
                return new AtmosphereInstance(this);
            }
        }
    }

    public static final class RingInstance extends OrientedVolumeInstance {

        private final float planetRadius;
        private final float ringOuterRadius;
        private final float ringInnerRadius;
        private final String ringTexture;

        private RingInstance(Builder builder) {
            super(builder);
            this.planetRadius = builder.planetRadius;
            this.ringOuterRadius = builder.ringOuterRadius;
            this.ringInnerRadius = builder.ringInnerRadius;
            this.ringTexture = builder.ringTexture;
        }

        public float planetRadius() {
            return this.planetRadius;
        }

        public float ringOuterRadius() {
            return this.ringOuterRadius;
        }

        public float ringInnerRadius() {
            return this.ringInnerRadius;
        }

        public String ringTexture() {
            return finalizeTexture(this.ringTexture);
        }

        @Override
        protected float getBaseRadius() {
            return 1.0F;
        }

        public static Builder at(Vec3 position) {
            return new Builder(position);
        }

        public static final class Builder extends OrientedVolumeInstance.Builder<RingInstance, Builder> {
            private float planetRadius;
            private float ringOuterRadius;
            private float ringInnerRadius;
            private String ringTexture = TEXTURE_BASE_PATH;

            private Builder(Vec3 position) {
                super(position);
            }

            public Builder planetRadius(float planetRadius) {
                this.planetRadius = planetRadius;
                return this;
            }

            public Builder ringOuterRadius(float ringOuterRadius) {
                this.ringOuterRadius = ringOuterRadius;
                return this;
            }

            public Builder ringInnerRadius(float ringInnerRadius) {
                this.ringInnerRadius = ringInnerRadius;
                return this;
            }

            public Builder ringTexture(String name) {
                this.ringTexture = resolveTexturePath(name);
                return this;
            }

            @Override
            public RingInstance build() {
                return new RingInstance(this);
            }
        }

        public static final class RocksInstance extends OrientedVolumeInstance {

            private final float planetRadius;
            private final float ringInnerRadius;
            private final float ringOuterRadius;
            private final int rockCount;
            private final float rockMinSize;
            private final float rockMaxSize;
            private final float rockHeight;
            private final float orbitSpeed;
            private final float seed;
            private final String rockTexture;

            private RocksInstance(Builder builder) {
                super(builder);
                this.planetRadius = builder.planetRadius;
                this.ringInnerRadius = builder.ringInnerRadius;
                this.ringOuterRadius = builder.ringOuterRadius;
                this.rockCount = builder.rockCount;
                this.rockMinSize = builder.rockMinSize;
                this.rockMaxSize = builder.rockMaxSize;
                this.rockHeight = builder.rockHeight;
                this.orbitSpeed = builder.orbitSpeed;
                this.seed = builder.seed;
                this.rockTexture = builder.rockTexture;
            }

            public float planetRadius() {
                return this.planetRadius;
            }

            public float ringInnerRadius() {
                return this.ringInnerRadius;
            }

            public float ringOuterRadius() {
                return this.ringOuterRadius;
            }

            public int rockCount() {
                return this.rockCount;
            }

            public float rockMinSize() {
                return this.rockMinSize;
            }

            public float rockMaxSize() {
                return this.rockMaxSize;
            }

            public float rockHeight() {
                return this.rockHeight;
            }

            public float orbitSpeed() {
                return this.orbitSpeed;
            }

            public float seed() {
                return this.seed;
            }

            public String rockTexture() {
                return finalizeTexture(this.rockTexture);
            }

            @Override
            protected float getBaseRadius() {
                return this.ringOuterRadius;
            }

            public static Builder at(Vec3 position) {
                return new Builder(position);
            }

            public static final class Builder extends OrientedVolumeInstance.Builder<RocksInstance, Builder> {
                private float planetRadius;
                private float ringInnerRadius = 120.0F;
                private float ringOuterRadius = 200.0F;
                private int rockCount = 1000;
                private float rockMinSize = 1.0F;
                private float rockMaxSize = 3.0F;
                private float rockHeight = 2.0F;
                private float orbitSpeed = 0.01F;
                private float seed = 42.0F;
                private String rockTexture = TEXTURE_BASE_PATH;

                public Builder(Vec3 position) {
                    super(position);
                }

                public Builder planetRadius(float planetRadius) {
                    this.planetRadius = planetRadius;
                    return this;
                }

                public Builder ringInnerRadius(float ringInnerRadius) {
                    this.ringInnerRadius = ringInnerRadius;
                    return this;
                }

                public Builder ringOuterRadius(float ringOuterRadius) {
                    this.ringOuterRadius = ringOuterRadius;
                    return this;
                }

                public Builder rockCount(int rockCount) {
                    this.rockCount = rockCount;
                    return this;
                }

                public Builder rockSize(float fixedSize) {
                    this.rockMinSize = fixedSize;
                    this.rockMaxSize = fixedSize;
                    return this;
                }

                public Builder rockSize(float minSize, float maxSize) {
                    this.rockMinSize = minSize;
                    this.rockMaxSize = maxSize;
                    return this;
                }

                public Builder rockMinSize(float rockMinSize) {
                    this.rockMinSize = rockMinSize;
                    return this;
                }

                public Builder rockMaxSize(float rockMaxSize) {
                    this.rockMaxSize = rockMaxSize;
                    return this;
                }

                public Builder rockHeight(float rockHeight) {
                    this.rockHeight = rockHeight;
                    return this;
                }

                public Builder orbitSpeed(float orbitSpeed) {
                    this.orbitSpeed = orbitSpeed;
                    return this;
                }

                public Builder seed(float seed) {
                    this.seed = seed;
                    return this;
                }

                public Builder rockTexture(String name) {
                    this.rockTexture = resolveTexturePath(name);
                    return this;
                }

                @Override
                public RocksInstance build() {
                    return new RocksInstance(this);
                }
            }
        }
    }
}