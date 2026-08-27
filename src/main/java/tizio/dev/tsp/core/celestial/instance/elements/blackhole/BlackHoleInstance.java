package tizio.dev.tsp.core.celestial.instance.elements.blackhole;

import net.minecraft.world.phys.Vec3;
import tizio.dev.tsp.core.utils.volume.OrientedVolumeInstance;

public final class BlackHoleInstance extends OrientedVolumeInstance {

    private final float radius;
    private final float diskRotationSpeed;
    private final float intensity;

    private BlackHoleInstance(Builder builder) {
        super(builder);
        this.radius = builder.radius;
        this.diskRotationSpeed = builder.diskRotationSpeed;
        this.intensity = builder.intensity;
    }

    public float radius() {
        return this.radius;
    }

    public float diskRotationSpeed() {
        return this.diskRotationSpeed;
    }

    public float intensity() {
        return this.intensity;
    }

    @Override
    protected float getBaseRadius() {
        return this.radius;
    }

    public static Builder at(Vec3 position) {
        return new Builder(position);
    }

    public static final class Builder extends OrientedVolumeInstance.Builder<BlackHoleInstance, Builder> {
        private float radius = 1.0F;
        private float diskRotationSpeed = 0.20F;
        private float intensity = 1.0F;

        private Builder(Vec3 position) {
            super(position);
        }

        public Builder radius(float radius) {
            if (radius <= 0.0F) return this;
            this.radius = radius;
            return this;
        }

        public Builder diskRotationSpeed(float diskRotationSpeed) {
            this.diskRotationSpeed = diskRotationSpeed;
            return this;
        }

        public Builder intensity(float intensity) {
            this.intensity = intensity;
            return this;
        }

        @Override
        public BlackHoleInstance build() {
            return new BlackHoleInstance(this);
        }
    }

    public static final class Config {
        public String id = "blackhole";
        public String parentId = "sun";
        public float radius = 100.0F;
        public String colorHex = "#000000";
        public float yaw = 0.0F;
        public float pitch = 0.0F;
        public float roll = 0.0F;
        public float diskRotationSpeed = 0.20F;
        public float intensity = 1.0F;

        public Config() {}

        public Config copy() {
            Config copy = new Config();
            copy.id = this.id;
            copy.parentId = this.parentId;
            copy.radius = this.radius;
            copy.colorHex = this.colorHex;
            copy.yaw = this.yaw;
            copy.pitch = this.pitch;
            copy.roll = this.roll;
            copy.diskRotationSpeed = this.diskRotationSpeed;
            copy.intensity = this.intensity;
            return copy;
        }
    }
}