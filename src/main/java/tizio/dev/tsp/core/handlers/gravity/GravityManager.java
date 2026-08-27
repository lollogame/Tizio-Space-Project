package tizio.dev.tsp.core.handlers.gravity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import tizio.dev.tsp.core.data.CelestialJsonLoader;

import java.util.HashMap;
import java.util.Map;


public final class GravityManager {

    /** Standard gravitational acceleration on Earth (9.80665 m/s²). */
    public static final double EARTH_GRAVITY_MS2 = 9.80665D;

    public static final double VANILLA_LIVING_GRAVITY = 0.08D;
    public static final double VANILLA_ITEM_GRAVITY = 0.04D;
    public static final double VANILLA_FALLING_BLOCK_GRAVITY = 0.04D;
    public static final double VANILLA_ARROW_GRAVITY = 0.05D;
    public static final double VANILLA_TNT_GRAVITY = 0.04D;

    private static final Map<String, Double> DEFAULT_GRAVITY_MAP = new HashMap<>();

    static {
        DEFAULT_GRAVITY_MAP.put("tsp:space", 0.0D);
        DEFAULT_GRAVITY_MAP.put("tsp:moon", 1.62D);
        DEFAULT_GRAVITY_MAP.put("tsp:mars", 3.72D);
    }

    private GravityManager() {}

    public static double getGravityMs2(Level level) {
        if (level == null) return EARTH_GRAVITY_MS2;

        CelestialJsonLoader.ensureLoaded();

        String dimId = level.dimension().location().toString();

        Float jsonGravity = CelestialJsonLoader.getGravityForDimension(dimId);
        if (jsonGravity != null) {
            return Math.max(0.0D, jsonGravity.doubleValue());
        }

        return DEFAULT_GRAVITY_MAP.getOrDefault(dimId, EARTH_GRAVITY_MS2);
    }

    public static double getGravityScale(Level level) {
        double ms2 = getGravityMs2(level);
        if (ms2 <= 0.0D) return 0.0D;
        return ms2 / EARTH_GRAVITY_MS2;
    }

    public static double getGravityScale(Entity entity) {
        if (entity == null || entity.isNoGravity()) {
            return 0.0D;
        }
        return getGravityScale(entity.level());
    }

    public static double getLivingGravity(LivingEntity entity) {
        return VANILLA_LIVING_GRAVITY * getGravityScale(entity);
    }

    public static double getItemGravity(ItemEntity entity) {
        return VANILLA_ITEM_GRAVITY * getGravityScale(entity);
    }

    public static double getFallingBlockGravity(FallingBlockEntity entity) {
        return VANILLA_FALLING_BLOCK_GRAVITY * getGravityScale(entity);
    }

    public static double getArrowGravity(AbstractArrow entity) {
        return VANILLA_ARROW_GRAVITY * getGravityScale(entity);
    }

    public static float getThrowableGravity(ThrowableProjectile entity, float baseGravity) {
        return (float) (baseGravity * getGravityScale(entity));
    }

    public static double getTntGravity(PrimedTnt entity) {
        return VANILLA_TNT_GRAVITY * getGravityScale(entity);
    }

    public static float getFallDamageScale(LivingEntity entity) {
        double scale = getGravityScale(entity);
        if (scale <= 0.05D) return 0.0F;
        return (float) scale;
    }
}
