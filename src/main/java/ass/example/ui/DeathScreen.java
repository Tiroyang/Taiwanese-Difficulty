package ass.example.ui;

import ass.example.core.DeathReason;
import ass.example.system.LanguageSystem;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGLForKtKt.getb;
import static com.almasb.fxgl.dsl.FXGLForKtKt.getd;

/**
 * 死亡畫面。
 */
public class DeathScreen extends StackPane {

    private static final double SCREEN_WIDTH = 1280;
    private static final double SCREEN_HEIGHT = 720;

    private final Rectangle background = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);

    private final Text deathTitle = new Text("YOU FAILED");
    private final Text titleText = new Text();
    private final Text subtitleText = new Text();

    private final Text extraInfoText = new Text();
    private final Text newRecordText = new Text();

    private final StackPane respawnButton;

    private final VBox detailTextBox = new VBox(12);

    private final AchievementToast achievementToast = new AchievementToast();

    public DeathScreen(Runnable onRespawn, Runnable onMainMenu) {
        setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        setVisible(false);
        setPickOnBounds(true);

        background.setFill(Color.rgb(0, 0, 0, 0.78));

        loadFonts();
        setupTexts();

        respawnButton = createImageTextButton(
                "/assets/textures/ui/respawn_button.png",
                LanguageSystem.getInstance().text("menu.rebirth"),
                onRespawn
        );

        setupLayout();

        getChildren().addAll(
                background,
                deathTitle,
                detailTextBox,
                respawnButton,
                achievementToast
        );
    }

    private void loadFonts() {
        try {
            Font font = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/OptimusPrinceps.ttf"), 16);
            System.out.println(font.getFamily());
        } catch (Exception e) {
            System.out.println("Load font failed.");
        }
    }

    private void setupTexts() {
        deathTitle.setStyle("""
            -fx-font-size: 128px;
            -fx-fill: rgb(255, 0, 0, 0.65);
            -fx-font-family: "OptimusPrinceps";
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

        extraInfoText.setStyle("""
        -fx-font-size: 21px;
        -fx-fill: rgba(255,255,255,0.82);
        """);

        newRecordText.setStyle("""
        -fx-font-size: 26px;
        -fx-fill: #ffd36a;
        -fx-font-weight: bold;
        """);
        newRecordText.setEffect(new DropShadow(8, Color.BLACK));

        detailTextBox.setAlignment(Pos.CENTER);
        detailTextBox.getChildren().addAll(
                titleText,
                subtitleText,
                extraInfoText,
                newRecordText
        );
    }

    private void setupLayout() {
        StackPane.setAlignment(deathTitle, Pos.CENTER);

        StackPane.setAlignment(detailTextBox, Pos.CENTER);
        detailTextBox.setTranslateY(95);

        StackPane.setAlignment(respawnButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(respawnButton, new Insets(0, 48, 42, 0));
        respawnButton.setTranslateY(50);

        StackPane.setAlignment(achievementToast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(achievementToast, new Insets(0, 0, -2, 0));
    }

    private void updateModeExtraInfo(int deathCount) {
        boolean streetMode = false;

        try {
            streetMode = getb("streetEndlessMode");
        } catch (Exception ignored) {
        }

        if (!streetMode) {
            extraInfoText.setText(LanguageSystem.getInstance().text("menu.deathCount") + deathCount);
            newRecordText.setText("");
            return;
        }

        double currentDistance = 0;
        double bestDistance = 0;
        boolean newRecord = false;

        try {
            currentDistance = getd("streetRunDistance");
        } catch (Exception ignored) {
        }

        try {
            bestDistance = getd("streetBestDistance");
        } catch (Exception ignored) {
        }

        try {
            newRecord = getb("streetNewRecord");
        } catch (Exception ignored) {
        }

        int current = (int) Math.floor(currentDistance);
        int best = (int) Math.floor(bestDistance);

        extraInfoText.setText(
                LanguageSystem.getInstance().text("menu.score") + current + "\n" +
                        LanguageSystem.getInstance().text("menu.highestScore") + best
        );

        if (newRecord) {
            newRecordText.setText(LanguageSystem.getInstance().text("menu.newRecord"));
        } else {
            newRecordText.setText("");
        }
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

    public void show(DeathReason reason, int deathCount) {
        titleText.setText(reason.getTitle());
        subtitleText.setText(reason.getSubtitle());

        updateModeExtraInfo(deathCount);

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

        /*
         * DeathTitle
         */
        deathTitle.setOpacity(0);
        deathTitle.setScaleX(0.72);
        deathTitle.setScaleY(0.72);
        deathTitle.setTranslateY(0);

        /*
         * Title / Subtitle
         */
        detailTextBox.setOpacity(0);
        detailTextBox.setTranslateY(140);

        titleText.setOpacity(1);
        titleText.setTranslateY(0);

        subtitleText.setOpacity(1);
        subtitleText.setTranslateY(0);

        extraInfoText.setOpacity(0);
        extraInfoText.setTranslateY(45);

        newRecordText.setOpacity(0);
        newRecordText.setTranslateY(45);

        /*
         * respawnButton
         */
        respawnButton.setOpacity(0);
        respawnButton.setTranslateX(-220);
        respawnButton.setTranslateY(0);
        respawnButton.setScaleX(1);
        respawnButton.setScaleY(1);
    }

    private void playShowAnimation() {
        /*
         * 1. DeathTitle
         */
        FadeTransition deathFadeIn = new FadeTransition(Duration.seconds(1), deathTitle);
        deathFadeIn.setFromValue(0);
        deathFadeIn.setToValue(1);

        ScaleTransition deathScaleUp = new ScaleTransition(Duration.seconds(0.55), deathTitle);
        deathScaleUp.setFromX(0.72);
        deathScaleUp.setFromY(0.72);
        deathScaleUp.setToX(1.0);
        deathScaleUp.setToY(1.0);
        deathScaleUp.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition deathTitleAppear = new ParallelTransition(
                deathFadeIn,
                deathScaleUp
        );

        /*
         * 2. Title / Subtitle
         */
        TranslateTransition deathTitlePushUp = new TranslateTransition(Duration.seconds(0.45), deathTitle);
        deathTitlePushUp.setFromY(0);
        deathTitlePushUp.setToY(-95);
        deathTitlePushUp.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition detailFadeIn = new FadeTransition(Duration.seconds(0.45), detailTextBox);
        detailFadeIn.setFromValue(0);
        detailFadeIn.setToValue(1);

        TranslateTransition detailMoveUp = new TranslateTransition(Duration.seconds(0.45), detailTextBox);
        detailMoveUp.setFromY(140);
        detailMoveUp.setToY(95);
        detailMoveUp.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition extraFade = new FadeTransition(Duration.seconds(0.35), extraInfoText);
        extraFade.setFromValue(0);
        extraFade.setToValue(1);

        TranslateTransition extraMove = new TranslateTransition(Duration.seconds(0.35), extraInfoText);
        extraMove.setFromY(45);
        extraMove.setToY(0);
        extraMove.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition recordFade = new FadeTransition(Duration.seconds(0.35), newRecordText);
        recordFade.setFromValue(0);
        recordFade.setToValue(1);

        TranslateTransition recordMove = new TranslateTransition(Duration.seconds(0.35), newRecordText);
        recordMove.setFromY(45);
        recordMove.setToY(0);
        recordMove.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition detailAnim = new ParallelTransition(
                deathTitlePushUp,
                detailFadeIn,
                detailMoveUp,
                extraFade,
                extraMove,
                recordFade,
                recordMove
        );

        /*
         * 3. respawnButton
         */
        FadeTransition respawnFade = new FadeTransition(Duration.seconds(0.36), respawnButton);
        respawnFade.setFromValue(0);
        respawnFade.setToValue(1);

        TranslateTransition respawnSlide = new TranslateTransition(Duration.seconds(0.36), respawnButton);
        respawnSlide.setFromX(-220);
        respawnSlide.setToX(0);
        respawnSlide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition respawnAnim = new ParallelTransition(
                respawnFade,
                respawnSlide
        );

        SequentialTransition sequence = new SequentialTransition(
                deathTitleAppear,
                new PauseTransition(Duration.seconds(0.18)),
                detailAnim,
                new PauseTransition(Duration.seconds(0.18)),
                respawnAnim
        );

        sequence.play();
    }

    public void showAchievementUnlock(DeathReason reason) {
        achievementToast.showUnlock(reason);
    }
}