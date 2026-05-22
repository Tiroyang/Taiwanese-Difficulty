package ass.example.system;

import ass.example.core.SoundId;
import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public class AudioSystem {

    /*
     * Singleton：
     * 因為 MainMenu 通常在 initGame 前就被建立，
     * 用 Singleton 可以讓 MainMenu 和遊戲內共用同一份音量設定。
     */
    private static final AudioSystem INSTANCE = new AudioSystem();

    public static AudioSystem getInstance() {
        return INSTANCE;
    }

    private final Preferences prefs = Preferences.userNodeForPackage(AudioSystem.class);

    private double masterVolume;
    private double musicVolume;
    private double sfxVolume;

    private boolean masterMuted;
    private boolean musicMuted;
    private boolean sfxMuted;

    private boolean buttonSoundEnabled;

    private final Map<SoundId, AudioClip> soundCache = new HashMap<>();

    private AudioSystem() {
        loadSettings();
    }

    public void playSFX(SoundId soundId) {
        if (soundId == null) {
            return;
        }

        double finalVolume = getEffectiveSfxVolume(soundId.getBuiltInVolume());

        if (finalVolume <= 0) {
            return;
        }

        AudioClip clip = getClip(soundId);

        if (clip != null) {
            clip.play(finalVolume);
        }
    }

    public void playButtonSFX(SoundId soundId) {
        if (!buttonSoundEnabled) {
            return;
        }

        playSFX(soundId);
    }

    private AudioClip getClip(SoundId soundId) {
        if (soundCache.containsKey(soundId)) {
            return soundCache.get(soundId);
        }

        String path = "/assets/sounds/" + soundId.getFileName();
        URL url = getClass().getResource(path);

        if (url == null) {
            System.err.println("Sound not found: " + path);
            return null;
        }

        AudioClip clip = new AudioClip(url.toExternalForm());
        soundCache.put(soundId, clip);

        return clip;
    }

    public double getEffectiveMusicVolume() {
        if (masterMuted || musicMuted) {
            return 0;
        }

        return clamp(masterVolume * musicVolume, 0.0, 1.0);
    }

    public double getEffectiveSfxVolume() {
        return getEffectiveSfxVolume(1.0);
    }

    public double getEffectiveSfxVolume(double builtInVolume) {
        if (masterMuted || sfxMuted) {
            return 0;
        }

        return clamp(masterVolume * sfxVolume * builtInVolume, 0.0, 1.0);
    }

    public void setMasterVolume(double masterVolume) {
        this.masterVolume = clamp(masterVolume, 0.0, 1.0);

        if (this.masterVolume > 0) {
            masterMuted = false;
        }

        saveSettings();
    }

    public void setMusicVolume(double musicVolume) {
        this.musicVolume = clamp(musicVolume, 0.0, 1.0);

        if (this.musicVolume > 0) {
            musicMuted = false;
        }

        saveSettings();
    }

    public void setSfxVolume(double sfxVolume) {
        this.sfxVolume = clamp(sfxVolume, 0.0, 1.0);

        if (this.sfxVolume > 0) {
            sfxMuted = false;
        }

        saveSettings();
    }

    public void setMasterMuted(boolean masterMuted) {
        this.masterMuted = masterMuted;
        saveSettings();
    }

    public void setMusicMuted(boolean musicMuted) {
        this.musicMuted = musicMuted;
        saveSettings();
    }

    public void setSfxMuted(boolean sfxMuted) {
        this.sfxMuted = sfxMuted;
        saveSettings();
    }

    public void setButtonSoundEnabled(boolean buttonSoundEnabled) {
        this.buttonSoundEnabled = buttonSoundEnabled;
        saveSettings();
    }

    public double getMasterVolume() {
        return masterVolume;
    }

    public double getMusicVolume() {
        return musicVolume;
    }

    public double getSfxVolume() {
        return sfxVolume;
    }

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

    public void toggleMasterMuted() {
        masterMuted = !masterMuted;
        saveSettings();
    }

    public void toggleMusicMuted() {
        musicMuted = !musicMuted;
        saveSettings();
    }

    public void toggleSfxMuted() {
        sfxMuted = !sfxMuted;
        saveSettings();
    }

    public void resetSettings() {
        masterVolume = 1.0;
        musicVolume = 0.45;
        sfxVolume = 1.0;

        masterMuted = false;
        musicMuted = false;
        sfxMuted = false;

        buttonSoundEnabled = true;

        saveSettings();
    }

    private void loadSettings() {
        masterVolume = prefs.getDouble("masterVolume", 1.0);
        musicVolume = prefs.getDouble("musicVolume", 0.45);
        sfxVolume = prefs.getDouble("sfxVolume", 1.0);

        masterMuted = prefs.getBoolean("masterMuted", false);
        musicMuted = prefs.getBoolean("musicMuted", false);
        sfxMuted = prefs.getBoolean("sfxMuted", false);

        buttonSoundEnabled = prefs.getBoolean("buttonSoundEnabled", true);
    }

    private void saveSettings() {
        prefs.putDouble("masterVolume", masterVolume);
        prefs.putDouble("musicVolume", musicVolume);
        prefs.putDouble("sfxVolume", sfxVolume);

        prefs.putBoolean("masterMuted", masterMuted);
        prefs.putBoolean("musicMuted", musicMuted);
        prefs.putBoolean("sfxMuted", sfxMuted);

        prefs.putBoolean("buttonSoundEnabled", buttonSoundEnabled);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}