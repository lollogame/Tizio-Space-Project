package tizio.dev.tsp.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ConfigManager {

    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
    //private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue SYSTEM_EDITOR;

    static {

        COMMON_BUILDER.push("----General Config----");

            SYSTEM_EDITOR = COMMON_BUILDER.comment("Development System Editor. NOTE: Intended for Single Player mode only, may not work or sync for others who wants to see whatever you modify.").define("isDevelopmentMode", false);

        COMMON_BUILDER.pop();

        //CLIENT_BUILDER.push("----Client Config----");
        //CLIENT_BUILDER.pop();
    }

    private static final ForgeConfigSpec COMMON_SPEC = COMMON_BUILDER.build();
    //private static final ForgeConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        //ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    public static boolean isDevelopmentMode(){
        return SYSTEM_EDITOR.get().booleanValue();
    }
}
