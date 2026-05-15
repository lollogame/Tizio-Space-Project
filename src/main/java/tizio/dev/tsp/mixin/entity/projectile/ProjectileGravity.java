package tizio.dev.tsp.mixin.entity.projectile;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tizio.dev.tsp.utils.GravityUtil;

@Mixin(Projectile.class)
public class ProjectileGravity {

    @Inject(method = "tick", at = @At("TAIL"))
    private void tsp$fixProjectileGravity(CallbackInfo ci) {
        Projectile p = (Projectile) (Object) this;

        if (p instanceof AbstractArrow) return;

        if (!p.isAlive()) return;

        if (p.onGround()) return;
        if (p.horizontalCollision) return;

        Vec3 motion = p.getDeltaMovement();

        double vanillaGravity = 0.03;
        double customGravity = GravityUtil.Projectile.getGravity(p);

        double newY = motion.y + vanillaGravity - customGravity;

        p.setDeltaMovement(motion.x, newY, motion.z);
    }
}