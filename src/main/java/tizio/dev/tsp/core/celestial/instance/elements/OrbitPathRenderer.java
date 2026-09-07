package tizio.dev.tsp.core.celestial.instance.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.client.ClientRenderTypes;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.gui.SystemEditor;
import tizio.dev.tsp.core.utils.volume.VolumeRenderUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OrbitPathRenderer {

    private static final int SEGMENTS = 120;
    private static final float[] DEFAULT_COLOR = {0.6F, 0.85F, 1.0F};

    private OrbitPathRenderer() {
    }

    public static List<VolumeRenderUtil.RenderTask> buildTasks(Camera camera, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.screen instanceof SystemEditor)) {
            return List.of();
        }

        ResourceLocation currentDimLoc = mc.level.dimension().location();
        if (!CelestialJsonLoader.isSpaceDimension(currentDimLoc)) {
            return List.of();
        }

        Map<String, SolarSystemData> systems = CelestialJsonLoader.getActiveSystems();
        if (systems == null || systems.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now();
        String currentDimId = currentDimLoc.toString();
        Vec3 camPos = camera.getPosition();

        List<VolumeRenderUtil.RenderTask> tasks = new ArrayList<>();

        for (SolarSystemData system : systems.values()) {
            if (!currentDimId.equalsIgnoreCase(system.dimension)) {
                continue;
            }
            collectSystemTasks(system, now, camPos, poseStack, bufferSource, tasks);
        }

        return tasks;
    }

    private static void collectSystemTasks(SolarSystemData system, Instant now, Vec3 camPos, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, List<VolumeRenderUtil.RenderTask> tasks) {
        if (system.bodies == null || system.bodies.isEmpty()) {
            return;
        }

        float globalScale = system.globalScale;
        Vec3 origin = new Vec3(system.originX, system.originY, system.originZ);
        Map<String, Vec3> bodyPositions = CelestialJsonLoader.calculateBodyPositions(system, now);
        String starId = (system.star != null && system.star.id != null && !system.star.id.isBlank())
                ? system.star.id
                : "sun";

        for (PlanetInstance.Config body : system.bodies) {
            if (body.orbit == null || !body.orbit.enabled || body.orbit.radius <= 0.0D) {
                continue;
            }

            String parentId = (body.parentId != null && !body.parentId.isBlank()) ? body.parentId : "sun";
            Vec3 parentPos = bodyPositions.getOrDefault(parentId, origin);
            boolean isMoonRing = !parentId.equalsIgnoreCase(starId);

            float[] rgb = parseColorHex(body.colorHex, body.id);
            float alpha = isMoonRing ? 0.7F : 1.0F;

            double radius = body.orbit.radius * globalScale;
            double inclinationDeg = body.orbit.inclination;
            double ascendingNodeDeg = body.orbit.ascendingNode;
            double verticalOffset = body.orbit.verticalOffset * globalScale;

            tasks.add(new VolumeRenderUtil.RenderTask(
                    parentPos,
                    VolumeRenderUtil.RenderPass.ORBIT_PATH,
                    () -> renderOrbit(camPos, poseStack, bufferSource, parentPos, radius, inclinationDeg, ascendingNodeDeg, verticalOffset, rgb, alpha)
            ));
        }
    }

    private static void renderOrbit(Vec3 camPos, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 parentPos, double radius, double inclinationDeg, double ascendingNodeDeg, double verticalOffset, float[] rgb, float alpha) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.pushPose();
        Vec3 relParentPos = parentPos.subtract(camPos);
        poseStack.translate(relParentPos.x, relParentPos.y, relParentPos.z);
        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer consumer = bufferSource.getBuffer(ClientRenderTypes.orbitLines());

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        Vec3 previous = orbitOffset(radius, 0.0D, inclinationDeg, ascendingNodeDeg, verticalOffset);

        for (int i = 1; i <= SEGMENTS; i++) {
            double angleDeg = (360.0D * i) / SEGMENTS;
            Vec3 current = orbitOffset(radius, angleDeg, inclinationDeg, ascendingNodeDeg, verticalOffset);

            addLine(consumer, matrix, previous, current, rgb, alpha);

            previous = current;
        }

        bufferSource.endBatch(ClientRenderTypes.orbitLines());

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        poseStack.popPose();
    }

    private static void addLine(VertexConsumer consumer, Matrix4f matrix, Vec3 a, Vec3 b, float[] rgb, float alpha) {
        consumer.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        consumer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
    }

    private static Vec3 orbitOffset(double radius, double angleDeg, double inclinationDeg, double ascendingNodeDeg, double verticalOffset) {
        double angle = Math.toRadians(angleDeg);
        double x = Math.cos(angle) * radius;
        double y = verticalOffset;
        double z = Math.sin(angle) * radius;

        double inclination = Math.toRadians(inclinationDeg);
        double inclinedY = y * Math.cos(inclination) - z * Math.sin(inclination);
        double inclinedZ = y * Math.sin(inclination) + z * Math.cos(inclination);

        double node = Math.toRadians(ascendingNodeDeg);
        double nodeX = x * Math.cos(node) + inclinedZ * Math.sin(node);
        double nodeZ = -x * Math.sin(node) + inclinedZ * Math.cos(node);

        return new Vec3(nodeX, inclinedY, nodeZ);
    }

    private static float[] parseColorHex(String hex, String seed) {
        float[] rgb = DEFAULT_COLOR;
        if (hex != null && !hex.isBlank()) {
            try {
                String clean = hex.startsWith("#") ? hex.substring(1) : hex;
                int value = Integer.parseInt(clean, 16);
                rgb = new float[]{
                        ((value >> 16) & 0xFF) / 255.0F,
                        ((value >> 8) & 0xFF) / 255.0F,
                        (value & 0xFF) / 255.0F
                };
            } catch (NumberFormatException ignored) {
            }
        }
        return vividColor(rgb, seed);
    }

    private static float[] vividColor(float[] rgb, String seed) {
        float max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        float min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        float delta = max - min;

        float hue;
        if (delta < 1.0E-3F) {
            int hash = seed != null ? seed.hashCode() : 0;
            hue = Math.floorMod(hash, 360);
        } else if (max == rgb[0]) {
            hue = 60.0F * (((rgb[1] - rgb[2]) / delta) % 6.0F);
            if (hue < 0.0F) {
                hue += 360.0F;
            }
        } else if (max == rgb[1]) {
            hue = 60.0F * (((rgb[2] - rgb[0]) / delta) + 2.0F);
        } else {
            hue = 60.0F * (((rgb[0] - rgb[1]) / delta) + 4.0F);
        }

        return hsvToRgb(hue, 0.85F, 1.0F);
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1.0F - Math.abs((h / 60.0F) % 2.0F - 1.0F));
        float m = v - c;
        float r, g, b;
        if (h < 60.0F) {
            r = c; g = x; b = 0.0F;
        } else if (h < 120.0F) {
            r = x; g = c; b = 0.0F;
        } else if (h < 180.0F) {
            r = 0.0F; g = c; b = x;
        } else if (h < 240.0F) {
            r = 0.0F; g = x; b = c;
        } else if (h < 300.0F) {
            r = x; g = 0.0F; b = c;
        } else {
            r = c; g = 0.0F; b = x;
        }
        return new float[]{r + m, g + m, b + m};
    }
}