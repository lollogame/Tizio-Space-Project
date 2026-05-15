package tizio.dev.tsp.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

public class GravityUtil {

    public static double getGravity(LivingEntity entity) {

        if (Utils.inDimension(entity, "tsp:space")) return 0.005;
        if (Utils.inDimension(entity, "tsp:moon")) return 0.03;
        if (Utils.inDimension(entity, "tsp:mars")) return 0.05;

        return 0.08;
    }

    public static class FallDamage {

        public static float getDamageScale(LivingEntity entity) {

            if (Utils.inDimension(entity, "tsp:space")) return 0.0f;
            if (Utils.inDimension(entity, "tsp:moon")) return 0.2f;
            if (Utils.inDimension(entity, "tsp:mars")) return 0.5f;

            return 1.0f;
        }
    }

    public static class Projectile {

        public static double getGravity(Entity entity) {

            if (Utils.inDimension(entity, "tsp:space")) return 0.0;
            if (Utils.inDimension(entity, "tsp:moon")) return 0.02;
            if (Utils.inDimension(entity, "tsp:mars")) return 0.04;

            return 0.03;
        }
    }

    public static class Item {

        public static double getGravity(ItemEntity entity) {

            if (Utils.inDimension(entity, "tsp:space")) return 0.0;
            if (Utils.inDimension(entity, "tsp:moon")) return 0.02;
            if (Utils.inDimension(entity, "tsp:mars")) return 0.01;

            return entity.getDeltaMovement().y;
        }
    }
}
