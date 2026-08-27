package tizio.dev.tsp.mixin.gravity;

import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import tizio.dev.tsp.core.handlers.gravity.GravityManager;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityGravityMixin {

    @ModifyArg(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"),
            index = 1
    )
    private double tsp$modifyFallingBlockGravity(double y) {
        if (Math.abs(y - (-0.04D)) < 1e-5) {
            return -GravityManager.getFallingBlockGravity((FallingBlockEntity) (Object) this);
        }
        return y;
    }
}
