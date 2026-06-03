package ass.example.ui;

import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.CursorManager;
import ass.example.system.LanguageSystem;
import ass.example.system.dialogue.DialogueLine;
import ass.example.system.dialogue.DialogueLine.DialogueButton;
import ass.example.system.dialogue.DialogueSystem;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.net.URL;

/**
 * DialogueUI
 *
 * 對話系統的畫面層。
 *
 * 功能：
 * 1. 顯示角色立繪。
 * 2. 顯示半透明黑色背景。
 * 3. 顯示對話框。
 * 4. 顯示角色名稱框。
 * 5. 顯示打字機文字效果。
 * 6. 顯示對話選項按鈕。
 * 7. 處理玩家點擊對話框進入下一句。
 * 8. 處理玩家點擊選項按鈕。
 *
 * 職責分工：
 * - DialogueUI 只負責畫面與點擊行為。
 * - DialogueLine 保存單句對話資料。
 * - DialogueLine.DialogueButton 保存選項資料。
 * - DialogueSystem 控制對話流程與切換。
 * - DialogueDatabase 保存所有對話資料。
 */
public class DialogueUI extends StackPane {

    // =========================================================
    // Screen Settings
    // =========================================================

    private static final double SCREEN_WIDTH = 1280.0;
    private static final double SCREEN_HEIGHT = 720.0;


    // =========================================================
    // Portrait Settings
    // =========================================================

    private static final double PORTRAIT_HEIGHT = 1000.0;

    private static final double PORTRAIT_LAYOUT_X = 100.0;
    private static final double PORTRAIT_LAYOUT_Y = 50.0;

    private static final double PORTRAIT_SPEAK_SCALE = 1.03;
    private static final double PORTRAIT_SCALE_DURATION = 0.14;


    // =========================================================
    // Dialogue Box Settings
    // =========================================================

    private static final double DIALOGUE_BOX_WIDTH = 980.0;
    private static final double DIALOGUE_BOX_HEIGHT = 168.0;

    private static final double DIALOGUE_TEXT_WIDTH = 890.0;

    private static final double DIALOGUE_BOX_BOTTOM_MARGIN = 36.0;


    // =========================================================
    // Name Box Settings
    // =========================================================

    private static final double NAME_BOX_WIDTH = 110.0;
    private static final double NAME_BOX_HEIGHT = 46.0;


    // =========================================================
    // Choice Button Settings
    // =========================================================

    private static final double BUTTON_BOX_GAP = 14.0;

    private static final double CHOICE_BUTTON_WIDTH = 156.0;
    private static final double CHOICE_BUTTON_HEIGHT = 44.0;


    // =========================================================
    // Animation Settings
    // =========================================================

    private static final double LINE_ENTER_DURATION = 0.14;
    private static final double LINE_ENTER_OFFSET_Y = 18.0;

    private static final double TYPEWRITER_INTERVAL_SECONDS = 0.035;

    private static final double BUTTON_FADE_DURATION = 0.16;


    // =========================================================
    // Dependencies
    // =========================================================

    /**
     * 對話流程控制器。
     */
    private final DialogueSystem dialogueSystem;

    /**
     * 音效系統。
     *
     * AudioSystem 是全域服務，適合單例。
     */
    private final AudioSystem audioSystem = AudioSystem.getInstance();

    /**
     * 語言系統。
     *
     * LanguageSystem 是全域服務，適合單例。
     */
    private final LanguageSystem languageSystem = LanguageSystem.getInstance();


    // =========================================================
    // UI Nodes
    // =========================================================

    /**
     * 角色立繪。
     */
    private final ImageView portraitView = new ImageView();

    /**
     * 對話時覆蓋全畫面的黑色半透明背景。
     */
    private final Rectangle darkOverlay = new Rectangle(
            SCREEN_WIDTH,
            SCREEN_HEIGHT
    );

    /**
     * 對話框容器。
     */
    private final StackPane dialogueBox = new StackPane();

    /**
     * 對話框背景。
     */
    private final Rectangle dialogueBoxBg = new Rectangle(
            DIALOGUE_BOX_WIDTH,
            DIALOGUE_BOX_HEIGHT
    );

    /**
     * 角色名稱框容器。
     */
    private final StackPane nameBox = new StackPane();

    /**
     * 角色名稱框背景。
     */
    private final Rectangle nameBoxBg = new Rectangle(
            NAME_BOX_WIDTH,
            NAME_BOX_HEIGHT
    );

    /**
     * 角色名稱文字。
     */
    private final Text nameText = new Text();

    /**
     * 對話文字。
     */
    private final Text dialogueText = new Text();

    /**
     * 選項按鈕列。
     */
    private final HBox buttonBox = new HBox(BUTTON_BOX_GAP);


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * 目前正在顯示的對話行。
     */
    private DialogueLine currentLine;

    /**
     * 打字機 Timeline。
     */
    private Timeline typewriterTimeline;

    /**
     * 目前是否正在打字。
     */
    private boolean typing = false;

    /**
     * 目前完整對話文字。
     */
    private String fullText = "";

    /**
     * 目前已顯示到第幾個字。
     */
    private int currentCharIndex = 0;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立對話 UI。
     *
     * @param dialogueSystem 對話流程控制器
     */
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
                nameBox,
                dialogueBox,
                buttonBox
        );

        CursorManager.install(this);

        setOnMouseClicked(event -> {
            Node target = (Node) event.getTarget();

            if (isInsideButtonBox(target)) {
                return;
            }

            handleDialogueClick();
        });
    }


    // =========================================================
    // Public API
    // =========================================================

    /**
     * 顯示指定對話行。
     *
     * 流程：
     * 1. 停止上一句打字機。
     * 2. 記錄目前對話行。
     * 3. 清除並隱藏選項。
     * 4. 更新名稱框。
     * 5. 載入預設立繪。
     * 6. 播放對話框進場動畫。
     * 7. 開始打字機效果。
     *
     * @param line 要顯示的對話行
     */
    public void showLine(DialogueLine line) {
        stopTypewriter();

        currentLine = line;

        hideAndClearButtons();
        updateNameBox(line);

        loadPortrait(line.getDefaultPortraitPath());

        playLineEnterAnimation();
        startTypewriter(line);
    }


    // =========================================================
    // Setup UI
    // =========================================================

    /**
     * 設定黑色半透明背景。
     */
    private void setupOverlay() {
        darkOverlay.setFill(Color.rgb(0, 0, 0, 0.28));
        darkOverlay.setMouseTransparent(true);
    }

    /**
     * 設定角色立繪。
     */
    private void setupPortrait() {
        portraitView.setFitHeight(PORTRAIT_HEIGHT);
        portraitView.setPreserveRatio(true);
        portraitView.setSmooth(false);
        portraitView.setOpacity(0);
        portraitView.setManaged(false);

        portraitView.setLayoutX(PORTRAIT_LAYOUT_X);
        portraitView.setLayoutY(PORTRAIT_LAYOUT_Y);
    }

    /**
     * 設定對話框。
     */
    private void setupDialogueBox() {
        dialogueBoxBg.setArcWidth(24);
        dialogueBoxBg.setArcHeight(24);
        dialogueBoxBg.setFill(createPinkDotPattern());
        dialogueBoxBg.setStroke(Color.WHITE);
        dialogueBoxBg.setStrokeWidth(2.0);
        dialogueBoxBg.setEffect(
                new DropShadow(18, Color.rgb(0, 0, 0, 0.35))
        );

        dialogueText.setWrappingWidth(DIALOGUE_TEXT_WIDTH);
        dialogueText.setStyle("""
                -fx-font-size: 26px;
                -fx-fill: white;
                -fx-font-weight: bold;
                -fx-stroke: black;
                -fx-stroke-width: 0.4px;
                """);
        dialogueText.setEffect(new DropShadow(5, Color.BLACK));

        StackPane.setAlignment(dialogueText, Pos.TOP_LEFT);
        StackPane.setMargin(
                dialogueText,
                new Insets(34, 42, 30, 42)
        );

        dialogueBox.getChildren().addAll(
                dialogueBoxBg,
                dialogueText
        );

        dialogueBox.setPrefSize(DIALOGUE_BOX_WIDTH, DIALOGUE_BOX_HEIGHT);
        dialogueBox.setMinSize(DIALOGUE_BOX_WIDTH, DIALOGUE_BOX_HEIGHT);
        dialogueBox.setMaxSize(DIALOGUE_BOX_WIDTH, DIALOGUE_BOX_HEIGHT);

        StackPane.setAlignment(dialogueBox, Pos.BOTTOM_CENTER);
        StackPane.setMargin(
                dialogueBox,
                new Insets(0, 0, DIALOGUE_BOX_BOTTOM_MARGIN, 0)
        );
    }

    /**
     * 設定角色名稱框。
     */
    private void setupNameBox() {
        nameBoxBg.setArcWidth(16);
        nameBoxBg.setArcHeight(16);
        nameBoxBg.setFill(Color.rgb(247, 230, 238, 0.92));
        nameBoxBg.setStroke(Color.WHITE);
        nameBoxBg.setStrokeWidth(1.5);

        nameText.setStyle("""
                -fx-font-size: 24px;
                -fx-fill: white;
                -fx-font-weight: bold;
                -fx-stroke: HotPink;
                -fx-stroke-width: 1.5px;
                -fx-stroke-type: outside;
                """);

        nameBox.getChildren().addAll(
                nameBoxBg,
                nameText
        );

        nameBox.setPrefSize(NAME_BOX_WIDTH, NAME_BOX_HEIGHT);
        nameBox.setMaxSize(NAME_BOX_WIDTH, NAME_BOX_HEIGHT);

        StackPane.setAlignment(nameBox, Pos.BOTTOM_LEFT);
        StackPane.setMargin(
                nameBox,
                new Insets(0, 0, 200, 200)
        );
    }

    /**
     * 設定選項按鈕列。
     */
    private void setupButtonBox() {
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setVisible(false);
        buttonBox.setOpacity(0);

        StackPane.setAlignment(buttonBox, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(
                buttonBox,
                new Insets(0, 170, 62, 0)
        );
    }


    // =========================================================
    // Line Display
    // =========================================================

    /**
     * 更新角色名稱框顯示。
     *
     * @param line 對話行
     */
    private void updateNameBox(DialogueLine line) {
        boolean hasCharacterName = hasCharacterName(line);

        nameBox.setVisible(hasCharacterName);
        nameBox.setManaged(hasCharacterName);

        if (hasCharacterName) {
            nameText.setText(text(line.getCharacterNameKey()));
        } else {
            nameText.setText("");
        }
    }

    /**
     * 播放對話框與名稱框進場動畫。
     */
    private void playLineEnterAnimation() {
        portraitView.setOpacity(1);

        dialogueBox.setOpacity(0);
        dialogueBox.setTranslateY(LINE_ENTER_OFFSET_Y);

        nameBox.setOpacity(0);
        nameBox.setTranslateY(LINE_ENTER_OFFSET_Y);

        FadeTransition boxFade = createFadeTransition(
                dialogueBox,
                LINE_ENTER_DURATION,
                0,
                1
        );

        TranslateTransition boxMove = createTranslateYTransition(
                dialogueBox,
                LINE_ENTER_DURATION,
                LINE_ENTER_OFFSET_Y,
                0
        );

        if (hasCharacterName(currentLine)) {
            FadeTransition nameFade = createFadeTransition(
                    nameBox,
                    LINE_ENTER_DURATION,
                    0,
                    1
            );

            TranslateTransition nameMove = createTranslateYTransition(
                    nameBox,
                    LINE_ENTER_DURATION,
                    LINE_ENTER_OFFSET_Y,
                    0
            );

            new ParallelTransition(
                    boxFade,
                    boxMove,
                    nameFade,
                    nameMove
            ).play();

            return;
        }

        nameBox.setOpacity(0);
        nameBox.setVisible(false);

        new ParallelTransition(
                boxFade,
                boxMove
        ).play();
    }


    // =========================================================
    // Typewriter
    // =========================================================

    /**
     * 開始打字機效果。
     *
     * @param line 對話行
     */
    private void startTypewriter(DialogueLine line) {
        fullText = text(line.getTextKey());
        currentCharIndex = 0;

        dialogueText.setText("");
        typing = true;

        startSpeakingPortraitEffect(line);

        typewriterTimeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(TYPEWRITER_INTERVAL_SECONDS),
                        event -> printNextCharacter()
                )
        );

        typewriterTimeline.setCycleCount(Animation.INDEFINITE);
        typewriterTimeline.play();
    }

    /**
     * 顯示下一個字。
     */
    private void printNextCharacter() {
        if (currentCharIndex >= fullText.length()) {
            finishTypewriter();
            return;
        }

        currentCharIndex++;

        dialogueText.setText(
                fullText.substring(0, currentCharIndex)
        );

        playTypingSoundIfNeeded();
    }

    /**
     * 結束打字機效果。
     */
    private void finishTypewriter() {
        stopTypewriter();

        dialogueText.setText(fullText);
        typing = false;

        stopSpeakingPortraitEffect();

        showButtonsIfNeeded();
    }

    /**
     * 跳過打字機，直接顯示全文。
     */
    private void skipTypewriter() {
        if (typing) {
            finishTypewriter();
        }
    }

    /**
     * 停止打字機 Timeline。
     */
    private void stopTypewriter() {
        if (typewriterTimeline != null) {
            typewriterTimeline.stop();
            typewriterTimeline = null;
        }
    }

    /**
     * 每兩個字播放一次對話嗶聲。
     */
    private void playTypingSoundIfNeeded() {
        if (currentCharIndex % 2 == 0) {
            audioSystem.playSFX(SoundId.DIALOG_BLIP);
        }
    }


    // =========================================================
    // Portrait Effects
    // =========================================================

    /**
     * 開始說話立繪效果。
     *
     * 有角色名稱時：
     * - 切成 speaking portrait。
     * - 稍微放大角色。
     *
     * 旁白時：
     * - 不切 speaking portrait。
     * - 不做放大效果。
     */
    private void startSpeakingPortraitEffect(DialogueLine line) {
        if (!hasCharacterName(line)) {
            portraitView.setScaleX(1.0);
            portraitView.setScaleY(1.0);
            return;
        }

        loadPortrait(line.getSpeakingPortraitPath());

        ScaleTransition speakScale = new ScaleTransition(
                Duration.seconds(PORTRAIT_SCALE_DURATION),
                portraitView
        );

        speakScale.setToX(PORTRAIT_SPEAK_SCALE);
        speakScale.setToY(PORTRAIT_SPEAK_SCALE);
        speakScale.setInterpolator(Interpolator.EASE_OUT);
        speakScale.play();
    }

    /**
     * 停止說話立繪效果，回到預設立繪。
     */
    private void stopSpeakingPortraitEffect() {
        if (!hasCharacterName(currentLine)) {
            return;
        }

        loadPortrait(currentLine.getDefaultPortraitPath());

        ScaleTransition scaleBack = new ScaleTransition(
                Duration.seconds(PORTRAIT_SCALE_DURATION),
                portraitView
        );

        scaleBack.setToX(1.0);
        scaleBack.setToY(1.0);
        scaleBack.setInterpolator(Interpolator.EASE_OUT);
        scaleBack.play();
    }


    // =========================================================
    // Dialogue Click Handling
    // =========================================================

    /**
     * 處理對話畫面點擊。
     */
    private void handleDialogueClick() {
        if (currentLine == null) {
            return;
        }

        if (typing) {
            skipTypewriter();
            return;
        }

        if (currentLine.hasButtons() && !currentLine.isAllowClickNext()) {
            return;
        }

        if (currentLine.isAllowClickNext()) {
            dialogueSystem.nextFrom(currentLine);
        }
    }

    /**
     * 判斷點擊目標是否在選項按鈕列內。
     *
     * @param node 點擊目標
     * @return true 表示點到按鈕列內
     */
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


    // =========================================================
    // Buttons
    // =========================================================

    /**
     * 清空並隱藏選項按鈕。
     */
    private void hideAndClearButtons() {
        buttonBox.getChildren().clear();
        buttonBox.setVisible(false);
        buttonBox.setOpacity(0);
    }

    /**
     * 若目前對話行有選項，顯示選項按鈕。
     */
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

        FadeTransition fade = createFadeTransition(
                buttonBox,
                BUTTON_FADE_DURATION,
                0,
                1
        );

        fade.play();
    }

    /**
     * 建立單一選項按鈕。
     *
     * @param buttonData 選項資料
     * @return 選項按鈕 Node
     */
    private StackPane createChoiceButton(DialogueButton buttonData) {
        Rectangle background = createChoiceButtonBackground();

        Text label = createChoiceButtonLabel(
                text(buttonData.getTextKey())
        );

        StackPane button = new StackPane(
                background,
                label
        );

        button.setPrefSize(
                CHOICE_BUTTON_WIDTH,
                CHOICE_BUTTON_HEIGHT
        );
        button.setMaxSize(
                CHOICE_BUTTON_WIDTH,
                CHOICE_BUTTON_HEIGHT
        );
        button.setPickOnBounds(true);

        setupChoiceButtonEvents(
                button,
                background,
                label,
                buttonData
        );

        return button;
    }

    /**
     * 建立選項按鈕背景。
     */
    private Rectangle createChoiceButtonBackground() {
        Rectangle background = new Rectangle(
                CHOICE_BUTTON_WIDTH,
                CHOICE_BUTTON_HEIGHT
        );

        background.setArcWidth(12);
        background.setArcHeight(12);
        background.setFill(Color.rgb(0, 0, 0, 0.78));
        background.setStroke(Color.rgb(255, 255, 255, 0.72));
        background.setStrokeWidth(1.4);

        return background;
    }

    /**
     * 建立選項按鈕文字。
     */
    private Text createChoiceButtonLabel(String labelText) {
        Text label = new Text(labelText);

        label.setStyle("""
                -fx-font-size: 20px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        return label;
    }

    /**
     * 設定選項按鈕事件。
     */
    private void setupChoiceButtonEvents(
            StackPane button,
            Rectangle background,
            Text label,
            DialogueButton buttonData
    ) {
        button.setOnMouseEntered(event -> {
            background.setFill(Color.rgb(213, 105, 16, 0.86));
            label.setFill(Color.BLACK);
            audioSystem.playSFX(SoundId.BUTTON_HOVER);
        });

        button.setOnMouseExited(event -> {
            background.setFill(Color.rgb(0, 0, 0, 0.78));
            label.setFill(Color.WHITE);
        });

        button.setOnMouseClicked(event -> {
            event.consume();

            if (typing) {
                skipTypewriter();
                return;
            }

            audioSystem.playSFX(SoundId.BUTTON_PRESSED);
            buttonData.run();
        });
    }


    // =========================================================
    // Portrait Loading
    // =========================================================

    /**
     * 載入角色立繪。
     *
     * @param path 圖片資源路徑
     */
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
        } catch (Exception exception) {
            System.out.println("Portrait load failed: " + path);
            exception.printStackTrace();
            portraitView.setImage(null);
        }
    }


    // =========================================================
    // Pattern / Style Helpers
    // =========================================================

    /**
     * 建立粉紅點點背景樣式。
     *
     * 用於對話框背景。
     *
     * @return ImagePattern
     */
    private ImagePattern createPinkDotPattern() {
        int size = 50;

        Canvas canvas = new Canvas(size, size);
        GraphicsContext graphics = canvas.getGraphicsContext2D();

        graphics.clearRect(0, 0, size, size);

        graphics.setFill(Color.rgb(253, 172, 203, 0.80));
        graphics.fillRect(0, 0, size, size);

        graphics.setFill(Color.rgb(234, 39, 130, 0.10));
        graphics.fillOval(0, 0, size * 0.4, size * 0.4);
        graphics.fillOval(size * 0.5, size * 0.5, size * 0.4, size * 0.4);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        WritableImage image = new WritableImage(size, size);
        canvas.snapshot(params, image);

        return new ImagePattern(
                image,
                0,
                0,
                size,
                size,
                false
        );
    }


    // =========================================================
    // Animation Helpers
    // =========================================================

    /**
     * 建立淡入淡出動畫。
     */
    private FadeTransition createFadeTransition(
            Node target,
            double seconds,
            double fromValue,
            double toValue
    ) {
        FadeTransition transition = new FadeTransition(
                Duration.seconds(seconds),
                target
        );

        transition.setFromValue(fromValue);
        transition.setToValue(toValue);

        return transition;
    }

    /**
     * 建立 Y 軸移動動畫。
     */
    private TranslateTransition createTranslateYTransition(
            Node target,
            double seconds,
            double fromY,
            double toY
    ) {
        TranslateTransition transition = new TranslateTransition(
                Duration.seconds(seconds),
                target
        );

        transition.setFromY(fromY);
        transition.setToY(toY);
        transition.setInterpolator(Interpolator.EASE_OUT);

        return transition;
    }


    // =========================================================
    // Language Helpers
    // =========================================================

    /**
     * 取得目前語言文字。
     *
     * @param key 語言 key
     * @return 翻譯後文字
     */
    private String text(String key) {
        return languageSystem.text(key);
    }

    /**
     * 判斷對話行是否有角色名稱。
     *
     * @param line 對話行
     * @return true 表示有角色名稱
     */
    private boolean hasCharacterName(DialogueLine line) {
        return line != null &&
                line.getCharacterNameKey() != null &&
                !line.getCharacterNameKey().isBlank();
    }
}