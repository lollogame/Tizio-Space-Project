package tizio.dev.tsp.core.handlers.sfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class SoundLoopManager {

    private static class SoundData {
        boolean started = false;
        SoundLoopInstance loopInstance = null;
    }

    private static final Map<BlockPos, SoundData> ACTIVE = new HashMap<>();

    public static void play(Level level, BlockPos pos, SoundEvent startSound, SoundEvent loopSound, float volume, float pitch) {
        if (!level.isClientSide) return;
        SoundData data = ACTIVE.computeIfAbsent(pos, p -> new SoundData());

        if (!data.started && startSound != null) {
            playSingleSound(level, pos, startSound, volume, pitch);
            data.started = true;
        }

        if (data.loopInstance != null) return;
        Block expectedBlock = level.getBlockState(pos).getBlock();
        SoundLoopInstance loop = new SoundLoopInstance(loopSound, SoundSource.BLOCKS, Vec3.atCenterOf(pos), volume, pitch, pos.immutable(), expectedBlock);
        data.loopInstance = loop;
        Minecraft.getInstance().getSoundManager().play(loop);
    }

    public static void playSingleSound(Level level, BlockPos pos, SoundEvent start, float volume, float pitch) {
        if (!level.isClientSide) return;
        Vec3 c = Vec3.atCenterOf(pos);
        SimpleSoundInstance instance = new SimpleSoundInstance(start.getLocation(), SoundSource.BLOCKS, volume, pitch, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.LINEAR, c.x, c.y, c.z, false);
        Minecraft.getInstance().getSoundManager().play(instance);
    }

    public static void stopLoop(BlockPos pos) {
        SoundData data = ACTIVE.get(pos);
        if (data == null) return;

        if (data.loopInstance != null) {
            data.loopInstance.requestStop();
            Minecraft.getInstance().getSoundManager().stop(data.loopInstance);
            data.loopInstance = null;
        }
        data.started = false;
    }

    public static void stopAll(BlockPos pos) {
        SoundData data = ACTIVE.remove(pos);
        if (data == null) return;

        if (data.loopInstance != null) {
            data.loopInstance.requestStop();
            Minecraft.getInstance().getSoundManager().stop(data.loopInstance);
        }
    }

    public static void updatePos(BlockPos pos) {
        SoundData data = ACTIVE.get(pos);
        if (data != null && data.loopInstance != null) {
            data.loopInstance.updatePosition(Vec3.atCenterOf(pos));
        }
    }
}
