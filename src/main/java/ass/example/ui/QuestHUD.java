package ass.example.ui;

import ass.example.core.QuestType;
import ass.example.system.LanguageSystem;
import ass.example.system.quest.QuestState;
import ass.example.system.quest.QuestSystem;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QuestHUD extends Pane {

    private final QuestSystem questSystem = QuestSystem.getInstance();
    private final LanguageSystem languageSystem = LanguageSystem.getInstance();

    private final Map<QuestType, StackPane> questRows = new HashMap<>();

    private boolean playingCompletionAnimation = false;

    private static final double TOGGLE_WIDTH = 17;
    private static final double TOGGLE_HEIGHT = 32;

    private static final double TEXT_MIN_WIDTH = 120;
    private static final double TEXT_MAX_WIDTH = 310;

    private static final double ICON_SPACE_WIDTH = 16;

    private static final double ROW_HEIGHT = 58;
    private static final double LINE_HEIGHT = 3;
    private static final double ROW_GAP = 14;

    private static final double HUD_Y = 130;

    /*
     * 底線從畫面左側伸出，所以 row 的 layoutX 放 0。
     * 文字與按鈕的內容再用 padding 推進。
     */
    private static final double ROW_X = 0;

    private static final double CONTENT_LEFT_PADDING = 6;

    private static final double HOVER_ZONE_WIDTH = 72;
    private static final double HOVER_ZONE_HEIGHT = 150;

    /*
     * 收起按鈕的位置。
     * hidden：整個按鈕藏在左側外。
     * peek：整個按鈕滑進畫面內。
     */
    private static final double COLLAPSED_BUTTON_HIDDEN_X = -TOGGLE_WIDTH - 10;
    private static final double COLLAPSED_BUTTON_PEEK_X = 10;

    private static final double TEXT_WIDTH = 270;
    private static final double ROW_WIDTH = TOGGLE_WIDTH + TEXT_WIDTH + ICON_SPACE_WIDTH;

    private static final double HUD_X = 18;

    private static final double VISIBLE_X = HUD_X;

    /*
     * 收起後，整組 HUD 到畫面外。
     * 但因為按鈕在 row 最左側，所以 peek 時只讓按鈕露出。
     */
    private static final double HIDDEN_X = -ROW_WIDTH - 18;

    /*
     * 滑鼠靠近時，只讓 [>] 出現在畫面左緣附近。
     */
    private static final double PEEK_X = -TOGGLE_WIDTH + 8;

    private final HBox slideGroup = new HBox(8);
    private final VBox questList = new VBox(14);

    private final StackPane toggleButton = new StackPane();
    private final Text toggleText = new Text("<");
    private final Rectangle hoverZone = new Rectangle();

    private boolean collapsed = false;
    private boolean togglePeeked = false;

    private final StackPane collapsedToggleButton = new StackPane();

    private double currentRowWidth = 360;

    public QuestHUD() {
        setPrefSize(1280, 720);
        setMinSize(1280, 720);
        setMaxSize(1280, 720);
        setPickOnBounds(false);

        questList.setLayoutX(ROW_X);
        questList.setLayoutY(HUD_Y);
        questList.setTranslateX(0);
        questList.setAlignment(Pos.TOP_LEFT);
        questList.setMouseTransparent(false);

        setupHoverZone();
        setupCollapsedToggleButton();

        getChildren().addAll(
                questList,
                hoverZone,
                collapsedToggleButton
        );

        refresh();
    }

    private String text(String key) {
        return languageSystem.text(key);
    }

    private double measureQuestTextWidth(String content) {
        Text measure = new Text(content);
        measure.setFont(Font.font("System", FontWeight.BOLD, 18));
        return measure.getLayoutBounds().getWidth();
    }

    private double computeTextWidth(String content) {
        double measured = measureQuestTextWidth(content) + 24;

        if (measured < TEXT_MIN_WIDTH) {
            return TEXT_MIN_WIDTH;
        }

        if (measured > TEXT_MAX_WIDTH) {
            return TEXT_MAX_WIDTH;
        }

        return measured;
    }

    private double computeRowWidth(String questText) {
        return CONTENT_LEFT_PADDING
                + TOGGLE_WIDTH
                + 8
                + computeTextWidth(questText)
                + 8
                + ICON_SPACE_WIDTH;
    }

    private void setupHoverZone() {
        hoverZone.setWidth(HOVER_ZONE_WIDTH);
        hoverZone.setHeight(HOVER_ZONE_HEIGHT);
        hoverZone.setFill(Color.rgb(255, 255, 255, 0.01));

        hoverZone.setLayoutX(0);
        hoverZone.setLayoutY(HUD_Y - 16);
        hoverZone.setMouseTransparent(true);

        hoverZone.setOnMouseEntered(e -> {
            if (collapsed) {
                peekToggleButton();
            }
        });

        hoverZone.setOnMouseExited(e -> {
            PauseTransition wait = new PauseTransition(Duration.seconds(0.18));

            wait.setOnFinished(ev -> {
                if (collapsed
                        && !hoverZone.isHover()
                        && !collapsedToggleButton.isHover()) {
                    hidePeekButton();
                }
            });

            wait.play();
        });
    }

    private void moveQuestListTo(double targetX, Duration duration, Interpolator interpolator) {
        TranslateTransition move = new TranslateTransition(duration, questList);
        move.setToX(targetX);
        move.setInterpolator(interpolator);
        move.play();
    }

    private void moveCollapsedButtonTo(double targetX, Duration duration, Interpolator interpolator) {
        TranslateTransition move = new TranslateTransition(duration, collapsedToggleButton);
        move.setToX(targetX);
        move.setInterpolator(interpolator);
        move.play();
    }

    private void collapseHUD() {
        if (collapsed) {
            return;
        }

        collapsed = true;
        togglePeeked = false;

        double hiddenX = -currentRowWidth - 20;

        TranslateTransition move = new TranslateTransition(Duration.seconds(0.24), questList);
        move.setToX(hiddenX);
        move.setInterpolator(Interpolator.EASE_IN);

        move.setOnFinished(e -> {
            collapsedToggleButton.setVisible(true);
            collapsedToggleButton.setOpacity(1);
            collapsedToggleButton.setDisable(false);
            collapsedToggleButton.setMouseTransparent(false);
            collapsedToggleButton.setPickOnBounds(true);
            collapsedToggleButton.setTranslateX(COLLAPSED_BUTTON_HIDDEN_X);

            /*
             * hoverZone 只負責偵測靠近，不要蓋住按鈕。
             */
            hoverZone.setMouseTransparent(false);

            /*
             * 關鍵：
             * 先讓 hoverZone 啟用，再把按鈕拉到最上層。
             */
            hoverZone.toFront();
            collapsedToggleButton.toFront();
        });

        move.play();
    }

    private void expandHUD() {
        collapsed = false;
        togglePeeked = false;

        hoverZone.setMouseTransparent(true);

        FadeTransition hidePeek = new FadeTransition(Duration.seconds(0.08), collapsedToggleButton);
        hidePeek.setFromValue(1);
        hidePeek.setToValue(0);

        hidePeek.setOnFinished(e -> {
            collapsedToggleButton.setVisible(false);
            collapsedToggleButton.setOpacity(1);
            collapsedToggleButton.setTranslateX(COLLAPSED_BUTTON_HIDDEN_X);

            questList.toFront();
            moveQuestListTo(0, Duration.seconds(0.24), Interpolator.EASE_OUT);
        });

        hidePeek.play();
    }

    private void peekToggleButton() {
        if (!collapsed) {
            return;
        }

        togglePeeked = true;

        collapsedToggleButton.setVisible(true);
        collapsedToggleButton.setOpacity(1);
        collapsedToggleButton.setDisable(false);
        collapsedToggleButton.setMouseTransparent(false);
        collapsedToggleButton.setPickOnBounds(true);

        /*
         * 每次探出都拉到最上層。
         */
        collapsedToggleButton.toFront();

        moveCollapsedButtonTo(
                COLLAPSED_BUTTON_PEEK_X,
                Duration.seconds(0.16),
                Interpolator.EASE_OUT
        );
    }

    private void hidePeekButton() {
        if (!collapsed) {
            return;
        }

        togglePeeked = false;

        moveCollapsedButtonTo(
                COLLAPSED_BUTTON_HIDDEN_X,
                Duration.seconds(0.16),
                Interpolator.EASE_IN
        );
    }

    public void update() {
        if (playingCompletionAnimation) {
            return;
        }

        QuestType completedWaiting =
                questSystem.getNextCompletedQuestWaitingForAnimation();

        if (completedWaiting != null) {
            refresh();
            playQuestCompletedAnimation(completedWaiting);
            return;
        }

        refresh();
    }

    public void refresh() {
        questList.getChildren().clear();
        questRows.clear();

        currentRowWidth = 0;

        List<QuestType> visibleQuests = questSystem.getVisibleQuests();

        for (int i = 0; i < visibleQuests.size(); i++) {
            QuestType quest = visibleQuests.get(i);

            StackPane row = createQuestRow(quest, i == 0);

            questRows.put(quest, row);
            questList.getChildren().add(row);
        }
    }

    private double measureTextWidth(String content) {
        Text measure = new Text(content);

        // 要和任務文字的字型大小一致
        measure.setFont(Font.font("System", FontWeight.BOLD, 18));

        return measure.getLayoutBounds().getWidth();
    }

    private StackPane createQuestRow(QuestType quest, boolean showToggle) {
        QuestState state = questSystem.getState(quest);
        boolean completed = state != null && state.isCompleted();

        String displayText = createQuestDisplayText(quest);

        double textWidth = computeTextWidth(displayText);
        double rowWidth = computeRowWidth(displayText);

        currentRowWidth = Math.max(currentRowWidth, rowWidth);

        VBox rowRoot = new VBox(5);
        rowRoot.setAlignment(Pos.TOP_LEFT);
        rowRoot.setPrefSize(rowWidth, ROW_HEIGHT);
        rowRoot.setMaxSize(rowWidth, ROW_HEIGHT);

        HBox content = new HBox(8);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPrefWidth(rowWidth);
        content.setPadding(new Insets(0, 0, 0, CONTENT_LEFT_PADDING));

        Node toggle = showToggle
                ? createQuestToggleButton()
                : createTogglePlaceholder();

        Text title = new Text(displayText);
        title.setWrappingWidth(textWidth);
        title.setStyle("""
        -fx-font-size: 18px;
        -fx-fill: white;
        -fx-font-weight: bold;
        """);
        title.setEffect(new DropShadow(4, Color.BLACK));

        boolean showStaticCheck = completed && state != null && state.isCompletionAnimationPlayed();

        StackPane iconSpace = createQuestIconSpace(showStaticCheck);

        content.getChildren().addAll(
                toggle,
                title,
                iconSpace
        );

        /*
         * 底線不吃 padding，直接從畫面左側開始。
         */
        Rectangle underline = new Rectangle(rowWidth - 10, LINE_HEIGHT);
        underline.setArcWidth(LINE_HEIGHT);
        underline.setArcHeight(LINE_HEIGHT);
        underline.setFill(completed
                ? Color.rgb(90, 220, 120, 0.95)
                : Color.rgb(213, 105, 16, 0.95));

        rowRoot.getChildren().addAll(content, underline);

        StackPane row = new StackPane(rowRoot);
        row.setPrefSize(rowWidth, ROW_HEIGHT);
        row.setMaxSize(rowWidth, ROW_HEIGHT);
        row.setPickOnBounds(false);

        return row;
    }

    private Region createTogglePlaceholder() {
        Region region = new Region();
        region.setPrefSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        region.setMinSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        region.setMaxSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        return region;
    }

    private StackPane createQuestIconSpace(boolean completed) {
        StackPane iconSpace = new StackPane();
        iconSpace.setPrefSize(ICON_SPACE_WIDTH, TOGGLE_HEIGHT);
        iconSpace.setMinSize(ICON_SPACE_WIDTH, TOGGLE_HEIGHT);
        iconSpace.setMaxSize(ICON_SPACE_WIDTH, TOGGLE_HEIGHT);

        if (completed) {
            ImageView check = createCheckIcon();
            check.setFitWidth(24);
            check.setFitHeight(24);
            iconSpace.getChildren().add(check);
        }

        return iconSpace;
    }

    private void setupCollapsedToggleButton() {
        Rectangle bg = new Rectangle(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        bg.setArcWidth(9);
        bg.setArcHeight(9);
        bg.setFill(Color.rgb(0, 0, 0, 0.72));
        bg.setStroke(Color.rgb(255, 255, 255, 0.7));
        bg.setStrokeWidth(1.2);

        Text arrow = new Text(">");
        arrow.setStyle("""
        -fx-font-size: 22px;
        -fx-fill: white;
        -fx-font-weight: bold;
        """);
        arrow.setEffect(new DropShadow(4, Color.BLACK));

        collapsedToggleButton.getChildren().addAll(bg, arrow);
        collapsedToggleButton.setPrefSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        collapsedToggleButton.setMinSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        collapsedToggleButton.setMaxSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);

        collapsedToggleButton.setLayoutX(0);
        collapsedToggleButton.setLayoutY(HUD_Y + 6);
        collapsedToggleButton.setTranslateX(COLLAPSED_BUTTON_HIDDEN_X);

        collapsedToggleButton.setVisible(false);
        collapsedToggleButton.setManaged(false);
        collapsedToggleButton.setPickOnBounds(true);

        collapsedToggleButton.setOnMouseEntered(e -> {
            bg.setFill(Color.rgb(245, 135, 35, 0.95));
        });

        collapsedToggleButton.setOnMouseExited(e -> {
            bg.setFill(Color.rgb(0, 0, 0, 0.72));

            if (collapsed && togglePeeked && !hoverZone.isHover()) {
                hidePeekButton();
            }
        });

        collapsedToggleButton.setOnMouseClicked(e -> {
            e.consume();

            if (collapsed) {
                expandHUD();
            }
        });
    }

    private StackPane createQuestToggleButton() {
        Rectangle bg = new Rectangle(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        bg.setArcWidth(9);
        bg.setArcHeight(9);
        bg.setFill(Color.rgb(0, 0, 0, 0.62));
        bg.setStroke(Color.rgb(255, 255, 255, 0.68));
        bg.setStrokeWidth(1.2);

        Text arrow = new Text("<");
        arrow.setStyle("""
        -fx-font-size: 22px;
        -fx-fill: white;
        -fx-font-weight: bold;
        """);
        arrow.setEffect(new DropShadow(4, Color.BLACK));

        StackPane button = new StackPane(bg, arrow);
        button.setPrefSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        button.setMinSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        button.setMaxSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        button.setPickOnBounds(true);

        button.setOnMouseEntered(e -> {
            bg.setFill(Color.rgb(245, 135, 35, 0.95));
        });

        button.setOnMouseExited(e -> {
            bg.setFill(Color.rgb(0, 0, 0, 0.62));
        });

        button.setOnMousePressed(e -> {
            e.consume();

            System.out.println("PRESSED expandedToggleButton");

            if (!collapsed) {
                collapseHUD();
            }
        });

        return button;
    }

    private String createQuestDisplayText(QuestType quest) {
        QuestState state = questSystem.getState(quest);

        int amount = state == null ? 0 : state.getAmount();
        int required = quest.getRequiredAmount();

        return text(quest.getTitleKey()) + "  (" + amount + "/" + required + ")";
    }

    private void playQuestCompletedAnimation(QuestType quest) {
        StackPane row = questRows.get(quest);

        if (row == null) {
            refresh();
            row = questRows.get(quest);

            if (row == null) {
                questSystem.markCompletionAnimationPlayed(quest);
                refresh();
                return;
            }
        }

        playingCompletionAnimation = true;

        int rowIndex = questList.getChildren().indexOf(row);

        ImageView checkIcon = createCheckIcon();

        /*
         * 不要再根據文字長度手動硬算位置。
         * 直接放在 row 的右側，避免和文字卡在一起。
         */
        StackPane.setAlignment(checkIcon, Pos.CENTER_RIGHT);
        StackPane.setMargin(checkIcon, new Insets(0, 14, 18, 0));

        checkIcon.setOpacity(0);
        checkIcon.setScaleX(0.2);
        checkIcon.setScaleY(0.2);
        checkIcon.setMouseTransparent(true);

        /*
         * 關鍵：
         * 你原本少了這行，所以 checkIcon 沒有被加入畫面。
         */
        row.getChildren().add(checkIcon);
        checkIcon.toFront();

        FadeTransition checkFadeIn = new FadeTransition(Duration.seconds(0.14), checkIcon);
        checkFadeIn.setFromValue(0);
        checkFadeIn.setToValue(1);

        ScaleTransition checkPop = new ScaleTransition(Duration.seconds(0.18), checkIcon);
        checkPop.setFromX(0.2);
        checkPop.setFromY(0.2);
        checkPop.setToX(1.18);
        checkPop.setToY(1.18);
        checkPop.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition checkSettle = new ScaleTransition(Duration.seconds(0.10), checkIcon);
        checkSettle.setFromX(1.18);
        checkSettle.setFromY(1.18);
        checkSettle.setToX(1.0);
        checkSettle.setToY(1.0);
        checkSettle.setInterpolator(Interpolator.EASE_IN);

        PauseTransition stay = new PauseTransition(Duration.seconds(0.28));

        SequentialTransition sequence = new SequentialTransition(
                new ParallelTransition(checkFadeIn, checkPop),
                checkSettle,
                stay
        );

        StackPane finalRow = row;

        sequence.setOnFinished(e -> {
            /*
             * 避免 checkIcon 跟後面的 page replace 一起卡在 slot 裡。
             */
            finalRow.getChildren().remove(checkIcon);

            questSystem.markCompletionAnimationPlayed(quest);
            playPageReplaceAnimation(finalRow, rowIndex);
        });

        sequence.play();
    }

    private ImageView createCheckIcon() {
        Image image = new Image(Objects.requireNonNull(
                getClass().getResource("/assets/textures/ui/check.png")
        ).toExternalForm());

        ImageView icon = new ImageView(image);
        icon.setFitWidth(24);
        icon.setFitHeight(24);
        icon.setPreserveRatio(true);
        icon.setMouseTransparent(true);

        return icon;
    }

    private void playPageReplaceAnimation(StackPane oldRow, int rowIndex) {
        if (oldRow == null) {
            playingCompletionAnimation = false;
            refresh();
            return;
        }

        int safeIndex = questList.getChildren().indexOf(oldRow);

        if (safeIndex < 0) {
            safeIndex = rowIndex;
        }

        if (safeIndex < 0 || safeIndex >= questList.getChildren().size()) {
            playingCompletionAnimation = false;
            refresh();
            return;
        }

        StackPane newRow = createReplacementRow(safeIndex);

        double slotWidth = oldRow.getBoundsInParent().getWidth();

        if (slotWidth <= 0) {
            slotWidth = currentRowWidth;
        }

        if (newRow != null) {
            double newWidth = newRow.prefWidth(-1);
            slotWidth = Math.max(slotWidth, newWidth);
        }

        if (newRow == null) {
            TranslateTransition oldUp = new TranslateTransition(Duration.seconds(0.24), oldRow);
            oldUp.setFromY(0);
            oldUp.setToY(-ROW_HEIGHT - ROW_GAP);
            oldUp.setInterpolator(Interpolator.EASE_IN);

            FadeTransition oldFade = new FadeTransition(Duration.seconds(0.22), oldRow);
            oldFade.setFromValue(1);
            oldFade.setToValue(0);

            ParallelTransition outOnly = new ParallelTransition(oldUp, oldFade);

            outOnly.setOnFinished(e -> {
                playingCompletionAnimation = false;
                refresh();
            });

            outOnly.play();
            return;
        }

        StackPane pageSlot = new StackPane();
        pageSlot.setAlignment(Pos.TOP_LEFT);
        pageSlot.setPrefSize(slotWidth, ROW_HEIGHT);
        pageSlot.setMinSize(slotWidth, ROW_HEIGHT);
        pageSlot.setMaxSize(slotWidth, ROW_HEIGHT);
        pageSlot.setClip(new Rectangle(slotWidth, ROW_HEIGHT + 20));

        questList.getChildren().set(safeIndex, pageSlot);

        oldRow.setOpacity(1);
        oldRow.setTranslateX(0);
        oldRow.setTranslateY(0);

        newRow.setOpacity(0);
        newRow.setTranslateX(0);
        newRow.setTranslateY(ROW_HEIGHT + ROW_GAP);

        StackPane.setAlignment(oldRow, Pos.TOP_LEFT);
        StackPane.setAlignment(newRow, Pos.TOP_LEFT);

        pageSlot.getChildren().addAll(newRow, oldRow);

        TranslateTransition oldUp = new TranslateTransition(Duration.seconds(0.28), oldRow);
        oldUp.setFromY(0);
        oldUp.setToY(-ROW_HEIGHT - ROW_GAP);
        oldUp.setInterpolator(Interpolator.EASE_IN);

        FadeTransition oldFade = new FadeTransition(Duration.seconds(0.22), oldRow);
        oldFade.setFromValue(1);
        oldFade.setToValue(0);

        TranslateTransition newUp = new TranslateTransition(Duration.seconds(0.30), newRow);
        newUp.setFromY(ROW_HEIGHT + ROW_GAP);
        newUp.setToY(0);
        newUp.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition newFade = new FadeTransition(Duration.seconds(0.22), newRow);
        newFade.setFromValue(0);
        newFade.setToValue(1);

        ParallelTransition pageFlip = new ParallelTransition(
                oldUp,
                oldFade,
                newUp,
                newFade
        );

        pageFlip.setOnFinished(e -> {
            playingCompletionAnimation = false;
            refresh();

            /*
             * 如果 HUD 目前是收起狀態，refresh 後仍保持收起。
             */
            if (collapsed) {
                questList.setTranslateX(-currentRowWidth - 20);
            }
        });

        pageFlip.play();
    }

    private StackPane createReplacementRow(int rowIndex) {
        List<QuestType> visibleQuests = questSystem.getVisibleQuests();

        if (rowIndex < 0 || rowIndex >= visibleQuests.size()) {
            return null;
        }

        QuestType quest = visibleQuests.get(rowIndex);

        /*
         * replace 後仍然只有第一列需要 toggle。
         */
        return createQuestRow(quest, rowIndex == 0);
    }
}