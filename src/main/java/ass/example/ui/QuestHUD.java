package ass.example.ui;

import ass.example.core.QuestType;
import ass.example.system.LanguageSystem;
import ass.example.system.QuestSystem;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * QuestHUD
 *
 * 任務提示 HUD。
 *
 * 功能：
 * 1. 顯示目前可見任務。
 * 2. 支援任務完成勾勾動畫。
 * 3. 任務完成後可播放舊任務往上、新任務補位的 replace 動畫。
 * 4. 支援 HUD 收合到畫面左側。
 * 5. 收合後滑鼠靠近左側 hoverZone 時，只讓展開按鈕探出。
 *
 * 單例判斷：
 * QuestHUD 不建議做 Singleton。
 *
 * 原因：
 * - QuestHUD 是 JavaFX Node。
 * - Node 會被加入 / 移除不同 Scene 或 UI Layer。
 * - 做成 Singleton 容易造成舊 UI 殘留、父節點衝突、動畫狀態未清乾淨。
 *
 * 適合 Singleton 的是：
 * - QuestSystem
 * - LanguageSystem
 */
public class QuestHUD extends Pane {

    // =========================================================
    // System References
    // =========================================================

    private final QuestSystem questSystem =
            QuestSystem.getInstance();

    private final LanguageSystem languageSystem =
            LanguageSystem.getInstance();


    // =========================================================
    // Layout Constants
    // =========================================================

    /**
     * 展開 / 收合按鈕尺寸。
     */
    private static final double TOGGLE_WIDTH = 17;
    private static final double TOGGLE_HEIGHT = 32;

    /**
     * 任務文字寬度限制。
     *
     * 文字會依照實際內容自動計算寬度，
     * 但不會小於 TEXT_MIN_WIDTH，也不會大於 TEXT_MAX_WIDTH。
     */
    private static final double TEXT_MIN_WIDTH = 120;
    private static final double TEXT_MAX_WIDTH = 310;

    /**
     * 勾勾 icon 預留空間。
     *
     * 數值小一點可以讓 icon 更靠近文字。
     */
    private static final double ICON_SPACE_WIDTH = 16;

    /**
     * 單列任務高度與底線高度。
     */
    private static final double ROW_HEIGHT = 58;
    private static final double ROW_GAP = 14;
    private static final double LINE_HEIGHT = 3;

    /**
     * HUD 起始位置。
     *
     * questList 的 layoutX 固定 0，
     * 因為底線希望從畫面左側延伸出來。
     */
    private static final double ROW_X = 0;
    private static final double HUD_Y = 130;

    /**
     * 任務內容左側 padding。
     *
     * 只推進按鈕與文字，不推進底線。
     */
    private static final double CONTENT_LEFT_PADDING = 6;

    /**
     * 收合後左側感應區。
     *
     * 注意：
     * hoverZone 只在 HUD 收合後才打開 mouseTransparent = false。
     */
    private static final double HOVER_ZONE_WIDTH = 25;
    private static final double HOVER_ZONE_HEIGHT = 50;

    /**
     * 收合按鈕的位置。
     *
     * HIDDEN：完全藏到畫面左側外。
     * PEEK：滑進畫面內，讓玩家可以點擊展開。
     */
    private static final double COLLAPSED_BUTTON_HIDDEN_X = -TOGGLE_WIDTH - 10;
    private static final double COLLAPSED_BUTTON_PEEK_X = 10;


    // =========================================================
    // UI Nodes
    // =========================================================

    /**
     * 任務列表本體。
     *
     * 所有任務 row 都放在這個 VBox 裡。
     */
    private final VBox questList = new VBox(ROW_GAP);

    /**
     * HUD 收合後，滑鼠靠近左側時用來偵測的透明區域。
     */
    private final Rectangle hoverZone = new Rectangle();

    /**
     * HUD 收合後顯示的獨立展開按鈕。
     *
     * 這顆按鈕和任務列中的 "<" 是分開的，
     * 避免整組 questList 滑出畫面後按鈕也無法點擊。
     */
    private final StackPane collapsedToggleButton = new StackPane();


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * 每個 QuestType 對應目前畫面上的 row。
     *
     * 任務完成動畫會靠它找出要播放動畫的那一列。
     */
    private final Map<QuestType, StackPane> questRows = new HashMap<>();

    /**
     * 是否正在播放任務完成動畫。
     *
     * 播放期間 update() 不會重複 refresh，
     * 避免動畫中的 row 被刷新掉。
     */
    private boolean playingCompletionAnimation = false;

    /**
     * HUD 是否已收合。
     */
    private boolean collapsed = false;

    /**
     * 收合按鈕是否正在探出。
     */
    private boolean togglePeeked = false;

    /**
     * 目前可見任務列的最大寬度。
     *
     * 用於：
     * - 計算收合時 questList 要滑出多遠。
     * - replace 動畫 pageSlot 寬度 fallback。
     */
    private double currentRowWidth = 360;


    // =========================================================
    // Constructor
    // =========================================================

    public QuestHUD() {
        setupRoot();
        setupQuestList();
        setupHoverZone();
        setupCollapsedToggleButton();

        getChildren().addAll(
                questList,
                hoverZone,
                collapsedToggleButton
        );

        refresh();
    }


    // =========================================================
    // Basic Helpers
    // =========================================================

    private String text(String key) {
        return languageSystem.text(key);
    }

    private QuestSystem.QuestState getQuestState(QuestType quest) {
        return questSystem.getState(quest);
    }

    private boolean isQuestCompleted(QuestType quest) {
        QuestSystem.QuestState state = getQuestState(quest);

        return state != null && state.isCompleted();
    }

    private boolean isCompletionAnimationPlayed(QuestType quest) {
        QuestSystem.QuestState state = getQuestState(quest);

        return state != null && state.isCompletionAnimationPlayed();
    }


    // =========================================================
    // Setup
    // =========================================================

    /**
     * 設定整個 HUD Pane。
     */
    private void setupRoot() {
        setPrefSize(1280, 720);
        setMinSize(1280, 720);
        setMaxSize(1280, 720);

        /*
         * 讓 HUD 本身不阻擋其他遊戲輸入。
         * 真正需要滑鼠互動的按鈕會自己 setPickOnBounds(true)。
         */
        setPickOnBounds(false);
    }

    /**
     * 設定任務列表。
     */
    private void setupQuestList() {
        questList.setLayoutX(ROW_X);
        questList.setLayoutY(HUD_Y);
        questList.setTranslateX(0);
        questList.setAlignment(Pos.TOP_LEFT);
        questList.setMouseTransparent(false);
    }

    /**
     * 設定左側 hoverZone。
     *
     * hoverZone 在 HUD 展開時不接收滑鼠事件；
     * HUD 收合後才啟用，用來讓玩家滑鼠靠近左側時喚出展開按鈕。
     */
    private void setupHoverZone() {
        hoverZone.setWidth(HOVER_ZONE_WIDTH);
        hoverZone.setHeight(HOVER_ZONE_HEIGHT);
        hoverZone.setFill(Color.rgb(255, 255, 255, 0.01));

        hoverZone.setLayoutX(0);
        hoverZone.setLayoutY(HUD_Y - 30);

        /*
         * 初始 HUD 是展開狀態，所以 hoverZone 不需要接收滑鼠。
         */
        hoverZone.setMouseTransparent(true);

        hoverZone.setOnMouseEntered(e -> {
            if (collapsed) {
                peekCollapsedToggleButton();
            }
        });

        hoverZone.setOnMouseExited(e -> {
            PauseTransition wait = new PauseTransition(Duration.seconds(0.18));

            wait.setOnFinished(event -> {
                if (collapsed
                        && !hoverZone.isHover()
                        && !collapsedToggleButton.isHover()) {
                    hideCollapsedToggleButton();
                }
            });

            wait.play();
        });
    }

    /**
     * 設定 HUD 收合後的獨立展開按鈕。
     */
    private void setupCollapsedToggleButton() {
        Rectangle background = new Rectangle(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        background.setArcWidth(9);
        background.setArcHeight(9);
        background.setFill(Color.rgb(0, 0, 0, 0.72));
        background.setStroke(Color.rgb(255, 255, 255, 0.7));
        background.setStrokeWidth(1.2);

        Text arrow = new Text(">");
        arrow.setStyle("""
        -fx-font-size: 22px;
        -fx-fill: white;
        -fx-font-weight: bold;
        """);
        arrow.setEffect(new DropShadow(4, Color.BLACK));

        collapsedToggleButton.getChildren().addAll(background, arrow);
        collapsedToggleButton.setPrefSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        collapsedToggleButton.setMinSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        collapsedToggleButton.setMaxSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);

        collapsedToggleButton.setLayoutX(0);
        collapsedToggleButton.setLayoutY(HUD_Y + 16);
        collapsedToggleButton.setTranslateX(COLLAPSED_BUTTON_HIDDEN_X);

        collapsedToggleButton.setVisible(false);
        collapsedToggleButton.setManaged(false);
        collapsedToggleButton.setPickOnBounds(true);
        collapsedToggleButton.setMouseTransparent(false);

        collapsedToggleButton.setOnMouseEntered(e ->
                background.setFill(Color.rgb(245, 135, 35, 0.95))
        );

        collapsedToggleButton.setOnMouseExited(e -> {
            background.setFill(Color.rgb(0, 0, 0, 0.72));

            if (collapsed && togglePeeked && !hoverZone.isHover()) {
                hideCollapsedToggleButton();
            }
        });

        collapsedToggleButton.setOnMouseClicked(e -> {
            e.consume();

            if (collapsed) {
                expandHUD();
            }
        });
    }


    // =========================================================
    // Public Update / Refresh
    // =========================================================

    /**
     * 每幀由場景呼叫。
     *
     * 功能：
     * 1. 若正在播放完成動畫，避免重複刷新。
     * 2. 若有完成但還沒播動畫的任務，播放完成動畫。
     * 3. 否則正常刷新目前可見任務。
     */
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

    /**
     * 重新建立目前可見任務列。
     */
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

        /*
         * 如果 refresh 發生時 HUD 是收合狀態，
         * 保持它在畫面左側外。
         */
        if (collapsed) {
            questList.setTranslateX(getHiddenQuestListX());
        }
    }


    // =========================================================
    // HUD Collapse / Expand
    // =========================================================

    /**
     * 收合 HUD。
     *
     * questList 整組滑出畫面左側，
     * 動畫結束後啟用 collapsedToggleButton。
     */
    private void collapseHUD() {
        if (collapsed) {
            return;
        }

        collapsed = true;
        togglePeeked = false;

        TranslateTransition move = new TranslateTransition(Duration.seconds(0.24), questList);
        move.setToX(getHiddenQuestListX());
        move.setInterpolator(Interpolator.EASE_IN);

        move.setOnFinished(e -> {
            collapsedToggleButton.setVisible(true);
            collapsedToggleButton.setOpacity(1);
            collapsedToggleButton.setDisable(false);
            collapsedToggleButton.setMouseTransparent(false);
            collapsedToggleButton.setPickOnBounds(true);
            collapsedToggleButton.setTranslateX(COLLAPSED_BUTTON_HIDDEN_X);

            /*
             * 收合後才啟用 hoverZone。
             */
            hoverZone.setMouseTransparent(false);

            /*
             * 先把 hoverZone 拉前，再把按鈕拉到最前。
             * 這樣 hoverZone 能偵測滑鼠靠近，按鈕也不會被擋住。
             */
            hoverZone.toFront();
            collapsedToggleButton.toFront();
        });

        move.play();
    }

    /**
     * 展開 HUD。
     *
     * 先淡出探出的按鈕，再把 questList 拉回原位。
     */
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

            moveNodeX(
                    questList,
                    0,
                    Duration.seconds(0.24),
                    Interpolator.EASE_OUT
            );
        });

        hidePeek.play();
    }

    /**
     * 收合時，滑鼠靠近左側，讓展開按鈕探出。
     */
    private void peekCollapsedToggleButton() {
        if (!collapsed) {
            return;
        }

        togglePeeked = true;

        collapsedToggleButton.setVisible(true);
        collapsedToggleButton.setOpacity(1);
        collapsedToggleButton.setDisable(false);
        collapsedToggleButton.setMouseTransparent(false);
        collapsedToggleButton.setPickOnBounds(true);
        collapsedToggleButton.toFront();

        moveNodeX(
                collapsedToggleButton,
                COLLAPSED_BUTTON_PEEK_X,
                Duration.seconds(0.16),
                Interpolator.EASE_OUT
        );
    }

    /**
     * 收合時，滑鼠離開左側區域，讓展開按鈕藏回去。
     */
    private void hideCollapsedToggleButton() {
        if (!collapsed) {
            return;
        }

        togglePeeked = false;

        moveNodeX(
                collapsedToggleButton,
                COLLAPSED_BUTTON_HIDDEN_X,
                Duration.seconds(0.16),
                Interpolator.EASE_IN
        );
    }

    /**
     * 根據目前 row 最大寬度計算 questList 收合後的 X。
     */
    private double getHiddenQuestListX() {
        return -currentRowWidth - 20;
    }

    private void moveNodeX(
            Node node,
            double targetX,
            Duration duration,
            Interpolator interpolator
    ) {
        TranslateTransition move = new TranslateTransition(duration, node);
        move.setToX(targetX);
        move.setInterpolator(interpolator);
        move.play();
    }


    // =========================================================
    // Quest Row Creation
    // =========================================================

    /**
     * 建立單列任務。
     *
     * @param quest 任務類型
     * @param showToggle 是否顯示收合按鈕。只有第一列顯示。
     */
    private StackPane createQuestRow(QuestType quest, boolean showToggle) {
        boolean completed = isQuestCompleted(quest);
        boolean animationPlayed = isCompletionAnimationPlayed(quest);

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
                ? createExpandedToggleButton()
                : createTogglePlaceholder();

        Text title = createQuestTitleText(displayText, textWidth);

        StackPane iconSpace = createQuestIconSpace(completed && animationPlayed);

        content.getChildren().addAll(
                toggle,
                title,
                iconSpace
        );

        Rectangle underline = createUnderline(rowWidth, completed);

        rowRoot.getChildren().addAll(
                content,
                underline
        );

        StackPane row = new StackPane(rowRoot);
        row.setPrefSize(rowWidth, ROW_HEIGHT);
        row.setMaxSize(rowWidth, ROW_HEIGHT);
        row.setPickOnBounds(false);

        return row;
    }

    private Text createQuestTitleText(String displayText, double textWidth) {
        Text title = new Text(displayText);

        title.setWrappingWidth(textWidth);
        title.setStyle("""
        -fx-font-size: 18px;
        -fx-fill: white;
        -fx-font-weight: bold;
        """);
        title.setEffect(new DropShadow(4, Color.BLACK));

        return title;
    }

    private Rectangle createUnderline(double rowWidth, boolean completed) {
        Rectangle underline = new Rectangle(rowWidth - 10, LINE_HEIGHT);

        underline.setArcWidth(LINE_HEIGHT);
        underline.setArcHeight(LINE_HEIGHT);
        underline.setFill(completed
                ? Color.rgb(90, 220, 120, 0.95)
                : Color.rgb(213, 105, 16, 0.95));

        return underline;
    }

    private Region createTogglePlaceholder() {
        Region region = new Region();

        region.setPrefSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        region.setMinSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        region.setMaxSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);

        return region;
    }

    /**
     * 建立展開狀態下，第一列任務左側的 "<" 收合按鈕。
     */
    private StackPane createExpandedToggleButton() {
        Rectangle background = new Rectangle(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        background.setArcWidth(9);
        background.setArcHeight(9);
        background.setFill(Color.rgb(0, 0, 0, 0.62));
        background.setStroke(Color.rgb(255, 255, 255, 0.68));
        background.setStrokeWidth(1.2);

        Text arrow = new Text("<");
        arrow.setStyle("""
        -fx-font-size: 22px;
        -fx-fill: white;
        -fx-font-weight: bold;
        """);
        arrow.setEffect(new DropShadow(4, Color.BLACK));

        StackPane button = new StackPane(background, arrow);
        button.setPrefSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        button.setMinSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        button.setMaxSize(TOGGLE_WIDTH, TOGGLE_HEIGHT);
        button.setPickOnBounds(true);

        button.setOnMouseEntered(e ->
                background.setFill(Color.rgb(245, 135, 35, 0.95))
        );

        button.setOnMouseExited(e ->
                background.setFill(Color.rgb(0, 0, 0, 0.62))
        );

        /*
         * 這裡保留用 MousePressed。
         * 之前你測試過 pressed 比 clicked 更穩定。
         */
        button.setOnMousePressed(e -> {
            e.consume();

            if (!collapsed) {
                collapseHUD();
            }
        });

        return button;
    }

    /**
     * 建立任務右側 icon 預留區。
     *
     * 若任務已完成且動畫已播放，會顯示靜態 check icon。
     */
    private StackPane createQuestIconSpace(boolean showCheckIcon) {
        StackPane iconSpace = new StackPane();

        iconSpace.setPrefSize(ICON_SPACE_WIDTH, TOGGLE_HEIGHT);
        iconSpace.setMinSize(ICON_SPACE_WIDTH, TOGGLE_HEIGHT);
        iconSpace.setMaxSize(ICON_SPACE_WIDTH, TOGGLE_HEIGHT);

        if (showCheckIcon) {
            ImageView check = createCheckIcon();
            check.setFitWidth(24);
            check.setFitHeight(24);
            iconSpace.getChildren().add(check);
        }

        return iconSpace;
    }

    private String createQuestDisplayText(QuestType quest) {
        QuestSystem.QuestState state = getQuestState(quest);

        int amount = state == null
                ? 0
                : state.getAmount();

        int required = quest.getRequiredAmount();

        return text(quest.getTitleKey()) + "  (" + amount + "/" + required + ")";
    }


    // =========================================================
    // Text / Width Calculation
    // =========================================================

    private double measureTextWidth(String content) {
        Text measure = new Text(content);
        measure.setFont(Font.font("System", FontWeight.BOLD, 18));

        return measure.getLayoutBounds().getWidth();
    }

    private double computeTextWidth(String content) {
        double measured = measureTextWidth(content) + 24;

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


    // =========================================================
    // Quest Completion Animation
    // =========================================================

    /**
     * 播放任務完成勾勾動畫。
     *
     * 流程：
     * 1. 找到完成的任務 row。
     * 2. 在 row 右側加入放大的 check icon。
     * 3. 動畫結束後標記 completionAnimationPlayed。
     * 4. 播放 page replace 動畫。
     */
    private void playQuestCompletedAnimation(QuestType quest) {
        StackPane row = findOrRefreshQuestRow(quest);

        if (row == null) {
            questSystem.markCompletionAnimationPlayed(quest);
            refresh();
            return;
        }

        playingCompletionAnimation = true;

        int rowIndex = questList.getChildren().indexOf(row);

        ImageView checkIcon = createAnimatedCheckIcon();

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
             * 避免 checkIcon 和 replace 動畫互相卡住。
             */
            finalRow.getChildren().remove(checkIcon);

            questSystem.markCompletionAnimationPlayed(quest);

            playPageReplaceAnimation(finalRow, rowIndex);
        });

        sequence.play();
    }

    private StackPane findOrRefreshQuestRow(QuestType quest) {
        StackPane row = questRows.get(quest);

        if (row != null) {
            return row;
        }

        refresh();

        return questRows.get(quest);
    }

    /**
     * 建立完成動畫用的 check icon。
     *
     * 這個 icon 會直接疊在 row 的右側，
     * 不使用 iconSpace，避免跟文字卡在一起。
     */
    private ImageView createAnimatedCheckIcon() {
        ImageView checkIcon = createCheckIcon();

        StackPane.setAlignment(checkIcon, Pos.CENTER_RIGHT);
        StackPane.setMargin(checkIcon, new Insets(0, 14, 18, 0));

        checkIcon.setOpacity(0);
        checkIcon.setScaleX(0.2);
        checkIcon.setScaleY(0.2);
        checkIcon.setMouseTransparent(true);

        return checkIcon;
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


    // =========================================================
    // Quest Page Replace Animation
    // =========================================================

    /**
     * 任務完成後，把舊任務往上滑出，新任務從下方補進來。
     */
    private void playPageReplaceAnimation(StackPane oldRow, int rowIndex) {
        if (oldRow == null) {
            finishCompletionAnimationAndRefresh();
            return;
        }

        int safeIndex = resolveSafeRowIndex(oldRow, rowIndex);

        if (safeIndex < 0 || safeIndex >= questList.getChildren().size()) {
            finishCompletionAnimationAndRefresh();
            return;
        }

        StackPane newRow = createReplacementRow(safeIndex);

        if (newRow == null) {
            playOldRowOutOnlyAnimation(oldRow);
            return;
        }

        StackPane pageSlot = createPageSlot(oldRow, newRow);

        questList.getChildren().set(safeIndex, pageSlot);

        prepareRowsForPageReplace(oldRow, newRow);

        pageSlot.getChildren().addAll(newRow, oldRow);

        ParallelTransition pageFlip = createPageFlipAnimation(oldRow, newRow);

        pageFlip.setOnFinished(e -> finishCompletionAnimationAndRefresh());

        pageFlip.play();
    }

    private int resolveSafeRowIndex(StackPane oldRow, int fallbackIndex) {
        int index = questList.getChildren().indexOf(oldRow);

        if (index >= 0) {
            return index;
        }

        return fallbackIndex;
    }

    private StackPane createPageSlot(StackPane oldRow, StackPane newRow) {
        double slotWidth = oldRow.getBoundsInParent().getWidth();

        if (slotWidth <= 0) {
            slotWidth = currentRowWidth;
        }

        double newWidth = newRow.prefWidth(-1);
        slotWidth = Math.max(slotWidth, newWidth);

        StackPane pageSlot = new StackPane();
        pageSlot.setAlignment(Pos.TOP_LEFT);
        pageSlot.setPrefSize(slotWidth, ROW_HEIGHT);
        pageSlot.setMinSize(slotWidth, ROW_HEIGHT);
        pageSlot.setMaxSize(slotWidth, ROW_HEIGHT);

        /*
         * clip 高度多 20，讓上下滑動時不會太早被裁掉。
         */
        pageSlot.setClip(new Rectangle(slotWidth, ROW_HEIGHT + 20));

        return pageSlot;
    }

    private void prepareRowsForPageReplace(StackPane oldRow, StackPane newRow) {
        oldRow.setOpacity(1);
        oldRow.setTranslateX(0);
        oldRow.setTranslateY(0);

        newRow.setOpacity(0);
        newRow.setTranslateX(0);
        newRow.setTranslateY(ROW_HEIGHT + ROW_GAP);

        StackPane.setAlignment(oldRow, Pos.TOP_LEFT);
        StackPane.setAlignment(newRow, Pos.TOP_LEFT);
    }

    private ParallelTransition createPageFlipAnimation(StackPane oldRow, StackPane newRow) {
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

        return new ParallelTransition(
                oldUp,
                oldFade,
                newUp,
                newFade
        );
    }

    /**
     * 若沒有下一列可補位，只播放舊列往上消失。
     */
    private void playOldRowOutOnlyAnimation(StackPane oldRow) {
        TranslateTransition oldUp = new TranslateTransition(Duration.seconds(0.24), oldRow);
        oldUp.setFromY(0);
        oldUp.setToY(-ROW_HEIGHT - ROW_GAP);
        oldUp.setInterpolator(Interpolator.EASE_IN);

        FadeTransition oldFade = new FadeTransition(Duration.seconds(0.22), oldRow);
        oldFade.setFromValue(1);
        oldFade.setToValue(0);

        ParallelTransition outOnly = new ParallelTransition(
                oldUp,
                oldFade
        );

        outOnly.setOnFinished(e -> finishCompletionAnimationAndRefresh());

        outOnly.play();
    }

    /**
     * 建立 replace 動畫中要補進來的新任務列。
     */
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

    private void finishCompletionAnimationAndRefresh() {
        playingCompletionAnimation = false;

        refresh();

        /*
         * refresh 會重建 row。
         * 若 HUD 原本是收合狀態，要保持在收合位置。
         */
        if (collapsed) {
            questList.setTranslateX(getHiddenQuestListX());
        }
    }
}