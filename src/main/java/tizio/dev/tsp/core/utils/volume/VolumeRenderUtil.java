package tizio.dev.tsp.core.utils.volume;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL30C.glGenerateMipmap;

public final class VolumeRenderUtil {

    public static boolean DEBUG = false;
    public static boolean PLANET_SHADOW_CAST = true;

    public enum RenderPass {
        PLANET_SURFACE(0),
        ATMOSPHERE(1),
        PLANET_RING(2),
        PLANET_ROCKS_RING(3),
        SUN(4),
        BLACK_HOLE(4),
        CLOUDS(5);

        private final int priority;

        RenderPass(int priority) {
            this.priority = priority;
        }

        public int priority() {
            return this.priority;
        }
    }

    public static boolean isVisible(Frustum frustum, Vec3 center, float halfExtent) {
        if (frustum == null) {
            return true;
        }

        float cullExtent = halfExtent * 1.7320508F;

        return frustum.isVisible(new AABB(
                center.x - cullExtent, center.y - cullExtent, center.z - cullExtent,
                center.x + cullExtent, center.y + cullExtent, center.z + cullExtent
        ));
    }

    public record RenderTask(Vec3 position, RenderPass pass, Runnable render) {}

    @SafeVarargs
    public static List<RenderTask> mergeSorted(Vec3 cameraPos, List<RenderTask>... taskLists) {
        List<RenderTask> all = new ArrayList<>();
        for (List<RenderTask> list : taskLists) all.addAll(list);
        all.sort(
                Comparator
                        .comparingDouble((RenderTask task) -> task.position().distanceToSqr(cameraPos))
                        .reversed()
                        .thenComparingInt(task -> task.pass().priority())
        );
        return all;
    }

    public static PreparedVolume prepareVolume(OrientedVolumeInstance instance, Camera camera, PoseStack poseStack) {

        Vec3 relativeCenter = instance.position().subtract(camera.getPosition());
        Vector3f axisX = instance.orientation().transform(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f axisY = instance.orientation().transform(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f axisZ = instance.orientation().transform(new Vector3f(0.0F, 0.0F, 1.0F));

        Matrix3f localFromWorld = new Matrix3f().rotation(instance.orientation()).invert();

        Vector3f cameraLocalPos = localFromWorld.transform(new Vector3f(
                (float) -relativeCenter.x,
                (float) -relativeCenter.y,
                (float) -relativeCenter.z
        ));

        Matrix3f poseRot = new Matrix3f(poseStack.last().pose());
        Vector3f centerRelativeView = poseRot.transform(new Vector3f((float) relativeCenter.x, (float) relativeCenter.y, (float) relativeCenter.z));
        Vector3f axisXView = poseRot.transform(new Vector3f(axisX));
        Vector3f axisYView = poseRot.transform(new Vector3f(axisY));
        Vector3f axisZView = poseRot.transform(new Vector3f(axisZ));

        return new PreparedVolume(relativeCenter, axisX, axisY, axisZ, centerRelativeView, axisXView, axisYView, axisZView, cameraLocalPos);
    }

    public static void renderCube(VertexConsumer consumer, Matrix4f pose, PreparedVolume volume, float halfExtent) {
        renderCube(consumer, pose, volume.relativeCenter(), volume.axisX(), volume.axisY(), volume.axisZ(), halfExtent);
    }

    public static void renderCubeLines(VertexConsumer consumer, Matrix4f pose, PreparedVolume volume, float halfExtent, Vector3f color) {
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        renderCubeLines(consumer, pose, volume.relativeCenter(), volume.axisX(), volume.axisY(), volume.axisZ(), halfExtent, color);
        RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart());
        RenderSystem.setShaderFogEnd(RenderSystem.getShaderFogEnd());
    }

    public static void setFloat(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    public static void setInt(ShaderInstance shader, String name, int value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    public static void setFloatArray(ShaderInstance shader, String name, float[] values) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(values);
    }

    public static void setVec3(ShaderInstance shader, String name, Vector3f value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value.x(), value.y(), value.z());
        }
    }

    public static void setVec4(ShaderInstance shader, String name, float x, float y, float z, float w) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z, w);
        }
    }

    public static void enableScissor(int x, int y, int width, int height) {
        RenderSystem.enableScissor(x, y, width, height);
    }

    public static void enableScissor(ScissorRect rect) {
        RenderSystem.enableScissor(rect.x(), rect.y(), rect.width(), rect.height());
    }

    public static void disableScissor() {
        RenderSystem.disableScissor();
    }

    public record ScissorRect(int x, int y, int width, int height) {}

    public static ScissorRect computeScissorRect(PreparedVolume volume, float halfExtent, Matrix4f projectionMatrix, int viewportWidth, int viewportHeight) {
        Vector3f center = volume.centerRelativeView();
        Vector3f ax = new Vector3f(volume.axisXView()).mul(halfExtent);
        Vector3f ay = new Vector3f(volume.axisYView()).mul(halfExtent);
        Vector3f az = new Vector3f(volume.axisZView()).mul(halfExtent);

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (float sx = -1.0F; sx <= 1.0F; sx += 2.0F) {
            for (float sy = -1.0F; sy <= 1.0F; sy += 2.0F) {
                for (float sz = -1.0F; sz <= 1.0F; sz += 2.0F) {
                    float vx = center.x() + ax.x() * sx + ay.x() * sy + az.x() * sz;
                    float vy = center.y() + ax.y() * sx + ay.y() * sy + az.y() * sz;
                    float vz = center.z() + ax.z() * sx + ay.z() * sy + az.z() * sz;

                    Vector4f clip = new Vector4f(vx, vy, vz, 1.0F);
                    projectionMatrix.transform(clip);

                    if (clip.w() <= 1.0E-5F) {
                        return null;
                    }

                    float ndcX = clip.x() / clip.w();
                    float ndcY = clip.y() / clip.w();

                    float screenX = (ndcX * 0.5F + 0.5F) * viewportWidth;
                    float screenY = (ndcY * 0.5F + 0.5F) * viewportHeight;

                    if (screenX < minX) minX = screenX;
                    if (screenX > maxX) maxX = screenX;
                    if (screenY < minY) minY = screenY;
                    if (screenY > maxY) maxY = screenY;
                }
            }
        }

        int x = Math.max(0, (int) Math.floor(minX));
        int y = Math.max(0, (int) Math.floor(minY));
        int maxXi = Math.min(viewportWidth, (int) Math.ceil(maxX));
        int maxYi = Math.min(viewportHeight, (int) Math.ceil(maxY));

        int width = Math.max(0, maxXi - x);
        int height = Math.max(0, maxYi - y);

        return new ScissorRect(x, y, width, height);
    }

    public static void setSampler(ShaderInstance shader, String samplerName, ResourceLocation textureLocation, int textureUnit) {
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(textureLocation);
        if (texture == null) { return; }

        RenderSystem.setShaderTexture(textureUnit, textureLocation);
        RenderSystem.activeTexture(GL_TEXTURE0 + textureUnit);
        RenderSystem.bindTexture(texture.getId());
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
//        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
//        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);

        shader.setSampler(samplerName, texture.getId());
    }

    private static void renderCube(VertexConsumer consumer, Matrix4f pose, Vec3 center, Vector3f axisX, Vector3f axisY, Vector3f axisZ, float halfExtent) {
        Vector3f px = new Vector3f(axisX).mul(halfExtent);
        Vector3f py = new Vector3f(axisY).mul(halfExtent);
        Vector3f pz = new Vector3f(axisZ).mul(halfExtent);

        Vector3f ppp = vertex(center, px, py, pz, 1.0F, 1.0F, 1.0F);
        Vector3f ppm = vertex(center, px, py, pz, 1.0F, 1.0F, -1.0F);
        Vector3f pmp = vertex(center, px, py, pz, 1.0F, -1.0F, 1.0F);
        Vector3f pmm = vertex(center, px, py, pz, 1.0F, -1.0F, -1.0F);
        Vector3f mpp = vertex(center, px, py, pz, -1.0F, 1.0F, 1.0F);
        Vector3f mpm = vertex(center, px, py, pz, -1.0F, 1.0F, -1.0F);
        Vector3f mmp = vertex(center, px, py, pz, -1.0F, -1.0F, 1.0F);
        Vector3f mmm = vertex(center, px, py, pz, -1.0F, -1.0F, -1.0F);

        addQuad(consumer, pose, ppp, pmp, pmm, ppm);
        addQuad(consumer, pose, mpp, mpm, mmm, mmp);
        addQuad(consumer, pose, ppp, ppm, mpm, mpp);
        addQuad(consumer, pose, pmp, mmp, mmm, pmm);
        addQuad(consumer, pose, ppp, mpp, mmp, pmp);
        addQuad(consumer, pose, ppm, pmm, mmm, mpm);
    }

    private static void renderCubeLines(VertexConsumer consumer, Matrix4f pose, Vec3 center, Vector3f axisX, Vector3f axisY, Vector3f axisZ, float halfExtent, Vector3f color) {
        Vector3f px = new Vector3f(axisX).mul(halfExtent);
        Vector3f py = new Vector3f(axisY).mul(halfExtent);
        Vector3f pz = new Vector3f(axisZ).mul(halfExtent);

        Vector3f ppp = vertex(center, px, py, pz, 1, 1, 1);
        Vector3f ppm = vertex(center, px, py, pz, 1, 1, -1);
        Vector3f pmp = vertex(center, px, py, pz, 1, -1, 1);
        Vector3f pmm = vertex(center, px, py, pz, 1, -1, -1);
        Vector3f mpp = vertex(center, px, py, pz, -1, 1, 1);
        Vector3f mpm = vertex(center, px, py, pz, -1, 1, -1);
        Vector3f mmp = vertex(center, px, py, pz, -1, -1, 1);
        Vector3f mmm = vertex(center, px, py, pz, -1, -1, -1);

        addLine(consumer, pose, ppp, pmp, color); addLine(consumer, pose, ppp, ppm, color); addLine(consumer, pose, ppp, mpp, color);
        addLine(consumer, pose, mmm, mmp, color); addLine(consumer, pose, mmm, mpm, color); addLine(consumer, pose, mmm, pmm, color);
        addLine(consumer, pose, pmp, mmp, color); addLine(consumer, pose, pmp, pmm, color);
        addLine(consumer, pose, ppm, mpm, color); addLine(consumer, pose, ppm, pmm, color);
        addLine(consumer, pose, mpp, mpm, color); addLine(consumer, pose, mpp, mmp, color);
    }

    private static void addLine(VertexConsumer consumer, Matrix4f pose, Vector3f a, Vector3f b, Vector3f color) {
        consumer.vertex(pose, a.x(), a.y(), a.z()).color((int) color.x(), (int) color.y(), (int) color.z(), 255).normal(new Vector3f(b).sub(a).normalize().x(), new Vector3f(b).sub(a).normalize().y(), new Vector3f(b).sub(a).normalize().z()).endVertex();
        consumer.vertex(pose, b.x(), b.y(), b.z()).color((int) color.x(), (int) color.y(), (int) color.z(), 255).normal(new Vector3f(b).sub(a).normalize().x(), new Vector3f(b).sub(a).normalize().y(), new Vector3f(b).sub(a).normalize().z()).endVertex();
    }

    private static Vector3f vertex(Vec3 center, Vector3f axisX, Vector3f axisY, Vector3f axisZ, float sx, float sy, float sz) {
        return new Vector3f(
                (float) center.x + axisX.x() * sx + axisY.x() * sy + axisZ.x() * sz,
                (float) center.y + axisX.y() * sx + axisY.y() * sy + axisZ.y() * sz,
                (float) center.z + axisX.z() * sx + axisY.z() * sy + axisZ.z() * sz
        );
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f pose, Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
        consumer.vertex(pose, a.x(), a.y(), a.z()).uv(0.0F, 0.0F).endVertex();
        consumer.vertex(pose, d.x(), d.y(), d.z()).uv(1.0F, 0.0F).endVertex();
        consumer.vertex(pose, c.x(), c.y(), c.z()).uv(1.0F, 1.0F).endVertex();
        consumer.vertex(pose, b.x(), b.y(), b.z()).uv(0.0F, 1.0F).endVertex();
    }
}