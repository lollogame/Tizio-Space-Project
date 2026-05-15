package tizio.dev.engine.utils;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.concurrent.ThreadLocalRandom;

public class Color {

    public static final Vec3 WRGB = new Vec3(255, 255, 255);
    public static final Vec3 MAX_WAVE_LENGHT = new Vec3(1000, 1000, 1000);
    public static final Vector4f WRGBA = new Vector4f(255, 255, 255, 255);

    public static Vector4f RGBA(int R, int G, int B, int A) {
        return new Vector4f(R, G, B, A);
    }

    public static Vector4f RGBA(Vec3 color, int A) {
        return new Vector4f((float) color.x, (float) color.y, (float) color.z, A);
    }

    public static Vec3 RGB(int R, int G, int B) {
        return new Vec3(R, G, B);
    }

    public static int iRGBA(int r, int g, int b, int a) {
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        a = Math.max(0, Math.min(255, a));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Deprecated
    public static Vector4f mix(Vector4f baseColor, Vec3 withThisColor, float influence) {
        influence = Mth.clamp(influence, 0.0f, 1.0f);
        float r = Mth.lerp(influence, baseColor.x, (float) withThisColor.x);
        float g = Mth.lerp(influence, baseColor.y, (float) withThisColor.y);
        float b = Mth.lerp(influence, baseColor.z, (float) withThisColor.z);
        return new Vector4f(r, g, b, baseColor.w);
    }

    public static Vector3f randomRGB() {
        return new Vector3f(
                ThreadLocalRandom.current().nextFloat(),
                ThreadLocalRandom.current().nextFloat(),
                ThreadLocalRandom.current().nextFloat()
        );
    }
}
