package ass.example.ui;

import ass.example.Main;
import ass.example.core.*;
import ass.example.scenes.SceneManager;
import ass.example.system.*;
import ass.example.ui.save.SaveMenuMode;
import ass.example.ui.save.SaveSlotPanel;
import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static com.almasb.fxgl.dsl.FXGLForKtKt.getGameController;

public class MainMenu extends FXGLMenu {

    private static final double SCREEN_WIDTH = 1280;
    private static final double SCREEN_HEIGHT = 720;

    private final StackPane root = new StackPane();

    private ImageView backgroundView;
    private ImageView logoView;
    private VBox mainButtonBox;

    private StackPane pageLayer;
    private StackPane expandedAchievementCell = null;

    private Label devModeLabel;

    private boolean firstCreate = true;
    private boolean cutscene = true;

    private final AchievementSystem achievementSystem = new AchievementSystem();

    // 選項頁
    private Rectangle darkOverlay;
    private StackPane selectedSettingsButton = null;
    private Image volumeIcon;
    private Image volumeDownIcon;
    private Image volumeMuteIcon;
    private final WindowSystem windowSystem = WindowSystem.getInstance();
    private final LanguageSystem languageSystem = LanguageSystem.getInstance();

    // Sound
    private final MusicSystem musicSystem = MusicSystem.getInstance();
    private final AudioSystem audioSystem = AudioSystem.getInstance();

    @Override
    public void onCreate() {
        windowSystem.installResizeListener();
        windowSystem.applySavedSettings();

        musicSystem.stopBGM();

        if (firstCreate) {
            firstCreate = false;

            resetToMainMenuFirst();
            playIntroAnimation();
            musicSystem.playBGMFrom(
                    "/assets/music/mainmenu/Tom Petty - Love Is A Long Road.mp3",
                    16.5,
                    true
            );
        } else {
            resetToMainMenuSecondary();
            musicSystem.playBGMFrom(
                    "/assets/music/mainmenu/Tom Petty - Love Is A Long Road.mp3",
                    0.0,
                    true
            );
        }
    }

    @Override
    public void onDestroy() {
        musicSystem.stopBGM();
    }

    public MainMenu() {
        super(MenuType.MAIN_MENU);

        root.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        root.setMinSize(0, 0);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        createBackground();
        createDarkOverlay();
        createPageLayer();
        createLogo();
        createMainButtons();
        createDeveloperLabel();

        getContentRoot().getChildren().add(root);

        resetToMainMenuFirst();
    }

    private String text(String key) {
        return languageSystem.text(key);
    }

    // =========================
    // 基礎畫面
    // =========================

    private void createBackground() {
        backgroundView = loadImageView(
                "/assets/textures/ui/mainmenu/titlescreen_bg.png",
                SCREEN_WIDTH,
                SCREEN_HEIGHT
        );

        backgroundView.setOpacity(0);
        backgroundView.setPreserveRatio(false);

        /*
         * 重點：
         * 背景跟著 root 尺寸鋪滿，不會因視窗變大被推到一邊。
         */
        backgroundView.fitWidthProperty().bind(root.widthProperty());
        backgroundView.fitHeightProperty().bind(root.heightProperty());

        root.getChildren().add(backgroundView);
    }

    private void createLogo() {
        logoView = loadImageView(
                "/assets/textures/ui/mainmenu/titlescreen_logo.png",
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

    private void createPageLayer() {
        pageLayer = new StackPane();
        pageLayer.setVisible(false);
        pageLayer.setPickOnBounds(false);

        root.getChildren().add(pageLayer);
    }

    private void createMainButtons() {
        mainButtonBox = new VBox(16);
        mainButtonBox.setAlignment(Pos.CENTER);
        mainButtonBox.setOpacity(0);
        mainButtonBox.setTranslateY(80);

        mainButtonBox.getChildren().addAll(
                createMenuButton(text("menu.story"), this::showStoryModePage),
                createMenuButton(text("menu.miniGame"), this::showMiniGameModePage),
                createMenuButton(text("menu.achievement"), this::showAchievementPage),
                createMenuButton(text("menu.settings"), this::showSettingsPage),
                createMenuButton(text("menu.exit"), this::requestExitGame , exitButtonStyle())
        );

        StackPane.setAlignment(mainButtonBox, Pos.TOP_CENTER);

        StackPane.setMargin(mainButtonBox, new Insets(225, 0, 0, 0));

        root.getChildren().add(mainButtonBox);
    }

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

    // =========================
    // 主畫面動畫
    // =========================

    private void playIntroAnimation() {
        FadeTransition bgFade = new FadeTransition(Duration.seconds(0.85), backgroundView);
        bgFade.setFromValue(0);
        bgFade.setToValue(1);

        ScaleTransition bgScale = new ScaleTransition(Duration.seconds(0.65), backgroundView);
        bgScale.setFromX(1.08);
        bgScale.setFromY(1.08);
        bgScale.setToX(1.0);
        bgScale.setToY(1.0);
        bgScale.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition bgIntro = new ParallelTransition(
                bgFade,
                bgScale
        );

        FadeTransition logoFade = new FadeTransition(Duration.seconds(0.35), logoView);
        logoFade.setFromValue(0);
        logoFade.setToValue(1);

        ScaleTransition logoScale = new ScaleTransition(Duration.seconds(0.42), logoView);
        logoScale.setFromX(0.4);
        logoScale.setFromY(0.4);
        logoScale.setToX(1.0);
        logoScale.setToY(1.0);
        logoScale.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition logoPop = new ParallelTransition(logoFade, logoScale);

        FadeTransition buttonFade = new FadeTransition(Duration.seconds(0.45), mainButtonBox);
        buttonFade.setFromValue(0);
        buttonFade.setToValue(1);

        TranslateTransition buttonMove = new TranslateTransition(Duration.seconds(0.45), mainButtonBox);
        buttonMove.setFromY(80);
        buttonMove.setToY(0);
        buttonMove.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition buttonAnim = new ParallelTransition(buttonFade, buttonMove);

        SequentialTransition seq = new SequentialTransition(
                bgIntro,
                new PauseTransition(Duration.seconds(1.5)),
                logoPop,
                new PauseTransition(Duration.seconds(0.13)),
                buttonAnim
        );

        seq.setOnFinished(e -> {
            cutscene = false;
        });

        seq.play();
    }

    // =========================
    // Page 切換
    // =========================

    private void showPage(Node page) {
        pageLayer.getChildren().clear();
        pageLayer.getChildren().add(page);

        pageLayer.setVisible(true);
        pageLayer.setOpacity(0);

        FadeTransition darkFade = new FadeTransition(Duration.seconds(0.25), darkOverlay);
        darkFade.setFromValue(darkOverlay.getOpacity());
        darkFade.setToValue(1.0);

        FadeTransition logoFade = new FadeTransition(Duration.seconds(0.18), logoView);
        logoFade.setFromValue(logoView.getOpacity());
        logoFade.setToValue(0);

        FadeTransition buttonFade = new FadeTransition(Duration.seconds(0.18), mainButtonBox);
        buttonFade.setFromValue(mainButtonBox.getOpacity());
        buttonFade.setToValue(0);

        FadeTransition pageFade = new FadeTransition(Duration.seconds(0.28), pageLayer);
        pageFade.setFromValue(0);
        pageFade.setToValue(1);

        ParallelTransition transition = new ParallelTransition(
                darkFade,
                logoFade,
                buttonFade,
                pageFade
        );

        transition.setOnFinished(e -> {
            logoView.setVisible(false);
            mainButtonBox.setVisible(false);
        });

        transition.play();
    }

    private void closePage() {
        logoView.setVisible(true);
        mainButtonBox.setVisible(true);

        FadeTransition pageFade = new FadeTransition(Duration.seconds(0.18), pageLayer);
        pageFade.setFromValue(pageLayer.getOpacity());
        pageFade.setToValue(0);

        FadeTransition darkFade = new FadeTransition(Duration.seconds(0.22), darkOverlay);
        darkFade.setFromValue(darkOverlay.getOpacity());
        darkFade.setToValue(0);

        FadeTransition logoFade = new FadeTransition(Duration.seconds(0.22), logoView);
        logoFade.setFromValue(0);
        logoFade.setToValue(1);

        FadeTransition buttonFade = new FadeTransition(Duration.seconds(0.22), mainButtonBox);
        buttonFade.setFromValue(0);
        buttonFade.setToValue(1);

        ParallelTransition transition = new ParallelTransition(
                pageFade,
                darkFade,
                logoFade,
                buttonFade
        );

        transition.setOnFinished(e -> {
            pageLayer.getChildren().clear();
            pageLayer.setVisible(false);
        });

        transition.play();
    }

    // =========================
    // 故事模式
    // =========================

    private void showStoryModePage() {
        BorderPane page = createSubPageBase();

        VBox leftMenu = createLeftMenu(
                createSubButton(text("menu.storyMode.newGame"), () -> {
                    fireNewGame();
                }),
                createSubButton(text("menu.storyMode.loadSaves"), () -> {
                    showRightContent(
                            page,
                            new SaveSlotPanel(
                                    SaveMenuMode.LOAD,
                                    null,
                                    slotIndex -> {
                                        fireNewGame();
                                    },
                                    null
                            )
                    );
                }),

                createSubButton(text("menu.storyMode.editSave"), () -> {
                    showRightContent(
                            page,
                            new SaveSlotPanel(
                                    SaveMenuMode.EDIT,
                                    null,
                                    null,
                                    null
                            )
                    );
                }),
                createSubButton(text("menu.common.back"), this::closePage)
        );

        page.setLeft(leftMenu);
        page.setCenter(createInfoPanel(text("menu.story"), text("menu.storyMode.description")));

        showPage(page);
    }

    private VBox createSaveList(String title) {
        VBox box = new VBox(14);
        box.setPadding(new Insets(40));
        box.setAlignment(Pos.TOP_LEFT);

        Text titleText = createPageTitle(title);

        box.getChildren().addAll(
                titleText,
                createTextBlock("存檔 1：尚無資料"),
                createTextBlock("存檔 2：尚無資料"),
                createTextBlock("存檔 3：尚無資料")
        );

        return box;
    }

    // =========================
    // 無盡模式
    // =========================

    private void showMiniGameModePage() {
        BorderPane page = createSubPageBase();

        VBox leftMenu = createLeftMenu(
                createSubButton(text("menu.miniGameMode.StreetEndless"), () -> {
                    musicSystem.stopBGM();
                    SceneManager.requestStartScene(SceneType.STREET_ENDLESS);
                    fireNewGame();
                }),
                createSubButton(text("menu.miniGameMode.comingSoon"), () -> showRightContent(page, createInfoPanel(text("menu.miniGameMode.comingSoon"), text("menu.miniGameMode.comingSoon.description")))),
                createSubButton(text("menu.miniGameMode.comingSoon"), () -> showRightContent(page, createInfoPanel(text("menu.miniGameMode.comingSoon"), text("menu.miniGameMode.comingSoon.description")))),
                createSubButton(text("menu.miniGameMode.comingSoon"), () -> showRightContent(page, createInfoPanel(text("menu.miniGameMode.comingSoon"), text("menu.miniGameMode.comingSoon.description")))),
                createSubButton(text("menu.common.back"), this::closePage)
        );

        page.setLeft(leftMenu);
        page.setCenter(createInfoPanel(text("menu.miniGame"), text("menu.miniGameMode.description")
        ));

        showPage(page);
    }

    // =========================
    // 成就頁
    // =========================

    private void showAchievementPage() {
        StackPane page = new StackPane();
        page.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        Text pageTitle = createPageTitle("成就");
        StackPane.setAlignment(pageTitle, Pos.TOP_CENTER);
        StackPane.setMargin(pageTitle, new Insets(42, 0, 0, 0));

        ScrollPane achievementScroll = createAchievementScroll();
        achievementScroll.setPrefSize(SCREEN_WIDTH, 530);
        achievementScroll.setMaxSize(Double.MAX_VALUE, 530);
        achievementScroll.getStyleClass().add("settings-scroll");
        achievementScroll.getStylesheets().add(
                getClass().getResource("/style.css").toExternalForm()
        );

        StackPane.setAlignment(achievementScroll, Pos.CENTER);
        StackPane.setMargin(achievementScroll, new Insets(90, 0, 70, 0));

        StackPane backButton = createSubButton(text("menu.common.back"), this::closePage);
        StackPane.setAlignment(backButton, Pos.BOTTOM_LEFT);

        /*
         * 目前 X 值維持左側，但 Y 再偏下。
         * 如果還想更低，把 bottom 的 18 改成 8。
         */
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

        expandedAchievementCell = null;

        DeathReason[] reasons = DeathReason.values();

        for (DeathReason reason : reasons) {
            list.getChildren().add(createExpandableAchievementCell(reason));
        }

        ScrollPane scroll = new ScrollPane(list);

        /*
         * 讓 ScrollPane 吃滿左右。
         * 這樣捲軸會靠在畫面最右側。
         */
        scroll.setPrefWidth(SCREEN_WIDTH);
        scroll.setMaxWidth(Double.MAX_VALUE);

        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        /*
         * 關鍵：
         * list 的寬度綁定 ScrollPane viewport 寬度。
         * 這樣 VBox 的 Pos.TOP_CENTER 才會以整個畫面寬度置中。
         */
        list.prefWidthProperty().bind(scroll.widthProperty());

        scroll.setStyle("""
            -fx-background: transparent;
            -fx-background-color: transparent;
            -fx-padding: 0;
            """);

        list.setStyle("""
            -fx-background-color: transparent;
            """);

        return scroll;
    }

    private Node createAchievementIcon(DeathReason reason, boolean unlocked) {
        if (!unlocked) {
            Text locked = new Text("?");
            locked.setStyle("""
                -fx-font-size: 38px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);
            return locked;
        }

        String iconPath = reason.getIconPath();

        if (iconPath != null && !iconPath.isBlank()) {
            try {
                var url = getClass().getResource("/" + iconPath);

                if (url != null) {
                    ImageView imageView = new ImageView(new Image(url.toExternalForm()));
                    imageView.setFitWidth(54);
                    imageView.setFitHeight(54);
                    imageView.setPreserveRatio(true);
                    return imageView;
                }
            } catch (Exception ignored) {
            }
        }

        Text fallback = new Text("☠");
        fallback.setStyle("""
            -fx-font-size: 38px;
            -fx-fill: white;
            -fx-font-weight: bold;
            """);
        return fallback;
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

        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefSize(collapsedWidth, height);
        card.setMinSize(collapsedWidth, height);
        card.setMaxSize(collapsedWidth, height);
        card.setPadding(new Insets(0, 18, 0, 18));

        Rectangle bg = new Rectangle(collapsedWidth, height);
        bg.setArcWidth(14);
        bg.setArcHeight(14);
        bg.setFill(unlocked
                ? Color.rgb(210, 60, 60, 0.78)
                : Color.rgb(40, 40, 40, 0.88));
        bg.setStroke(Color.rgb(255, 255, 255, unlocked ? 0.9 : 0.35));
        bg.setStrokeWidth(unlocked ? 2.0 : 1.0);

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(70, height);
        iconBox.setMinSize(70, height);

        Node icon = createAchievementIcon(reason, unlocked);
        iconBox.getChildren().add(icon);

        VBox detailBox = new VBox(5);
        detailBox.setAlignment(Pos.CENTER_LEFT);
        detailBox.setOpacity(0);
        detailBox.setMouseTransparent(true);

        Text title = new Text(unlocked ? reason.getTitle() : text("Locked"));
        title.setWrappingWidth(220);
        title.setStyle("""
            -fx-font-size: 18px;
            -fx-fill: white;
            -fx-font-weight: bold;
            """);

        Text subtitle = new Text(unlocked ? reason.getSubtitle() : text("menu.achievement.description"));
        subtitle.setWrappingWidth(220);
        subtitle.setStyle("""
            -fx-font-size: 14px;
            -fx-fill: rgba(255,255,255,0.78);
            """);

        detailBox.getChildren().addAll(title, subtitle);

        StackPane cardBackgroundLayer = new StackPane(bg);
        cardBackgroundLayer.setMouseTransparent(true);
        cardBackgroundLayer.setTranslateX(8);

        card.getChildren().addAll(iconBox, detailBox);

        StackPane cardWrapper = new StackPane(cardBackgroundLayer, card);
        cardWrapper.setPrefSize(collapsedWidth, height);
        cardWrapper.setMinSize(collapsedWidth, height);
        cardWrapper.setMaxSize(collapsedWidth, height);
        cardWrapper.setAlignment(Pos.CENTER);

        /*
         * cardWrapper 自己仍然在 slot 中央，
         * 但 cardWrapper 裡面的內容靠左。
         */
        StackPane.setAlignment(cardBackgroundLayer, Pos.CENTER);
        StackPane.setAlignment(card, Pos.CENTER_LEFT);

        /*
         * 讓 HBox 的寬度跟著 cardWrapper 展開。
         * 這樣展開後 icon + detailBox 會自然貼向左側。
         */
        card.prefWidthProperty().bind(cardWrapper.widthProperty());
        card.minWidthProperty().bind(cardWrapper.widthProperty());
        card.maxWidthProperty().bind(cardWrapper.widthProperty());

        slot.getChildren().add(cardWrapper);

        cardWrapper.setUserData(false);

        slot.setOnMouseClicked(e -> {
            if (!unlocked) {
                return;
            }

            boolean currentlyExpanded = (boolean) cardWrapper.getUserData();

            if (currentlyExpanded) {
                collapseAchievementCell(cardWrapper, bg, detailBox, collapsedWidth, height);
                expandedAchievementCell = null;
                return;
            }

            if (expandedAchievementCell != null && expandedAchievementCell != cardWrapper) {
                collapseExpandedAchievementCell();
            }

            expandAchievementCell(cardWrapper, bg, detailBox, expandedWidth, height);
            expandedAchievementCell = cardWrapper;
        });

        return slot;
    }

    private void expandAchievementCell(
            StackPane cardWrapper,
            Rectangle bg,
            VBox detailBox,
            double expandedWidth,
            double height
    ) {
        cardWrapper.setUserData(true);

        Timeline widthAnim = new Timeline(
                new KeyFrame(Duration.seconds(0.22),
                        new KeyValue(bg.widthProperty(), expandedWidth, Interpolator.EASE_OUT),
                        new KeyValue(cardWrapper.prefWidthProperty(), expandedWidth, Interpolator.EASE_OUT),
                        new KeyValue(cardWrapper.minWidthProperty(), expandedWidth, Interpolator.EASE_OUT),
                        new KeyValue(cardWrapper.maxWidthProperty(), expandedWidth, Interpolator.EASE_OUT)
                )
        );

        FadeTransition detailFade = new FadeTransition(Duration.seconds(0.18), detailBox);
        detailFade.setFromValue(0);
        detailFade.setToValue(1);
        detailFade.setDelay(Duration.seconds(0.08));

        TranslateTransition detailMove = new TranslateTransition(Duration.seconds(0.22), detailBox);
        detailMove.setFromX(-12);
        detailMove.setToX(0);
        detailMove.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition anim = new ParallelTransition(
                widthAnim,
                detailFade,
                detailMove
        );

        anim.play();
    }

    private void collapseAchievementCell(
            StackPane cardWrapper,
            Rectangle bg,
            VBox detailBox,
            double collapsedWidth,
            double height
    ) {
        cardWrapper.setUserData(false);

        FadeTransition detailFade = new FadeTransition(Duration.seconds(0.08), detailBox);
        detailFade.setFromValue(detailBox.getOpacity());
        detailFade.setToValue(0);

        Timeline widthAnim = new Timeline(
                new KeyFrame(Duration.seconds(0.18),
                        new KeyValue(bg.widthProperty(), collapsedWidth, Interpolator.EASE_OUT),
                        new KeyValue(cardWrapper.prefWidthProperty(), collapsedWidth, Interpolator.EASE_OUT),
                        new KeyValue(cardWrapper.minWidthProperty(), collapsedWidth, Interpolator.EASE_OUT),
                        new KeyValue(cardWrapper.maxWidthProperty(), collapsedWidth, Interpolator.EASE_OUT)
                )
        );

        SequentialTransition anim = new SequentialTransition(
                detailFade,
                widthAnim
        );

        anim.play();
    }

    private void collapseExpandedAchievementCell() {
        if (expandedAchievementCell == null) {
            return;
        }

        StackPane cardWrapper = expandedAchievementCell;

        if (cardWrapper.getChildren().size() < 2) {
            expandedAchievementCell = null;
            return;
        }

        StackPane bgLayer = (StackPane) cardWrapper.getChildren().get(0);
        HBox card = (HBox) cardWrapper.getChildren().get(1);

        Rectangle bg = (Rectangle) bgLayer.getChildren().get(0);

        VBox detailBox = null;

        for (Node node : card.getChildren()) {
            if (node instanceof VBox) {
                detailBox = (VBox) node;
                break;
            }
        }

        if (detailBox != null) {
            collapseAchievementCell(cardWrapper, bg, detailBox, 92, 92);
        }

        expandedAchievementCell = null;
    }

    // =========================
    // 設定頁
    // =========================

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

        StackPane backButton = createSubButton(text("menu.common.back"), this::closePage);

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
        page.setCenter(createInfoPanel(text("menu.settings"), text("menu.settings.description")));

        showPage(page);
    }

    private StackPane createSettingsSideButton(String text, Runnable action) {
        double width = 240;
        double height = 46;

        StackPane button = new StackPane();
        button.setPrefSize(width, height);
        button.setMinSize(width, height);
        button.setMaxSize(width, height);
        button.setPickOnBounds(true);

        Rectangle bg = new Rectangle(width, height);
        bg.setArcWidth(10);
        bg.setArcHeight(10);
        bg.setFill(Color.rgb(0, 0, 0, 0.48));
        bg.setStroke(Color.rgb(255, 255, 255, 0.45));
        bg.setStrokeWidth(1.2);

        Text label = new Text(text);
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

        button.getChildren().addAll(bg, label, arrow);

        button.setUserData(new SettingsButtonState(bg, arrow));

        button.setOnMouseEntered(e -> {
            if (button != selectedSettingsButton) {
                audioSystem.playButtonSFX(SoundId.BUTTON_HOVER);
                bg.setFill(Color.rgb(255, 255, 255, 0.14));
            }
        });

        button.setOnMouseExited(e -> {
            if (button != selectedSettingsButton) {
                bg.setFill(Color.rgb(0, 0, 0, 0.48));
            }
        });

        button.setOnMouseClicked(e -> {
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

    private void setSettingsButtonSelected(StackPane button, boolean selected) {
        Object data = button.getUserData();

        if (!(data instanceof SettingsButtonState state)) {
            return;
        }

        Rectangle bg = state.background();
        Text arrow = state.arrow();

        arrow.setVisible(selected);

        if (selected) {
            bg.setFill(Color.rgb(255, 255, 255, 0.24));
            bg.setStroke(Color.WHITE);
            bg.setStrokeWidth(2.0);
        } else {
            bg.setFill(Color.rgb(0, 0, 0, 0.48));
            bg.setStroke(Color.rgb(255, 255, 255, 0.45));
            bg.setStrokeWidth(1.2);
        }
    }

    private record SettingsButtonState(Rectangle background, Text arrow) {
    }

    // 操作配置
    private ImageView createKeyImage(String path) {
        Image image = new Image(getClass().getResource(path).toExternalForm());

        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(50);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        return imageView;
    }

    private Text createInlineText(String text) {
        Text t = new Text(text);
        t.setStyle("""
            -fx-font-size: 22px;
            -fx-fill: rgba(255,255,255,0.86);
            """);
        return t;
    }

    private VBox createKeyConfigPanel() {
        VBox box = createPanelBox();

        HBox left = new HBox(8);
        left.setAlignment(Pos.CENTER_LEFT);
        left.getChildren().addAll(
                createKeyImage("/assets/textures/ui/keys/key-a.png"),
                createInlineText(" / "),
                createKeyImage("/assets/textures/ui/keys/key-left.png"),
                createInlineText(text("menu.settings.keyConfig.left"))
        );

        HBox right = new HBox(8);
        right.setAlignment(Pos.CENTER_LEFT);
        right.getChildren().addAll(
                createKeyImage("/assets/textures/ui/keys/key-d.png"),
                createInlineText(" / "),
                createKeyImage("/assets/textures/ui/keys/key-right.png"),
                createInlineText(text("menu.settings.keyConfig.right"))
        );

        HBox jump = new HBox(8);
        jump.setAlignment(Pos.CENTER_LEFT);
        jump.getChildren().addAll(
                createKeyImage("/assets/textures/ui/keys/key-w.png"),
                createInlineText(" / "),
                createKeyImage("/assets/textures/ui/keys/key-up.png"),
                createInlineText(" / "),
                createKeyImage("/assets/textures/ui/keys/key-space.png"),
                createInlineText(text("menu.settings.keyConfig.jump"))
        );

        HBox drop = new HBox(8);
        drop.setAlignment(Pos.CENTER_LEFT);
        drop.getChildren().addAll(
                createKeyImage("/assets/textures/ui/keys/key-s.png"),
                createInlineText(" / "),
                createKeyImage("/assets/textures/ui/keys/key-down.png"),
                createInlineText(text("menu.settings.keyConfig.drop"))
        );

        HBox interact = new HBox(8);
        interact.setAlignment(Pos.CENTER_LEFT);
        interact.getChildren().addAll(
                createKeyImage("/assets/textures/ui/keys/key-f.png"),
                createInlineText(text("menu.settings.keyConfig.interact"))
        );

        HBox dash = new HBox(8);
        dash.setAlignment(Pos.CENTER_LEFT);
        dash.getChildren().addAll(
                createKeyImage("/assets/textures/ui/keys/key-shift.png"),
                createInlineText(text("menu.settings.keyConfig.dash"))
        );

        HBox pause = new HBox(8);
        pause.setAlignment(Pos.CENTER_LEFT);
        pause.getChildren().addAll(
                createKeyImage("/assets/textures/ui/keys/key-escape.png"),
                createInlineText(text("menu.settings.keyConfig.pause"))
        );


        box.getChildren().addAll(
                left,
                right,
                jump,
                drop,
                interact,
                dash,
                pause
        );
        return box;
    }

    // 聲音設定
    @FunctionalInterface
    private interface VolumeSetter {
        void set(double value);
    }

    @FunctionalInterface
    private interface MuteSetter {
        void set(boolean muted);
    }

    private void loadVolumeIcons() {
        if (volumeIcon != null && volumeDownIcon != null && volumeMuteIcon != null) {
            return;
        }

        volumeIcon = loadImageOrNull("/assets/textures/ui/volume/volume.png");
        volumeDownIcon = loadImageOrNull("/assets/textures/ui/volume/volume-down.png");
        volumeMuteIcon = loadImageOrNull("/assets/textures/ui/volume/volume-mute.png");
    }

    private Image loadImageOrNull(String path) {
        try {
            var url = getClass().getResource(path);

            if (url == null) {
                System.out.println("Image not found: " + path);
                return null;
            }

            return new Image(url.toExternalForm());

        } catch (Exception e) {
            System.out.println("Image load failed: " + path);
            return null;
        }
    }

    private void updateVolumeIcon(ImageView iconView, double displayVolume) {
        if (iconView == null) {
            return;
        }

        if (displayVolume <= 0) {
            iconView.setImage(volumeMuteIcon);
        } else if (displayVolume <= 0.5) {
            iconView.setImage(volumeDownIcon);
        } else {
            iconView.setImage(volumeIcon);
        }
    }

    private String toPercentText(double value) {
        return Math.round(value * 100) + "%";
    }

    private void updateSliderProgressStyle(Slider slider) {
        double min = slider.getMin();
        double max = slider.getMax();
        double value = slider.getValue();

        double percent = (value - min) / (max - min) * 100.0;

        slider.lookup(".track").setStyle(String.format("""
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

    private StackPane createToggleSwitch(boolean enabled, ToggleAction action) {
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

        /*
         * 第一次建立：直接放到正確位置，不播放動畫。
         */
        updateToggleSwitch(toggle, enabled, false);

        toggle.setOnMouseClicked(e -> {
            ToggleSwitchState state = (ToggleSwitchState) toggle.getUserData();

            boolean newValue = !state.enabled();

            /*
             * 點擊切換：播放動畫。
             */
            updateToggleSwitch(toggle, newValue, true);

            if (action != null) {
                action.onToggle(newValue);
            }
        });

        return toggle;
    }

    private HBox createButtonSoundToggleRow() {
        Label nameLabel = new Label(text("menu.settings.volume.button_sound"));
        nameLabel.setMinWidth(90);
        nameLabel.setStyle("""
            -fx-font-size: 18px;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            """);

        Label stateLabel = new Label(audioSystem.isButtonSoundEnabled() ? text("menu.common.on") : text("menu.common.off"));
        stateLabel.setMinWidth(52);
        stateLabel.setStyle("""
            -fx-font-size: 17px;
            -fx-text-fill: white;
            """);

        StackPane toggleSwitch = createToggleSwitch(
                audioSystem.isButtonSoundEnabled(),
                enabled -> {
                    audioSystem.setButtonSoundEnabled(enabled);
                    stateLabel.setText(enabled ? text("menu.common.on") : text("menu.common.off"));

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

    private void updateToggleSwitch(StackPane toggle, boolean enabled, boolean animate) {
        Object data = toggle.getUserData();

        if (!(data instanceof ToggleSwitchState state)) {
            return;
        }

        Rectangle track = state.track();
        Rectangle knob = state.knob();

        double targetX = enabled ? 32 : 0;

        track.setFill(enabled
                ? Color.rgb(255, 255, 255, 0.82)
                : Color.rgb(0, 0, 0, 0.62));

        track.setStroke(Color.rgb(255, 255, 255, 0.65));
        track.setStrokeWidth(1.2);

        knob.setFill(enabled
                ? Color.rgb(213, 105, 16)
                : Color.rgb(180, 180, 180));

        if (animate) {
            TranslateTransition move = new TranslateTransition(Duration.seconds(0.14), knob);
            move.setToX(targetX);
            move.setInterpolator(Interpolator.EASE_OUT);
            move.play();
        } else {
            /*
             * 初始化時直接瞬移，不播放動畫。
             */
            knob.setTranslateX(targetX);
        }

        toggle.setUserData(new ToggleSwitchState(track, knob, enabled));
    }

    private HBox createVolumeRow(
            String name,
            double initialVolume,
            boolean initiallyMuted,
            VolumeSetter volumeSetter,
            MuteSetter muteSetter,
            Runnable onChanged
    ) {
        Label nameLabel = new Label(name);
        nameLabel.setMinWidth(90);
        nameLabel.setStyle("""
            -fx-font-size: 18px;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            """);

        ImageView iconView = new ImageView();
        iconView.setFitWidth(30);
        iconView.setFitHeight(30);
        iconView.setPreserveRatio(true);
        iconView.setSmooth(true);

        double displayVolume = initiallyMuted ? 0 : initialVolume;
        updateVolumeIcon(iconView, displayVolume);

        StackPane iconButton = new StackPane(iconView);
        iconButton.setPrefSize(38, 38);
        iconButton.setMaxSize(38, 38);
        iconButton.setPickOnBounds(true);
        iconButton.setStyle("""
            -fx-background-color: rgba(255,255,255,0.08);
            -fx-background-radius: 8;
            """);

        Slider slider = new Slider(0, 1, displayVolume);
        slider.setMaxWidth(Double.MAX_VALUE);
        slider.setPrefWidth(360);
        slider.getStyleClass().add("settings-slider");
        slider.getStylesheets().add(
                getClass().getResource("/style.css").toExternalForm()
        );

        slider.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateSliderProgressStyle(slider);
        });

        slider.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                slider.applyCss();
                updateSliderProgressStyle(slider);
            }
        });

        Label percentLabel = new Label(toPercentText(displayVolume));
        percentLabel.setMinWidth(52);
        percentLabel.setAlignment(Pos.CENTER_RIGHT);
        percentLabel.setStyle("""
            -fx-font-size: 17px;
            -fx-text-fill: white;
            """);

        final boolean[] muted = {initiallyMuted};
        final double[] lastVolumeBeforeMute = {initialVolume > 0 ? initialVolume : 1.0};

        iconButton.setOnMouseClicked(e -> {
            double sliderValue = slider.getValue();

            if (!muted[0] && sliderValue > 0) {
                lastVolumeBeforeMute[0] = sliderValue;
                muted[0] = true;

                muteSetter.set(true);
                slider.setValue(0);

                percentLabel.setText("0%");
                updateVolumeIcon(iconView, 0);

                if (onChanged != null) {
                    onChanged.run();
                }

                return;
            }

            if (muted[0]) {
                muted[0] = false;
                muteSetter.set(false);

                double restoreVolume = lastVolumeBeforeMute[0] <= 0
                        ? 1.0
                        : lastVolumeBeforeMute[0];

                volumeSetter.set(restoreVolume);
                slider.setValue(restoreVolume);

                percentLabel.setText(toPercentText(restoreVolume));
                updateVolumeIcon(iconView, restoreVolume);

                if (onChanged != null) {
                    onChanged.run();
                }
            }
        });

        slider.valueProperty().addListener((obs, oldValue, newValue) -> {
            double value = newValue.doubleValue();

            if (value > 0) {
                muted[0] = false;
                muteSetter.set(false);
                lastVolumeBeforeMute[0] = value;
            }

            if (value <= 0) {
                muted[0] = true;
                muteSetter.set(true);
            }

            volumeSetter.set(value);

            percentLabel.setText(toPercentText(value));
            updateVolumeIcon(iconView, value);

            if (onChanged != null) {
                onChanged.run();
            }
        });

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

    // 視窗大小
    private String windowModeText(WindowMode mode) {
        if (mode == null) {
            return "";
        }

        return text(mode.getTextKey());
    }

    private VBox createWindowSizePanel() {
        VBox box = createPanelBox();

        Text current = createTextBlock(text("menu.settings.window.current") + windowSystem.getCurrentLabel());

        ComboBox<WindowMode> modeBox = new ComboBox<>();
        modeBox.getItems().addAll(
                WindowMode.DEFAULT,
                WindowMode.CUSTOM,
                WindowMode.FULLSCREEN
        );
        modeBox.getStyleClass().add("settings-combo-box");
        modeBox.getStylesheets().add(
                getClass().getResource("/style.css").toExternalForm()
        );

        modeBox.setValue(windowSystem.getMode());
        modeBox.setPrefWidth(260);
        modeBox.setStyle("""
            -fx-font-size: 18px;
            """);

        modeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(WindowMode mode) {
                return windowModeText(mode);
            }

            @Override
            public WindowMode fromString(String string) {
                return null;
            }
        });

        StackPane applyButton = createSubButton(text("menu.common.apply"), () -> {
            WindowMode selectedMode = modeBox.getValue();

            windowSystem.applyMode(selectedMode);

            current.setText(text("menu.settings.window.current") + windowSystem.getCurrentLabel());
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

    // 語言系統
    private void refreshMainMenuTexts() {
        mainButtonBox.getChildren().clear();

        mainButtonBox.getChildren().addAll(
                createMenuButton(text("menu.story"), this::showStoryModePage),
                createMenuButton(text("menu.miniGame"), this::showMiniGameModePage),
                createMenuButton(text("menu.achievement"), this::showAchievementPage),
                createMenuButton(text("menu.settings"), this::showSettingsPage),
                createMenuButton(text("menu.exit"), this::requestExitGame, exitButtonStyle())
        );

        cutscene = false;
    }

    private VBox createLanguagePanel() {
        VBox box = createPanelBox();

        Text current = createTextBlock(
                text("menu.settings.language.current") + languageSystem.getCurrentLanguage()
        );

        ComboBox<Language> languageBox = new ComboBox<>();
        languageBox.getItems().addAll(Language.ZH_TW, Language.EN_US);
        languageBox.setValue(languageSystem.getCurrentLanguage());
        languageBox.getStyleClass().add("settings-combo-box");
        languageBox.getStylesheets().add(
                getClass().getResource("/style.css").toExternalForm()
        );

        languageBox.setPrefWidth(260);
        languageBox.setStyle("""
            -fx-font-size: 18px;
            """);

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

    // 重置遊戲
    private void resetSettingsToDefault() {
        // 音量設定
        audioSystem.resetSettings();
        musicSystem.applyVolume();

        // 視窗設定，如果你有 WindowSystem
        if (windowSystem != null) {
            windowSystem.resetSettings();
            windowSystem.applySavedSettings();
        }

        // 語言設定，如果你有 LanguageSystem
        if (languageSystem != null) {
            languageSystem.resetSettings();
            refreshMainMenuTexts();
            showSettingsPage();
        }
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
                        } catch (IOException e) {
                            System.out.println("Failed to delete: " + path);
                            e.printStackTrace();
                        }
                    });

        } catch (IOException e) {
            System.out.println("Failed to format local save data.");
            e.printStackTrace();
        }
    }

    private void deleteLocalData() {
        audioSystem.resetSettings();

        if (windowSystem != null) {
            windowSystem.resetSettings();
        }

        if (languageSystem != null) {
            languageSystem.resetSettings();
        }

        achievementSystem.resetAll();

        StreetEndlessRecordSystem.getInstance().reset();

        deleteLocalSaveFolder();

        if (devModeLabel != null) {
            devModeLabel.setVisible(false);
        }

        musicSystem.applyVolume();

        if (windowSystem != null) {
            windowSystem.applySavedSettings();
        }
        showSettingsPage();
    }

    private StackPane createPopupButton(String text, Runnable action) {
        return createPopupButton(text, action, defaultButtonStyle());
    }

    private StackPane createPopupButton(String text, Runnable action, ButtonStyle style) {
        StackPane button = createButtonBase(text, 120, 42, style);

        button.setOnMouseClicked(e -> {
            if (!cutscene) {
                audioSystem.playButtonSFX(SoundId.BUTTON_PRESSED);
            }

            if (action != null) {
                action.run();
            }
        });

        return button;
    }

    private void showConfirmNotice(String message, Runnable onConfirm) {
        VBox box = new VBox(18);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        box.setMaxSize(460, 180);

        Rectangle bg = new Rectangle(460, 180);
        bg.setArcWidth(18);
        bg.setArcHeight(18);
        bg.setFill(Color.rgb(0, 0, 0, 0.88));
        bg.setStroke(Color.WHITE);

        Text text = createTextBlock(message);
        text.setWrappingWidth(380);
        text.setTextAlignment(TextAlignment.CENTER);

        HBox buttons = new HBox(16);
        buttons.setAlignment(Pos.CENTER);

        StackPane confirm = createPopupButton(text("menu.common.confirm"), () -> {
            pageLayer.getChildren().remove(box.getParent());

            if (onConfirm != null) {
                onConfirm.run();
            }
        }, exitButtonStyle());

        StackPane cancel = createPopupButton(text("menu.common.cancel"), () -> {
            pageLayer.getChildren().remove(box.getParent());
        });

        buttons.getChildren().addAll(confirm, cancel);
        box.getChildren().addAll(text, buttons);

        StackPane popup = new StackPane(bg, box);
        popup.setAlignment(Pos.CENTER);

        pageLayer.getChildren().add(popup);
    }

    private VBox createResetGamePanel() {
        VBox box = createPanelBox();

        box.getChildren().addAll(
                createPageTitle(text("menu.settings.reset")),
                createTextBlock(text("menu.settings.reset.description")),

                createSubButton(text("menu.settings.reset.resetSettingsToDefault"), () -> {
                    resetSettingsToDefault();
                    showTextNotice(text("menu.settings.reset.resetSettingsToDefault.notification"));
                }),

                createSubButton(text("menu.settings.reset.clearAchievement"), () -> {
                    achievementSystem.resetAll();
                    showTextNotice(text("menu.settings.reset.clearAchievement.notification"));
                }),

                createSubButton(text("menu.settings.reset.deleteLocalData"), () -> {
                    showConfirmNotice(
                            text("menu.settings.reset.deleteLocalData.comfirmNotice"),
                            () -> {
                                deleteLocalData();
                                text("menu.settings.reset.deleteLocalData.notification");
                            }
                    );
                }, exitButtonStyle())
        );

        return box;
    }

    // 開發者模式
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

        Rectangle bg = new Rectangle(width, height);
        bg.setArcWidth(10);
        bg.setArcHeight(10);

        Text label = new Text(initialText);
        label.setStyle("""
            -fx-font-size: 22px;
            -fx-fill: white;
            -fx-font-weight: bold;
            """);

        button.getChildren().addAll(bg, label);

        SunkenButtonState state = new SunkenButtonState(bg, label, initiallyPressed);
        button.setUserData(state);

        applySunkenButtonState(button, initiallyPressed);

        button.setOnMouseClicked(e -> {
            SunkenButtonState currentState = (SunkenButtonState) button.getUserData();

            boolean newPressed = !currentState.pressed();

            button.setUserData(new SunkenButtonState(bg, label, newPressed));

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

        button.setOnMouseEntered(e -> {
            SunkenButtonState currentState = (SunkenButtonState) button.getUserData();

            if (!currentState.pressed()) {
                bg.setFill(Color.rgb(255, 255, 255, 0.18));
                label.setFill(Color.BLACK);
            }

            audioSystem.playButtonSFX(SoundId.BUTTON_HOVER);
        });

        button.setOnMouseExited(e -> {
            SunkenButtonState currentState = (SunkenButtonState) button.getUserData();
            applySunkenButtonState(button, currentState.pressed());
        });

        return button;
    }

    private void applySunkenButtonState(StackPane button, boolean pressed) {
        Object data = button.getUserData();

        if (!(data instanceof SunkenButtonState state)) {
            return;
        }

        Rectangle bg = state.background();
        Text label = state.label();

        if (pressed) {
            /*
             * 下沉狀態。
             */
            bg.setFill(Color.rgb(0, 0, 0, 0.78));
            bg.setStroke(Color.rgb(255, 255, 255, 0.95));
            bg.setStrokeWidth(2.2);

            label.setFill(Color.WHITE);

            button.setTranslateY(4);
            button.setScaleX(0.98);
            button.setScaleY(0.96);

            button.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.85)));

        } else {
            /*
             * 彈起狀態。
             */
            bg.setFill(Color.rgb(0, 0, 0, 0.58));
            bg.setStroke(Color.rgb(255, 255, 255, 0.72));
            bg.setStrokeWidth(1.4);

            label.setFill(Color.WHITE);

            button.setTranslateY(0);
            button.setScaleX(1.0);
            button.setScaleY(1.0);

            button.setEffect(null);
        }
    }

    private record SunkenButtonState(
            Rectangle background,
            Text label,
            boolean pressed
    ) {
    }

    @FunctionalInterface
    private interface TogglePressedAction {
        void onChanged(boolean pressed);
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

    // 沒用的資訊
    private StackPane createInfoCard(String title, String content) {
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

        Rectangle bg = new Rectangle(700, 120);
        bg.setArcWidth(18);
        bg.setArcHeight(18);
        bg.setFill(Color.rgb(0, 0, 0, 0.35));
        bg.setStroke(Color.rgb(255, 255, 255, 0.22));
        bg.setStrokeWidth(1.2);

        StackPane card = new StackPane(bg, textBox);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(700);

        return card;
    }

    private Text createCreditSectionTitle(String text) {
        Text title = new Text(text);
        title.setStyle("""
            -fx-font-size: 24px;
            -fx-fill: white;
            -fx-font-weight: bold;
            """);
        title.setEffect(new DropShadow(6, Color.BLACK));
        return title;
    }

    private HBox createCreditRow(String role, String name) {
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

    private ScrollPane createCreditsScroll() {
        VBox credits = new VBox(14);
        credits.setPadding(new Insets(18, 22, 18, 22));
        credits.setAlignment(Pos.TOP_LEFT);

        credits.getChildren().addAll(
                createCreditSectionTitle(text("menu.settings.about.credits")),

                createCreditRow(
                        text("menu.settings.about.credits.game_design"),
                        "Tiro"
                ),
                createCreditRow(
                        text("menu.settings.about.credits.level_design"),
                        "Tiro"
                ),
                createCreditRow(
                        text("menu.settings.about.credits.narrative_design"),
                        "Tiro"
                ),

                createCreditDivider(),

                createCreditRow(
                        text("menu.settings.about.credits.game_programming"),
                        "Tiro"
                ),
                createCreditRow(
                        text("menu.settings.about.credits.system_logic"),
                        "Tiro"
                ),
                createCreditRow(
                        text("menu.settings.about.credits.ui_programming"),
                        "Tiro"
                ),

                createCreditDivider(),

                createCreditRow(
                        text("menu.settings.about.credits.art_2d"),
                        text("menu.settings.about.credits.tiro_online_assets")
                ),
                createCreditRow(
                        text("menu.settings.about.credits.character_design"),
                        text("menu.settings.about.credits.tiro_online_assets")
                ),
                createCreditRow(
                        text("menu.settings.about.credits.animation"),
                        "Tiro"
                ),

                createCreditDivider(),

                createCreditRow(
                        text("menu.settings.about.credits.sound_design"),
                        text("menu.settings.about.credits.online_assets")
                ),
                createCreditRow(
                        text("menu.settings.about.credits.music"),
                        text("menu.settings.about.credits.online_assets")
                ),
                createCreditRow(
                        text("menu.settings.about.credits.testing"),
                        "Tiro"
                )
        );

        ScrollPane scroll = new ScrollPane(credits);
        scroll.setPrefSize(720, 270);
        scroll.setMaxSize(720, 270);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPannable(true);

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

        var css = getClass().getResource("/style.css");
        if (css != null) {
            scroll.getStylesheets().add(css.toExternalForm());
        }

        credits.setStyle("""
            -fx-background-color: transparent;
            """);

        return scroll;
    }

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

    private void showTextNotice(String text) {
        Label notice = new Label(text);
        notice.setStyle("""
                -fx-font-size: 22px;
                -fx-text-fill: white;
                -fx-background-color: rgba(0,0,0,0.8);
                -fx-padding: 20;
                """);

        StackPane popup = new StackPane(notice);
        popup.setAlignment(Pos.CENTER);

        pageLayer.getChildren().add(popup);

        PauseTransition wait = new PauseTransition(Duration.seconds(1.2));
        wait.setOnFinished(e -> pageLayer.getChildren().remove(popup));
        wait.play();
    }

    // =========================
    // 共用 UI
    // =========================

    private void createDarkOverlay() {
        darkOverlay = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);
        darkOverlay.setFill(Color.rgb(0, 0, 0, 0.5));
        darkOverlay.setOpacity(0);
        darkOverlay.setMouseTransparent(true);

        root.getChildren().add(darkOverlay);
    }

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

            boolean isLastButton = i == buttons.length - 1;

            if (isLastButton) {
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

    private void showRightContent(BorderPane page, Node content) {
        page.setCenter(content);
    }

    private VBox createInfoPanel(String title, String body) {
        VBox box = createPanelBox();
        box.getChildren().addAll(
                createPageTitle(title),
                createTextBlock(body)
        );
        return box;
    }

    private Text createPageTitle(String text) {
        Text title = new Text(text);
        title.setStyle("""
                -fx-font-size: 34px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);
        title.setEffect(new DropShadow(8, Color.BLACK));
        return title;
    }

    private Text createTextBlock(String text) {
        Text t = new Text(text);
        t.setWrappingWidth(620);
        t.setStyle("""
                -fx-font-size: 22px;
                -fx-fill: rgba(255,255,255,0.86);
                """);
        return t;
    }

    private StackPane createMenuButton(String text, Runnable action) {
        return createMenuButton(text, action, defaultButtonStyle());
    }

    private StackPane createMenuButton(String text, Runnable action, ButtonStyle style) {
        StackPane button = createButtonBase(text, 280, 58, style);

        button.setOnMouseClicked(e -> {
            if (!cutscene) {
                audioSystem.playButtonSFX(SoundId.BUTTON_PRESSED);

                if (action != null) {
                    action.run();
                }
            }
        });

        return button;
    }

    private StackPane createSubButton(String text, Runnable action) {
        return createSubButton(text, action, defaultButtonStyle());
    }

    private StackPane createSubButton(String text, Runnable action, ButtonStyle style) {
        StackPane button = createButtonBase(text, 240, 46, style);

        button.setOnMouseClicked(e -> {
            if (!cutscene) {
                audioSystem.playButtonSFX(SoundId.BUTTON_PRESSED);

                if (action != null) {
                    action.run();
                }
            }
        });

        return button;
    }

    private StackPane createButtonBase(
            String text,
            double width,
            double height,
            ButtonStyle style
    ) {
        StackPane button = new StackPane();
        button.setPrefSize(width, height);
        button.setMinSize(width, height);
        button.setMaxSize(width, height);
        button.setPickOnBounds(true);

        Rectangle bg = new Rectangle(width, height);
        bg.setArcWidth(style.arc());
        bg.setArcHeight(style.arc());
        bg.setFill(style.normalFill());
        bg.setStroke(style.normalStroke());
        bg.setStrokeWidth(style.strokeWidth());

        Text label = new Text(text);
        label.setStyle("""
            -fx-font-size: 22px;
            -fx-font-weight: bold;
            """);
        label.setFill(style.normalText());

        button.getChildren().addAll(bg, label);

        button.setOnMouseEntered(e -> {
            if (!cutscene) {
                audioSystem.playButtonSFX(SoundId.BUTTON_HOVER);
            }

            bg.setFill(style.hoverFill());
            bg.setStroke(style.hoverStroke());
            label.setFill(style.hoverText());

            ScaleTransition st = new ScaleTransition(Duration.seconds(0.08), button);
            st.setToX(style.hoverScale());
            st.setToY(style.hoverScale());
            st.play();
        });

        button.setOnMouseExited(e -> {
            bg.setFill(style.normalFill());
            bg.setStroke(style.normalStroke());
            label.setFill(style.normalText());

            ScaleTransition st = new ScaleTransition(Duration.seconds(0.08), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        button.setOnMousePressed(e -> {
            bg.setFill(style.pressedFill());
            bg.setStroke(style.pressedStroke());
            label.setFill(style.pressedText());

            button.setScaleX(0.97);
            button.setScaleY(0.97);
        });

        button.setOnMouseReleased(e -> {
            bg.setFill(style.hoverFill());
            bg.setStroke(style.hoverStroke());
            label.setFill(style.hoverText());

            button.setScaleX(style.hoverScale());
            button.setScaleY(style.hoverScale());
        });

        return button;
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

    private ImageView loadImageView(String path, double width, double height) {
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

        Rectangle fallback = new Rectangle(width, height);
        fallback.setFill(Color.rgb(20, 20, 20));

        ImageView empty = new ImageView();
        empty.setFitWidth(width);
        empty.setFitHeight(height);

        return empty;
    }

    private void resetToMainMenuFirst() {
        pageLayer.getChildren().clear();
        pageLayer.setVisible(false);
        pageLayer.setOpacity(0);

        if (darkOverlay != null) {
            darkOverlay.setOpacity(0);
        }

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

        if (devModeLabel != null) {
            devModeLabel.setVisible(Main.devMode);
        }
    }

    private void resetToMainMenuSecondary() {
        // 關閉所有子頁面
        pageLayer.getChildren().clear();
        pageLayer.setVisible(false);
        pageLayer.setOpacity(0);

        // 背景變暗遮罩還原
        if (darkOverlay != null) {
            darkOverlay.setOpacity(0);
        }

        // Logo 和主選單按鈕恢復
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

        // 開發者模式提示
        if (devModeLabel != null) {
            devModeLabel.setVisible(Main.devMode);
        }
    }

    private void showConfirmNoticeOnMainMenu(String message, Runnable onConfirm) {
        pageLayer.getChildren().clear();
        pageLayer.setVisible(true);
        pageLayer.setOpacity(1);
        pageLayer.setPickOnBounds(true);
        pageLayer.toFront();

        VBox box = new VBox(18);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        box.setMaxSize(460, 180);

        Rectangle bg = new Rectangle(460, 180);
        bg.setArcWidth(18);
        bg.setArcHeight(18);
        bg.setFill(Color.rgb(0, 0, 0, 0.88));
        bg.setStroke(Color.rgb(255, 255, 255, 0.95));
        bg.setStrokeWidth(1.8);
        bg.setEffect(new DropShadow(18, Color.rgb(0, 0, 0, 0.9)));

        Text textNode = createTextBlock(message);
        textNode.setWrappingWidth(380);
        textNode.setTextAlignment(TextAlignment.CENTER);

        HBox buttons = new HBox(16);
        buttons.setAlignment(Pos.CENTER);

        StackPane popup = new StackPane(bg, box);
        popup.setAlignment(Pos.CENTER);

        StackPane confirm = createPopupButton(
                text("menu.common.confirm"),
                () -> {
                    pageLayer.getChildren().remove(popup);

                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                },
                exitButtonStyle()
        );

        StackPane cancel = createPopupButton(
                text("menu.common.cancel"),
                () -> {
                    pageLayer.getChildren().remove(popup);
                    pageLayer.setVisible(false);
                    pageLayer.setOpacity(0);
                    pageLayer.setPickOnBounds(false);
                }
        );

        buttons.getChildren().addAll(confirm, cancel);
        box.getChildren().addAll(textNode, buttons);

        pageLayer.getChildren().add(popup);
    }

    private void requestExitGame() {
        showConfirmNoticeOnMainMenu(
                text("menu.exit.confirm"),
                () -> {
                    musicSystem.stopBGM();
                    getGameController().exit();
                }
        );
    }
}