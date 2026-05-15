package tizio.dev.engine.utils;

import net.minecraft.resources.ResourceLocation;
import tizio.dev.tsp.MainClass;

import java.util.HashMap;
import java.util.Map;

public class Materials {

    //SHItty class
    private static final Map<String, Object> TEXTURE_CACHE = new HashMap<>();

    private static final ResourceLocation NO_TEXTURE = Texture.add("textures/planets/debug");

    public static final ResourceLocation NAV_P_SELECTOR = Texture.add("textures/grid");
    public static final ResourceLocation SPACE_SKYBOX = Texture.addEnv("milky_way");
    public static final ResourceLocation DARK_SPACE_SKYBOX = Texture.addEnv("spacebox_dark");
    public static final ResourceLocation COMET_ELEMENT = Texture.addEnv("comet");
    public static final ResourceLocation NULL = Texture.addEnv("skybox_null");

    public static final ResourceLocation OVERWORLD_MAT = Texture.add("textures/planets/overworld");
    public static final ResourceLocation OVERWORLD_NIGHT_MAT = Texture.add("textures/planets/overworld_night");

    private Materials() {
    }

    public static Map<String, Object> getCache() {
        return TEXTURE_CACHE;
    }

    public static final ResourceLocation getNoTexture() {
        return NO_TEXTURE;
    }

    public static void callRegistry() {
        MainClass.LOGGER.info("[" + MainClass.MODID + "]: Materials Loaded");
    }

    protected static class Texture {

        protected static ResourceLocation add(String path) {
            return new ResourceLocation(MainClass.MODID, path + ".png");
        }

        protected static ResourceLocation addEnv(String path) {
            return new ResourceLocation(MainClass.MODID, "textures/environment/" + path + ".png");
        }
    }

    public static void clearCache() {
        TEXTURE_CACHE.clear();
        MainClass.LOGGER.info("[" + MainClass.MODID.toUpperCase() + " Materials" + "] Texture cache cleared");
    }
}
