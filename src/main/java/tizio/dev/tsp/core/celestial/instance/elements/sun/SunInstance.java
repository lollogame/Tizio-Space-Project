package tizio.dev.tsp.core.celestial.instance.elements.sun;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tizio.dev.tsp.config.DataConfig.Star;
import tizio.dev.tsp.core.utils.volume.OrientedVolumeInstance;
import tizio.dev.tsp.core.utils.Utils;

public final class SunInstance extends OrientedVolumeInstance {

    private final float planetRadius;
    private final float sunRadius;

    private SunInstance(Builder builder) {
        super(builder);
        this.planetRadius = builder.planetRadius;
        this.sunRadius = builder.sunRadius;
    }

    public float planetRadius() {
        return this.planetRadius;
    }

    public float sunRadius() {
        return this.sunRadius;
    }

    @Override
    protected float getBaseRadius() {
        return this.sunRadius;
    }

    public static Builder at(Vec3 position) {
        return new Builder(position);
    }

    public static final class Builder extends OrientedVolumeInstance.Builder<SunInstance, Builder> {

        private float planetRadius = Star.BUILDER_PLANET_RADIUS_DEF;
        private float sunRadius = Star.BUILDER_SUN_RADIUS_DEF;

        private Builder(Vec3 position) {
            super(position);
            this.color = new Vector3f(Star.BUILDER_COLOR_DEF);
            this.quadScale = Star.BUILDER_QUAD_SCALE_DEF;
        }

        public Builder planetRadius(float planetRadius) {
            this.planetRadius = planetRadius;
            return this;
        }

        public Builder sunRadius(float sunRadius) {
            this.sunRadius = sunRadius;
            return this;
        }

        @Override
        public SunInstance build() {
            return new SunInstance(this);
        }
    }

    public static final class Config {
        public String type = Star.TYPE_DEF;
        public String id = Star.ID_DEF;
        public float radius = Star.RADIUS.defF();
        public String colorHex = Star.COLOR_HEX_DEF;
        public float yaw = Star.YAW.defF();
        public float pitch = Star.PITCH.defF();
        public float roll = Star.ROLL.defF();
        public float sunRadiusFactor = Star.SUN_RADIUS_FACTOR_DEF;
        public float scatteringStrength = Star.SCATTERING_STRENGTH_DEF;
        public float densityFalloff = Star.DENSITY_FALLOFF_DEF;
        public boolean enabled = Star.ENABLED_DEF;
        public float diskRotationSpeed = Star.DISK_ROTATION_SPEED.defF();
        public float intensity = Star.INTENSITY.defF();

        public Config() {}

        public Config(String id, float radius, String colorHex) {
            this.id = id;
            this.radius = radius;
            this.colorHex = colorHex;
        }

        public boolean isBlackHole() {
            return Utils.isBlackHole(this.type);
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
