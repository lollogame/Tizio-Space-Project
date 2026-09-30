package tizio.dev.tsp.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ConfigManager {

    public static final ForgeConfigSpec.BooleanValue SYSTEM_EDITOR;
    public static final ForgeConfigSpec.BooleanValue PLANET_CLOUDS;
    public static final ForgeConfigSpec.BooleanValue HAS_COLOR_CORRECTION;
    public static final ForgeConfigSpec.BooleanValue HAS_LENS_FLARE;
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    static {
        COMMON_BUILDER.push("----General Config----");

        SYSTEM_EDITOR = COMMON_BUILDER
                .comment("Development System Editor. NOTE: Intended for Single Player mode only, may not work or sync for others who wants to see whatever you modify.")
                .define("isDevelopmentMode", false);

        COMMON_BUILDER.pop();

        CLIENT_BUILDER.push("----Client Config----");

        PLANET_CLOUDS = CLIENT_BUILDER
                .comment("\n Here you can enable planet's surface clouds, if disabled, vanilla clouds will be rendered.")
                .define("enablePlanetClouds", true);

        HAS_COLOR_CORRECTION = CLIENT_BUILDER
                .comment("\n ------------------------------------------------------------------------------------------------------\n")
                .comment(" Here you can enable ACES - HABLE - UNREAL - AgX color correction, just because its cool, if not you can disable it here :]")
                .define("enableColorCorrection", true);

        HAS_LENS_FLARE = CLIENT_BUILDER
                .comment("\n ------------------------------------------------------------------------------------------------------\n")
                .comment(" Here you can enable Sun's lens flare post process effect")
                .define("enableLensFlare", true);

        CLIENT_BUILDER.pop();

        COMMON_SPEC = COMMON_BUILDER.build();
        CLIENT_SPEC = CLIENT_BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    public static ForgeConfigSpec getClientSpec() {
        return CLIENT_SPEC;
    }

    private static boolean isConfigLoaded() {
        return (COMMON_SPEC.isLoaded() && CLIENT_SPEC.isLoaded());
    }

    public static boolean isDevelopmentMode() {
        if (!isConfigLoaded()) return false;
        return SYSTEM_EDITOR.get();
    }

    public static boolean enablePlanetClouds() {
        if (!isConfigLoaded()) return true;
        return PLANET_CLOUDS.get();
    }

    public static boolean hasPostColorCorrection() {
        if (!isConfigLoaded()) return true;
        return HAS_COLOR_CORRECTION.get();
    }

    public static boolean hasPostLensFlare() {
        if (!isConfigLoaded()) return true;
        return HAS_LENS_FLARE.get();
    }
}