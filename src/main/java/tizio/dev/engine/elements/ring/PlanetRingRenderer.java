package tizio.dev.engine.elements.ring;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tizio.dev.engine.instance.ClientRenderRegistries;
import tizio.dev.engine.instance.ClientRenderTypes;
import tizio.dev.engine.volume.PreparedVolume;
import tizio.dev.engine.volume.VolumeRenderUtil;
import tizio.dev.tsp.MainClass;

import java.util.List;
import java.util.stream.Collectors;

public final class PlanetRingRenderer {

    public static List<VolumeRenderUtil.RenderTask> buildTasks(ShaderInstance shader, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        if (shader == null) return List.of();
        return ClientRenderRegistries.PLANETS_RINGS.instances().stream()
                .map(instance -> new VolumeRenderUtil.RenderTask(
                        instance.position(),
                        VolumeRenderUtil.RenderPass.PLANET_RING,
                        () -> renderInstance(shader, instance, camera, frustum, poseStack, bufferSource)
                )).collect(Collectors.toList());
    }

    private static void renderInstance(ShaderInstance shader, PlanetRingInstance instance, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        float halfExtent = instance.quadRadius();
        if (!VolumeRenderUtil.isVisible(frustum, instance.position(), halfExtent)) {
            return;
        }

        PreparedVolume volume = VolumeRenderUtil.prepareVolume(instance, camera, poseStack);
        Matrix3f localFromWorld = new Matrix3f().rotation(instance.orientation()).invert();
        Vector3f lightLocal = localFromWorld.transform(instance.lightDirection()).normalize();

        VolumeRenderUtil.setFloat(shader, "PlanetRadius", instance.planetRadius());
        VolumeRenderUtil.setFloat(shader, "RingInnerRadius", instance.ringInnerRadius());
        VolumeRenderUtil.setFloat(shader, "RingOuterRadius", instance.ringOuterRadius());
        VolumeRenderUtil.setVec3(shader, "BaseColor", instance.color());
        VolumeRenderUtil.setVec3(shader, "LightDirection", lightLocal);

        VolumeRenderUtil.setSampler(ResourceLocation.fromNamespaceAndPath(MainClass.MODID, instance.ringTexture()), 0);

        VolumeRenderUtil.setVec3(shader, "CenterRelative", volume.centerRelativeView());
        VolumeRenderUtil.setVec3(shader, "CameraLocalPos", volume.cameraLocalPos());
        VolumeRenderUtil.setVec3(shader, "AxisX", volume.axisXView());
        VolumeRenderUtil.setVec3(shader, "AxisY", volume.axisYView());
        VolumeRenderUtil.setVec3(shader, "AxisZ", volume.axisZView());


//        RenderSystem.enableBlend();
//        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//        RenderSystem.disableCull();
//        RenderSystem.enableDepthTest();
//        RenderSystem.depthMask(false);

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(ClientRenderTypes.ring());
        VolumeRenderUtil.renderCube(consumer, pose, volume, halfExtent);

//        RenderSystem.depthMask(true);
//        RenderSystem.enableCull();
//        RenderSystem.disableBlend();
//        RenderSystem.defaultBlendFunc();

        bufferSource.endBatch(ClientRenderTypes.ring());

        if (VolumeRenderUtil.DEBUG) {
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            VolumeRenderUtil.renderCubeLines(lineConsumer, pose, volume, halfExtent, new Vector3f(0, 255, 255));
            bufferSource.endBatch(RenderType.lines());
        }
    }
}
