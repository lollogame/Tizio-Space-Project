package tizio.dev.tsp.core.handlers.sfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class SoundLoopInstance extends AbstractTickableSoundInstance {

    private boolean shouldStop = false;
    private final BlockPos blockPos;
    private final Block expectedBlock;

    public SoundLoopInstance(SoundEvent sound, SoundSource category, Vec3 pos, float volume, float pitch, BlockPos blockPos, Block expectedBlock) {
        super(sound, category, SoundInstance.createUnseededRandom());
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.volume = volume;
        this.pitch = pitch;
        this.looping = true;
        this.delay = 0;
        this.blockPos = blockPos;
        this.expectedBlock = expectedBlock;
    }

    @Override
    public void tick() {
        if (shouldStop || !isSourceStillValid()) {
            this.stop();
        }
    }

    private boolean isSourceStillValid() {
        if (blockPos == null || expectedBlock == null) return true;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return false;
        if (!minecraft.level.isLoaded(blockPos)) return false;
        return minecraft.level.getBlockState(blockPos).is(expectedBlock);
    }

    public void requestStop() {
        this.shouldStop = true;
        this.stop();
    }

    public void updatePosition(Vec3 pos) {
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }
}
