package tizio.dev.tsp.core.handlers.temperature;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.core.handlers.oxygen.OxygenManager;

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

    private TemperatureManager() {}

    public static float getTemperature(Player player) {
        if (player == null) return NEUTRAL_TEMPERATURE;
        if (!player.getPersistentData().contains(TEMPERATURE_TAG)) {
            player.getPersistentData().putFloat(TEMPERATURE_TAG, NEUTRAL_TEMPERATURE);
            return NEUTRAL_TEMPERATURE;
        }
        return Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, player.getPersistentData().getFloat(TEMPERATURE_TAG)));
    }

    public static void setTemperature(Player player, float temperature) {
        if (player == null) return;
        float clamped = Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, temperature));
        player.getPersistentData().putFloat(TEMPERATURE_TAG, clamped);
    }

    public static float getEnvironmentTemperature(Level level) {
        if (level == null) return NEUTRAL_TEMPERATURE;
        String dimId = level.dimension().location().toString();
        Float jsonTemp = CelestialJsonLoader.getTemperatureForDimension(dimId);
        if (jsonTemp != null) {
            return Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, jsonTemp));
        }
        if ("minecraft:overworld".equalsIgnoreCase(dimId)) {
            return NEUTRAL_TEMPERATURE;
        }
        if ("tsp:space".equalsIgnoreCase(dimId)) {
            return MIN_TEMPERATURE;
        }
        return NEUTRAL_TEMPERATURE;
    }

    public static void tickTemperature(ServerPlayer player) {
        if (player == null || !player.isAlive()) return;

        if (player.isCreative() || player.isSpectator()) {
            setTemperature(player, NEUTRAL_TEMPERATURE);
            return;
        }

        boolean hasSuit = OxygenManager.hasFullSpaceSuit(player);
        float envTemp = getEnvironmentTemperature(player.level());
        float currentTemp = getTemperature(player);

        if (hasSuit) {
            if (currentTemp > NEUTRAL_TEMPERATURE) {
                currentTemp = Math.max(NEUTRAL_TEMPERATURE, currentTemp - SUIT_REGULATION_RATE);
            } else if (currentTemp < NEUTRAL_TEMPERATURE) {
                currentTemp = Math.min(NEUTRAL_TEMPERATURE, currentTemp + SUIT_REGULATION_RATE);
            }
        } else {
            if (currentTemp < envTemp) {
                currentTemp = Math.min(envTemp, currentTemp + ENVIRONMENT_EXPOSURE_RATE);
            } else if (currentTemp > envTemp) {
                currentTemp = Math.max(envTemp, currentTemp - ENVIRONMENT_EXPOSURE_RATE);
            }
        }

        if (currentTemp <= FREEZING_THRESHOLD) {
            player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze() + 60, player.getTicksFrozen() + 8));
            if (currentTemp <= EXTREME_FREEZING_THRESHOLD && player.tickCount % 30 == 0) {
                player.hurt(player.damageSources().freeze(), TEMPERATURE_DAMAGE);
            }
        } else if (currentTemp >= HEAT_THRESHOLD) {
            if (player.getRemainingFireTicks() <= 0) {
                player.setSecondsOnFire(2);
            }
            if (currentTemp >= EXTREME_HEAT_THRESHOLD && player.tickCount % 30 == 0) {
                player.hurt(player.damageSources().onFire(), TEMPERATURE_DAMAGE);
            }
        }

        setTemperature(player, currentTemp);
    }
}
