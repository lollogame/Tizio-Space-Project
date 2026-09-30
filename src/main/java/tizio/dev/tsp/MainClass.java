package tizio.dev.tsp;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;
import tizio.dev.tsp.config.ConfigManager;
import tizio.dev.tsp.core.utils.Utils;
import tizio.dev.tsp.registry.*;

@Mod(MainClass.MODID)
public class MainClass {

    public static final String MODID = "tsp";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean DEBUG = resolveDebug();

    public MainClass() {

        Utils.ModLoadingCheck.ensureSafeLoad();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ConfigManager.register();
        RegisterPackets.register();
        loadRegisters(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[" + MODID.toUpperCase() + "] Mod fully loaded. Debug: " + DEBUG);

    }

    private static boolean resolveDebug() {
        if (Boolean.getBoolean(MODID + ".debug")) {
            return true;
        }
        if ("true".equalsIgnoreCase(System.getenv(MODID.toUpperCase() + "_DEBUG"))) {
            return true;
        }
        return !FMLLoader.isProduction();
    }

    private void loadRegisters(IEventBus bus) {

        RegisterBlocks.register(bus);
        RegisterStructures.register(bus);
        RegisterFeatures.register(bus);
        RegisterItems.register(bus);
        RegisterSounds.register(bus);
        RegisterTabs.register(bus);

        LOGGER.info("[" + MODID.toUpperCase() + "] All Registries have finished.");
    }
}