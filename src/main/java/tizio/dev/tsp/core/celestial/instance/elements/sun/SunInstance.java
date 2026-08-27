package tizio.dev.tsp.core.celestial.instance.elements.sun;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tizio.dev.tsp.core.utils.volume.OrientedVolumeInstance;

public final class SunInstance extends OrientedVolumeInstance {

    private final float planetRadius;
    private final float sunRadius;
    private final float densityFalloff;
    private final float scatteringStrength;
    private final Vector3f waveLengths;

    private SunInstance(Builder builder) {
        super(builder);
        this.planetRadius = builder.planetRadius;
        this.sunRadius = builder.sunRadius;
        this.densityFalloff = builder.densityFalloff;
        this.scatteringStrength = builder.scatteringStrength;
        this.waveLengths = new Vector3f(builder.waveLengths);
    }

    public float planetRadius() {
        return this.planetRadius;
    }

    public float sunRadius() {
        return this.sunRadius;
    }

    public float densityFalloff() {
        return this.densityFalloff;
    }

    public float scatteringStrength() {
        return this.scatteringStrength;
    }

    public Vector3f waveLengths() {
        return new Vector3f(this.waveLengths);
    }

    @Override
    protected float getBaseRadius() {
        return this.sunRadius;
    }

    public static Builder at(Vec3 position) {
        return new Builder(position);
    }

    public static final class Builder extends OrientedVolumeInstance.Builder<SunInstance, Builder> {
        private float planetRadius = 1.0F;
        private float sunRadius = 1.95F;
        private float densityFalloff = 1.3F;
        private float scatteringStrength = 1.4F;
        private Vector3f waveLengths = new Vector3f(780.0F, 540.0F, 440.0F);

        private Builder(Vec3 position) {
            super(position);
            this.color = new Vector3f(1.0F, 0.90F, 0.72F);
            this.quadScale = 1.2F;
        }

        public Builder planetRadius(float planetRadius) {
            this.planetRadius = planetRadius;
            return this;
        }

        public Builder sunRadius(float sunRadius) {
            this.sunRadius = sunRadius;
            return this;
        }

        public Builder densityFalloff(float densityFalloff) {
            this.densityFalloff = densityFalloff;
            return this;
        }

        public Builder scatteringStrength(float scatteringStrength) {
            this.scatteringStrength = scatteringStrength;
            return this;
        }

        public Builder waveLengths(float redNm, float greenNm, float blueNm) {
            this.waveLengths = new Vector3f(redNm, greenNm, blueNm);
            return this;
        }

        @Override
        public SunInstance build() {
            return new SunInstance(this);
        }
    }

    public static final class Config {
        public String type = "star";
        public String id = "sun";
        public float radius = 2000.0F;
        public String colorHex = "#ffd48a";
        public float yaw = 0.0F;
        public float pitch = 0.0F;
        public float roll = 0.0F;
        public float sunRadiusFactor = 1.3F;
        public float scatteringStrength = 0.5F;
        public float densityFalloff = 5.0F;
        public boolean enabled = true;
        public float diskRotationSpeed = 0.20F;
        public float intensity = 1.0F;

        public Config() {}

        public Config(String id, float radius, String colorHex) {
            this.id = id;
            this.radius = radius;
            this.colorHex = colorHex;
        }

        public boolean isBlackHole() {
            return "blackhole".equalsIgnoreCase(this.type);
        }

        public Config copy() {
            Config copy = new Config();
            copy.type = this.type;
            copy.id = this.id;
            copy.radius = this.radius;
            copy.colorHex = this.colorHex;
            copy.yaw = this.yaw;
            copy.pitch = this.pitch;
            copy.roll = this.roll;
            copy.sunRadiusFactor = this.sunRadiusFactor;
            copy.scatteringStrength = this.scatteringStrength;
            copy.densityFalloff = this.densityFalloff;
            copy.enabled = this.enabled;
            copy.diskRotationSpeed = this.diskRotationSpeed;
            copy.intensity = this.intensity;
            return copy;
        }
    }
}