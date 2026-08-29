package tizio.dev.tsp.core.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.joml.Vector3f;
import org.slf4j.Logger;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.celestial.instance.elements.blackhole.BlackHoleInstance;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.celestial.instance.elements.planet.ring.PlanetRingRockRenderer;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunInstance;
import tizio.dev.tsp.core.client.ClientRenderRegistries;

import java.io.File;
import java.io.Reader;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

public final class CelestialJsonLoader {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern SAFE_ID = Pattern.compile("[a-z0-9_/-]+");
    private static final Pattern SAFE_TEXTURE = Pattern.compile("[a-z0-9_/:.-]+");
    private static final Instant J2000 = Instant.parse("2000-01-01T12:00:00Z");
    private static final double MILLIS_PER_DAY = 86_400_000.0D;
    private static final long REFRESH_INTERVAL_MS = 1_000L;

    private static final float DEFAULT_ATMOSPHERE_THICKNESS = 0.07F;
    private static final float DEFAULT_ATMOSPHERE_INTENSITY = 0.34F;
    private static final float DEFAULT_ATMOSPHERE_EXPOSURE = 3.25F;
    private static final float DEFAULT_RAYLEIGH_SCALE_HEIGHT = 0.0913F;
    private static final float DEFAULT_RAYLEIGH_STRENGTH = 0.0856F;

    private static final Vector3f DEFAULT_ATMOSPHERE_WAVELENGTHS = new Vector3f(1000F, 1000F, 1000F);
    private static final float MAX_PLANET_RADIUS = 10_000.0F;
    private static final float MAX_STAR_RADIUS = 100_000.0F;

    private static final Map<String, SolarSystemData> activeSystems = new LinkedHashMap<>();
    private static volatile LoadedData loadedData = LoadedData.EMPTY;
    private static volatile long nextRefreshMs = 0L;
    private static volatile ResourceLocation currentDimension = new ResourceLocation(MainClass.MODID, "space");
    private static volatile String activeSelectedSystemId = null;

    public static String getActiveSelectedSystemId() {
        return activeSelectedSystemId;
    }

    public static void setActiveSelectedSystemId(String systemId) {
        if (systemId != null && !systemId.equals(activeSelectedSystemId)) {
            activeSelectedSystemId = systemId;
            forceFullRebuild();
        }
    }

    public static Map<String, SolarSystemData> getActiveSystems() {
        return activeSystems;
    }

    public static SolarSystemData getActiveSystem(String systemId) {
        return activeSystems.get(systemId);
    }

    public static void updateSystemInMemory(SolarSystemData config, boolean rebuildNow) {
        if (config != null && config.id != null) {
            activeSystems.put(config.id, config);
            if (rebuildNow) {
                forceFullRebuild();
            }
        }
    }

    public static void removeSystemInMemory(String systemId) {
        if (systemId != null && activeSystems.containsKey(systemId)) {
            activeSystems.remove(systemId);
            forceFullRebuild();
        }
    }

    public static File exportActiveSystem(String systemId) throws Exception {
        SolarSystemData config = getActiveSystem(systemId);
        if (config == null) {
            throw new IllegalArgumentException("Solar system not found: " + systemId);
        }
        File gameDir = FMLPaths.GAMEDIR.get().toFile();
        File exportFile = new File(gameDir, "config/tsp/exported_solar_systems/" + config.id + ".json");
        return CelestialJsonExporter.exportToFile(config, exportFile);
    }

    public static LoadedData loadFromDatapacks(ResourceManager resourceManager) {
        List<LoadedJson> systems = loadJsonFolder(resourceManager, "solar_systems");
        List<LoadedJson> planets = loadJsonFolder(resourceManager, "planets");
        return new LoadedData(systems, planets);
    }

    public static void applyDatapackData(LoadedData data) {
        loadedData = data == null ? LoadedData.EMPTY : data;
        activeSystems.clear();

        for (LoadedJson systemJson : loadedData.solarSystems()) {
            try {
                SolarSystemData systemConfig = parseSolarSystem(systemJson.id(), systemJson.root());
                if (systemConfig != null) {
                    activeSystems.put(systemConfig.id, systemConfig);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse solar system json '{}'", systemJson.id(), e);
            }
        }

        forceFullRebuild();
        LOGGER.info("Loaded {} solar system(s) into active memory", activeSystems.size());
    }

    public static void setCurrentDimension(ResourceLocation dimension) {
        if (dimension != null && !dimension.equals(currentDimension)) {
            currentDimension = dimension;
            forceFullRebuild();
        }
    }

    public static void reloadFromClientResources() {
        if (!isClientSide()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        try {
            applyDatapackData(loadFromDatapacks(minecraft.getResourceManager()));
        } catch (Exception e) {
            LOGGER.error("Failed to reload system data from client resources", e);
        }
    }

    public static void ensureLoaded() {
        if (activeSystems.isEmpty() && loadedData == LoadedData.EMPTY) {
            reloadFromClientResources();
        }
    }

    public static void forceFullRebuild() {
        if (!isClientSide()) {
            return;
        }
        ClientRenderRegistries.clearAll();
        PlanetRingRockRenderer.clearCache();
        nextRefreshMs = 0L;
        rebuildForCurrentTime();
    }

    public static void rebuildForCurrentTime() {
        if (!isClientSide()) {
            return;
        }
        rebuildAt(Instant.now());
    }

    public static void refreshDynamicPositions() {
        if (!isClientSide()) {
            return;
        }
        long nowMs = System.currentTimeMillis();
        if (nowMs < nextRefreshMs) {
            return;
        }

        nextRefreshMs = nowMs + REFRESH_INTERVAL_MS;
        rebuildAt(Instant.ofEpochMilli(nowMs));
    }

    private static void rebuildAt(Instant now) {
        ResourceLocation dim = currentDimension;
        if (dim == null || activeSystems.isEmpty()) {
            ClientRenderRegistries.clearAll();
            PlanetRingRockRenderer.clearCache();
            return;
        }

        SolarSystemData systemToBuild = null;
        if (activeSelectedSystemId != null) {
            systemToBuild = activeSystems.get(activeSelectedSystemId);
        }

        if (systemToBuild == null) {
            for (SolarSystemData system : activeSystems.values()) {
                ResourceLocation sysDim = ResourceLocation.tryParse(system.dimension);
                if (sysDim == null) {
                    sysDim = new ResourceLocation("minecraft", "overworld");
                }
                if (dim.equals(sysDim)) {
                    systemToBuild = system;
                    activeSelectedSystemId = system.id;
                    break;
                }
            }
        }

        if (systemToBuild != null) {
            try {
                Set<ResourceLocation> activeStarKeys = new HashSet<>();
                Set<ResourceLocation> activeBlackHoleKeys = new HashSet<>();
                Set<ResourceLocation> activeSurfaceKeys = new HashSet<>();
                Set<ResourceLocation> activeRingKeys = new HashSet<>();
                Set<ResourceLocation> activeRockKeys = new HashSet<>();
                Set<ResourceLocation> activeAtmosKeys = new HashSet<>();

                buildSolarSystem(systemToBuild, now, activeStarKeys, activeBlackHoleKeys, activeSurfaceKeys, activeRingKeys, activeRockKeys, activeAtmosKeys);

                ClientRenderRegistries.SUNS.retainAll(activeStarKeys);
                ClientRenderRegistries.BLACK_HOLES.retainAll(activeBlackHoleKeys);
                ClientRenderRegistries.PLANETS_SURFACES.retainAll(activeSurfaceKeys);
                ClientRenderRegistries.PLANETS_RINGS.retainAll(activeRingKeys);
                ClientRenderRegistries.PLANETS_RING_ROCKS.retainAll(activeRockKeys);
                ClientRenderRegistries.ATMOSPHERES.retainAll(activeAtmosKeys);

                PlanetRingRockRenderer.cleanupCache(ClientRenderRegistries.PLANETS_RING_ROCKS.instances());

            } catch (Exception exception) {
                LOGGER.error("Failed to build solar system '{}'", systemToBuild.id, exception);
            }
        } else {
            ClientRenderRegistries.clearAll();
            PlanetRingRockRenderer.clearCache();
        }
    }

    private static boolean isClientSide() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    private static List<LoadedJson> loadJsonFolder(ResourceManager resourceManager, String folder) {
        List<LoadedJson> loaded = new ArrayList<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(folder, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    continue;
                }
                loaded.add(new LoadedJson(id, element.getAsJsonObject()));
            } catch (Exception exception) {
                LOGGER.error("Failed to read system json '{}'", id, exception);
            }
        }
        return loaded;
    }

    public static SolarSystemData parseSolarSystem(ResourceLocation resourceId, JsonObject root) {
        String systemId = safeId(string(root, "id", stripJsonExtension(resourceId)), stripJsonExtension(resourceId));
        String dimension = string(root, "dimension", "tsp:space");
        Vec3 originVec = vec3(root, "origin", new Vec3(0.0, 500.0, 0.0));

        SolarSystemData config = new SolarSystemData(systemId, dimension);
        config.originX = originVec.x;
        config.originY = originVec.y;
        config.originZ = originVec.z;
        config.globalScale = (float) number(root, "globalScale", 1.0D);
        config.gravity = (float) number(root, "gravity", 0.0D);

        JsonObject starObj = object(root, "star");
        if (starObj == null) {
            starObj = object(root, "sun");
        }
        if (starObj != null) {
            config.star = parseStar(starObj);
        }

        JsonArray bodiesArr = array(root, "bodies");
        if (bodiesArr != null) {
            for (JsonElement elem : bodiesArr) {
                if (!elem.isJsonObject()) continue;
                PlanetInstance.Config body = parseBody(elem.getAsJsonObject(), "planet", "sun");
                if (body != null) {
                    config.bodies.add(body);
                }
            }
        }

        return config;
    }

    private static SunInstance.Config parseStar(JsonObject starObj) {
        SunInstance.Config star = new SunInstance.Config();
        star.id = safeId(string(starObj, "id", "sun"), "sun");
        star.type = string(starObj, "type", "star");
        star.radius = (float) number(starObj, "radius", 2000.0);
        star.colorHex = colorHex(starObj, "#ffd48a");
        star.enabled = bool(starObj, "enabled", true);
        star.diskRotationSpeed = (float) number(starObj, "diskRotationSpeed", 0.20D);
        star.intensity = (float) number(starObj, "intensity", 1.0D);
        Vec3 rot = rotation(starObj);
        star.yaw = (float) rot.x;
        star.pitch = (float) rot.y;
        star.roll = (float) rot.z;
        return star;
    }

    private static PlanetInstance.Config parseBody(JsonObject obj, String defaultType, String defaultParent) {
        String id = safeId(string(obj, "id", "body"), "body");
        String type = string(obj, "type", defaultType);
        String parentId = safeId(string(obj, "parentId", string(obj, "parent", defaultParent)), defaultParent);
        float radius = radius(obj, 100.0F);
        String texture = texture(string(obj, "texture", string(obj, "dayTexture", "earth_mat_0")), "earth_mat_0");
        String nightTexture = texture(string(obj, "nightTexture", ""), "");
        String color = colorHex(obj, "#7fb8ff");
        Vec3 rot = rotation(obj);

        PlanetInstance.Config body = new PlanetInstance.Config(id, type, parentId, radius, texture, color);
        body.dimension = string(obj, "dimension", "");
        body.gravity = (float) number(obj, "gravity", 9.81D);
        body.oxygen = bool(obj, "oxygen", bool(object(obj, "surface"), "oxygen", false));
        body.temperature = (float) clamp(number(obj, "temperature", number(object(obj, "surface"), "temperature", 0.0D)), -1.0D, 1.0D);
        body.nightTexture = nightTexture;
        body.yaw = (float) rot.x;
        body.pitch = (float) rot.y;
        body.roll = (float) rot.z;
        body.diskRotationSpeed = (float) number(obj, "diskRotationSpeed", 0.20D);
        body.intensity = (float) number(obj, "intensity", 1.0D);

        JsonObject spinObj = object(obj, "orbit");
        body.spinHours = number(obj, "spinHours", number(obj, "rotationHours", number(obj, "spinPeriodHours",
                number(obj, "spinPeriodDays", number(obj, "rotationPeriodDays", number(spinObj, "spinPeriodDays", 1.0D))) * 24.0D)));

        JsonObject orbitObj = object(obj, "orbit");
        body.orbit = new PlanetInstance.Orbit();
        body.orbit.radius = number(obj, "orbitRadius", number(obj, "distance", number(orbitObj, "radius", number(orbitObj, "distance", 0.0D))));
        body.orbit.periodDays = number(obj, "orbitalPeriodDays", number(orbitObj, "periodDays", 0.0D));
        body.orbit.epochAngle = number(obj, "epochAngleDeg", number(orbitObj, "epochAngle", number(orbitObj, "angleDeg", 0.0D)));
        body.orbit.epochUtc = string(obj, "epochUtc", string(orbitObj, "epochUtc", "2000-01-01T12:00:00Z"));
        body.orbit.inclination = number(obj, "inclinationDeg", number(orbitObj, "inclination", 0.0D));
        body.orbit.ascendingNode = number(obj, "ascendingNodeDeg", number(orbitObj, "ascendingNode", 0.0D));
        body.orbit.verticalOffset = number(obj, "height", number(orbitObj, "verticalOffset", 0.0D));
        body.orbit.enabled = orbitObj == null || bool(orbitObj, "enabled", true);

        JsonObject skyObj = object(obj, "sky");
        body.sky = new PlanetInstance.Sky();
        if (skyObj != null) {
            body.sky.skyboxRotation = bool(skyObj, "skyboxRotation", false);
            body.sky.skyboxConstant = bool(skyObj, "skyboxConstant", false);
            body.sky.skyboxTexture = texture(string(skyObj, "skyboxTexture", "milky_way"), "milky_way");
            body.sky.starsEnabled = bool(skyObj, "starsEnabled", false);
            body.sky.starsAmount = (int) number(skyObj, "starsAmount", 5000.0D);
            body.sky.starsSeed = (int) number(skyObj, "starsSeed", 0.0D);
            body.sky.starsColorHex = colorHex(skyObj, "#ffffff");
            body.sky.groundMode = bool(skyObj, "groundMode", false);
        }

        JsonObject fogObj = object(obj, "fog");
        body.fog = new PlanetInstance.SurfaceInstance.SurfaceFog();
        if (fogObj != null) {
            body.fog.enabled = bool(fogObj, "enabled", true);
            body.fog.colorHex = colorHex(fogObj, "#000000");
            body.fog.shape = string(fogObj, "shape", "CYLINDER");
            body.fog.startDistance = (float) number(fogObj, "startDistance", 0.0D);
            body.fog.endDistance = (float) number(fogObj, "endDistance", 192.0D);
            body.fog.useRenderDistance = bool(fogObj, "useRenderDistance", false);
        }

        if (body.isBlackHole()) {
            body.atmosphere = new PlanetInstance.Atmosphere();
            body.atmosphere.enabled = false;
            body.ring = new PlanetInstance.Ring();
            body.ring.enabled = false;
        } else {
            JsonObject atmosObj = object(obj, "atmosphere");
            if (atmosObj != null && bool(atmosObj, "enabled", true)) {
                body.atmosphere = new PlanetInstance.Atmosphere();
                body.atmosphere.enabled = true;
                body.atmosphere.thickness = (float) number(atmosObj, "thickness", number(atmosObj, "atmosphereRadius", 0.23D));
                body.atmosphere.exposure = (float) number(atmosObj, "exposure", DEFAULT_ATMOSPHERE_EXPOSURE);
                body.atmosphere.intensity = (float) number(atmosObj, "intensity", DEFAULT_ATMOSPHERE_INTENSITY);
                body.atmosphere.rayleighScaleHeight = (float) number(atmosObj, "rayleighScaleHeight", DEFAULT_RAYLEIGH_SCALE_HEIGHT);
                body.atmosphere.rayleighStrength = (float) number(atmosObj, "rayleighStrength", DEFAULT_RAYLEIGH_STRENGTH);
                Vector3f wl = wavelengths(atmosObj, DEFAULT_ATMOSPHERE_WAVELENGTHS);
                body.atmosphere.wavelengthR = wl.x;
                body.atmosphere.wavelengthG = wl.y;
                body.atmosphere.wavelengthB = wl.z;
                body.atmosphere.colorHex = colorHex(atmosObj, color);
            } else {
                body.atmosphere = new PlanetInstance.Atmosphere();
                body.atmosphere.enabled = false;
            }

            JsonObject ringObj = object(obj, "ring");
            if (ringObj != null && bool(ringObj, "enabled", false)) {
                body.ring = new PlanetInstance.Ring();
                body.ring.enabled = true;
                body.ring.innerRadius = (float) number(ringObj, "innerRadius", radius * 1.2F);
                body.ring.outerRadius = (float) number(ringObj, "outerRadius", radius * 2.0F);
                body.ring.texture = texture(string(ringObj, "texture", "saturn_ring"), "saturn_ring");
                body.ring.rockTexture = texture(string(ringObj, "rockTexture", string(ringObj, "rock_texture", "rock_texture")), "rock_texture");
                body.ring.colorHex = colorHex(ringObj, "#ffffff");
                Vec3 ringRot = rotation(ringObj, rot);
                body.ring.yaw = (float) ringRot.x;
                body.ring.pitch = (float) ringRot.y;
                body.ring.roll = (float) ringRot.z;

                JsonObject rocksObj = object(ringObj, "rocks");
                body.ring.rocksEnabled = bool(ringObj, "rocksEnabled", bool(rocksObj, "enabled", false));
                body.ring.rockCount = (int) number(ringObj, "rockCount", number(rocksObj, "count", 4000.0D));
                body.ring.rockMinSize = (float) number(ringObj, "rockMinSize", number(rocksObj, "minSize", -1.0D));
                body.ring.rockMaxSize = (float) number(ringObj, "rockMaxSize", number(rocksObj, "maxSize", -1.0D));
                body.ring.rockHeight = (float) number(ringObj, "rockHeight", number(rocksObj, "height", -1.0D));
                body.ring.rockOrbitSpeed = (float) number(ringObj, "rockOrbitSpeed", number(rocksObj, "orbitSpeed", 0.015D));
            } else {
                body.ring = new PlanetInstance.Ring();
                body.ring.enabled = false;
            }

            JsonObject cloudsObj = object(obj, "clouds");
            if (cloudsObj != null && bool(cloudsObj, "enabled", true)) {
                body.clouds = new PlanetInstance.Clouds();
                body.clouds.enabled = true;
                body.clouds.texture = texture(string(cloudsObj, "texture", string(cloudsObj, "noiseTexture", "noise1")), "noise1");
                body.clouds.height = (float) number(cloudsObj, "height", number(cloudsObj, "altitude", 0.03D));
                body.clouds.density = (float) number(cloudsObj, "density", number(cloudsObj, "coverage", 0.5D));
                body.clouds.windSpeed = (float) number(cloudsObj, "windSpeed", number(cloudsObj, "speed", 0.02D));
                body.clouds.colorHex = colorHex(cloudsObj, "#ffffff");
                body.clouds.alpha = (float) number(cloudsObj, "alpha", 1.0D);
                body.clouds.noiseScale = (float) number(cloudsObj, "noiseScale", number(cloudsObj, "scale", 1.0D));
            } else if (has(obj, "cloudTexture") || has(obj, "cloudsEnabled")) {
                body.clouds = new PlanetInstance.Clouds();
                body.clouds.enabled = bool(obj, "cloudsEnabled", true);
                body.clouds.texture = texture(string(obj, "cloudTexture", "noise1"), "noise1");
                body.clouds.height = (float) number(obj, "cloudHeight", 0.03D);
                body.clouds.density = (float) number(obj, "cloudDensity", number(obj, "cloudCoverage", 0.5D));
                body.clouds.windSpeed = (float) number(obj, "cloudWindSpeed", 0.01D);
                body.clouds.colorHex = colorHex(obj, "#ffffff");
                body.clouds.alpha = (float) number(obj, "cloudAlpha", 1.0D);
                body.clouds.noiseScale = (float) number(obj, "cloudNoiseScale", number(obj, "noiseScale", 1.0D));
            } else {
                body.clouds = new PlanetInstance.Clouds();
                body.clouds.enabled = false;
            }
        }

        return body;
    }

    private static void buildSolarSystem(SolarSystemData system, Instant now,
                                         Set<ResourceLocation> activeStarKeys,
                                         Set<ResourceLocation> activeBlackHoleKeys,
                                         Set<ResourceLocation> activeSurfaceKeys,
                                         Set<ResourceLocation> activeRingKeys,
                                         Set<ResourceLocation> activeRockKeys,
                                         Set<ResourceLocation> activeAtmosKeys) {
        String registryPrefix = system.id;
        float globalScale = Math.max(0.0001F, system.globalScale <= 0.0F ? 1.0F : system.globalScale);
        Vec3 origin = new Vec3(system.originX, system.originY, system.originZ);
        Map<String, Vec3> bodyPositions = calculateBodyPositions(system, now);

        Vec3 lightPosition = origin;
        if (system.star != null && system.star.enabled) {
            String starId = safeId(system.star.id, "sun");
            Vec3 starPosition = bodyPositions.getOrDefault(starId, origin);
            float radius = (float) clamp(system.star.radius * globalScale, 0.05F, MAX_STAR_RADIUS);
            Vector3f color = parseColor(system.star.colorHex, new Vector3f(1.0F, 0.9F, 0.65F));
            Vec3 rot = new Vec3(system.star.yaw, system.star.pitch, system.star.roll);

            String fullStarId = registryPrefix + "/" + starId;

            if (system.star.isBlackHole()) {
                registerBlackHole(fullStarId, starPosition, radius, color, rot, system.star.diskRotationSpeed, system.star.intensity);
                activeBlackHoleKeys.add(ClientRenderRegistries.BLACK_HOLES.id(fullStarId));
            } else {
                registerStar(fullStarId, starPosition, radius, color, rot);
                activeStarKeys.add(ClientRenderRegistries.SUNS.id(fullStarId));
            }

            lightPosition = starPosition;
        }

        for (PlanetInstance.Config body : system.bodies) {
            Vec3 bodyPos = bodyPositions.getOrDefault(body.id, origin);
            String fullBodyId = registryPrefix + "/" + body.id;

            if (body.isBlackHole()) {
                float radius = (float) clamp(body.radius * globalScale, 0.05F, MAX_PLANET_RADIUS);
                Vector3f color = parseColor(body.colorHex, new Vector3f(1.0F, 0.72F, 0.22F));
                Vec3 rot = new Vec3(body.yaw, body.pitch, body.roll);

                registerBlackHole(fullBodyId, bodyPos, radius, color, rot, body.diskRotationSpeed, body.intensity);
                activeBlackHoleKeys.add(ClientRenderRegistries.BLACK_HOLES.id(fullBodyId));
            } else if ("star".equalsIgnoreCase(body.type) || "sun".equalsIgnoreCase(body.type)) {
                float radius = (float) clamp(body.radius * globalScale, 0.05F, MAX_STAR_RADIUS);
                Vector3f color = parseColor(body.colorHex, new Vector3f(1.0F, 0.9F, 0.65F));
                Vec3 rot = new Vec3(body.yaw, body.pitch, body.roll);

                registerStar(fullBodyId, bodyPos, radius, color, rot);
                activeStarKeys.add(ClientRenderRegistries.SUNS.id(fullBodyId));

                lightPosition = bodyPos;
            } else {
                registerBodyInstance(fullBodyId, body, bodyPos, lightPosition, globalScale,
                        activeSurfaceKeys, activeRingKeys, activeRockKeys, activeAtmosKeys);
            }
        }
    }

    public static Map<String, Vec3> calculateBodyPositions(SolarSystemData system, Instant now) {
        if (system == null) return Collections.emptyMap();
        float globalScale = Math.max(0.0001F, system.globalScale <= 0.0F ? 1.0F : system.globalScale);
        Vec3 origin = new Vec3(system.originX, system.originY, system.originZ);
        Map<String, Vec3> bodyPositions = new HashMap<>();

        Vec3 lightPosition = origin;
        if (system.star != null && system.star.enabled) {
            String starId = safeId(system.star.id, "sun");
            bodyPositions.put(starId, origin);
            lightPosition = origin;
        }

        for (PlanetInstance.Config body : system.bodies) {
            String parentId = safeId(body.parentId != null ? body.parentId : "sun", "sun");
            Vec3 parentPos = bodyPositions.getOrDefault(parentId, lightPosition);
            Vec3 bodyPos = resolveBodyPosition(body, origin, parentPos, now, globalScale);
            bodyPositions.put(body.id, bodyPos);
        }

        return bodyPositions;
    }

    public static List<BodySpatialInfo> getDimensionBodiesInSpace(Instant now) {
        List<BodySpatialInfo> result = new ArrayList<>();
        for (SolarSystemData system : activeSystems.values()) {
            if (!"tsp:space".equals(system.dimension)) continue;
            float globalScale = Math.max(0.0001F, system.globalScale <= 0.0F ? 1.0F : system.globalScale);
            Map<String, Vec3> positions = calculateBodyPositions(system, now);
            for (PlanetInstance.Config body : system.bodies) {
                if (body.dimension == null || body.dimension.isBlank()) continue;
                Vec3 pos = positions.get(body.id);
                if (pos == null) continue;

                double physRadius = Math.max(0.05, body.radius * globalScale);
                double visualRadius = resolveAtmosphereRadiusConfig(body.atmosphere, (float) physRadius);
                result.add(new BodySpatialInfo(system, body, pos, physRadius, visualRadius, body.dimension));
            }
        }
        return result;
    }

    public static BodySpatialInfo getBodyByDimension(String dimensionId, Instant now) {
        if (dimensionId == null || dimensionId.isBlank()) return null;
        for (SolarSystemData system : activeSystems.values()) {
            float globalScale = Math.max(0.0001F, system.globalScale <= 0.0F ? 1.0F : system.globalScale);
            Map<String, Vec3> positions = calculateBodyPositions(system, now);
            for (PlanetInstance.Config body : system.bodies) {
                if (dimensionId.equalsIgnoreCase(body.dimension)) {
                    Vec3 pos = positions.get(body.id);
                    if (pos == null) continue;

                    double physRadius = Math.max(0.05, body.radius * globalScale);
                    double visualRadius = resolveAtmosphereRadiusConfig(body.atmosphere, (float) physRadius);
                    return new BodySpatialInfo(system, body, pos, physRadius, visualRadius, body.dimension);
                }
            }
        }
        return null;
    }

    public static Float getGravityForDimension(String dimensionId) {
        if (dimensionId == null || dimensionId.isBlank()) return null;
        for (SolarSystemData system : activeSystems.values()) {
            if (dimensionId.equalsIgnoreCase(system.dimension)) {
                return system.gravity;
            }
            for (PlanetInstance.Config body : system.bodies) {
                if (dimensionId.equalsIgnoreCase(body.dimension)) {
                    return body.gravity;
                }
            }
        }
        return null;
    }

    public static Boolean hasOxygenForDimension(String dimensionId) {
        if (dimensionId == null || dimensionId.isBlank()) return null;
        if ("minecraft:overworld".equalsIgnoreCase(dimensionId)) {
            return true;
        }
        for (SolarSystemData system : activeSystems.values()) {
            if (dimensionId.equalsIgnoreCase(system.dimension)) {
                return false;
            }
            for (PlanetInstance.Config body : system.bodies) {
                if (dimensionId.equalsIgnoreCase(body.dimension)) {
                    return body.oxygen;
                }
            }
        }
        return null;
    }

    public static Float getTemperatureForDimension(String dimensionId) {
        if (dimensionId == null || dimensionId.isBlank()) return null;
        if ("minecraft:overworld".equalsIgnoreCase(dimensionId)) {
            return 0.0F;
        }
        for (SolarSystemData system : activeSystems.values()) {
            if (dimensionId.equalsIgnoreCase(system.dimension)) {
                return -1.0F;
            }
            for (PlanetInstance.Config body : system.bodies) {
                if (dimensionId.equalsIgnoreCase(body.dimension)) {
                    return body.temperature;
                }
            }
        }
        return null;
    }

    private static void registerStar(String id, Vec3 position, float radius, Vector3f color, Vec3 rotation) {
        SunInstance existing = ClientRenderRegistries.SUNS.get(id);
        if (existing != null) {
            existing.updateDynamicState(position, new Vector3f(0.0F, 1.0F, 0.0F), (float) rotation.x, (float) rotation.y, (float) rotation.z);
        } else {
            ClientRenderRegistries.SUNS.put(
                    id,
                    SunInstance.at(position)
                            .planetRadius(radius)
                            .sunRadius(radius * 1.95F)
                            .quadRadius(radius * 1.8F)
                            .densityFalloff(30.0F)
                            .scatteringStrength(0.5F)
                            .color(color)
                            .eulerDegrees((float) rotation.x, (float) rotation.y, (float) rotation.z)
                            .build()
            );
        }
    }

    private static float blackHoleQuadRadius(float radius) {
        return Math.max(radius * 9.5F, radius);
    }

    private static void registerBlackHole(String id, Vec3 position, float radius, Vector3f color, Vec3 rotation, float diskRotationSpeed, float intensity) {
        BlackHoleInstance existing = ClientRenderRegistries.BLACK_HOLES.get(id);
        if (existing != null) {
            existing.updateDynamicState(position, (float) rotation.x, (float) rotation.y, (float) rotation.z);
        } else {
            ClientRenderRegistries.BLACK_HOLES.put(
                    id,
                    BlackHoleInstance.at(position)
                            .radius(radius)
                            .quadRadius(blackHoleQuadRadius(radius))
                            .diskRotationSpeed(diskRotationSpeed)
                            .intensity(intensity)
                            .color(color)
                            .eulerDegrees((float) rotation.x, (float) rotation.y, (float) rotation.z)
                            .build()
            );
        }
    }

    private static void registerBodyInstance(String id, PlanetInstance.Config body, Vec3 position, Vec3 lightPosition, float globalScale,
                                             Set<ResourceLocation> activeSurfaceKeys,
                                             Set<ResourceLocation> activeRingKeys,
                                             Set<ResourceLocation> activeRockKeys,
                                             Set<ResourceLocation> activeAtmosKeys) {
        float radius = (float) clamp(body.radius * globalScale, 0.05F, MAX_PLANET_RADIUS);
        Vector3f color = parseColor(body.colorHex, new Vector3f(0.55F, 0.78F, 1.0F));
        Vec3 rotation = new Vec3(body.yaw, body.pitch, body.roll);
        Vector3f lightDir = lightDirection(lightPosition.toVector3f(), position.toVector3f());

        if (body.surfaceEnabled) {
            String surfaceId = id + "_surface";
            activeSurfaceKeys.add(ClientRenderRegistries.PLANETS_SURFACES.id(surfaceId));

            PlanetInstance.SurfaceInstance existing = ClientRenderRegistries.PLANETS_SURFACES.get(surfaceId);
            if (existing != null) {
                existing.updateDynamicState(position, lightDir, (float) rotation.x, (float) rotation.y, (float) rotation.z);
                existing.setSpinHours(body.spinHours);
            } else {
                String dayTexture = texture(body.texture, "debug");
                String nightTexture = texture(body.nightTexture, "");
                PlanetInstance.SurfaceInstance.Builder builder = PlanetInstance.SurfaceInstance.at(position)
                        .planetRadius(radius)
                        .quadRadius(surfaceQuadRadius(radius))
                        .dayTexture(dayTexture)
                        .color(color)
                        .lightDirection(lightDir)
                        .eulerDegrees((float) rotation.x, (float) rotation.y, (float) rotation.z)
                        .spinHours(body.spinHours);

                if (!nightTexture.isBlank()) {
                    builder.nightTexture(nightTexture);
                }

                if (body.clouds != null && body.clouds.enabled) {
                    builder.cloudsEnabled(true)
                            .cloudTexture(texture(body.clouds.texture, "noise1"))
                            .cloudHeight(body.clouds.height)
                            .cloudCoverage(body.clouds.density)
                            .cloudWindSpeed(body.clouds.windSpeed)
                            .cloudColor(parseColor(body.clouds.colorHex, new Vector3f(1.0F, 1.0F, 1.0F)))
                            .cloudAlpha(body.clouds.alpha)
                            .cloudNoiseScale(body.clouds.noiseScale);
                } else {
                    builder.cloudsEnabled(false);
                }

                ClientRenderRegistries.PLANETS_SURFACES.put(surfaceId, builder.build());
            }
        }

        if (body.ring != null && body.ring.enabled) {
            float innerRadius = (float) clamp(body.ring.innerRadius * globalScale, radius * 1.05F, radius * 8.0F);
            float outerRadius = (float) clamp(body.ring.outerRadius * globalScale, innerRadius + radius * 0.15F, radius * 10.0F);
            float quadRadius = outerRadius * 1.04F;

            String ringId = id + "_ring";
            activeRingKeys.add(ClientRenderRegistries.PLANETS_RINGS.id(ringId));

            PlanetInstance.RingInstance existingRing = ClientRenderRegistries.PLANETS_RINGS.get(ringId);
            if (existingRing != null) {
                existingRing.updateDynamicState(position, lightDir, body.ring.yaw, body.ring.pitch, body.ring.roll);
            } else {
                String ringTexture = texture(body.ring.texture, "saturn_ring");
                Vector3f ringColor = parseColor(body.ring.colorHex, new Vector3f(1.0F, 1.0F, 1.0F));

                ClientRenderRegistries.PLANETS_RINGS.put(
                        ringId,
                        PlanetInstance.RingInstance.at(position)
                                .planetRadius(radius)
                                .quadRadius(quadRadius)
                                .ringInnerRadius(innerRadius)
                                .ringOuterRadius(outerRadius)
                                .ringTexture(ringTexture)
                                .color(ringColor)
                                .lightDirection(lightDir)
                                .eulerDegrees(body.ring.yaw, body.ring.pitch, body.ring.roll)
                                .build()
                );
            }

            if (body.ring.rocksEnabled && body.ring.rockCount > 0) {
                String rocksId = id + "_ring_rocks";
                activeRockKeys.add(ClientRenderRegistries.PLANETS_RING_ROCKS.id(rocksId));

                PlanetInstance.RingInstance.RocksInstance existingRocks = ClientRenderRegistries.PLANETS_RING_ROCKS.get(rocksId);
                if (existingRocks != null) {
                    existingRocks.updateDynamicState(position, lightDir, body.ring.yaw, body.ring.pitch, body.ring.roll);
                } else {
                    float ringWidth = outerRadius - innerRadius;
                    float rockMinSize = body.ring.rockMinSize > 0.0F
                            ? body.ring.rockMinSize * globalScale
                            : Math.max(ringWidth * 0.01F, 0.5F);
                    float rockMaxSize = body.ring.rockMaxSize > 0.0F
                            ? body.ring.rockMaxSize * globalScale
                            : Math.max(ringWidth * 0.05F, rockMinSize * 2.0F);
                    float rockHeight = body.ring.rockHeight > 0.0F
                            ? body.ring.rockHeight * globalScale
                            : Math.max(ringWidth * 0.04F, rockMaxSize * 0.5F);
                    String rockTexture = texture(body.ring.rockTexture, "debug");

                    ClientRenderRegistries.PLANETS_RING_ROCKS.put(
                            rocksId,
                            PlanetInstance.RingInstance.RocksInstance.at(position)
                                    .planetRadius(radius)
                                    .quadRadius(quadRadius)
                                    .ringInnerRadius(innerRadius)
                                    .ringOuterRadius(outerRadius)
                                    .rockCount(body.ring.rockCount)
                                    .rockMinSize(rockMinSize)
                                    .rockMaxSize(rockMaxSize)
                                    .rockHeight(rockHeight)
                                    .orbitSpeed(body.ring.rockOrbitSpeed)
                                    .rockTexture(rockTexture)
                                    .lightDirection(lightDir)
                                    .eulerDegrees(body.ring.yaw, body.ring.pitch, body.ring.roll)
                                    .build()
                    );
                }
            }
        }

        if (body.atmosphere != null && body.atmosphere.enabled) {
            String atmosId = id + "_atmosphere";
            activeAtmosKeys.add(ClientRenderRegistries.ATMOSPHERES.id(atmosId));

            PlanetInstance.AtmosphereInstance existingAtmos = ClientRenderRegistries.ATMOSPHERES.get(atmosId);
            if (existingAtmos != null) {
                existingAtmos.updateDynamicState(position, lightDir, (float) rotation.x, (float) rotation.y, (float) rotation.z);
            } else {
                float atmosphereRadius = resolveAtmosphereRadiusConfig(body.atmosphere, radius);
                float atmosphereThickness = Math.max(atmosphereRadius - radius, radius * 0.01F);
                float atmosphereQuadRadius = atmosphereRadius + atmosphereThickness * 0.18F;
                Vector3f atmosColor = parseColor(body.atmosphere.colorHex, color);
                Vector3f wl = new Vector3f(body.atmosphere.wavelengthR, body.atmosphere.wavelengthG, body.atmosphere.wavelengthB);

                ClientRenderRegistries.ATMOSPHERES.put(
                        atmosId,
                        PlanetInstance.AtmosphereInstance.at(position)
                                .planetRadius(radius)
                                .quadRadius(atmosphereQuadRadius)
                                .atmosphereRadius(atmosphereRadius)
                                .exposure(body.atmosphere.exposure)
                                .intensity(body.atmosphere.intensity)
                                .rayleighScaleHeight(body.atmosphere.rayleighScaleHeight)
                                .rayleighStrength(body.atmosphere.rayleighStrength)
                                .color(atmosColor)
                                .waveLengths(wl)
                                .lightDirection(lightDir)
                                .eulerDegrees((float) rotation.x, (float) rotation.y, (float) rotation.z)
                                .build()
                );
            }
        }
    }

    private static Vec3 resolveBodyPosition(PlanetInstance.Config body, Vec3 origin, Vec3 parentPosition, Instant now, float globalScale) {
        if (body.orbit == null || !body.orbit.enabled) {
            return parentPosition;
        }

        double radius = body.orbit.radius * globalScale;
        double angleDeg = orbitAngleConfig(body.orbit, now);
        double inclinationDeg = body.orbit.inclination;
        double ascendingNodeDeg = body.orbit.ascendingNode;
        double verticalOffset = body.orbit.verticalOffset * globalScale;

        Vec3 offset = orbitOffset(radius, angleDeg, inclinationDeg, ascendingNodeDeg, verticalOffset);
        return parentPosition.add(offset);
    }

    private static double orbitAngleConfig(PlanetInstance.Orbit orbit, Instant now) {
        if (orbit.periodDays <= 0.0D) {
            return orbit.epochAngle;
        }
        Instant epoch = parseEpoch(orbit.epochUtc);
        double elapsedDays = (now.toEpochMilli() - epoch.toEpochMilli()) / MILLIS_PER_DAY;
        return orbit.epochAngle + elapsedDays * (360.0D / orbit.periodDays);
    }

    private static Instant parseEpoch(String epochUtc) {
        if (epochUtc == null || epochUtc.isBlank() || "j2000".equalsIgnoreCase(epochUtc)) {
            return J2000;
        }
        try {
            return Instant.parse(epochUtc);
        } catch (DateTimeParseException exception) {
            return J2000;
        }
    }

    private static Vec3 orbitOffset(double radius, double angleDeg, double inclinationDeg, double ascendingNodeDeg, double verticalOffset) {
        radius = clamp(radius, 0.0D, 500000.0D);
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

    private static float surfaceQuadRadius(float radius) {
        return Math.max(radius * 1.75F, radius + 0.25F);
    }

    private static Vector3f lightDirection(Vector3f sunPosition, Vector3f planetPosition) {
        Vector3f dir = new Vector3f(sunPosition).sub(planetPosition);
        float len = dir.length();
        if (len < 1e-6f) return new Vector3f(0.0f, 1.0f, 0.0f);
        return dir.div(len);
    }

    public static float resolveAtmosphereRadiusConfig(PlanetInstance.Atmosphere atmos, float planetRadius) {
        if (atmos == null || !atmos.enabled) {
            return planetRadius;
        }
        float minRadius = planetRadius * 1.01F;
        float maxRadius = planetRadius * 3.0F;
        float thickness = atmos.thickness;
        if (thickness <= 0.0F) {
            return planetRadius * (1.0F + DEFAULT_ATMOSPHERE_THICKNESS);
        }
        if (thickness <= 2.0F) {
            return (float) clamp(planetRadius * (1.0F + thickness), minRadius, maxRadius);
        }
        return (float) clamp(thickness, minRadius, maxRadius);
    }

    private static Vector3f wavelengths(JsonObject atmosphere, Vector3f fallback) {
        if (atmosphere == null) return new Vector3f(fallback);
        if (has(atmosphere, "wavelengths") && atmosphere.get("wavelengths").isJsonArray()) {
            JsonArray arr = atmosphere.getAsJsonArray("wavelengths");
            if (arr.size() >= 3) {
                return new Vector3f((float) arr.get(0).getAsDouble(), (float) arr.get(1).getAsDouble(), (float) arr.get(2).getAsDouble());
            }
        }
        return new Vector3f(fallback);
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

    public static Vector3f parseColor(String hex, Vector3f fallback) {
        if (hex != null && hex.startsWith("#") && hex.length() == 7) {
            try {
                int r = Integer.parseInt(hex.substring(1, 3), 16);
                int g = Integer.parseInt(hex.substring(3, 5), 16);
                int b = Integer.parseInt(hex.substring(5, 7), 16);
                return new Vector3f(r / 255.0F, g / 255.0F, b / 255.0F);
            } catch (Exception ignored) {}
        }
        return new Vector3f(fallback);
    }

    public static String colorToHex(Vector3f color) {
        int r = Math.min(255, Math.max(0, (int) (color.x * 255.0F)));
        int g = Math.min(255, Math.max(0, (int) (color.y * 255.0F)));
        int b = Math.min(255, Math.max(0, (int) (color.z * 255.0F)));
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private static String colorHex(JsonObject source, String fallbackHex) {
        String hex = string(source, "color", "");
        if (hex.startsWith("#") && hex.length() == 7) {
            return hex;
        }
        return fallbackHex;
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
        if (value == null || value.isBlank()) {
            return normalize(fallback);
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('\\', '/');

        if (normalized.endsWith(".png")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }

        if (SAFE_TEXTURE.matcher(normalized).matches() && !normalized.contains("..")) {
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

    public record BodySpatialInfo(
            SolarSystemData system,
            PlanetInstance.Config body,
            Vec3 spacePosition,
            double physicalRadius,
            double visualRadius,
            String dimension
    ) {}
}