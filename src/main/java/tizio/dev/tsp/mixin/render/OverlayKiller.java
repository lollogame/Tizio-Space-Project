package tizio.dev.tsp.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tizio.dev.tsp.core.gui.PhotoModeScreen;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class OverlayKiller {

    @Shadow
    @Nullable
    public Overlay overlay;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void tsp$clearOverlayOnPhotoMode(Screen guiScreen, CallbackInfo ci) {
        if (guiScreen instanceof PhotoModeScreen) this.overlay = null;
    }

}
