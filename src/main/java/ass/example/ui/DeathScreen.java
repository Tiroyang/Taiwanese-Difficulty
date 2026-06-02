package ass.example.ui;

import ass.example.core.DeathReason;
import ass.example.system.LanguageSystem;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGLForKtKt.getb;
import static com.almasb.fxgl.dsl.FXGLForKtKt.getd;

/**
 * DeathScreen
 *
 * 死亡畫面 UI。
 *
 * 功能：
 * 1. 顯示死亡主標題。
 * 2. 顯示死亡原因標題與副標題。
 * 3. 故事模式顯示死亡次數。
 * 4. Street Endless 模式顯示本局分數、最高分數與新紀錄提示。
 * 5. 提供重生按鈕。
 * 6. 支援死亡成就解鎖 Toast。
 * 7. 播放死亡畫面進場動畫。
 *
 * 單例判斷：
 * DeathScreen 不適合做成單例。
 *
 * 原因：
 * - 它是 JavaFX UI Node。
 * - 它會被 DeathSystem 建立並加入 UI layer。
 * - 它持有 AchievementToast、按鈕、動畫狀態。
 * - 若做成單例，切換場景或重新初始化 UI 時容易殘留舊狀態。
 */
public class DeathScreen extends StackPane {

    // =========================================================
    // Layout Constants
    // =========================================================

    private static final double SCREEN_WIDTH = 1280.0;
    private static final double SCREEN_HEIGHT = 720.0;

    private static final double DETAIL_BOX_TARGET_Y = 95.0;
    private static final double DETAIL_BOX_START_Y = 140.0;

    private static final double DEATH_TITLE_TARGET_Y = -95.0;

    private static final double BUTTON_WIDTH = 320.0;
    private static final double BUTTON_HEIGHT = 92.0;

    private static final double BUTTON_BOTTOM_MARGIN = 42.0;
    private static final double RESPAWN_BUTTON_RIGHT_MARGIN = 48.0;

    private static final double BUTTON_START_TRANSLATE_X = -220.0;


    // =========================================================
    // Animation Constants
    // =========================================================

    private static final double DEATH_TITLE_FADE_SECONDS = 1.0;
    private static final double DEATH_TITLE_SCALE_SECONDS = 0.55;

    private static final double DETAIL_ANIMATION_SECONDS = 0.45;
    private static final double EXTRA_INFO_ANIMATION_SECONDS = 0.35;

    private static final double BUTTON_ANIMATION_SECONDS = 0.36;

    private static final double BUTTON_HOVER_SECONDS = 0.08;
    private static final double BUTTON_HOVER_SCALE = 1.06;
    private static final double BUTTON_PRESSED_SCALE = 0.96;

    private static final double SEQUENCE_PAUSE_SECONDS = 0.18;


    // =========================================================
    // Asset Constants
    // =========================================================

    private static final String FONT_PATH =
            "/assets/fonts/OptimusPrinceps.ttf";

    private static final String RESPAWN_BUTTON_IMAGE_PATH =
            "/assets/textures/ui/respawn_button.png";


    // =========================================================
    // Dependencies
    // =========================================================

    private final LanguageSystem languageSystem =
            LanguageSystem.getInstance();

    private final Runnable onRespawn;


    // =========================================================
    // UI Nodes
    // =========================================================

    private final Rectangle background =
            new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);

    private final Text deathTitleText =
            new Text("YOU FAILED");

    private final Text titleText =
            new Text();

    private final Text subtitleText =
            new Text();

    private final Text extraInfoText =
            new Text();

    private final Text newRecordText =
            new Text();

    private final VBox detailTextBox =
            new VBox(12);

    private final StackPane respawnButton;

    private final AchievementToast achievementToast =
            new AchievementToast();


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * 目前播放中的進場動畫。
     *
     * 若死亡畫面連續 show，可以先停止舊動畫，
     * 避免動畫狀態互相干擾。
     */
    private SequentialTransition currentShowAnimation;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建議使用的新建構子。
     *
     * @param onRespawn 點擊重生按鈕時執行
     */
    public DeathScreen(Runnable onRespawn) {
        this.onRespawn = onRespawn;

        loadFonts();

        setupRoot();
        setupBackground();
        setupTexts();

        respawnButton = createImageTextButton(
                RESPAWN_BUTTON_IMAGE_PATH,
                text("menu.rebirth"),
                this::handleRespawnClicked
        );

        setupLayout();
        assembleNodes();

        CursorManager.install(this);
    }

    /**
     * 相容舊版 DeathSystem 用。
     *
     * 如果 DeathSystem 目前仍然是：
     *
     * new DeathScreen(this::respawn, this::goToMainMenu)
     *
     * 保留這個建構子可以避免編譯錯誤。
     * 第二個參數已不再使用。
     */
    public DeathScreen(
            Runnable onRespawn,
            Runnable ignoredOnMainMenu
    ) {
        this(onRespawn);
    }


    // =========================================================
    // Setup
    // =========================================================

    /**
     * 設定根節點。
     */
    private void setupRoot() {
        setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        setVisible(false);
        setPickOnBounds(true);
    }

    /**
     * 設定半透明黑色背景。
     */
    private void setupBackground() {
        background.setFill(Color.rgb(0, 0, 0, 0.78));
    }

    /**
     * 載入死亡畫面字型。
     */
    private void loadFonts() {
        try {
            Font.loadFont(
                    getClass().getResourceAsStream(FONT_PATH),
                    16
            );

        } catch (Exception exception) {
            System.out.println("DeathScreen font load failed.");
        }
    }

    /**
     * 設定所有文字樣式。
     */
    private void setupTexts() {
        setupDeathTitleText();
        setupDeathDetailTexts();
        setupExtraInfoTexts();

        detailTextBox.setAlignment(Pos.CENTER);
        detailTextBox.getChildren().addAll(
                titleText,
                subtitleText,
                extraInfoText,
                newRecordText
        );
    }

    /**
     * 設定死亡主標題。
     */
    private void setupDeathTitleText() {
        deathTitleText.setStyle("""
                -fx-font-size: 128px;
                -fx-fill: rgb(255, 0, 0, 0.65);
                -fx-font-family: "OptimusPrinceps";
                -fx-font-weight: bold;
                """);
    }

    /**
     * 設定死亡原因標題與副標題。
     */
    private void setupDeathDetailTexts() {
        titleText.setStyle("""
                -fx-font-size: 32px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        subtitleText.setStyle("""
                -fx-font-size: 24px;
                -fx-fill: white;
                """);
    }

    /**
     * 設定額外資訊文字。
     */
    private void setupExtraInfoTexts() {
        extraInfoText.setStyle("""
                -fx-font-size: 21px;
                -fx-fill: rgba(255,255,255,0.82);
                """);

        newRecordText.setStyle("""
                -fx-font-size: 26px;
                -fx-fill: #ffd36a;
                -fx-font-weight: bold;
                """);

        newRecordText.setEffect(
                new DropShadow(8, Color.BLACK)
        );
    }

    /**
     * 設定各 UI 元件位置。
     */
    private void setupLayout() {
        StackPane.setAlignment(deathTitleText, Pos.CENTER);

        StackPane.setAlignment(detailTextBox, Pos.CENTER);
        detailTextBox.setTranslateY(DETAIL_BOX_TARGET_Y);

        /*
         * 保留原本重生按鈕位置：
         * 右下角。
         */
        StackPane.setAlignment(respawnButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(
                respawnButton,
                new Insets(
                        0,
                        RESPAWN_BUTTON_RIGHT_MARGIN,
                        BUTTON_BOTTOM_MARGIN,
                        0
                )
        );

        StackPane.setAlignment(achievementToast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(
                achievementToast,
                new Insets(0, 0, -2, 0)
        );
    }

    /**
     * 將所有 UI 元件加入 DeathScreen。
     */
    private void assembleNodes() {
        getChildren().addAll(
                background,
                deathTitleText,
                detailTextBox,
                respawnButton,
                achievementToast
        );
    }


    // =========================================================
    // Public API
    // =========================================================

    /**
     * 顯示死亡畫面。
     *
     * @param reason 死亡原因
     * @param deathCount 死亡次數
     */
    public void show(
            DeathReason reason,
            int deathCount
    ) {
        if (reason == null) {
            return;
        }

        updateDeathReasonText(reason);
        updateModeExtraInfo(deathCount);

        setVisible(true);
        toFront();

        stopCurrentShowAnimationIfNeeded();
        resetAnimationState();
        playShowAnimation();
    }

    /**
     * 隱藏死亡畫面。
     */
    public void hide() {
        stopCurrentShowAnimationIfNeeded();

        setVisible(false);
        resetAnimationState();
    }

    /**
     * 顯示成就解鎖提示。
     *
     * @param reason 解鎖的死亡原因
     */
    public void showAchievementUnlock(DeathReason reason) {
        achievementToast.showUnlock(reason);
    }


    // =========================================================
    // Content Update
    // =========================================================

    /**
     * 更新死亡原因文字。
     */
    private void updateDeathReasonText(DeathReason reason) {
        titleText.setText(reason.getTitle());
        subtitleText.setText(reason.getSubtitle());
    }

    /**
     * 根據目前模式更新額外資訊。
     */
    private void updateModeExtraInfo(int deathCount) {
        if (!isStreetEndlessMode()) {
            updateStoryModeExtraInfo(deathCount);
            return;
        }

        updateStreetEndlessExtraInfo();
    }

    /**
     * 更新故事模式死亡資訊。
     */
    private void updateStoryModeExtraInfo(int deathCount) {
        extraInfoText.setText(
                text("menu.deathCount") + deathCount
        );

        newRecordText.setText("");
    }

    /**
     * 更新 Street Endless 死亡資訊。
     */
    private void updateStreetEndlessExtraInfo() {
        int currentDistance = (int) Math.floor(
                getDoubleVar("streetRunDistance", 0)
        );

        int bestDistance = (int) Math.floor(
                getDoubleVar("streetBestDistance", 0)
        );

        boolean newRecord = getBooleanVar("streetNewRecord", false);

        extraInfoText.setText(
                text("menu.score") + currentDistance + "\n" +
                        text("menu.highestScore") + bestDistance
        );

        newRecordText.setText(
                newRecord
                        ? text("menu.newRecord")
                        : ""
        );
    }


    // =========================================================
    // Button Creation
    // =========================================================

    /**
     * 建立圖片文字按鈕。
     *
     * 若圖片不存在，會改用矩形 fallback。
     */
    private StackPane createImageTextButton(
            String imagePath,
            String labelText,
            Runnable action
    ) {
        StackPane button = new StackPane();

        button.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setMinSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setMaxSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setPickOnBounds(true);

        Node backgroundNode = createButtonBackground(
                imagePath,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        );

        Text label = createButtonLabel(labelText);

        button.getChildren().addAll(
                backgroundNode,
                label
        );

        setupButtonEvents(button, action);

        return button;
    }

    /**
     * 建立按鈕文字。
     */
    private Text createButtonLabel(String labelText) {
        Text label = new Text(labelText);

        label.setStyle("""
                -fx-font-size: 22px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        label.setEffect(
                new DropShadow(6, Color.rgb(0, 0, 0, 0.85))
        );

        return label;
    }

    /**
     * 建立按鈕背景。
     */
    private Node createButtonBackground(
            String imagePath,
            double width,
            double height
    ) {
        ImageView imageView = tryCreateButtonImageView(
                imagePath,
                width,
                height
        );

        if (imageView != null) {
            return imageView;
        }

        return createButtonFallbackBackground(width, height);
    }

    /**
     * 嘗試建立圖片背景。
     */
    private ImageView tryCreateButtonImageView(
            String imagePath,
            double width,
            double height
    ) {
        try {
            var url = getClass().getResource(imagePath);

            if (url == null) {
                return null;
            }

            Image image = new Image(url.toExternalForm());

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setPreserveRatio(false);

            return imageView;

        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * 建立圖片不存在時的 fallback 背景。
     */
    private Rectangle createButtonFallbackBackground(
            double width,
            double height
    ) {
        Rectangle fallback = new Rectangle(width, height);

        fallback.setArcWidth(18);
        fallback.setArcHeight(18);
        fallback.setFill(Color.rgb(30, 30, 30, 0.95));
        fallback.setStroke(Color.WHITE);
        fallback.setStrokeWidth(2);

        return fallback;
    }

    /**
     * 設定按鈕滑鼠事件。
     */
    private void setupButtonEvents(
            StackPane button,
            Runnable action
    ) {
        button.setOnMouseClicked(event -> {
            if (action != null) {
                action.run();
            }
        });

        button.setOnMouseEntered(event ->
                playButtonScaleAnimation(button, BUTTON_HOVER_SCALE)
        );

        button.setOnMouseExited(event ->
                playButtonScaleAnimation(button, 1.0)
        );

        button.setOnMousePressed(event -> {
            button.setScaleX(BUTTON_PRESSED_SCALE);
            button.setScaleY(BUTTON_PRESSED_SCALE);
        });

        button.setOnMouseReleased(event -> {
            button.setScaleX(BUTTON_HOVER_SCALE);
            button.setScaleY(BUTTON_HOVER_SCALE);
        });
    }

    /**
     * 播放按鈕縮放動畫。
     */
    private void playButtonScaleAnimation(
            StackPane button,
            double targetScale
    ) {
        ScaleTransition scale = new ScaleTransition(
                Duration.seconds(BUTTON_HOVER_SECONDS),
                button
        );

        scale.setToX(targetScale);
        scale.setToY(targetScale);
        scale.play();
    }

    /**
     * 點擊重生按鈕。
     */
    private void handleRespawnClicked() {
        if (onRespawn != null) {
            onRespawn.run();
        }
    }


    // =========================================================
    // Animation State
    // =========================================================

    /**
     * 重置死亡畫面動畫前狀態。
     */
    private void resetAnimationState() {
        background.setOpacity(1.0);

        resetDeathTitleAnimationState();
        resetDetailTextAnimationState();
        resetButtonAnimationState(respawnButton);
    }

    /**
     * 重置死亡主標題動畫狀態。
     */
    private void resetDeathTitleAnimationState() {
        deathTitleText.setOpacity(0);
        deathTitleText.setScaleX(0.72);
        deathTitleText.setScaleY(0.72);
        deathTitleText.setTranslateY(0);
    }

    /**
     * 重置死亡資訊文字動畫狀態。
     */
    private void resetDetailTextAnimationState() {
        detailTextBox.setOpacity(0);
        detailTextBox.setTranslateY(DETAIL_BOX_START_Y);

        titleText.setOpacity(1);
        titleText.setTranslateY(0);

        subtitleText.setOpacity(1);
        subtitleText.setTranslateY(0);

        extraInfoText.setOpacity(0);
        extraInfoText.setTranslateY(45);

        newRecordText.setOpacity(0);
        newRecordText.setTranslateY(45);
    }

    /**
     * 重置單顆按鈕動畫狀態。
     */
    private void resetButtonAnimationState(StackPane button) {
        button.setOpacity(0);
        button.setTranslateX(BUTTON_START_TRANSLATE_X);
        button.setTranslateY(0);
        button.setScaleX(1);
        button.setScaleY(1);
    }

    /**
     * 若目前死亡畫面動畫正在播放，先停止。
     */
    private void stopCurrentShowAnimationIfNeeded() {
        if (currentShowAnimation != null) {
            currentShowAnimation.stop();
            currentShowAnimation = null;
        }
    }


    // =========================================================
    // Show Animation
    // =========================================================

    /**
     * 播放死亡畫面進場動畫。
     */
    private void playShowAnimation() {
        currentShowAnimation = createShowAnimationSequence();

        currentShowAnimation.setOnFinished(event ->
                currentShowAnimation = null
        );

        currentShowAnimation.play();
    }

    /**
     * 建立完整進場動畫。
     */
    private SequentialTransition createShowAnimationSequence() {
        return new SequentialTransition(
                createDeathTitleAppearAnimation(),
                new PauseTransition(Duration.seconds(SEQUENCE_PAUSE_SECONDS)),
                createDetailAppearAnimation(),
                new PauseTransition(Duration.seconds(SEQUENCE_PAUSE_SECONDS)),
                createButtonAppearAnimation(respawnButton)
        );
    }

    /**
     * 建立死亡主標題出現動畫。
     */
    private ParallelTransition createDeathTitleAppearAnimation() {
        FadeTransition fadeIn = new FadeTransition(
                Duration.seconds(DEATH_TITLE_FADE_SECONDS),
                deathTitleText
        );
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleUp = new ScaleTransition(
                Duration.seconds(DEATH_TITLE_SCALE_SECONDS),
                deathTitleText
        );
        scaleUp.setFromX(0.72);
        scaleUp.setFromY(0.72);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);

        return new ParallelTransition(
                fadeIn,
                scaleUp
        );
    }

    /**
     * 建立死亡原因與額外資訊出現動畫。
     */
    private ParallelTransition createDetailAppearAnimation() {
        TranslateTransition deathTitlePushUp =
                new TranslateTransition(
                        Duration.seconds(DETAIL_ANIMATION_SECONDS),
                        deathTitleText
                );
        deathTitlePushUp.setFromY(0);
        deathTitlePushUp.setToY(DEATH_TITLE_TARGET_Y);
        deathTitlePushUp.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition detailFadeIn =
                new FadeTransition(
                        Duration.seconds(DETAIL_ANIMATION_SECONDS),
                        detailTextBox
                );
        detailFadeIn.setFromValue(0);
        detailFadeIn.setToValue(1);

        TranslateTransition detailMoveUp =
                new TranslateTransition(
                        Duration.seconds(DETAIL_ANIMATION_SECONDS),
                        detailTextBox
                );
        detailMoveUp.setFromY(DETAIL_BOX_START_Y);
        detailMoveUp.setToY(DETAIL_BOX_TARGET_Y);
        detailMoveUp.setInterpolator(Interpolator.EASE_OUT);

        return new ParallelTransition(
                deathTitlePushUp,
                detailFadeIn,
                detailMoveUp,
                createExtraInfoFadeAnimation(),
                createExtraInfoMoveAnimation(),
                createNewRecordFadeAnimation(),
                createNewRecordMoveAnimation()
        );
    }

    /**
     * 建立額外資訊淡入動畫。
     */
    private FadeTransition createExtraInfoFadeAnimation() {
        FadeTransition fade = new FadeTransition(
                Duration.seconds(EXTRA_INFO_ANIMATION_SECONDS),
                extraInfoText
        );

        fade.setFromValue(0);
        fade.setToValue(1);

        return fade;
    }

    /**
     * 建立額外資訊上移動畫。
     */
    private TranslateTransition createExtraInfoMoveAnimation() {
        TranslateTransition move = new TranslateTransition(
                Duration.seconds(EXTRA_INFO_ANIMATION_SECONDS),
                extraInfoText
        );

        move.setFromY(45);
        move.setToY(0);
        move.setInterpolator(Interpolator.EASE_OUT);

        return move;
    }

    /**
     * 建立新紀錄文字淡入動畫。
     */
    private FadeTransition createNewRecordFadeAnimation() {
        FadeTransition fade = new FadeTransition(
                Duration.seconds(EXTRA_INFO_ANIMATION_SECONDS),
                newRecordText
        );

        fade.setFromValue(0);
        fade.setToValue(1);

        return fade;
    }

    /**
     * 建立新紀錄文字上移動畫。
     */
    private TranslateTransition createNewRecordMoveAnimation() {
        TranslateTransition move = new TranslateTransition(
                Duration.seconds(EXTRA_INFO_ANIMATION_SECONDS),
                newRecordText
        );

        move.setFromY(45);
        move.setToY(0);
        move.setInterpolator(Interpolator.EASE_OUT);

        return move;
    }

    /**
     * 建立單顆按鈕出現動畫。
     */
    private ParallelTransition createButtonAppearAnimation(StackPane button) {
        FadeTransition fade = new FadeTransition(
                Duration.seconds(BUTTON_ANIMATION_SECONDS),
                button
        );
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(
                Duration.seconds(BUTTON_ANIMATION_SECONDS),
                button
        );
        slide.setFromX(BUTTON_START_TRANSLATE_X);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        return new ParallelTransition(
                fade,
                slide
        );
    }


    // =========================================================
    // Game Var Helpers
    // =========================================================

    /**
     * 判斷目前是否為 Street Endless 模式。
     */
    private boolean isStreetEndlessMode() {
        return getBooleanVar("streetEndlessMode", false);
    }

    /**
     * 安全取得 boolean game var。
     */
    private boolean getBooleanVar(
            String key,
            boolean defaultValue
    ) {
        try {
            return getb(key);
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    /**
     * 安全取得 double game var。
     */
    private double getDoubleVar(
            String key,
            double defaultValue
    ) {
        try {
            return getd(key);
        } catch (Exception exception) {
            return defaultValue;
        }
    }


    // =========================================================
    // Text Helper
    // =========================================================

    /**
     * 取得目前語言文字。
     */
    private String text(String key) {
        return languageSystem.text(key);
    }
}