package tizio.dev.tsp.core.celestial.renderer.environment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

public final class SkyRenderContext {

    static int ticks = 0;
    static float partialTick = 0.0F;
    static PoseStack poseStack = null;
    static Matrix4f projectionMatrix = null;

    static Runnable setupFog = null;
    static VertexBuffer abyssBuffer = null;
    static VertexBuffer deepSkyBuffer = null;
    static VertexBuffer skyboxBuffer = null;
    public static VertexBuffer atmosphereBuffer = null;

    public static VertexBuffer lineBuffer = null;
    static VertexBuffer starBuffer = null;

    static int starAmount = 0;
    static int starSeed = 0;
    static final int MAX_COMETS = 3;
    static final List<CometData> activeComets = new ArrayList<>();
    static long lastCometSpawnTime = 0L;

    static final RandomSource cometRandom = RandomSource.create();
    static Class<?> effectsClass = null;
    static List<Predicate<Object[]>> customSky = null;

    private SkyRenderContext() {}

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
