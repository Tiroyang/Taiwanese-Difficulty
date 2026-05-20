package ass.example.system;

import ass.example.core.SoundId;
import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AudioSystem {

    private double masterVolume = 1.0;
    private double sfxVolume = 1.0;
    private boolean muted = false;

    private final Map<SoundId, AudioClip> soundCache = new HashMap<>();

    public void playSFX(SoundId soundId) {
        if (muted) {
            return;
        }

        double finalVolume = clamp(
                masterVolume * sfxVolume * soundId.getBuiltInVolume(),
                0.0,
                1.0
        );

        if (finalVolume <= 0) {
            return;
        }

        AudioClip clip = getClip(soundId);

        if (clip != null) {
            clip.play(finalVolume);
        }
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

    public void setMasterVolume(double masterVolume) {
        this.masterVolume = clamp(masterVolume, 0.0, 1.0);
    }

    public void setSfxVolume(double sfxVolume) {
        this.sfxVolume = clamp(sfxVolume, 0.0, 1.0);
    }

    public double getMasterVolume() {
        return masterVolume;
    }

    public double getSfxVolume() {
        return sfxVolume;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void toggleMuted() {
        muted = !muted;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}