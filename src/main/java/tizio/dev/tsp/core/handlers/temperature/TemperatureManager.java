package tizio.dev.tsp.core.handlers.temperature;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tizio.dev.tsp.config.DataConfig;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.handlers.oxygen.OxygenManager;
import tizio.dev.tsp.core.utils.Utils;

public final class TemperatureManager {

    public static final String TEMPERATURE_TAG = "tsp_temperature";
    public static final float MIN_TEMPERATURE = -1.0F;
    public static final float MAX_TEMPERATURE = 1.0F;
    public static final float NEUTRAL_TEMPERATURE = 0.0F;

    public static final float SUIT_REGULATION_RATE = 0.01F;
    public static final float ENVIRONMENT_EXPOSURE_RATE = 0.005F;

    public static final float FREEZING_THRESHOLD = -0.7F;
    public static final float EXTREME_FREEZING_THRESHOLD = -0.9F;
    public static final float HEAT_THRESHOLD = 0.7F;
    public static final float EXTREME_HEAT_THRESHOLD = 0.9F;

    public static final float TEMPERATURE_DAMAGE = 1.0F;

    public static final int ARMOR_HEAT_DAMAGE_AMOUNT = 1;
    public static final int ARMOR_EXTREME_HEAT_DAMAGE_AMOUNT = 3;

    public static float SOLAR_HEAT_TRIGGER_DISTANCE = 1.0F;
    private static final double SOLAR_HEAT_MIN_SAFE_MULTIPLIER = 1.0;
    private static final double SOLAR_HEAT_MAX_SAFE_MULTIPLIER = 1.2;

    public static final float SUN_CONTACT_TEMPERATURE = 1000.0F;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private TemperatureManager() {}

    public static float getTemperature(Player player) {
        if (player == null) return NEUTRAL_TEMPERATURE;
        if (!player.getPersistentData().contains(TEMPERATURE_TAG)) {
            player.getPersistentData().putFloat(TEMPERATURE_TAG, NEUTRAL_TEMPERATURE);
            return NEUTRAL_TEMPERATURE;
        }
        return Utils.clamp(player.getPersistentData().getFloat(TEMPERATURE_TAG), MIN_TEMPERATURE, MAX_TEMPERATURE);
    }

    public static void setTemperature(Player player, float temperature) {
        if (player == null) return;
        float clamped = Utils.clamp(temperature, MIN_TEMPERATURE, MAX_TEMPERATURE);
        player.getPersistentData().putFloat(TEMPERATURE_TAG, clamped);
    }

    public static float getEnvironmentTemperature(Level level) {
        if (level == null) return NEUTRAL_TEMPERATURE;
        String dimId = Utils.getDimensionId(level);
        Float jsonTemp = CelestialJsonLoader.getTemperatureForDimension(dimId);
        if (jsonTemp != null) {
            return Utils.clamp(jsonTemp, MIN_TEMPERATURE, MAX_TEMPERATURE);
        }
        if ("minecraft:overworld".equalsIgnoreCase(dimId)) {
            return NEUTRAL_TEMPERATURE;
        }
        if (CelestialJsonLoader.isSpaceDimension(dimId)) {
            return MIN_TEMPERATURE;
        }
        return NEUTRAL_TEMPERATURE;
    }

    public static float calculateSolarHeat(ServerPlayer player) {

        if (player == null) return 0.0F;
        Level level = player.level();
        String dimId = Utils.getDimensionId(level);

        if (!CelestialJsonLoader.isSpaceDimension(dimId)) {
            return 0.0F;
        }

        Float sunRadiusBoxed = CelestialJsonLoader.getStarPhysicalRadius(dimId);
        if (sunRadiusBoxed == null || sunRadiusBoxed <= 0.0F) {
            return 0.0F;
        }
        float sunRadius = sunRadiusBoxed;

        Vec3 playerPos = player.position();
        Vec3 sunPos = DataConfig.System.ORIGIN_DEF;

        double distance = playerPos.distanceTo(sunPos);

        double extremeDistance = sunRadius * 1.0;
        double safeMultiplier = SOLAR_HEAT_MIN_SAFE_MULTIPLIER + (SOLAR_HEAT_MAX_SAFE_MULTIPLIER - SOLAR_HEAT_MIN_SAFE_MULTIPLIER) * Utils.clamp(SOLAR_HEAT_TRIGGER_DISTANCE, 0.0F, 1.0F);
        double safeDistance = sunRadius * safeMultiplier;

        if (distance >= safeDistance) {
            return 0.0F;
        }

        if (distance <= extremeDistance) {
            return 2.0F;
        }

        double factor = 1.0 - ((distance - extremeDistance) / (safeDistance - extremeDistance));
        return Utils.clamp((float) factor * 1.8F, 0.0F, 2.0F);
    }

    public static float getSolarSurfaceTemperature(float solarHeat) {
        return Utils.clamp(solarHeat, 0.0F, 2.0F) / 2.0F * SUN_CONTACT_TEMPERATURE;
    }

    public static void tickTemperature(ServerPlayer player) {
        if (player == null || !player.isAlive()) return;

        if (player.isCreative() || player.isSpectator()) {
            setTemperature(player, NEUTRAL_TEMPERATURE);
            return;
        }

        boolean hasSuit = OxygenManager.hasFullSpaceSuit(player);
        float baseEnvTemp = getEnvironmentTemperature(player.level());
        float solarHeat = calculateSolarHeat(player);
        float effectiveEnvTemp = Utils.clamp(baseEnvTemp + (solarHeat * 2.0F), MIN_TEMPERATURE, 2.0F);
        float currentTemp = getTemperature(player);

        if (hasSuit) {
            if (effectiveEnvTemp > HEAT_THRESHOLD) {
                float excessHeat = effectiveEnvTemp - HEAT_THRESHOLD;
                currentTemp += excessHeat * 0.02F;
            } else {
                if (currentTemp > NEUTRAL_TEMPERATURE) {
                    currentTemp = Math.max(NEUTRAL_TEMPERATURE, currentTemp - SUIT_REGULATION_RATE);
                } else if (currentTemp < NEUTRAL_TEMPERATURE) {
                    currentTemp = Math.min(NEUTRAL_TEMPERATURE, currentTemp + SUIT_REGULATION_RATE);
                }
            }
        } else {
            if (currentTemp < effectiveEnvTemp) {
                currentTemp = Math.min(effectiveEnvTemp, currentTemp + ENVIRONMENT_EXPOSURE_RATE * (1.0F + solarHeat));
            } else if (currentTemp > effectiveEnvTemp) {
                currentTemp = Math.max(effectiveEnvTemp, currentTemp - ENVIRONMENT_EXPOSURE_RATE);
            }
        }

        currentTemp = Utils.clamp(currentTemp, MIN_TEMPERATURE, MAX_TEMPERATURE);

        if (currentTemp <= FREEZING_THRESHOLD) {
            player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze() + 60, player.getTicksFrozen() + 8));
            if (currentTemp <= EXTREME_FREEZING_THRESHOLD && player.tickCount % 30 == 0) {
                player.hurt(player.damageSources().freeze(), TEMPERATURE_DAMAGE);
            }
        } else if (currentTemp >= HEAT_THRESHOLD || solarHeat >= 1.2F) {
            if (player.getRemainingFireTicks() <= 0) {
                player.setSecondsOnFire(3);
            }
            boolean extremeHeat = currentTemp >= EXTREME_HEAT_THRESHOLD || solarHeat >= 1.5F;
            if (extremeHeat && player.tickCount % 20 == 0) {
                player.hurt(player.damageSources().onFire(), TEMPERATURE_DAMAGE * (solarHeat >= 1.5F ? 2.0F : 1.0F));
            }
            damageArmorFromHeat(player, extremeHeat ? ARMOR_EXTREME_HEAT_DAMAGE_AMOUNT : ARMOR_HEAT_DAMAGE_AMOUNT);
        }

        setTemperature(player, currentTemp);
    }

    private static void damageArmorFromHeat(ServerPlayer player, int amount) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                stack.hurtAndBreak(amount, player, brokenEntity -> brokenEntity.broadcastBreakEvent(slot));
            }
        }
    }

}