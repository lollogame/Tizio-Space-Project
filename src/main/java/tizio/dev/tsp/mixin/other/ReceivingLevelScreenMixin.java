package tizio.dev.tsp.mixin.other;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReceivingLevelScreen.class)
public abstract class ReceivingLevelScreenMixin {

    /**
     * Annulla il rendering visivo della schermata di caricamento.
     * La logica di caricamento dei chunk continua a girare in background senza mostrare lo schermo marrone/grigio.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void tsp$hideLoadingScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();
    }
}
