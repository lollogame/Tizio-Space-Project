package tizio.dev.tsp.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tizio.dev.tsp.core.celestial.instance.elements.planet.PlanetInstance;
import tizio.dev.tsp.core.data.CelestialJsonLoader;

import java.time.Instant;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererCloudsMixin {

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void tsp$onRenderClouds(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        ci.cancel();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        ResourceLocation dimensionId = mc.level.dimension().location();
        if ("tsp".equals(dimensionId.getNamespace()) && "space".equals(dimensionId.getPath())) {
            return;
        }

        CelestialJsonLoader.BodySpatialInfo info = CelestialJsonLoader.getBodyByDimension(dimensionId.toString(), Instant.now());
        if (info == null) {
            return;
        }

        PlanetInstance.Config body = info.body();
        if (body.clouds == null || !body.clouds.enabled) {
            return;
        }
    }
}