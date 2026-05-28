package ass.example.ui;

import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.dialogue.DialogueButton;
import ass.example.system.dialogue.DialogueLine;
import ass.example.system.dialogue.DialogueSystem;
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
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.net.URL;

public class DialogueUI extends StackPane {

    private static final double SCREEN_WIDTH = 1280;
    private static final double SCREEN_HEIGHT = 720;

    private final DialogueSystem dialogueSystem;
    private final AudioSystem audioSystem = AudioSystem.getInstance();

    private final ImageView portraitView = new ImageView();

    private final Rectangle darkOverlay = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);

    private final StackPane dialogueBox = new StackPane();
    private final Rectangle dialogueBoxBg = new Rectangle(980, 168);

    private final StackPane nameBox = new StackPane();
    private final Rectangle nameBoxBg = new Rectangle(220, 46);
    private final Text nameText = new Text();

    private final Text dialogueText = new Text();

    private final HBox buttonBox = new HBox(14);

    private DialogueLine currentLine;

    private Timeline typewriterTimeline;
    private boolean typing = false;
    private String fullText = "";
    private int currentCharIndex = 0;

    public DialogueUI(DialogueSystem dialogueSystem) {
        this.dialogueSystem = dialogueSystem;

        setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        setPickOnBounds(true);

        setupOverlay();
        setupPortrait();
        setupDialogueBox();
        setupNameBox();
        setupButtonBox();

        getChildren().addAll(
                darkOverlay,
                portraitView,
                dialogueBox,
                nameBox,
                buttonBox
        );

        setOnMouseClicked(e -> {
            /*
             * 避免按到按鈕時，同時觸發對話框下一句。
             */
            Node target = (Node) e.getTarget();

            if (isInsideButtonBox(target)) {
                return;
            }

            handleDialogueClick();
        });
    }

    private boolean hasCharacterName(DialogueLine line) {
        return line != null
                && line.getCharacterName() != null
                && !line.getCharacterName().isBlank();
    }

    private void setupOverlay() {
        darkOverlay.setFill(Color.rgb(0, 0, 0, 0.28));
        darkOverlay.setMouseTransparent(true);
    }

    private void setupPortrait() {
        portraitView.setFitHeight(560);
        portraitView.setPreserveRatio(true);
        portraitView.setSmooth(false);
        portraitView.setOpacity(0);

        StackPane.setAlignment(portraitView, Pos.BOTTOM_LEFT);
        StackPane.setMargin(portraitView, new Insets(0, 0, 108, 70));
    }

    private void setupDialogueBox() {
        dialogueBoxBg.setArcWidth(24);
        dialogueBoxBg.setArcHeight(24);
        dialogueBoxBg.setFill(Color.rgb(0, 0, 0, 0.82));
        dialogueBoxBg.setStroke(Color.rgb(255, 255, 255, 0.75));
        dialogueBoxBg.setStrokeWidth(2.0);
        dialogueBoxBg.setEffect(new DropShadow(18, Color.rgb(0, 0, 0, 0.85)));

        dialogueText.setWrappingWidth(890);
        dialogueText.setStyle("""
                -fx-font-size: 26px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);
        dialogueText.setEffect(new DropShadow(5, Color.BLACK));

        StackPane.setAlignment(dialogueText, Pos.TOP_LEFT);
        StackPane.setMargin(dialogueText, new Insets(34, 42, 30, 42));

        dialogueBox.getChildren().addAll(dialogueBoxBg, dialogueText);
        dialogueBox.setPrefSize(980, 168);
        dialogueBox.setMaxSize(980, 168);

        StackPane.setAlignment(dialogueBox, Pos.BOTTOM_CENTER);
        StackPane.setMargin(dialogueBox, new Insets(0, 0, 36, 0));
    }

    private void setupNameBox() {
        nameBoxBg.setArcWidth(16);
        nameBoxBg.setArcHeight(16);
        nameBoxBg.setFill(Color.rgb(213, 105, 16, 0.92));
        nameBoxBg.setStroke(Color.WHITE);
        nameBoxBg.setStrokeWidth(1.5);

        nameText.setStyle("""
                -fx-font-size: 24px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);
        nameText.setEffect(new DropShadow(4, Color.BLACK));

        nameBox.getChildren().addAll(nameBoxBg, nameText);
        nameBox.setPrefSize(220, 46);
        nameBox.setMaxSize(220, 46);

        /*
         * 名稱框壓在對話框上方偏左。
         */
        StackPane.setAlignment(nameBox, Pos.BOTTOM_LEFT);
        StackPane.setMargin(nameBox, new Insets(0, 0, 188, 200));
    }

    private void setupButtonBox() {
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setVisible(false);
        buttonBox.setOpacity(0);

        StackPane.setAlignment(buttonBox, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(buttonBox, new Insets(0, 170, 62, 0));
    }

    public void showLine(DialogueLine line) {
        stopTypewriter();

        currentLine = line;

        buttonBox.getChildren().clear();
        buttonBox.setVisible(false);
        buttonBox.setOpacity(0);

        boolean hasCharacter = hasCharacterName(line);

        nameBox.setVisible(hasCharacter);
        nameBox.setManaged(hasCharacter);

        if (hasCharacter) {
            nameText.setText(line.getCharacterName());
        } else {
            nameText.setText("");
        }

        loadPortrait(line.getDefaultPortraitPath());

        playLineEnterAnimation();

        startTypewriter(line);
    }

    private void startTypewriter(DialogueLine line) {
        fullText = line.getText();
        currentCharIndex = 0;
        dialogueText.setText("");

        typing = true;

        if (hasCharacterName(line)) {
            /*
             * 只有有角色名稱時，才切換說話立繪與放大。
             */
            loadPortrait(line.getSpeakingPortraitPath());

            ScaleTransition speakScale = new ScaleTransition(Duration.seconds(0.14), portraitView);
            speakScale.setToX(1.06);
            speakScale.setToY(1.06);
            speakScale.setInterpolator(Interpolator.EASE_OUT);
            speakScale.play();
        } else {
            /*
             * 旁白 / 系統文字：
             * 不做說話動畫，也不切 speaking portrait。
             */
            portraitView.setScaleX(1.0);
            portraitView.setScaleY(1.0);
        }

        typewriterTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0.035), e -> printNextCharacter())
        );

        typewriterTimeline.setCycleCount(Animation.INDEFINITE);
        typewriterTimeline.play();
    }

    private void printNextCharacter() {
        if (currentCharIndex >= fullText.length()) {
            finishTypewriter();
            return;
        }

        currentCharIndex++;

        dialogueText.setText(fullText.substring(0, currentCharIndex));

        /*
         * 你可以新增 SoundId.DIALOGUE_TYPE。
         * 若還沒有，就先用 BUTTON_HOVER 測試。
         */
        if (currentCharIndex % 2 == 0) {
            audioSystem.playSFX(SoundId.DIALOG_BLIP);
        }
    }

    private void finishTypewriter() {
        stopTypewriter();

        dialogueText.setText(fullText);
        typing = false;

        /*
         * 打字結束後角色回到默認立繪與大小。
         */
        if (hasCharacterName(currentLine)) {
            loadPortrait(currentLine.getDefaultPortraitPath());

            ScaleTransition scaleBack = new ScaleTransition(Duration.seconds(0.14), portraitView);
            scaleBack.setToX(1.0);
            scaleBack.setToY(1.0);
            scaleBack.setInterpolator(Interpolator.EASE_OUT);
            scaleBack.play();
        }

        showButtonsIfNeeded();
    }

    private void skipTypewriter() {
        if (!typing) {
            return;
        }

        finishTypewriter();
    }

    private void stopTypewriter() {
        if (typewriterTimeline != null) {
            typewriterTimeline.stop();
            typewriterTimeline = null;
        }
    }

    private void handleDialogueClick() {
        if (currentLine == null) {
            return;
        }

        /*
         * 打字機尚未結束時，點擊只跳過打字。
         */
        if (typing) {
            skipTypewriter();
            return;
        }

        /*
         * 有按鈕時，不允許點對話框跳過，除非這句設定 allowClickNext。
         */
        if (currentLine.hasButtons() && !currentLine.isAllowClickNext()) {
            return;
        }

        /*
         * 打字結束後，若允許點擊下一句，就切下一句或結束。
         */
        if (currentLine.isAllowClickNext()) {
            dialogueSystem.nextFrom(currentLine);
        }
    }

    private void showButtonsIfNeeded() {
        if (currentLine == null || !currentLine.hasButtons()) {
            return;
        }

        buttonBox.getChildren().clear();

        for (DialogueButton buttonData : currentLine.getButtons()) {
            StackPane button = createChoiceButton(buttonData);
            buttonBox.getChildren().add(button);
        }

        buttonBox.setVisible(true);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.16), buttonBox);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private StackPane createChoiceButton(DialogueButton buttonData) {
        double width = 156;
        double height = 44;

        Rectangle bg = new Rectangle(width, height);
        bg.setArcWidth(12);
        bg.setArcHeight(12);
        bg.setFill(Color.rgb(0, 0, 0, 0.78));
        bg.setStroke(Color.rgb(255, 255, 255, 0.72));
        bg.setStrokeWidth(1.4);

        Text label = new Text(buttonData.getText());
        label.setStyle("""
                -fx-font-size: 20px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        StackPane button = new StackPane(bg, label);
        button.setPrefSize(width, height);
        button.setMaxSize(width, height);
        button.setPickOnBounds(true);

        button.setOnMouseEntered(e -> {
            bg.setFill(Color.rgb(255, 255, 255, 0.86));
            label.setFill(Color.BLACK);
            audioSystem.playSFX(SoundId.BUTTON_HOVER);
        });

        button.setOnMouseExited(e -> {
            bg.setFill(Color.rgb(0, 0, 0, 0.78));
            label.setFill(Color.WHITE);
        });

        button.setOnMouseClicked(e -> {
            e.consume();

            if (typing) {
                skipTypewriter();
                return;
            }

            audioSystem.playSFX(SoundId.BUTTON_PRESSED);
            buttonData.run();
        });

        return button;
    }

    private void loadPortrait(String path) {
        if (path == null || path.isBlank()) {
            portraitView.setImage(null);
            return;
        }

        try {
            URL url = getClass().getResource(path);

            if (url == null) {
                System.out.println("Portrait not found: " + path);
                portraitView.setImage(null);
                return;
            }

            portraitView.setImage(new Image(url.toExternalForm()));
        } catch (Exception e) {
            System.out.println("Portrait load failed: " + path);
            e.printStackTrace();
        }
    }

    private void playLineEnterAnimation() {
        portraitView.setOpacity(1);

        dialogueBox.setOpacity(0);
        dialogueBox.setTranslateY(18);

        nameBox.setOpacity(0);
        nameBox.setTranslateY(18);

        FadeTransition boxFade = new FadeTransition(Duration.seconds(0.14), dialogueBox);
        boxFade.setFromValue(0);
        boxFade.setToValue(1);

        TranslateTransition boxMove = new TranslateTransition(Duration.seconds(0.14), dialogueBox);
        boxMove.setFromY(18);
        boxMove.setToY(0);
        boxMove.setInterpolator(Interpolator.EASE_OUT);

        if (hasCharacterName(currentLine)) {
            nameBox.setOpacity(0);
            nameBox.setTranslateY(18);

            FadeTransition nameFade = new FadeTransition(Duration.seconds(0.14), nameBox);
            nameFade.setFromValue(0);
            nameFade.setToValue(1);

            TranslateTransition nameMove = new TranslateTransition(Duration.seconds(0.14), nameBox);
            nameMove.setFromY(18);
            nameMove.setToY(0);
            nameMove.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(
                    boxFade,
                    boxMove,
                    nameFade,
                    nameMove
            ).play();

        } else {
            nameBox.setOpacity(0);
            nameBox.setVisible(false);

            new ParallelTransition(
                    boxFade,
                    boxMove
            ).play();
        }
    }

    private boolean isInsideButtonBox(Node node) {
        Node current = node;

        while (current != null) {
            if (current == buttonBox) {
                return true;
            }

            current = current.getParent();
        }

        return false;
    }
}