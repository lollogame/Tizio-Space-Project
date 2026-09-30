package tizio.dev.tsp.registry;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.config.ConfigManager;
import tizio.dev.tsp.core.gui.GraphicsSettings;
import tizio.dev.tsp.core.gui.PhotoModeScreen;
import tizio.dev.tsp.core.gui.SystemEditor;
import tizio.dev.tsp.engine.camera.CameraPlanetOrbit;

@Mod.EventBusSubscriber(modid = MainClass.MODID, value = Dist.CLIENT)
public final class RegisterKeybinds {

    public static final KeyMapping TOGGLE_EDITOR = new KeyMapping(
            "key.tsp.planet_editor",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F6,
            "key.categories.tsp"
    );

    private static final KeyMapping PHOTO_MODE = new KeyMapping(
            "key.tsp.photo_mode",
            InputConstants.KEY_F7,
            "key.categories.tsp"
    );

    private static final KeyMapping GRAPHICS_SETTINGS = new KeyMapping(
            "key.tsp.graphic_settings",
            InputConstants.KEY_F8,
            "key.categories.tsp"
    );

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        if (PHOTO_MODE.consumeClick()) Minecraft.getInstance().setScreen(new PhotoModeScreen());
        if (GRAPHICS_SETTINGS.consumeClick()) Minecraft.getInstance().setScreen(new GraphicsSettings());

        if (!ConfigManager.isDevelopmentMode()) return;
        while (TOGGLE_EDITOR.consumeClick()) {
            if (mc.screen instanceof SystemEditor) {
                mc.setScreen(null);
                CameraPlanetOrbit.deactivate();
            } else if (mc.level != null) {
                mc.setScreen(new SystemEditor());
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MainClass.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_EDITOR);
            event.register(PHOTO_MODE);
            event.register(GRAPHICS_SETTINGS);
        }
    }
}

