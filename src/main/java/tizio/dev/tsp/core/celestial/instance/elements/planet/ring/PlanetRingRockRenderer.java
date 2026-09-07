package tizio.dev.tsp.core.celestial.instance.elements.planet.ring;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL31C;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.client.ClientRenderRegistries;
import tizio.dev.tsp.core.client.ClientRenderTypes;
import tizio.dev.tsp.core.utils.Materials;
import tizio.dev.tsp.core.utils.volume.PreparedVolume;
import tizio.dev.tsp.core.utils.volume.VolumeRenderUtil;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class PlanetRingRockRenderer {

    private static VertexBuffer SINGLE_CUBE_MESH = null;

    public static List<VolumeRenderUtil.RenderTask> buildTasks(ShaderInstance shader, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float timeSeconds) {
        if (shader == null) return List.of();
        return ClientRenderRegistries.PLANETS_RING_ROCKS.instances().stream()
                .map(instance -> new VolumeRenderUtil.RenderTask(
                        instance.position(),
                        VolumeRenderUtil.RenderPass.PLANET_ROCKS_RING,
                        () -> renderInstance(shader, instance, camera, frustum, poseStack, bufferSource, timeSeconds)
                )).collect(Collectors.toList());
    }

    private static void renderInstance(ShaderInstance shader, PlanetInstance.RingInstance.RocksInstance instance, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float timeSeconds) {

        float maxDist = Math.max(instance.ringOuterRadius() * 0.10f, 20.0f);
        double cameraDistCenter = instance.position().distanceTo(camera.getPosition());

        if (cameraDistCenter - instance.ringOuterRadius() > maxDist) {
            return;
        }

        PreparedVolume volume = VolumeRenderUtil.prepareVolume(instance, camera, poseStack);
        VertexBuffer mesh = getOrCreateSingleCubeMesh();

        Matrix3f localFromWorld = new Matrix3f().rotation(instance.orientation()).invert();
        Vector3f lightLocal = localFromWorld.transform(instance.lightDirection()).normalize();

        VolumeRenderUtil.setFloat(shader, "PlanetRadius", instance.planetRadius());
        VolumeRenderUtil.setFloat(shader, "RingInnerRadius", instance.ringInnerRadius());
        VolumeRenderUtil.setFloat(shader, "RingOuterRadius", instance.ringOuterRadius());
        VolumeRenderUtil.setFloat(shader, "RockMinSize", instance.rockMinSize());
        VolumeRenderUtil.setFloat(shader, "RockMaxSize", instance.rockMaxSize());
        VolumeRenderUtil.setFloat(shader, "RockHeight", instance.rockHeight());
        VolumeRenderUtil.setFloat(shader, "OrbitSpeed", instance.orbitSpeed());
        VolumeRenderUtil.setFloat(shader, "Time", timeSeconds);
        VolumeRenderUtil.setFloat(shader, "RingSeed", instance.seed());
        VolumeRenderUtil.setVec3(shader, "LightDirection", lightLocal);

        VolumeRenderUtil.setVec3(shader, "CenterRelative", volume.centerRelativeView());
        VolumeRenderUtil.setVec3(shader, "CameraLocalPos", volume.cameraLocalPos());
        VolumeRenderUtil.setVec3(shader, "AxisX", volume.axisXView());
        VolumeRenderUtil.setVec3(shader, "AxisY", volume.axisYView());
        VolumeRenderUtil.setVec3(shader, "AxisZ", volume.axisZView());

        VolumeRenderUtil.setSampler(shader, "Sampler0", Materials.resolveTextureLocation(instance.rockTexture()), 0);

        Matrix4f projection = RenderSystem.getProjectionMatrix();
        int vpWidth = Minecraft.getInstance().getWindow().getWidth();
        int vpHeight = Minecraft.getInstance().getWindow().getHeight();
        VolumeRenderUtil.ScissorRect scissor = VolumeRenderUtil.computeScissorRect(volume, instance.quadRadius(), projection, vpWidth, vpHeight);

        if (scissor != null && (scissor.width() == 0 || scissor.height() == 0)) {
            return;
        }

        if (scissor != null) {
            VolumeRenderUtil.enableScissor(scissor);
        }

        RenderType renderType = ClientRenderTypes.planetRingRocks();
        renderType.setupRenderState();
        drawInstancedWithShader(mesh, poseStack.last().pose(), projection, shader, instance.rockCount());
        renderType.clearRenderState();

        if (scissor != null) {
            VolumeRenderUtil.disableScissor();
        }
    }

    private static void drawInstancedWithShader(VertexBuffer mesh, Matrix4f modelView, Matrix4f projection, ShaderInstance shader, int instanceCount) {
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(modelView);
        }
        if (shader.PROJECTION_MATRIX != null) {
            shader.PROJECTION_MATRIX.set(projection);
        }
        shader.apply();
        mesh.bind();
        GL31C.glDrawArraysInstanced(GL11C.GL_TRIANGLES, 0, 36, instanceCount);
        VertexBuffer.unbind();
        shader.clear();
    }

    public static void cleanupCache(Collection<PlanetInstance.RingInstance.RocksInstance> activeInstances) {}

    public static void clearCache() {
        if (SINGLE_CUBE_MESH != null) {
            SINGLE_CUBE_MESH.close();
            SINGLE_CUBE_MESH = null;
        }
    }

    private static synchronized VertexBuffer getOrCreateSingleCubeMesh() {
        if (SINGLE_CUBE_MESH == null) {
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
            addCube(builder);

            SINGLE_CUBE_MESH = new VertexBuffer(VertexBuffer.Usage.STATIC);
            SINGLE_CUBE_MESH.bind();
            SINGLE_CUBE_MESH.upload(builder.end());
            VertexBuffer.unbind();
        }
        return SINGLE_CUBE_MESH;
    }

    private static void addCube(BufferBuilder builder) {
        addFace(builder, 1, -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1);
        addFace(builder, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1);
        addFace(builder, -1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, -1);
        addFace(builder, -1, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, 1);
        addFace(builder, -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1);
        addFace(builder, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1, -1);
    }

    private static void addFace(BufferBuilder builder, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
        builder.vertex(ax, ay, az).uv(0.0F, 0.0F).endVertex();
        builder.vertex(bx, by, bz).uv(1.0F, 0.0F).endVertex();
        builder.vertex(cx, cy, cz).uv(1.0F, 1.0F).endVertex();

        builder.vertex(ax, ay, az).uv(0.0F, 0.0F).endVertex();
        builder.vertex(cx, cy, cz).uv(1.0F, 1.0F).endVertex();
        builder.vertex(dx, dy, dz).uv(0.0F, 1.0F).endVertex();
    }
}
