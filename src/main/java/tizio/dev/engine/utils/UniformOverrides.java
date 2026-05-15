package tizio.dev.engine.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UniformOverrides {

    public record Entry(float value, float min, float max) {}

    private static final Map<String, Entry> OVERRIDES = new LinkedHashMap<>();

    private UniformOverrides() {}

    public static void register(String name, float defaultValue, float min, float max) {
        OVERRIDES.putIfAbsent(name, new Entry(defaultValue, min, max));
    }

    public static boolean has(String name) { return OVERRIDES.containsKey(name); }
    public static float get(String name) { return OVERRIDES.get(name).value(); }
    public static Map<String, Entry> all() { return OVERRIDES; }

    public static void set(String name, float value) {
        Entry e = OVERRIDES.get(name);
        if (e != null) OVERRIDES.put(name, new Entry(value, e.min(), e.max()));
    }
}
