package tizio.dev.tsp.core.celestial.instance.elements.planet;

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
import tizio.dev.tsp.core.client.ClientRenderRegistries;
import tizio.dev.tsp.core.client.ClientRenderTypes;
import tizio.dev.tsp.core.utils.Materials;
import tizio.dev.tsp.core.utils.volume.PreparedVolume;
import tizio.dev.tsp.core.utils.volume.VolumeRenderUtil;

import java.util.List;
import java.util.stream.Collectors;

public final class PlanetSurfaceRenderer {

    public static List<VolumeRenderUtil.RenderTask> buildTasks(ShaderInstance shader, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        return buildTasks(shader, camera, frustum, poseStack, bufferSource, 0.0F);
    }

    public static List<VolumeRenderUtil.RenderTask> buildTasks(ShaderInstance shader, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float timeSeconds) {
        if (shader == null) return List.of();
        return ClientRenderRegistries.PLANETS_SURFACES.instances().stream()
                .map(instance -> new VolumeRenderUtil.RenderTask(
                        instance.position(),
                        VolumeRenderUtil.RenderPass.PLANET_SURFACE,
                        () -> renderInstance(shader, instance, camera, frustum, poseStack, bufferSource, timeSeconds)
                )).collect(Collectors.toList());
    }

    public static List<VolumeRenderUtil.RenderTask> buildTasks(ShaderInstance shader, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float timeSeconds, List<PlanetInstance.SurfaceInstance> instances) {
        if (shader == null || instances == null) return List.of();
        return instances.stream()
                .map(instance -> new VolumeRenderUtil.RenderTask(
                        instance.position(),
                        VolumeRenderUtil.RenderPass.PLANET_SURFACE,
                        () -> renderInstance(shader, instance, camera, frustum, poseStack, bufferSource, timeSeconds)
                )).collect(Collectors.toList());
    }

    private static void renderInstance(ShaderInstance shader, PlanetInstance.SurfaceInstance instance, Camera camera, Frustum frustum, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float timeSeconds) {
        float halfExtent = instance.quadRadius();
        if (!VolumeRenderUtil.isVisible(frustum, instance.position(), halfExtent)) {
            return;
        }

        PreparedVolume volume = VolumeRenderUtil.prepareVolume(instance, camera, poseStack);
        Matrix3f localFromWorld = new Matrix3f().rotation(instance.orientation()).invert();
        Vector3f lightLocal = localFromWorld.transform(instance.lightDirection()).normalize();

        VolumeRenderUtil.setFloat(shader, "PlanetRadius", instance.planetRadius());
        VolumeRenderUtil.setFloat(shader, "UseNightTexture", instance.hasNightTexture() ? 1.0F : 0.0F);
        VolumeRenderUtil.setFloat(shader, "EmissiveStrength", instance.emissiveStrength());
        VolumeRenderUtil.setVec3(shader, "LightDirection", lightLocal);
        VolumeRenderUtil.setFloat(shader, "Time", timeSeconds);
        VolumeRenderUtil.setFloat(shader, "SurfaceRotation", instance.surfaceRotationAngle(timeSeconds));
        VolumeRenderUtil.setFloat(shader, "CloudsEnabled", instance.hasClouds() ? 1.0F : 0.0F);
        VolumeRenderUtil.setFloat(shader, "CloudHeight", instance.cloudHeight());
        VolumeRenderUtil.setFloat(shader, "CloudCoverage", instance.cloudCoverage());
        VolumeRenderUtil.setFloat(shader, "CloudWindSpeed", instance.cloudWindSpeed());
        VolumeRenderUtil.setFloat(shader, "CloudNoiseScale", instance.cloudNoiseScale());
        VolumeRenderUtil.setVec4(shader, "CloudColor", instance.cloudColor().x(), instance.cloudColor().y(), instance.cloudColor().z(), instance.cloudAlpha());
        VolumeRenderUtil.setSampler(shader, "Sampler0", Materials.resolveTextureLocation(instance.dayTexture()), 0);
        VolumeRenderUtil.setSampler(shader, "Sampler1", Materials.resolveTextureLocation(instance.hasNightTexture() ? instance.nightTexture() : instance.dayTexture()), 1);
        VolumeRenderUtil.setSampler(shader, "Sampler2", Materials.resolveTextureLocation(instance.hasClouds() ? instance.cloudTexture() : "noise1"), 2);

        VolumeRenderUtil.setVec3(shader, "CenterRelative", volume.centerRelativeView());
        VolumeRenderUtil.setVec3(shader, "CameraLocalPos", volume.cameraLocalPos());
        VolumeRenderUtil.setVec3(shader, "AxisX", volume.axisXView());
        VolumeRenderUtil.setVec3(shader, "AxisY", volume.axisYView());
        VolumeRenderUtil.setVec3(shader, "AxisZ", volume.axisZView());

        int maxShadows = 4;
        int shadowCount = 0;

        if (VolumeRenderUtil.PLANET_SHADOW_CAST) {
            Vector3f lightWorldDir = new Vector3f(instance.lightDirection()).normalize();

            double distanceToSun = instance.position().length();

            for (PlanetInstance.SurfaceInstance other : ClientRenderRegistries.PLANETS_SURFACES.instances()) {
                if (other == instance) continue;
                if (shadowCount >= maxShadows) break;

                net.minecraft.world.phys.Vec3 relPos = other.position().subtract(instance.position());

                float dotLight = (float) (relPos.x * lightWorldDir.x() + relPos.y * lightWorldDir.y() + relPos.z * lightWorldDir.z());

                if (dotLight <= 0.0F || dotLight >= distanceToSun) continue;

                float relDistSq = (float) relPos.lengthSqr();
                float distSq = relDistSq - dotLight * dotLight;
                float shadowOuterBound = other.planetRadius() + instance.planetRadius() + Math.max(other.planetRadius() * 0.2F, dotLight * 0.03F);
                if (distSq > shadowOuterBound * shadowOuterBound) continue;

                Vector3f localCenter = localFromWorld.transform(new Vector3f((float) relPos.x, (float) relPos.y, (float) relPos.z));

                VolumeRenderUtil.setVec4(shader, "ShadowPlanet" + shadowCount, localCenter.x(), localCenter.y(), localCenter.z(), other.planetRadius());
                shadowCount++;
            }
        }

        VolumeRenderUtil.setInt(shader, "ShadowPlanetCount", shadowCount);

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
        VertexConsumer consumer = bufferSource.getBuffer(ClientRenderTypes.planetSurface());
        VolumeRenderUtil.renderCube(consumer, pose, volume, halfExtent);

        bufferSource.endBatch(ClientRenderTypes.planetSurface());

        if (VolumeRenderUtil.DEBUG) {
            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
            VolumeRenderUtil.renderCubeLines(lineConsumer, pose, volume, halfExtent, new Vector3f(255, 255, 0));
            bufferSource.endBatch(RenderType.lines());
        }

        if (scissor != null) {
            VolumeRenderUtil.disableScissor();
        }
    }
}
