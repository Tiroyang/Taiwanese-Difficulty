package ass.example.system;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;

/**
 * MusicSystem
 *
 * 全遊戲背景音樂系統。
 *
 * 功能：
 * 1. 播放背景音樂。
 * 2. 從指定秒數開始播放背景音樂。
 * 3. 支援播放「第一次 intro，之後從 loopStart 無限循環」的背景音樂。
 * 4. 停止背景音樂。
 * 5. 暫停 / 恢復背景音樂。
 * 6. 根據 AudioSystem 的音樂音量套用目前音量。
 */
public final class MusicSystem {

    // =========================================================
    // Singleton
    // =========================================================

    /**
     * MusicSystem 單例。
     */
    private static final MusicSystem INSTANCE = new MusicSystem();

    /**
     * 取得 MusicSystem 單例。
     *
     * @return MusicSystem
     */
    public static MusicSystem getInstance() {
        return INSTANCE;
    }


    // =========================================================
    // Dependencies
    // =========================================================

    /**
     * 音訊設定系統。
     *
     * 用於取得目前實際音樂音量。
     */
    private final AudioSystem audioSystem = AudioSystem.getInstance();


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * 目前播放中的 MediaPlayer。
     */
    private MediaPlayer currentMusic;

    /**
     * 目前播放中的音樂資源路徑。
     *
     * 用於判斷是否重複播放同一首音樂。
     */
    private String currentPath;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * private constructor。
     *
     * 外部只能透過 getInstance() 使用。
     */
    private MusicSystem() {
    }


    // =========================================================
    // Public API - Play BGM
    // =========================================================

    /**
     * 播放背景音樂。
     *
     * 若目前已經是同一首音樂：
     * - 正在播放：只重新套用音量，不重播。
     * - 暫停中：重新套用音量後繼續播放。
     *
     * @param path 音樂資源路徑
     * @param loop 是否循環播放
     */
    public void playBGM(
            String path,
            boolean loop
    ) {
        if (path == null || path.isBlank()) {
            return;
        }

        if (resumeIfSameMusic(path)) {
            return;
        }

        stopBGM();

        MediaPlayer player = createMediaPlayer(path);

        if (player == null) {
            return;
        }

        currentMusic = player;
        currentPath = path;

        configureLoop(currentMusic, loop);

        currentMusic.setOnReady(() -> {
            applyVolume();
            currentMusic.play();
        });
    }

    /**
     * 從指定秒數開始播放背景音樂。
     *
     * @param path 音樂資源路徑
     * @param startSeconds 開始播放秒數
     * @param loop 是否循環播放
     */
    public void playBGMFrom(
            String path,
            double startSeconds,
            boolean loop
    ) {
        if (path == null || path.isBlank()) {
            return;
        }

        stopBGM();

        MediaPlayer player = createMediaPlayer(path);

        if (player == null) {
            return;
        }

        currentMusic = player;
        currentPath = path;

        configureLoop(currentMusic, loop);

        currentMusic.setOnReady(() -> {
            currentMusic.seek(Duration.seconds(Math.max(0, startSeconds)));
            applyVolume();
            currentMusic.play();
        });
    }

    /**
     * 播放有 intro 的背景音樂。
     *
     * 流程：
     * 1. 第一次從 firstStartSeconds 開始播放。
     * 2. 播放結束後，改成從 loopStartSeconds 開始無限循環。
     *
     * @param path 音樂資源路徑
     * @param firstStartSeconds 第一次播放起點
     * @param loopStartSeconds 循環播放起點
     */
    public void playBGMIntroThenLoop(
            String path,
            double firstStartSeconds,
            double loopStartSeconds
    ) {
        if (path == null || path.isBlank()) {
            return;
        }

        stopBGM();

        MediaPlayer player = createMediaPlayer(path);

        if (player == null) {
            return;
        }

        currentMusic = player;
        currentPath = path;

        currentMusic.setCycleCount(1);

        currentMusic.setOnReady(() -> {
            applyVolume();
            currentMusic.seek(Duration.seconds(Math.max(0, firstStartSeconds)));
            currentMusic.play();
        });

        currentMusic.setOnEndOfMedia(() -> restartAsLoop(loopStartSeconds));
    }


    // =========================================================
    // Public API - Control BGM
    // =========================================================

    /**
     * 停止目前背景音樂。
     *
     * 1. stop()
     * 2. dispose()
     * 3. 清除 currentMusic
     * 4. 清除 currentPath
     */
    public void stopBGM() {
        if (currentMusic == null) {
            return;
        }

        currentMusic.stop();
        currentMusic.dispose();

        currentMusic = null;
        currentPath = null;
    }

    /**
     * 暫停目前背景音樂。
     */
    public void pauseBGM() {
        if (currentMusic == null) {
            return;
        }

        currentMusic.pause();
    }

    /**
     * 恢復目前背景音樂。
     */
    public void resumeBGM() {
        if (currentMusic == null) {
            return;
        }

        applyVolume();
        currentMusic.play();
    }

    /**
     * 重新套用目前音樂音量。
     *
     * 當設定選單調整 musicVolume / masterVolume 時，
     * 可以呼叫此方法即時更新 BGM 音量。
     */
    public void applyVolume() {
        if (currentMusic == null) {
            return;
        }

        currentMusic.setVolume(audioSystem.getEffectiveMusicVolume());
    }


    // =========================================================
    // Internal Playback Helpers
    // =========================================================

    /**
     * 如果目前已經是同一首音樂，嘗試直接恢復或維持播放。
     *
     * @param path 目標音樂路徑
     * @return true 表示已處理，不需要重新建立 MediaPlayer
     */
    private boolean resumeIfSameMusic(String path) {
        if (!isSameMusic(path)) {
            return false;
        }

        MediaPlayer.Status status = currentMusic.getStatus();

        if (status == MediaPlayer.Status.PLAYING) {
            applyVolume();
            return true;
        }

        if (status == MediaPlayer.Status.PAUSED) {
            applyVolume();
            currentMusic.play();
            return true;
        }

        return false;
    }

    /**
     * 判斷目前音樂是否和指定路徑相同。
     *
     * @param path 音樂路徑
     * @return true 表示目前已經是同一首
     */
    private boolean isSameMusic(String path) {
        return currentMusic != null &&
                currentPath != null &&
                currentPath.equals(path);
    }

    /**
     * 建立 MediaPlayer。
     *
     * @param path 音樂資源路徑
     * @return MediaPlayer；若找不到資源則回傳 null
     */
    private MediaPlayer createMediaPlayer(String path) {
        try {
            URL url = getClass().getResource(path);

            if (url == null) {
                System.out.println("Music not found: " + path);
                return null;
            }

            Media media = new Media(url.toExternalForm());

            return new MediaPlayer(media);

        } catch (Exception exception) {
            System.out.println("Music load failed: " + path);
            exception.printStackTrace();
            return null;
        }
    }

    /**
     * 設定是否循環播放。
     *
     * @param player MediaPlayer
     * @param loop 是否循環
     */
    private void configureLoop(
            MediaPlayer player,
            boolean loop
    ) {
        if (player == null) {
            return;
        }

        player.setCycleCount(loop ? MediaPlayer.INDEFINITE : 1);
    }

    /**
     * intro 播放完畢後，重新設定成從指定秒數開始循環播放。
     *
     * @param loopStartSeconds 循環起點秒數
     */
    private void restartAsLoop(double loopStartSeconds) {
        if (currentMusic == null) {
            return;
        }

        double safeLoopStartSeconds = Math.max(0, loopStartSeconds);

        currentMusic.stop();
        currentMusic.setStartTime(Duration.seconds(safeLoopStartSeconds));
        currentMusic.setCycleCount(MediaPlayer.INDEFINITE);
        currentMusic.seek(Duration.seconds(safeLoopStartSeconds));

        applyVolume();
        currentMusic.play();
    }


    // =========================================================
    // Getters
    // =========================================================

    /**
     * 取得目前音樂路徑。
     *
     * @return 目前音樂路徑；若沒有播放則回傳 null
     */
    public String getCurrentPath() {
        return currentPath;
    }

    /**
     * 判斷目前是否正在播放音樂。
     *
     * @return true 表示正在播放
     */
    public boolean isPlaying() {
        return currentMusic != null &&
                currentMusic.getStatus() == MediaPlayer.Status.PLAYING;
    }

    /**
     * 判斷目前是否有音樂被建立。
     *
     * 暫停狀態也算有音樂。
     *
     * @return true 表示 currentMusic 不為 null
     */
    public boolean hasMusic() {
        return currentMusic != null;
    }
}