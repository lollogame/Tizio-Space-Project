package tizio.dev.tsp.core.client;

import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.celestial.instance.elements.blackhole.BlackHoleInstance;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunInstance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ClientRenderRegistries {

    private static final List<ClientRenderInstanceRegistry<?>> ALL_REGISTRIES = new CopyOnWriteArrayList<>();

    public static final ClientRenderInstanceRegistry<PlanetInstance.AtmosphereInstance> ATMOSPHERES = create(MainClass.MODID);
    public static final ClientRenderInstanceRegistry<SunInstance> SUNS = create(MainClass.MODID);
    public static final ClientRenderInstanceRegistry<PlanetInstance.SurfaceInstance> PLANETS_SURFACES = create(MainClass.MODID);
    public static final ClientRenderInstanceRegistry<PlanetInstance.RingInstance> PLANETS_RINGS = create(MainClass.MODID);
    public static final ClientRenderInstanceRegistry<PlanetInstance.RingInstance.RocksInstance> PLANETS_RING_ROCKS = create(MainClass.MODID);
    public static final ClientRenderInstanceRegistry<BlackHoleInstance> BLACK_HOLES = create(MainClass.MODID);

    public static void clearAll() {
        for (ClientRenderInstanceRegistry<?> registry : ALL_REGISTRIES) {
            registry.clear();
        }
    }

    public static <T> ClientRenderInstanceRegistry<T> create(String defaultNamespace) {
        ClientRenderInstanceRegistry<T> registry = new ClientRenderInstanceRegistry<>(defaultNamespace);
        ALL_REGISTRIES.add(registry);
        return registry;
    }
}
