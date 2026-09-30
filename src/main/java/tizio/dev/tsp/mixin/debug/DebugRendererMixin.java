package tizio.dev.tsp.mixin.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.debug.GameEventListenerRenderer;
import net.minecraft.client.renderer.debug.GameTestDebugRenderer;
import net.minecraft.client.renderer.debug.LightSectionDebugRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class DebugRendererMixin {

    @Unique
    private static final boolean forceDebugRender = false;
    @Shadow
    @Final
    public DebugRenderer.SimpleDebugRenderer collisionBoxRenderer;
    @Shadow
    @Final
    public DebugRenderer.SimpleDebugRenderer waterDebugRenderer;
    @Shadow
    @Final
    public DebugRenderer.SimpleDebugRenderer solidFaceRenderer;
    @Shadow
    @Final
    public DebugRenderer.SimpleDebugRenderer heightMapRenderer;
    @Shadow
    @Final
    public GameTestDebugRenderer gameTestDebugRenderer;
    @Shadow
    @Final
    public LightSectionDebugRenderer skyLightSectionDebugRenderer;
    @Shadow
    @Final
    public GameEventListenerRenderer gameEventListenerRenderer;
    @Shadow
    @Final
    public DebugRenderer.SimpleDebugRenderer lightDebugRenderer;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderDebug(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, double camX, double camY, double camZ, CallbackInfo ci) {
        if (!forceDebugRender) return;

        //this.collisionBoxRenderer.render(poseStack, bufferSource, camX, camY, camZ);
        this.waterDebugRenderer.render(poseStack, bufferSource, camX, camY, camZ);
        //this.solidFaceRenderer.render(poseStack, bufferSource, camX, camY, camZ);
        //this.heightMapRenderer.render(poseStack, bufferSource, camX, camY, camZ);
        this.gameTestDebugRenderer.render(poseStack, bufferSource, camX, camY, camZ);
        this.skyLightSectionDebugRenderer.render(poseStack, bufferSource, camX, camY, camZ);
        this.lightDebugRenderer.render(poseStack, bufferSource, camX, camY, camZ);
        this.gameEventListenerRenderer.render(poseStack, bufferSource, camX, camY, camZ);

    }
}