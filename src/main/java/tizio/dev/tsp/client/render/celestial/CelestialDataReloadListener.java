package tizio.dev.tsp.client.render.celestial;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.MainClass;

@Mod.EventBusSubscriber(modid = MainClass.MODID)
public final class CelestialDataReloadListener {
    private CelestialDataReloadListener() {
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SimplePreparableReloadListener<CelestialJsonLoader.LoadedData>() {
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

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        CelestialJsonLoader.rebuildForCurrentTime();
    }
}
