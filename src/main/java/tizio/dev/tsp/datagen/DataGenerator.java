package tizio.dev.tsp.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.MainClass;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = MainClass.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerator {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        net.minecraft.data.DataGenerator generator = event.getGenerator();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new TSPBlockStateProvider(generator.getPackOutput(), existingFileHelper));
        generator.addProvider(event.includeClient(), new ItemsDataGenerator(generator.getPackOutput(), existingFileHelper));
        generator.addProvider(event.includeClient(), new SFXDataGenerator(generator.getPackOutput(), existingFileHelper));
        generator.addProvider(event.includeClient(), new TSPLanguageGenerator(generator.getPackOutput()));

        generator.addProvider(event.includeServer(), new LootTableProvider(generator.getPackOutput(), Set.of(), List.of(new LootTableProvider.SubProviderEntry(TSPBlockLootProvider::new, LootContextParamSets.BLOCK))));
        generator.addProvider(event.includeServer(), new TSPRecipeProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new OreWorldGenProvider(generator.getPackOutput(), lookupProvider));
        generator.addProvider(event.includeServer(), new TSPBlockTagsProvider(generator.getPackOutput(), lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), new TSPBiomeTagsProvider(generator.getPackOutput(), lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), new SurfaceStructureProvider(generator));
    }
}
