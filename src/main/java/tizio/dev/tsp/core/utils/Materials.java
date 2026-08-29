package tizio.dev.tsp.core.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import tizio.dev.tsp.MainClass;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Materials {

    public static final ResourceLocation DEFAULT_DEBUG_TEXTURE = Texture.add("textures/engine/no_texture");

    public static final ResourceLocation SPACE_SKYBOX = Texture.addEnv("milky_way");
    public static final ResourceLocation DARK_SPACE_SKYBOX = Texture.addEnv("spacebox_dark");
    public static final ResourceLocation COMET_ELEMENT = Texture.addEnv("comet");
    public static final ResourceLocation NULL = Texture.addEnv("skybox_null");

    public static final ResourceLocation OVERWORLD_MAT = Texture.add("textures/planets/overworld");
    public static final ResourceLocation OVERWORLD_NIGHT_MAT = Texture.add("textures/planets/overworld_night");

    // Cache per evitare I/O e allocazioni di stringhe ad ogni frame
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> SKY_TEXTURE_CACHE = new ConcurrentHashMap<>();

    private Materials() {}

    public static ResourceLocation getNoTexture() {
        return DEFAULT_DEBUG_TEXTURE;
    }

    /**
     * Da chiamare quando il client ricarica le risorse (F3 + T) per svuotare la cache.
     */
    public static void clearCache() {
        TEXTURE_CACHE.clear();
        SKY_TEXTURE_CACHE.clear();
    }

    protected static class Texture {

        protected static ResourceLocation add(String path) {
            return ResourceLocation.fromNamespaceAndPath(MainClass.MODID, path + ".png");
        }

        protected static ResourceLocation addEnv(String path) {
            return ResourceLocation.fromNamespaceAndPath(MainClass.MODID, "textures/environment/" + path + ".png");
        }
    }

    public static ResourceLocation resolveTextureLocation(String rawTexture) {
        if (rawTexture == null || rawTexture.isBlank()) {
            return DEFAULT_DEBUG_TEXTURE;
        }
        return TEXTURE_CACHE.computeIfAbsent(rawTexture, key -> resolveInternal(key, "planets"));
    }

    public static ResourceLocation resolveSkyTextureLocation(String rawTexture) {
        if (rawTexture == null || rawTexture.isBlank()) {
            return DEFAULT_DEBUG_TEXTURE;
        }
        return SKY_TEXTURE_CACHE.computeIfAbsent(rawTexture, key -> resolveInternal(key, "environment"));
    }

    private static ResourceLocation resolveInternal(String rawTexture, String defaultSubFolder) {
        try {
            String path = rawTexture.trim().replace('\\', '/').toLowerCase(Locale.ROOT);

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            String namespace = MainClass.MODID;

            int colonIndex = path.indexOf(':');
            if (colonIndex != -1) {
                namespace = path.substring(0, colonIndex);
                path = path.substring(colonIndex + 1);
            }

            if (path.isEmpty() || path.endsWith("/")) {
                return DEFAULT_DEBUG_TEXTURE;
            }

            if (!path.startsWith("textures/")) {
                if (path.contains("/")) {
                    path = "textures/" + path;
                } else {
                    path = "textures/" + defaultSubFolder + "/" + path;
                }
            }

            if (!path.endsWith(".png")) {
                path = path + ".png";
            }

            ResourceLocation candidateLocation = new ResourceLocation(namespace, path);

            if (textureExists(candidateLocation)) {
                return candidateLocation;
            }

        } catch (Exception e) {
            return DEFAULT_DEBUG_TEXTURE;
        }

        return DEFAULT_DEBUG_TEXTURE;
    }

    private static boolean textureExists(ResourceLocation location) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}