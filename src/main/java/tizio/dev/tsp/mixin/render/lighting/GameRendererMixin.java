package tizio.dev.tsp.mixin.render.lighting;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tizio.dev.tsp.core.celestial.lighting.LightShadeManager;

@OnlyIn(Dist.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void tsp$onRenderLevelHead(float partialTicks, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        LightShadeManager.setRenderingLevel(true);
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void tsp$onRenderLevelReturn(float partialTicks, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        LightShadeManager.setRenderingLevel(false);
    }

}
