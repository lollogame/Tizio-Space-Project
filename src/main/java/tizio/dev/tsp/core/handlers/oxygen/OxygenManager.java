package tizio.dev.tsp.core.handlers.oxygen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.resources.armor.CustomArmorItem;

public final class OxygenManager {

    public static final String OXYGEN_TAG = "tsp_oxygen";
    public static final float MAX_OXYGEN = 1.0F;
    public static final float MIN_OXYGEN = 0.0F;
    public static final float OXYGEN_RECOVERY_RATE = 0.005F;
    public static final float OXYGEN_DEPLETION_RATE = 0.05F;
    public static final float SUFFOCATION_DAMAGE = 2.0F;

    private OxygenManager() {}

    public static float getOxygen(Player player) {
        if (player == null) return MAX_OXYGEN;
        if (!player.getPersistentData().contains(OXYGEN_TAG)) {
            player.getPersistentData().putFloat(OXYGEN_TAG, MAX_OXYGEN);
            return MAX_OXYGEN;
        }
        return Math.max(MIN_OXYGEN, Math.min(MAX_OXYGEN, player.getPersistentData().getFloat(OXYGEN_TAG)));
    }

    public static void setOxygen(Player player, float oxygen) {
        if (player == null) return;
        float clamped = Math.max(MIN_OXYGEN, Math.min(MAX_OXYGEN, oxygen));
        player.getPersistentData().putFloat(OXYGEN_TAG, clamped);
    }

    public static boolean hasFullSpaceSuit(LivingEntity entity) {
        if (entity == null) return false;

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof CustomArmorItem)) {
                return false;
            }
        }
        return true;
    }

    public static int getEquippedSpaceSuitPieces(LivingEntity entity) {
        if (entity == null) return 0;
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof CustomArmorItem) {
                count++;
            }
        }
        return count;
    }

    public static boolean hasOxygenInEnvironment(Level level) {
        if (level == null) return true;
        String dimId = level.dimension().location().toString();
        Boolean hasOxygen = CelestialJsonLoader.hasOxygenForDimension(dimId);
        if (hasOxygen != null) {
            return hasOxygen;
        }
        return "minecraft:overworld".equalsIgnoreCase(dimId);
    }

    public static void tickOxygen(ServerPlayer player) {
        if (player == null || !player.isAlive()) return;

        if (player.isCreative() || player.isSpectator()) {
            setOxygen(player, MAX_OXYGEN);
            return;
        }

        boolean envOxygen = hasOxygenInEnvironment(player.level());
        boolean hasSuit = hasFullSpaceSuit(player);
        float currentOxygen = getOxygen(player);

        if (envOxygen) {
            currentOxygen = Math.min(MAX_OXYGEN, currentOxygen + OXYGEN_RECOVERY_RATE);
        } else {
            if (hasSuit) {
                currentOxygen = Math.min(MAX_OXYGEN, currentOxygen + OXYGEN_RECOVERY_RATE);
            } else {
                currentOxygen = Math.max(MIN_OXYGEN, currentOxygen - OXYGEN_DEPLETION_RATE);
                if (currentOxygen <= MIN_OXYGEN) {
                    if (player.tickCount % 20 == 0) {
                        player.hurt(player.damageSources().generic(), SUFFOCATION_DAMAGE);
                    }
                }
            }
        }

        setOxygen(player, currentOxygen);
    }
}
