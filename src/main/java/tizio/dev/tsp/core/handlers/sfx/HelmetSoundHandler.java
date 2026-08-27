package tizio.dev.tsp.core.handlers.sfx;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.handlers.oxygen.OxygenManager;
import tizio.dev.tsp.registry.RegisterSounds;
import tizio.dev.tsp.resources.armor.CustomArmorItem;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MainClass.MODID)
public final class HelmetSoundHandler {

    private static boolean wasCustomHelmet = false;
    private static String lastDimensionId = "";
    private static int dimensionCooldown = 0;

    private HelmetSoundHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            wasCustomHelmet = false;
            lastDimensionId = "";
            dimensionCooldown = 0;
            return;
        }

        ItemStack headItem = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        boolean isCustomHelmet = headItem.getItem() instanceof CustomArmorItem;

        String currentDimensionId = mc.level.dimension().location().toString();

        if (!currentDimensionId.equals(lastDimensionId)) {
            lastDimensionId = currentDimensionId;
            dimensionCooldown = 20;
            wasCustomHelmet = isCustomHelmet;
            SoundMuffleState.isMuffled = isCustomHelmet;
            SoundMuffleState.currentMuffleFactor = isCustomHelmet ? 1.0f : 0.0f;
            OpenALMuffleFilter.updateFilterValues(SoundMuffleState.currentMuffleFactor);
            return;
        }

        if (dimensionCooldown > 0) {
            dimensionCooldown--;
            wasCustomHelmet = isCustomHelmet;
            SoundMuffleState.isMuffled = isCustomHelmet;
            SoundMuffleState.currentMuffleFactor = isCustomHelmet ? 1.0f : 0.0f;
            OpenALMuffleFilter.updateFilterValues(SoundMuffleState.currentMuffleFactor);
            return;
        }

        if (isCustomHelmet != wasCustomHelmet) {
            wasCustomHelmet = isCustomHelmet;

            boolean hasOxygen = OxygenManager.hasOxygenInEnvironment(mc.level);

            SoundMuffleState.isMuffled = isCustomHelmet;

            SoundEvent soundToPlay;
            if (isCustomHelmet) {
                soundToPlay = hasOxygen ? RegisterSounds.HELMET_EQUIP_NO_OXYGEN.get() : RegisterSounds.HELMET_EQUIP_OXYGEN.get();
            } else {
                soundToPlay = hasOxygen ? RegisterSounds.HELMET_REMOVE_NO_OXYGEN.get() : RegisterSounds.HELMET_REMOVE_OXYGEN.get();
            }

            SoundMuffleState.suppressNextVanillaEquipSound = true;

            mc.level.playLocalSound(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    soundToPlay,
                    SoundSource.PLAYERS,
                    0.4F,
                    1.0F,
                    false
            );
        }
    }
}