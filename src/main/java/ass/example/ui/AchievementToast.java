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
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * AchievementToast
 *
 * 成就解鎖提示 UI。
 *
 * 功能：
 * 1. 顯示死亡成就解鎖提示。
 * 2. 左側顯示死亡原因 icon。
 * 3. 右側顯示「已解鎖」與死亡原因名稱。
 * 4. 支援死亡原因自訂 icon。
 * 5. 若死亡原因沒有 icon，使用預設 icon。
 * 6. 播放滑入、停留、滑出動畫。
 */
public class AchievementToast extends StackPane {
 
    // Layout Constants 

    /**
     * Toast 寬度。
     */
    private static final double TOAST_WIDTH = 420.0;

    /**
     * Toast 高度。
     */
    private static final double TOAST_HEIGHT = 74.0;

    /**
     * 左側 icon 尺寸。
     *
     * 與 Toast 高度相同，形成正方形 icon 區。
     */
    private static final double ICON_SIZE = TOAST_HEIGHT;

    /**
     * 文字區左側 padding。
     */
    private static final double TEXT_LEFT_PADDING = 14.0;

    /**
     * 文字區右側 padding。
     */
    private static final double TEXT_RIGHT_PADDING = 18.0;

 
    // Animation Constants 

    /**
     * Toast 出現 / 消失動畫秒數。
     */
    private static final double ANIMATION_SECONDS = 0.22;

    /**
     * Toast 停留秒數。
     */
    private static final double STAY_SECONDS = 2.5;

    /**
     * Toast 從下方滑入 / 滑出的 Y 位移。
     */
    private static final double SLIDE_OFFSET_Y = 90.0;
 
    // Asset Constants 

    /**
     * 當 DeathReason 沒有 iconPath 時使用的預設 icon。
     */
    private static final String DEFAULT_ICON_PATH = "/assets/textures/ui/deathicon/achievementtoast_sample.jpg";

 
    // Dependencies 

    /**
     * 語言系統。
     */
    private final LanguageSystem languageSystem = LanguageSystem.getInstance();

 
    // UI Nodes 

    /**
     * Toast 背景。
     */
    private final Rectangle background = new Rectangle(TOAST_WIDTH, TOAST_HEIGHT);

    /**
     * 左側成就 icon。
     */
    private final ImageView iconView = new ImageView();

    /**
     * 上方小標題。
     *
     * 例如：「已解鎖」
     */
    private final Text titleText = new Text();

    /**
     * 下方成就名稱。
     *
     * 例如：「撞到天花板」
     */
    private final Text subtitleText = new Text();

 
    // Runtime State 

    /**
     * 目前播放中的動畫。
     *
     * 若新的成就解鎖時上一個動畫還沒播完，
     * 會先停止舊動畫再播放新的。
     */
    private SequentialTransition currentAnimation;

 
    // Constructor 

    /**
     * 建立 AchievementToast。
     */
    public AchievementToast() {
        setupRoot();
        setupBackground();
        setupIcon();
        setupTexts();
        setupLayout();
    }

 
    // Setup 

    /**
     * 設定 Toast 根節點。
     */
    private void setupRoot() {
        setPrefSize(TOAST_WIDTH, TOAST_HEIGHT);
        setMinSize(TOAST_WIDTH, TOAST_HEIGHT);
        setMaxSize(TOAST_WIDTH, TOAST_HEIGHT);

        setVisible(false);
        setMouseTransparent(true);
    }

    /**
     * 設定背景。
     */
    private void setupBackground() {
        background.setFill(Color.rgb(0, 0, 0, 0.82));
    }

    /**
     * 設定 icon。
     */
    private void setupIcon() {
        iconView.setFitWidth(ICON_SIZE);
        iconView.setFitHeight(ICON_SIZE);

        /*
         * false：
         * 強制壓成 74 x 74 正方形。
         *
         * 若希望保持圖片比例，可以改成 true。
         */
        iconView.setPreserveRatio(false);
        iconView.setSmooth(true);
    }

    /**
     * 設定文字樣式。
     */
    private void setupTexts() {
        titleText.setStyle("""
                -fx-font-size: 15px;
                -fx-fill: rgba(255,255,255,0.72);
                -fx-font-weight: bold;
                """);

        subtitleText.setStyle("""
                -fx-font-size: 22px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        subtitleText.setEffect(new DropShadow(6, Color.BLACK));
    }

    /**
     * 組裝 Toast 版面。
     */
    private void setupLayout() {
        VBox textBox = createTextBox();
        HBox content = createContentBox(textBox);

        getChildren().addAll(background, content);

        StackPane.setAlignment(content, Pos.CENTER_LEFT);
    }

    /**
     * 建立文字區。
     */
    private VBox createTextBox() {
        VBox textBox = new VBox(4);

        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setPadding(new Insets(
                0,
                TEXT_RIGHT_PADDING,
                0,
                TEXT_LEFT_PADDING
        ));

        textBox.getChildren().addAll(
                titleText,
                subtitleText
        );

        return textBox;
    }

    /**
     * 建立整體內容區。
     */
    private HBox createContentBox(VBox textBox) {
        HBox content = new HBox(0);

        content.setAlignment(Pos.CENTER_LEFT);
        content.setPrefSize(TOAST_WIDTH, TOAST_HEIGHT);
        content.setMaxSize(TOAST_WIDTH, TOAST_HEIGHT);

        content.getChildren().addAll(
                iconView,
                textBox
        );

        return content;
    }

 
    // Public API 

    /**
     * 顯示成就解鎖提示。
     *
     * @param reason 解鎖的死亡原因
     */
    public void showUnlock(DeathReason reason) {
        if (reason == null) {
            return;
        }

        updateContent(reason);
        stopCurrentAnimationIfNeeded();
        resetBeforeAnimation();

        currentAnimation = createToastAnimation();

        currentAnimation.setOnFinished(event -> {
            setVisible(false);
            currentAnimation = null;
        });

        currentAnimation.play();
    }

 
    // Content Update 

    /**
     * 更新 Toast 顯示內容。
     */
    private void updateContent(DeathReason reason) {
        setIcon(reason);

        titleText.setText(languageSystem.text("achievement.unlocked"));
        subtitleText.setText(reason.getTitle());
    }

    /**
     * 設定死亡原因 icon。
     *
     * 若 DeathReason 沒有 icon，會使用預設 icon。
     */
    private void setIcon(DeathReason reason) {
        Image image = loadDeathIcon(reason);

        if (image == null) {
            image = loadImage(DEFAULT_ICON_PATH);
        }

        iconView.setImage(image);
    }

 
    // Animation 

    /**
     * 若目前動畫尚未播完，先停止。
     */
    private void stopCurrentAnimationIfNeeded() {
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
        }
    }

    /**
     * 播放動畫前重置狀態。
     */
    private void resetBeforeAnimation() {
        setVisible(true);
        setOpacity(0);
        setTranslateY(SLIDE_OFFSET_Y);
    }

    /**
     * 建立完整 Toast 動畫。
     *
     * 流程：
     * 1. 淡入 + 上滑
     * 2. 停留
     * 3. 淡出 + 下滑
     */
    private SequentialTransition createToastAnimation() {
        ParallelTransition showAnimation = createShowAnimation();
        PauseTransition stayAnimation = new PauseTransition(
                Duration.seconds(STAY_SECONDS)
        );
        ParallelTransition hideAnimation = createHideAnimation();

        return new SequentialTransition(
                showAnimation,
                stayAnimation,
                hideAnimation
        );
    }

    /**
     * 建立出現動畫。
     */
    private ParallelTransition createShowAnimation() {
        FadeTransition fadeIn = new FadeTransition(
                Duration.seconds(ANIMATION_SECONDS),
                this
        );
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(
                Duration.seconds(ANIMATION_SECONDS),
                this
        );
        slideIn.setFromY(SLIDE_OFFSET_Y);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        return new ParallelTransition(
                fadeIn,
                slideIn
        );
    }

    /**
     * 建立消失動畫。
     */
    private ParallelTransition createHideAnimation() {
        FadeTransition fadeOut = new FadeTransition(
                Duration.seconds(ANIMATION_SECONDS),
                this
        );
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(
                Duration.seconds(ANIMATION_SECONDS),
                this
        );
        slideOut.setFromY(0);
        slideOut.setToY(SLIDE_OFFSET_Y);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        return new ParallelTransition(
                fadeOut,
                slideOut
        );
    }

 
    // Image Loading 

    /**
     * 載入 DeathReason 指定的 icon。
//     *
//     * 支援：
//     * 1. "assets/textures/ui/deathicon/xxx.png"
//     * 2. "/assets/textures/ui/deathicon/xxx.png"
     *
     * @param reason 死亡原因
     * @return Image；若沒有 icon 或載入失敗，回傳 null
     */
    private Image loadDeathIcon(DeathReason reason) {
        String iconPath = reason.getIconPath();

        if (iconPath == null || iconPath.isBlank()) {
            return null;
        }

        return loadImage(normalizeResourcePath(iconPath));
    }

    /**
     * 將資源路徑補成以 "/" 開頭。
     */
    private String normalizeResourcePath(String path) {
        if (path.startsWith("/")) {
            return path;
        }

        return "/" + path;
    }

    /**
     * 載入圖片資源。
     *
     * @param path 圖片資源路徑
     * @return Image；若載入失敗，回傳 null
     */
    private Image loadImage(String path) {
        try {
            var url = getClass().getResource(path);

            if (url == null) {
                System.out.println("AchievementToast icon not found: " + path);
                return null;
            }

            return new Image(url.toExternalForm());

        } catch (Exception exception) {
            System.out.println("AchievementToast icon load failed: " + path);
            exception.printStackTrace();
            return null;
        }
    }
}