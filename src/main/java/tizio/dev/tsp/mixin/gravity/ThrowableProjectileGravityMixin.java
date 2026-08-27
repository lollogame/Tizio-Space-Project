package tizio.dev.tsp.mixin.gravity;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tizio.dev.tsp.core.handlers.gravity.GravityManager;

@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileGravityMixin {

    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true)
    private void tsp$scaleThrowableGravity(CallbackInfoReturnable<Float> cir) {
        ThrowableProjectile entity = (ThrowableProjectile) (Object) this;
        float baseGravity = cir.getReturnValueF();
        cir.setReturnValue(GravityManager.getThrowableGravity(entity, baseGravity));
    }
}
