package tizio.dev.tsp.core.handlers.sfx;

public final class SoundMuffleState {

    public static volatile boolean isMuffled = false;
    public static volatile boolean suppressNextVanillaEquipSound = false;
    public static volatile float currentMuffleFactor = 0.0f;

}
