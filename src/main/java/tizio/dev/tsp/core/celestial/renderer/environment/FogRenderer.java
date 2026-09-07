package tizio.dev.tsp.core.celestial.renderer.environment;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.data.CelestialJsonLoader;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FogRenderer {

    public static ViewportEvent.ComputeFogColor colorProvider = null;
    public static ViewportEvent.RenderFog shapeProvider = null;

    public static void setColor(Vec3 color) {
        if (colorProvider == null) return;
        int x = (255 << 24 | (int) color.x << 16 | (int) color.y << 8 | (int) color.z);
        colorProvider.setRed((x >> 16 & 255) / 255.0F);
        colorProvider.setGreen((x >> 8 & 255) / 255.0F);
        colorProvider.setBlue((x & 255) / 255.0F);
    }

    public static void setColor(float r, float g, float b) {
        if (colorProvider == null) return;
        colorProvider.setRed(r);
        colorProvider.setGreen(g);
        colorProvider.setBlue(b);
    }

    public static void setColorHex(String hex) {
        if (colorProvider == null) return;
        Vector3f rgb = CelestialJsonLoader.parseColor(hex, new Vector3f(0.0F, 0.0F, 0.0F));
        colorProvider.setRed(rgb.x);
        colorProvider.setGreen(rgb.y);
        colorProvider.setBlue(rgb.z);
    }

    public static void setDistance(float start, float end) {
        if (shapeProvider == null) return;
        shapeProvider.setNearPlaneDistance(start);
        shapeProvider.setFarPlaneDistance(end);
        if (!shapeProvider.isCanceled()) {
            shapeProvider.setCanceled(true);
        }
    }

    public static void setShape(FogShape shape) {
        if (shapeProvider == null) return;
        shapeProvider.setFogShape(shape);
        if (!shapeProvider.isCanceled()) {
            shapeProvider.setCanceled(true);
        }
    }

    public static PlanetInstance.Config resolveCurrentBody() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        String currentDimId = mc.level.dimension().location().toString();
        for (SolarSystemData system : CelestialJsonLoader.getActiveSystems().values()) {
            for (PlanetInstance.Config body : system.bodies) {
                if (currentDimId.equalsIgnoreCase(body.dimension)) {
                    return body;
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void computeFogColor(ViewportEvent.ComputeFogColor event) {
        colorProvider = event;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Entity entity = event.getCamera().getEntity();
        if (entity == null) return;

        if (event.getCamera().getFluidInCamera() != FogType.NONE) return;

        String dimId = level.dimension().location().toString();

        if (CelestialJsonLoader.isSpaceDimension(dimId)) {
            setColor(0.0F, 0.0F, 0.0F);
            return;
        }

        PlanetInstance.Config body = resolveCurrentBody();
        if (body != null && body.fog != null && body.fog.enabled) {
            float partialTick = (float) event.getPartialTick();
            float timeOfDay = level.getTimeOfDay(partialTick);
            float sunHeight = Mth.cos(timeOfDay * ((float) Math.PI * 2.0F)) * 2.0F + 0.5F;
            float dayFactor = Mth.clamp(sunHeight, 0.0F, 1.0F);

            Vector3f rgb = CelestialJsonLoader.parseColor(body.fog.colorHex, new Vector3f(0.0F, 0.0F, 0.0F));

            float defR = event.getRed();
            float defG = event.getGreen();
            float defB = event.getBlue();

            float brightness = dayFactor * 0.92F + 0.08F;
            float customR = rgb.x * brightness;
            float customG = rgb.y * brightness;
            float customB = rgb.z * brightness;

            float finalR = Mth.lerp(dayFactor, Math.min(defR, customR), customR);
            float finalG = Mth.lerp(dayFactor, Math.min(defG, customG), customG);
            float finalB = Mth.lerp(dayFactor, Math.min(defB, customB), customB);

            event.setRed(finalR);
            event.setGreen(finalG);
            event.setBlue(finalB);
        }
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog event) {
        shapeProvider = event;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Entity entity = event.getCamera().getEntity();
        if (entity == null) return;

        if (event.getCamera().getFluidInCamera() != FogType.NONE) return;

        String dimId = level.dimension().location().toString();

        if (CelestialJsonLoader.isSpaceDimension(dimId)) {
            setShape(FogShape.CYLINDER);
            float renderDist = mc.gameRenderer.getRenderDistance();
            setDistance(renderDist * 0.5F, renderDist);
            return;
        }

        PlanetInstance.Config body = resolveCurrentBody();
        if (body != null && body.fog != null && body.fog.enabled) {
            FogShape shape = "SPHERE".equalsIgnoreCase(body.fog.shape) ? FogShape.SPHERE : FogShape.CYLINDER;
            setShape(shape);

            float renderDist = mc.gameRenderer.getRenderDistance();
            float start;
            float end;

            if (body.fog.useRenderDistance) {
                float pctStart = body.fog.startDistance / 100.0F;
                float pctEnd = Math.max(0.01F, body.fog.endDistance / 100.0F);
                start = renderDist * pctStart;
                end = renderDist * pctEnd;
            } else {
                start = body.fog.startDistance;
                end = body.fog.endDistance > 0.0F ? body.fog.endDistance : renderDist;
            }

            if (event.getMode() == net.minecraft.client.renderer.FogRenderer.FogMode.FOG_SKY) {
                setDistance(0.0F, end);
            } else {
                setDistance(start, end);
            }
        }
    }
}
