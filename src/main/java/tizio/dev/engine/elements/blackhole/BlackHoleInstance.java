package tizio.dev.engine.elements.blackhole;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tizio.dev.engine.volume.OrientedVolumeInstance;

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
            this.color = new Vector3f(1.0F, 0.72F, 0.22F); // Default override
            this.quadScale = 3.0F; // Default override
        }

        public Builder radius(float radius) {
            if(radius > 1) return this;
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
}
