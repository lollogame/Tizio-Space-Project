package tizio.dev.tsp.registry;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.world.CraterConfiguration;
import tizio.dev.tsp.world.SpacingFeatureConfiguration;
import tizio.dev.tsp.world.feature.CraterFeature;
import tizio.dev.tsp.world.feature.RockFeature;

public class RegisterFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MainClass.MODID);

    public static final RegistryObject<Feature<CraterConfiguration>> CRATER = FEATURES.register("crater", () -> new CraterFeature(CraterConfiguration.CODEC));
    public static final RegistryObject<Feature<SpacingFeatureConfiguration>> ROCK_FEATURE = FEATURES.register("rock_feature", () -> new RockFeature(SpacingFeatureConfiguration.CODEC));

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }

}
