package tizio.dev.tsp.client.render.celestial;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;
import tizio.dev.engine.elements.atmosphere.AtmosphereInstance;
import tizio.dev.engine.elements.planet.PlanetSurfaceInstance;
import tizio.dev.engine.elements.ring.PlanetRingInstance;
import tizio.dev.engine.elements.sun.SunInstance;
import tizio.dev.engine.instance.ClientRenderRegistries;

import java.io.Reader;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class CelestialJsonLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern SAFE_ID = Pattern.compile("[a-z0-9_/-]+");
    private static final Pattern SAFE_TEXTURE = Pattern.compile("[a-z0-9_/-]+");
    private static final Instant J2000 = Instant.parse("2000-01-01T12:00:00Z");
    private static final double MILLIS_PER_DAY = 86_400_000.0D;
    private static final long REFRESH_INTERVAL_MS = 1_000L;
    private static final float DEFAULT_ATMOSPHERE_THICKNESS = 0.07F;
    private static final float DEFAULT_ATMOSPHERE_INTENSITY = 0.34F;
    private static final float DEFAULT_ATMOSPHERE_EXPOSURE = 3.25F;
    private static final float DEFAULT_RAYLEIGH_SCALE_HEIGHT = 0.0913F;
    private static final float DEFAULT_RAYLEIGH_STRENGTH = 0.0856F;
    private static final Vector3f DEFAULT_ATMOSPHERE_WAVELENGTHS = new Vector3f(1000F, 1000F, 1000F);
    private static final float MAX_PLANET_RADIUS = 128.0F;
    private static final float MAX_STAR_RADIUS = 100_000.0F;

    private static volatile LoadedData loadedData = LoadedData.EMPTY;
    private static volatile long nextRefreshMs = 0L;
    private static volatile ResourceLocation currentDimension = new ResourceLocation("minecraft", "overworld");

    private CelestialJsonLoader() {}

    public static LoadedData loadFromDatapacks(ResourceManager resourceManager) {
        List<LoadedJson> systems = loadJsonFolder(resourceManager, "solar_systems");
        List<LoadedJson> planets = loadJsonFolder(resourceManager, "planets");
        return new LoadedData(systems, planets);
    }

    public static void applyDatapackData(LoadedData data) {
        loadedData = data == null ? LoadedData.EMPTY : data;
        nextRefreshMs = 0L;
        rebuildForCurrentTime();
        LOGGER.info("Loaded {} solar system datapack json file(s) and {} single planet datapack json file(s)",
                loadedData.solarSystems().size(), loadedData.singlePlanets().size());
    }

    public static void setCurrentDimension(ResourceLocation dimension) {
        if (dimension != null && !dimension.equals(currentDimension)) {
            currentDimension = dimension;
            nextRefreshMs = 0L;
        }
    }

    public static void reloadFromClientResources() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        try {
            LoadedData data = loadFromDatapacks(minecraft.getResourceManager());
            applyDatapackData(data);
        } catch (Exception e) {
            LOGGER.error("Failed to reload celestial data from client resources", e);
        }
    }

    public static void ensureLoaded() {
        if (loadedData == LoadedData.EMPTY) {
            reloadFromClientResources();
        }
    }

    public static void rebuildForCurrentTime() {
        rebuildAt(Instant.now());
    }

    public static void refreshDynamicPositions() {
        long nowMs = System.currentTimeMillis();
        if (nowMs < nextRefreshMs) {
            return;
        }

        nextRefreshMs = nowMs + REFRESH_INTERVAL_MS;
        rebuildAt(Instant.ofEpochMilli(nowMs));
    }

    private static void rebuildAt(Instant now) {
        ClientRenderRegistries.clearAll();

        ResourceLocation dim = currentDimension;
        if (dim == null) {
            return;
        }

        for (LoadedJson system : loadedData.solarSystems()) {
            try {
                String dimStr = string(system.root(), "dimension", "minecraft:overworld");
                ResourceLocation sysDim = ResourceLocation.tryParse(dimStr);
                if (sysDim == null) {
                    sysDim = new ResourceLocation("minecraft", "overworld");
                }
                if (!dim.equals(sysDim)) {
                    continue;
                }
                loadSolarSystem(system, now);
            } catch (Exception exception) {
                LOGGER.error("Failed to build solar system '{}'", system.id(), exception);
            }
        }

        for (LoadedJson planet : loadedData.singlePlanets()) {
            try {
                String dimStr = string(planet.root(), "dimension", "minecraft:overworld");
                ResourceLocation planetDim = ResourceLocation.tryParse(dimStr);
                if (planetDim == null) {
                    planetDim = new ResourceLocation("minecraft", "overworld");
                }
                if (!dim.equals(planetDim)) {
                    continue;
                }
                loadSinglePlanet(planet, now);
            } catch (Exception exception) {
                LOGGER.error("Failed to build planet '{}'", planet.id(), exception);
            }
        }
    }

    private static List<LoadedJson> loadJsonFolder(ResourceManager resourceManager, String folder) {
        List<LoadedJson> loaded = new ArrayList<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(folder, id -> id.getPath().endsWith(".json"));
        LOGGER.info("CelestialJsonLoader searching folder '{}' found {} resource(s)", folder, resources.size());
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            LOGGER.info("CelestialJsonLoader found resource: {}", entry.getKey());
            ResourceLocation id = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    LOGGER.warn("Skipping celestial json '{}' because its root is not an object", id);
                    continue;
                }
                loaded.add(new LoadedJson(id, element.getAsJsonObject()));
                LOGGER.info("CelestialJsonLoader successfully loaded json '{}'", id);
            } catch (Exception exception) {
                LOGGER.error("Failed to read celestial json '{}'", id, exception);
            }
        }

        return loaded;
    }

    private static void loadSolarSystem(LoadedJson loaded, Instant now) {
        JsonObject root = loaded.root();
        String systemId = safeId(string(root, "id", stripJsonExtension(loaded.id())), stripJsonExtension(loaded.id()));
        String registryPrefix = loaded.id().getNamespace() + "/" + systemId;
        Vec3 origin = vec3(root, "origin", Vec3.ZERO);
        Map<String, Vec3> bodyPositions = new HashMap<>();

        JsonObject star = object(root, "star");
        if (star == null) {
            star = object(root, "sun");
        }

        Vec3 lightPosition = origin;
        if (star != null && bool(star, "enabled", true)) {
            String starId = safeId(string(star, "id", "sun"), "sun");
            Vec3 starPosition = resolvePosition(star, origin, origin, now);
            float radius = starRadius(star, 3.0F);

            registerStar(registryPrefix + "/" + starId, starPosition, radius, color(star, new Vector3f(1.0F, 0.9F, 0.65F)), rotation(star));
            bodyPositions.put(starId, starPosition);
            lightPosition = starPosition;
        }

        JsonArray bodies = array(root, "bodies");
        if (bodies != null) {
            for (JsonElement element : bodies) {
                if (!element.isJsonObject()) continue;
                JsonObject body = element.getAsJsonObject();
                String type = string(body, "type", "planet").toLowerCase(Locale.ROOT);
                String bodyId = safeId(string(body, "id", "body"), "body");
                String parentId = safeId(string(body, "parentId", string(body, "parent", "sun")), "sun");
                Vec3 parentPosition = bodyPositions.getOrDefault(parentId, lightPosition);
                Vec3 bodyPosition = resolvePosition(body, origin, parentPosition, now);

                if ("star".equals(type) || "sun".equals(type)) {
                    float radius = starRadius(body, 3.0F);
                    registerStar(registryPrefix + "/" + bodyId, bodyPosition, radius, color(body, new Vector3f(1.0F, 0.9F, 0.65F)), rotation(body));
                    lightPosition = bodyPosition;
                } else {
                    registerPlanet(registryPrefix + "/" + bodyId, body, bodyPosition, lightPosition);
                }

                bodyPositions.put(bodyId, bodyPosition);
                registerMoons(registryPrefix, body, bodyPosition, lightPosition, bodyPositions, now);
            }
        }

        JsonArray planets = array(root, "planets");
        if (planets != null) {
            for (JsonElement element : planets) {
                if (!element.isJsonObject()) continue;
                JsonObject planet = element.getAsJsonObject();
                String planetId = safeId(string(planet, "id", "planet"), "planet");
                String parentId = safeId(string(planet, "parentId", string(planet, "parent", "sun")), "sun");
                Vec3 parentPosition = bodyPositions.getOrDefault(parentId, lightPosition);
                Vec3 planetPosition = resolvePosition(planet, origin, parentPosition, now);

                registerPlanet(registryPrefix + "/" + planetId, planet, planetPosition, lightPosition);
                bodyPositions.put(planetId, planetPosition);
                registerMoons(registryPrefix, planet, planetPosition, lightPosition, bodyPositions, now);
            }
        }
    }

    private static void loadSinglePlanet(LoadedJson loaded, Instant now) {
        JsonObject root = loaded.root();
        String id = safeId(string(root, "id", stripJsonExtension(loaded.id())), stripJsonExtension(loaded.id()));
        Vec3 position = resolvePosition(root, Vec3.ZERO, Vec3.ZERO, now);
        Vec3 lightPosition = vec3(root, "lightFrom", position.add(64.0D, 0.0D, 0.0D));
        registerPlanet(loaded.id().getNamespace() + "/planets/" + id, root, position, lightPosition);
    }

    private static void registerMoons(String registryPrefix, JsonObject parentBody, Vec3 parentPosition, Vec3 lightPosition, Map<String, Vec3> bodyPositions, Instant now) {
        JsonArray moons = array(parentBody, "moons");
        if (moons == null) return;

        for (JsonElement element : moons) {
            if (!element.isJsonObject()) continue;
            JsonObject moon = element.getAsJsonObject();
            String moonId = safeId(string(moon, "id", "moon"), "moon");
            Vec3 moonPosition = resolvePosition(moon, Vec3.ZERO, parentPosition, now);

            registerPlanet(registryPrefix + "/" + moonId, moon, moonPosition, lightPosition);
            bodyPositions.put(moonId, moonPosition);
            registerMoons(registryPrefix, moon, moonPosition, lightPosition, bodyPositions, now);
        }
    }

    private static void registerPlanet(String id, JsonObject source, Vec3 position, Vec3 lightPosition) {
        JsonObject surface = object(source, "surface");
        boolean surfaceEnabled = surface == null || bool(surface, "enabled", true);
        float radius = radius(source, 1.0F);
        Vector3f color = color(source, new Vector3f(0.55F, 0.78F, 1.0F));
        Vec3 rotation = rotation(source);
        Vector3f lightDirection = lightDirection(lightPosition.toVector3f(), position.toVector3f());

        if (surfaceEnabled) {
            String dayTexture = texture(string(source, "texture", string(source, "dayTexture", string(surface, "dayTexture", "debug"))), "debug");
            String nightTexture = texture(string(source, "nightTexture", string(surface, "nightTexture", "")), "");
            PlanetSurfaceInstance.Builder builder = PlanetSurfaceInstance.at(position)
                    .planetRadius(radius)
                    .quadRadius(surfaceQuadRadius(radius))
                    .dayTexture(dayTexture)
                    .color(color)
                    .lightDirection(lightDirection)
                    .eulerDegrees((float) rotation.y, (float) rotation.x, (float) rotation.z);

            if (!nightTexture.isBlank()) {
                builder.nightTexture(nightTexture);
            }

            ClientRenderRegistries.PLANETS_SURFACES.put(id + "_surface", builder.build());
        }

        JsonObject ring = object(source, "ring");
        if (ring != null && bool(ring, "enabled", false)) {
            float defaultInnerRadius = radius * 0.35F;
            float defaultOuterRadius = radius * 1.30F;
            float innerRadius = (float) clamp(number(ring, "innerRadius", defaultInnerRadius), radius * 1.05F, radius * 8.0F);
            float outerRadius = (float) clamp(number(ring, "outerRadius", defaultOuterRadius), innerRadius + radius * 0.15F, radius * 10.0F);
            float quadRadius = outerRadius * 1.04F;
            String ringTexture = texture(string(ring, "texture", "saturn_ring"), "saturn_ring");
            Vec3 ringRotation = rotation(ring, rotation);

            ClientRenderRegistries.PLANETS_RINGS.put(
                    id + "_ring",
                    PlanetRingInstance.at(position)
                            .planetRadius(radius)
                            .quadRadius(quadRadius)
                            .ringInnerRadius(innerRadius)
                            .ringOuterRadius(outerRadius)
                            .ringTexture(ringTexture)
                            .color(color(ring, new Vector3f(1.0F, 1.0F, 1.0F)))
                            .lightDirection(lightDirection)
                            .eulerDegrees((float) ringRotation.y, (float) ringRotation.x, (float) ringRotation.z)
                            .build()
            );
        }

        JsonObject atmosphere = object(source, "atmosphere");
        if (atmosphere != null && bool(atmosphere, "enabled", true)) {
            float atmosphereRadius = resolveAtmosphereRadius(atmosphere, radius);
            float atmosphereThickness = Math.max(atmosphereRadius - radius, radius * 0.01F);
            float atmosphereQuadRadius = atmosphereRadius + atmosphereThickness * 0.18F;

            ClientRenderRegistries.ATMOSPHERES.put(
                    id + "_atmosphere",
                    AtmosphereInstance.at(position)
                            .planetRadius(radius)
                            .quadRadius(atmosphereQuadRadius)
                            .atmosphereRadius(atmosphereRadius)
                            .exposure(DEFAULT_ATMOSPHERE_EXPOSURE)
                            .intensity(DEFAULT_ATMOSPHERE_INTENSITY)
                            .rayleighScaleHeight(DEFAULT_RAYLEIGH_SCALE_HEIGHT)
                            .rayleighStrength(DEFAULT_RAYLEIGH_STRENGTH)
                            .color(color(atmosphere, color))
                            .waveLengths(wavelengths(atmosphere, DEFAULT_ATMOSPHERE_WAVELENGTHS))
                            .lightDirection(lightDirection)
                            .eulerDegrees((float) rotation.y, (float) rotation.x, (float) rotation.z)
                            .build()
            );
        }
    }

    private static void registerStar(String id, Vec3 position, float radius, Vector3f color, Vec3 rotation) {
        ClientRenderRegistries.SUNS.put(
                id,
                SunInstance.at(position)
                        .planetRadius(radius)
                        .sunRadius(radius * 1.95F)
                        .quadRadius(radius * 1.8F)
                        .densityFalloff(30.0F)
                        .scatteringStrength(0.5F)
                        .color(color)
                        .eulerDegrees((float) rotation.y, (float) rotation.x, (float) rotation.z)
                        .build()
        );
    }

    private static Vec3 resolvePosition(JsonObject source, Vec3 origin, Vec3 parentPosition, Instant now) {
        if (source != null && has(source, "position")) {
            return origin.add(vec3(source, "position", Vec3.ZERO));
        }

        JsonObject orbit = object(source, "orbit");
        boolean orbitEnabled = orbit == null || bool(orbit, "enabled", true);
        if (!orbitEnabled) {
            return parentPosition;
        }

        double radius = number(source, "orbitRadius", number(source, "distance", number(orbit, "radius", number(orbit, "distance", 0.0D))));
        double angleDeg = orbitAngle(source, orbit, now);
        double inclinationDeg = number(source, "inclinationDeg", number(orbit, "inclinationDeg", 0.0D));
        double ascendingNodeDeg = number(source, "ascendingNodeDeg", number(orbit, "ascendingNodeDeg", 0.0D));
        double verticalOffset = number(source, "height", number(orbit, "verticalOffset", 0.0D));

        Vec3 offset = orbitOffset(radius, angleDeg, inclinationDeg, ascendingNodeDeg, verticalOffset);
        return parentPosition.add(offset);
    }

    private static double orbitAngle(JsonObject source, JsonObject orbit, Instant now) {
        double staticAngle = number(source, "orbitAngleDeg", number(orbit, "angleDeg", number(orbit, "phaseDeg", 0.0D)));
        double periodDays = number(source, "orbitalPeriodDays", number(orbit, "periodDays", 0.0D));
        if (periodDays <= 0.0D) {
            return staticAngle;
        }

        double epochAngleDeg = number(source, "epochAngleDeg", number(orbit, "epochAngleDeg", staticAngle));
        Instant epoch = epoch(source, orbit);
        double elapsedDays = (now.toEpochMilli() - epoch.toEpochMilli()) / MILLIS_PER_DAY;
        return epochAngleDeg + elapsedDays * (360.0D / periodDays);
    }

    private static Instant epoch(JsonObject source, JsonObject orbit) {
        String sourceEpoch = string(source, "epochUtc", "");
        String orbitEpoch = string(orbit, "epochUtc", sourceEpoch);
        if (orbitEpoch.isBlank() || "j2000".equalsIgnoreCase(orbitEpoch)) {
            return J2000;
        }

        try {
            return Instant.parse(orbitEpoch);
        } catch (DateTimeParseException exception) {
            return J2000;
        }
    }

    private static Vec3 orbitOffset(double radius, double angleDeg, double inclinationDeg, double ascendingNodeDeg, double verticalOffset) {
        radius = clamp(radius, 0.0D, 10000.0D);
        double angle = Math.toRadians(angleDeg);
        double x = Math.cos(angle) * radius;
        double y = verticalOffset;
        double z = Math.sin(angle) * radius;

        double inclination = Math.toRadians(inclinationDeg);
        double inclinedY = y * Math.cos(inclination) - z * Math.sin(inclination);
        double inclinedZ = y * Math.sin(inclination) + z * Math.cos(inclination);

        double node = Math.toRadians(ascendingNodeDeg);
        double nodeX = x * Math.cos(node) + inclinedZ * Math.sin(node);
        double nodeZ = -x * Math.sin(node) + inclinedZ * Math.cos(node);

        return new Vec3(nodeX, inclinedY, nodeZ);
    }

    private static float radius(JsonObject source, float fallback) {
        JsonObject surface = object(source, "surface");
        return (float) clamp(number(source, "radius", number(surface, "radius", fallback)), 0.05F, MAX_PLANET_RADIUS);
    }

    private static float starRadius(JsonObject source, float fallback) {
        return (float) clamp(number(source, "radius", fallback), 0.05F, MAX_STAR_RADIUS);
    }

    private static float surfaceQuadRadius(float radius) {
        return Math.max(radius * 1.75F, radius + 0.25F);
    }

    private static Vector3f lightDirection(Vector3f sunPosition, Vector3f planetPosition) {
        Vector3f dir = new Vector3f(sunPosition).sub(planetPosition);
        float len = dir.length();
        if (len < 1e-6f) return new Vector3f(0.0f, 1.0f, 0.0f);
        return dir.div(len);
    }

    private static float resolveAtmosphereRadius(JsonObject atmosphere, float planetRadius) {
        float defaultRadius = planetRadius * (1.0F + DEFAULT_ATMOSPHERE_THICKNESS);
        float minRadius = planetRadius * 0.0F;
        float maxRadius = planetRadius * 3.0F;
        if (atmosphere == null || !has(atmosphere, "atmosphereRadius")) {
            float thicknessRatio = (float) clamp(number(atmosphere, "thickness", DEFAULT_ATMOSPHERE_THICKNESS), 0.01F, 2.0F);
            return (float) clamp(planetRadius * (1.0F + thicknessRatio), minRadius, maxRadius);
        }

        float raw = (float) number(atmosphere, "atmosphereRadius", defaultRadius);
        if (raw <= 0.0F) {
            return defaultRadius;
        }

        if (raw <= 1.0F) {
            return (float) clamp(planetRadius * (1.0F + raw), minRadius, maxRadius);
        }

        if (raw <= 4.0F) {
            return (float) clamp(planetRadius * raw, minRadius, maxRadius);
        }

        return (float) clamp(raw, minRadius, maxRadius);
    }

    private static Vector3f wavelengths(JsonObject atmosphere, Vector3f fallback) {
        Vector3f parsed = wavelengths(atmosphere, "wavelengths", fallback);
        if (parsed != null) {
            return parsed;
        }

        parsed = wavelengths(atmosphere, "waveLengths", fallback);
        if (parsed != null) {
            return parsed;
        }

        return new Vector3f(fallback);
    }

    private static Vector3f wavelengths(JsonObject source, String name, Vector3f fallback) {
        if (source == null || !has(source, name)) {
            return null;
        }

        JsonElement element = source.get(name);
        if (element.isJsonArray()) {
            JsonArray values = element.getAsJsonArray();
            if (values.size() >= 3) {
                return new Vector3f(
                        (float) clamp(values.get(0).getAsDouble(), 1.0F, 4000.0F),
                        (float) clamp(values.get(1).getAsDouble(), 1.0F, 4000.0F),
                        (float) clamp(values.get(2).getAsDouble(), 1.0F, 4000.0F)
                );
            }
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            return new Vector3f(
                    (float) clamp(number(obj, "r", number(obj, "red", fallback.x)), 1.0F, 4000.0F),
                    (float) clamp(number(obj, "g", number(obj, "green", fallback.y)), 1.0F, 4000.0F),
                    (float) clamp(number(obj, "b", number(obj, "blue", fallback.z)), 1.0F, 4000.0F)
            );
        }

        return null;
    }

    private static Vec3 rotation(JsonObject source) {
        return rotation(source, Vec3.ZERO);
    }

    private static Vec3 rotation(JsonObject source, Vec3 fallback) {
        JsonObject rotation = object(source, "rotation");
        if (rotation == null) {
            rotation = object(source, "tilt");
        }
        return vec3(rotation, fallback);
    }

    private static Vector3f color(JsonObject source, Vector3f fallback) {
        JsonObject colorObject = object(source, "color");
        if (colorObject == null) {
            JsonObject surface = object(source, "surface");
            colorObject = object(surface, "color");
        }

        if (colorObject != null) {
            return new Vector3f(
                    (float) clamp(number(colorObject, "r", number(colorObject, "yaw", fallback.x)), 0.0F, 1.0F),
                    (float) clamp(number(colorObject, "g", number(colorObject, "pitch", fallback.y)), 0.0F, 1.0F),
                    (float) clamp(number(colorObject, "b", number(colorObject, "roll", fallback.z)), 0.0F, 1.0F)
            );
        }

        JsonArray colorArray = array(source, "color");
        if (colorArray != null && colorArray.size() >= 3) {
            return new Vector3f(
                    (float) clamp(colorArray.get(0).getAsDouble(), 0.0F, 1.0F),
                    (float) clamp(colorArray.get(1).getAsDouble(), 0.0F, 1.0F),
                    (float) clamp(colorArray.get(2).getAsDouble(), 0.0F, 1.0F)
            );
        }

        String hex = string(source, "color", "");
        if (hex.startsWith("#") && hex.length() == 7) {
            try {
                int red = Integer.parseInt(hex.substring(1, 3), 16);
                int green = Integer.parseInt(hex.substring(3, 5), 16);
                int blue = Integer.parseInt(hex.substring(5, 7), 16);
                return new Vector3f(red / 255.0F, green / 255.0F, blue / 255.0F);
            } catch (NumberFormatException ignored) {
                return new Vector3f(fallback);
            }
        }

        return new Vector3f(fallback);
    }

    private static Vec3 vec3(JsonObject source, String name, Vec3 fallback) {
        if (source == null || !has(source, name)) {
            return fallback;
        }

        JsonElement element = source.get(name);
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() >= 3) {
                return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
            }
        }

        if (element.isJsonObject()) {
            return vec3(element.getAsJsonObject(), fallback);
        }

        return fallback;
    }

    private static Vec3 vec3(JsonObject source, Vec3 fallback) {
        if (source == null) {
            return fallback;
        }

        return new Vec3(
                clamp(number(source, "yaw", fallback.x), -10000.0D, 10000.0D),
                clamp(number(source, "pitch", fallback.y), -10000.0D, 10000.0D),
                clamp(number(source, "roll", fallback.z), -10000.0D, 10000.0D)
        );
    }

    private static String safeId(String value, String fallback) {
        String normalized = normalize(value);
        if (!normalized.isBlank() && SAFE_ID.matcher(normalized).matches() && !normalized.contains("..")) {
            return normalized;
        }
        String safeFallback = normalize(fallback).replaceAll("[^a-z0-9_/-]", "_");
        return safeFallback.isBlank() ? "unnamed" : safeFallback;
    }

    private static String texture(String value, String fallback) {
        String normalized = normalize(value)
                .replace('\\', '/')
                .replace("textures/planets/", "");

        if (normalized.endsWith(".png")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }

        if (!normalized.isBlank() && SAFE_TEXTURE.matcher(normalized).matches() && !normalized.contains("..")) {
            return normalized;
        }

        return normalize(fallback);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static JsonObject object(JsonObject source, String name) {
        if (source == null || !has(source, name) || !source.get(name).isJsonObject()) {
            return null;
        }
        return source.getAsJsonObject(name);
    }

    private static JsonArray array(JsonObject source, String name) {
        if (source == null || !has(source, name) || !source.get(name).isJsonArray()) {
            return null;
        }
        return source.getAsJsonArray(name);
    }

    private static String string(JsonObject source, String name, String fallback) {
        if (source == null || !has(source, name) || !source.get(name).isJsonPrimitive()) {
            return fallback;
        }
        return source.get(name).getAsString();
    }

    private static double number(JsonObject source, String name, double fallback) {
        if (source == null || !has(source, name) || !source.get(name).isJsonPrimitive()) {
            return fallback;
        }

        try {
            return source.get(name).getAsDouble();
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject source, String name, boolean fallback) {
        if (source == null || !has(source, name) || !source.get(name).isJsonPrimitive()) {
            return fallback;
        }
        return source.get(name).getAsBoolean();
    }

    private static boolean has(JsonObject source, String name) {
        return source != null && source.has(name) && !source.get(name).isJsonNull();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String stripJsonExtension(ResourceLocation id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    public record LoadedData(List<LoadedJson> solarSystems, List<LoadedJson> singlePlanets) {
        public static final LoadedData EMPTY = new LoadedData(List.of(), List.of());
    }

    public record LoadedJson(ResourceLocation id, JsonObject root) {
    }

}
