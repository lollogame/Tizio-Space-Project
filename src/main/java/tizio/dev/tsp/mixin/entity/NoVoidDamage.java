package tizio.dev.tsp.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import tizio.dev.tsp.core.data.CelestialJsonLoader;

@Mixin({Entity.class})
public abstract class NoVoidDamage {

    @Shadow
    protected Level level;

    @Shadow
    public abstract double getY();

    @Shadow
    protected abstract void onBelowWorld();

    @Overwrite
    public void checkBelowWorld() {
        if (this.getY() < (double) (this.level.getMinBuildHeight() - 64) && !CelestialJsonLoader.isSpaceDimension(this.level)) {
            this.onBelowWorld();
        }
    }

}
