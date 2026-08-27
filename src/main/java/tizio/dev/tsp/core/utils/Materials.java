package tizio.dev.tsp.core.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import tizio.dev.tsp.MainClass;

import java.util.Locale;

public class Materials {


    public static final ResourceLocation DEFAULT_DEBUG_TEXTURE = Texture.add("textures/engine/no_texture");

    public static final ResourceLocation SPACE_SKYBOX = Texture.addEnv("milky_way");
    public static final ResourceLocation DARK_SPACE_SKYBOX = Texture.addEnv("spacebox_dark");
    public static final ResourceLocation COMET_ELEMENT = Texture.addEnv("comet");
    public static final ResourceLocation NULL = Texture.addEnv("skybox_null");

    public static final ResourceLocation OVERWORLD_MAT = Texture.add("textures/planets/overworld");
    public static final ResourceLocation OVERWORLD_NIGHT_MAT = Texture.add("textures/planets/overworld_night");

    private Materials() {}

    public static ResourceLocation getNoTexture() {
        return DEFAULT_DEBUG_TEXTURE;
    }

    protected static class Texture {

        protected static ResourceLocation add(String path) {
            return ResourceLocation.fromNamespaceAndPath(MainClass.MODID, path + ".png");
        }

        protected static ResourceLocation addEnv(String path) {
            return ResourceLocation.fromNamespaceAndPath(MainClass.MODID, "textures/environment/" + path + ".png");
        }
    }

    /**
     * Resolves a texture location from a raw path string.
     * Uses DEFAULT_DEBUG_TEXTURE ("textures/planets/no_texture.png") as static hardcoded fallback
     * whenever rawTexture is null, blank, or fails resolution.
     */

    public static ResourceLocation resolveTextureLocation(String rawTexture) {
        if (rawTexture == null || rawTexture.isBlank()) {
            return DEFAULT_DEBUG_TEXTURE;
        }

        try {

            String path = rawTexture.trim().replace('\\', '/').toLowerCase(Locale.ROOT);

            if (path.startsWith("/")) { path = path.substring(1); }

            String namespace = MainClass.MODID;

            if (path.contains(":")) {
                String[] parts = path.split(":", 2);
                namespace = parts[0];
                path = parts[1];
            }

            if (path.isEmpty() || path.endsWith("/")) {
                return DEFAULT_DEBUG_TEXTURE;
            }

            if (!path.startsWith("textures/")) {
                if (path.contains("/")) {
                    path = "textures/" + path;
                } else {
                    path = "textures/planets/" + path;
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

    public static ResourceLocation resolveSkyTextureLocation(String rawTexture) {
        if (rawTexture == null || rawTexture.isBlank()) {
            return DEFAULT_DEBUG_TEXTURE;
        }

        try {

            String path = rawTexture.trim().replace('\\', '/').toLowerCase(Locale.ROOT);

            if (path.startsWith("/")) { path = path.substring(1); }

            String namespace = MainClass.MODID;

            if (path.contains(":")) {
                String[] parts = path.split(":", 2);
                namespace = parts[0];
                path = parts[1];
            }

            if (path.isEmpty() || path.endsWith("/")) {
                return DEFAULT_DEBUG_TEXTURE;
            }

            if (!path.startsWith("textures/")) {
                if (path.contains("/")) {
                    path = "textures/" + path;
                } else {
                    path = "textures/environment/" + path;
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
