package tizio.dev.tsp.mixin.render.camera;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraInvoker {

    @Invoker("setPosition")
    void callSetPosition(double x, double y, double z);

    @Invoker("setRotation")
    void callSetRotation(float yaw, float pitch);
}
