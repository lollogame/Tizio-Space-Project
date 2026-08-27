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
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.client.ClientRenderRegistries;
import tizio.dev.tsp.core.client.ClientRenderTypes;
import tizio.dev.tsp.core.utils.Materials;
import tizio.dev.tsp.core.utils.volume.PreparedVolume;
import tizio.dev.tsp.core.utils.volume.VolumeRenderUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class PlanetRingRockRenderer {

    private static final Map<PlanetInstance.RingInstance.RocksInstance, VertexBuffer> MESH_CACHE = new ConcurrentHashMap<>();

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

        float halfExtent = instance.quadRadius();
        if (!VolumeRenderUtil.isVisible(frustum, instance.position(), halfExtent)) {
            return;
        }

        double cameraDist = instance.position().distanceTo(camera.getPosition());
        float maxDist = Math.max(instance.ringOuterRadius() * 0.215f, 20.0f);

        if (cameraDist - instance.ringOuterRadius() > maxDist) {
            return;
        }

        float planetRadius = instance.planetRadius() / 2.0f;

        VertexBuffer mesh = MESH_CACHE.computeIfAbsent(instance, inst -> buildMesh(inst.rockCount()));

        PreparedVolume volume = VolumeRenderUtil.prepareVolume(instance, camera, poseStack);
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

//        ResourceLocation textureLocation = Materials.resolveTextureLocation(instance.rockTexture());
//        Minecraft.getInstance().getTextureManager().bindForSetup(textureLocation);
//        RenderSystem.setShaderTexture(0, textureLocation);
//        VolumeRenderUtil.setSampler(textureLocation, 0);

        VolumeRenderUtil.setSampler(Materials.resolveTextureLocation(instance.rockTexture()), 0);

        Matrix4f projection = RenderSystem.getProjectionMatrix();
        int vpWidth = Minecraft.getInstance().getWindow().getWidth();
        int vpHeight = Minecraft.getInstance().getWindow().getHeight();
        VolumeRenderUtil.ScissorRect scissor = VolumeRenderUtil.computeScissorRect(volume, halfExtent, projection, vpWidth, vpHeight);

        if (scissor != null && (scissor.width() == 0 || scissor.height() == 0)) {
            return;
        }

        if (scissor != null) {
            VolumeRenderUtil.enableScissor(scissor);
        }

        mesh.bind();

        VolumeRenderUtil.setFloat(shader, "OpaquePass", 1.0f);
        RenderType opaqueType = ClientRenderTypes.planetRingRocksOpaque();
        opaqueType.setupRenderState(); mesh.drawWithShader(poseStack.last().pose(), projection, shader); opaqueType.clearRenderState();

        VolumeRenderUtil.setFloat(shader, "OpaquePass", 0.0f);
        RenderType transparentType = ClientRenderTypes.planetRingRocksTransparent();
        transparentType.setupRenderState(); mesh.drawWithShader(poseStack.last().pose(), projection, shader); transparentType.clearRenderState();

        VertexBuffer.unbind();

        if (VolumeRenderUtil.DEBUG) {
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            VolumeRenderUtil.renderCubeLines(lineConsumer, poseStack.last().pose(), volume, halfExtent, new Vector3f(50, 100, 255));
            bufferSource.endBatch(RenderType.lines());
        }

        if (scissor != null) {
            VolumeRenderUtil.disableScissor();
        }
    }

    public static void cleanupCache(Collection<PlanetInstance.RingInstance.RocksInstance> activeInstances) {
        MESH_CACHE.entrySet().removeIf(entry -> {
            boolean obsolete = !activeInstances.contains(entry.getKey());
            if (obsolete) {
                entry.getValue().close();
            }
            return obsolete;
        });
    }

    public static void clearCache() {
        MESH_CACHE.values().forEach(VertexBuffer::close);
        MESH_CACHE.clear();
    }

    private static VertexBuffer buildMesh(int rockCount) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        for (int i = 0; i < rockCount; i++) {
            addCube(builder);
        }

        VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vertexBuffer.bind();
        vertexBuffer.upload(builder.end());
        VertexBuffer.unbind();
        return vertexBuffer;
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
        builder.vertex(dx, dy, dz).uv(0.0F, 1.0F).endVertex();
    }

}