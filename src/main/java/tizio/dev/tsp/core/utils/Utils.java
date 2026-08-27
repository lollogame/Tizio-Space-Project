package tizio.dev.tsp.core.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.joml.Vector3f;
import tizio.dev.tsp.MainClass;

public class Utils {

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static Vec3 DegreesToDir(Vec3 rotation) {
        double yaw = Math.toRadians(rotation.y);
        double pitch = Math.toRadians(rotation.x);
        double roll = Math.toRadians(rotation.z);

        Vec3 dir = new Vec3(0, 1, 0);
        dir = rotateX(dir, pitch);
        dir = rotateY(dir, yaw);
        dir = rotateZ(dir, roll);

        return dir.normalize();
    }

    public static float getNightFactor(ClientLevel level, float partialTicks) {
        if (level == null) return 0.0f;

        float time = (level.getDayTime() + partialTicks) % 24000L;

        float duskStart = 12000f;
        float nightStart = 14000f;
        float dawnStart = 22000f;
        float dayStart = 24000f;

        if (time >= nightStart && time <= dawnStart) {
            return 1.0f;
        }

        if (time >= duskStart && time < nightStart) {
            return (time - duskStart) / (nightStart - duskStart);
        }

        if (time > dawnStart && time < dayStart) {
            return 1.0f - ((time - dawnStart) / (dayStart - dawnStart));
        }

        return 0.0f;
    }

    @Deprecated
    public static Vec3 lookAtPosition(Vec3 planePos, Vec3 targetPos) {
        Vec3 dir = targetPos.subtract(planePos);
        double dx = dir.x;
        double dy = dir.y;
        double dz = dir.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, distXZ));
        float roll = 0.0f;
        return new Vec3(pitch, yaw, roll);
    }

    public static void applyCameraBillboard(PoseStack poseStack, Camera camera) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - camera.getYRot()));
    }

    public static float smoothstep(float edge0, float edge1, float x) {
        x = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return x * x * (3 - 2 * x);
    }

    public static float getDistance(Vec3 point1, Vec3 point2) {
        if (point1 != null && point2 != null) {
            return (float) point1.distanceTo(point2);
        }
        return 0f;
    }

    public static Vec3 sunAngleToPos(double sunAngle, double distance) {
        double y = Math.cos(sunAngle);
        double x = Math.sin(sunAngle);
        double z = 0;
        return new Vec3(x * distance, y * distance, z * distance);
    }

    private static Vec3 rotateX(Vec3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double y = v.y * cos - v.z * sin;
        double z = v.y * sin + v.z * cos;
        return new Vec3(v.x, y, z);
    }

    private static Vec3 rotateY(Vec3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.x * cos + v.z * sin;
        double z = -v.x * sin + v.z * cos;
        return new Vec3(x, v.y, z);
    }

    private static Vec3 rotateZ(Vec3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.x * cos - v.y * sin;
        double y = v.x * sin + v.y * cos;
        return new Vec3(x, y, v.z);
    }

    public static float getVecDistance(Vec3 pos1, Vec3 pos2) {
        return (float) pos2.subtract(pos1).length();
    }

    public static double getRawDistance(Vec3 pos1, Vec3 pos2) {
        double deltaX = pos2.x() - pos1.x();
        double deltaY = pos2.y() - pos1.y();
        double deltaZ = pos2.z() - pos1.z();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    public static Vec3 angleToDir(float angleDegrees) {
        double radians = Math.toRadians(angleDegrees);
        double y = Math.sin(radians);
        return new Vec3(0, y, 0);
    }

    public static Vector3f computeLightDir(Vector3f sunPosition, Vector3f planetPosition) {
        Vector3f dir = new Vector3f(sunPosition).sub(planetPosition);
        float len = dir.length();
        if (len < 1e-6f) return new Vector3f(0.0f, 1.0f, 0.0f);
        return dir.div(len);
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

    public static boolean inDimension(ServerLevel level, String dimensionId) {
        if (level == null || dimensionId == null) return false;

        return level.dimension().location().toString().equals(dimensionId);
    }

    public static String getDimensionId(Entity entity) {
        return entity.level().dimension().location().toString();
    }

    public static boolean isModLoaded(String MODID){
        if(MODID == MainClass.MODID) return false;
        if(MODID != null || MODID.isEmpty()){
            return ModList.get().isLoaded(MODID);
        }
        return false;
    }
}
