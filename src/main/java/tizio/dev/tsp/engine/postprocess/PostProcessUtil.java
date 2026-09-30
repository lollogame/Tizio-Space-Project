package tizio.dev.tsp.engine.postprocess;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tizio.dev.tsp.MainClass;

import java.lang.reflect.Field;
import java.util.List;

public final class PostProcessUtil {

    private static Field PASSES_FIELD;
    private static Field EFFECT_FIELD;

    static {
        try {
            for (Field field : PostChain.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    PASSES_FIELD = field;
                    break;
                }
            }
            for (Field field : PostPass.class.getDeclaredFields()) {
                if (EffectInstance.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    EFFECT_FIELD = field;
                    break;
                }
            }
        } catch (Exception e) {
            MainClass.LOGGER.error("Failed to initialize post-process reflection access", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<PostPass> getPasses(PostChain chain) {
        if (PASSES_FIELD == null) return List.of();
        try {
            return (List<PostPass>) PASSES_FIELD.get(chain);
        } catch (Exception e) {
            return List.of();
        }
    }

    public static EffectInstance getEffect(PostPass pass) {
        if (EFFECT_FIELD == null) return null;
        try {
            return (EffectInstance) EFFECT_FIELD.get(pass);
        } catch (Exception e) {
            return null;
        }
    }

    public static int getWidth() {
        return Minecraft.getInstance().getWindow().getWidth();
    }

    public static int getHeight() {
        return Minecraft.getInstance().getWindow().getHeight();
    }

    public static ResourceLocation addShader(String shader) {
        return ResourceLocation.fromNamespaceAndPath(MainClass.MODID, "shaders/post/" + shader + ".json");
    }

    public static void setFloat(EffectInstance effect, String name, float value) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    public static void setInt(EffectInstance effect, String name, int value) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    public static void setVec2(EffectInstance effect, String name, float x, float y) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(x, y);
    }

    public static void setVec2(EffectInstance effect, String name, Vector2f vec) {
        setVec2(effect, name, vec.x(), vec.y());
    }

    public static void setVec3(EffectInstance effect, String name, float x, float y, float z) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(x, y, z);
    }

    public static void setVec3(EffectInstance effect, String name, Vector3f vec) {
        setVec3(effect, name, vec.x(), vec.y(), vec.z());
    }

    public static void setVec4(EffectInstance effect, String name, float x, float y, float z, float w) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(x, y, z, w);
    }

    public static void setVec4(EffectInstance effect, String name, Vector4f vec) {
        setVec4(effect, name, vec.x(), vec.y(), vec.z(), vec.w());
    }

    public static void setMatrix(EffectInstance effect, String name, Matrix4f matrix) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(matrix);
    }
}
