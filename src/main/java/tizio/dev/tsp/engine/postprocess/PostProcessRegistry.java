package tizio.dev.tsp.engine.postprocess;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import tizio.dev.tsp.MainClass;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PostProcessRegistry {

    private static final Map<ResourceLocation, PostChain> CHAINS = new HashMap<>();
    private static final Set<ResourceLocation> FAILED = new HashSet<>();
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    public static PostChain getOrLoad(ResourceLocation location) {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        checkResize(width, height);

        if (location == null || FAILED.contains(location)) {
            return null;
        }

        PostChain existing = CHAINS.get(location);
        if (existing != null) {
            return existing;
        }

        try {
            PostChain chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), location);
            chain.resize(width, height);
            CHAINS.put(location, chain);
            return chain;
        } catch (IOException exception) {
            FAILED.add(location);
            MainClass.LOGGER.error("Cannot load post shader '{}'", location, exception);
            return null;
        }
    }

    private static void checkResize(int width, int height) {
        if (width != lastWidth || height != lastHeight) {
            for (PostChain chain : CHAINS.values()) {
                if (chain != null) {
                    chain.resize(width, height);
                }
            }
            lastWidth = width;
            lastHeight = height;
        }
    }

    public static void clear() {
        for (PostChain chain : CHAINS.values()) {
            if (chain != null) chain.close();
        }
        CHAINS.clear();
        FAILED.clear();
    }
}