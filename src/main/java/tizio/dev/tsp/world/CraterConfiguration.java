package tizio.dev.tsp.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record CraterConfiguration(int minRadius, int maxRadius, int rimWidth, int maxDepth, int rimHeight, int maxSurfaceY) implements FeatureConfiguration {

    public static final Codec<CraterConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("min_radius").forGetter(CraterConfiguration::minRadius),
            Codec.INT.fieldOf("max_radius").forGetter(CraterConfiguration::maxRadius),
            Codec.INT.fieldOf("rim_width").forGetter(CraterConfiguration::rimWidth),
            Codec.INT.fieldOf("max_depth").forGetter(CraterConfiguration::maxDepth),
            Codec.INT.fieldOf("rim_height").forGetter(CraterConfiguration::rimHeight),
            Codec.INT.fieldOf("max_surface_y").forGetter(CraterConfiguration::maxSurfaceY)
    ).apply(instance, CraterConfiguration::new));
}
