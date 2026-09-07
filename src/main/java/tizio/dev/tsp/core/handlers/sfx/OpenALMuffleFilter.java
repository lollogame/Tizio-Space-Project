package tizio.dev.tsp.core.handlers.sfx;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;

public final class OpenALMuffleFilter {

    private static int filterId = 0;
    private static boolean supported = false;

    public static void reset() {
        filterId = 0;
        supported = false;
    }

    public static int getFilterId() {
        try {
            if (filterId > 0 && EXTEfx.alIsFilter(filterId)) {
                return filterId;
            }

            filterId = EXTEfx.alGenFilters();
            if (filterId > 0 && AL10.alGetError() == AL10.AL_NO_ERROR) {
                EXTEfx.alFilteri(filterId, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
                supported = (AL10.alGetError() == AL10.AL_NO_ERROR);
                if (supported) {
                    updateFilterValues(SoundMuffleState.currentMuffleFactor);
                }
            } else {
                supported = false;
                filterId = 0;
            }
        } catch (Throwable t) {
            supported = false;
            filterId = 0;
        }
        return supported ? filterId : 0;
    }

    public static void updateFilterValues(float muffleFactor) {
        try {
            int fId = getFilterId();
            if (fId > 0 && EXTEfx.alIsFilter(fId)) {
                float gainHF = 1.0f - (muffleFactor * (1.0f - 0.03f));
                float gain = 1.0f - (muffleFactor * (1.0f - 0.7f));

                EXTEfx.alFilterf(fId, EXTEfx.AL_LOWPASS_GAIN, gain);
                EXTEfx.alFilterf(fId, EXTEfx.AL_LOWPASS_GAINHF, gainHF);
            }
        } catch (Throwable ignored) {}
    }

    public static void apply(int sourceId) {
        int fId = getFilterId();
        if (fId > 0) {
            try {
                AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, fId);
            } catch (Throwable ignored) {}
        }
    }

    public static void remove(int sourceId) {
        if (supported && filterId > 0) {
            try {
                AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
            } catch (Throwable ignored) {}
        }
    }
}
