package tizio.dev.tsp.core.celestial.instance.elements.planet.atmosphere;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunInstance;
import tizio.dev.tsp.core.client.ClientRenderRegistries;
import tizio.dev.tsp.core.client.ClientRenderTypes;
import tizio.dev.tsp.core.utils.volume.PreparedVolume;
import tizio.dev.tsp.core.utils.volume.VolumeRenderUtil;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class AtmosphereRenderer {

    public static List<VolumeRenderUtil.RenderTask> buildTasks(ShaderInstance shader, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        if (shader == null) return List.of();
        return ClientRenderRegistries.ATMOSPHERES.instances().stream()
                .map(instance -> new VolumeRenderUtil.RenderTask(
                        instance.position(),
                        VolumeRenderUtil.RenderPass.ATMOSPHERE,
                        () -> renderInstance(shader, instance, camera, frustum, poseStack, bufferSource)
                )).collect(Collectors.toList());
    }

    public static List<VolumeRenderUtil.RenderTask> buildTasks(ShaderInstance shader, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, List<PlanetInstance.AtmosphereInstance> instances) {
        if (shader == null || instances == null) return List.of();
        return instances.stream()
                .map(instance -> new VolumeRenderUtil.RenderTask(
                        instance.position(),
                        VolumeRenderUtil.RenderPass.ATMOSPHERE,
                        () -> renderInstance(shader, instance, camera, frustum, poseStack, bufferSource)
                )).collect(Collectors.toList());
    }

    private static void renderInstance(ShaderInstance shader, PlanetInstance.AtmosphereInstance instance, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        float halfExtent = instance.quadRadius();
        if (!VolumeRenderUtil.isVisible(frustum, instance.position(), halfExtent)) {
            return;
        }

        PreparedVolume volume = VolumeRenderUtil.prepareVolume(instance, camera, poseStack);
        Matrix3f localFromWorld = new Matrix3f().rotation(instance.orientation()).invert();

        int maxLights = 4;
        List<SunInstance> nearestSuns = ClientRenderRegistries.SUNS.instances().stream()
                .sorted(Comparator.comparingDouble(s -> s.position().subtract(instance.position()).lengthSqr()))
                .limit(maxLights)
                .collect(Collectors.toList());

        int lightCount;
        Vector3f[] lightDirsLocal = new Vector3f[maxLights];

        if (!nearestSuns.isEmpty()) {
            lightCount = nearestSuns.size();
            for (int i = 0; i < lightCount; i++) {
                net.minecraft.world.phys.Vec3 rel = nearestSuns.get(i).position().subtract(instance.position());
                Vector3f worldDir = new Vector3f((float) rel.x, (float) rel.y, (float) rel.z).normalize();
                lightDirsLocal[i] = localFromWorld.transform(worldDir).normalize();
            }
        } else {
            lightCount = 1;
            lightDirsLocal[0] = localFromWorld.transform(new Vector3f(instance.lightDirection())).normalize();
        }

        VolumeRenderUtil.setFloat(shader, "PlanetRadius", instance.planetRadius());
        VolumeRenderUtil.setFloat(shader, "AtmosphereRadius", instance.atmosphereRadius());
        VolumeRenderUtil.setFloat(shader, "Intensity", instance.intensity());
        VolumeRenderUtil.setFloat(shader, "Exposure", instance.exposure());
        VolumeRenderUtil.setFloat(shader, "RayleighScaleHeight", instance.rayleighScaleHeight());
        VolumeRenderUtil.setFloat(shader, "RayleighStrength", instance.rayleighStrength());
        VolumeRenderUtil.setVec3(shader, "BaseColor", instance.color());
        VolumeRenderUtil.setVec3(shader, "WaveLengths", instance.waveLengths());

        VolumeRenderUtil.setInt(shader, "LightCount", lightCount);
        for (int i = 0; i < lightCount; i++) {
            VolumeRenderUtil.setVec3(shader, "LightDirection" + i, lightDirsLocal[i]);
        }

        VolumeRenderUtil.setVec3(shader, "CenterRelative", volume.centerRelativeView());
        VolumeRenderUtil.setVec3(shader, "CameraLocalPos", volume.cameraLocalPos());
        VolumeRenderUtil.setVec3(shader, "AxisX", volume.axisXView());
        VolumeRenderUtil.setVec3(shader, "AxisY", volume.axisYView());
        VolumeRenderUtil.setVec3(shader, "AxisZ", volume.axisZView());

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

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(ClientRenderTypes.planetAtmosphere());
        VolumeRenderUtil.renderCube(consumer, pose, volume, halfExtent);

        bufferSource.endBatch(ClientRenderTypes.planetAtmosphere());

        if (VolumeRenderUtil.DEBUG) {
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            VolumeRenderUtil.renderCubeLines(lineConsumer, pose, volume, halfExtent, new Vector3f(255, 0, 255));
            bufferSource.endBatch(RenderType.lines());
        }

        if (scissor != null) {
            VolumeRenderUtil.disableScissor();
        }
    }
}