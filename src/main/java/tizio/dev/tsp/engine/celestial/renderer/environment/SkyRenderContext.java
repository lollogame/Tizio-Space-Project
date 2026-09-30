package tizio.dev.tsp.engine.celestial.renderer.environment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class SkyRenderContext {

    public static final float SKY_TILT_DEGREES = -32.5F;
    static final int MAX_COMETS = 3;
    static final List<CometData> activeComets = new ArrayList<>();
    static final RandomSource cometRandom = RandomSource.create();
    public static VertexBuffer atmosphereBuffer = null;
    public static VertexBuffer lineBuffer = null;
    static float tiltDegrees = 0.0F;
    static int ticks = 0;
    static float partialTick = 0.0F;
    static PoseStack poseStack = null;
    static Matrix4f projectionMatrix = null;
    static Runnable setupFog = null;
    static VertexBuffer abyssBuffer = null;
    static VertexBuffer deepSkyBuffer = null;
    static VertexBuffer skyboxBuffer = null;
    static VertexBuffer starBuffer = null;
    static int starAmount = 0;
    static int starSeed = 0;
    static long lastCometSpawnTime = 0L;

    private SkyRenderContext() {
    }

    static Quaternionf tiltRotation() {
        return new Quaternionf().rotationX((float) Math.toRadians(tiltDegrees));
    }

    static final class CometData {

        Vec3 startPos;
        Vec3 velocity;
        float age;
        float life;
        float size;
        int color;

        Deque<Vec3> history = new ArrayDeque<>();
    }
}