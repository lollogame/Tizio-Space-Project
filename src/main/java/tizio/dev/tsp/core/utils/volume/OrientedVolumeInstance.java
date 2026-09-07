package tizio.dev.tsp.core.utils.volume;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class OrientedVolumeInstance {

    protected static final String TEXTURE_BASE_PATH = "textures/planets/";

    protected Vec3 position;
    protected final float quadScale;
    protected final Float explicitQuadRadius;
    protected final Vector3f color;
    protected Vector3f lightDirection;
    protected Quaternionf orientation;

    protected OrientedVolumeInstance(Builder<?, ?> builder) {
        this.position = builder.position;
        this.quadScale = builder.quadScale;
        this.explicitQuadRadius = builder.explicitQuadRadius;
        this.color = new Vector3f(builder.color);
        this.lightDirection = new Vector3f(builder.lightDirection);
        this.orientation = new Quaternionf(builder.orientation);
    }

    public Vec3 position() {
        return this.position;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }

    public float quadScale() {
        return this.quadScale;
    }

    public Vector3f color() {
        return new Vector3f(this.color);
    }

    public Vector3f lightDirection() {
        return new Vector3f(this.lightDirection);
    }

    public void setLightDirection(Vector3f lightDirection) {
        this.lightDirection = new Vector3f(lightDirection);
    }

    public Quaternionf orientation() {
        return new Quaternionf(this.orientation);
    }

    public void setOrientation(Quaternionf orientation) {
        this.orientation = new Quaternionf(orientation);
    }

    public void setEulerDegrees(float yaw, float pitch, float roll) {
        this.orientation = new Quaternionf()
                .rotateXYZ((float) Math.toRadians(pitch), (float) Math.toRadians(yaw), (float) Math.toRadians(roll));
    }

    public void updateDynamicState(Vec3 position, Vector3f lightDirection, float yaw, float pitch, float roll) {
        setPosition(position);
        setLightDirection(lightDirection);
        setEulerDegrees(yaw, pitch, roll);
    }

    public void updateDynamicState(Vec3 position, float yaw, float pitch, float roll) {
        setPosition(position);
        setEulerDegrees(yaw, pitch, roll);
    }

    public float quadRadius() {
        return this.explicitQuadRadius != null ? this.explicitQuadRadius : getBaseRadius() * this.quadScale;
    }

    protected abstract float getBaseRadius();

    protected static String resolveTexturePath(String name) {
        return TEXTURE_BASE_PATH + name;
    }

    protected static String finalizeTexture(String texturePath) {
        if (texturePath == null) {
            return "";
        }
        String normalized = texturePath.trim();
        if (normalized.isBlank() || TEXTURE_BASE_PATH.equals(normalized)) {
            return "";
        }
        return normalized.endsWith(".png") ? normalized : normalized + ".png";
    }

    public abstract static class Builder<T extends OrientedVolumeInstance, B extends Builder<T, B>> {
        protected Vec3 position;
        protected float quadScale = 1.05F;
        protected Float explicitQuadRadius;
        protected Vector3f color = new Vector3f(1.0F, 1.0F, 1.0F);
        protected Vector3f lightDirection = new Vector3f(0.25F, 0.35F, 1.0F).normalize();
        protected Quaternionf orientation = new Quaternionf();

        protected Builder(Vec3 position) {
            this.position = position;
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        public B position(Vec3 position) {
            this.position = position;
            return self();
        }

        public B quadScale(float quadScale) {
            this.quadScale = quadScale;
            return self();
        }

        public B quadRadius(float quadRadius) {
            this.explicitQuadRadius = quadRadius;
            return self();
        }

        public B color(float red, float green, float blue) {
            this.color = new Vector3f(red, green, blue);
            return self();
        }

        public B color(Vector3f color) {
            this.color = new Vector3f(color);
            return self();
        }

        public B color255(int red, int green, int blue) {
            this.color = new Vector3f(red / 255.0F, green / 255.0F, blue / 255.0F);
            return self();
        }

        public B lightDirection(float x, float y, float z) {
            this.lightDirection = new Vector3f(x, y, z).normalize();
            return self();
        }

        public B lightDirection(Vector3f lightDirection) {
            this.lightDirection = new Vector3f(lightDirection).normalize();
            return self();
        }

        public B orientation(Quaternionf orientation) {
            this.orientation = new Quaternionf(orientation);
            return self();
        }

        public B eulerDegrees(float yaw, float pitch, float roll) {
            this.orientation = new Quaternionf().rotateXYZ((float) Math.toRadians(pitch), (float) Math.toRadians(yaw), (float) Math.toRadians(roll));
            return self();
        }

        public abstract T build();
    }
}
