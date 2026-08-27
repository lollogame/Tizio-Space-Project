package tizio.dev.tsp.mixin.render;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyIn(Dist.CLIENT)
@Mixin(GameRenderer.class)
public abstract class DepthMixin {

    @Inject(method = {"getDepthFar"}, at = {@At("HEAD")}, cancellable = true)
    public void onGetDepthFar(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Float.POSITIVE_INFINITY);
    }
}
