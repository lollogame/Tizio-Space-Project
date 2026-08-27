package tizio.dev.tsp.core.celestial.instance.elements.sun;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.client.ClientRenderRegistries;
import tizio.dev.tsp.core.client.ClientRenderTypes;
import tizio.dev.tsp.core.client.ClientShaderRegistry;
import tizio.dev.tsp.core.utils.volume.PreparedVolume;
import tizio.dev.tsp.core.utils.volume.VolumeRenderUtil;

import java.util.List;
import java.util.stream.Collectors;

public final class SunRenderer {

    private static final float FLARE_SIZE_MULTIPLIER = 6.0F;
    private static final float FLARE_FADE_NEAR_MULTIPLIER = 1.5F;
    private static final float FLARE_FADE_FAR_MULTIPLIER  = 10.0F;
    private static final float FLARE_VIEW_FADE_START = 1.0F;
    private static final float FLARE_VIEW_FADE_END   = 0.15F;


    public static List<VolumeRenderUtil.RenderTask> buildTasks(ShaderInstance shader, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float timeSeconds) {
        if (shader == null) return List.of();
        return ClientRenderRegistries.SUNS.instances().stream()
                .map(instance -> new VolumeRenderUtil.RenderTask(
                        instance.position(),
                        VolumeRenderUtil.RenderPass.SUN,
                        () -> renderInstance(shader, instance, camera, frustum, poseStack, bufferSource, timeSeconds)
                )).collect(Collectors.toList());
    }

    /** Overload that renders an explicit list of instances instead of the global registry. */
    public static List<VolumeRenderUtil.RenderTask> buildTasks(ShaderInstance shader, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float timeSeconds, List<SunInstance> instances) {
        if (shader == null || instances == null) return List.of();
        return instances.stream()
                .map(instance -> new VolumeRenderUtil.RenderTask(
                        instance.position(),
                        VolumeRenderUtil.RenderPass.SUN,
                        () -> renderInstance(shader, instance, camera, frustum, poseStack, bufferSource, timeSeconds)
                )).collect(Collectors.toList());
    }

    private static void renderInstance(ShaderInstance shader, SunInstance instance, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float timeSeconds) {
        float halfExtent = instance.quadRadius();
        if (!VolumeRenderUtil.isVisible(frustum, instance.position(), halfExtent)) {
            return;
        }

        PreparedVolume volume = VolumeRenderUtil.prepareVolume(instance, camera, poseStack);

        VolumeRenderUtil.setFloat(shader, "Time", timeSeconds);
        VolumeRenderUtil.setFloat(shader, "PlanetRadius", instance.planetRadius());
        VolumeRenderUtil.setFloat(shader, "SunRadius", instance.sunRadius());
        VolumeRenderUtil.setFloat(shader, "BloomRadius", instance.sunRadius() * 1.12F);
        VolumeRenderUtil.setFloat(shader, "Density", instance.densityFalloff());
        VolumeRenderUtil.setFloat(shader, "ScatteringStrength", instance.scatteringStrength());
        VolumeRenderUtil.setVec3(shader, "SunTint", instance.color());

        VolumeRenderUtil.setSampler(ResourceLocation.fromNamespaceAndPath(MainClass.MODID, "textures/planets/noise1.png"), 0);

        VolumeRenderUtil.setVec3(shader, "CenterRelative", volume.centerRelativeView());
        VolumeRenderUtil.setVec3(shader, "CameraLocalPos", volume.cameraLocalPos());
        VolumeRenderUtil.setVec3(shader, "AxisX", volume.axisXView());
        VolumeRenderUtil.setVec3(shader, "AxisY", volume.axisYView());
        VolumeRenderUtil.setVec3(shader, "AxisZ", volume.axisZView());

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(ClientRenderTypes.sun());
        VolumeRenderUtil.renderCube(consumer, pose, volume, halfExtent);

        bufferSource.endBatch(ClientRenderTypes.sun());

        renderFlare(instance, camera, frustum, pose, timeSeconds);

        if (VolumeRenderUtil.DEBUG) {
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            VolumeRenderUtil.renderCubeLines(lineConsumer, pose, volume, halfExtent, new Vector3f(255, 128, 0));
            bufferSource.endBatch(RenderType.lines());
        }
    }

    private static void renderFlare(SunInstance instance, Camera camera, Frustum frustum, Matrix4f pose, float timeSeconds) {
        ShaderInstance flareShader = ClientShaderRegistry.sunFlareShader();
        if (flareShader == null) return;

        Vec3 toSun = instance.position().subtract(camera.getPosition());
        double distance = toSun.length();

        float fadeNear = instance.sunRadius() * FLARE_FADE_NEAR_MULTIPLIER;
        float fadeFar  = instance.sunRadius() * FLARE_FADE_FAR_MULTIPLIER;
        float distanceAlpha = (float) ((distance - fadeNear) / (fadeFar - fadeNear));
        distanceAlpha = Math.max(0.0F, Math.min(1.0F, distanceAlpha));
        if (distanceAlpha <= 0.0F) return;

        Vector3f dirToSun = new Vector3f((float) toSun.x, (float) toSun.y, (float) toSun.z);
        if (dirToSun.lengthSquared() < 1e-6f) return;
        dirToSun.normalize();

        float dot = camera.getLookVector().dot(dirToSun);
        float viewAlpha = (dot - FLARE_VIEW_FADE_END) / (FLARE_VIEW_FADE_START - FLARE_VIEW_FADE_END);
        viewAlpha = Math.max(0.0F, Math.min(1.0F, viewAlpha));
        if (viewAlpha <= 0.0F) return;

        float alpha = distanceAlpha * viewAlpha;

        float size = instance.sunRadius() * FLARE_SIZE_MULTIPLIER;
        if (!VolumeRenderUtil.isVisible(frustum, instance.position(), size)) {
            return;
        }

        Vector3f worldUp = new Vector3f(0.0F, 1.0F, 0.0F);
        if (Math.abs(dirToSun.y) > 0.999F) {
            worldUp = new Vector3f(0.0F, 0.0F, 1.0F);
        }

        Vector3f right = new Vector3f(worldUp).cross(dirToSun).normalize();
        Vector3f up    = new Vector3f(dirToSun).cross(right).normalize();

        Vector3f center = new Vector3f((float) toSun.x, (float) toSun.y, (float) toSun.z);
        Vector3f right2 = new Vector3f(right).mul(size);
        Vector3f up2    = new Vector3f(up).mul(size);

        Vector3f p1 = new Vector3f(center).sub(right2).sub(up2);
        Vector3f p2 = new Vector3f(center).add(right2).sub(up2);
        Vector3f p3 = new Vector3f(center).add(right2).add(up2);
        Vector3f p4 = new Vector3f(center).sub(right2).add(up2);

        RenderSystem.setShader(() -> flareShader);
        VolumeRenderUtil.setFloat(flareShader, "Time", timeSeconds);
        VolumeRenderUtil.setFloat(flareShader, "FlareAlpha", alpha);
        VolumeRenderUtil.setVec3(flareShader, "SunTint", instance.color());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        buffer.vertex(pose, p1.x(), p1.y(), p1.z()).uv(0.0F, 1.0F).color(255, 255, 255, 255).endVertex();
        buffer.vertex(pose, p2.x(), p2.y(), p2.z()).uv(1.0F, 1.0F).color(255, 255, 255, 255).endVertex();
        buffer.vertex(pose, p3.x(), p3.y(), p3.z()).uv(1.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        buffer.vertex(pose, p4.x(), p4.y(), p4.z()).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();

        tesselator.end();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }
}