package tizio.dev.tsp.core.handlers.teleport;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

public class SpaceTeleporter implements ITeleporter {

    private final Vec3 targetPos;
    private final Vec3 targetDeltaMovement;
    private final float targetYaw;
    private final float targetPitch;
    private final boolean applyDeltaMovement;

    public SpaceTeleporter(Vec3 targetPos, Vec3 targetDeltaMovement, float targetYaw, float targetPitch, boolean applyDeltaMovement) {
        this.targetPos = targetPos;
        this.targetDeltaMovement = targetDeltaMovement;
        this.targetYaw = targetYaw;
        this.targetPitch = targetPitch;
        this.applyDeltaMovement = applyDeltaMovement;
    }

    public SpaceTeleporter(Vec3 targetPos, float targetYaw, float targetPitch) {
        this(targetPos, Vec3.ZERO, targetYaw, targetPitch, false);
    }

    @Override
    public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
        Entity repositioned = repositionEntity.apply(false);
        if (repositioned != null) {
            repositioned.moveTo(targetPos.x, targetPos.y, targetPos.z, targetYaw, targetPitch);
            if (applyDeltaMovement && targetDeltaMovement != null) {
                repositioned.setDeltaMovement(targetDeltaMovement);
                repositioned.hurtMarked = true;
            }
            if (repositioned instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.teleport(targetPos.x, targetPos.y, targetPos.z, targetYaw, targetPitch);
            }
        }
        return repositioned;
    }

    public Vec3 getTargetPos() {
        return targetPos;
    }

    public Vec3 getTargetDeltaMovement() {
        return targetDeltaMovement;
    }

    public float getTargetYaw() {
        return targetYaw;
    }

    public float getTargetPitch() {
        return targetPitch;
    }
}
