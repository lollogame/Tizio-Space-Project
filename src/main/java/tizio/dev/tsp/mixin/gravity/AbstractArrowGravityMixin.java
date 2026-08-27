package tizio.dev.tsp.mixin.gravity;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tizio.dev.tsp.core.handlers.gravity.GravityManager;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowGravityMixin {

    @Shadow
    protected boolean inGround;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tsp$modifyArrowGravity(CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;

        if (arrow.isNoGravity() || this.inGround || !arrow.isAlive()) {
            return;
        }

        double vanillaGravity = GravityManager.VANILLA_ARROW_GRAVITY;
        double customGravity = GravityManager.getArrowGravity(arrow);
        double diff = vanillaGravity - customGravity;

        if (Math.abs(diff) > 1e-6) {
            Vec3 motion = arrow.getDeltaMovement();
            arrow.setDeltaMovement(motion.x, motion.y + diff, motion.z);
        }
    }
}
