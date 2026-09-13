package tizio.dev.tsp.mixin.other;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tizio.dev.tsp.core.data.CelestialJsonLoader;

@Mixin(ServerLevel.class)
public class SLWeatherHandlerMixin {

    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void cancelSpaceWeatherCycle(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        ResourceLocation dimensionId = level.dimension().location();

        if (CelestialJsonLoader.isSpaceDimension(dimensionId)) {
            level.setRainLevel(0.0F);
            level.setThunderLevel(0.0F);
            ci.cancel();
        }
    }
}
