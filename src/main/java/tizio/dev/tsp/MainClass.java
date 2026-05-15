package tizio.dev.tsp;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MainClass.MODID)
public class MainClass {

    public static final String MODID = "tsp";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MainClass() {
        MinecraftForge.EVENT_BUS.register(this);
    }
}
