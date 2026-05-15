package tizio.dev.engine.elements.sun;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tizio.dev.engine.volume.OrientedVolumeInstance;

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


    public float planetRadius() { return this.planetRadius; }
    public float sunRadius() { return this.sunRadius; }
    public float densityFalloff() { return this.densityFalloff; }
    public float scatteringStrength() { return this.scatteringStrength; }

    public Vector3f waveLengths() { return new Vector3f(this.waveLengths); }
    @Override
    protected float getBaseRadius() {
        return this.sunRadius;
    }

    public static Builder at(Vec3 position) {
        return new Builder(position);
    }

    public static final class Builder extends OrientedVolumeInstance.Builder<SunInstance, Builder> {
        private float planetRadius = 1.0F;
        private float sunRadius = 3.95F;
        private float densityFalloff = 7.3F;
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
}
