package ass.example.system;

import ass.example.core.SoundId;
import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * AudioSystem
 *
 * 全遊戲音效與音量設定系統。
 *
 * 功能：
 * 1. 播放 SFX 音效。
 * 2. 快取 AudioClip，避免重複載入音效檔。
 * 3. 管理 master / music / sfx 音量。
 * 4. 管理 master / music / sfx 靜音狀態。
 * 5. 管理按鈕音效是否啟用。
 * 6. 使用 Preferences 保存設定。
 */
public final class AudioSystem {

    // =========================================================
    // Singleton
    // =========================================================

    /**
     * AudioSystem 單例。
     */
    private static final AudioSystem INSTANCE = new AudioSystem();

    /**
     * 取得 AudioSystem 單例。
     *
     * @return AudioSystem
     */
    public static AudioSystem getInstance() {
        return INSTANCE;
    }


    // =========================================================
    // Preferences Keys
    // =========================================================

    private static final String KEY_MASTER_VOLUME = "masterVolume";
    private static final String KEY_MUSIC_VOLUME = "musicVolume";
    private static final String KEY_SFX_VOLUME = "sfxVolume";

    private static final String KEY_MASTER_MUTED = "masterMuted";
    private static final String KEY_MUSIC_MUTED = "musicMuted";
    private static final String KEY_SFX_MUTED = "sfxMuted";

    private static final String KEY_BUTTON_SOUND_ENABLED = "buttonSoundEnabled";


    // =========================================================
    // Default Settings
    // =========================================================

    private static final double DEFAULT_MASTER_VOLUME = 1.0;
    private static final double DEFAULT_MUSIC_VOLUME = 0.45;
    private static final double DEFAULT_SFX_VOLUME = 1.0;

    private static final boolean DEFAULT_MASTER_MUTED = false;
    private static final boolean DEFAULT_MUSIC_MUTED = false;
    private static final boolean DEFAULT_SFX_MUTED = false;

    private static final boolean DEFAULT_BUTTON_SOUND_ENABLED = true;


    // =========================================================
    // Audio Path Settings
    // =========================================================

    /**
     * 音效素材根目錄。
     *
     * SoundId 只保存相對於 assets/sounds 的檔名。
     */
    private static final String SOUND_ROOT = "/assets/sounds/";


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * Java  。
     *
     * 用於保存音量與靜音設定。
     */
    private final Preferences prefs = Preferences.userNodeForPackage(AudioSystem.class);

    /**
     * 主音量。
     *
     * 範圍：0.0 ~ 1.0
     */
    private double masterVolume;

    /**
     * 音樂音量。
     *
     * 範圍：0.0 ~ 1.0
     *
     * 實際音樂音量 = masterVolume * musicVolume
     */
    private double musicVolume;

    /**
     * 音效音量。
     *
     * 範圍：0.0 ~ 1.0
     *
     * 實際音效音量 = masterVolume * sfxVolume * builtInVolume
     */
    private double sfxVolume;

    /**
     * 主音量是否靜音。
     */
    private boolean masterMuted;

    /**
     * 音樂是否靜音。
     */
    private boolean musicMuted;

    /**
     * 音效是否靜音。
     */
    private boolean sfxMuted;

    /**
     * UI 按鈕音效是否啟用。
     *
     * 這只控制 playButtonSFX(...)。
     */
    private boolean buttonSoundEnabled;

    /**
     * 音效快取。
     *
     * 第一次播放某個 SoundId 時才載入 AudioClip，之後直接從快取取出。
     */
    private final Map<SoundId, AudioClip> soundCache = new HashMap<>();


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立 AudioSystem。
     *
     * private：
     * - 確保外部只能透過 getInstance() 使用。
     */
    private AudioSystem() {
        loadSettings();
    }


    // =========================================================
    // SFX Playback
    // =========================================================

    /**
     * 播放一般音效。
     *
     * 音量計算：
     * finalVolume = masterVolume * sfxVolume * soundId.builtInVolume
     *
     * 若 masterMuted 或 sfxMuted 為 true，音量為 0，不播放。
     *
     * @param soundId 音效 ID
     */
    public void playSFX(SoundId soundId) {
        if (soundId == null) {
            return;
        }

        double finalVolume =
                getEffectiveSfxVolume(soundId.getBuiltInVolume());

        if (finalVolume <= 0) {
            return;
        }

        AudioClip clip = getClip(soundId);

        if (clip != null) {
            clip.play(finalVolume);
        }
    }

    /**
     * 播放按鈕音效。
     *
     * - 會先檢查 buttonSoundEnabled。
     *
     * @param soundId 音效 ID
     */
    public void playButtonSFX(SoundId soundId) {
        if (!buttonSoundEnabled) {
            return;
        }

        playSFX(soundId);
    }

    /**
     * 取得指定 SoundId 對應的 AudioClip。
     *
     * 若尚未載入，會從資源路徑載入並放入快取。
     *
     * @param soundId 音效 ID
     * @return AudioClip；若找不到音效檔則回傳 null
     */
    private AudioClip getClip(SoundId soundId) {
        if (soundCache.containsKey(soundId)) {
            return soundCache.get(soundId);
        }

        String path = SOUND_ROOT + soundId.getFileName();
        URL url = getClass().getResource(path);

        if (url == null) {
            System.err.println("Sound not found: " + path);
            return null;
        }

        AudioClip clip = new AudioClip(url.toExternalForm());
        soundCache.put(soundId, clip);

        return clip;
    }


    // =========================================================
    // Effective Volume
    // =========================================================

    /**
     * 取得目前實際音樂音量。
     *
     * @return 0.0 ~ 1.0
     */
    public double getEffectiveMusicVolume() {
        if (masterMuted || musicMuted) {
            return 0;
        }

        return clamp01(masterVolume * musicVolume);
    }

    /**
     * 取得目前實際音效音量。
     *
     * 不包含 SoundId 自帶音量。
     *
     * @return 0.0 ~ 1.0
     */
    public double getEffectiveSfxVolume() {
        return getEffectiveSfxVolume(1.0);
    }

    /**
     * 取得目前實際音效音量。
     *
     * @param builtInVolume SoundId 自帶音量倍率
     * @return 0.0 ~ 1.0
     */
    public double getEffectiveSfxVolume(double builtInVolume) {
        if (masterMuted || sfxMuted) {
            return 0;
        }

        return clamp01(masterVolume * sfxVolume * builtInVolume);
    }


    // =========================================================
    // Volume Setters
    // =========================================================

    /**
     * 設定主音量。
     *
     * 若音量大於 0，會自動解除 masterMuted。
     *
     * @param masterVolume 主音量，範圍 0.0 ~ 1.0
     */
    public void setMasterVolume(double masterVolume) {
        this.masterVolume = clamp01(masterVolume);

        if (this.masterVolume > 0) {
            masterMuted = false;
        }

        saveSettings();
    }

    /**
     * 設定音樂音量。
     *
     * 若音量大於 0，會自動解除 musicMuted。
     *
     * @param musicVolume 音樂音量，範圍 0.0 ~ 1.0
     */
    public void setMusicVolume(double musicVolume) {
        this.musicVolume = clamp01(musicVolume);

        if (this.musicVolume > 0) {
            musicMuted = false;
        }

        saveSettings();
    }

    /**
     * 設定音效音量。
     *
     * 若音量大於 0，會自動解除 sfxMuted。
     *
     * @param sfxVolume 音效音量，範圍 0.0 ~ 1.0
     */
    public void setSfxVolume(double sfxVolume) {
        this.sfxVolume = clamp01(sfxVolume);

        if (this.sfxVolume > 0) {
            sfxMuted = false;
        }

        saveSettings();
    }


    // =========================================================
    // Mute Setters
    // =========================================================

    /**
     * 設定主靜音。
     *
     * @param masterMuted 是否主靜音
     */
    public void setMasterMuted(boolean masterMuted) {
        this.masterMuted = masterMuted;
        saveSettings();
    }

    /**
     * 設定音樂靜音。
     *
     * @param musicMuted 是否音樂靜音
     */
    public void setMusicMuted(boolean musicMuted) {
        this.musicMuted = musicMuted;
        saveSettings();
    }

    /**
     * 設定音效靜音。
     *
     * @param sfxMuted 是否音效靜音
     */
    public void setSfxMuted(boolean sfxMuted) {
        this.sfxMuted = sfxMuted;
        saveSettings();
    }

    /**
     * 設定按鈕音效是否啟用。
     *
     * @param buttonSoundEnabled true 表示啟用
     */
    public void setButtonSoundEnabled(boolean buttonSoundEnabled) {
        this.buttonSoundEnabled = buttonSoundEnabled;
        saveSettings();
    }


    // =========================================================
    // Toggle Mute
    // =========================================================

    /**
     * 切換主靜音。
     */
    public void toggleMasterMuted() {
        setMasterMuted(!masterMuted);
    }

    /**
     * 切換音樂靜音。
     */
    public void toggleMusicMuted() {
        setMusicMuted(!musicMuted);
    }

    /**
     * 切換音效靜音。
     */
    public void toggleSfxMuted() {
        setSfxMuted(!sfxMuted);
    }


    // =========================================================
    // Reset Settings
    // =========================================================

    /**
     * 重置所有音訊設定為預設值。
     */
    public void resetSettings() {
        masterVolume = DEFAULT_MASTER_VOLUME;
        musicVolume = DEFAULT_MUSIC_VOLUME;
        sfxVolume = DEFAULT_SFX_VOLUME;

        masterMuted = DEFAULT_MASTER_MUTED;
        musicMuted = DEFAULT_MUSIC_MUTED;
        sfxMuted = DEFAULT_SFX_MUTED;

        buttonSoundEnabled = DEFAULT_BUTTON_SOUND_ENABLED;

        saveSettings();
    }


    // =========================================================
    // Load / Save Settings
    // =========================================================

    /**
     * 從 Preferences 讀取音訊設定。
     */
    private void loadSettings() {
        masterVolume = prefs.getDouble(
                KEY_MASTER_VOLUME,
                DEFAULT_MASTER_VOLUME
        );

        musicVolume = prefs.getDouble(
                KEY_MUSIC_VOLUME,
                DEFAULT_MUSIC_VOLUME
        );

        sfxVolume = prefs.getDouble(
                KEY_SFX_VOLUME,
                DEFAULT_SFX_VOLUME
        );

        masterMuted = prefs.getBoolean(
                KEY_MASTER_MUTED,
                DEFAULT_MASTER_MUTED
        );

        musicMuted = prefs.getBoolean(
                KEY_MUSIC_MUTED,
                DEFAULT_MUSIC_MUTED
        );

        sfxMuted = prefs.getBoolean(
                KEY_SFX_MUTED,
                DEFAULT_SFX_MUTED
        );

        buttonSoundEnabled = prefs.getBoolean(
                KEY_BUTTON_SOUND_ENABLED,
                DEFAULT_BUTTON_SOUND_ENABLED
        );

        normalizeLoadedSettings();
    }

    /**
     * 將目前音訊設定寫入 Preferences。
     */
    private void saveSettings() {
        prefs.putDouble(KEY_MASTER_VOLUME, masterVolume);
        prefs.putDouble(KEY_MUSIC_VOLUME, musicVolume);
        prefs.putDouble(KEY_SFX_VOLUME, sfxVolume);

        prefs.putBoolean(KEY_MASTER_MUTED, masterMuted);
        prefs.putBoolean(KEY_MUSIC_MUTED, musicMuted);
        prefs.putBoolean(KEY_SFX_MUTED, sfxMuted);

        prefs.putBoolean(KEY_BUTTON_SOUND_ENABLED, buttonSoundEnabled);
    }

    /**
     * 修正讀取到的不合法音量。
     */
    private void normalizeLoadedSettings() {
        masterVolume = clamp01(masterVolume);
        musicVolume = clamp01(musicVolume);
        sfxVolume = clamp01(sfxVolume);
    }


    // =========================================================
    // Getters - Volume
    // =========================================================

    public double getMasterVolume() {
        return masterVolume;
    }

    public double getMusicVolume() {
        return musicVolume;
    }

    public double getSfxVolume() {
        return sfxVolume;
    }


    // =========================================================
    // Getters - Mute
    // =========================================================

    public boolean isMasterMuted() {
        return masterMuted;
    }

    public boolean isMusicMuted() {
        return musicMuted;
    }

    public boolean isSfxMuted() {
        return sfxMuted;
    }

    public boolean isButtonSoundEnabled() {
        return buttonSoundEnabled;
    }


    // =========================================================
    // Internal Helpers
    // =========================================================

    /**
     * 將數值限制在 0.0 ~ 1.0。
     *
     * @param value 原始值
     * @return 限制後的值
     */
    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}