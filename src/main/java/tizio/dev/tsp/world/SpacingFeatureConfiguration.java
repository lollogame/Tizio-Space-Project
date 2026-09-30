package tizio.dev.tsp.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record SpacingFeatureConfiguration(int spacing) implements FeatureConfiguration {
    public static final Codec<SpacingFeatureConfiguration> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("spacing", 4).forGetter(config -> config.spacing)
            ).apply(instance, SpacingFeatureConfiguration::new)
    );

}
