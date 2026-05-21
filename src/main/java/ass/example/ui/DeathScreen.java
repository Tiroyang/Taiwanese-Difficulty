package ass.example.ui;

import ass.example.core.DeathReasons;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * 死亡畫面。
 */
public class DeathScreen extends StackPane {

    private static final double SCREEN_WIDTH = 1280;
    private static final double SCREEN_HEIGHT = 720;

    private final Rectangle background = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);

    private final Text deathTitle = new Text("wasted");
    private final Text titleText = new Text();
    private final Text subtitleText = new Text();

    private final StackPane respawnButton;

    private final VBox centerTextBox = new VBox(18);

    public DeathScreen(Runnable onRespawn, Runnable onMainMenu) {
        setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        setVisible(false);
        setPickOnBounds(true);

        background.setFill(Color.rgb(0, 0, 0, 0.78));

        loadFonts();
        setupTexts();

        respawnButton = createImageTextButton(
                "/assets/textures/ui/respawn_button.png",
                "重生",
                onRespawn
        );

        setupLayout();

        getChildren().addAll(
                background,
                centerTextBox,
                respawnButton
        );
    }

    private void loadFonts() {
        try {
            Font.loadFont(
                    getClass().getResourceAsStream("/assets/fonts/Pricedown Bl.otf"),
                    16
            );
        } catch (Exception e) {
            System.out.println("Load failed.");
        }
    }

    private void setupTexts() {
        deathTitle.setStyle("""
                -fx-font-size: 128px;
                -fx-fill: rgb(255, 0, 0, 0.65);
                -fx-font-family: "Pricedown Black";
                -fx-font-weight: bold;
                """);

        titleText.setStyle("""
                -fx-font-size: 32px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        subtitleText.setStyle("""
                -fx-font-size: 24px;
                -fx-fill: white;
                """);

        centerTextBox.setAlignment(Pos.CENTER);
        centerTextBox.getChildren().addAll(
                deathTitle,
                titleText,
                subtitleText
        );
    }

    private void setupLayout() {
        StackPane.setAlignment(centerTextBox, Pos.CENTER);

        StackPane.setAlignment(respawnButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(respawnButton, new Insets(0, 48, 42, 0));
    }

    /**
     * 圖片文字按鈕模板。
     */
    private StackPane createImageTextButton(
            String imagePath,
            String text,
            Runnable action
    ) {
        StackPane button = new StackPane();
        button.setPrefSize(320, 92);
        button.setMinSize(320, 92);
        button.setMaxSize(320, 92);
        button.setPickOnBounds(true);

        Node backgroundNode = createButtonBackground(imagePath, 320, 92);

        Text label = new Text(text);
        label.setStyle("""
                -fx-font-size: 22px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);
        label.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.85)));

        button.getChildren().addAll(backgroundNode, label);

        button.setOnMouseClicked(e -> action.run());

        button.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.seconds(0.08), button);
            scale.setToX(1.06);
            scale.setToY(1.06);
            scale.play();
        });

        button.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.seconds(0.08), button);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });

        button.setOnMousePressed(e -> {
            button.setScaleX(0.96);
            button.setScaleY(0.96);
        });

        button.setOnMouseReleased(e -> {
            button.setScaleX(1.06);
            button.setScaleY(1.06);
        });

        return button;
    }

    private Node createButtonBackground(String imagePath, double width, double height) {
        try {
            var url = getClass().getResource(imagePath);

            if (url != null) {
                Image image = new Image(url.toExternalForm());
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(width);
                imageView.setFitHeight(height);
                imageView.setPreserveRatio(false);
                return imageView;
            }
        } catch (Exception ignored) {
        }

        /*
         * 如果圖片不存在用fallback。
         */
        Rectangle fallback = new Rectangle(width, height);
        fallback.setArcWidth(18);
        fallback.setArcHeight(18);
        fallback.setFill(Color.rgb(30, 30, 30, 0.95));
        fallback.setStroke(Color.WHITE);
        fallback.setStrokeWidth(2);
        return fallback;
    }

    public void show(DeathReasons reason, int deathCount) {
        titleText.setText(reason.getTitle());
        subtitleText.setText(reason.getSubtitle());

        setVisible(true);
        toFront();

        resetAnimationState();
        playShowAnimation();
    }

    public void hide() {
        setVisible(false);
        resetAnimationState();
    }

    private void resetAnimationState() {
        background.setOpacity(1.0);

        deathTitle.setOpacity(0);
        deathTitle.setScaleX(2.3);
        deathTitle.setScaleY(2.3);
        deathTitle.setTranslateY(0);

        titleText.setOpacity(0);
        titleText.setTranslateY(45);

        subtitleText.setOpacity(0);
        subtitleText.setTranslateY(45);

        respawnButton.setOpacity(0);
        respawnButton.setTranslateX(-220);
        respawnButton.setTranslateY(0);
        respawnButton.setScaleX(1);
        respawnButton.setScaleY(1);
    }

    private void playShowAnimation() {
        /*
         * 標題
         */
        FadeTransition deathFadeIn = new FadeTransition(Duration.seconds(0.08), deathTitle);
        deathFadeIn.setFromValue(0);
        deathFadeIn.setToValue(1);

        ScaleTransition deathScale = new ScaleTransition(Duration.seconds(0.26), deathTitle);
        deathScale.setFromX(2.3);
        deathScale.setFromY(2.3);
        deathScale.setToX(1.0);
        deathScale.setToY(1.0);
        deathScale.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition deathTitleAnim = new ParallelTransition(
                deathFadeIn,
                deathScale
        );

        /*
         * 死亡原因、小字
         */
        FadeTransition titleFade = new FadeTransition(Duration.seconds(0.35), titleText);
        titleFade.setFromValue(0);
        titleFade.setToValue(1);

        TranslateTransition titleMove = new TranslateTransition(Duration.seconds(0.35), titleText);
        titleMove.setFromY(45);
        titleMove.setToY(0);
        titleMove.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition subtitleFade = new FadeTransition(Duration.seconds(0.35), subtitleText);
        subtitleFade.setFromValue(0);
        subtitleFade.setToValue(1);

        TranslateTransition subtitleMove = new TranslateTransition(Duration.seconds(0.35), subtitleText);
        subtitleMove.setFromY(45);
        subtitleMove.setToY(0);
        subtitleMove.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition textAnim = new ParallelTransition(
                titleFade,
                titleMove,
                subtitleFade,
                subtitleMove
        );

        /*
         * 重生按鈕
         */
        FadeTransition respawnFade = new FadeTransition(Duration.seconds(0.36), respawnButton);
        respawnFade.setFromValue(0);
        respawnFade.setToValue(1);

        TranslateTransition respawnSlide = new TranslateTransition(Duration.seconds(0.36), respawnButton);
        respawnSlide.setFromX(-220);
        respawnSlide.setToX(0);
        respawnSlide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition buttonsAnim = new ParallelTransition(
                respawnFade,
                respawnSlide
        );

        SequentialTransition sequence = new SequentialTransition(
                deathTitleAnim,
                new PauseTransition(Duration.seconds(0.08)),
                textAnim,
                new PauseTransition(Duration.seconds(0.12)),
                buttonsAnim
        );

        sequence.play();
    }
}