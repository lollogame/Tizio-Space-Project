package tizio.dev.tsp.core.celestial.instance.elements.blackhole;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.client.ClientRenderRegistries;
import tizio.dev.tsp.core.client.ClientRenderTypes;
import tizio.dev.tsp.core.utils.Materials;
import tizio.dev.tsp.core.utils.volume.PreparedVolume;
import tizio.dev.tsp.core.utils.volume.VolumeRenderUtil;

import java.util.List;
import java.util.stream.Collectors;

public final class BlackHoleRenderer {

    public static List<VolumeRenderUtil.RenderTask> buildTasks(ShaderInstance shader, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float timeSeconds) {
        if (shader == null) return List.of();
        return ClientRenderRegistries.BLACK_HOLES.instances().stream()
                .map(instance -> new VolumeRenderUtil.RenderTask(
                        instance.position(),
                        VolumeRenderUtil.RenderPass.BLACK_HOLE,
                        () -> renderInstance(shader, instance, camera, frustum, poseStack, bufferSource, timeSeconds)
                )).collect(Collectors.toList());
    }

    private static void renderInstance(ShaderInstance shader, BlackHoleInstance instance, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float timeSeconds) {
        float halfExtent = instance.quadRadius();
        if (!VolumeRenderUtil.isVisible(frustum, instance.position(), halfExtent)) {
            return;
        }

        PreparedVolume volume = VolumeRenderUtil.prepareVolume(instance, camera, poseStack);

        VolumeRenderUtil.setFloat(shader, "Time", timeSeconds);
        VolumeRenderUtil.setSampler(shader, "Sampler0", Materials.resolveTextureLocation("noise4"), 0);

        VolumeRenderUtil.setFloat(shader, "EffectRadius", instance.radius());
        VolumeRenderUtil.setFloat(shader, "DiskRotationSpeed", instance.diskRotationSpeed());
        VolumeRenderUtil.setFloat(shader, "Intensity", instance.intensity());
        VolumeRenderUtil.setVec3(shader, "BaseColor", instance.color());
        VolumeRenderUtil.setVec3(shader, "CameraLocalPos", volume.cameraLocalPos());

        VolumeRenderUtil.setVec3(shader, "CenterRelative", volume.centerRelativeView());
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
        VertexConsumer consumer = bufferSource.getBuffer(ClientRenderTypes.blackHole());
        VolumeRenderUtil.renderCube(consumer, pose, volume, halfExtent);

        bufferSource.endBatch(ClientRenderTypes.blackHole());

        if (VolumeRenderUtil.DEBUG) {
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            VolumeRenderUtil.renderCubeLines(lineConsumer, pose, volume, halfExtent, new Vector3f(0, 255, 0));
            bufferSource.endBatch(RenderType.lines());
        }

        if (scissor != null) {
            VolumeRenderUtil.disableScissor();
        }
    }
}