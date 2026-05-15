package tizio.dev.engine.elements.atmosphere;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tizio.dev.engine.volume.OrientedVolumeInstance;

public final class AtmosphereInstance extends OrientedVolumeInstance {

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



    public Vector3f waveLengths() {return new Vector3f(this.waveLengths);}

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
