package ass.example.system;

import ass.example.core.WindowMode;
import javafx.animation.PauseTransition;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.prefs.Preferences;

import static com.almasb.fxgl.dsl.FXGL.*;

public class WindowSystem {

    private static final WindowSystem INSTANCE = new WindowSystem();

    public static WindowSystem getInstance() {
        return INSTANCE;
    }

    private final Preferences prefs =
            Preferences.userNodeForPackage(WindowSystem.class);

    private final int defaultWidth = 1280;
    private final int defaultHeight = 720;

    private WindowMode mode = WindowMode.DEFAULT;

    private int customWidth = 1600;
    private int customHeight = 900;

    /*
     * 用來避免程式自己 setWidth / setHeight 時，
     * 被 listener 誤判成玩家手動調整。
     */
    private boolean applyingProgrammaticResize = false;

    private boolean resizeListenerInstalled = false;

    private PauseTransition resizeSaveDelay;

    private WindowSystem() {
        loadSettings();
    }

    public void installResizeListener() {
        if (resizeListenerInstalled) {
            return;
        }

        resizeListenerInstalled = true;

        Stage stage = getPrimaryStage();

        resizeSaveDelay = new PauseTransition(Duration.seconds(0.25));
        resizeSaveDelay.setOnFinished(e -> saveManualCustomSize());

        stage.widthProperty().addListener((obs, oldValue, newValue) -> {
            onStageManuallyResized();
        });

        stage.heightProperty().addListener((obs, oldValue, newValue) -> {
            onStageManuallyResized();
        });
    }

    private void onStageManuallyResized() {
        Stage stage = getPrimaryStage();

        if (applyingProgrammaticResize) {
            return;
        }

        if (stage.isFullScreen()) {
            return;
        }

        /*
         * 拖曳時不要每一幀都存，等 0.25 秒後再記錄。
         */
        resizeSaveDelay.playFromStart();
    }

    private void saveManualCustomSize() {
        Stage stage = getPrimaryStage();

        if (applyingProgrammaticResize) {
            return;
        }

        if (stage.isFullScreen()) {
            return;
        }

        int width = (int) Math.round(stage.getWidth());
        int height = (int) Math.round(stage.getHeight());

        if (width < 640 || height < 360) {
            return;
        }

        customWidth = width;
        customHeight = height;
        mode = WindowMode.CUSTOM;

        saveSettings();

        System.out.println("Custom window size saved: " + customWidth + " x " + customHeight);
    }

    public void applySavedSettings() {
        installResizeListener();
        applyMode(mode);
    }

    public void applyMode(WindowMode selectedMode) {
        if (selectedMode == null) {
            return;
        }

        mode = selectedMode;

        switch (selectedMode) {
            case DEFAULT -> applyWindowed(defaultWidth, defaultHeight);
            case CUSTOM -> applyWindowed(customWidth, customHeight);
            case FULLSCREEN -> applyFullscreen();
        }

        saveSettings();
    }

    private void applyWindowed(int width, int height) {
        Stage stage = getPrimaryStage();

        applyingProgrammaticResize = true;

        stage.setFullScreen(false);

        /*
         * 注意：
         * 這裡是套用視窗大小，不是記錄 custom。
         * 所以不會覆蓋 customWidth / customHeight。
         */
        stage.setWidth(width);
        stage.setHeight(height);
        stage.centerOnScreen();

        releaseProgrammaticResizeFlag();
    }

    private void applyFullscreen() {
        Stage stage = getPrimaryStage();

        applyingProgrammaticResize = true;

        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(
                javafx.scene.input.KeyCombination.NO_MATCH
        );

        stage.setFullScreen(true);

        releaseProgrammaticResizeFlag();
    }

    private void releaseProgrammaticResizeFlag() {
        PauseTransition delay = new PauseTransition(Duration.seconds(0.35));
        delay.setOnFinished(e -> applyingProgrammaticResize = false);
        delay.play();
    }

    public WindowMode getMode() {
        return mode;
    }

    public int getCustomWidth() {
        return customWidth;
    }

    public int getCustomHeight() {
        return customHeight;
    }

    public String getCurrentLabel() {
        return switch (mode) {
            case DEFAULT -> defaultWidth + " × " + defaultHeight;
            case CUSTOM -> customWidth + " × " + customHeight;
            case FULLSCREEN -> "全螢幕";
        };
    }

    private void loadSettings() {
        String modeName = prefs.get("windowMode", WindowMode.DEFAULT.name());

        try {
            mode = WindowMode.valueOf(modeName);
        } catch (Exception e) {
            mode = WindowMode.DEFAULT;
        }

        customWidth = prefs.getInt("customWidth", 1280);
        customHeight = prefs.getInt("customHeight", 720);
    }

    private void saveSettings() {
        prefs.put("windowMode", mode.name());
        prefs.putInt("customWidth", customWidth);
        prefs.putInt("customHeight", customHeight);
    }

    public void resetSettings() {
        mode = WindowMode.DEFAULT;

        customWidth = 1280;
        customHeight = 720;

        saveSettings();
        applySavedSettings();
    }
}