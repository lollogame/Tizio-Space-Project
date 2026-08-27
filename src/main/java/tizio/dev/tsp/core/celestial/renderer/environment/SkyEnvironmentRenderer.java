package tizio.dev.tsp.core.celestial.renderer.environment;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
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
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.client.ClientShaderRegistry;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.handlers.teleport.SpaceTransitionHandler;
import tizio.dev.tsp.core.utils.Color;
import tizio.dev.tsp.core.utils.Materials;
import tizio.dev.tsp.core.utils.volume.VolumeRenderUtil;

import static org.lwjgl.opengl.GL13.*;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class SkyEnvironmentRenderer {

    public static final Vector3f SKYBOX_ROTATION = new Vector3f(-45.0F, 10.0F, 24.0F);
    public static final Vector3f MAX_SKYBOX_BRIGHTNESS = new Vector3f(200.0F, 200.0F, 200.0F);

    private SkyEnvironmentRenderer() {}

    private static VertexBuffer milkyWayBuffer;

    private static final java.util.function.Predicate<Object[]> SKY_PREDICATE = args -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) return false;

        if (mc.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) {
            return false;
        }

        SkyRenderContext.ticks          = (Integer)   args[1];
        SkyRenderContext.partialTick    = (Float)     args[2];
        SkyRenderContext.poseStack      = (PoseStack) args[3];
        SkyRenderContext.projectionMatrix = (Matrix4f) args[5];
        SkyRenderContext.setupFog       = (Runnable)  args[7];

        FogRenderer.levelFogColor();
        SkyRenderContext.setupFog.run();

        Entity entity = mc.gameRenderer.getMainCamera().getEntity();

        boolean rendered = false;
        if (entity != null) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            rendered = renderForDimension(mc.level.dimension());
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        return rendered;
    };

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.level.effects() == null) return;

            Class<?> effectsClass = mc.level.effects().getClass().getSuperclass();
            if (!effectsClass.getName().contains("TspDimensionEffects")) return;

            if (effectsClass != SkyRenderContext.effectsClass) {
                SkyRenderContext.effectsClass = effectsClass;
                SkyRenderContext.customSky = (java.util.List<java.util.function.Predicate<Object[]>>) effectsClass.getField("CUSTOM_SKY").get(null);
            }

            if (SkyRenderContext.customSky != null && !SkyRenderContext.customSky.contains(SKY_PREDICATE)) {
                SkyRenderContext.customSky.add(SKY_PREDICATE);
            }
        } catch (Exception ignored) {
        }
    }

    private static PlanetInstance.Config resolveBodyForDimension(ResourceKey<Level> dimension) {
        String dimKey = dimension.location().toString();
        for (SolarSystemData system : CelestialJsonLoader.getActiveSystems().values()) {
            for (PlanetInstance.Config body : system.bodies) {
                if (dimKey.equals(body.dimension)) {
                    return body;
                }
            }
        }
        return null;
    }

    private static SolarSystemData resolveSystemForDimension(ResourceKey<Level> dimension) {
        String dimKey = dimension.location().toString();
        for (SolarSystemData system : CelestialJsonLoader.getActiveSystems().values()) {
            if (dimKey.equals(system.dimension)) {
                return system;
            }
        }
        return null;
    }

    private static void renderGroundSky(PlanetInstance.Config body, Vec3 darkSkyTint, float minY, float maxY) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;

        float pt = SkyRenderContext.partialTick;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y();
        float heightFade = Mth.clamp((float) ((camY - minY) / (maxY - minY)), 0.0F, 1.0F);
        float starBrightness = level.getStarBrightness(pt);
        float nightFade = Mth.clamp(starBrightness * 1.8F, 0.0F, 1.0F);

        if (body.atmosphere != null && body.atmosphere.enabled) {

            Vector3f wavelengths = new Vector3f(
                    body.atmosphere.wavelengthR,
                    body.atmosphere.wavelengthG,
                    body.atmosphere.wavelengthB
            );

            renderAtmosphereSkybox(Color.iRGBA(255, 255, 255, 200), wavelengths);
        }

        if (body.sky != null && body.sky.skyboxConstant) {
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, Materials.resolveSkyTextureLocation(body.sky.skyboxTexture));

            float rotation = body.sky.skyboxRotation ? (-level.getTimeOfDay(pt) * 360.0F) + 320.0F : 0.0F;

            renderSkybox(40.30F, 0.0F, rotation, Color.packColor(255, 255, 255, 255), true);

        } else if (body.sky != null) {
            float totalFade = Math.max(heightFade, nightFade);

            if (totalFade > 0.0F) {
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderTexture(0, Materials.resolveSkyTextureLocation(body.sky.skyboxTexture));

                float rotation = body.sky.skyboxRotation ? (-level.getTimeOfDay(pt) * 360.0F) + 320.0F : 0.0F;
                int alpha = Mth.clamp((int) (totalFade * 255.0F), 0, 255);

                renderSkybox(40.30F, 0.0F, rotation, Color.packColor(alpha, 255, 255, 255), true);
            }
        }

        if (body.sky != null && body.sky.starsEnabled) {

            float totalFade = body.sky.skyboxConstant ? 1.0F : Math.max(heightFade, nightFade);
            if (totalFade > 0.0F) {

                int alpha = Mth.clamp((int) (totalFade * 255.0F), 0, 255);
                Vector3f starRgb = CelestialJsonLoader.parseColor(body.sky.starsColorHex, new Vector3f(1.0F, 1.0F, 1.0F));

                int r = (int) (starRgb.x * 255.0F);
                int g = (int) (starRgb.y * 255.0F);
                int b = (int) (starRgb.z * 255.0F);

                float rotation = body.sky.skyboxRotation ? (-level.getTimeOfDay(pt) * 360.0F) + 320.0F : 0.0F;
                renderStars(body.sky.starsAmount, body.sky.starsSeed, 40.30F, 0.0F, rotation, Color.packColor(alpha, r, g, b), false);
            }
        }
    }

    private static void renderPlanetSky(PlanetInstance.Config body, Vec3 darkSkyTint, float minY, float maxY) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        float pt = SkyRenderContext.partialTick;

        double camY = mc.gameRenderer.getMainCamera().getPosition().y();
        float heightFade = Mth.clamp((float) ((camY - minY) / (maxY - minY)), 0.0f, 1.0f);
        float rawStarBrightness = level.getStarBrightness(pt);
        float nightFade = Mth.clamp(rawStarBrightness * 1.8f, 0.0f, 1.0f);
        float totalFade = Math.max(heightFade, nightFade);

        if (body.atmosphere != null && body.atmosphere.enabled) {
            Vector3f wavelengths = new Vector3f(body.atmosphere.wavelengthR, body.atmosphere.wavelengthG, body.atmosphere.wavelengthB);
            renderAtmosphereSkybox(Color.iRGBA(255, 255, 255, 200), wavelengths);
        }

        if (body.sky != null && body.sky.skyboxConstant) {
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, Materials.resolveSkyTextureLocation(body.sky.skyboxTexture));

            float rotation = body.sky.skyboxRotation ? (-level.getTimeOfDay(pt) * 360.0F) + 320.0F : 0.0F;
            renderSkybox(20.30F, 0.0f, rotation, Color.packColor(255, (int) darkSkyTint.z, (int) darkSkyTint.y, (int) darkSkyTint.x), true);

        } else if (body.sky != null && totalFade > 0.0F) {
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, Materials.resolveSkyTextureLocation(body.sky.skyboxTexture));

            float rotation = body.sky.skyboxRotation ? (-level.getTimeOfDay(pt) * 360.0F) + 320.0F : 0.0F;
            int alpha = Mth.clamp((int) (totalFade * 255.0F), 0, 255);

            renderSkybox(20.30F, 0.0F, rotation, Color.packColor(alpha, 255, 255, 255), true);
        }

        if (body.sky != null && body.sky.starsEnabled) {

            float starsFade = body.sky.skyboxConstant ? 1.0F : totalFade;
            if (starsFade > 0.0F) {

                int alpha = Mth.clamp((int) (starsFade * 255.0F), 0, 255);
                Vector3f starRgb = CelestialJsonLoader.parseColor(body.sky.starsColorHex, new Vector3f(1.0F, 1.0F, 1.0F));

                int r = (int) (starRgb.x * 255.0F);
                int g = (int) (starRgb.y * 255.0F);
                int b = (int) (starRgb.z * 255.0F);

                float rotation = body.sky.skyboxRotation ? (-level.getTimeOfDay(pt) * 360.0F) + 320.0F : 0.0F;
                renderStars(body.sky.starsAmount, body.sky.starsSeed, 20.30F, 0F, rotation, Color.packColor(alpha, r, g, b), true);
            }
        }
    }

    private static void renderSystemOverviewSky(Vec3 darkSkyTint) {
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, Materials.SPACE_SKYBOX);
        renderSkybox(0, 24, 12, Color.packColor(255, (int) darkSkyTint.z, (int) darkSkyTint.y, (int) darkSkyTint.x), true);
    }

    private static boolean renderForDimension(ResourceKey<Level> dimension) {

        final Vec3 darkSkyTint = new Vec3(255, 255, 255);
        float minY = 320.0f;
        float maxY = SpaceTransitionHandler.ESCAPE_ALTITUDE;

        PlanetInstance.Config body = resolveBodyForDimension(dimension);

        if (body != null && body.sky != null && body.sky.groundMode) {

            renderSky(false, false, false, false, false, true);
            renderGroundSky(body, darkSkyTint, minY, maxY);

        } else if (body != null) {

            renderSky(false, false, false, false, false, false);
            renderPlanetSky(body, darkSkyTint, minY, maxY);

        } else {

            SolarSystemData system = resolveSystemForDimension(dimension);
            if (system != null) {
                renderSky(false, false, false, false, false, false);
                renderSystemOverviewSky(darkSkyTint);
            } else {
                renderSky(true, true, true, true, true, true);
            }
        }

        return true;
    }

    private static void renderSpaceSkyboxHeightFade(float yaw, float pitch, float roll, Vec3 darkSkyTint, float minY, float maxY) {
        double camY = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y();

        float fadeFactor = Mth.clamp((float) ((camY - minY) / (maxY - minY)), 0.0f, 1.0f);

        if (fadeFactor > 0.0f) {
            int alpha = (int) (fadeFactor * 255.0F);

            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, Materials.SPACE_SKYBOX);
            renderSkybox(yaw, pitch, roll, Color.packColor(alpha, (int) darkSkyTint.z, (int) darkSkyTint.y, (int) darkSkyTint.x), true);
        }
    }

    public static void renderSky(boolean deepSky, boolean sunlights, boolean sun, boolean moon, boolean stars, boolean abyss) {
        Minecraft mc     = Minecraft.getInstance();
        ClientLevel level = mc.level;
        float pt          = SkyRenderContext.partialTick;

        if (deepSky) {
            Vec3 skyColor = level.getSkyColor(mc.gameRenderer.getMainCamera().getPosition(), pt);
            RenderSystem.defaultBlendFunc();
            renderDeepSky(Color.packColor(255,
                    (int) (skyColor.x() * 255.0D),
                    (int) (skyColor.y() * 255.0D),
                    (int) (skyColor.z() * 255.0D)));
        }
        if (sunlights) {
            float[] color = level.effects().getSunriseColor(level.getTimeOfDay(pt), pt);
            if (color != null) {
                RenderSystem.defaultBlendFunc();
                renderSunlights(Color.packColor(
                        (int) (color[3] * 255.0F),
                        (int) (color[0] * 255.0F),
                        (int) (color[1] * 255.0F),
                        (int) (color[2] * 255.0F)));
            }
        }
        if (sun) {
            RenderSystem.setShaderTexture(0, new ResourceLocation("minecraft:textures/environment/sun.png"));
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ONE,       GlStateManager.DestFactor.ZERO);
            renderSun(60.0F, 0xFFFFFFFF, false);
        }
        if (moon) {
            RenderSystem.setShaderTexture(0, new ResourceLocation("minecraft:textures/environment/moon_phases.png"));
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ONE,       GlStateManager.DestFactor.ZERO);
            renderMoon(40.0F, 0xFFFFFFFF, true, false);
        }
        if (stars) {
            int brightness = (int) (level.getStarBrightness(pt) * 255.0F);
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ONE,       GlStateManager.DestFactor.ZERO);
            renderStars(5500, 842, 90.0F, level.getTimeOfDay(pt) * 360.0F, 0.0F,
                    Color.packColor(brightness, brightness, brightness, brightness), false);
        }
        if (abyss) {
            RenderSystem.defaultBlendFunc();
            renderAbyss(0xFF000000, false);
        }
    }

    public static void renderSkybox(float yaw, float pitch, float roll, int color, boolean constant) {
        Minecraft mc  = Minecraft.getInstance();
        Vec3 camPos   = mc.gameRenderer.getMainCamera().getPosition();
        boolean foggy = mc.level.effects().isFoggyAt(Mth.floor(camPos.x()), Mth.floor(camPos.y())) || mc.gui.getBossOverlay().shouldCreateWorldFog();
        if (foggy && !constant) return;

        if (SkyRenderContext.skyboxBuffer == null) {
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            // Bottom face
            bb.vertex(-0.5F, -0.5F, -0.5F).uv(0.0F,        0.0F).endVertex();
            bb.vertex(-0.5F, -0.5F,  0.5F).uv(0.0F,        0.5F).endVertex();
            bb.vertex( 0.5F, -0.5F,  0.5F).uv(1.0F / 3.0F, 0.5F).endVertex();
            bb.vertex( 0.5F, -0.5F, -0.5F).uv(1.0F / 3.0F, 0.0F).endVertex();
            // Top face
            bb.vertex(-0.5F,  0.5F,  0.5F).uv(1.0F / 3.0F, 0.0F).endVertex();
            bb.vertex(-0.5F,  0.5F, -0.5F).uv(1.0F / 3.0F, 0.5F).endVertex();
            bb.vertex( 0.5F,  0.5F, -0.5F).uv(2.0F / 3.0F, 0.5F).endVertex();
            bb.vertex( 0.5F,  0.5F,  0.5F).uv(2.0F / 3.0F, 0.0F).endVertex();
            // South face
            bb.vertex( 0.5F,  0.5F,  0.5F).uv(2.0F / 3.0F, 0.0F).endVertex();
            bb.vertex( 0.5F, -0.5F,  0.5F).uv(2.0F / 3.0F, 0.5F).endVertex();
            bb.vertex(-0.5F, -0.5F,  0.5F).uv(1.0F,        0.5F).endVertex();
            bb.vertex(-0.5F,  0.5F,  0.5F).uv(1.0F,        0.0F).endVertex();
            // West face
            bb.vertex(-0.5F,  0.5F,  0.5F).uv(0.0F,        0.5F).endVertex();
            bb.vertex(-0.5F, -0.5F,  0.5F).uv(0.0F,        1.0F).endVertex();
            bb.vertex(-0.5F, -0.5F, -0.5F).uv(1.0F / 3.0F, 1.0F).endVertex();
            bb.vertex(-0.5F,  0.5F, -0.5F).uv(1.0F / 3.0F, 0.5F).endVertex();
            // North face
            bb.vertex(-0.5F,  0.5F, -0.5F).uv(1.0F / 3.0F, 0.5F).endVertex();
            bb.vertex(-0.5F, -0.5F, -0.5F).uv(1.0F / 3.0F, 1.0F).endVertex();
            bb.vertex( 0.5F, -0.5F, -0.5F).uv(2.0F / 3.0F, 1.0F).endVertex();
            bb.vertex( 0.5F,  0.5F, -0.5F).uv(2.0F / 3.0F, 0.5F).endVertex();
            // East face
            bb.vertex( 0.5F,  0.5F, -0.5F).uv(2.0F / 3.0F, 0.5F).endVertex();
            bb.vertex( 0.5F, -0.5F, -0.5F).uv(2.0F / 3.0F, 1.0F).endVertex();
            bb.vertex( 0.5F, -0.5F,  0.5F).uv(1.0F,        1.0F).endVertex();
            bb.vertex( 0.5F,  0.5F,  0.5F).uv(1.0F,        0.5F).endVertex();

            SkyRenderContext.skyboxBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            SkyRenderContext.skyboxBuffer.bind();
            setTextureFilter();
            SkyRenderContext.skyboxBuffer.upload(bb.end());
        } else {
            SkyRenderContext.skyboxBuffer.bind();
            setTextureFilter();
        }

        float size = mc.options.getEffectiveRenderDistance() << 6;
        SkyRenderContext.poseStack.pushPose();
        SkyRenderContext.poseStack.mulPose(Axis.YN.rotationDegrees(yaw + SKYBOX_ROTATION.x()));
        SkyRenderContext.poseStack.mulPose(Axis.XP.rotationDegrees(pitch + SKYBOX_ROTATION.y()));
        SkyRenderContext.poseStack.mulPose(Axis.ZN.rotationDegrees(roll + SKYBOX_ROTATION.z()));
        SkyRenderContext.poseStack.scale(size, size, size);

        int a = (color >> 24) & 255;
        int r = (int) (((color >> 16) & 255) * (MAX_SKYBOX_BRIGHTNESS.x() / 255.0F));
        int g = (int) (((color >> 8) & 255) * (MAX_SKYBOX_BRIGHTNESS.y() / 255.0F));
        int b = (int) ((color & 255) * (MAX_SKYBOX_BRIGHTNESS.z() / 255.0F));
        int adjustedColor = Color.packColor(a, Mth.clamp(r, 0, 255), Mth.clamp(g, 0, 255), Mth.clamp(b, 0, 255));

        Color.applyColor(adjustedColor);
        SkyRenderContext.skyboxBuffer.drawWithShader(
                SkyRenderContext.poseStack.last().pose(),
                SkyRenderContext.projectionMatrix,
                GameRenderer.getPositionTexShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        SkyRenderContext.poseStack.popPose();
    }

    public static void renderAtmosphereSkybox(int color, Vector3f uWavelenghts) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        float pt = SkyRenderContext.partialTick;

        if (SkyRenderContext.atmosphereBuffer == null) {
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

            bb.vertex(-0.5F, -0.5F, -0.5F).endVertex();
            bb.vertex(-0.5F, -0.5F,  0.5F).endVertex();
            bb.vertex( 0.5F, -0.5F,  0.5F).endVertex();
            bb.vertex( 0.5F, -0.5F, -0.5F).endVertex();

            bb.vertex(-0.5F,  0.5F,  0.5F).endVertex();
            bb.vertex(-0.5F,  0.5F, -0.5F).endVertex();
            bb.vertex( 0.5F,  0.5F, -0.5F).endVertex();
            bb.vertex( 0.5F,  0.5F,  0.5F).endVertex();

            bb.vertex( 0.5F,  0.5F,  0.5F).endVertex();
            bb.vertex( 0.5F, -0.5F,  0.5F).endVertex();
            bb.vertex(-0.5F, -0.5F,  0.5F).endVertex();
            bb.vertex(-0.5F,  0.5F,  0.5F).endVertex();

            bb.vertex(-0.5F,  0.5F,  0.5F).endVertex();
            bb.vertex(-0.5F, -0.5F,  0.5F).endVertex();
            bb.vertex(-0.5F, -0.5F, -0.5F).endVertex();
            bb.vertex(-0.5F,  0.5F, -0.5F).endVertex();

            bb.vertex(-0.5F,  0.5F, -0.5F).endVertex();
            bb.vertex(-0.5F, -0.5F, -0.5F).endVertex();
            bb.vertex( 0.5F, -0.5F, -0.5F).endVertex();
            bb.vertex( 0.5F,  0.5F, -0.5F).endVertex();

            bb.vertex( 0.5F,  0.5F, -0.5F).endVertex();
            bb.vertex( 0.5F, -0.5F, -0.5F).endVertex();
            bb.vertex( 0.5F, -0.5F,  0.5F).endVertex();
            bb.vertex( 0.5F,  0.5F,  0.5F).endVertex();

            SkyRenderContext.atmosphereBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            SkyRenderContext.atmosphereBuffer.bind();
            SkyRenderContext.atmosphereBuffer.upload(bb.end());
        } else {
            SkyRenderContext.atmosphereBuffer.bind();
        }

        float theta = level.getTimeOfDay(pt) * ((float) Math.PI * 2.0F);
        float uCamHeight = (float) mc.gameRenderer.getMainCamera().getPosition().y;

        VolumeRenderUtil.setVec3(ClientShaderRegistry.skyboxAtmosphere(), "SunDir", new Vector3f(-Mth.sin(theta), Mth.cos(theta), 0.0F));
        VolumeRenderUtil.setFloat(ClientShaderRegistry.skyboxAtmosphere(), "uCamHeight", uCamHeight);
        VolumeRenderUtil.setVec3(ClientShaderRegistry.skyboxAtmosphere(), "uWavelenghts", uWavelenghts);


        float size = mc.options.getEffectiveRenderDistance() << 6;
        SkyRenderContext.poseStack.pushPose();
        SkyRenderContext.poseStack.scale(size, size, size);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Color.applyColor(color);

        SkyRenderContext.atmosphereBuffer.drawWithShader(
                SkyRenderContext.poseStack.last().pose(),
                SkyRenderContext.projectionMatrix,
                ClientShaderRegistry.skyboxAtmosphere());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        SkyRenderContext.poseStack.popPose();
    }

    public static void renderEndSky(float yaw, float pitch, float roll, int color, boolean constant) {
        Minecraft mc  = Minecraft.getInstance();
        Vec3 camPos   = mc.gameRenderer.getMainCamera().getPosition();
        boolean foggy = mc.level.effects().isFoggyAt(Mth.floor(camPos.x()), Mth.floor(camPos.y()))
                || mc.gui.getBossOverlay().shouldCreateWorldFog();
        if (foggy && !constant) return;

        SkyRenderContext.poseStack.pushPose();
        SkyRenderContext.poseStack.mulPose(Axis.YN.rotationDegrees(yaw + SKYBOX_ROTATION.x()));
        SkyRenderContext.poseStack.mulPose(Axis.XP.rotationDegrees(pitch + SKYBOX_ROTATION.y()));
        SkyRenderContext.poseStack.mulPose(Axis.ZN.rotationDegrees(roll + SKYBOX_ROTATION.z()));
        Matrix4f mat = SkyRenderContext.poseStack.last().pose();

        int a = (color >> 24) & 255;
        int r = (int) (((color >> 16) & 255) * (MAX_SKYBOX_BRIGHTNESS.x() / 255.0F));
        int g = (int) (((color >> 8) & 255) * (MAX_SKYBOX_BRIGHTNESS.y() / 255.0F));
        int b = (int) ((color & 255) * (MAX_SKYBOX_BRIGHTNESS.z() / 255.0F));
        int adjustedColor = Color.packColor(a, Mth.clamp(r, 0, 255), Mth.clamp(g, 0, 255), Mth.clamp(b, 0, 255));

        Color.applyColor(adjustedColor);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        // Bottom
        bb.vertex(mat, -100F, -100F, -100F).uv( 0F,  0F).endVertex();
        bb.vertex(mat, -100F, -100F,  100F).uv( 0F, 16F).endVertex();
        bb.vertex(mat,  100F, -100F,  100F).uv(16F, 16F).endVertex();
        bb.vertex(mat,  100F, -100F, -100F).uv(16F,  0F).endVertex();
        // South
        bb.vertex(mat, -100F, -100F,  100F).uv( 0F,  0F).endVertex();
        bb.vertex(mat, -100F,  100F,  100F).uv( 0F, 16F).endVertex();
        bb.vertex(mat,  100F,  100F,  100F).uv(16F, 16F).endVertex();
        bb.vertex(mat,  100F, -100F,  100F).uv(16F,  0F).endVertex();
        // North
        bb.vertex(mat, -100F,  100F, -100F).uv( 0F,  0F).endVertex();
        bb.vertex(mat, -100F, -100F, -100F).uv( 0F, 16F).endVertex();
        bb.vertex(mat,  100F, -100F, -100F).uv(16F, 16F).endVertex();
        bb.vertex(mat,  100F,  100F, -100F).uv(16F,  0F).endVertex();
        // Top
        bb.vertex(mat, -100F,  100F,  100F).uv( 0F,  0F).endVertex();
        bb.vertex(mat, -100F,  100F, -100F).uv( 0F, 16F).endVertex();
        bb.vertex(mat,  100F,  100F, -100F).uv(16F, 16F).endVertex();
        bb.vertex(mat,  100F,  100F,  100F).uv(16F,  0F).endVertex();
        // West
        bb.vertex(mat, -100F,  100F, -100F).uv( 0F,  0F).endVertex();
        bb.vertex(mat, -100F,  100F,  100F).uv( 0F, 16F).endVertex();
        bb.vertex(mat, -100F, -100F,  100F).uv(16F, 16F).endVertex();
        bb.vertex(mat, -100F, -100F, -100F).uv(16F,  0F).endVertex();
        // East
        bb.vertex(mat,  100F, -100F, -100F).uv( 0F,  0F).endVertex();
        bb.vertex(mat,  100F, -100F,  100F).uv( 0F, 16F).endVertex();
        bb.vertex(mat,  100F,  100F,  100F).uv(16F, 16F).endVertex();
        bb.vertex(mat,  100F, -100F, -100F).uv(16F,  0F).endVertex();

        BufferUploader.drawWithShader(bb.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        SkyRenderContext.poseStack.popPose();
    }

    public static void renderStars(int amount, int seed, float yaw, float pitch, float roll, int color, boolean constant) {
        // Rebuild the star buffer only when params change
        if (SkyRenderContext.starBuffer == null
                || amount != SkyRenderContext.starAmount
                || seed   != SkyRenderContext.starSeed) {
            SkyRenderContext.starAmount = amount;
            SkyRenderContext.starSeed   = seed;

            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            RandomSource rng = RandomSource.create(seed);

            for (int i = 0; i < amount; i++) {
                float x = rng.nextFloat() * 2.0F - 1.0F;
                float y = rng.nextFloat() * 2.0F - 1.0F;
                float z = rng.nextFloat() * 2.0F - 1.0F;
                float r = 0.25F + 0.15F * rng.nextFloat();
                float lenSq = x * x + y * y + z * z;
                if (lenSq < 1.0F && lenSq > 0.01F) {
                    float invLen = 1.0F / Mth.sqrt(lenSq);
                    x *= invLen; y *= invLen; z *= invLen;
                    float sx = x * 200.0F, sy = y * 200.0F, sz = z * 200.0F;

                    float azimuth  = (float) Math.atan2(x, z);
                    float sinAz    = Mth.sin(azimuth), cosAz = Mth.cos(azimuth);
                    float altitude = (float) Math.atan2(Mth.sqrt(x * x + z * z), y);
                    float sinAlt   = Mth.sin(altitude), cosAlt = Mth.cos(altitude);
                    float spin     = (float) rng.nextDouble() * Mth.PI * 2.0F;
                    float sinSpin  = Mth.sin(spin), cosSpin = Mth.cos(spin);

                    for (int j = 0; j < 4; j++) {
                        float u = ((j & 2) - 1)       * r;
                        float v = ((j + 1 & 2) - 1)   * r;
                        float rotU = u * cosSpin - v * sinSpin;
                        float rotV = v * cosSpin + u * sinSpin;
                        float dx   = -rotU * cosAlt;
                        float bx   =  dx * sinAz - rotV * cosAz;
                        float by   =  rotU * sinAlt;
                        float bz   =  rotV * sinAz + dx * cosAz;
                        bb.vertex(sx + bx, sy + by, sz + bz).endVertex();
                    }
                }
            }

            if (SkyRenderContext.starBuffer != null) SkyRenderContext.starBuffer.close();
            SkyRenderContext.starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            SkyRenderContext.starBuffer.bind();
            SkyRenderContext.starBuffer.upload(bb.end());
        } else {
            SkyRenderContext.starBuffer.bind();
        }

        float alpha = (color >>> 24) / 255.0F;
        if (!constant) alpha *= (1.0F - Minecraft.getInstance().level.getRainLevel(SkyRenderContext.partialTick));

        SkyRenderContext.poseStack.pushPose();
        SkyRenderContext.poseStack.mulPose(Axis.YN.rotationDegrees(yaw + SKYBOX_ROTATION.x()));
        SkyRenderContext.poseStack.mulPose(Axis.XP.rotationDegrees(pitch + SKYBOX_ROTATION.y()));
        SkyRenderContext.poseStack.mulPose(Axis.ZN.rotationDegrees(roll + SKYBOX_ROTATION.z()));
        FogRenderer.setupNoFog();
        RenderSystem.setShaderColor(
                (color >> 16 & 255) / 255.0F,
                (color >>  8 & 255) / 255.0F,
                (color       & 255) / 255.0F, alpha);
        SkyRenderContext.starBuffer.drawWithShader(
                SkyRenderContext.poseStack.last().pose(),
                SkyRenderContext.projectionMatrix,
                GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        SkyRenderContext.setupFog.run();
        SkyRenderContext.poseStack.popPose();
    }

    public static void renderSun(float size, int color, boolean constant) {
        ClientLevel level = Minecraft.getInstance().level;
        float r     = size / 2.0F;
        float alpha = (color >>> 24) / 255.0F;
        if (!constant) alpha *= (1.0F - level.getRainLevel(SkyRenderContext.partialTick));

        SkyRenderContext.poseStack.pushPose();
        setTextureFilter();
        SkyRenderContext.poseStack.mulPose(Axis.ZP.rotationDegrees(level.getTimeOfDay(SkyRenderContext.partialTick) * 360.0F));
        Matrix4f mat = SkyRenderContext.poseStack.last().pose();
        RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(mat,  r, 100.0F, -r).uv(0.0F, 0.0F).endVertex();
        bb.vertex(mat,  r, 100.0F,  r).uv(1.0F, 0.0F).endVertex();
        bb.vertex(mat, -r, 100.0F,  r).uv(1.0F, 1.0F).endVertex();
        bb.vertex(mat, -r, 100.0F, -r).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        SkyRenderContext.poseStack.popPose();
        setDisabledTextureFilter();
    }

    public static void renderSun(float size, float angle, int color) {
        ClientLevel level = Minecraft.getInstance().level;
        float r     = size / 2.0F;
        float alpha = ((color >>> 24) / 255.0F) * (1.0F - level.getRainLevel(SkyRenderContext.partialTick));

        SkyRenderContext.poseStack.pushPose();
        SkyRenderContext.poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        Matrix4f mat = SkyRenderContext.poseStack.last().pose();
        RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(mat,  r, 100.0F, -r).uv(0.0F, 0.0F).endVertex();
        bb.vertex(mat,  r, 100.0F,  r).uv(1.0F, 0.0F).endVertex();
        bb.vertex(mat, -r, 100.0F,  r).uv(1.0F, 1.0F).endVertex();
        bb.vertex(mat, -r, 100.0F, -r).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        SkyRenderContext.poseStack.popPose();
    }

    public static void renderMoon(float size, int color, boolean phase, boolean constant) {
        ClientLevel level = Minecraft.getInstance().level;
        float r  = size / 2.0F;
        float u0 = 0.0F, v0 = 0.0F, u1 = 1.0F, v1 = 1.0F;
        if (phase) {
            int moonPhase = level.getMoonPhase();
            int col = moonPhase & 3;
            int row = (moonPhase >> 2) & 1;
            u0 = col        / 4.0F;
            v0 = row        / 2.0F;
            u1 = (col + 1)  / 4.0F;
            v1 = (row + 1)  / 2.0F;
        }
        float alpha = (color >>> 24) / 255.0F;
        if (!constant) alpha *= (1.0F - level.getRainLevel(SkyRenderContext.partialTick));

        SkyRenderContext.poseStack.pushPose();
        SkyRenderContext.poseStack.mulPose(Axis.ZP.rotationDegrees(level.getTimeOfDay(SkyRenderContext.partialTick) * 360.0F));
        Matrix4f mat = SkyRenderContext.poseStack.last().pose();
        RenderSystem.setShaderColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(mat, -r, -100.0F, -r).uv(u1, v1).endVertex();
        bb.vertex(mat, -r, -100.0F,  r).uv(u0, v1).endVertex();
        bb.vertex(mat,  r, -100.0F,  r).uv(u0, v0).endVertex();
        bb.vertex(mat,  r, -100.0F, -r).uv(u1, v0).endVertex();
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        SkyRenderContext.poseStack.popPose();
    }

    public static void renderSunlights(int color) {
        ClientLevel level   = Minecraft.getInstance().level;
        float[] rawColor    = level.effects().getSunriseColor(level.getTimeOfDay(SkyRenderContext.partialTick), SkyRenderContext.partialTick);
        if (rawColor == null) return;

        int r     = color >> 16 & 255;
        int g     = color >>  8 & 255;
        int b     = color       & 255;
        int alpha = (int) ((color >>> 24) * rawColor[3]);
        boolean sunRising = Mth.sin(level.getSunAngle(SkyRenderContext.partialTick)) < 0.0F;

        Matrix4f mat = SkyRenderContext.poseStack.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // Fan center: toward the sun or moon
        if (sunRising) bb.vertex(mat,  100.0F, 0.0F, 0.0F).color(r, g, b, alpha).endVertex();
        else           bb.vertex(mat, -100.0F, 0.0F, 0.0F).color(r, g, b, alpha).endVertex();

        for (int i = 0; i <= 16; i++) {
            float deg = i * Mth.TWO_PI / 16.0F;
            float sin = Mth.sin(deg), cos = Mth.cos(deg);
            if (sunRising) bb.vertex(mat,  cos * 120.0F, cos * 40.0F * rawColor[3], -sin * 120.0F).color(r, g, b, 0).endVertex();
            else           bb.vertex(mat, -cos * 120.0F, cos * 40.0F * rawColor[3],  sin * 120.0F).color(r, g, b, 0).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
    }

    public static void renderTexture(float size, float yaw, float pitch, float roll, int color, boolean constant) {
        float r     = size / 2.0F;
        float alpha = (color >>> 24) / 255.0F;
        if (!constant) alpha *= (1.0F - Minecraft.getInstance().level.getRainLevel(SkyRenderContext.partialTick));

        SkyRenderContext.poseStack.pushPose();
        SkyRenderContext.poseStack.mulPose(Axis.YN.rotationDegrees(yaw + SKYBOX_ROTATION.x()));
        SkyRenderContext.poseStack.mulPose(Axis.XP.rotationDegrees(pitch + SKYBOX_ROTATION.y()));
        SkyRenderContext.poseStack.mulPose(Axis.ZN.rotationDegrees(roll + SKYBOX_ROTATION.z()));
        Matrix4f mat = SkyRenderContext.poseStack.last().pose();

        int a = (color >> 24) & 255;
        int red = (int) (((color >> 16) & 255) * (MAX_SKYBOX_BRIGHTNESS.x() / 255.0F));
        int green = (int) (((color >> 8) & 255) * (MAX_SKYBOX_BRIGHTNESS.y() / 255.0F));
        int blue = (int) ((color & 255) * (MAX_SKYBOX_BRIGHTNESS.z() / 255.0F));
        int adjustedColor = Color.packColor(a, Mth.clamp(red, 0, 255), Mth.clamp(green, 0, 255), Mth.clamp(blue, 0, 255));

        RenderSystem.setShaderColor((adjustedColor >> 16 & 255) / 255.0F, (adjustedColor >> 8 & 255) / 255.0F, (adjustedColor & 255) / 255.0F, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(mat,  r,  r, 100.0F).uv(0.0F, 0.0F).endVertex();
        bb.vertex(mat,  r, -r, 100.0F).uv(0.0F, 1.0F).endVertex();
        bb.vertex(mat, -r, -r, 100.0F).uv(1.0F, 1.0F).endVertex();
        bb.vertex(mat, -r,  r, 100.0F).uv(1.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        SkyRenderContext.poseStack.popPose();
    }

    public static void renderAbyss(int color, boolean constant) {
        Minecraft mc = Minecraft.getInstance();
        boolean belowHorizon = mc.player.getEyePosition(SkyRenderContext.partialTick).y()
                - mc.level.getLevelData().getHorizonHeight(mc.level) < 0.0D;
        if (!belowHorizon && !constant) return;

        if (SkyRenderContext.abyssBuffer == null) {
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
            bb.vertex(0.0F, -16.0F, 0.0F).endVertex();
            for (int i = 0; i <= 8; i++) {
                float angle = i * 45.0F * Mth.DEG_TO_RAD;
                bb.vertex(-512.0F * Mth.cos(angle), -16.0F, 512.0F * Mth.sin(angle)).endVertex();
            }
            SkyRenderContext.abyssBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            SkyRenderContext.abyssBuffer.bind();
            SkyRenderContext.abyssBuffer.upload(bb.end());
        } else {
            SkyRenderContext.abyssBuffer.bind();
        }

        SkyRenderContext.poseStack.pushPose();
        SkyRenderContext.poseStack.translate(0.0F, 12.0F, 0.0F);
        RenderSystem.setShaderColor(
                (color >> 16 & 255) / 255.0F,
                (color >>  8 & 255) / 255.0F,
                (color       & 255) / 255.0F,
                (color >>> 24)      / 255.0F);
        SkyRenderContext.abyssBuffer.drawWithShader(
                SkyRenderContext.poseStack.last().pose(),
                SkyRenderContext.projectionMatrix,
                GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        SkyRenderContext.poseStack.popPose();
    }

    public static void renderDeepSky(int color) {
        if (SkyRenderContext.deepSkyBuffer == null) {
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
            bb.vertex(0.0F, 16.0F, 0.0F).endVertex();
            for (int i = 0; i <= 8; i++) {
                float angle = 45.0F * i * Mth.DEG_TO_RAD;
                bb.vertex(512.0F * Mth.cos(angle), 16.0F, 512.0F * Mth.sin(angle)).endVertex();
            }
            SkyRenderContext.deepSkyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            SkyRenderContext.deepSkyBuffer.bind();
            SkyRenderContext.deepSkyBuffer.upload(bb.end());
        } else {
            SkyRenderContext.deepSkyBuffer.bind();
        }
        RenderSystem.setShaderColor(
                (color >> 16 & 255) / 255.0F,
                (color >>  8 & 255) / 255.0F,
                (color       & 255) / 255.0F,
                (color >>> 24)      / 255.0F);
        SkyRenderContext.deepSkyBuffer.drawWithShader(
                SkyRenderContext.poseStack.last().pose(),
                SkyRenderContext.projectionMatrix,
                GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void renderComets(float yaw, float pitch, float roll, int minSpawn, int maxSpawn) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long gameTime = mc.level.getGameTime();

        // Spawn a new comet if under the limit and enough time has passed
        if (SkyRenderContext.activeComets.size() < SkyRenderContext.MAX_COMETS
                && gameTime - SkyRenderContext.lastCometSpawnTime
                > minSpawn + SkyRenderContext.cometRandom.nextInt(maxSpawn)) {
            SkyRenderContext.lastCometSpawnTime = gameTime;
            SkyRenderContext.activeComets.add(spawnComet());
        }

        // Remove expired comets
        SkyRenderContext.activeComets.removeIf(c -> c.age > c.life);
        if (SkyRenderContext.activeComets.isEmpty()) return;

        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (SkyRenderContext.CometData comet : SkyRenderContext.activeComets) {
            comet.age += 1.0F;
            float interpAge = comet.age - 1.0F + SkyRenderContext.partialTick;
            Vec3 headPos    = comet.startPos.add(comet.velocity.scale(interpAge));

            comet.history.addLast(headPos);
            if (comet.history.size() > 60) comet.history.removeFirst();

            float lifeRatio   = comet.age / comet.life;
            float baseAlphaF  = Mth.clamp(1.0F - lifeRatio, 0.0F, 1.0F);
            int   baseAlpha   = (int) (baseAlphaF * 255.0F);

            SkyRenderContext.poseStack.pushPose();
            SkyRenderContext.poseStack.mulPose(Axis.YN.rotationDegrees(yaw + SKYBOX_ROTATION.x()));
            SkyRenderContext.poseStack.mulPose(Axis.XP.rotationDegrees(pitch + SKYBOX_ROTATION.y()));
            SkyRenderContext.poseStack.mulPose(Axis.ZN.rotationDegrees(roll + SKYBOX_ROTATION.z()));

            // Head quad
            addCometQuad(bb, headPos, comet.size, comet.color, baseAlpha, SkyRenderContext.poseStack);

            // Tail segments (fading & growing toward the back)
            final int   TAIL_SEGMENTS = 15;
            final float SEGMENT_GAP   = 1.5F;
            Vec3 velNorm = comet.velocity.normalize();
            for (int s = 1; s <= TAIL_SEGMENTS; s++) {
                float t       = s / (float) TAIL_SEGMENTS;
                Vec3  segPos  = headPos.add(velNorm.scale(-s * SEGMENT_GAP));
                float segSize = comet.size * (0.6F + (1.0F - t) * 2.5F);
                float fade    = (float) Math.pow(1.0F - t, 1.4);
                int   segAlpha = Mth.clamp((int) (baseAlphaF * fade * 255.0F), 0, 255);
                addCometQuad(bb, segPos, segSize, comet.color, segAlpha, SkyRenderContext.poseStack);
            }

            SkyRenderContext.poseStack.popPose();
        }

        BufferUploader.drawWithShader(bb.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private static SkyRenderContext.CometData spawnComet() {
        RandomSource rng = SkyRenderContext.cometRandom;
        SkyRenderContext.CometData c = new SkyRenderContext.CometData();

        double theta = rng.nextDouble() * Math.PI * 2.0;
        double phi   = Math.acos(2.0 * rng.nextDouble() - 1.0);
        Vec3   dir   = new Vec3(Math.sin(phi) * Math.cos(theta), Math.cos(phi), Math.sin(phi) * Math.sin(theta));

        c.startPos = dir.scale(90.0 + rng.nextDouble() * 40.0);

        Vec3 up  = Math.abs(dir.y) > 0.9 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
        Vec3 tanA = dir.cross(up).normalize();
        Vec3 tanB = dir.cross(tanA).normalize();
        Vec3 vel  = tanA.scale(rng.nextDouble() * 2.0 - 1.0)
                .add(tanB.scale(rng.nextDouble() * 2.0 - 1.0))
                .normalize();

        c.velocity = vel.scale(0.3 + rng.nextDouble() * 0.4);
        c.age      = 0F;
        c.life     = 200 + rng.nextInt(300);
        c.size     = 0.5F + rng.nextFloat() * 0.8F;
        c.color    = 0xFFFFFFFF;
        for (int i = 0; i < 6; i++) c.history.addLast(c.startPos);
        return c;
    }

    private static void addCometQuad(BufferBuilder bb, Vec3 pos, float size, int color, int alpha, PoseStack pose) {
        Matrix4f mat = pose.last().pose();
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >>  8 & 255) / 255.0F;
        float b = (color       & 255) / 255.0F;
        float a = alpha / 255.0F;
        float half = size * 0.5F;
        float ang  = (float) ((pos.x + pos.y + pos.z) % (Math.PI * 2));
        float sa   = Mth.sin(ang), ca = Mth.cos(ang);
        float px   = (float) pos.x, py = (float) pos.y, pz = (float) pos.z;

        bb.vertex(mat, px - half * ca, py - half * sa, pz).color(r, g, b, a).endVertex();
        bb.vertex(mat, px + half * ca, py - half * sa, pz).color(r, g, b, a).endVertex();
        bb.vertex(mat, px + half * ca, py + half * sa, pz).color(r, g, b, a).endVertex();
        bb.vertex(mat, px - half * ca, py + half * sa, pz).color(r, g, b, a).endVertex();
    }

    private static void setTextureFilter() {
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL,  0);
    }

    private static void setDisabledTextureFilter() {
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL,  0);
    }

}