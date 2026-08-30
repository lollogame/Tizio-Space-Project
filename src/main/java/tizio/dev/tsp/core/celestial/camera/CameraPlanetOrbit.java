package tizio.dev.tsp.core.celestial.camera;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.data.CelestialJsonLoader;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = MainClass.MODID, value = Dist.CLIENT)
public final class CameraPlanetOrbit {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SPACE_DIMENSION = new ResourceLocation(MainClass.MODID, "space");

    private static final float MIN_DISTANCE_MULTIPLIER = 1.15F;
    private static final float INITIAL_DISTANCE_MULTIPLIER = 5.0F;
    private static final float MAX_DISTANCE_MULTIPLIER = 60.0F;
    private static final double DRAG_SENSITIVITY = 0.4D;
    private static final double ZOOM_STEP_FACTOR = 0.9D;
    private static final double ZOOM_LERP_ALPHA = 0.25D;
    private static final double ZOOM_SNAP_EPSILON = 0.001D;

    private static Method setPositionMethod;
    private static Method setRotationMethod;
    private static boolean reflectionAvailable;

    static {
        try {
            setPositionMethod = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
            setPositionMethod.setAccessible(true);
            setRotationMethod = Camera.class.getDeclaredMethod("setRotation", float.class, float.class);
            setRotationMethod.setAccessible(true);
            reflectionAvailable = true;
        } catch (NoSuchMethodException e) {
            reflectionAvailable = false;
        }
    }

    private static boolean active = false;
    private static String focusedBodyId = null;
    private static Vec3 focusPoint = Vec3.ZERO;
    private static double minDistance = 1.0D;
    private static double maxDistance = 100.0D;
    private static double distance = 10.0D;
    private static double targetDistance = 10.0D;
    private static double orbitYaw = 0.0D;
    private static double orbitPitch = 20.0D;

    private CameraPlanetOrbit() {}

    public static boolean isActive() {
        return active;
    }

    public static boolean isInSpaceDimension() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && SPACE_DIMENSION.equals(mc.level.dimension().location());
    }

    public static boolean activate(CelestialJsonLoader.BodySpatialInfo target) {
        if (target == null) {
            return false;
        }
        return activate(target.body().id, target.spacePosition(), target.visualRadius());
    }

    public static boolean activate(String targetId, Vec3 position, double visualRadius) {
        if (targetId == null || position == null || !reflectionAvailable || !isInSpaceDimension()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return false;
        }

        focusedBodyId = targetId;
        focusPoint = position;

        double radius = Math.max(0.1D, visualRadius);
        minDistance = Math.max(1.0D, radius * MIN_DISTANCE_MULTIPLIER);
        maxDistance = Math.max(minDistance + 5.0D, radius * MAX_DISTANCE_MULTIPLIER);

        distance = clamp(radius * INITIAL_DISTANCE_MULTIPLIER, minDistance, maxDistance);
        targetDistance = distance;
        orbitYaw = 0.0D;
        orbitPitch = 20.0D;
        active = true;
        return true;
    }

    public static void updateFocus(CelestialJsonLoader.BodySpatialInfo target) {
        if (target == null) {
            return;
        }
        updateFocus(target.body().id, target.spacePosition(), target.visualRadius());
    }

    public static void updateFocus(String targetId, Vec3 position, double visualRadius) {
        if (!active || targetId == null || focusedBodyId == null || !focusedBodyId.equals(targetId)) {
            return;
        }

        focusPoint = position;

        double currentRadius = Math.max(0.1D, visualRadius);
        double newMin = Math.max(1.0D, currentRadius * MIN_DISTANCE_MULTIPLIER);
        double newMax = Math.max(newMin + 5.0D, currentRadius * MAX_DISTANCE_MULTIPLIER);

        if (Math.abs(minDistance - newMin) > 0.0001D || Math.abs(maxDistance - newMax) > 0.0001D) {
            double oldRange = Math.max(0.0001D, maxDistance - minDistance);
            double relTarget = (targetDistance - minDistance) / oldRange;
            double relDistance = (distance - minDistance) / oldRange;

            minDistance = newMin;
            maxDistance = newMax;

            double newRange = maxDistance - minDistance;
            targetDistance = clamp(minDistance + relTarget * newRange, minDistance, maxDistance);
            distance = clamp(minDistance + relDistance * newRange, minDistance, maxDistance);
        } else {
            targetDistance = clamp(targetDistance, minDistance, maxDistance);
            distance = clamp(distance, minDistance, maxDistance);
        }
    }

    public static void deactivate() {
        active = false;
        focusedBodyId = null;
    }

    public static void handleScroll(double scrollDelta) {
        if (!active) {
            return;
        }
        targetDistance = clamp(targetDistance * Math.pow(ZOOM_STEP_FACTOR, scrollDelta), minDistance, maxDistance);
    }

    public static void handleDrag(double dragX, double dragY) {
        if (!active) {
            return;
        }
        orbitYaw += dragX * DRAG_SENSITIVITY;
        orbitPitch = clamp(orbitPitch + dragY * DRAG_SENSITIVITY, -89.0D, 89.0D);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null || !isInSpaceDimension()) {
            deactivate();
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (active) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (active) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {

        if (!active || !reflectionAvailable) {
            return;
        }

        targetDistance = Math.max(minDistance, targetDistance);

        double delta = targetDistance - distance;
        distance = Math.abs(delta) < ZOOM_SNAP_EPSILON ? targetDistance : distance + delta * ZOOM_LERP_ALPHA;
        distance = Math.max(minDistance, distance);

        double yawRad = Math.toRadians(orbitYaw);
        double pitchRad = Math.toRadians(orbitPitch);
        double horizontal = distance * Math.cos(pitchRad);
        double offsetX = horizontal * Math.sin(yawRad);
        double offsetZ = -horizontal * Math.cos(yawRad);
        double offsetY = distance * Math.sin(pitchRad);

        Vec3 cameraPos = focusPoint.add(offsetX, offsetY, offsetZ);
        Vec3 dir = focusPoint.subtract(cameraPos).normalize();
        double horizontalDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float lookYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float lookPitch = (float) Math.toDegrees(-Math.atan2(dir.y, horizontalDist));

        try {
            setPositionMethod.invoke(event.getCamera(), cameraPos);
            setRotationMethod.invoke(event.getCamera(), lookYaw, lookPitch);
        } catch (Exception e) {
            deactivate();
            return;
        }

        event.setYaw(lookYaw);
        event.setPitch(lookPitch);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}