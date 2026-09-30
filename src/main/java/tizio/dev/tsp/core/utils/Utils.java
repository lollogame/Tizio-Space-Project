package tizio.dev.tsp.core.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import tizio.dev.tsp.MainClass;

public class Utils {

    public static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        if (Float.isNaN(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean isModLoaded(String modId) {
        if (MainClass.MODID.equals(modId)) return false;
        if (modId != null && !modId.isEmpty()) {
            return ModList.get().isLoaded(modId);
        }
        return false;
    }

    @Deprecated
    public static boolean inDimension(String dimensionId) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ClientOnly.inDimensionClient(dimensionId);
        }
        return false;
    }

    @Deprecated
    public static boolean inDimension(Entity entity, String dimensionId) {
        if (entity == null || entity.level() == null || dimensionId == null) return false;

        return entity.level().dimension().location().toString().equals(dimensionId);
    }

    @Deprecated
    public static boolean inDimension(Level level, String dimensionId) {
        if (level == null || dimensionId == null) return false;

        return level.dimension().location().toString().equals(dimensionId);
    }


    public static String getDimensionId(Entity entity) {
        if (entity == null || entity.level() == null) return "";
        return entity.level().dimension().location().toString();
    }

    public static String getDimensionId(Level level) {
        if (level == null) return "";
        return level.dimension().location().toString();
    }

    public static boolean isBlackHole(String type) {
        return "blackhole".equalsIgnoreCase(type);
    }

    private static class ClientOnly {
        private static boolean inDimensionClient(String dimensionId) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null || dimensionId == null) return false;
            return mc.level.dimension().location().toString().equals(dimensionId);
        }
    }

    public final class ModLoadingCheck {

        private static final String[] BLACKLIST = {"oculus", "iris"};

        public static void ensureSafeLoad() {
            for (String modId : BLACKLIST) {
                if (isModLoaded(modId)) {
                    MainClass.LOGGER.warn("[" + MainClass.MODID.toUpperCase() + "] Detected potentially incompatible mod: " + modId + ". Some visual features may not work as expected.");
                }
            }
        }
    }
}
