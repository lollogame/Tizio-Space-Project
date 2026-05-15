package tizio.dev.engine.elements.planet;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tizio.dev.engine.volume.OrientedVolumeInstance;

public final class PlanetSurfaceInstance extends OrientedVolumeInstance {

    private final float planetRadius;
    private final String dayTexture;
    private final String nightTexture;
    private final float emissiveStrength;

    private PlanetSurfaceInstance(Builder builder) {
        super(builder);
        this.planetRadius = builder.planetRadius;
        this.dayTexture = builder.dayTexture;
        this.nightTexture = builder.nightTexture;
        this.emissiveStrength = builder.emissiveStrength;
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

    @Override
    protected float getBaseRadius() {
        return 1.0f; // Original code returns just quadScale
    }

    public static Builder at(Vec3 position) {
        return new Builder(position);
    }

    public static final class Builder extends OrientedVolumeInstance.Builder<PlanetSurfaceInstance, Builder> {
        private float planetRadius;
        private String dayTexture = "textures/planets/";
        private String nightTexture = "textures/planets/";
        private float emissiveStrength;

        private Builder(Vec3 position) {
            super(position);
            this.color = new Vector3f(0.55F, 0.78F, 1.0F); // Default override
        }

        public Builder planetRadius(float planetRadius) {
            this.planetRadius = planetRadius;
            return this;
        }

        public Builder dayTexture(String name) {
            this.dayTexture += name;
            return this;
        }

        public Builder nightTexture(String name) {
            this.nightTexture += name;
            return this;
        }

        public Builder emissiveStrength(float emissiveStrength) {
            this.emissiveStrength = emissiveStrength;
            return this;
        }

        @Override
        public PlanetSurfaceInstance build() {
            return new PlanetSurfaceInstance(this);
        }
    }

    private static String finalizeTexture(String texturePath) {
        if (texturePath == null) {
            return "";
        }

        String normalized = texturePath.trim();
        if (normalized.isBlank() || "textures/planets/".equals(normalized)) {
            return "";
        }

        return normalized.endsWith(".png") ? normalized : normalized + ".png";
    }
}
