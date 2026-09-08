package tizio.dev.tsp.core.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import tizio.dev.tsp.MainClass;

public class Utils {

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
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

    public static boolean inDimension(String dimensionId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || dimensionId == null) return false;

        return mc.level.dimension().location().toString().equals(dimensionId);
    }

    public static boolean inDimension(Entity entity, String dimensionId) {
        if (entity == null || entity.level() == null || dimensionId == null) return false;

        return entity.level().dimension().location().toString().equals(dimensionId);
    }

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

    public final class ModLoadingCheck {

        private static final String[] BLACKLIST = {"oculus", "iris"};

        public static void ensureSafeLoad() {
            for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
                String className = frame.getClassName();
                if (className.contains("org.spongepowered.asm.mixin") || className.contains("com.llamalad7.mixinextras")) {
                    Runtime.getRuntime().halt(666);
                }
            }

            for (String modId : BLACKLIST) {
                if (isModLoaded(modId)) {
                    System.err.println("["+MainClass.MODID.toUpperCase()+"] Failed to load the mod due to incompatible mod: " + modId);
                    Runtime.getRuntime().halt(1);
                }
            }
        }
    }
}
