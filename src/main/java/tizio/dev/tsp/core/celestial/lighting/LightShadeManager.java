package tizio.dev.tsp.core.celestial.lighting;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunInstance;
import tizio.dev.tsp.core.client.ClientRenderRegistries;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class LightShadeManager {

    public static float SUN_BASE_INTENSITY = 2.5F;
    public static float SUN_DIRECT_MIN_SHADE = 0.50F;
    public static float SUN_DIRECT_FACTOR = 0.50F;

    public static float SHADOW_FACE_SHADE = 0.50F;
    public static float VOID_AMBIENT_R = 0.05F;
    public static float VOID_AMBIENT_G = 0.05F;
    public static float VOID_AMBIENT_B = 0.05F;

    public static float TORCH_INTENSITY = 1.0F;
    public static float TORCH_BOOST_R = 0.0F;
    public static float TORCH_BOOST_G = 0.0F;
    public static float TORCH_BOOST_B = 0.0F;
    public static float TORCH_FALLOFF_EXPONENT = 1.3F;

    public static float ENTITY_SUN_INTENSITY = 1.0F;
    public static float ENTITY_SHADOW_FILL_INTENSITY = 0.0F;
    public static float ENTITY_SECONDARY_STAR_INTENSITY = 0.5F;

    public static final Vector3f primarySunDirection = new Vector3f(0.0F, 1.0F, 0.0F);
    public static final Vector3f primarySunColor = new Vector3f(1.0F, 1.0F, 1.0F);
    public static float primarySunIntensity = 1.0F;

    public static final Vector3f secondarySunDirection = new Vector3f(0.0F, 1.0F, 0.0F);
    public static final Vector3f secondarySunColor = new Vector3f(0.0F, 0.0F, 0.0F);
    public static float secondarySunIntensity = 0.0F;

    private static boolean spaceLightingActive = false;
    private static boolean renderingLevel = false;
    private static long lastUpdateFrame = -1L;

    private LightShadeManager() {}

    public static boolean isSpaceLightingActive() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        return CelestialJsonLoader.isSpaceDimension(mc.level.dimension().location());
    }

    public static boolean isRenderingLevel() {
        return renderingLevel;
    }

    public static void setRenderingLevel(boolean rendering) {
        renderingLevel = rendering;
    }

    public static void update(Camera camera, ClientLevel level) {
        if (!isSpaceLightingActive()) {
            spaceLightingActive = false;
            return;
        }

        spaceLightingActive = true;
        Vec3 camPos = camera.getPosition();

        List<SunInstance> suns = new ArrayList<>(ClientRenderRegistries.SUNS.instances());
        if (suns.isEmpty()) {
            primarySunDirection.set(0.2F, 1.0F, -0.7F).normalize();
            primarySunColor.set(1.0F, 0.95F, 0.85F);
            primarySunIntensity = SUN_BASE_INTENSITY;

            secondarySunDirection.set(0.0F, 0.0F, 0.0F);
            secondarySunColor.set(0.0F, 0.0F, 0.0F);
            secondarySunIntensity = 0.0F;
            return;
        }

        suns.sort(Comparator.comparingDouble((SunInstance s) -> {
            double distSq = Math.max(1.0, s.position().distanceToSqr(camPos));
            return -((double) s.sunRadius() / Math.sqrt(distSq));
        }));

        SunInstance mainSun = suns.get(0);
        Vec3 toMainSun = mainSun.position().subtract(camPos);
        if (toMainSun.lengthSqr() < 1e-4) {
            toMainSun = new Vec3(0, 1, 0);
        }
        Vec3 normMain = toMainSun.normalize();
        primarySunDirection.set((float) normMain.x, (float) normMain.y, (float) normMain.z);
        Vector3f mainColor = mainSun.color();
        primarySunColor.set(mainColor.x(), mainColor.y(), mainColor.z());
        primarySunIntensity = SUN_BASE_INTENSITY;

        if (suns.size() > 1) {
            SunInstance secSun = suns.get(1);
            Vec3 toSecSun = secSun.position().subtract(camPos);
            if (toSecSun.lengthSqr() < 1e-4) {
                toSecSun = new Vec3(0, -1, 0);
            }
            Vec3 normSec = toSecSun.normalize();
            secondarySunDirection.set((float) normSec.x, (float) normSec.y, (float) normSec.z);
            Vector3f secColor = secSun.color();
            secondarySunColor.set(secColor.x(), secColor.y(), secColor.z());
            secondarySunIntensity = 0.5F;
        } else {

            secondarySunDirection.set(0.0F, 0.0F, 0.0F);
            secondarySunColor.set(0.0F, 0.0F, 0.0F);
            secondarySunIntensity = 0.0F;
        }
    }

    @Deprecated
    public static Vector3f getPrimarySunDirection() {
        ensureUpdated();
        return primarySunDirection;
    }

    @Deprecated
    public static Vector3f getSecondarySunDirection() {
        ensureUpdated();
        return secondarySunDirection;
    }

    public static Vector3f getPrimarySunDirectionForEntities() {
        ensureUpdated();
        Vector3f dir = new Vector3f(primarySunDirection);
        dir.mul(ENTITY_SUN_INTENSITY);
        return dir;
    }

    public static Vector3f getSecondarySunDirectionForEntities() {
        ensureUpdated();
        if (secondarySunIntensity > 0.05F) {
            Vector3f sec = new Vector3f(secondarySunDirection);
            sec.mul(ENTITY_SECONDARY_STAR_INTENSITY);
            return sec;
        } else if (ENTITY_SHADOW_FILL_INTENSITY > 0.001F) {

            Vector3f fill = new Vector3f(primarySunDirection).negate().mul(ENTITY_SHADOW_FILL_INTENSITY);
            return fill;
        } else {
            return new Vector3f(0.0F, 0.0F, 0.0F);
        }
    }
    @Deprecated
    public static Vector3f getPrimarySunColor() {
        return primarySunColor;
    }
    @Deprecated
    public static float getPrimarySunIntensity() {
        return primarySunIntensity;
    }

    public static float getBlockFaceShade(Direction direction) {
        if (!isSpaceLightingActive()) {
            switch (direction) {
                case DOWN: return 0.5F;
                case UP: return 1.0F;
                case NORTH: case SOUTH: return 0.8F;
                case WEST: case EAST: return 0.6F;
                default: return 1.0F;
            }
        }

        ensureUpdated();

        int stepX = direction.getStepX();
        int stepY = direction.getStepY();
        int stepZ = direction.getStepZ();

        float dot = stepX * primarySunDirection.x() + stepY * primarySunDirection.y() + stepZ * primarySunDirection.z();

        float secDot = 0.0F;
        if (secondarySunIntensity > 0.05F) {
            secDot = Math.max(0.0F, stepX * secondarySunDirection.x() + stepY * secondarySunDirection.y() + stepZ * secondarySunDirection.z()) * secondarySunIntensity;
        }

        if (dot > 0.0F) {

            float directLight = SUN_DIRECT_MIN_SHADE + SUN_DIRECT_FACTOR * dot;
            return Math.min(1.0F, directLight + secDot * 0.2F);
        } else {

            return Math.min(1.0F, SHADOW_FACE_SHADE + secDot * 0.2F);
        }
    }

    private static void ensureUpdated() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.gameRenderer != null && mc.gameRenderer.getMainCamera() != null) {
            long currentTick = mc.level.getGameTime();
            if (currentTick != lastUpdateFrame) {
                lastUpdateFrame = currentTick;
                update(mc.gameRenderer.getMainCamera(), mc.level);
            }
        }
    }

    public static void adjustLightmapColors(ClientLevel level, float partialTick, float skyDarken, float blockLightRedFlicker, float skyLight, int pixelX, int pixelY, Vector3f colors) {
        if (!isSpaceLightingActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }

        ensureUpdated();

        if (pixelX == 15 && pixelY == 15) {
            return;
        }

        float skyFraction = Math.max(0.0F, Math.min(1.0F, pixelY / 15.0F));
        float blockFraction = Math.max(0.0F, Math.min(1.0F, pixelX / 15.0F));

        if (skyFraction > 0.0F) {
            float sunInfluence = skyFraction * (1.0F - blockFraction * 0.80F);

            float sunR = primarySunColor.x() * primarySunIntensity;
            float sunG = primarySunColor.y() * primarySunIntensity;
            float sunB = primarySunColor.z() * primarySunIntensity;

            float torchBoost = (float) Math.pow(blockFraction, TORCH_FALLOFF_EXPONENT) * TORCH_INTENSITY;

            float r = colors.x() * (1.0F + (sunR - 1.0F) * sunInfluence) + torchBoost * TORCH_BOOST_R;
            float g = colors.y() * (1.0F + (sunG - 1.0F) * sunInfluence) + torchBoost * TORCH_BOOST_G;
            float b = colors.z() * (1.0F + (sunB - 1.0F) * sunInfluence) + torchBoost * TORCH_BOOST_B;

            if (secondarySunIntensity > 0.15F) {
                r += secondarySunColor.x() * sunInfluence * (secondarySunIntensity * 0.2F);
                g += secondarySunColor.y() * sunInfluence * (secondarySunIntensity * 0.2F);
                b += secondarySunColor.z() * sunInfluence * (secondarySunIntensity * 0.2F);
            }

            colors.set(Math.min(1.0F, r), Math.min(1.0F, g), Math.min(1.0F, b));
        } else if (blockFraction > 0.0F) {

            float torchBoost = (float) Math.pow(blockFraction, TORCH_FALLOFF_EXPONENT) * TORCH_INTENSITY;
            float r = colors.x() + torchBoost * (TORCH_BOOST_R * 0.6F);
            float g = colors.y() + torchBoost * (TORCH_BOOST_G * 0.6F);
            float b = colors.z() + torchBoost * (TORCH_BOOST_B * 0.6F);
            colors.set(Math.min(1.0F, r), Math.min(1.0F, g), Math.min(1.0F, b));
        } else {

            colors.set(
                    Math.max(colors.x(), VOID_AMBIENT_R),
                    Math.max(colors.y(), VOID_AMBIENT_G),
                    Math.max(colors.z(), VOID_AMBIENT_B)
            );
        }
    }
}
