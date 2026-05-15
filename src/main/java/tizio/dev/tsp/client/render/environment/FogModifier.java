package tizio.dev.tsp.client.render.environment;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.utils.Utils;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FogModifier {

    public static ViewportEvent.ComputeFogColor colorProvider = null;
    public static ViewportEvent.RenderFog shapeProvider = null;

    public static void setColor(Vec3 color) {
        int x = (255 << 24 | (int) color.x << 16 | (int) color.y << 8 | (int) color.z);
        colorProvider.setRed((x >> 16 & 255) / 255.0F);
        colorProvider.setGreen((x >> 8 & 255) / 255.0F);
        colorProvider.setBlue((x & 255) / 255.0F);
    }

    public static void setDistance(float start, float end) {
        shapeProvider.setNearPlaneDistance(start);
        shapeProvider.setFarPlaneDistance(end);
        if (!shapeProvider.isCanceled()) {
            shapeProvider.setCanceled(true);
        }
    }

    public static void setShape(FogShape shape) {
        shapeProvider.setFogShape(shape);
        if (!shapeProvider.isCanceled()) {
            shapeProvider.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void computeFogColor(ViewportEvent.ComputeFogColor event) {
        colorProvider = event;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Entity entity = colorProvider.getCamera().getEntity();
        if (level != null && entity != null) {

            if (Utils.inDimension("tsp:moon")) {
                setColor(new Vec3(0, 0, 0));
            } else if (Utils.inDimension("tsp:space")) {
                setColor(new Vec3(0, 0, 0));
            }
        }
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog event) {
        shapeProvider = event;
        if (shapeProvider.getMode() == FogRenderer.FogMode.FOG_TERRAIN) {
            ClientLevel level = Minecraft.getInstance().level;
            Entity entity = shapeProvider.getCamera().getEntity();
            if (level != null && entity != null) {

                if (Utils.inDimension("tsp:mars")) {
                    setShape(FogShape.CYLINDER);
                    setDistance(0, Minecraft.getInstance().gameRenderer.getRenderDistance() + 100);
                }
            }
        }
    }
}
