package tizio.dev.engine.elements.atmosphere;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import tizio.dev.engine.instance.ClientRenderRegistries;
import tizio.dev.engine.instance.ClientRenderTypes;
import tizio.dev.engine.utils.UniformOverrides;
import tizio.dev.engine.volume.PreparedVolume;
import tizio.dev.engine.volume.VolumeRenderUtil;

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

    private static void renderInstance(ShaderInstance shader, AtmosphereInstance instance, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        float halfExtent = instance.quadRadius();
        if (!VolumeRenderUtil.isVisible(frustum, instance.position(), halfExtent)) {
            return;
        }

        PreparedVolume volume = VolumeRenderUtil.prepareVolume(instance, camera, poseStack);
        Matrix3f localFromWorld = new Matrix3f().rotation(instance.orientation()).invert();
        Vector3f lightLocal = localFromWorld.transform(instance.lightDirection()).normalize();

        VolumeRenderUtil.setFloat(shader, "PlanetRadius", instance.planetRadius());
        VolumeRenderUtil.setFloat(shader, "AtmosphereRadius", instance.atmosphereRadius());
        VolumeRenderUtil.setFloat(shader, "Intensity", instance.intensity());
        VolumeRenderUtil.setFloat(shader, "Exposure", instance.exposure());
        VolumeRenderUtil.setFloat(shader, "RayleighScaleHeight", instance.rayleighScaleHeight());
        VolumeRenderUtil.setFloat(shader, "RayleighStrength", instance.rayleighStrength());
        VolumeRenderUtil.setVec3(shader, "BaseColor", instance.color());
        VolumeRenderUtil.setVec3(shader, "WaveLengths", instance.waveLengths());
        VolumeRenderUtil.setVec3(shader, "LightDirection", lightLocal);

        VolumeRenderUtil.setVec3(shader, "CenterRelative", volume.centerRelativeView());
        VolumeRenderUtil.setVec3(shader, "CameraLocalPos", volume.cameraLocalPos());
        VolumeRenderUtil.setVec3(shader, "AxisX", volume.axisXView());
        VolumeRenderUtil.setVec3(shader, "AxisY", volume.axisYView());
        VolumeRenderUtil.setVec3(shader, "AxisZ", volume.axisZView());


        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(ClientRenderTypes.atmosphere());
        VolumeRenderUtil.renderCube(consumer, pose, volume, halfExtent);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();

        bufferSource.endBatch(ClientRenderTypes.atmosphere());

        if (VolumeRenderUtil.DEBUG) {
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            VolumeRenderUtil.renderCubeLines(lineConsumer, pose, volume, halfExtent, new Vector3f(255, 0, 255));
            bufferSource.endBatch(RenderType.lines());
        }
    }
}
