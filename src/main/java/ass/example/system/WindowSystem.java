package ass.example.system;

import ass.example.core.WindowMode;
import javafx.animation.PauseTransition;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.prefs.Preferences;

import static com.almasb.fxgl.dsl.FXGL.getPrimaryStage;

/**
 * WindowSystem
 *
 * 遊戲視窗設定系統。
 *
 * 功能：
 * 1. 管理目前視窗模式。
 * 2. 支援預設視窗大小。
 * 3. 支援自訂視窗大小。
 * 4. 支援全螢幕。
 * 5. 偵測玩家手動拖曳視窗大小，並自動保存成 CUSTOM。
 * 6. 使用 Preferences 保存視窗設定。
 */
public final class WindowSystem {
 
    // Singleton 

    /**
     * WindowSystem 單例。
     */
    private static final WindowSystem INSTANCE = new WindowSystem();

    /**
     * 取得 WindowSystem 單例。
     *
     * @return WindowSystem
     */
    public static WindowSystem getInstance() {
        return INSTANCE;
    }

 
    // Preferences Keys 

    private static final String KEY_WINDOW_MODE = "windowMode";
    private static final String KEY_CUSTOM_WIDTH = "customWidth";
    private static final String KEY_CUSTOM_HEIGHT = "customHeight";

 
    // Default Settings 

    /**
     * 預設視窗寬度。
     */
    private static final int DEFAULT_WIDTH = 1280;

    /**
     * 預設視窗高度。
     */
    private static final int DEFAULT_HEIGHT = 720;

    /**
     * 最小允許保存的自訂寬度。
     */
    private static final int MIN_CUSTOM_WIDTH = 640;

    /**
     * 最小允許保存的自訂高度。
     */
    private static final int MIN_CUSTOM_HEIGHT = 360;

    /**
     * 使用者停止拖曳視窗後，延遲多久才保存尺寸。
     *
     * 可以避免每一幀都寫入 Preferences。
     */
    private static final double RESIZE_SAVE_DELAY_SECONDS = 0.25;

    /**
     * 程式主動改變視窗大小後，延遲多久才解除 programmatic resize flag。
     *
     * 避免 stage.setWidth / setHeight 觸發 listener，被誤判成玩家手動拖曳視窗。
     */
    private static final double PROGRAMMATIC_RESIZE_RELEASE_SECONDS = 0.35;

 
    // Runtime State 

    /**
     * Java Preferences。
     *
     * 用於保存視窗模式與自訂尺寸。
     */
    private final Preferences prefs =
            Preferences.userNodeForPackage(WindowSystem.class);

    /**
     * 目前視窗模式。
     */
    private WindowMode mode = WindowMode.DEFAULT;

    /**
     * 自訂視窗寬度。
     */
    private int customWidth = DEFAULT_WIDTH;

    /**
     * 自訂視窗高度。
     */
    private int customHeight = DEFAULT_HEIGHT;

    /**
     * 是否正在由程式主動調整視窗大小。
     *
     * true 時，resize listener 不會把尺寸保存成自訂尺寸。
     */
    private boolean applyingProgrammaticResize = false;

    /**
     * 是否已安裝 resize listener。
     *
     * 避免重複安裝 listener。
     */
    private boolean resizeListenerInstalled = false;

    /**
     * 拖曳視窗後延遲保存尺寸用的 timer。
     */
    private PauseTransition resizeSaveDelay;

 
    // Constructor 

    /**
     * 建立視窗設定系統。
     *
     * private：
     * - 避免外部 new WindowSystem()。
     * - 確保全遊戲共用同一份視窗設定。
     */
    private WindowSystem() {
        loadSettings();
    }

 
    // Initialization 

    /**
     * 套用已保存的視窗設定。
     *
     * 通常在 MainMenu 或遊戲啟動後呼叫一次。
     *
     * 會：
     * 1. 安裝視窗 resize listener。
     * 2. 套用目前保存的 WindowMode。
     */
    public void applySavedSettings() {
        installResizeListener();
        applyMode(mode);
    }

    /**
     * 安裝視窗大小變更監聽器。
     *
     * 用於偵測玩家手動拖曳視窗大小，並將新大小保存成 CUSTOM。
     */
    public void installResizeListener() {
        if (resizeListenerInstalled) {
            return;
        }

        resizeListenerInstalled = true;

        Stage stage = getPrimaryStage();

        resizeSaveDelay = new PauseTransition(
                Duration.seconds(RESIZE_SAVE_DELAY_SECONDS)
        );

        resizeSaveDelay.setOnFinished(event -> saveManualCustomSize());

        stage.widthProperty().addListener((observable, oldValue, newValue) ->
                onStageManuallyResized()
        );

        stage.heightProperty().addListener((observable, oldValue, newValue) ->
                onStageManuallyResized()
        );
    }

 
    // Apply Window Mode 

    /**
     * 套用指定視窗模式。
     *
     * @param selectedMode 目標視窗模式
     */
    public void applyMode(WindowMode selectedMode) {
        if (selectedMode == null) {
            return;
        }

        mode = selectedMode;

        switch (selectedMode) {
            case DEFAULT -> applyWindowed(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            case CUSTOM -> applyWindowed(customWidth, customHeight);
            case FULLSCREEN -> applyFullscreen();
        }

        saveSettings();
    }

    /**
     * 套用視窗模式。
     *
     * @param width 視窗寬度
     * @param height 視窗高度
     */
    private void applyWindowed(
            int width,
            int height
    ) {
        Stage stage = getPrimaryStage();

        beginProgrammaticResize();

        stage.setFullScreen(false);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.centerOnScreen();

        releaseProgrammaticResizeFlagLater();
    }

    /**
     * 套用全螢幕模式。
     */
    private void applyFullscreen() {
        Stage stage = getPrimaryStage();

        beginProgrammaticResize();

        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreen(true);

        releaseProgrammaticResizeFlagLater();
    }

    /**
     * 標記目前是程式主動調整視窗。
     */
    private void beginProgrammaticResize() {
        applyingProgrammaticResize = true;
    }

    /**
     * 延遲解除程式主動 resize 狀態。
     *
     * 原因：
     * stage.setWidth / setHeight 可能會連續觸發數次 listener。
     */
    private void releaseProgrammaticResizeFlagLater() {
        PauseTransition delay = new PauseTransition(
                Duration.seconds(PROGRAMMATIC_RESIZE_RELEASE_SECONDS)
        );

        delay.setOnFinished(event -> applyingProgrammaticResize = false);
        delay.play();
    }

 
    // Manual Resize Handling 

    /**
     * 當 Stage 寬高改變時呼叫。
     *
     * 若是玩家手動拖曳視窗，會延遲保存目前尺寸。
     */
    private void onStageManuallyResized() {
        if (!shouldSaveManualResize()) {
            return;
        }

        /*
         * 玩家拖曳視窗時，width / height 會快速連續改變。
         * 所以使用 playFromStart()，等停止拖曳一小段時間後才保存。
         */
        resizeSaveDelay.playFromStart();
    }

    /**
     * 判斷目前是否應保存手動調整後的視窗大小。
     *
     * @return true 表示可以保存
     */
    private boolean shouldSaveManualResize() {
        Stage stage = getPrimaryStage();

        if (applyingProgrammaticResize) {
            return false;
        }

        if (stage.isFullScreen()) {
            return false;
        }

        return resizeSaveDelay != null;
    }

    /**
     * 保存玩家手動拖曳後的自訂尺寸。
     *
     * 會：
     * 1. 讀取目前 Stage 寬高。
     * 2. 若尺寸合法，保存為 CUSTOM 模式。
     */
    private void saveManualCustomSize() {
        if (!shouldSaveManualResize()) {
            return;
        }

        Stage stage = getPrimaryStage();

        int width = (int) Math.round(stage.getWidth());
        int height = (int) Math.round(stage.getHeight());

        if (!isValidCustomSize(width, height)) {
            return;
        }

        customWidth = width;
        customHeight = height;
        mode = WindowMode.CUSTOM;

        saveSettings();

        System.out.println(
                "Custom window size saved: " +
                        customWidth +
                        " x " +
                        customHeight
        );
    }

    /**
     * 判斷自訂視窗尺寸是否合法。
     *
     * @param width 寬度
     * @param height 高度
     * @return true 表示尺寸可保存
     */
    private boolean isValidCustomSize(
            int width,
            int height
    ) {
        return width >= MIN_CUSTOM_WIDTH &&
                height >= MIN_CUSTOM_HEIGHT;
    }

 
    // Reset 

    /**
     * 重置視窗設定。
     *
     * 會：
     * 1. 設定為 DEFAULT 模式。
     * 2. 自訂尺寸回到預設值。
     * 3. 保存設定。
     * 4. 立即套用設定。
     */
    public void resetSettings() {
        mode = WindowMode.DEFAULT;
        customWidth = DEFAULT_WIDTH;
        customHeight = DEFAULT_HEIGHT;

        saveSettings();
        applySavedSettings();
    }

 
    // Load / Save Settings 

    /**
     * 從 Preferences 讀取視窗設定。
     */
    private void loadSettings() {
        loadWindowMode();
        loadCustomSize();
    }

    /**
     * 讀取視窗模式。
     */
    private void loadWindowMode() {
        String modeName = prefs.get(
                KEY_WINDOW_MODE,
                WindowMode.DEFAULT.name()
        );

        try {
            mode = WindowMode.valueOf(modeName);
        } catch (Exception exception) {
            mode = WindowMode.DEFAULT;
        }
    }

    /**
     * 讀取自訂視窗尺寸。
     */
    private void loadCustomSize() {
        customWidth = prefs.getInt(KEY_CUSTOM_WIDTH, DEFAULT_WIDTH);
        customHeight = prefs.getInt(KEY_CUSTOM_HEIGHT, DEFAULT_HEIGHT);

        if (!isValidCustomSize(customWidth, customHeight)) {
            customWidth = DEFAULT_WIDTH;
            customHeight = DEFAULT_HEIGHT;
        }
    }

    /**
     * 將目前視窗設定寫入 Preferences。
     */
    private void saveSettings() {
        prefs.put(KEY_WINDOW_MODE, mode.name());
        prefs.putInt(KEY_CUSTOM_WIDTH, customWidth);
        prefs.putInt(KEY_CUSTOM_HEIGHT, customHeight);
    }

 
    // Getters 

    /**
     * 取得目前視窗模式。
     *
     * @return WindowMode
     */
    public WindowMode getMode() {
        return mode;
    }

    /**
     * 取得自訂視窗寬度。
     *
     * @return 自訂寬度
     */
    public int getCustomWidth() {
        return customWidth;
    }

    /**
     * 取得自訂視窗高度。
     *
     * @return 自訂高度
     */
    public int getCustomHeight() {
        return customHeight;
    }

    /**
     * 取得目前視窗設定顯示文字。
     *
     * @return 顯示文字
     */
    public String getCurrentLabel() {
        return switch (mode) {
            case DEFAULT -> DEFAULT_WIDTH + " × " + DEFAULT_HEIGHT;
            case CUSTOM -> customWidth + " × " + customHeight;
            case FULLSCREEN -> "全螢幕";
        };
    }
}