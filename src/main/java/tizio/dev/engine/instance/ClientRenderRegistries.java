package tizio.dev.engine.instance;

import tizio.dev.engine.elements.atmosphere.AtmosphereInstance;
import tizio.dev.engine.elements.blackhole.BlackHoleInstance;
import tizio.dev.engine.elements.planet.PlanetSurfaceInstance;
import tizio.dev.engine.elements.ring.PlanetRingInstance;
import tizio.dev.engine.elements.sun.SunInstance;
import tizio.dev.tsp.MainClass;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ClientRenderRegistries {

    private static final List<ClientRenderInstanceRegistry<?>> ALL_REGISTRIES = new CopyOnWriteArrayList<>();

    public static final ClientRenderInstanceRegistry<AtmosphereInstance> ATMOSPHERES = create(MainClass.MODID);
    public static final ClientRenderInstanceRegistry<SunInstance> SUNS = create(MainClass.MODID);
    public static final ClientRenderInstanceRegistry<PlanetSurfaceInstance> PLANETS_SURFACES = create(MainClass.MODID);
    public static final ClientRenderInstanceRegistry<PlanetRingInstance> PLANETS_RINGS = create(MainClass.MODID);
    public static final ClientRenderInstanceRegistry<BlackHoleInstance> BLACK_HOLES = create(MainClass.MODID);

    private ClientRenderRegistries() {

    }

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
