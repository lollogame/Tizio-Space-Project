package tizio.dev.tsp.mixin.render.lighting;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tizio.dev.tsp.core.celestial.lighting.LightShadeManager;

@OnlyIn(Dist.CLIENT)
@Mixin(RenderSystem.class)
public abstract class DiffuseLightingMixin {

    @Inject(method = "setupLevelDiffuseLighting(Lorg/joml/Vector3f;Lorg/joml/Vector3f;Lorg/joml/Matrix4f;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tsp$onSetupLevelDiffuseLighting(org.joml.Vector3f light0, org.joml.Vector3f light1, Matrix4f matrix4f, CallbackInfo ci) {
        if (LightShadeManager.isSpaceLightingActive() && LightShadeManager.isRenderingLevel()) {
            org.joml.Vector3f sun0 = LightShadeManager.getPrimarySunDirectionForEntities();
            org.joml.Vector3f sun1 = LightShadeManager.getSecondarySunDirectionForEntities();
            GlStateManager.setupLevelDiffuseLighting(sun0, sun1, matrix4f);
            ci.cancel();
        }
    }
}