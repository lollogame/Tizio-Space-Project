package tizio.dev.tsp.core.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunInstance;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.engine.client.ClientRenderRegistries;

public final class NearestSunSelector {

    private static final int MAX_SUNS = 4;
    private static final ThreadLocal<SunInstance[]> SCRATCH = ThreadLocal.withInitial(() -> new SunInstance[MAX_SUNS]);
    private static final ThreadLocal<double[]> DISTANCES = ThreadLocal.withInitial(() -> new double[MAX_SUNS]);

    public static SunInstance[] select(Vec3 position) {

        SunInstance[] result = SCRATCH.get();
        double[] distances = DISTANCES.get();

        for (int i = 0; i < MAX_SUNS; i++) {
            result[i] = null;
            distances[i] = Double.POSITIVE_INFINITY;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!CelestialJsonLoader.isSpaceDimension(mc.level)) {
            return result;
        }

        for (SunInstance sun : ClientRenderRegistries.SUNS.instances()) {
            Vec3 sunPosition = sun.position();
            double dx = sunPosition.x - position.x;
            double dy = sunPosition.y - position.y;
            double dz = sunPosition.z - position.z;
            double distance = dx * dx + dy * dy + dz * dz;

            for (int i = 0; i < MAX_SUNS; i++) {
                if (distance >= distances[i]) {
                    continue;
                }
                for (int j = MAX_SUNS - 1; j > i; j--) {
                    result[j] = result[j - 1];
                    distances[j] = distances[j - 1];
                }
                result[i] = sun;
                distances[i] = distance;
                break;
            }
        }

        return result;
    }
}
