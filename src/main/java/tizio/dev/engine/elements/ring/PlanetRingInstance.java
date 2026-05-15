package tizio.dev.engine.elements.ring;

import net.minecraft.world.phys.Vec3;
import tizio.dev.engine.volume.OrientedVolumeInstance;

public final class PlanetRingInstance extends OrientedVolumeInstance {

    private final float planetRadius;
    private final float ringOuterRadius;
    private final float ringInnerRadius;
    private final String ringTexture;

    private PlanetRingInstance(Builder builder) {
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
    public String ringTexture(){ return this.ringTexture + ".png";}
    @Override
    protected float getBaseRadius() {
        return 1.0f;
    }


    public static Builder at(Vec3 position) {
        return new Builder(position);
    }

    public static final class Builder extends OrientedVolumeInstance.Builder<PlanetRingInstance, Builder> {
        private float planetRadius;
        private float ringOuterRadius;
        private float ringInnerRadius;
        private String ringTexture = "textures/planets/";

        private Builder(Vec3 position) {
            super(position);
        }

        public Builder planetRadius(float planetRadius) {
            this.planetRadius = planetRadius;
            return this;
        }

        public Builder ringOuterRadius(float planetOuterRadius) {
            this.ringOuterRadius = planetOuterRadius;
            return this;
        }

        public Builder ringInnerRadius(float ringInnerRadius) {
            this.ringInnerRadius = ringInnerRadius;
            return this;
        }

        public Builder ringTexture(String name) {
            this.ringTexture += name;
            return this;
        }

        @Override
        public PlanetRingInstance build() {
            return new PlanetRingInstance(this);
        }
    }
}
