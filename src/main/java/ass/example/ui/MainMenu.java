package ass.example.ui;

import ass.example.Main;
import ass.example.core.Language;
import ass.example.core.DeathReason;
import ass.example.core.SceneType;
import ass.example.core.SoundId;
import ass.example.core.WindowMode;
import ass.example.scenes.system.SceneManager;
import ass.example.system.*;
import ass.example.ui.save.SaveMenuMode;
import ass.example.ui.save.SaveSlotPanel;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static com.almasb.fxgl.dsl.FXGLForKtKt.getGameController;

/**
 * MainMenu
 *
 * 遊戲主選單。
 *
 * 功能：
 * 1. 顯示主畫面背景、Logo、主選單按鈕。
 * 2. 播放主選單進場動畫。
 * 3. 提供故事模式、迷你遊戲、成就、設定、離開遊戲等入口。
 * 4. 管理主選單內的子頁切換。
 * 5. 管理音量、視窗、語言、開發者模式、重置資料等設定頁。
 * 6. 管理成就列表展示。
 *
 * 單例判斷：
 * MainMenu 不適合做成單例。
 *
 * 原因：
 * - 它繼承 FXGLMenu，生命週期由 FXGL 管理。
 * - 它是 UI 畫面本身，不是純資料或全域系統。
 * - 它持有 pageLayer、selectedSettingsButton、expandedAchievementCell 等 UI 狀態。
 * - 若做成單例，切換 Scene 或重新建立 Menu 時容易殘留舊 UI 狀態。
 */
public class MainMenu extends FXGLMenu {

    // =========================================================
    // Layout Constants
    // =========================================================

    private static final double SCREEN_WIDTH = 1280.0;
    private static final double SCREEN_HEIGHT = 720.0;

    private static final double MAIN_BUTTON_WIDTH = 280.0;
    private static final double MAIN_BUTTON_HEIGHT = 58.0;

    private static final double SUB_BUTTON_WIDTH = 240.0;
    private static final double SUB_BUTTON_HEIGHT = 46.0;

    private static final double SETTINGS_SIDE_BUTTON_WIDTH = 240.0;
    private static final double SETTINGS_SIDE_BUTTON_HEIGHT = 46.0;


    // =========================================================
    // Assets
    // =========================================================

    private static final String MAIN_MENU_BG_PATH = "/assets/textures/ui/mainmenu/titlescreen_bg.png";

    private static final String MAIN_MENU_LOGO_PATH = "/assets/textures/ui/mainmenu/titlescreen_logo.png";

    private static final String MAIN_MENU_BGM_PATH = "/assets/music/mainmenu/Happy Wheels Theme.mp3";


    // =========================================================
    // Dependencies
    // =========================================================

    private final AchievementSystem achievementSystem =
            AchievementSystem.getInstance();

    private final WindowSystem windowSystem =
            WindowSystem.getInstance();

    private final LanguageSystem languageSystem =
            LanguageSystem.getInstance();

    private final MusicSystem musicSystem =
            MusicSystem.getInstance();

    private final AudioSystem audioSystem =
            AudioSystem.getInstance();


    // =========================================================
    // Root UI
    // =========================================================

    private final StackPane root = new StackPane();

    private ImageView backgroundView;
    private ImageView logoView;

    private VBox mainButtonBox;
    private StackPane pageLayer;
    private Rectangle darkOverlay;

    private Label devModeLabel;


    // =========================================================
    // Page State
    // =========================================================

    private StackPane selectedSettingsButton;
    private StackPane expandedAchievementCell;

    private boolean firstCreate = true;
    private boolean cutscene = true;


    // =========================================================
    // Cached Images
    // =========================================================

    private Image volumeIcon;
    private Image volumeDownIcon;
    private Image volumeMuteIcon;


    // =========================================================
    // Constructor / FXGL Lifecycle
    // =========================================================

    public MainMenu() {
        super(MenuType.MAIN_MENU);

        setupRoot();
        createBackground();
        createDarkOverlay();
        createPageLayer();
        createLogo();
        createMainButtons();
        createDeveloperLabel();

        getContentRoot().getChildren().add(root);

        CursorManager.install(getContentRoot());

        resetToMainMenuFirst();
    }

    /**
     * FXGL Menu 建立時呼叫。
     */
    @Override
    public void onCreate() {
        CursorManager.install(getContentRoot());

        windowSystem.installResizeListener();
        windowSystem.applySavedSettings();

        musicSystem.stopBGM();

        if (firstCreate) {
            firstCreate = false;
            resetToMainMenuFirst();
            playIntroAnimation();
        } else {
            resetToMainMenuSecondary();
        }

        musicSystem.playBGMIntroThenLoop(
                MAIN_MENU_BGM_PATH,
                1.0,
                20.5
        );
    }

    /**
     * FXGL Menu 銷毀時呼叫。
     */
    @Override
    public void onDestroy() {
        musicSystem.stopBGM();
    }


    // =========================================================
    // Basic Setup
    // =========================================================

    private void setupRoot() {
        root.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        root.setMinSize(0, 0);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    /**
     * 建立主選單背景。
     */
    private void createBackground() {
        backgroundView = loadImageView(
                MAIN_MENU_BG_PATH,
                SCREEN_WIDTH,
                SCREEN_HEIGHT
        );

        backgroundView.setOpacity(0);
        backgroundView.setPreserveRatio(false);

        backgroundView.fitWidthProperty().bind(root.widthProperty());
        backgroundView.fitHeightProperty().bind(root.heightProperty());

        root.getChildren().add(backgroundView);
    }

    /**
     * 建立黑色半透明遮罩。
     *
     * 用於打開子頁時讓主畫面變暗。
     */
    private void createDarkOverlay() {
        darkOverlay = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);
        darkOverlay.setFill(Color.rgb(0, 0, 0, 0.5));
        darkOverlay.setOpacity(0);
        darkOverlay.setMouseTransparent(true);

        root.getChildren().add(darkOverlay);
    }

    /**
     * 建立子頁圖層。
     */
    private void createPageLayer() {
        pageLayer = new StackPane();
        pageLayer.setVisible(false);
        pageLayer.setPickOnBounds(false);

        root.getChildren().add(pageLayer);
    }

    /**
     * 建立 Logo。
     */
    private void createLogo() {
        logoView = loadImageView(
                MAIN_MENU_LOGO_PATH,
                612,
                195
        );

        logoView.setOpacity(0);
        logoView.setScaleX(0.4);
        logoView.setScaleY(0.4);

        StackPane.setAlignment(logoView, Pos.TOP_CENTER);
        StackPane.setMargin(logoView, new Insets(70, 0, 0, 0));

        root.getChildren().add(logoView);
    }

    /**
     * 建立主選單按鈕。
     */
    private void createMainButtons() {
        mainButtonBox = new VBox(16);
        mainButtonBox.setAlignment(Pos.CENTER);
        mainButtonBox.setOpacity(0);
        mainButtonBox.setTranslateY(80);

        refreshMainMenuTexts();

        StackPane.setAlignment(mainButtonBox, Pos.TOP_CENTER);
        StackPane.setMargin(mainButtonBox, new Insets(225, 0, 0, 0));

        root.getChildren().add(mainButtonBox);
    }

    /**
     * 建立 DEV MODE 標籤。
     */
    private void createDeveloperLabel() {
        devModeLabel = new Label("DEV MODE");

        devModeLabel.setStyle("""
                -fx-font-size: 13px;
                -fx-text-fill: rgba(255,255,255,0.65);
                -fx-background-color: rgba(0,0,0,0.4);
                -fx-padding: 4 8 4 8;
                """);

        devModeLabel.setVisible(Main.devMode);

        StackPane.setAlignment(devModeLabel, Pos.BOTTOM_LEFT);
        StackPane.setMargin(devModeLabel, new Insets(0, 0, 12, 12));

        root.getChildren().add(devModeLabel);
    }


    // =========================================================
    // Main Menu Animation
    // =========================================================

    /**
     * 播放第一次進入主選單的動畫。
     */
    private void playIntroAnimation() {
        ParallelTransition backgroundIntro = createBackgroundIntroAnimation();
        ParallelTransition logoIntro = createLogoIntroAnimation();
        ParallelTransition buttonIntro = createMainButtonsIntroAnimation();

        SequentialTransition sequence = new SequentialTransition(
                backgroundIntro,
                logoIntro,
                buttonIntro
        );

        sequence.setOnFinished(event -> cutscene = false);
        sequence.play();
    }

    private ParallelTransition createBackgroundIntroAnimation() {
        FadeTransition fade = new FadeTransition(
                Duration.seconds(0.85),
                backgroundView
        );
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(
                Duration.seconds(0.65),
                backgroundView
        );
        scale.setFromX(1.08);
        scale.setFromY(1.08);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        return new ParallelTransition(fade, scale);
    }

    private ParallelTransition createLogoIntroAnimation() {
        FadeTransition fade = new FadeTransition(
                Duration.seconds(0.35),
                logoView
        );
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(
                Duration.seconds(0.42),
                logoView
        );
        scale.setFromX(0.4);
        scale.setFromY(0.4);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        return new ParallelTransition(fade, scale);
    }

    private ParallelTransition createMainButtonsIntroAnimation() {
        FadeTransition fade = new FadeTransition(
                Duration.seconds(0.45),
                mainButtonBox
        );
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition move = new TranslateTransition(
                Duration.seconds(0.45),
                mainButtonBox
        );
        move.setFromY(80);
        move.setToY(0);
        move.setInterpolator(Interpolator.EASE_OUT);

        return new ParallelTransition(fade, move);
    }


    // =========================================================
    // Page Navigation
    // =========================================================

    /**
     * 顯示子頁。
     */
    private void showPage(Node page) {
        selectedSettingsButton = null;

        pageLayer.getChildren().clear();
        pageLayer.getChildren().add(page);

        pageLayer.setVisible(true);
        pageLayer.setPickOnBounds(true);
        pageLayer.setOpacity(0);

        FadeTransition darkFade = createFadeTransition(
                darkOverlay,
                darkOverlay.getOpacity(),
                1.0,
                0.25
        );

        FadeTransition logoFade = createFadeTransition(
                logoView,
                logoView.getOpacity(),
                0,
                0.18
        );

        FadeTransition buttonFade = createFadeTransition(
                mainButtonBox,
                mainButtonBox.getOpacity(),
                0,
                0.18
        );

        FadeTransition pageFade = createFadeTransition(
                pageLayer,
                0,
                1,
                0.28
        );

        ParallelTransition transition = new ParallelTransition(
                darkFade,
                logoFade,
                buttonFade,
                pageFade
        );

        transition.setOnFinished(event -> {
            logoView.setVisible(false);
            mainButtonBox.setVisible(false);
        });

        transition.play();
    }

    /**
     * 關閉目前子頁，回主選單。
     */
    private void closePage() {
        logoView.setVisible(true);
        mainButtonBox.setVisible(true);

        FadeTransition pageFade = createFadeTransition(
                pageLayer,
                pageLayer.getOpacity(),
                0,
                0.18
        );

        FadeTransition darkFade = createFadeTransition(
                darkOverlay,
                darkOverlay.getOpacity(),
                0,
                0.22
        );

        FadeTransition logoFade = createFadeTransition(
                logoView,
                0,
                1,
                0.22
        );

        FadeTransition buttonFade = createFadeTransition(
                mainButtonBox,
                0,
                1,
                0.22
        );

        ParallelTransition transition = new ParallelTransition(
                pageFade,
                darkFade,
                logoFade,
                buttonFade
        );

        transition.setOnFinished(event -> {
            pageLayer.getChildren().clear();
            pageLayer.setVisible(false);
            pageLayer.setPickOnBounds(false);
        });

        transition.play();
    }

    /**
     * 更換右側內容。
     */
    private void showRightContent(BorderPane page, Node content) {
        page.setCenter(content);
    }


    // =========================================================
    // Story Mode Page
    // =========================================================

    private void showStoryModePage() {
        BorderPane page = createSubPageBase();

        VBox leftMenu = createLeftMenu(
                createSubButton(text("menu.storyMode.newGame"), this::startStoryNewGame),

                createSubButton(text("menu.storyMode.loadSaves"), () ->
                        showRightContent(
                                page,
                                new SaveSlotPanel(
                                        SaveMenuMode.LOAD,
                                        null,
                                        slotIndex -> fireNewGame(),
                                        null
                                )
                        )
                ),

                createSubButton(text("menu.storyMode.editSave"), () ->
                        showRightContent(
                                page,
                                new SaveSlotPanel(
                                        SaveMenuMode.EDIT,
                                        null,
                                        null,
                                        null
                                )
                        )
                ),

                createSubButton(text("menu.common.back"), this::closePage)
        );

        page.setLeft(leftMenu);
        page.setCenter(createInfoPanel(
                text("menu.story"),
                text("menu.storyMode.description")
        ));

        showPage(page);
    }

    /**
     * 開始故事模式新遊戲。
     */
    private void startStoryNewGame() {
        musicSystem.stopBGM();
        SceneManager.clearPendingStartScene();
        fireNewGame();
    }


    // =========================================================
    // Mini Game Page
    // =========================================================

    private void showMiniGameModePage() {
        BorderPane page = createSubPageBase();

        VBox leftMenu = createLeftMenu(
                createSubButton(
                        text("menu.miniGameMode.StreetEndless"),
                        this::startStreetEndlessMode
                ),

                createSubButton(
                        text("menu.miniGameMode.comingSoon"),
                        () -> showComingSoonMiniGameInfo(page)
                ),

                createSubButton(
                        text("menu.miniGameMode.comingSoon"),
                        () -> showComingSoonMiniGameInfo(page)
                ),

                createSubButton(
                        text("menu.miniGameMode.comingSoon"),
                        () -> showComingSoonMiniGameInfo(page)
                ),

                createSubButton(text("menu.common.back"), this::closePage)
        );

        page.setLeft(leftMenu);
        page.setCenter(createInfoPanel(
                text("menu.miniGame"),
                text("menu.miniGameMode.description")
        ));

        showPage(page);
    }

    /**
     * 開始 Street Endless 迷你遊戲。
     */
    private void startStreetEndlessMode() {
        musicSystem.stopBGM();
        SceneManager.requestStartScene(SceneType.STREET_ENDLESS);
        fireNewGame();
    }

    /**
     * 顯示尚未開放關卡資訊。
     */
    private void showComingSoonMiniGameInfo(BorderPane page) {
        showRightContent(
                page,
                createInfoPanel(
                        text("menu.miniGameMode.comingSoon"),
                        text("menu.miniGameMode.comingSoon.description")
                )
        );
    }


    // =========================================================
    // Achievement Page
    // =========================================================

    private void showAchievementPage() {
        StackPane page = new StackPane();
        page.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        Text pageTitle = createPageTitle(text("menu.achievement"));
        StackPane.setAlignment(pageTitle, Pos.TOP_CENTER);
        StackPane.setMargin(pageTitle, new Insets(42, 0, 0, 0));

        ScrollPane achievementScroll = createAchievementScroll();

        StackPane.setAlignment(achievementScroll, Pos.CENTER);
        StackPane.setMargin(achievementScroll, new Insets(90, 0, 70, 0));

        StackPane backButton = createSubButton(
                text("menu.common.back"),
                this::closePage
        );

        StackPane.setAlignment(backButton, Pos.BOTTOM_LEFT);
        StackPane.setMargin(backButton, new Insets(0, 0, 42, 57));

        page.getChildren().addAll(
                achievementScroll,
                pageTitle,
                backButton
        );

        showPage(page);
    }

    private ScrollPane createAchievementScroll() {
        VBox list = new VBox(18);
        list.setAlignment(Pos.TOP_CENTER);
        list.setPadding(new Insets(18, 0, 18, 0));
        list.setStyle("-fx-background-color: transparent;");

        expandedAchievementCell = null;

        for (DeathReason reason : DeathReason.values()) {
            list.getChildren().add(createExpandableAchievementCell(reason));
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setPrefSize(SCREEN_WIDTH, 530);
        scroll.setMaxSize(Double.MAX_VALUE, 530);
        scroll.setPrefWidth(SCREEN_WIDTH);
        scroll.setMaxWidth(Double.MAX_VALUE);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        list.prefWidthProperty().bind(scroll.widthProperty());

        scroll.setStyle("""
                -fx-background: transparent;
                -fx-background-color: transparent;
                -fx-padding: 0;
                """);

        applyStyleSheet(scroll);
        scroll.getStyleClass().add("settings-scroll");

        return scroll;
    }

    private StackPane createExpandableAchievementCell(DeathReason reason) {
        boolean unlocked = achievementSystem.isUnlocked(reason);

        double collapsedWidth = 92;
        double expandedWidth = 360;
        double height = 92;

        StackPane slot = new StackPane();
        slot.setPrefSize(expandedWidth, height);
        slot.setMinSize(expandedWidth, height);
        slot.setMaxSize(expandedWidth, height);
        slot.setAlignment(Pos.CENTER);

        Rectangle background = createAchievementCellBackground(
                collapsedWidth,
                height,
                unlocked
        );

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(70, height);
        iconBox.setMinSize(70, height);
        iconBox.getChildren().add(createAchievementIcon(reason, unlocked));

        VBox detailBox = createAchievementDetailBox(reason, unlocked);

        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefSize(collapsedWidth, height);
        card.setMinSize(collapsedWidth, height);
        card.setMaxSize(collapsedWidth, height);
        card.setPadding(new Insets(0, 18, 0, 18));
        card.getChildren().addAll(iconBox, detailBox);

        StackPane backgroundLayer = new StackPane(background);
        backgroundLayer.setMouseTransparent(true);
        backgroundLayer.setTranslateX(8);

        StackPane cardWrapper = new StackPane(backgroundLayer, card);
        cardWrapper.setPrefSize(collapsedWidth, height);
        cardWrapper.setMinSize(collapsedWidth, height);
        cardWrapper.setMaxSize(collapsedWidth, height);
        cardWrapper.setAlignment(Pos.CENTER);
        cardWrapper.setUserData(false);

        card.prefWidthProperty().bind(cardWrapper.widthProperty());
        card.minWidthProperty().bind(cardWrapper.widthProperty());
        card.maxWidthProperty().bind(cardWrapper.widthProperty());

        slot.getChildren().add(cardWrapper);

        slot.setOnMouseClicked(event -> {
            if (!unlocked) {
                return;
            }

            toggleAchievementCell(
                    cardWrapper,
                    background,
                    detailBox,
                    collapsedWidth,
                    expandedWidth,
                    height
            );
        });

        return slot;
    }

    private Rectangle createAchievementCellBackground(
            double width,
            double height,
            boolean unlocked
    ) {
        Rectangle background = new Rectangle(width, height);

        background.setArcWidth(14);
        background.setArcHeight(14);
        background.setFill(
                unlocked
                        ? Color.rgb(210, 60, 60, 0.78)
                        : Color.rgb(40, 40, 40, 0.88)
        );
        background.setStroke(
                Color.rgb(255, 255, 255, unlocked ? 0.9 : 0.35)
        );
        background.setStrokeWidth(unlocked ? 2.0 : 1.0);

        return background;
    }

    private VBox createAchievementDetailBox(
            DeathReason reason,
            boolean unlocked
    ) {
        VBox detailBox = new VBox(5);
        detailBox.setAlignment(Pos.CENTER_LEFT);
        detailBox.setOpacity(0);
        detailBox.setMouseTransparent(true);

        Text title = new Text(
                unlocked
                        ? reason.getTitle()
                        : text("menu.achievement.locked")
        );
        title.setWrappingWidth(220);
        title.setStyle("""
                -fx-font-size: 18px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        Text subtitle = new Text(
                unlocked
                        ? reason.getSubtitle()
                        : text("menu.achievement.description")
        );
        subtitle.setWrappingWidth(220);
        subtitle.setStyle("""
                -fx-font-size: 14px;
                -fx-fill: rgba(255,255,255,0.78);
                """);

        detailBox.getChildren().addAll(title, subtitle);

        return detailBox;
    }

    private Node createAchievementIcon(
            DeathReason reason,
            boolean unlocked
    ) {
        if (!unlocked) {
            return createIconText("?");
        }

        Image iconImage = loadDeathReasonIcon(reason);

        if (iconImage != null) {
            ImageView imageView = new ImageView(iconImage);
            imageView.setFitWidth(54);
            imageView.setFitHeight(54);
            imageView.setPreserveRatio(true);
            return imageView;
        }

        return createIconText("☠");
    }

    private Text createIconText(String value) {
        Text text = new Text(value);

        text.setStyle("""
                -fx-font-size: 38px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        return text;
    }

    private Image loadDeathReasonIcon(DeathReason reason) {
        if (reason == null ||
                reason.getIconPath() == null ||
                reason.getIconPath().isBlank()) {
            return null;
        }

        String path = reason.getIconPath().startsWith("/")
                ? reason.getIconPath()
                : "/" + reason.getIconPath();

        return loadImageOrNull(path);
    }

    private void toggleAchievementCell(
            StackPane cardWrapper,
            Rectangle background,
            VBox detailBox,
            double collapsedWidth,
            double expandedWidth,
            double height
    ) {
        boolean expanded = (boolean) cardWrapper.getUserData();

        if (expanded) {
            collapseAchievementCell(
                    cardWrapper,
                    background,
                    detailBox,
                    collapsedWidth
            );
            expandedAchievementCell = null;
            return;
        }

        if (expandedAchievementCell != null &&
                expandedAchievementCell != cardWrapper) {
            collapseExpandedAchievementCell();
        }

        expandAchievementCell(
                cardWrapper,
                background,
                detailBox,
                expandedWidth
        );

        expandedAchievementCell = cardWrapper;
    }

    private void expandAchievementCell(
            StackPane cardWrapper,
            Rectangle background,
            VBox detailBox,
            double expandedWidth
    ) {
        cardWrapper.setUserData(true);

        Timeline widthAnimation = new Timeline(
                new KeyFrame(
                        Duration.seconds(0.22),
                        new KeyValue(background.widthProperty(), expandedWidth, Interpolator.EASE_OUT),
                        new KeyValue(cardWrapper.prefWidthProperty(), expandedWidth, Interpolator.EASE_OUT),
                        new KeyValue(cardWrapper.minWidthProperty(), expandedWidth, Interpolator.EASE_OUT),
                        new KeyValue(cardWrapper.maxWidthProperty(), expandedWidth, Interpolator.EASE_OUT)
                )
        );

        FadeTransition detailFade = new FadeTransition(
                Duration.seconds(0.18),
                detailBox
        );
        detailFade.setFromValue(0);
        detailFade.setToValue(1);
        detailFade.setDelay(Duration.seconds(0.08));

        TranslateTransition detailMove = new TranslateTransition(
                Duration.seconds(0.22),
                detailBox
        );
        detailMove.setFromX(-12);
        detailMove.setToX(0);
        detailMove.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(
                widthAnimation,
                detailFade,
                detailMove
        ).play();
    }

    private void collapseAchievementCell(
            StackPane cardWrapper,
            Rectangle background,
            VBox detailBox,
            double collapsedWidth
    ) {
        cardWrapper.setUserData(false);

        FadeTransition detailFade = new FadeTransition(
                Duration.seconds(0.08),
                detailBox
        );
        detailFade.setFromValue(detailBox.getOpacity());
        detailFade.setToValue(0);

        Timeline widthAnimation = new Timeline(
                new KeyFrame(
                        Duration.seconds(0.18),
                        new KeyValue(background.widthProperty(), collapsedWidth, Interpolator.EASE_OUT),
                        new KeyValue(cardWrapper.prefWidthProperty(), collapsedWidth, Interpolator.EASE_OUT),
                        new KeyValue(cardWrapper.minWidthProperty(), collapsedWidth, Interpolator.EASE_OUT),
                        new KeyValue(cardWrapper.maxWidthProperty(), collapsedWidth, Interpolator.EASE_OUT)
                )
        );

        new SequentialTransition(
                detailFade,
                widthAnimation
        ).play();
    }

    /**
     * 收起目前展開中的成就 Cell。
     */
    private void collapseExpandedAchievementCell() {
        if (expandedAchievementCell == null) {
            return;
        }

        StackPane cardWrapper = expandedAchievementCell;

        if (cardWrapper.getChildren().size() < 2) {
            expandedAchievementCell = null;
            return;
        }

        StackPane backgroundLayer = (StackPane) cardWrapper.getChildren().get(0);
        HBox card = (HBox) cardWrapper.getChildren().get(1);

        Rectangle background = (Rectangle) backgroundLayer.getChildren().get(0);

        VBox detailBox = card.getChildren()
                .stream()
                .filter(node -> node instanceof VBox)
                .map(node -> (VBox) node)
                .findFirst()
                .orElse(null);

        if (detailBox != null) {
            collapseAchievementCell(
                    cardWrapper,
                    background,
                    detailBox,
                    92
            );
        }

        expandedAchievementCell = null;
    }


    // =========================================================
    // Settings Page
    // =========================================================

    private void showSettingsPage() {
        BorderPane page = createSubPageBase();

        VBox leftMenu = createLeftMenu();

        StackPane keyButton = createSettingsSideButton(
                text("menu.settings.KeyConfig"),
                () -> showRightContent(page, createKeyConfigPanel())
        );

        StackPane volumeButton = createSettingsSideButton(
                text("menu.settings.volume"),
                () -> showRightContent(page, createVolumePanel())
        );

        StackPane windowButton = createSettingsSideButton(
                text("menu.settings.window"),
                () -> showRightContent(page, createWindowSizePanel())
        );

        StackPane languageButton = createSettingsSideButton(
                text("menu.settings.language"),
                () -> showRightContent(page, createLanguagePanel())
        );

        StackPane resetButton = createSettingsSideButton(
                text("menu.settings.reset"),
                () -> showRightContent(page, createResetGamePanel())
        );

        StackPane devButton = createSettingsSideButton(
                text("menu.settings.dev_mode"),
                () -> showRightContent(page, createDeveloperModePanel())
        );

        StackPane aboutButton = createSettingsSideButton(
                text("menu.settings.about"),
                () -> showRightContent(page, createAboutGamePanel())
        );

        StackPane backButton = createSubButton(
                text("menu.common.back"),
                this::closePage
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        leftMenu.getChildren().addAll(
                keyButton,
                volumeButton,
                windowButton,
                languageButton,
                resetButton,
                devButton,
                aboutButton,
                spacer,
                backButton
        );

        page.setLeft(leftMenu);
        page.setCenter(createInfoPanel(
                text("menu.settings"),
                text("menu.settings.description")
        ));

        showPage(page);
    }

    private StackPane createSettingsSideButton(
            String labelText,
            Runnable action
    ) {
        StackPane button = new StackPane();
        button.setPrefSize(SETTINGS_SIDE_BUTTON_WIDTH, SETTINGS_SIDE_BUTTON_HEIGHT);
        button.setMinSize(SETTINGS_SIDE_BUTTON_WIDTH, SETTINGS_SIDE_BUTTON_HEIGHT);
        button.setMaxSize(SETTINGS_SIDE_BUTTON_WIDTH, SETTINGS_SIDE_BUTTON_HEIGHT);
        button.setPickOnBounds(true);

        Rectangle background = new Rectangle(
                SETTINGS_SIDE_BUTTON_WIDTH,
                SETTINGS_SIDE_BUTTON_HEIGHT
        );
        background.setArcWidth(10);
        background.setArcHeight(10);

        Text label = new Text(labelText);
        label.setStyle("""
                -fx-font-size: 22px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        Text arrow = new Text(">");
        arrow.setVisible(false);
        arrow.setStyle("""
                -fx-font-size: 26px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        StackPane.setAlignment(label, Pos.CENTER_LEFT);
        StackPane.setMargin(label, new Insets(0, 0, 0, 24));

        StackPane.setAlignment(arrow, Pos.CENTER_RIGHT);
        StackPane.setMargin(arrow, new Insets(0, -28, 0, 0));

        button.getChildren().addAll(background, label, arrow);
        button.setUserData(new SettingsButtonState(background, arrow));

        setSettingsButtonSelected(button, false);

        button.setOnMouseEntered(event -> {
            if (button != selectedSettingsButton) {
                audioSystem.playButtonSFX(SoundId.BUTTON_HOVER);
                background.setFill(Color.rgb(255, 255, 255, 0.14));
            }
        });

        button.setOnMouseExited(event -> {
            if (button != selectedSettingsButton) {
                setSettingsButtonSelected(button, false);
            }
        });

        button.setOnMouseClicked(event -> {
            audioSystem.playButtonSFX(SoundId.BUTTON_PRESSED);
            selectSettingsButton(button);

            if (action != null) {
                action.run();
            }
        });

        return button;
    }

    private void selectSettingsButton(StackPane button) {
        if (selectedSettingsButton != null) {
            setSettingsButtonSelected(selectedSettingsButton, false);
        }

        selectedSettingsButton = button;
        setSettingsButtonSelected(button, true);
    }

    private void setSettingsButtonSelected(
            StackPane button,
            boolean selected
    ) {
        Object data = button.getUserData();

        if (!(data instanceof SettingsButtonState state)) {
            return;
        }

        Rectangle background = state.background();
        Text arrow = state.arrow();

        arrow.setVisible(selected);

        if (selected) {
            background.setFill(Color.rgb(255, 255, 255, 0.24));
            background.setStroke(Color.WHITE);
            background.setStrokeWidth(2.0);
            return;
        }

        background.setFill(Color.rgb(0, 0, 0, 0.48));
        background.setStroke(Color.rgb(255, 255, 255, 0.45));
        background.setStrokeWidth(1.2);
    }

    private record SettingsButtonState(
            Rectangle background,
            Text arrow
    ) {
    }


    // =========================================================
    // Key Config Panel
    // =========================================================

    private VBox createKeyConfigPanel() {
        VBox box = createPanelBox();

        box.getChildren().addAll(
                createKeyConfigRow(
                        "/assets/textures/ui/keys/key-a.png",
                        " / ",
                        "/assets/textures/ui/keys/key-left.png",
                        text("menu.settings.keyConfig.left")
                ),
                createKeyConfigRow(
                        "/assets/textures/ui/keys/key-d.png",
                        " / ",
                        "/assets/textures/ui/keys/key-right.png",
                        text("menu.settings.keyConfig.right")
                ),
                createKeyConfigRow(
                        "/assets/textures/ui/keys/key-w.png",
                        " / ",
                        "/assets/textures/ui/keys/key-up.png",
                        " / ",
                        "/assets/textures/ui/keys/key-space.png",
                        text("menu.settings.keyConfig.jump")
                ),
                createKeyConfigRow(
                        "/assets/textures/ui/keys/key-s.png",
                        " / ",
                        "/assets/textures/ui/keys/key-down.png",
                        text("menu.settings.keyConfig.drop")
                ),
                createKeyConfigRow(
                        "/assets/textures/ui/keys/key-f.png",
                        text("menu.settings.keyConfig.interact")
                ),
                createKeyConfigRow(
                        "/assets/textures/ui/keys/key-shift.png",
                        text("menu.settings.keyConfig.dash")
                ),
                createKeyConfigRow(
                        "/assets/textures/ui/keys/key-escape.png",
                        text("menu.settings.keyConfig.pause")
                )
        );

        return box;
    }

    /**
     * 建立按鍵說明列。
     *
     * items 可傳 String 或圖片路徑：
     * - 若 String 以 /assets/ 開頭，會視為圖片。
     * - 其他 String 會視為文字。
     */
    private HBox createKeyConfigRow(String... items) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        for (String item : items) {
            if (item.startsWith("/assets/")) {
                row.getChildren().add(createKeyImage(item));
            } else {
                row.getChildren().add(createInlineText(item));
            }
        }

        return row;
    }

    private ImageView createKeyImage(String path) {
        Image image = new Image(getClass().getResource(path).toExternalForm());

        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(50);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        return imageView;
    }

    private Text createInlineText(String value) {
        Text text = new Text(value);
        text.setStyle("""
                -fx-font-size: 22px;
                -fx-fill: rgba(255,255,255,0.86);
                """);
        return text;
    }


    // =========================================================
    // Volume Panel
    // =========================================================

    @FunctionalInterface
    private interface VolumeSetter {
        void set(double value);
    }

    @FunctionalInterface
    private interface MuteSetter {
        void set(boolean muted);
    }

    private VBox createVolumePanel() {
        VBox box = createPanelBox();

        loadVolumeIcons();

        box.getChildren().addAll(
                createPageTitle(text("menu.settings.volume")),

                createVolumeRow(
                        text("menu.settings.volume.global"),
                        audioSystem.getMasterVolume(),
                        audioSystem.isMasterMuted(),
                        audioSystem::setMasterVolume,
                        audioSystem::setMasterMuted,
                        musicSystem::applyVolume
                ),

                createVolumeRow(
                        text("menu.settings.volume.music"),
                        audioSystem.getMusicVolume(),
                        audioSystem.isMusicMuted(),
                        audioSystem::setMusicVolume,
                        audioSystem::setMusicMuted,
                        musicSystem::applyVolume
                ),

                createVolumeRow(
                        text("menu.settings.volume.sound"),
                        audioSystem.getSfxVolume(),
                        audioSystem.isSfxMuted(),
                        audioSystem::setSfxVolume,
                        audioSystem::setSfxMuted,
                        null
                ),

                createButtonSoundToggleRow()
        );

        return box;
    }

    private void loadVolumeIcons() {
        if (volumeIcon != null &&
                volumeDownIcon != null &&
                volumeMuteIcon != null) {
            return;
        }

        volumeIcon = loadImageOrNull("/assets/textures/ui/volume/volume.png");
        volumeDownIcon = loadImageOrNull("/assets/textures/ui/volume/volume-down.png");
        volumeMuteIcon = loadImageOrNull("/assets/textures/ui/volume/volume-mute.png");
    }

    private HBox createVolumeRow(
            String name,
            double initialVolume,
            boolean initiallyMuted,
            VolumeSetter volumeSetter,
            MuteSetter muteSetter,
            Runnable onChanged
    ) {
        Label nameLabel = createVolumeNameLabel(name);

        ImageView iconView = createVolumeIconView();

        double displayVolume = initiallyMuted ? 0 : initialVolume;
        updateVolumeIcon(iconView, displayVolume);

        StackPane iconButton = createVolumeIconButton(iconView);

        Slider slider = createVolumeSlider(displayVolume);
        Label percentLabel = createVolumePercentLabel(displayVolume);

        VolumeRowState state = new VolumeRowState(
                initiallyMuted,
                initialVolume > 0 ? initialVolume : 1.0
        );

        iconButton.setOnMouseClicked(event -> toggleVolumeMute(
                state,
                slider,
                percentLabel,
                iconView,
                volumeSetter,
                muteSetter,
                onChanged
        ));

        slider.valueProperty().addListener((observable, oldValue, newValue) ->
                handleVolumeSliderChanged(
                        state,
                        newValue.doubleValue(),
                        percentLabel,
                        iconView,
                        volumeSetter,
                        muteSetter,
                        onChanged
                )
        );

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
                nameLabel,
                iconButton,
                slider,
                percentLabel
        );

        HBox.setHgrow(slider, Priority.ALWAYS);

        return row;
    }

    private Label createVolumeNameLabel(String name) {
        Label label = new Label(name);
        label.setMinWidth(90);
        label.setStyle("""
                -fx-font-size: 18px;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                """);
        return label;
    }

    private ImageView createVolumeIconView() {
        ImageView iconView = new ImageView();
        iconView.setFitWidth(30);
        iconView.setFitHeight(30);
        iconView.setPreserveRatio(true);
        iconView.setSmooth(true);
        return iconView;
    }

    private StackPane createVolumeIconButton(ImageView iconView) {
        StackPane button = new StackPane(iconView);
        button.setPrefSize(38, 38);
        button.setMaxSize(38, 38);
        button.setPickOnBounds(true);
        button.setStyle("""
                -fx-background-color: rgba(255,255,255,0.08);
                -fx-background-radius: 8;
                """);
        return button;
    }

    private Slider createVolumeSlider(double initialValue) {
        Slider slider = new Slider(0, 1, initialValue);
        slider.setMaxWidth(Double.MAX_VALUE);
        slider.setPrefWidth(360);
        slider.getStyleClass().add("settings-slider");
        applyStyleSheet(slider);

        slider.valueProperty().addListener((observable, oldValue, newValue) ->
                updateSliderProgressStyle(slider)
        );

        slider.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                slider.applyCss();
                updateSliderProgressStyle(slider);
            }
        });

        return slider;
    }

    private Label createVolumePercentLabel(double value) {
        Label label = new Label(toPercentText(value));
        label.setMinWidth(52);
        label.setAlignment(Pos.CENTER_RIGHT);
        label.setStyle("""
                -fx-font-size: 17px;
                -fx-text-fill: white;
                """);
        return label;
    }

    private void toggleVolumeMute(
            VolumeRowState state,
            Slider slider,
            Label percentLabel,
            ImageView iconView,
            VolumeSetter volumeSetter,
            MuteSetter muteSetter,
            Runnable onChanged
    ) {
        double sliderValue = slider.getValue();

        if (!state.muted && sliderValue > 0) {
            state.lastVolumeBeforeMute = sliderValue;
            state.muted = true;

            muteSetter.set(true);
            slider.setValue(0);

            percentLabel.setText("0%");
            updateVolumeIcon(iconView, 0);
            run(onChanged);
            return;
        }

        if (state.muted) {
            state.muted = false;
            muteSetter.set(false);

            double restoreVolume = state.lastVolumeBeforeMute <= 0
                    ? 1.0
                    : state.lastVolumeBeforeMute;

            volumeSetter.set(restoreVolume);
            slider.setValue(restoreVolume);

            percentLabel.setText(toPercentText(restoreVolume));
            updateVolumeIcon(iconView, restoreVolume);
            run(onChanged);
        }
    }

    private void handleVolumeSliderChanged(
            VolumeRowState state,
            double value,
            Label percentLabel,
            ImageView iconView,
            VolumeSetter volumeSetter,
            MuteSetter muteSetter,
            Runnable onChanged
    ) {
        if (value > 0) {
            state.muted = false;
            state.lastVolumeBeforeMute = value;
            muteSetter.set(false);
        } else {
            state.muted = true;
            muteSetter.set(true);
        }

        volumeSetter.set(value);

        percentLabel.setText(toPercentText(value));
        updateVolumeIcon(iconView, value);
        run(onChanged);
    }

    private void updateVolumeIcon(
            ImageView iconView,
            double displayVolume
    ) {
        if (displayVolume <= 0) {
            iconView.setImage(volumeMuteIcon);
        } else if (displayVolume <= 0.5) {
            iconView.setImage(volumeDownIcon);
        } else {
            iconView.setImage(volumeIcon);
        }
    }

    private void updateSliderProgressStyle(Slider slider) {
        Node track = slider.lookup(".track");

        if (track == null) {
            return;
        }

        double min = slider.getMin();
        double max = slider.getMax();
        double value = slider.getValue();

        double percent = (value - min) / (max - min) * 100.0;

        track.setStyle(String.format("""
                -fx-background-color:
                    linear-gradient(to right,
                        rgba(213, 105, 16, 0.95) 0%%,
                        rgba(213, 105, 16, 0.95) %.1f%%,
                        rgba(0, 0, 0, 0.55) %.1f%%,
                        rgba(0, 0, 0, 0.55) 100%%);
                -fx-border-color: rgba(255, 255, 255, 0.28);
                -fx-border-width: 1px;
                -fx-pref-height: 8px;
                """, percent, percent));
    }

    private String toPercentText(double value) {
        return Math.round(value * 100) + "%";
    }

    private static class VolumeRowState {
        private boolean muted;
        private double lastVolumeBeforeMute;

        private VolumeRowState(
                boolean muted,
                double lastVolumeBeforeMute
        ) {
            this.muted = muted;
            this.lastVolumeBeforeMute = lastVolumeBeforeMute;
        }
    }


    // =========================================================
    // Toggle Switch
    // =========================================================

    @FunctionalInterface
    private interface ToggleAction {
        void onToggle(boolean enabled);
    }

    private record ToggleSwitchState(
            Rectangle track,
            Rectangle knob,
            boolean enabled
    ) {
    }

    private HBox createButtonSoundToggleRow() {
        Label nameLabel = new Label(text("menu.settings.volume.button_sound"));
        nameLabel.setMinWidth(90);
        nameLabel.setStyle("""
            -fx-font-size: 18px;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            """);

        Label stateLabel = new Label(
                audioSystem.isButtonSoundEnabled()
                        ? text("menu.common.on")
                        : text("menu.common.off")
        );
        stateLabel.setMinWidth(52);
        stateLabel.setStyle("""
            -fx-font-size: 17px;
            -fx-text-fill: white;
            """);

        StackPane toggleSwitch = createToggleSwitch(
                audioSystem.isButtonSoundEnabled(),
                enabled -> {
                    audioSystem.setButtonSoundEnabled(enabled);

                    stateLabel.setText(
                            enabled
                                    ? text("menu.common.on")
                                    : text("menu.common.off")
                    );

                    if (enabled) {
                        audioSystem.playSFX(SoundId.BUTTON_PRESSED);
                    }
                }
        );

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
                nameLabel,
                toggleSwitch,
                stateLabel
        );

        return row;
    }

    private StackPane createToggleSwitch(
            boolean enabled,
            ToggleAction action
    ) {
        double width = 64;
        double height = 32;
        double knobSize = 26;

        Rectangle track = new Rectangle(width, height);
        track.setArcWidth(height);
        track.setArcHeight(height);

        Rectangle knob = new Rectangle(knobSize, knobSize);
        knob.setArcWidth(knobSize);
        knob.setArcHeight(knobSize);
        knob.setFill(Color.WHITE);

        StackPane toggle = new StackPane(track, knob);
        toggle.setPrefSize(width, height);
        toggle.setMinSize(width, height);
        toggle.setMaxSize(width, height);
        toggle.setPickOnBounds(true);

        StackPane.setAlignment(knob, Pos.CENTER_LEFT);
        StackPane.setMargin(knob, new Insets(0, 0, 0, 3));

        toggle.setUserData(new ToggleSwitchState(track, knob, enabled));

        updateToggleSwitch(toggle, enabled, false);

        toggle.setOnMouseClicked(event -> {
            ToggleSwitchState state = (ToggleSwitchState) toggle.getUserData();
            boolean newValue = !state.enabled();

            updateToggleSwitch(toggle, newValue, true);

            if (action != null) {
                action.onToggle(newValue);
            }
        });

        return toggle;
    }

    private void updateToggleSwitch(
            StackPane toggle,
            boolean enabled,
            boolean animate
    ) {
        Object data = toggle.getUserData();

        if (!(data instanceof ToggleSwitchState state)) {
            return;
        }

        Rectangle track = state.track();
        Rectangle knob = state.knob();

        double targetX = enabled ? 32 : 0;

        track.setFill(
                enabled
                        ? Color.rgb(255, 255, 255, 0.82)
                        : Color.rgb(0, 0, 0, 0.62)
        );
        track.setStroke(Color.rgb(255, 255, 255, 0.65));
        track.setStrokeWidth(1.2);

        knob.setFill(
                enabled
                        ? Color.rgb(213, 105, 16)
                        : Color.rgb(180, 180, 180)
        );

        if (animate) {
            TranslateTransition move = new TranslateTransition(
                    Duration.seconds(0.14),
                    knob
            );
            move.setToX(targetX);
            move.setInterpolator(Interpolator.EASE_OUT);
            move.play();
        } else {
            knob.setTranslateX(targetX);
        }

        toggle.setUserData(new ToggleSwitchState(track, knob, enabled));
    }


    // =========================================================
    // Window Size Panel
    // =========================================================

    private VBox createWindowSizePanel() {
        VBox box = createPanelBox();

        Text current = createTextBlock(
                text("menu.settings.window.current") +
                        windowSystem.getCurrentLabel()
        );

        ComboBox<WindowMode> modeBox = createWindowModeComboBox();

        StackPane applyButton = createSubButton(text("menu.common.apply"), () -> {
            WindowMode selectedMode = modeBox.getValue();

            windowSystem.applyMode(selectedMode);

            current.setText(
                    text("menu.settings.window.current") +
                            windowSystem.getCurrentLabel()
            );
        });

        box.getChildren().addAll(
                createPageTitle(text("menu.settings.window")),
                current,
                createTextBlock(text("menu.settings.window.description")),
                modeBox,
                applyButton
        );

        return box;
    }

    private ComboBox<WindowMode> createWindowModeComboBox() {
        ComboBox<WindowMode> modeBox = new ComboBox<>();

        modeBox.getItems().addAll(
                WindowMode.DEFAULT,
                WindowMode.CUSTOM,
                WindowMode.FULLSCREEN
        );

        modeBox.setValue(windowSystem.getMode());
        modeBox.setPrefWidth(260);
        modeBox.setStyle("-fx-font-size: 18px;");

        modeBox.getStyleClass().add("settings-combo-box");
        applyStyleSheet(modeBox);

        modeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(WindowMode mode) {
                return mode == null ? "" : text(mode.getTextKey());
            }

            @Override
            public WindowMode fromString(String string) {
                return null;
            }
        });

        return modeBox;
    }


    // =========================================================
    // Language Panel
    // =========================================================

    private VBox createLanguagePanel() {
        VBox box = createPanelBox();

        Text current = createTextBlock(
                text("menu.settings.language.current") +
                        languageSystem.getCurrentLanguage()
        );

        ComboBox<Language> languageBox = createLanguageComboBox();

        StackPane applyButton = createSubButton(text("menu.common.apply"), () -> {
            languageSystem.setLanguage(languageBox.getValue());

            refreshMainMenuTexts();
            showSettingsPage();
        });

        box.getChildren().addAll(
                createPageTitle(text("menu.settings.language")),
                current,
                languageBox,
                applyButton
        );

        return box;
    }

    private ComboBox<Language> createLanguageComboBox() {
        ComboBox<Language> languageBox = new ComboBox<>();

        languageBox.getItems().addAll(
                Language.ZH_TW,
                Language.EN_US
        );

        languageBox.setValue(languageSystem.getCurrentLanguage());
        languageBox.setPrefWidth(260);
        languageBox.setStyle("-fx-font-size: 18px;");

        languageBox.getStyleClass().add("settings-combo-box");
        applyStyleSheet(languageBox);

        return languageBox;
    }

    /**
     * 重新建立主選單按鈕文字。
     *
     * 切換語言後使用。
     */
    private void refreshMainMenuTexts() {
        if (mainButtonBox == null) {
            return;
        }

        mainButtonBox.getChildren().clear();

        mainButtonBox.getChildren().addAll(
                createMenuButton(text("menu.story"), this::showStoryModePage),
                createMenuButton(text("menu.miniGame"), this::showMiniGameModePage),
                createMenuButton(text("menu.achievement"), this::showAchievementPage),
                createMenuButton(text("menu.settings"), this::showSettingsPage),
                createMenuButton(
                        text("menu.exit"),
                        this::requestExitGame,
                        exitButtonStyle()
                )
        );
    }


    // =========================================================
    // Reset Panel
    // =========================================================

    private VBox createResetGamePanel() {
        VBox box = createPanelBox();

        box.getChildren().addAll(
                createPageTitle(text("menu.settings.reset")),
                createTextBlock(text("menu.settings.reset.description")),

                createSubButton(
                        text("menu.settings.reset.resetSettingsToDefault"),
                        () -> {
                            resetSettingsToDefault();
                            showTextNotice(text("menu.settings.reset.resetSettingsToDefault.notification"));
                        }
                ),

                createSubButton(
                        text("menu.settings.reset.clearAchievement"),
                        () -> {
                            achievementSystem.resetAll();
                            showTextNotice(text("menu.settings.reset.clearAchievement.notification"));
                        }
                ),

                createSubButton(
                        text("menu.settings.reset.deleteLocalData"),
                        () -> showConfirmPopup(
                                text("menu.settings.reset.deleteLocalData.comfirmNotice"),
                                () -> {
                                    deleteLocalData();
                                    showTextNotice(text("menu.settings.reset.deleteLocalData.notification"));
                                }
                        ),
                        exitButtonStyle()
                )
        );

        return box;
    }

    private void resetSettingsToDefault() {
        audioSystem.resetSettings();
        musicSystem.applyVolume();

        windowSystem.resetSettings();
        windowSystem.applySavedSettings();

        languageSystem.resetSettings();

        refreshMainMenuTexts();
        showSettingsPage();
    }

    private void deleteLocalData() {
        audioSystem.resetSettings();
        windowSystem.resetSettings();
        languageSystem.resetSettings();

        achievementSystem.resetAll();
        StreetEndlessRecordSystem.getInstance().reset();

        deleteLocalSaveFolder();

        if (devModeLabel != null) {
            devModeLabel.setVisible(false);
        }

        musicSystem.applyVolume();
        windowSystem.applySavedSettings();

        refreshMainMenuTexts();
        showSettingsPage();
    }

    private void deleteLocalSaveFolder() {
        Path saveFolder = Path.of(
                System.getProperty("user.home"),
                ".taiwanese_difficulty"
        );

        if (!Files.exists(saveFolder)) {
            return;
        }

        try {
            Files.walk(saveFolder)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            System.out.println("Failed to delete: " + path);
                            exception.printStackTrace();
                        }
                    });

        } catch (IOException exception) {
            System.out.println("Failed to format local save data.");
            exception.printStackTrace();
        }
    }


    // =========================================================
    // Developer Mode Panel
    // =========================================================

    @FunctionalInterface
    private interface TogglePressedAction {
        void onChanged(boolean pressed);
    }

    private record SunkenButtonState(
            Rectangle background,
            Text label,
            boolean pressed
    ) {
    }

    private VBox createDeveloperModePanel() {
        VBox box = createPanelBox();

        StackPane toggleButton = createSunkenToggleButton(
                Main.devMode
                        ? text("menu.settings.dev_mode.toggleOn")
                        : text("menu.settings.dev_mode.toggleOff"),
                Main.devMode,
                pressed -> {
                    Main.devMode = pressed;

                    if (devModeLabel != null) {
                        devModeLabel.setVisible(Main.devMode);
                    }

                    showTextNotice(
                            Main.devMode
                                    ? text("menu.settings.dev_mode.activated")
                                    : text("menu.settings.dev_mode.deactivated")
                    );
                }
        );

        box.getChildren().addAll(
                createPageTitle(text("menu.settings.dev_mode")),
                toggleButton
        );

        return box;
    }

    private StackPane createSunkenToggleButton(
            String initialText,
            boolean initiallyPressed,
            TogglePressedAction action
    ) {
        double width = 260;
        double height = 52;

        StackPane button = new StackPane();
        button.setPrefSize(width, height);
        button.setMinSize(width, height);
        button.setMaxSize(width, height);
        button.setPickOnBounds(true);

        Rectangle background = new Rectangle(width, height);
        background.setArcWidth(10);
        background.setArcHeight(10);

        Text label = new Text(initialText);
        label.setStyle("""
                -fx-font-size: 22px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        button.getChildren().addAll(background, label);
        button.setUserData(new SunkenButtonState(
                background,
                label,
                initiallyPressed
        ));

        applySunkenButtonState(button, initiallyPressed);

        button.setOnMouseClicked(event -> {
            SunkenButtonState state =
                    (SunkenButtonState) button.getUserData();

            boolean newPressed = !state.pressed();

            button.setUserData(new SunkenButtonState(
                    background,
                    label,
                    newPressed
            ));

            label.setText(
                    newPressed
                            ? text("menu.settings.dev_mode.toggleOn")
                            : text("menu.settings.dev_mode.toggleOff")
            );

            applySunkenButtonState(button, newPressed);
            audioSystem.playButtonSFX(SoundId.BUTTON_PRESSED);

            if (action != null) {
                action.onChanged(newPressed);
            }
        });

        button.setOnMouseEntered(event -> {
            SunkenButtonState state =
                    (SunkenButtonState) button.getUserData();

            if (!state.pressed()) {
                background.setFill(Color.rgb(255, 255, 255, 0.18));
                label.setFill(Color.BLACK);
            }

            audioSystem.playButtonSFX(SoundId.BUTTON_HOVER);
        });

        button.setOnMouseExited(event -> {
            SunkenButtonState state =
                    (SunkenButtonState) button.getUserData();

            applySunkenButtonState(button, state.pressed());
        });

        return button;
    }

    private void applySunkenButtonState(
            StackPane button,
            boolean pressed
    ) {
        Object data = button.getUserData();

        if (!(data instanceof SunkenButtonState state)) {
            return;
        }

        Rectangle background = state.background();
        Text label = state.label();

        if (pressed) {
            background.setFill(Color.rgb(0, 0, 0, 0.78));
            background.setStroke(Color.rgb(255, 255, 255, 0.95));
            background.setStrokeWidth(2.2);

            label.setFill(Color.WHITE);

            button.setTranslateY(4);
            button.setScaleX(0.98);
            button.setScaleY(0.96);
            button.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.85)));
            return;
        }

        background.setFill(Color.rgb(0, 0, 0, 0.58));
        background.setStroke(Color.rgb(255, 255, 255, 0.72));
        background.setStrokeWidth(1.4);

        label.setFill(Color.WHITE);

        button.setTranslateY(0);
        button.setScaleX(1.0);
        button.setScaleY(1.0);
        button.setEffect(null);
    }


    // =========================================================
    // About Page
    // =========================================================

    private VBox createAboutGamePanel() {
        VBox box = createPanelBox();

        box.getChildren().addAll(
                createPageTitle(text("menu.settings.about")),

                createInfoCard(
                        text("menu.settings.about.info"),
                        text("menu.settings.about.info.name") +
                                text("menu.settings.about.info.genre") +
                                text("menu.settings.about.info.version") +
                                FXGL.getSettings().getVersion()
                ),

                createInfoCard(
                        text("menu.settings.about.description"),
                        text("menu.settings.about.description.construct") +
                                text("menu.settings.about.description.content")
                ),

                createCreditsScroll()
        );

        return box;
    }

    private StackPane createInfoCard(
            String title,
            String content
    ) {
        VBox textBox = new VBox(8);
        textBox.setPadding(new Insets(16, 20, 16, 20));
        textBox.setAlignment(Pos.TOP_LEFT);

        Text titleText = new Text(title);
        titleText.setStyle("""
                -fx-font-size: 22px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        Text contentText = new Text(content);
        contentText.setWrappingWidth(620);
        contentText.setStyle("""
                -fx-font-size: 18px;
                -fx-fill: rgba(255,255,255,0.84);
                """);

        textBox.getChildren().addAll(titleText, contentText);

        Rectangle background = new Rectangle(700, 120);
        background.setArcWidth(18);
        background.setArcHeight(18);
        background.setFill(Color.rgb(0, 0, 0, 0.35));
        background.setStroke(Color.rgb(255, 255, 255, 0.22));
        background.setStrokeWidth(1.2);

        StackPane card = new StackPane(background, textBox);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(700);

        return card;
    }

    private ScrollPane createCreditsScroll() {
        VBox credits = new VBox(14);
        credits.setPadding(new Insets(18, 22, 18, 22));
        credits.setAlignment(Pos.TOP_LEFT);

        credits.getChildren().addAll(
                createCreditSectionTitle(text("menu.settings.about.credits")),

                createCreditRow(text("menu.settings.about.credits.game_design"), "Tiro"),
                createCreditRow(text("menu.settings.about.credits.level_design"), "Tiro"),
                createCreditRow(text("menu.settings.about.credits.narrative_design"), "Tiro"),

                createCreditDivider(),

                createCreditRow(text("menu.settings.about.credits.game_programming"), "Tiro"),
                createCreditRow(text("menu.settings.about.credits.system_logic"), "Tiro"),
                createCreditRow(text("menu.settings.about.credits.ui_programming"), "Tiro"),

                createCreditDivider(),

                createCreditRow(
                        text("menu.settings.about.credits.art_2d"),
                        "Tiro" +
                                text("menu.settings.about.credits.comma") +
                                text("menu.settings.about.credits.online_assets")
                ),
                createCreditRow(
                        text("menu.settings.about.credits.character_design"),
                        "Tiro" +
                                text("menu.settings.about.credits.comma") +
                                text("menu.settings.about.credits.online_assets")
                ),
                createCreditRow(text("menu.settings.about.credits.animation"), "Tiro"),

                createCreditDivider(),

                createCreditRow(
                        text("menu.settings.about.credits.sound_design"),
                        text("menu.settings.about.credits.online_assets")
                ),
                createCreditRow(
                        text("menu.settings.about.credits.music"),
                        text("menu.settings.about.credits.online_assets")
                ),

                createCreditDivider(),

                createCreditRow(text("menu.settings.about.credits.translation.en"), "Tiro"),

                createCreditDivider(),

                createCreditRow(text("menu.settings.about.credits.testing"), "Tiro")
        );

        ScrollPane scroll = new ScrollPane(credits);
        scroll.setPrefSize(720, 270);
        scroll.setMaxSize(720, 270);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scroll.setStyle("""
                -fx-background: transparent;
                -fx-background-color: rgba(0,0,0,0.35);
                -fx-background-radius: 18;
                -fx-border-color: rgba(255,255,255,0.22);
                -fx-border-width: 1.2;
                -fx-border-radius: 18;
                -fx-padding: 4;
                """);

        scroll.getStyleClass().add("settings-scroll");
        applyStyleSheet(scroll);

        credits.setStyle("-fx-background-color: transparent;");

        return scroll;
    }

    private Text createCreditSectionTitle(String value) {
        Text title = new Text(value);

        title.setStyle("""
                -fx-font-size: 24px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        title.setEffect(new DropShadow(6, Color.BLACK));

        return title;
    }

    private HBox createCreditRow(
            String role,
            String name
    ) {
        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);

        Text roleText = new Text(role);
        roleText.setWrappingWidth(150);
        roleText.setStyle("""
                -fx-font-size: 18px;
                -fx-fill: rgba(255,255,255,0.7);
                -fx-font-weight: bold;
                """);

        Text nameText = new Text(name);
        nameText.setWrappingWidth(430);
        nameText.setStyle("""
                -fx-font-size: 18px;
                -fx-fill: white;
                """);

        row.getChildren().addAll(roleText, nameText);

        return row;
    }

    private Rectangle createCreditDivider() {
        Rectangle line = new Rectangle(640, 1);
        line.setFill(Color.rgb(255, 255, 255, 0.18));
        return line;
    }


    // =========================================================
    // Common Page UI
    // =========================================================

    private BorderPane createSubPageBase() {
        BorderPane page = new BorderPane();
        page.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        page.setPickOnBounds(false);
        return page;
    }

    private VBox createLeftMenu(Node... buttons) {
        VBox box = new VBox();
        box.setAlignment(Pos.TOP_LEFT);
        box.setPrefWidth(360);
        box.setMinWidth(360);
        box.setMaxWidth(360);
        box.setPrefHeight(SCREEN_HEIGHT);
        box.setMinHeight(SCREEN_HEIGHT);
        box.setPadding(new Insets(50, 0, 42, 57));

        box.setBackground(new Background(new BackgroundFill(
                Color.rgb(213, 105, 16, 0.82),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        box.setBorder(new Border(new BorderStroke(
                Color.rgb(255, 255, 255, 0.22),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 1.5, 0, 0)
        )));

        VBox topButtons = new VBox(16);
        topButtons.setAlignment(Pos.TOP_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        if (buttons.length == 0) {
            return box;
        }

        for (int i = 0; i < buttons.length; i++) {
            Node button = buttons[i];

            if (i == buttons.length - 1) {
                box.getChildren().addAll(topButtons, spacer, button);
            } else {
                topButtons.getChildren().add(button);
            }
        }

        return box;
    }

    private VBox createPanelBox() {
        VBox box = new VBox(18);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(70, 90, 70, 40));
        return box;
    }

    private VBox createInfoPanel(
            String title,
            String body
    ) {
        VBox box = createPanelBox();

        box.getChildren().addAll(
                createPageTitle(title),
                createTextBlock(body)
        );

        return box;
    }

    private Text createPageTitle(String value) {
        Text title = new Text(value);

        title.setStyle("""
                -fx-font-size: 34px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        title.setEffect(new DropShadow(8, Color.BLACK));

        return title;
    }

    private Text createTextBlock(String value) {
        Text text = new Text(value);

        text.setWrappingWidth(620);
        text.setStyle("""
                -fx-font-size: 22px;
                -fx-fill: rgba(255,255,255,0.86);
                """);

        return text;
    }


    // =========================================================
    // Common Buttons
    // =========================================================

    private StackPane createMenuButton(
            String value,
            Runnable action
    ) {
        return createMenuButton(value, action, defaultButtonStyle());
    }

    private StackPane createMenuButton(
            String value,
            Runnable action,
            ButtonStyle style
    ) {
        StackPane button = createButtonBase(
                value,
                MAIN_BUTTON_WIDTH,
                MAIN_BUTTON_HEIGHT,
                style
        );

        button.setOnMouseClicked(event -> {
            if (cutscene) {
                return;
            }

            audioSystem.playButtonSFX(SoundId.BUTTON_PRESSED);
            run(action);
        });

        return button;
    }

    private StackPane createSubButton(
            String value,
            Runnable action
    ) {
        return createSubButton(value, action, defaultButtonStyle());
    }

    private StackPane createSubButton(
            String value,
            Runnable action,
            ButtonStyle style
    ) {
        StackPane button = createButtonBase(
                value,
                SUB_BUTTON_WIDTH,
                SUB_BUTTON_HEIGHT,
                style
        );

        button.setOnMouseClicked(event -> {
            if (cutscene) {
                return;
            }

            audioSystem.playButtonSFX(SoundId.BUTTON_PRESSED);
            run(action);
        });

        return button;
    }

    private StackPane createPopupButton(
            String value,
            Runnable action
    ) {
        return createPopupButton(value, action, defaultButtonStyle());
    }

    private StackPane createPopupButton(
            String value,
            Runnable action,
            ButtonStyle style
    ) {
        StackPane button = createButtonBase(value, 120, 42, style);

        button.setOnMouseClicked(event -> {
            if (!cutscene) {
                audioSystem.playButtonSFX(SoundId.BUTTON_PRESSED);
            }

            run(action);
        });

        return button;
    }

    private StackPane createButtonBase(
            String value,
            double width,
            double height,
            ButtonStyle style
    ) {
        StackPane button = new StackPane();

        button.setPrefSize(width, height);
        button.setMinSize(width, height);
        button.setMaxSize(width, height);
        button.setPickOnBounds(true);

        Rectangle background = new Rectangle(width, height);
        background.setArcWidth(style.arc());
        background.setArcHeight(style.arc());
        background.setFill(style.normalFill());
        background.setStroke(style.normalStroke());
        background.setStrokeWidth(style.strokeWidth());

        Text label = new Text(value);
        label.setStyle("""
                -fx-font-size: 22px;
                -fx-font-weight: bold;
                """);
        label.setFill(style.normalText());

        button.getChildren().addAll(background, label);

        setupButtonHoverAndPressEffects(
                button,
                background,
                label,
                style
        );

        return button;
    }

    private void setupButtonHoverAndPressEffects(
            StackPane button,
            Rectangle background,
            Text label,
            ButtonStyle style
    ) {
        button.setOnMouseEntered(event -> {
            if (!cutscene) {
                audioSystem.playButtonSFX(SoundId.BUTTON_HOVER);
            }

            background.setFill(style.hoverFill());
            background.setStroke(style.hoverStroke());
            label.setFill(style.hoverText());

            playScale(button, style.hoverScale());
        });

        button.setOnMouseExited(event -> {
            background.setFill(style.normalFill());
            background.setStroke(style.normalStroke());
            label.setFill(style.normalText());

            playScale(button, 1.0);
        });

        button.setOnMousePressed(event -> {
            background.setFill(style.pressedFill());
            background.setStroke(style.pressedStroke());
            label.setFill(style.pressedText());

            button.setScaleX(0.97);
            button.setScaleY(0.97);
        });

        button.setOnMouseReleased(event -> {
            background.setFill(style.hoverFill());
            background.setStroke(style.hoverStroke());
            label.setFill(style.hoverText());

            button.setScaleX(style.hoverScale());
            button.setScaleY(style.hoverScale());
        });
    }

    private void playScale(
            Node node,
            double scale
    ) {
        ScaleTransition transition = new ScaleTransition(
                Duration.seconds(0.08),
                node
        );

        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }

    private record ButtonStyle(
            Color normalFill,
            Color normalStroke,
            Color normalText,

            Color hoverFill,
            Color hoverStroke,
            Color hoverText,

            Color pressedFill,
            Color pressedStroke,
            Color pressedText,

            double strokeWidth,
            double arc,
            double hoverScale
    ) {
    }

    private ButtonStyle defaultButtonStyle() {
        return new ButtonStyle(
                Color.rgb(0, 0, 0, 0.58),
                Color.rgb(255, 255, 255, 0.72),
                Color.WHITE,

                Color.rgb(255, 255, 255, 0.58),
                Color.WHITE,
                Color.BLACK,

                Color.rgb(213, 105, 16, 0.92),
                Color.WHITE,
                Color.WHITE,

                1.4,
                10,
                1.04
        );
    }

    private ButtonStyle exitButtonStyle() {
        return new ButtonStyle(
                Color.rgb(0, 0, 0, 0.58),
                Color.rgb(255, 255, 255, 0.72),
                Color.WHITE,

                Color.rgb(168, 27, 27, 0.9),
                Color.rgb(255, 255, 255, 0.4),
                Color.BLACK,

                Color.rgb(80, 0, 0, 0.95),
                Color.rgb(255, 150, 150, 1.0),
                Color.WHITE,

                1.6,
                10,
                1.05
        );
    }


    // =========================================================
    // Popup / Notice
    // =========================================================

    private void showTextNotice(String value) {
        Label notice = new Label(value);

        notice.setStyle("""
                -fx-font-size: 22px;
                -fx-text-fill: white;
                -fx-background-color: rgba(0,0,0,0.8);
                -fx-padding: 20;
                """);

        StackPane popup = new StackPane(notice);
        popup.setAlignment(Pos.CENTER);
        popup.setMouseTransparent(true);

        pageLayer.getChildren().add(popup);

        PauseTransition wait = new PauseTransition(Duration.seconds(1.2));
        wait.setOnFinished(event -> pageLayer.getChildren().remove(popup));
        wait.play();
    }

    private void showConfirmPopup(
            String message,
            Runnable onConfirm
    ) {
        pageLayer.setVisible(true);
        pageLayer.setOpacity(1);
        pageLayer.setPickOnBounds(true);
        pageLayer.toFront();

        VBox box = new VBox(18);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        box.setMaxSize(460, 180);

        Rectangle background = new Rectangle(460, 180);
        background.setArcWidth(18);
        background.setArcHeight(18);
        background.setFill(Color.rgb(0, 0, 0, 0.88));
        background.setStroke(Color.rgb(255, 255, 255, 0.95));
        background.setStrokeWidth(1.8);
        background.setEffect(new DropShadow(18, Color.rgb(0, 0, 0, 0.9)));

        Text textNode = createTextBlock(message);
        textNode.setWrappingWidth(380);
        textNode.setTextAlignment(TextAlignment.CENTER);

        HBox buttons = new HBox(16);
        buttons.setAlignment(Pos.CENTER);

        StackPane popup = new StackPane(background, box);
        popup.setAlignment(Pos.CENTER);

        StackPane confirm = createPopupButton(
                text("menu.common.confirm"),
                () -> {
                    pageLayer.getChildren().remove(popup);
                    run(onConfirm);
                },
                exitButtonStyle()
        );

        StackPane cancel = createPopupButton(
                text("menu.common.cancel"),
                () -> {
                    pageLayer.getChildren().remove(popup);

                    if (pageLayer.getChildren().isEmpty()) {
                        pageLayer.setVisible(false);
                        pageLayer.setOpacity(0);
                        pageLayer.setPickOnBounds(false);
                    }
                }
        );

        buttons.getChildren().addAll(confirm, cancel);
        box.getChildren().addAll(textNode, buttons);

        pageLayer.getChildren().add(popup);
    }


    // =========================================================
    // Main Menu Reset / Exit
    // =========================================================

    private void resetToMainMenuFirst() {
        cutscene = true;

        clearPageLayer();

        darkOverlay.setOpacity(0);

        backgroundView.setOpacity(0);

        logoView.setVisible(true);
        logoView.setOpacity(0);
        logoView.setScaleX(0.4);
        logoView.setScaleY(0.4);
        logoView.setTranslateX(0);
        logoView.setTranslateY(0);

        mainButtonBox.setVisible(true);
        mainButtonBox.setOpacity(0);
        mainButtonBox.setTranslateX(0);
        mainButtonBox.setTranslateY(80);

        updateDeveloperLabel();
    }

    private void resetToMainMenuSecondary() {
        cutscene = false;

        clearPageLayer();

        darkOverlay.setOpacity(0);

        logoView.setVisible(true);
        logoView.setOpacity(1);
        logoView.setScaleX(1);
        logoView.setScaleY(1);
        logoView.setTranslateX(0);
        logoView.setTranslateY(0);

        mainButtonBox.setVisible(true);
        mainButtonBox.setOpacity(1);
        mainButtonBox.setTranslateX(0);
        mainButtonBox.setTranslateY(0);

        updateDeveloperLabel();
    }

    private void clearPageLayer() {
        pageLayer.getChildren().clear();
        pageLayer.setVisible(false);
        pageLayer.setPickOnBounds(false);
        pageLayer.setOpacity(0);
    }

    private void updateDeveloperLabel() {
        if (devModeLabel != null) {
            devModeLabel.setVisible(Main.devMode);
        }
    }

    private void requestExitGame() {
        showConfirmPopup(
                text("menu.exit.confirm"),
                () -> {
                    musicSystem.stopBGM();
                    getGameController().exit();
                }
        );
    }


    // =========================================================
    // Utility
    // =========================================================

    private FadeTransition createFadeTransition(
            Node node,
            double from,
            double to,
            double seconds
    ) {
        FadeTransition transition = new FadeTransition(
                Duration.seconds(seconds),
                node
        );

        transition.setFromValue(from);
        transition.setToValue(to);

        return transition;
    }

    private ImageView loadImageView(
            String path,
            double width,
            double height
    ) {
        try {
            URL url = getClass().getResource(path);

            if (url != null) {
                Image image = new Image(url.toExternalForm());

                ImageView view = new ImageView(image);
                view.setFitWidth(width);
                view.setFitHeight(height);
                view.setPreserveRatio(false);

                return view;
            }

        } catch (Exception ignored) {
        }

        ImageView empty = new ImageView();
        empty.setFitWidth(width);
        empty.setFitHeight(height);
        return empty;
    }

    private Image loadImageOrNull(String path) {
        try {
            var url = getClass().getResource(path);

            if (url == null) {
                System.out.println("Image not found: " + path);
                return null;
            }

            return new Image(url.toExternalForm());

        } catch (Exception exception) {
            System.out.println("Image load failed: " + path);
            return null;
        }
    }

    private void applyStyleSheet(Node node) {
        if (!(node instanceof Parent parent)) {
            return;
        }

        var css = getClass().getResource("/style.css");

        if (css != null) {
            String cssPath = css.toExternalForm();

            if (!parent.getStylesheets().contains(cssPath)) {
                parent.getStylesheets().add(cssPath);
            }
        }
    }

    private String text(String key) {
        return languageSystem.text(key);
    }

    private void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }
}