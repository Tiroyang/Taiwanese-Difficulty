package ass.example.core;

/**
 * SoundId
 *
 * 遊戲音效列表。
 *
 * 每個 SoundId 包含：
 * 1. fileName：
 *    - 音效檔案相對路徑。
 *
 * 2. builtInVolume：
 *    - 此音效自身的預設音量倍率。
 *
 * AudioSystem 可使用：
 *
 * soundId.getFileName()
 * soundId.getBuiltInVolume()
 *
 * 來播放正確音效。
 */
public enum SoundId {

    // =========================================================
    // UI
    // =========================================================

    /**
     * 滑鼠移到按鈕上的音效。
     */
    BUTTON_HOVER(
            "useraction/button_hover.wav",
            1.0
    ),

    /**
     * 按下按鈕的音效。
     */
    BUTTON_PRESSED(
            "useraction/button_pressed.wav",
            0.4
    ),


    // =========================================================
    // Player
    // =========================================================

    /**
     * 玩家死亡音效。
     */
    DEATH(
            "characters/player/death.wav",
            1.0
    ),

    /**
     * 玩家腳步聲。
     */
    FOOTSTEP(
            "characters/player/footstep.wav",
            0.05
    ),

    /**
     * 玩家跳躍音效。
     */
    JUMP(
            "characters/player/jump.wav",
            0.8
    ),


    // =========================================================
    // Dialogue
    // =========================================================

    /**
     * 對話文字嗶聲。
     */
    DIALOG_BLIP(
            "characters/dialog_blip.wav",
            0.05
    ),


    // =========================================================
    // HouseScene - Props
    // =========================================================

    /**
     * 開門音效。
     */
    DOOR_OPEN(
            "props/scene1/door_open.wav",
            0.8
    ),

    /**
     * 關門音效。
     */
    DOOR_CLOSE(
            "props/scene1/door_close.wav",
            0.7
    ),

    /**
     * 吃東西 / 喝水音效。
     */
    EATING(
            "props/scene1/eating.wav",
            0.6
    ),

    /**
     * 刷牙音效。
     */
    BRUSHING_TEETH(
            "props/scene1/brushing_teeth.wav",
            1.0
    ),

    /**
     * 折棉被音效。
     */
    FOLDING_QUILT(
            "props/scene1/folding_quilt.wav",
            1.0
    ),

    /**
     * 穿鞋 / 裝備音效。
     */
    EQUIP(
            "props/scene1/equip.wav",
            1.0
    ),


    // =========================================================
    // Mom
    // =========================================================

    /**
     * 媽媽 Boss 戰死亡音效。
     */
    MOM_BATTLE_DEATH(
            "characters/mom/mombattledeath.wav",
            0.3
    ),

    /**
     * 媽媽跳舞制裁音效。
     */
    MOM_DANCE_OFF(
            "characters/mom/momdanceoff.wav",
            0.5
    );


    // =========================================================
    // Audio Settings
    // =========================================================

    /**
     * 音效檔案相對路徑。
     */
    private final String fileName;

    /**
     * 音效內建音量倍率。
     *
     * AudioSystem 播放時可再乘上全域音效音量。
     */
    private final double builtInVolume;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立音效資料。
     *
     * @param fileName 音效檔案路徑
     * @param builtInVolume 內建音量倍率
     */
    SoundId(
            String fileName,
            double builtInVolume
    ) {
        this.fileName = fileName;
        this.builtInVolume = builtInVolume;
    }


    // =========================================================
    // Getters
    // =========================================================

    /**
     * 取得音效檔案路徑。
     *
     * @return 音效檔案路徑
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 取得音效內建音量倍率。
     *
     * @return 內建音量倍率
     */
    public double getBuiltInVolume() {
        return builtInVolume;
    }
}