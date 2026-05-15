package tizio.dev.engine.utils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.engine.gui.DebugUniformGui;
import tizio.dev.engine.volume.VolumeRenderUtil;
import tizio.dev.tsp.MainClass;

@Mod.EventBusSubscriber(modid = MainClass.MODID, value = Dist.CLIENT)
public final class DebugKeyBindings {

    public static final KeyMapping TOGGLE_GUI = new KeyMapping(
            "key.tsp.debug_gui",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F8,
            "key.categories.tsp"
    );

    private DebugKeyBindings() {}

    @Mod.EventBusSubscriber(modid = MainClass.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_GUI);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (TOGGLE_GUI.consumeClick()) {
            if (mc.screen instanceof DebugUniformGui) {
                mc.setScreen(null);
            } else if (VolumeRenderUtil.DEBUG) {
                mc.setScreen(new DebugUniformGui());
            }
        }
    }
}
