package tizio.dev.tsp.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LevelRenderer.class)
public class CloudDistance {

    @ModifyConstant(method = "buildClouds", constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = -3))
    private int modifyFancyStart(int original) {
        return -32;
    }

    @ModifyConstant(method = "buildClouds", constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 4))
    private int modifyFancyEnd(int original) {
        return 32;
    }

    @ModifyConstant(method = "buildClouds", constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = -32))
    private int modifyFastStart(int original) {
        return -128;
    }

    @ModifyConstant(method = "buildClouds", constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 32))
    private int modifyFastEnd(int original) {
        return 128;
    }

    @ModifyConstant(method = "renderClouds", constant = @org.spongepowered.asm.mixin.injection.Constant(floatValue = 12.0F))
    private float modifyCloudScale(float original) {
        return 32.0F;
    }

    @ModifyConstant(method = "renderClouds", constant = @org.spongepowered.asm.mixin.injection.Constant(doubleValue = 2048.0D))
    private double modifyCloudWrap(double original) {
        return 12192.0D;
    }

}
