package tizio.dev.tsp.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SoundDefinitionsProvider;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.registry.RegisterSounds;

public class SFXDataGenerator extends SoundDefinitionsProvider {

    public SFXDataGenerator(PackOutput output, ExistingFileHelper helper) {
        super(output, MainClass.MODID, helper);
    }

    @Override
    public void registerSounds() {
        for (RegistryObject<SoundEvent> soundEvent : RegisterSounds.SFX_EVENTS.getEntries()) {
            addSound(soundEvent);
        }
    }

    private void addSound(RegistryObject<SoundEvent> soundRegistryObject) {
        String soundName = soundRegistryObject.getId().getPath();
        System.out.println("Generating sound definition for: " + soundName);
        add(soundRegistryObject, definition().with(sound(new ResourceLocation(MainClass.MODID, soundName))));
    }
}
