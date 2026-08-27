package tizio.dev.tsp.core.celestial.instance.elements.planet.clouds;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.client.ClientShaderRegistry;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.utils.Materials;
import tizio.dev.tsp.core.utils.volume.VolumeRenderUtil;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;

public final class SkyCloudsRenderer {

    private static final float BASE_HEIGHT = 192.0F;
    private static final float HEIGHT_SCALE = 4000.0F;
    private static final float MIN_HALF_SIZE = 1024.0F;

    private SkyCloudsRenderer() {}

    public static List<VolumeRenderUtil.RenderTask> buildTasks(
            ShaderInstance shader,
            PoseStack poseStack,
            float partialTick,
            double camX,
            double camY,
            double camZ,
            PlanetInstance.Clouds clouds
    ) {
        List<VolumeRenderUtil.RenderTask> tasks = new ArrayList<>();

        if (shader == null || clouds == null || !clouds.enabled) {
            return tasks;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return tasks;
        }

        float halfSize = computeHalfSize(mc);
        float cloudWorldY = BASE_HEIGHT + clouds.height * HEIGHT_SCALE;
        Vector3f color = CelestialJsonLoader.parseColor(clouds.colorHex, new Vector3f(1.0F, 1.0F, 1.0F));
        float time = (mc.level.getGameTime() + partialTick) / 20.0F;
        Vector3f sunDir = computeSunDirection(mc, partialTick);

        Matrix4f cameraMatrix = new Matrix4f(poseStack.last().pose());
        Vec3 cloudPos = new Vec3(camX, cloudWorldY, camZ);

        tasks.add(new VolumeRenderUtil.RenderTask(cloudPos, VolumeRenderUtil.RenderPass.CLOUDS, () -> {
            PoseStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushPose();
            modelViewStack.mulPoseMatrix(cameraMatrix);
            modelViewStack.translate(0.0D, cloudWorldY - camY, 0.0D);
            RenderSystem.applyModelViewMatrix();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);

            RenderSystem.setShader(ClientShaderRegistry::skyClouds);
            RenderSystem.setShaderTexture(0, Materials.resolveTextureLocation(clouds.texture));
            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL,  0);

            VolumeRenderUtil.setFloat(shader, "Time", time);
            VolumeRenderUtil.setFloat(shader, "CloudWindSpeed", clouds.windSpeed);
            VolumeRenderUtil.setFloat(shader, "CloudCoverage", clouds.density);
            VolumeRenderUtil.setFloat(shader, "CloudNoiseScale", 0.10f); //clouds.noiseScale
            VolumeRenderUtil.setVec4(shader, "CloudColor", color.x(), color.y(), color.z(), clouds.alpha);
            VolumeRenderUtil.setVec3(shader, "LightDirection", sunDir);
            VolumeRenderUtil.setVec3(shader, "CameraPos", new Vector3f((float) camX, 0f, (float) camZ));
            VolumeRenderUtil.setFloat(shader, "MaxDistance", halfSize);

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            buffer.vertex(-halfSize, 0.0F, -halfSize).uv(((float) camX - halfSize), ((float) camZ - halfSize)).endVertex();
            buffer.vertex(-halfSize, 0.0F, halfSize).uv(((float) camX - halfSize), ((float) camZ + halfSize)).endVertex();
            buffer.vertex(halfSize, 0.0F, halfSize).uv(((float) camX + halfSize), ((float) camZ + halfSize)).endVertex();
            buffer.vertex(halfSize, 0.0F, -halfSize).uv(((float) camX + halfSize), ((float) camZ - halfSize)).endVertex();

            BufferUploader.drawWithShader(buffer.end());

            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();

            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }));

        return tasks;
    }

    private static float computeHalfSize(Minecraft mc) {
        int chunks = mc.options.renderDistance().get();
        float blocks = chunks * 16.0F;
        return Math.max(MIN_HALF_SIZE, blocks * 2.0F);
    }

    private static Vector3f computeSunDirection(Minecraft mc, float partialTick) {
        float angle = mc.level.getSunAngle(partialTick);
        Quaternionf rotation = new Quaternionf()
                .rotateY((float) Math.toRadians(-90.0))
                .rotateX(angle);
        return rotation.transform(new Vector3f(0.0F, 1.0F, 0.0F)).normalize();
    }
}