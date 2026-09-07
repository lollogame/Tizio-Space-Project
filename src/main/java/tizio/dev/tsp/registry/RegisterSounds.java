package tizio.dev.tsp.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.MainClass;

public class RegisterSounds {

    public static final DeferredRegister<SoundEvent> SFX_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MainClass.MODID);

    public static void register(IEventBus event){
        SFX_EVENTS.register(event);
        MainClass.LOGGER.info("["+MainClass.MODID+"]: Loaded -> " + SFX_EVENTS.getEntries().size() + " SFX.");
    }

    private static RegistryObject<SoundEvent> registerSoundEvents(String name) {
        return SFX_EVENTS.register(name, ()-> SoundEvent.createVariableRangeEvent(new ResourceLocation(MainClass.MODID, name)));
    }

    public static final RegistryObject<SoundEvent> HELMET_EQUIP_OXYGEN     = registerSoundEvents("helmet.equip.oxygen");
    public static final RegistryObject<SoundEvent> HELMET_EQUIP_NO_OXYGEN  = registerSoundEvents("helmet.equip.no_oxygen");
    public static final RegistryObject<SoundEvent> HELMET_REMOVE_OXYGEN    = registerSoundEvents("helmet.remove.oxygen");
    public static final RegistryObject<SoundEvent> HELMET_REMOVE_NO_OXYGEN = registerSoundEvents("helmet.remove.no_oxygen");
}
