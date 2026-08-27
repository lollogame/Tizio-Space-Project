package tizio.dev.tsp.core.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import tizio.dev.tsp.core.celestial.instance.elements.SolarSystemData;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public final class CelestialJsonExporter {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private CelestialJsonExporter() {}

    public static JsonObject toJson(SolarSystemData system) {
        JsonObject root = new JsonObject();
        root.addProperty("id", system.id);
        root.addProperty("dimension", system.dimension);
        root.addProperty("gravity", system.gravity);

        JsonArray origin = new JsonArray();
        origin.add(system.originX);
        origin.add(system.originY);
        origin.add(system.originZ);
        root.add("origin", origin);
        root.addProperty("globalScale", system.globalScale);

        if (system.star != null && system.star.enabled) {
            JsonObject star = new JsonObject();
            star.addProperty("id", system.star.id);
            star.addProperty("type", system.star.type);
            star.addProperty("radius", system.star.radius);
            star.addProperty("color", system.star.colorHex);

            if (system.star.isBlackHole()) {
                star.addProperty("diskRotationSpeed", system.star.diskRotationSpeed);
                star.addProperty("intensity", system.star.intensity);
            }

            if (system.star.yaw != 0.0f || system.star.pitch != 0.0f || system.star.roll != 0.0f) {
                JsonObject rot = new JsonObject();
                rot.addProperty("yaw", system.star.yaw);
                rot.addProperty("pitch", system.star.pitch);
                rot.addProperty("roll", system.star.roll);
                star.add("rotation", rot);
            }
            root.add("star", star);
        }

        JsonArray bodies = new JsonArray();
        for (PlanetInstance.Config body : system.bodies) {
            JsonObject bodyObj = new JsonObject();
            bodyObj.addProperty("id", body.id);
            bodyObj.addProperty("type", body.type);
            bodyObj.addProperty("parentId", body.parentId != null ? body.parentId : "sun");
            if (body.dimension != null && !body.dimension.isBlank()) {
                bodyObj.addProperty("dimension", body.dimension);
            }
            bodyObj.addProperty("gravity", body.gravity);
            bodyObj.addProperty("oxygen", body.oxygen);
            bodyObj.addProperty("temperature", body.temperature);
            bodyObj.addProperty("radius", body.radius);


            boolean isBH = body.isBlackHole();

            if (isBH) {
                bodyObj.addProperty("diskRotationSpeed", body.diskRotationSpeed);
                bodyObj.addProperty("intensity", body.intensity);
            } else {
                bodyObj.addProperty("texture", body.texture);
                if (body.nightTexture != null && !body.nightTexture.isBlank()) {
                    bodyObj.addProperty("nightTexture", body.nightTexture);
                }
            }
            bodyObj.addProperty("color", body.colorHex);

            if (body.yaw != 0.0f || body.pitch != 0.0f || body.roll != 0.0f) {
                JsonObject rot = new JsonObject();
                rot.addProperty("yaw", body.yaw);
                rot.addProperty("pitch", body.pitch);
                rot.addProperty("roll", body.roll);
                bodyObj.add("rotation", rot);
            }

            if (body != null) {
                bodyObj.addProperty("spinHours", body.spinHours);
            }

            if (body.sky != null && (body.sky.skyboxRotation || body.sky.starsEnabled || body.sky.groundMode)) {
                JsonObject sky = new JsonObject();
                sky.addProperty("skyboxRotation", body.sky.skyboxRotation);
                sky.addProperty("skyboxConstant", body.sky.skyboxConstant);
                sky.addProperty("skyboxTexture", body.sky.skyboxTexture);
                sky.addProperty("starsEnabled", body.sky.starsEnabled);
                sky.addProperty("starsAmount", body.sky.starsAmount);
                sky.addProperty("starsSeed", body.sky.starsSeed);
                sky.addProperty("color", body.sky.starsColorHex);
                sky.addProperty("groundMode", body.sky.groundMode);
                bodyObj.add("sky", sky);
            }

            if (body.fog != null && body.fog.enabled) {
                JsonObject fog = new JsonObject();
                fog.addProperty("enabled", true);
                fog.addProperty("color", body.fog.colorHex);
                fog.addProperty("shape", body.fog.shape);
                fog.addProperty("startDistance", body.fog.startDistance);
                fog.addProperty("endDistance", body.fog.endDistance);
                if (body.fog.useRenderDistance) {
                    fog.addProperty("useRenderDistance", true);
                }
                bodyObj.add("fog", fog);
            }

            if (body.orbit != null && body.orbit.enabled) {
                JsonObject orbit = new JsonObject();
                orbit.addProperty("radius", body.orbit.radius);
                orbit.addProperty("periodDays", body.orbit.periodDays);
                orbit.addProperty("epochAngle", body.orbit.epochAngle);
                orbit.addProperty("epochUtc", body.orbit.epochUtc);
                orbit.addProperty("inclination", body.orbit.inclination);
                orbit.addProperty("ascendingNode", body.orbit.ascendingNode);
                if (body.orbit.verticalOffset != 0.0) {
                    orbit.addProperty("verticalOffset", body.orbit.verticalOffset);
                }
                bodyObj.add("orbit", orbit);
            }

            if (!isBH) {
                if (body.atmosphere != null && body.atmosphere.enabled) {
                    JsonObject atmos = new JsonObject();
                    atmos.addProperty("enabled", true);
                    atmos.addProperty("thickness", body.atmosphere.thickness);
                    atmos.addProperty("exposure", body.atmosphere.exposure);
                    atmos.addProperty("intensity", body.atmosphere.intensity);
                    atmos.addProperty("rayleighScaleHeight", body.atmosphere.rayleighScaleHeight);
                    atmos.addProperty("rayleighStrength", body.atmosphere.rayleighStrength);

                    JsonArray wavelengths = new JsonArray();
                    wavelengths.add(body.atmosphere.wavelengthR);
                    wavelengths.add(body.atmosphere.wavelengthG);
                    wavelengths.add(body.atmosphere.wavelengthB);
                    atmos.add("wavelengths", wavelengths);
                    bodyObj.add("atmosphere", atmos);
                }

                if (body.ring != null && body.ring.enabled) {
                    JsonObject ring = new JsonObject();
                    ring.addProperty("enabled", true);
                    ring.addProperty("innerRadius", body.ring.innerRadius);
                    ring.addProperty("outerRadius", body.ring.outerRadius);
                    ring.addProperty("texture", body.ring.texture);
                    ring.addProperty("rockTexture", body.ring.rockTexture);
                    ring.addProperty("color", body.ring.colorHex);
                    if (body.ring.yaw != 0.0f || body.ring.pitch != 0.0f || body.ring.roll != 0.0f) {
                        JsonObject rRot = new JsonObject();
                        rRot.addProperty("yaw", body.ring.yaw);
                        rRot.addProperty("pitch", body.ring.pitch);
                        rRot.addProperty("roll", body.ring.roll);
                        ring.add("rotation", rRot);
                    }
                    ring.addProperty("rocksEnabled", body.ring.rocksEnabled);
                    ring.addProperty("rockCount", body.ring.rockCount);
                    if (body.ring.rockMinSize > 0.0f) {
                        ring.addProperty("rockMinSize", body.ring.rockMinSize);
                    }
                    if (body.ring.rockMaxSize > 0.0f) {
                        ring.addProperty("rockMaxSize", body.ring.rockMaxSize);
                    }
                    if (body.ring.rockHeight > 0.0f) {
                        ring.addProperty("rockHeight", body.ring.rockHeight);
                    }
                    ring.addProperty("rockOrbitSpeed", body.ring.rockOrbitSpeed);
                    bodyObj.add("ring", ring);
                }

                if (body.clouds != null && body.clouds.enabled) {
                    JsonObject clouds = new JsonObject();
                    clouds.addProperty("enabled", true);
                    clouds.addProperty("texture", body.clouds.texture);
                    clouds.addProperty("height", body.clouds.height);
                    clouds.addProperty("density", body.clouds.density);
                    clouds.addProperty("windSpeed", body.clouds.windSpeed);
                    clouds.addProperty("color", body.clouds.colorHex);
                    clouds.addProperty("noiseScale", body.clouds.noiseScale);
                    if (body.clouds.alpha != 1.0F) {
                        clouds.addProperty("alpha", body.clouds.alpha);
                    }
                    bodyObj.add("clouds", clouds);
                }
            }

            bodies.add(bodyObj);
        }
        root.add("bodies", bodies);

        return root;
    }

    public static File exportToFile(SolarSystemData system, File targetFile) throws IOException {
        if (targetFile.getParentFile() != null) {
            targetFile.getParentFile().mkdirs();
        }
        JsonObject json = toJson(system);
        try (FileWriter writer = new FileWriter(targetFile)) {
            GSON.toJson(json, writer);
        }
        LOGGER.info("Successfully exported solar system '{}' to {}", system.id, targetFile.getAbsolutePath());
        return targetFile;
    }
}