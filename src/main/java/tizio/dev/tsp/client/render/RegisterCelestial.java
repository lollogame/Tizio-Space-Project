package tizio.dev.tsp.client.render;

import tizio.dev.tsp.client.render.celestial.CelestialJsonLoader;

public final class RegisterCelestial {

    private RegisterCelestial() {
    }

    public static void register() {
        CelestialJsonLoader.rebuildForCurrentTime();
    }
}
