package tizio.dev.tsp.core.celestial.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.gui.SystemEditor;

import java.time.Instant;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MainClass.MODID, value = Dist.CLIENT)
public final class EditorOrbitRenderer {

    private static final ResourceLocation SPACE_DIMENSION = new ResourceLocation(MainClass.MODID, "space");
    private static final String SPACE_DIMENSION_ID = "tsp:space";

    private static final int SEGMENTS = 120;
    private static final float[] DEFAULT_COLOR = {0.6F, 0.85F, 1.0F};

    private static boolean editorSessionOpen = false;

    private EditorOrbitRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        if (mc.screen instanceof SystemEditor) {
            editorSessionOpen = true;
        } else if (mc.screen == null) {
            editorSessionOpen = false;
        }

        if (!editorSessionOpen) {
            return;
        }

        if (!SPACE_DIMENSION.equals(mc.level.dimension().location())) {
            return;
        }

        Map<String, SolarSystemData> systems = CelestialJsonLoader.getActiveSystems();
        if (systems == null || systems.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableCull();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.lineWidth(2.5F);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (SolarSystemData system : systems.values()) {
            if (!SPACE_DIMENSION_ID.equals(system.dimension)) {
                continue;
            }
            renderSystemOrbits(system, now, matrix, buffer);
        }

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.disableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0F);

        poseStack.popPose();
    }

    private static void renderSystemOrbits(SolarSystemData system, Instant now, Matrix4f matrix, BufferBuilder buffer) {
        if (system.bodies == null || system.bodies.isEmpty()) {
            return;
        }

        float globalScale = Math.max(0.0001F, system.globalScale <= 0.0F ? 1.0F : system.globalScale);
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

            Vec3 previous = orbitOffset(radius, 0.0D, inclinationDeg, ascendingNodeDeg, verticalOffset).add(parentPos);

            for (int i = 1; i <= SEGMENTS; i++) {
                double angleDeg = (360.0D * i) / SEGMENTS;
                Vec3 current = orbitOffset(radius, angleDeg, inclinationDeg, ascendingNodeDeg, verticalOffset).add(parentPos);

                buffer.vertex(matrix, (float) previous.x, (float) previous.y, (float) previous.z)
                        .color(rgb[0], rgb[1], rgb[2], alpha)
                        .endVertex();

                buffer.vertex(matrix, (float) current.x, (float) current.y, (float) current.z)
                        .color(rgb[0], rgb[1], rgb[2], alpha)
                        .endVertex();

                previous = current;
            }
        }
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