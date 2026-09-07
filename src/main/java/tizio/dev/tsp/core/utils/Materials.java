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

    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> SKY_TEXTURE_CACHE = new ConcurrentHashMap<>();

    public static ResourceLocation getNoTexture() {
        return DEFAULT_DEBUG_TEXTURE;
    }

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
            return getNoTexture();
        }
        return TEXTURE_CACHE.computeIfAbsent(rawTexture, key -> resolveInternal(key, "planets"));
    }

    public static ResourceLocation resolveSkyTextureLocation(String rawTexture) {
        if (rawTexture == null || rawTexture.isBlank()) {
            return getNoTexture();
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
                return getNoTexture();
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
            return getNoTexture();
        }

        return getNoTexture();
    }

    private static boolean textureExists(ResourceLocation location) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}
