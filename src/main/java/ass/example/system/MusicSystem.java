package ass.example.system;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;

public class MusicSystem {

    private static final MusicSystem INSTANCE = new MusicSystem();

    public static MusicSystem getInstance() {
        return INSTANCE;
    }

    private final AudioSystem audioSystem = AudioSystem.getInstance();

    private MediaPlayer currentMusic;

    private String currentPath;

    private MusicSystem() {
    }

    public void playBGM(String path, boolean loop) {
        if (currentMusic != null && path.equals(currentPath)) {
            if (currentMusic.getStatus() == MediaPlayer.Status.PLAYING) {
                applyVolume();
                return;
            }

            if (currentMusic.getStatus() == MediaPlayer.Status.PAUSED) {
                applyVolume();
                currentMusic.play();
                return;
            }
        }

        stopBGM();

        try {
            URL url = getClass().getResource(path);

            if (url == null) {
                System.out.println("Music not found: " + path);
                return;
            }

            Media media = new Media(url.toExternalForm());
            currentMusic = new MediaPlayer(media);
            currentPath = path;

            if (loop) {
                currentMusic.setCycleCount(MediaPlayer.INDEFINITE);
            } else {
                currentMusic.setCycleCount(1);
            }

            currentMusic.setOnReady(() -> {
                applyVolume();
                currentMusic.play();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playBGMFrom(String path, double seconds, boolean loop) {

        stopBGM();

        try {

            URL url = getClass().getResource(path);

            if (url == null) {
                return;
            }

            Media media = new Media(url.toExternalForm());

            currentMusic = new MediaPlayer(media);

            currentPath = path;

            if (loop) {
                currentMusic.setCycleCount(MediaPlayer.INDEFINITE);
            }

            currentMusic.setOnReady(() -> {

                currentMusic.seek(Duration.seconds(seconds));

                applyVolume();

                currentMusic.play();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playBGMIntroThenLoop(String path, double firstStartSeconds, double loopStartSeconds) {
        stopBGM();

        try {
            URL url = getClass().getResource(path);

            if (url == null) {
                System.out.println("Music not found: " + path);
                return;
            }

            Media media = new Media(url.toExternalForm());
            currentMusic = new MediaPlayer(media);
            currentPath = path;

            currentMusic.setCycleCount(1);

            currentMusic.setOnReady(() -> {
                applyVolume();
                currentMusic.seek(Duration.seconds(firstStartSeconds));
                currentMusic.play();
            });

            currentMusic.setOnEndOfMedia(() -> {
                /*
                 * 第一次播完後，改成從 loopStartSeconds 開始無限循環。
                 */
                currentMusic.stop();
                currentMusic.setStartTime(Duration.seconds(loopStartSeconds));
                currentMusic.setCycleCount(MediaPlayer.INDEFINITE);
                currentMusic.seek(Duration.seconds(loopStartSeconds));
                currentMusic.play();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopBGM() {

        if (currentMusic != null) {

            currentMusic.stop();

            currentMusic.dispose();

            currentMusic = null;

            currentPath = null;
        }
    }

    public void pauseBGM() {

        if (currentMusic != null) {
            currentMusic.pause();
        }
    }

    public void resumeBGM() {

        if (currentMusic != null) {
            currentMusic.play();
        }
    }

    public void applyVolume() {

        if (currentMusic != null) {

            currentMusic.setVolume(
                    audioSystem.getEffectiveMusicVolume()
            );
        }
    }
}