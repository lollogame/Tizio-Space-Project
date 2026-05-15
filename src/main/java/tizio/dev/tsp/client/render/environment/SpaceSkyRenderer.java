package tizio.dev.tsp.client.render.environment;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import tizio.dev.engine.utils.Materials;
import tizio.dev.tsp.utils.Utils;


import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import static org.lwjgl.opengl.GL13.*;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class SpaceSkyRenderer {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final List<CometData> activeComets = new ArrayList<>();
    private static long lastCometSpawnTime = 0L;
    private static final RandomSource cometRandom = RandomSource.create();
    private static final int MAX_COMETS = 3;

    private static List<Predicate<Object[]>> customSky = null;
    private static Class<?> effects = null;
    private static int ticks = 0;
    private static float partialTick = 0.0F;
    private static PoseStack poseStack = null;
    private static Matrix4f projectionMatrix = null;
    private static Runnable setupFog = null;
    private static VertexBuffer abyssBuffer = null;
    private static VertexBuffer deepSkyBuffer = null;
    private static VertexBuffer skyboxBuffer = null;
    public static VertexBuffer lineBuffer = null;
    private static VertexBuffer starBuffer = null;
    private static int amount = 0;
    private static int seed = 0;

    private static final Predicate<Object[]> PREDICATE = params -> {
        boolean flag = false;
        ticks = (Integer) params[1];
        partialTick = (Float) params[2];
        poseStack = (PoseStack) params[3];
        projectionMatrix = (Matrix4f) params[5];
        setupFog = (Runnable) params[7];
        FogRenderer.levelFogColor();
        setupFog.run();
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.gameRenderer.getMainCamera().getEntity();

        if (entity != null) {
            ClientLevel level = minecraft.level;
            Vec3 pos = entity.getPosition(partialTick);
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            flag = run(null, level.dimension());
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        return flag;
    };

    private static class CometData {
        Vec3 startPos;
        Vec3 velocity;
        float age;
        float life;
        float size;
        int color;
        Deque<Vec3> history;
    }

    public static void renderComets(float yaw, float pitch, float roll, int minSpawn, int maxSpawn) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long gameTime = mc.level.getGameTime();

        if (activeComets.size() < MAX_COMETS && gameTime - lastCometSpawnTime > minSpawn + cometRandom.nextInt(maxSpawn)) {
            lastCometSpawnTime = gameTime;

            CometData c = new CometData();

            double theta = cometRandom.nextDouble() * Math.PI * 2.0;
            double phi = Math.acos(2.0 * cometRandom.nextDouble() - 1.0);
            Vec3 dir = new Vec3(
                    Math.sin(phi) * Math.cos(theta),
                    Math.cos(phi),
                    Math.sin(phi) * Math.sin(theta)
            );

            double spawnDist = 90.0 + cometRandom.nextDouble() * 40.0;
            c.startPos = dir.scale(spawnDist);

            Vec3 upVec = new Vec3(0, 1, 0);
            if (Math.abs(dir.y) > 0.9) {
                upVec = new Vec3(0, 0, 1);
            }

            Vec3 tangentA = dir.cross(upVec).normalize();
            Vec3 tangentB = dir.cross(tangentA).normalize();

            double vx = (cometRandom.nextDouble() * 2.0 - 1.0);
            double vy = (cometRandom.nextDouble() * 2.0 - 1.0);
            Vec3 vel = tangentA.scale(vx).add(tangentB.scale(vy)).normalize();

            double speed = 0.3 + cometRandom.nextDouble() * 0.4;
            c.velocity = vel.scale(speed);

            c.age = 0f;
            c.life = 200 + cometRandom.nextInt(300);
            c.size = 0.5F + cometRandom.nextFloat() * 0.8F;
            c.color = 0xFFFFFFFF;
            c.history = new ArrayDeque<>();
            for (int i = 0; i < 6; i++) c.history.addLast(c.startPos);

            activeComets.add(c);
        }

        activeComets.removeIf(c -> c.age > c.life);

        if (activeComets.isEmpty()) return;

        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (CometData comet : activeComets) {
            comet.age += 1.0F;
            float interpAge = comet.age - 1.0F + partialTick;
            Vec3 interpPos = comet.startPos.add(comet.velocity.scale(interpAge));

            comet.history.addLast(interpPos);
            if (comet.history.size() > 60) comet.history.removeFirst();

            float lifeProgress = comet.age / comet.life;
            float baseAlphaF = Mth.clamp(1.0F - lifeProgress, 0.0F, 1.0F);
            int baseAlpha = (int) (baseAlphaF * 255.0F);

            poseStack.pushPose();
            poseStack.mulPose(Axis.YN.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.mulPose(Axis.ZN.rotationDegrees(roll));

            addCometQuad(buffer, interpPos, comet.size, comet.color, baseAlpha, poseStack);

            final int TAIL_SEGMENTS = 15;
            final float SEGMENT_GAP = 1.5f;

            Vec3 velNorm = comet.velocity.normalize();

            for (int s = 1; s <= TAIL_SEGMENTS; s++) {
                float t = s / (float) TAIL_SEGMENTS;
                Vec3 segPos = interpPos.add(velNorm.scale(-s * SEGMENT_GAP));

                float segSize = comet.size * (0.6F + (1.0F - t) * 2.5F);

                float fade = (float) Math.pow(1.0F - t, 1.4);
                int segAlpha = (int) (baseAlphaF * fade * 255.0F);
                segAlpha = Mth.clamp(segAlpha, 0, 255);

                addCometQuad(buffer, segPos, segSize, comet.color, segAlpha, poseStack);
            }

            poseStack.popPose();
        }

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private static void addCometQuad(BufferBuilder buffer, Vec3 pos, float size, int color, int alpha, PoseStack poseStack) {
        Matrix4f mat = poseStack.last().pose();

        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = alpha / 255.0F;

        float half = size * 0.5F;

        float ang = (float) ((pos.x + pos.y + pos.z) % (Math.PI * 2));
        float sa = Mth.sin(ang);
        float ca = Mth.cos(ang);

        buffer.vertex(mat, (float) (pos.x - half * ca), (float) (pos.y - half * sa), (float) pos.z).color(r, g, b, a).endVertex();
        buffer.vertex(mat, (float) (pos.x + half * ca), (float) (pos.y - half * sa), (float) pos.z).color(r, g, b, a).endVertex();
        buffer.vertex(mat, (float) (pos.x + half * ca), (float) (pos.y + half * sa), (float) pos.z).color(r, g, b, a).endVertex();
        buffer.vertex(mat, (float) (pos.x - half * ca), (float) (pos.y + half * sa), (float) pos.z).color(r, g, b, a).endVertex();
    }

    public static void renderAbyss(int color, boolean constant) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean visible = minecraft.player.getEyePosition(partialTick).y() - minecraft.level.getLevelData().getHorizonHeight(minecraft.level) < 0.0D;
        if (visible || constant) {
            if (abyssBuffer == null) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.setShader(GameRenderer::getPositionShader);
                BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
                bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
                bufferBuilder.vertex(0.0F, -16.0F, 0.0F).endVertex();
                for (int i = 0; i <= 8; ++i) {
                    bufferBuilder.vertex(-512.0F * Mth.cos(i * 45.0F * Mth.DEG_TO_RAD), -16.0F, 512.0F * Mth.sin(i * 45.0F * Mth.DEG_TO_RAD)).endVertex();
                }
                abyssBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                abyssBuffer.bind();
                abyssBuffer.upload(bufferBuilder.end());
            } else {
                abyssBuffer.bind();
            }
            poseStack.pushPose();
            poseStack.translate(0.0F, 12.0F, 0.0F);
            RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >>> 24) / 255.0F);
            abyssBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }

    public static void renderDeepSky(int color) {
        if (deepSkyBuffer == null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
            bufferBuilder.vertex(0.0F, 16.0F, 0.0F).color(color).endVertex();
            for (int i = 0; i <= 8; ++i) {
                bufferBuilder.vertex(512.0F * Mth.cos(45.0F * i * Mth.DEG_TO_RAD), 16.0F, 512.0F * Mth.sin(45.0F * i * Mth.DEG_TO_RAD)).endVertex();
            }
            deepSkyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            deepSkyBuffer.bind();
            deepSkyBuffer.upload(bufferBuilder.end());
        } else {
            deepSkyBuffer.bind();
        }
        RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >>> 24) / 255.0F);
        deepSkyBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void renderEndSky(float yaw, float pitch, float roll, int color, boolean constant) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 pos = minecraft.gameRenderer.getMainCamera().getPosition();
        boolean invisible = minecraft.level.effects().isFoggyAt(Mth.floor(pos.x()), Mth.floor(pos.y())) || minecraft.gui.getBossOverlay().shouldCreateWorldFog();
        if (!invisible || constant) {
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(yaw));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
            poseStack.mulPose(com.mojang.math.Axis.ZN.rotationDegrees(roll));
            Matrix4f matrix4f = poseStack.last().pose();
            RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >>> 24) / 255.0F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            for (int i = 0; i < 6; ++i) {
                switch (i) {
                    case 0:
                        bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).uv(0.0F, 0.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).uv(0.0F, 16.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).uv(16.0F, 16.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).uv(16.0F, 0.0F).endVertex();
                        break;
                    case 1:
                        bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).uv(0.0F, 0.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, -100.0F, 100.0F, 100.0F).uv(0.0F, 16.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, 100.0F, 100.0F, 100.0F).uv(16.0F, 16.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).uv(16.0F, 0.0F).endVertex();
                        break;
                    case 2:
                        bufferBuilder.vertex(matrix4f, -100.0F, 100.0F, -100.0F).uv(0.0F, 0.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).uv(0.0F, 16.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).uv(16.0F, 16.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, 100.0F, 100.0F, -100.0F).uv(16.0F, 0.0F).endVertex();
                        break;
                    case 3:
                        bufferBuilder.vertex(matrix4f, -100.0F, 100.0F, 100.0F).uv(0.0F, 0.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, -100.0F, 100.0F, -100.0F).uv(0.0F, 16.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, 100.0F, 100.0F, -100.0F).uv(16.0F, 16.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, 100.0F, 100.0F, 100.0F).uv(16.0F, 0.0F).endVertex();
                        break;
                    case 4:
                        bufferBuilder.vertex(matrix4f, -100.0F, 100.0F, -100.0F).uv(0.0F, 0.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, -100.0F, 100.0F, 100.0F).uv(0.0F, 16.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).uv(16.0F, 16.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).uv(16.0F, 0.0F).endVertex();
                        break;
                    case 5:
                        bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).uv(0.0F, 0.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).uv(0.0F, 16.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, 100.0F, 100.0F, 100.0F).uv(16.0F, 16.0F).endVertex();
                        bufferBuilder.vertex(matrix4f, 100.0F, 100.0F, -100.0F).uv(16.0F, 0.0F).endVertex();
                        break;
                }
            }
            BufferUploader.drawWithShader(bufferBuilder.end());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }

    public static void renderMoon(float size, int color, boolean phase, boolean constant) {
        ClientLevel level = Minecraft.getInstance().level;
        float r = size / 2.0F;
        float u0 = 0.0F;
        float v0 = 0.0F;
        float u1 = 1.0F;
        float v1 = 1.0F;
        if (phase) {
            int i0 = level.getMoonPhase();
            int i1 = i0 & 3;
            int i2 = (i0 >> 2) & 1;
            u0 = i1 / 4.0F;
            v0 = i2 / 2.0F;
            u1 = (i1 + 1) / 4.0F;
            v1 = (i2 + 1) / 2.0F;
        }
        float alpha = (color >>> 24) / 255.0F;
        if (!constant)
            alpha *= (1.0F - level.getRainLevel(partialTick));
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));
        Matrix4f matrix4f = poseStack.last().pose();
        RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, -r, -100.0F, -r).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrix4f, -r, -100.0F, r).uv(u0, v1).endVertex();
        bufferBuilder.vertex(matrix4f, r, -100.0F, r).uv(u0, v0).endVertex();
        bufferBuilder.vertex(matrix4f, r, -100.0F, -r).uv(u1, v0).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    public static void renderSky(boolean deepSky, boolean sunlights, boolean sun, boolean moon, boolean stars, boolean abyss) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (deepSky) {
            Vec3 color = level.getSkyColor(minecraft.gameRenderer.getMainCamera().getPosition(), partialTick);
            RenderSystem.defaultBlendFunc();
            renderDeepSky(255 << 24 | (int) (color.x() * 255.0D) << 16 | (int) (color.y() * 255.0D) << 8 | (int) (color.z() * 255.0D));
        }
        if (sunlights) {
            float[] color = level.effects().getSunriseColor(level.getTimeOfDay(partialTick), partialTick);
            if (color != null) {
                RenderSystem.defaultBlendFunc();
                renderSunlights((int) (color[3] * 255.0F) << 24 | (int) (color[0] * 255.0F) << 16 | (int) (color[1] * 255.0F) << 8 | (int) (color[2] * 255.0F));
            }
        }
        if (sun) {
            RenderSystem.setShaderTexture(0, new ResourceLocation("minecraft:textures/environment/sun.png"));
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            renderSun(60.0F, 255 << 24 | 255 << 16 | 255 << 8 | 255, false);
        }
        if (moon) {
            RenderSystem.setShaderTexture(0, new ResourceLocation("minecraft:textures/environment/moon_phases.png"));
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            renderMoon(40.0F, 255 << 24 | 255 << 16 | 255 << 8 | 255, true, false);
        }
        if (stars) {
            int color = (int) (level.getStarBrightness(partialTick) * 255.0F);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            renderStars(5500, 842, 90.0F, level.getTimeOfDay(partialTick) * 360.0F, 0.0F, color << 24 | color << 16 | color << 8 | color, false);
        }
        if (abyss) {
            RenderSystem.defaultBlendFunc();
            renderAbyss(255 << 24 | 0 << 16 | 0 << 8 | 0, false);
        }
    }

    public static void renderSkybox(float yaw, float pitch, float roll, int color, boolean constant) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 pos = minecraft.gameRenderer.getMainCamera().getPosition();
        boolean invisible = minecraft.level.effects().isFoggyAt(Mth.floor(pos.x()), Mth.floor(pos.y())) || minecraft.gui.getBossOverlay().shouldCreateWorldFog();
        if (!invisible || constant) {
            if (skyboxBuffer == null) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                for (int i = 0; i < 6; ++i) {
                    switch (i) {
                        case 0:
                            bufferBuilder.vertex(-0.5F, -0.5F, -0.5F).uv(0.0F, 0.0F).endVertex();
                            bufferBuilder.vertex(-0.5F, -0.5F, 0.5F).uv(0.0F, 0.5F).endVertex();
                            bufferBuilder.vertex(0.5F, -0.5F, 0.5F).uv(1.0F / 3.0F, 0.5F).endVertex();
                            bufferBuilder.vertex(0.5F, -0.5F, -0.5F).uv(1.0F / 3.0F, 0.0F).endVertex();
                            break;
                        case 1:
                            bufferBuilder.vertex(-0.5F, 0.5F, 0.5F).uv(1.0F / 3.0F, 0.0F).endVertex();
                            bufferBuilder.vertex(-0.5F, 0.5F, -0.5F).uv(1.0F / 3.0F, 0.5F).endVertex();
                            bufferBuilder.vertex(0.5F, 0.5F, -0.5F).uv(2.0F / 3.0F, 0.5F).endVertex();
                            bufferBuilder.vertex(0.5F, 0.5F, 0.5F).uv(2.0F / 3.0F, 0.0F).endVertex();
                            break;
                        case 2:
                            bufferBuilder.vertex(0.5F, 0.5F, 0.5F).uv(2.0F / 3.0F, 0.0F).endVertex();
                            bufferBuilder.vertex(0.5F, -0.5F, 0.5F).uv(2.0F / 3.0F, 0.5F).endVertex();
                            bufferBuilder.vertex(-0.5F, -0.5F, 0.5F).uv(1.0F, 0.5F).endVertex();
                            bufferBuilder.vertex(-0.5F, 0.5F, 0.5F).uv(1.0F, 0.0F).endVertex();
                            break;
                        case 3:
                            bufferBuilder.vertex(-0.5F, 0.5F, 0.5F).uv(0.0F, 0.5F).endVertex();
                            bufferBuilder.vertex(-0.5F, -0.5F, 0.5F).uv(0.0F, 1.0F).endVertex();
                            bufferBuilder.vertex(-0.5F, -0.5F, -0.5F).uv(1.0F / 3.0F, 1.0F).endVertex();
                            bufferBuilder.vertex(-0.5F, 0.5F, -0.5F).uv(1.0F / 3.0F, 0.5F).endVertex();
                            break;
                        case 4:
                            bufferBuilder.vertex(-0.5F, 0.5F, -0.5F).uv(1.0F / 3.0F, 0.5F).endVertex();
                            bufferBuilder.vertex(-0.5F, -0.5F, -0.5F).uv(1.0F / 3.0F, 1.0F).endVertex();
                            bufferBuilder.vertex(0.5F, -0.5F, -0.5F).uv(2.0F / 3.0F, 1.0F).endVertex();
                            bufferBuilder.vertex(0.5F, 0.5F, -0.5F).uv(2.0F / 3.0F, 0.5F).endVertex();
                            break;
                        case 5:
                            bufferBuilder.vertex(0.5F, 0.5F, -0.5F).uv(2.0F / 3.0F, 0.5F).endVertex();
                            bufferBuilder.vertex(0.5F, -0.5F, -0.5F).uv(2.0F / 3.0F, 1.0F).endVertex();
                            bufferBuilder.vertex(0.5F, -0.5F, 0.5F).uv(1.0F, 1.0F).endVertex();
                            bufferBuilder.vertex(0.5F, 0.5F, 0.5F).uv(1.0F, 0.5F).endVertex();
                            break;
                    }
                }
                skyboxBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                skyboxBuffer.bind();
                setTextureFilter();
                skyboxBuffer.upload(bufferBuilder.end());
            } else {
                skyboxBuffer.bind();
                setTextureFilter();
            }
            float size = minecraft.options.getEffectiveRenderDistance() << 6;
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(yaw));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
            poseStack.mulPose(com.mojang.math.Axis.ZN.rotationDegrees(roll));
            poseStack.scale(size, size, size);
            RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >>> 24) / 255.0F);
            skyboxBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, GameRenderer.getPositionTexShader());
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }

    private static void setTextureFilter() {
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
    }

    public static void renderStars(int amount, int seed, float yaw, float pitch, float roll, int color, boolean constant) {
        if (starBuffer == null || amount != SpaceSkyRenderer.amount || seed != SpaceSkyRenderer.seed) {
            SpaceSkyRenderer.amount = amount;
            SpaceSkyRenderer.seed = seed;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            RandomSource randomsource = RandomSource.create(seed);
            for (int i = 0; i < amount; ++i) {
                float f0 = randomsource.nextFloat() * 2.0F - 1.0F;
                float f1 = randomsource.nextFloat() * 2.0F - 1.0F;
                float f2 = randomsource.nextFloat() * 2.0F - 1.0F;
                float f3 = 0.15F + 0.1F * randomsource.nextFloat();
                float f4 = f0 * f0 + f1 * f1 + f2 * f2;
                if (f4 < 1.0F && f4 > 0.01F) {
                    f4 = 1.0F / Mth.sqrt(f4);
                    f0 *= f4;
                    f1 *= f4;
                    f2 *= f4;
                    float f5 = f0 * 200.0F;
                    float f6 = f1 * 200.0F;
                    float f7 = f2 * 200.0F;
                    float f8 = (float) Math.atan2(f0, f2);
                    float f9 = Mth.sin(f8);
                    float f10 = Mth.cos(f8);
                    float f11 = (float) Math.atan2(Mth.sqrt(f0 * f0 + f2 * f2), f1);
                    float f12 = Mth.sin(f11);
                    float f13 = Mth.cos(f11);
                    float f14 = (float) randomsource.nextDouble() * Mth.PI * 2.0F;
                    float f15 = Mth.sin(f14);
                    float f16 = Mth.cos(f14);
                    for (int j = 0; j < 4; ++j) {
                        float f17 = ((j & 2) - 1) * f3;
                        float f18 = ((j + 1 & 2) - 1) * f3;
                        float f20 = f17 * f16 - f18 * f15;
                        float f21 = f18 * f16 + f17 * f15;
                        float f22 = -f20 * f13;
                        float f23 = f22 * f9 - f21 * f10;
                        float f24 = f20 * f12;
                        float f25 = f21 * f9 + f22 * f10;
                        bufferBuilder.vertex(f5 + f23, f6 + f24, f7 + f25).endVertex();
                    }
                }
            }
            if (starBuffer != null) starBuffer.close();
            starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            starBuffer.bind();
            starBuffer.upload(bufferBuilder.end());
        } else {
            starBuffer.bind();
        }
        float alpha = (color >>> 24) / 255.0F;
        if (!constant)
            alpha *= (1.0F - Minecraft.getInstance().level.getRainLevel(partialTick));
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZN.rotationDegrees(roll));
        FogRenderer.setupNoFog();
        RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, alpha);
        starBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        setupFog.run();
        poseStack.popPose();
    }

    public static void renderSun(float size, int color, boolean constant) {
        ClientLevel level = Minecraft.getInstance().level;
        float r = size / 2.0F;
        float alpha = (color >>> 24) / 255.0F;
        if (!constant)
            alpha *= (1.0F - level.getRainLevel(partialTick));
        poseStack.pushPose();
        setTextureFilter();
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));
        Matrix4f matrix4f = poseStack.last().pose();
        RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, r, 100.0F, -r).uv(0.0F, 0.0F).endVertex();
        bufferBuilder.vertex(matrix4f, r, 100.0F, r).uv(1.0F, 0.0F).endVertex();
        bufferBuilder.vertex(matrix4f, -r, 100.0F, r).uv(1.0F, 1.0F).endVertex();
        bufferBuilder.vertex(matrix4f, -r, 100.0F, -r).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    public static void renderSun(float size, float angle, int color) {
        ClientLevel level = Minecraft.getInstance().level;
        float r = size / 2.0F;
        float alpha = (color >>> 24) / 255.0F;
        alpha *= (1.0F - level.getRainLevel(partialTick));
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
        Matrix4f matrix4f = poseStack.last().pose();
        RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, r, 100.0F, -r).uv(0.0F, 0.0F).endVertex();
        bufferBuilder.vertex(matrix4f, r, 100.0F, r).uv(1.0F, 0.0F).endVertex();
        bufferBuilder.vertex(matrix4f, -r, 100.0F, r).uv(1.0F, 1.0F).endVertex();
        bufferBuilder.vertex(matrix4f, -r, 100.0F, -r).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    public static void renderSunlights(int color) {
        ClientLevel level = Minecraft.getInstance().level;
        float[] rawColor = level.effects().getSunriseColor(level.getTimeOfDay(partialTick), partialTick);
        if (rawColor != null) {
            int red = color >> 16 & 255;
            int green = color >> 8 & 255;
            int blue = color & 255;
            int alpha = (int) ((color >>> 24) * rawColor[3]);
            Matrix4f matrix4f = poseStack.last().pose();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            boolean flag = Mth.sin(level.getSunAngle(partialTick)) < 0.0F;
            if (flag) {
                bufferBuilder.vertex(matrix4f, 100.0F, 0.0F, 0.0F).color(red, green, blue, alpha).endVertex();
            } else {
                bufferBuilder.vertex(matrix4f, -100.0F, 0.0F, 0.0F).color(red, green, blue, alpha).endVertex();
            }
            for (int i = 0; i <= 16; ++i) {
                float deg = i * Mth.TWO_PI / 16.0F;
                float sin = Mth.sin(deg);
                float cos = Mth.cos(deg);
                if (flag) {
                    bufferBuilder.vertex(matrix4f, cos * 120.0F, cos * 40.0F * rawColor[3], -sin * 120.0F).color(red, green, blue, 0).endVertex();
                } else {
                    bufferBuilder.vertex(matrix4f, -cos * 120.0F, cos * 40.0F * rawColor[3], sin * 120.0F).color(red, green, blue, 0).endVertex();
                }
            }
            BufferUploader.drawWithShader(bufferBuilder.end());
        }
    }

    public static void renderTexture(float size, float yaw, float pitch, float roll, int color, boolean constant) {
        float r = size / 2.0F;
        float alpha = (color >>> 24) / 255.0F;
        if (!constant)
            alpha *= (1.0F - Minecraft.getInstance().level.getRainLevel(partialTick));
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(yaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(com.mojang.math.Axis.ZN.rotationDegrees(roll));
        Matrix4f matrix4f = poseStack.last().pose();
        RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, r, r, 100.0F).uv(0.0F, 0.0F).endVertex();
        bufferBuilder.vertex(matrix4f, r, -r, 100.0F).uv(0.0F, 1.0F).endVertex();
        bufferBuilder.vertex(matrix4f, -r, -r, 100.0F).uv(1.0F, 1.0F).endVertex();
        bufferBuilder.vertex(matrix4f, -r, r, 100.0F).uv(1.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void renderSky(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        try {
            Class<?> effects = Minecraft.getInstance().level.effects().getClass().getSuperclass();
            if (!effects.getName().contains("TspDimensionEffects")) return;
            if (effects != SpaceSkyRenderer.effects) {
                SpaceSkyRenderer.effects = effects;
                SpaceSkyRenderer.customSky = (List<Predicate<Object[]>>) effects.getField("CUSTOM_SKY").get(null);
            }
            SpaceSkyRenderer.customSky.add(PREDICATE);
        } catch (Exception e) {
        }
    }

    private static boolean run(@Nullable Event event, ResourceKey<Level> dimension) {
        ClientLevel level = Minecraft.getInstance().level;
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        final Vec3 skyCol = new Vec3(20, 20, 20);

        if (dimension == null && camera != null) return false;

        if (Utils.inDimension("tsp:space")) {
            renderSky(true, true, false, false, true, true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, Materials.SPACE_SKYBOX);
            renderSkybox(0, 24, 12, 255 << 24 | (int) skyCol.z << 16 | (int) skyCol.y << 8 | (int) skyCol.x, true);
        }

        if (Utils.inDimension("tsp:moon")) {
            renderSky(false, false, false, false, false, false);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, Materials.SPACE_SKYBOX);
            renderSkybox(273, 121, 328, 255 << 24 | (int) skyCol.z << 16 | (int) skyCol.y << 8 | (int) skyCol.x, false);
        }

        if (Utils.inDimension("tsp:mars")) {
            renderSky(false, true, false, false, false, false);
            RenderSystem.defaultBlendFunc();
            float dayTime = level.getTimeOfDay(partialTick);
            float star = level.getStarBrightness(partialTick);
            float nightBlend = star;
            float dayBlend = 1.0f - nightBlend;
            RenderSystem.setShaderTexture(0, Materials.SPACE_SKYBOX);
            int nightAlpha = ((int) (nightBlend * 255.0f) << 24) | 0x00FFFFFF;
            renderSkybox(40.30f, 0, (-dayTime * 360.0F) + 320, nightAlpha, true);
        }

        if (Utils.inDimension("minecraft:overworld")) {
            renderSky(false, true, false, false, true, false);
            RenderSystem.defaultBlendFunc();
            float partial = partialTick;
            double playerY = mc.player.getY();
            double fadeStart = 100.0;
            double fadeEnd = 180;
            float heightFactor = 0.0F;
            if (playerY > fadeStart) {
                heightFactor = (float) ((playerY - fadeStart) / (fadeEnd - fadeStart));
                heightFactor = Mth.clamp(heightFactor, 0.0F, 1.0F);
            }
            float timeOfDay = level.getTimeOfDay(partial);
            float starBrightness = level.getStarBrightness(partial);
            float nightBlend = starBrightness;
            float dayBlend = 1.0F - nightBlend;
            float dayVisibility = dayBlend * (1.0F - heightFactor);
            float spaceVisibility = Math.max(starBrightness, heightFactor);
            float skyRotation = -timeOfDay * 360.0F;
            RenderSystem.setShaderTexture(0, Materials.NULL);
            int dayAlpha = ((int) (dayVisibility * 255.0F) << 24) | 0x00FFFFFF;
            if (dayVisibility > 0.0F) {
                renderSkybox(0.0F, 0.0F, 0.0F, dayAlpha, true);
            }
            if (spaceVisibility > 0.01F) {
                RenderSystem.setShaderTexture(0, Materials.SPACE_SKYBOX);
                int spaceAlpha = ((int) (spaceVisibility * 255.0F) << 24) | 0x00FFFFFF;
                renderSkybox(40.30F, 0.0F, skyRotation + 320.0F, spaceAlpha, true);
            }
        }

        return true;
    }
}