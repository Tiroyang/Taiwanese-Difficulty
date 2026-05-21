package ass.example.core;

/**
 * 音效列表和聲音大小
 */
public enum SoundId {

    DEATH("characters/player/death.wav", 1),

    DOOR_OPEN("props/scene1/door_open.wav", 1),
    DOOR_CLOSE("props/scene1/door_close.wav", 1),

    EATING("props/scene1/eating.wav", 0.8);

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