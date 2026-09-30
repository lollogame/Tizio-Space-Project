package tizio.dev.tsp.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tizio.dev.tsp.core.data.CelestialJsonLoader;

@Mixin(Entity.class)
public abstract class NoVoidDamage {

    @Shadow
    protected Level level;

    @Shadow
    public abstract double getY();

    @Inject(method = "checkBelowWorld", at = @At("HEAD"), cancellable = true)
    private void tsp$checkBelowWorld(CallbackInfo ci) {
        if (CelestialJsonLoader.isSpaceDimension(this.level)) {
            if (this.getY() < (double) (this.level.getMinBuildHeight() - 64)) {
                ci.cancel();
            }
        }
    }
}
