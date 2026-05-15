package tizio.dev.tsp.client.render.celestial;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.MainClass;

@Mod.EventBusSubscriber(modid = MainClass.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CelestialClientReloadListener {
    private CelestialClientReloadListener() {
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<CelestialJsonLoader.LoadedData>() {
            @Override
            protected CelestialJsonLoader.LoadedData prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return CelestialJsonLoader.loadFromDatapacks(resourceManager);
            }

            @Override
            protected void apply(CelestialJsonLoader.LoadedData loadedData, ResourceManager resourceManager, ProfilerFiller profiler) {
                CelestialJsonLoader.applyDatapackData(loadedData);
            }
        });
    }
}
