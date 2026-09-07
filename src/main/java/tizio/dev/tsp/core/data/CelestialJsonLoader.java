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
import tizio.dev.tsp.config.DataConfig;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.celestial.instance.elements.blackhole.BlackHoleInstance;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.celestial.instance.elements.planet.ring.PlanetRingRockRenderer;
import tizio.dev.tsp.core.celestial.instance.elements.sun.SunInstance;
import tizio.dev.tsp.core.client.ClientRenderRegistries;
import tizio.dev.tsp.core.utils.Utils;

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
    private static final Instant J2000 = Instant.parse(DataConfig.Orbit.EPOCH_UTC_DEF);
    private static final double MILLIS_PER_DAY = 86_400_000.0D;
    private static final long REFRESH_INTERVAL_MS = 1_000L;

    private static final float DEFAULT_ATMOSPHERE_THICKNESS = DataConfig.Atmosphere.THICKNESS_LOADER_FALLBACK;
    private static final float DEFAULT_ATMOSPHERE_INTENSITY = DataConfig.Atmosphere.INTENSITY.defF();
    private static final float DEFAULT_ATMOSPHERE_EXPOSURE = DataConfig.Atmosphere.EXPOSURE.defF();
    private static final float DEFAULT_RAYLEIGH_SCALE_HEIGHT = DataConfig.Atmosphere.RAYLEIGH_SCALE_HEIGHT.defF();
    private static final float DEFAULT_RAYLEIGH_STRENGTH = DataConfig.Atmosphere.RAYLEIGH_STRENGTH.defF();

    private static final Vector3f DEFAULT_ATMOSPHERE_WAVELENGTHS = DataConfig.Atmosphere.WAVELENGTHS_DEF;
    private static final float MAX_PLANET_RADIUS = DataConfig.Body.MAX_RADIUS;
    private static final float MAX_STAR_RADIUS = DataConfig.Star.MAX_RADIUS;

    private static final Map<String, SolarSystemData> activeSystems = new LinkedHashMap<>();
    private static volatile LoadedData loadedData = LoadedData.EMPTY;
    private static volatile long nextRefreshMs = 0L;
    private static volatile ResourceLocation currentDimension = ResourceLocation.tryParse(SolarSystemData.DEFAULT_SPACE_DIMENSION);
    private static volatile String activeSelectedSystemId = null;

    private static final java.util.Set<String> VANILLA_DIMENSION_BLACKLIST = DataConfig.System.VANILLA_DIMENSION_BLACKLIST;

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
        File exportFile = new File(gameDir, DataConfig.System.EXPORT_DIR + config.id + ".json");
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
        String dimension = string(root, "dimension", SolarSystemData.DEFAULT_SPACE_DIMENSION);
        if (isBlacklistedForSpaceDimension(dimension)) {
            LOGGER.warn("[TSP] Solar system '{}': dimension '{}' is blacklisted for space use. Falling back to '{}'.",
                    systemId, dimension, SolarSystemData.DEFAULT_SPACE_DIMENSION);
            dimension = SolarSystemData.DEFAULT_SPACE_DIMENSION;
        }
        Vec3 originVec = vec3(root, "origin", DataConfig.System.ORIGIN_DEF);

        SolarSystemData config = new SolarSystemData(systemId, dimension);
        config.originX = originVec.x;
        config.originY = originVec.y;
        config.originZ = originVec.z;
        config.globalScale = (float) Utils.clamp(number(root, "globalScale", DataConfig.System.SCALE.def()), DataConfig.System.SCALE.min(), DataConfig.System.SCALE.max());
        config.gravity = (float) number(root, "gravity", DataConfig.System.GRAVITY_DEF);

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
                PlanetInstance.Config body = parseBody(elem.getAsJsonObject(), DataConfig.Body.TYPE_DEF, DataConfig.Body.PARENT_ID_DEF);
                if (body != null) {
                    config.bodies.add(body);
                }
            }
        }

        return config;
    }

    private static SunInstance.Config parseStar(JsonObject starObj) {
        SunInstance.Config star = new SunInstance.Config();
        star.id = safeId(string(starObj, "id", DataConfig.Star.ID_DEF), DataConfig.Star.ID_DEF);
        star.type = string(starObj, "type", DataConfig.Star.TYPE_DEF);
        star.radius = (float) number(starObj, "radius", DataConfig.Star.RADIUS.def());
        star.colorHex = colorHex(starObj, DataConfig.Star.COLOR_HEX_DEF);
        star.enabled = bool(starObj, "enabled", DataConfig.Star.ENABLED_DEF);
        star.diskRotationSpeed = (float) number(starObj, "diskRotationSpeed", DataConfig.Star.DISK_ROTATION_SPEED.def());
        star.intensity = (float) number(starObj, "intensity", DataConfig.Star.INTENSITY.def());
        Vec3 rot = rotation(starObj);
        star.yaw = (float) rot.x;
        star.pitch = (float) rot.y;
        star.roll = (float) rot.z;
        return star;
    }

    private static PlanetInstance.Config parseBody(JsonObject obj, String defaultType, String defaultParent) {
        String id = safeId(string(obj, "id", DataConfig.Body.ID_DEF), DataConfig.Body.ID_DEF);
        String type = string(obj, "type", defaultType);
        String parentId = safeId(string(obj, "parentId", string(obj, "parent", defaultParent)), defaultParent);
        float radius = radius(obj, DataConfig.Body.FALLBACK_RADIUS);
        String texture = texture(string(obj, "texture", string(obj, "dayTexture", DataConfig.Body.TEXTURE_DEF)), DataConfig.Body.TEXTURE_DEF);
        String nightTexture = texture(string(obj, "nightTexture", DataConfig.Body.NIGHT_TEXTURE_DEF), DataConfig.Body.NIGHT_TEXTURE_DEF);
        String color = colorHex(obj, DataConfig.Body.COLOR_HEX_DEF);
        Vec3 rot = rotation(obj);

        PlanetInstance.Config body = new PlanetInstance.Config(id, type, parentId, radius, texture, color);
        body.dimension = string(obj, "dimension", DataConfig.Body.DIMENSION_DEF);
        body.gravity = (float) number(obj, "gravity", DataConfig.Body.GRAVITY_FALLBACK);
        body.oxygen = bool(obj, "oxygen", bool(object(obj, "surface"), "oxygen", DataConfig.Body.OXYGEN_DEF));
        body.temperature = (float) Utils.clamp(number(obj, "temperature", number(object(obj, "surface"), "temperature", DataConfig.Body.TEMPERATURE.def())), DataConfig.Body.TEMPERATURE.min(), DataConfig.Body.TEMPERATURE.max());
        body.nightTexture = nightTexture;
        body.yaw = (float) rot.x;
        body.pitch = (float) rot.y;
        body.roll = (float) rot.z;
        body.diskRotationSpeed = (float) number(obj, "diskRotationSpeed", DataConfig.Body.DISK_ROTATION_SPEED.def());
        body.intensity = (float) number(obj, "intensity", DataConfig.Body.INTENSITY.def());

        JsonObject spinObj = object(obj, "orbit");
        body.spinHours = number(obj, "spinHours", number(obj, "rotationHours", number(obj, "spinPeriodHours",
                number(obj, "spinPeriodDays", number(obj, "rotationPeriodDays", number(spinObj, "spinPeriodDays", 1.0D))) * DataConfig.Body.SPIN_HOURS.def())));

        JsonObject orbitObj = object(obj, "orbit");
        body.orbit = new PlanetInstance.Orbit();
        body.orbit.radius = number(obj, "orbitRadius", number(obj, "distance", number(orbitObj, "radius", number(orbitObj, "distance", DataConfig.Orbit.RADIUS_FALLBACK))));
        body.orbit.periodDays = number(obj, "orbitalPeriodDays", number(orbitObj, "periodDays", DataConfig.Orbit.PERIOD_DAYS_FALLBACK));
        body.orbit.epochAngle = number(obj, "epochAngleDeg", number(orbitObj, "epochAngle", number(orbitObj, "angleDeg", DataConfig.Orbit.EPOCH_ANGLE.def())));
        body.orbit.epochUtc = string(obj, "epochUtc", string(orbitObj, "epochUtc", DataConfig.Orbit.EPOCH_UTC_DEF));
        body.orbit.inclination = number(obj, "inclinationDeg", number(orbitObj, "inclination", DataConfig.Orbit.INCLINATION.def()));
        body.orbit.ascendingNode = number(obj, "ascendingNodeDeg", number(orbitObj, "ascendingNode", DataConfig.Orbit.ASCENDING_NODE.def()));
        body.orbit.verticalOffset = number(obj, "height", number(orbitObj, "verticalOffset", DataConfig.Orbit.VERTICAL_OFFSET.def()));
        body.orbit.enabled = orbitObj == null || bool(orbitObj, "enabled", DataConfig.Orbit.ENABLED_DEF);

        JsonObject skyObj = object(obj, "sky");
        body.sky = new PlanetInstance.Sky();
        if (skyObj != null) {
            body.sky.skyboxRotation = bool(skyObj, "skyboxRotation", DataConfig.Sky.SKYBOX_ROTATION_DEF);
            body.sky.skyboxConstant = bool(skyObj, "skyboxConstant", DataConfig.Sky.SKYBOX_CONSTANT_DEF);
            body.sky.skyboxTexture = texture(string(skyObj, "skyboxTexture", DataConfig.Sky.SKYBOX_TEXTURE_LOADER_DEF), DataConfig.Sky.SKYBOX_TEXTURE_LOADER_DEF);
            body.sky.starsEnabled = bool(skyObj, "starsEnabled", DataConfig.Sky.STARS_ENABLED_DEF);
            body.sky.starsAmount = (int) number(skyObj, "starsAmount", DataConfig.Sky.STARS_AMOUNT.def());
            body.sky.starsSeed = (int) number(skyObj, "starsSeed", DataConfig.Sky.STARS_SEED.def());
            body.sky.starsColorHex = colorHex(skyObj, DataConfig.Sky.STARS_COLOR_HEX_DEF);
            body.sky.groundMode = bool(skyObj, "groundMode", DataConfig.Sky.GROUND_MODE_DEF);
        }

        JsonObject fogObj = object(obj, "fog");
        body.fog = new PlanetInstance.SurfaceInstance.SurfaceFog();
        if (fogObj != null) {
            body.fog.enabled = bool(fogObj, "enabled", DataConfig.Fog.ENABLED_DEF);
            body.fog.colorHex = colorHex(fogObj, DataConfig.Fog.COLOR_HEX_DEF);
            body.fog.shape = string(fogObj, "shape", DataConfig.Fog.SHAPE_DEF);
            body.fog.startDistance = (float) number(fogObj, "startDistance", DataConfig.Fog.START_DISTANCE.def());
            body.fog.endDistance = (float) number(fogObj, "endDistance", DataConfig.Fog.END_DISTANCE_LOADER_FALLBACK);
            body.fog.useRenderDistance = bool(fogObj, "useRenderDistance", DataConfig.Fog.USE_RENDER_DISTANCE_DEF);
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
                body.atmosphere.thickness = (float) number(atmosObj, "thickness", number(atmosObj, "atmosphereRadius", DataConfig.Atmosphere.THICKNESS.def()));
                body.atmosphere.exposure = (float) number(atmosObj, "exposure", DEFAULT_ATMOSPHERE_EXPOSURE);
                body.atmosphere.intensity = (float) number(atmosObj, "intensity", DEFAULT_ATMOSPHERE_INTENSITY);
                body.atmosphere.rayleighScaleHeight = (float) number(atmosObj, "rayleighScaleHeight", DEFAULT_RAYLEIGH_SCALE_HEIGHT);
                body.atmosphere.rayleighStrength = (float) number(atmosObj, "rayleighStrength", DEFAULT_RAYLEIGH_STRENGTH);
                Vector3f wl = wavelengths(atmosObj, DEFAULT_ATMOSPHERE_WAVELENGTHS);
                body.atmosphere.wavelengthR = wl.x;
                body.atmosphere.wavelengthG = wl.y;
                body.atmosphere.wavelengthB = wl.z;
                body.atmosphere.colorHex = colorHex(atmosObj, DataConfig.Atmosphere.JSON_COLOR_HEX_DEF);
            } else {
                body.atmosphere = new PlanetInstance.Atmosphere();
                body.atmosphere.enabled = false;
            }

            JsonObject ringObj = object(obj, "ring");
            if (ringObj != null && bool(ringObj, "enabled", false)) {
                body.ring = new PlanetInstance.Ring();
                body.ring.enabled = true;
                body.ring.innerRadius = (float) number(ringObj, "innerRadius", DataConfig.Ring.INNER_RADIUS.def());
                body.ring.outerRadius = (float) number(ringObj, "outerRadius", DataConfig.Ring.OUTER_RADIUS.def());
                body.ring.texture = texture(string(ringObj, "texture", DataConfig.Ring.TEXTURE_DEF), DataConfig.Ring.TEXTURE_DEF);
                body.ring.rockTexture = texture(string(ringObj, "rockTexture", string(ringObj, "rock_texture", DataConfig.Ring.ROCK_TEXTURE_DEF)), DataConfig.Ring.ROCK_TEXTURE_DEF);
                body.ring.colorHex = colorHex(ringObj, DataConfig.Ring.COLOR_HEX_DEF);
                Vec3 ringRot = rotation(ringObj, rot);

                body.ring.yaw = (float) ringRot.x;
                body.ring.pitch = (float) ringRot.y;
                body.ring.roll = (float) ringRot.z;

                JsonObject rocksObj = object(ringObj, "rocks");
                body.ring.rocksEnabled = bool(ringObj, "rocksEnabled", bool(rocksObj, "enabled", DataConfig.Ring.ROCKS_ENABLED_DEF));
                body.ring.rockCount = (int) number(ringObj, "rockCount", number(rocksObj, "count", DataConfig.Ring.ROCK_COUNT.def()));
                body.ring.rockMinSize = (float) number(ringObj, "rockMinSize", number(rocksObj, "minSize", DataConfig.Ring.ROCK_MIN_SIZE.def()));
                body.ring.rockMaxSize = (float) number(ringObj, "rockMaxSize", number(rocksObj, "maxSize", DataConfig.Ring.ROCK_MAX_SIZE.def()));
                body.ring.rockHeight = (float) number(ringObj, "rockHeight", number(rocksObj, "height", DataConfig.Ring.ROCK_HEIGHT_FALLBACK));
                body.ring.rockOrbitSpeed = (float) number(ringObj, "rockOrbitSpeed", number(rocksObj, "orbitSpeed", DataConfig.Ring.ROCK_ORBIT_SPEED_FALLBACK));
            } else {
                body.ring = new PlanetInstance.Ring();
                body.ring.enabled = false;
            }

            JsonObject cloudsObj = object(obj, "clouds");
            if (cloudsObj != null && bool(cloudsObj, "enabled", true)) {
                body.clouds = new PlanetInstance.Clouds();
                body.clouds.enabled = true;
                body.clouds.texture = texture(string(cloudsObj, "texture", string(cloudsObj, "noiseTexture", DataConfig.Clouds.TEXTURE_DEF)), DataConfig.Clouds.TEXTURE_DEF);
                body.clouds.height = (float) number(cloudsObj, "height", number(cloudsObj, "altitude", DataConfig.Clouds.HEIGHT.def()));
                body.clouds.density = (float) number(cloudsObj, "density", number(cloudsObj, "coverage", DataConfig.Clouds.DENSITY.def()));
                body.clouds.windSpeed = (float) number(cloudsObj, "windSpeed", number(cloudsObj, "speed", DataConfig.Clouds.WIND_SPEED.def()));
                body.clouds.colorHex = colorHex(cloudsObj, DataConfig.Clouds.COLOR_HEX_DEF);
                body.clouds.alpha = (float) number(cloudsObj, "alpha", DataConfig.Clouds.ALPHA_DEF);
                body.clouds.noiseScale = (float) number(cloudsObj, "noiseScale", number(cloudsObj, "scale", DataConfig.Clouds.NOISE_SCALE.def()));
            } else if (has(obj, "cloudTexture") || has(obj, "cloudsEnabled")) {
                body.clouds = new PlanetInstance.Clouds();
                body.clouds.enabled = bool(obj, "cloudsEnabled", true);
                body.clouds.texture = texture(string(obj, "cloudTexture", DataConfig.Clouds.TEXTURE_DEF), DataConfig.Clouds.TEXTURE_DEF);
                body.clouds.height = (float) number(obj, "cloudHeight", DataConfig.Clouds.HEIGHT.def());
                body.clouds.density = (float) number(obj, "cloudDensity", number(obj, "cloudCoverage", DataConfig.Clouds.DENSITY.def()));
                body.clouds.windSpeed = (float) number(obj, "cloudWindSpeed", DataConfig.Clouds.WIND_SPEED.def());
                body.clouds.colorHex = colorHex(obj, DataConfig.Clouds.COLOR_HEX_DEF);
                body.clouds.alpha = (float) number(obj, "cloudAlpha", DataConfig.Clouds.ALPHA_DEF);
                body.clouds.noiseScale = (float) number(obj, "cloudNoiseScale", number(obj, "noiseScale", DataConfig.Clouds.NOISE_SCALE.def()));
            } else {
                body.clouds = new PlanetInstance.Clouds();
                body.clouds.enabled = false;
            }
        }

        return body;
    }

    private static void buildSolarSystem(SolarSystemData system, Instant now, Set<ResourceLocation> activeStarKeys, Set<ResourceLocation> activeBlackHoleKeys, Set<ResourceLocation> activeSurfaceKeys, Set<ResourceLocation> activeRingKeys, Set<ResourceLocation> activeRockKeys, Set<ResourceLocation> activeAtmosKeys) {

        String registryPrefix = system.id;
        float globalScale = (float) Utils.clamp(system.globalScale, DataConfig.System.SCALE.min(), DataConfig.System.SCALE.max());
        Vec3 origin = new Vec3(system.originX, system.originY, system.originZ);
        Map<String, Vec3> bodyPositions = calculateBodyPositions(system, now);

        Vec3 lightPosition = origin;
        if (system.star != null && system.star.enabled) {
            String starId = safeId(system.star.id, DataConfig.Star.ID_DEF);
            Vec3 starPosition = bodyPositions.getOrDefault(starId, origin);
            float radius = (float) Utils.clamp(system.star.radius * globalScale, DataConfig.Body.MIN_PHYSICAL_RADIUS, MAX_STAR_RADIUS);
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
                float radius = (float) Utils.clamp(body.radius * globalScale, DataConfig.Body.MIN_PHYSICAL_RADIUS, MAX_PLANET_RADIUS);
                Vector3f color = parseColor(body.colorHex, new Vector3f(1.0F, 0.72F, 0.22F));
                Vec3 rot = new Vec3(body.yaw, body.pitch, body.roll);

                registerBlackHole(fullBodyId, bodyPos, radius, color, rot, body.diskRotationSpeed, body.intensity);
                activeBlackHoleKeys.add(ClientRenderRegistries.BLACK_HOLES.id(fullBodyId));
            } else if ("star".equalsIgnoreCase(body.type) || "sun".equalsIgnoreCase(body.type)) {
                float radius = (float) Utils.clamp(body.radius * globalScale, DataConfig.Body.MIN_PHYSICAL_RADIUS, MAX_STAR_RADIUS);
                Vector3f color = parseColor(body.colorHex, new Vector3f(1.0F, 0.9F, 0.65F));
                Vec3 rot = new Vec3(body.yaw, body.pitch, body.roll);

                registerStar(fullBodyId, bodyPos, radius, color, rot);
                activeStarKeys.add(ClientRenderRegistries.SUNS.id(fullBodyId));

                lightPosition = bodyPos;
            } else {
                registerBodyInstance(fullBodyId, body, bodyPos, lightPosition, globalScale, activeSurfaceKeys, activeRingKeys, activeRockKeys, activeAtmosKeys);
            }
        }
    }

    public static Map<String, Vec3> calculateBodyPositions(SolarSystemData system, Instant now) {
        if (system == null) return Collections.emptyMap();
        float globalScale = (float) Utils.clamp(system.globalScale, DataConfig.System.SCALE.min(), DataConfig.System.SCALE.max());
        Vec3 origin = new Vec3(system.originX, system.originY, system.originZ);
        Map<String, Vec3> bodyPositions = new HashMap<>();

        Vec3 lightPosition = origin;
        if (system.star != null && system.star.enabled) {
            String starId = safeId(system.star.id, DataConfig.Star.ID_DEF);
            bodyPositions.put(starId, origin);
            if (system.star.id != null && !system.star.id.isBlank() && !starId.equals(system.star.id)) {
                bodyPositions.put(system.star.id, origin);
            }
            lightPosition = origin;
        }

        List<PlanetInstance.Config> pending = new ArrayList<>(system.bodies);
        int maxPasses = pending.size() + 1;
        while (!pending.isEmpty() && maxPasses-- > 0) {
            boolean progress = false;
            Iterator<PlanetInstance.Config> it = pending.iterator();
            while (it.hasNext()) {
                PlanetInstance.Config body = it.next();
                String parentId = safeId(body.parentId != null ? body.parentId : DataConfig.Body.PARENT_ID_DEF, DataConfig.Body.PARENT_ID_DEF);
                boolean hasParent = bodyPositions.containsKey(parentId) || (body.parentId != null && bodyPositions.containsKey(body.parentId));
                if (hasParent || maxPasses == 0) {
                    Vec3 parentPos = bodyPositions.get(parentId);
                    if (parentPos == null && body.parentId != null) {
                        parentPos = bodyPositions.get(body.parentId);
                    }
                    if (parentPos == null) {
                        parentPos = lightPosition;
                    }
                    Vec3 bodyPos = resolveBodyPosition(body, origin, parentPos, now, globalScale);
                    bodyPositions.put(body.id, bodyPos);
                    String safeBodyId = safeId(body.id, DataConfig.Body.ID_DEF);
                    if (!safeBodyId.equals(body.id)) {
                        bodyPositions.put(safeBodyId, bodyPos);
                    }
                    it.remove();
                    progress = true;
                }
            }
            if (!progress && !pending.isEmpty()) {
                PlanetInstance.Config body = pending.remove(0);
                String parentId = safeId(body.parentId != null ? body.parentId : DataConfig.Body.PARENT_ID_DEF, DataConfig.Body.PARENT_ID_DEF);
                Vec3 parentPos = bodyPositions.get(parentId);
                if (parentPos == null && body.parentId != null) {
                    parentPos = bodyPositions.get(body.parentId);
                }
                if (parentPos == null) {
                    parentPos = lightPosition;
                }
                Vec3 bodyPos = resolveBodyPosition(body, origin, parentPos, now, globalScale);
                bodyPositions.put(body.id, bodyPos);
                String safeBodyId = safeId(body.id, DataConfig.Body.ID_DEF);
                if (!safeBodyId.equals(body.id)) {
                    bodyPositions.put(safeBodyId, bodyPos);
                }
            }
        }

        return bodyPositions;
    }

    public static boolean isSpaceDimension(String dimensionId) {
        if (dimensionId == null || dimensionId.isBlank()) return false;
        for (SolarSystemData system : activeSystems.values()) {
            if (dimensionId.equalsIgnoreCase(system.dimension)) {
                return true;
            }
        }
        if (activeSystems.isEmpty()) {
            for (LoadedJson systemJson : loadedData.solarSystems()) {
                if (systemJson.root() != null && systemJson.root().has("dimension")) {
                    String dim = systemJson.root().get("dimension").getAsString();
                    if (dimensionId.equalsIgnoreCase(dim)) {
                        return true;
                    }
                }
            }
            return SolarSystemData.DEFAULT_SPACE_DIMENSION.equalsIgnoreCase(dimensionId);
        }
        return false;
    }

    public static boolean isSpaceDimension(ResourceLocation dimension) {
        if (dimension == null) return false;
        return isSpaceDimension(dimension.toString());
    }

    public static boolean isSpaceDimension(net.minecraft.world.level.Level level) {
        if (level == null) return false;
        return isSpaceDimension(level.dimension().location());
    }

    public static boolean isBlacklistedForSpaceDimension(String dimensionId) {
        if (dimensionId == null || dimensionId.isBlank()) return true;
        String id = dimensionId.toLowerCase(java.util.Locale.ROOT);
        if (VANILLA_DIMENSION_BLACKLIST.contains(id)) return true;
        for (SolarSystemData system : activeSystems.values()) {
            for (PlanetInstance.Config body : system.bodies) {
                if (body.dimension != null && body.dimension.toLowerCase(java.util.Locale.ROOT).equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Set<String> getAllConfiguredSpaceDimensions() {
        Set<String> set = new LinkedHashSet<>();
        set.add(SolarSystemData.DEFAULT_SPACE_DIMENSION);
        for (SolarSystemData system : activeSystems.values()) {
            if (system.dimension != null && !system.dimension.isBlank()) {
                set.add(system.dimension);
            }
        }
        for (LoadedJson systemJson : loadedData.solarSystems()) {
            if (systemJson.root() != null && systemJson.root().has("dimension")) {
                String dim = systemJson.root().get("dimension").getAsString();
                if (dim != null && !dim.isBlank()) {
                    set.add(dim);
                }
            }
        }
        return set;
    }

    public static SolarSystemData getSolarSystemForSpaceDimension(String spaceDimensionId) {
        if (spaceDimensionId == null || spaceDimensionId.isBlank()) return null;
        for (SolarSystemData system : activeSystems.values()) {
            if (spaceDimensionId.equalsIgnoreCase(system.dimension)) {
                return system;
            }
        }
        return null;
    }

    public static Float getStarPhysicalRadius(String spaceDimensionId) {
        SolarSystemData system = getSolarSystemForSpaceDimension(spaceDimensionId);
        if (system == null || system.star == null || !system.star.enabled) {
            return null;
        }
        float globalScale = (float) Utils.clamp(system.globalScale, DataConfig.System.SCALE.min(), DataConfig.System.SCALE.max());
        return (float) Utils.clamp(system.star.radius * globalScale, DataConfig.Body.MIN_PHYSICAL_RADIUS, MAX_STAR_RADIUS);
    }

    public static SolarSystemData getSolarSystemForBodyDimension(String bodyDimensionId) {
        if (bodyDimensionId == null || bodyDimensionId.isBlank()) return null;
        for (SolarSystemData system : activeSystems.values()) {
            for (PlanetInstance.Config body : system.bodies) {
                if (bodyDimensionId.equalsIgnoreCase(body.dimension)) {
                    return system;
                }
            }
        }
        return null;
    }

    public static String getSpaceDimensionForBodyDimension(String bodyDimensionId) {
        SolarSystemData system = getSolarSystemForBodyDimension(bodyDimensionId);
        if (system != null && system.dimension != null && !system.dimension.isBlank()) {
            return system.dimension;
        }
        return SolarSystemData.DEFAULT_SPACE_DIMENSION;
    }

    public static List<BodySpatialInfo> getDimensionBodiesInSpace(Instant now) {
        return getDimensionBodiesInSpace((String) null, now);
    }

    public static List<BodySpatialInfo> getDimensionBodiesInSpace(String spaceDimensionId, Instant now) {
        List<BodySpatialInfo> result = new ArrayList<>();
        for (SolarSystemData system : activeSystems.values()) {
            if (spaceDimensionId != null && !spaceDimensionId.isBlank() && !spaceDimensionId.equalsIgnoreCase(system.dimension)) {
                continue;
            }
            if (system.dimension == null || system.dimension.isBlank()) continue;
            float globalScale = (float) Utils.clamp(system.globalScale, DataConfig.System.SCALE.min(), DataConfig.System.SCALE.max());
            Map<String, Vec3> positions = calculateBodyPositions(system, now);
            for (PlanetInstance.Config body : system.bodies) {

                Vec3 pos = positions.get(body.id);
                if (pos == null) continue;

                double physRadius = Math.max(DataConfig.Body.MIN_PHYSICAL_RADIUS, body.radius * globalScale);
                double visualRadius = resolveAtmosphereRadiusConfig(body.atmosphere, (float) physRadius);
                result.add(new BodySpatialInfo(system, body, pos, physRadius, visualRadius, body.dimension));
            }
        }
        return result;
    }

    public static BodySpatialInfo getBodyByDimension(String dimensionId, Instant now) {
        if (dimensionId == null || dimensionId.isBlank()) return null;
        for (SolarSystemData system : activeSystems.values()) {
            float globalScale = (float) Utils.clamp(system.globalScale, DataConfig.System.SCALE.min(), DataConfig.System.SCALE.max());
            Map<String, Vec3> positions = calculateBodyPositions(system, now);
            for (PlanetInstance.Config body : system.bodies) {
                if (dimensionId.equalsIgnoreCase(body.dimension)) {
                    Vec3 pos = positions.get(body.id);
                    if (pos == null) continue;

                    double physRadius = Math.max(DataConfig.Body.MIN_PHYSICAL_RADIUS, body.radius * globalScale);
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
        if (isSpaceDimension(dimensionId)) {
            return false;
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
        if (isSpaceDimension(dimensionId)) {
            return -1.0F;
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

        float radius = (float) Utils.clamp(body.radius * globalScale, DataConfig.Body.MIN_PHYSICAL_RADIUS, MAX_PLANET_RADIUS);
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
                            .cloudTexture(texture(body.clouds.texture, DataConfig.Clouds.TEXTURE_DEF))
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
            float rawInner = body.ring.innerRadius;
            float rawOuter = body.ring.outerRadius;

            float baseInner = (rawInner <= 15.0F || rawInner < body.radius) ? rawInner * radius : rawInner * globalScale;
            float baseOuter = (rawOuter <= 15.0F || rawOuter < body.radius) ? rawOuter * radius : rawOuter * globalScale;

            if (baseOuter <= baseInner) {
                baseOuter = baseInner + radius * 0.15F;
            }

            float innerRadius = (float) Utils.clamp(baseInner, radius * 1.02F, radius * 8.0F);
            float outerRadius = (float) Utils.clamp(baseOuter, innerRadius + radius * 0.10F, radius * 10.0F);
            float quadRadius = outerRadius * 1.04F;

            String ringId = id + "_ring";
            activeRingKeys.add(ClientRenderRegistries.PLANETS_RINGS.id(ringId));

            PlanetInstance.RingInstance existingRing = ClientRenderRegistries.PLANETS_RINGS.get(ringId);
            if (existingRing != null) {
                existingRing.updateDynamicState(position, lightDir, body.ring.yaw, body.ring.pitch, body.ring.roll);
            } else {
                String ringTexture = texture(body.ring.texture, DataConfig.Ring.TEXTURE_DEF);
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

                    float planetScale = radius / 100.0F;

                    float rockMinSize = body.ring.rockMinSize > 0.0F
                            ? body.ring.rockMinSize * planetScale
                            : Math.max(ringWidth * 0.01F, 0.5F);
                    float rockMaxSize = body.ring.rockMaxSize > 0.0F
                            ? body.ring.rockMaxSize * planetScale
                            : Math.max(ringWidth * 0.05F, rockMinSize * 2.0F);
                    float rockHeight = body.ring.rockHeight >= 0.0F
                            ? body.ring.rockHeight * planetScale
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

    public static Vec3 orbitOffset(double radius, double angleDeg, double inclinationDeg, double ascendingNodeDeg, double verticalOffset) {
        radius = Utils.clamp(radius, 0.0D, DataConfig.Orbit.MAX_SCALED_RADIUS);
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
        return (float) Utils.clamp(number(source, "radius", number(surface, "radius", fallback)), DataConfig.Body.MIN_PHYSICAL_RADIUS, MAX_PLANET_RADIUS);
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
            return (float) Utils.clamp(planetRadius * (1.0F + thickness), minRadius, maxRadius);
        }
        return (float) Utils.clamp(thickness, minRadius, maxRadius);
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
                Utils.clamp(number(source, "yaw", fallback.x), -10000.0D, 10000.0D),
                Utils.clamp(number(source, "pitch", fallback.y), -10000.0D, 10000.0D),
                Utils.clamp(number(source, "roll", fallback.z), -10000.0D, 10000.0D)
        );
    }

    public static String safeId(String value, String fallback) {
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

    public record BodySpatialInfo(SolarSystemData system, PlanetInstance.Config body, Vec3 spacePosition, double physicalRadius, double visualRadius, String dimension) {}
}