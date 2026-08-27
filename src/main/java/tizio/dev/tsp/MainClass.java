package tizio.dev.tsp;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import tizio.dev.tsp.config.ConfigManager;
import tizio.dev.tsp.core.network.PacketsRegistry;
import tizio.dev.tsp.registry.*;

@Mod(MainClass.MODID)
public class MainClass {

    public static final String MODID = "tsp";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MainClass() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ConfigManager.register();
        PacketsRegistry.register();

        loadRegisters(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

    }

    private void loadRegisters(IEventBus bus) {

        RegisterBlocks.register(bus);
        RegisterStructures.register(bus);
        RegisterFeatures.register(bus);
        RegisterItems.register(bus);
        RegisterSounds.register(bus);
        RegisterTabs.register(bus);

        LOGGER.info("[" + getClass().getName() + "] All Registries have finished.");
    }

}
