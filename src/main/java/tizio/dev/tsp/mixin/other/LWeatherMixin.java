package tizio.dev.tsp.mixin.other;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tizio.dev.tsp.core.data.CelestialJsonLoader;


@Mixin(Level.class)
public class LWeatherMixin {

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void overrideSpaceRainLevel(float delta, CallbackInfoReturnable<Float> cir) {
        Level level = (Level) (Object) this;
        ResourceLocation dimensionId = level.dimension().location();

        if (CelestialJsonLoader.isSpaceDimension(dimensionId)) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void overrideSpaceThunderLevel(float delta, CallbackInfoReturnable<Float> cir) {
        Level level = (Level) (Object) this;
        ResourceLocation dimensionId = level.dimension().location();

        if (CelestialJsonLoader.isSpaceDimension(dimensionId)) {
            cir.setReturnValue(0.0F);
        }
    }
}
