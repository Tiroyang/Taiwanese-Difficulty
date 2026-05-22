package ass.example.ui;

import ass.example.core.DeathReason;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class AchievementToast extends StackPane {

    private static final double TOAST_WIDTH = 420;
    private static final double TOAST_HEIGHT = 74;
    private static final double ICON_SIZE = TOAST_HEIGHT;

    private static final String DEFAULT_ICON_PATH = "/assets/textures/ui/deathicon/achievementtoast_sample.jpg";

    private final Rectangle background = new Rectangle(TOAST_WIDTH, TOAST_HEIGHT);

    private final ImageView iconView = new ImageView();

    private final Text title = new Text("已解鎖");
    private final Text subtitle = new Text();

    private SequentialTransition currentAnimation;

    public AchievementToast() {
        setPrefSize(TOAST_WIDTH, TOAST_HEIGHT);
        setMinSize(TOAST_WIDTH, TOAST_HEIGHT);
        setMaxSize(TOAST_WIDTH, TOAST_HEIGHT);
        setVisible(false);
        setMouseTransparent(true);

        setupBackground();
        setupIcon();
        setupTexts();
        setupLayout();
    }

    private void setupBackground() {
        background.setFill(Color.rgb(0, 0, 0, 0.82));
    }

    private void setupIcon() {
        iconView.setFitWidth(ICON_SIZE);
        iconView.setFitHeight(ICON_SIZE);

        /*
         * false：強制壓成 74 x 74 正方形。
         * 如果你想保持圖片比例，改成 true。
         */
        iconView.setPreserveRatio(false);
        iconView.setSmooth(true);
    }

    private void setupTexts() {
        title.setStyle("""
                -fx-font-size: 15px;
                -fx-fill: rgba(255,255,255,0.72);
                -fx-font-weight: bold;
                """);

        subtitle.setStyle("""
                -fx-font-size: 22px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        subtitle.setEffect(new DropShadow(6, Color.BLACK));
    }

    private void setupLayout() {
        VBox textBox = new VBox(4);
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setPadding(new Insets(0, 18, 0, 14));
        textBox.getChildren().addAll(title, subtitle);

        HBox content = new HBox(0);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPrefSize(TOAST_WIDTH, TOAST_HEIGHT);
        content.setMaxSize(TOAST_WIDTH, TOAST_HEIGHT);
        content.getChildren().addAll(iconView, textBox);

        getChildren().addAll(background, content);
        StackPane.setAlignment(content, Pos.CENTER_LEFT);
    }

    public void showUnlock(DeathReason reason) {
        setIcon(reason);
        subtitle.setText(reason.getTitle());

        if (currentAnimation != null) {
            currentAnimation.stop();
        }

        setVisible(true);
        setOpacity(0);
        setTranslateY(90);
        setScaleX(0.95);
        setScaleY(0.95);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.22), this);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.seconds(0.22), this);
        slideIn.setFromY(90);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleIn = new ScaleTransition(Duration.seconds(0.22), this);
        scaleIn.setFromX(0.95);
        scaleIn.setFromY(0.95);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);

        PauseTransition stay = new PauseTransition(Duration.seconds(2.5));

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.22), this);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.seconds(0.22), this);
        slideOut.setFromY(0);
        slideOut.setToY(90);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition showAnim = new ParallelTransition(
                fadeIn,
                slideIn,
                scaleIn
        );

        ParallelTransition hideAnim = new ParallelTransition(
                fadeOut,
                slideOut
        );

        currentAnimation = new SequentialTransition(
                showAnim,
                stay,
                hideAnim
        );

        currentAnimation.setOnFinished(e -> {
            setVisible(false);
            currentAnimation = null;
        });

        currentAnimation.play();
    }

    private void setIcon(DeathReason reason) {
        Image image = loadDeathIcon(reason);

        if (image == null) {
            image = loadImage(DEFAULT_ICON_PATH);
        }

        iconView.setImage(image);
    }

    private Image loadDeathIcon(DeathReason reason) {
        if (reason == null) {
            return null;
        }

        String iconPath = reason.getIconPath();

        if (iconPath == null || iconPath.isBlank()) {
            return null;
        }

        /*
         * 支援兩種寫法：
         * 1. "assets/textures/ui/deathicon/xxx.png"
         * 2. "/assets/textures/ui/deathicon/xxx.png"
         */
        if (!iconPath.startsWith("/")) {
            iconPath = "/" + iconPath;
        }

        return loadImage(iconPath);
    }

    private Image loadImage(String path) {
        try {
            var url = getClass().getResource(path);

            if (url == null) {
                System.out.println("AchievementToast icon not found: " + path);
                return null;
            }

            return new Image(url.toExternalForm());

        } catch (Exception e) {
            System.out.println("AchievementToast icon load failed: " + path);
            return null;
        }
    }
}