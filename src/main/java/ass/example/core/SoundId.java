package ass.example.core;

/**
 * 音效列表和聲音大小
 */
public enum SoundId {

    BUTTON_HOVER("useraction/button_hover.wav", 1),
    BUTTON_PRESSED("useraction/button_pressed.wav", 0.4),

    DEATH("characters/player/death.wav", 1),
    FOOTSTEP("characters/player/footstep.wav", 1),

    DOOR_OPEN("props/scene1/door_open.wav", 1),
    DOOR_CLOSE("props/scene1/door_close.wav", 1),

    EATING("props/scene1/eating.wav", 0.6),
    BRUSHING_TEETH("props/scene1/brushing_teeth.wav", 1),
    FOLDING_QUILT("props/scene1/folding_quilt.wav", 1),
    EQUIP("props/scene1/equip.wav", 1);

    private final String fileName;
    private final double builtInVolume;

    SoundId(String fileName, double builtInVolume) {
        this.fileName = fileName;
        this.builtInVolume = builtInVolume;
    }

    public String getFileName() {
        return fileName;
    }

    public double getBuiltInVolume() {
        return builtInVolume;
    }
}