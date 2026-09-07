package tizio.dev.tsp.core.handlers.sfx;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.registry.RegisterSounds;
import tizio.dev.tsp.resources.armor.CustomArmorItem;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MainClass.MODID)
public final class SoundMuffleHandler {

    private static Field channelSourceField;

    private static final Map<Channel, SoundInstance> ACTIVE_CHANNELS = Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean lastMuffledState = false;
    private static final float FADE_SPEED = 0.5f;

    @SubscribeEvent
    public static void onSoundEngineLoad(SoundEngineLoadEvent event) {
        OpenALMuffleFilter.reset();
        ACTIVE_CHANNELS.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            boolean helmetOn = mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof CustomArmorItem;
            SoundMuffleState.isMuffled = helmetOn;
            SoundMuffleState.currentMuffleFactor = helmetOn ? 1.0f : 0.0f;
            lastMuffledState = helmetOn;
            OpenALMuffleFilter.updateFilterValues(SoundMuffleState.currentMuffleFactor);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            SoundMuffleState.isMuffled = false;
            SoundMuffleState.currentMuffleFactor = 0.0f;
            lastMuffledState = false;
            OpenALMuffleFilter.updateFilterValues(0.0f);
            updateAllActiveChannels(false);
            return;
        }

        boolean helmetOn = mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof CustomArmorItem;

        SoundMuffleState.isMuffled = helmetOn;
        float targetFactor = helmetOn ? 1.0f : 0.0f;

        if (Math.abs(SoundMuffleState.currentMuffleFactor - targetFactor) > 0.001f) {
            if (SoundMuffleState.currentMuffleFactor < targetFactor) {
                SoundMuffleState.currentMuffleFactor = Math.min(targetFactor, SoundMuffleState.currentMuffleFactor + FADE_SPEED);
            } else {
                SoundMuffleState.currentMuffleFactor = Math.max(targetFactor, SoundMuffleState.currentMuffleFactor - FADE_SPEED);
            }

            OpenALMuffleFilter.updateFilterValues(SoundMuffleState.currentMuffleFactor);
        } else {
            OpenALMuffleFilter.updateFilterValues(SoundMuffleState.currentMuffleFactor);
        }

        boolean activeOrFading = helmetOn || SoundMuffleState.currentMuffleFactor > 0.0f;

        if (helmetOn != lastMuffledState) {
            lastMuffledState = helmetOn;
            updateAllActiveChannels(activeOrFading);
        } else if (!helmetOn && SoundMuffleState.currentMuffleFactor == 0.0f) {
            updateAllActiveChannels(false);
        } else if (helmetOn && SoundMuffleState.currentMuffleFactor > 0.0f) {
            updateAllActiveChannels(true);
        }
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;

        var loc = sound.getLocation();
        if (!"minecraft".equals(loc.getNamespace())) return;

        if (loc.getPath().startsWith("item.armor.equip_")) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                boolean customHelmet = mc.player.getItemBySlot(EquipmentSlot.HEAD)
                        .getItem() instanceof CustomArmorItem;

                if (customHelmet || lastMuffledState || SoundMuffleState.suppressNextVanillaEquipSound) {
                    event.setSound(null);
                    SoundMuffleState.suppressNextVanillaEquipSound = false;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlaySoundSource(PlaySoundSourceEvent event) {
        SoundInstance sound = event.getSound();
        Channel channel = event.getChannel();

        if (sound == null || channel == null) return;

        ACTIVE_CHANNELS.put(channel, sound);

        int sourceId = getSourceId(channel);
        if (sourceId <= 0) return;

        boolean shouldFilter = SoundMuffleState.isMuffled || SoundMuffleState.currentMuffleFactor > 0.0f;
        applyFilterToSource(sourceId, sound, shouldFilter);
    }

    private static void updateAllActiveChannels(boolean shouldFilter) {
        ACTIVE_CHANNELS.entrySet().removeIf(entry -> {
            Channel channel = entry.getKey();
            SoundInstance sound = entry.getValue();

            if (channel == null || sound == null || channel.stopped()) {
                return true;
            }

            int sourceId = getSourceId(channel);
            if (sourceId > 0) {
                applyFilterToSource(sourceId, sound, shouldFilter);
            }
            return false;
        });
    }

    private static void applyFilterToSource(int sourceId, SoundInstance sound, boolean shouldFilter) {
        var loc = sound.getLocation();
        var source = sound.getSource();

        boolean isHelmetSound = MainClass.MODID.equals(loc.getNamespace())
                && (
                loc.equals(RegisterSounds.HELMET_EQUIP_OXYGEN.get().getLocation())
                        || loc.equals(RegisterSounds.HELMET_EQUIP_NO_OXYGEN.get().getLocation())
                        || loc.equals(RegisterSounds.HELMET_REMOVE_OXYGEN.get().getLocation())
                        || loc.equals(RegisterSounds.HELMET_REMOVE_NO_OXYGEN.get().getLocation())
        );

        boolean isExempt = isHelmetSound
                || source == SoundSource.MUSIC
                || source == SoundSource.MASTER;

        if (shouldFilter && !isExempt) {
            OpenALMuffleFilter.apply(sourceId);
        } else {
            OpenALMuffleFilter.remove(sourceId);
        }
    }

    private static int getSourceId(Channel channel) {
        try {
            if (channelSourceField == null) {
                channelSourceField = Channel.class.getDeclaredField("source");
                channelSourceField.setAccessible(true);
            }
            return channelSourceField.getInt(channel);
        } catch (Exception e) {
            return 0;
        }
    }
}
