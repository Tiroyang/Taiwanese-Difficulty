package ass.example.ui;

import ass.example.Main;
import ass.example.core.Language;
import ass.example.core.SoundId;
import ass.example.core.WindowMode;
import ass.example.system.*;
import ass.example.system.save.SaveSlotManager;
import ass.example.ui.save.SaveMenuMode;
import ass.example.ui.save.SaveSlotPanel;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.StringConverter;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGLForKtKt.getGameController;
import static com.almasb.fxgl.dsl.FXGLForKtKt.getb;

public class PauseMenu extends FXGLMenu {

    private static final double SCREEN_WIDTH = 1280;
    private static final double SCREEN_HEIGHT = 720;

    private final StackPane root = new StackPane();

    private final StackPane buttonStack = new StackPane();
    private final List<StackPane> pauseButtons = new java.util.ArrayList<>();

    private boolean menuExpanded = false;

    private final Rectangle overlay = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);

    private final VBox menuBox = new VBox(16);
    private final StackPane pageLayer = new StackPane();

    private final MusicSystem  musicSystem = MusicSystem.getInstance();
    private final AudioSystem audioSystem = AudioSystem.getInstance();
    private final LanguageSystem languageSystem = LanguageSystem.getInstance();
    private final WindowSystem windowSystem = WindowSystem.getInstance();

    private boolean animating = false;
    private StackPane selectedSubPageButton;

    public PauseMenu() {
        super(MenuType.GAME_MENU);

        root.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        root.setMinSize(0, 0);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        setupOverlay();
        setupMenuButtons();
        setupPageLayer();

        root.getChildren().addAll(
                overlay,
                menuBox,
                pageLayer
        );

        getContentRoot().getChildren().add(root);
    }

    private String text(String key) {
        return languageSystem.text(key);
    }

    @Override
    public void onCreate() {
        resetState();
        playOpenAnimation();
    }

    private StackPane createSelectablePauseButton(String text, Runnable action, PauseButtonType type) {
        final StackPane[] ref = new StackPane[1];

        ref[0] = createPauseButton(text, () -> {
            selectSubPageButton(ref[0]);

            if (action != null) {
                action.run();
            }
        }, type);

        return ref[0];
    }

    private void selectSubPageButton(StackPane button) {
        if (selectedSubPageButton != null) {
            selectedSubPageButton.getProperties().put("selected", false);
            applyPauseButtonNormalStyle(selectedSubPageButton);
        }

        selectedSubPageButton = button;
        selectedSubPageButton.getProperties().put("selected", true);
        applyPauseButtonPressedStyle(selectedSubPageButton);
    }

    private boolean isPauseButtonSelected(StackPane button) {
        Object value = button.getProperties().get("selected");
        return value instanceof Boolean && (Boolean) value;
    }

    private void applyPauseButtonNormalStyle(StackPane button) {
        Polygon bg = (Polygon) button.getProperties().get("bg");
        Text label = (Text) button.getProperties().get("label");
        PauseButtonStyle style = (PauseButtonStyle) button.getProperties().get("style");

        if (bg == null || label == null || style == null) {
            return;
        }

        bg.setFill(style.normalFill());
        bg.setStroke(style.normalStroke());
        label.setFill(style.normalText());

        button.setScaleX(1.0);
        button.setScaleY(1.0);

        TranslateTransition move = new TranslateTransition(Duration.seconds(0.08), button);
        move.setToX(0);
        move.play();
    }

    private void applyPauseButtonPressedStyle(StackPane button) {
        Polygon bg = (Polygon) button.getProperties().get("bg");
        Text label = (Text) button.getProperties().get("label");
        PauseButtonStyle style = (PauseButtonStyle) button.getProperties().get("style");

        if (bg == null || label == null || style == null) {
            return;
        }

        bg.setFill(style.pressedFill());
        bg.setStroke(style.hoverStroke());
        label.setFill(style.pressedText());

        button.setScaleX(0.98);
        button.setScaleY(0.96);

        TranslateTransition move = new TranslateTransition(Duration.seconds(0.08), button);
        move.setToX(12);
        move.play();
    }

    private void setupOverlay() {
        overlay.setFill(Color.rgb(0, 0, 0, 0.66));
        overlay.setOpacity(0);
        overlay.setMouseTransparent(false);

        overlay.widthProperty().bind(root.widthProperty());
        overlay.heightProperty().bind(root.heightProperty());
    }

    private void setupMenuButtons() {
        menuBox.setAlignment(Pos.CENTER_LEFT);
        menuBox.setPadding(new Insets(0, 0, 0, 64));
        menuBox.setTranslateX(-460);
        menuBox.setOpacity(0);

        Text title = new Text("PAUSED");
        title.setStyle("""
            -fx-font-size: 52px;
            -fx-fill: white;
            -fx-font-weight: bold;
            """);
        title.setEffect(new DropShadow(10, Color.WHITE));

        StackPane titleBox = new StackPane(title);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(0, 0, 18, 8));

        buttonStack.setPrefSize(380, 330);
        buttonStack.setMinSize(380, 330);
        buttonStack.setMaxSize(380, 330);
        buttonStack.setPickOnBounds(false);
        buttonStack.setAlignment(Pos.CENTER_LEFT);

        pauseButtons.clear();

        pauseButtons.add(createPauseButton(text("pause.resume"), this::resumeGame, PauseButtonType.PRIMARY));
        pauseButtons.add(createPauseButton(text("pause.save"), this::showSaveMenuPage, PauseButtonType.NORMAL));
        pauseButtons.add(createPauseButton(text("pause.settings"), this::showSettingsPage, PauseButtonType.NORMAL));
        pauseButtons.add(createPauseButton(text("pause.exitToMain"), this::exitToMainMenu, PauseButtonType.EXIT));

        for (StackPane button : pauseButtons) {
            StackPane.setAlignment(button, Pos.CENTER_LEFT);
            buttonStack.getChildren().add(button);
        }

        menuBox.getChildren().addAll(
                titleBox,
                buttonStack
        );

        StackPane.setAlignment(menuBox, Pos.CENTER_LEFT);
    }

    private void setupPageLayer() {
        pageLayer.setVisible(false);
        pageLayer.setOpacity(0);
        pageLayer.setPickOnBounds(false);

        StackPane.setAlignment(pageLayer, Pos.CENTER);
    }

    private void resetState() {
        animating = false;
        menuExpanded = false;

        overlay.setOpacity(0);

        menuBox.setTranslateX(-460);
        menuBox.setOpacity(0);
        menuBox.setVisible(true);

        for (StackPane button : pauseButtons) {
            button.setTranslateX(0);
            button.setTranslateY(0);
            button.setOpacity(0.95);
            button.setScaleX(0.96);
            button.setScaleY(0.96);
            button.setDisable(true);
        }

        pageLayer.getChildren().clear();
        pageLayer.setVisible(false);
        pageLayer.setOpacity(0);
        pageLayer.setPickOnBounds(false);
    }

    private void playOpenAnimation() {
        FadeTransition overlayFade = new FadeTransition(Duration.seconds(0.22), overlay);
        overlayFade.setFromValue(0);
        overlayFade.setToValue(1);

        FadeTransition menuFade = new FadeTransition(Duration.seconds(0.24), menuBox);
        menuFade.setFromValue(0);
        menuFade.setToValue(1);

        TranslateTransition menuSlide = new TranslateTransition(Duration.seconds(0.30), menuBox);
        menuSlide.setFromX(-460);
        menuSlide.setToX(0);
        menuSlide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition slideIn = new ParallelTransition(
                overlayFade,
                menuFade,
                menuSlide
        );

        slideIn.setOnFinished(e -> playButtonExpandAnimation());

        slideIn.play();
    }

    private void playButtonExpandAnimation() {
        ParallelTransition allButtonsAnim = new ParallelTransition();

        double gap = 74;

        /*
         * 4 顆按鈕時：
         * i = 0 -> -1.5 gap
         * i = 1 -> -0.5 gap
         * i = 2 -> +0.5 gap
         * i = 3 -> +1.5 gap
         *
         * 這樣會剛好以 Y 軸中心對稱攤開。
         */
        double center = (pauseButtons.size() - 1) / 2.0;

        for (int i = 0; i < pauseButtons.size(); i++) {
            StackPane button = pauseButtons.get(i);

            double targetY = (i - center) * gap;

            TranslateTransition moveY = new TranslateTransition(Duration.seconds(0.20), button);
            moveY.setFromY(0);
            moveY.setToY(targetY);
            moveY.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition fade = new FadeTransition(Duration.seconds(0.14), button);
            fade.setFromValue(button.getOpacity());
            fade.setToValue(1.0);

            ScaleTransition scale = new ScaleTransition(Duration.seconds(0.20), button);
            scale.setFromX(0.96);
            scale.setFromY(0.96);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition buttonAnim = new ParallelTransition(
                    moveY,
                    fade,
                    scale
            );

            allButtonsAnim.getChildren().add(buttonAnim);
        }

        allButtonsAnim.setOnFinished(e -> {
            menuExpanded = true;

            for (StackPane button : pauseButtons) {
                button.setDisable(false);
            }
        });

        allButtonsAnim.play();
    }

    private void resumeGame() {
        if (animating) {
            return;
        }

        animating = true;

        FadeTransition overlayFade = new FadeTransition(Duration.seconds(0.16), overlay);
        overlayFade.setFromValue(overlay.getOpacity());
        overlayFade.setToValue(0);

        FadeTransition menuFade = new FadeTransition(Duration.seconds(0.14), menuBox);
        menuFade.setFromValue(menuBox.getOpacity());
        menuFade.setToValue(0);

        TranslateTransition menuSlide = new TranslateTransition(Duration.seconds(0.18), menuBox);
        menuSlide.setFromX(menuBox.getTranslateX());
        menuSlide.setToX(-420);
        menuSlide.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition close = new ParallelTransition(
                overlayFade,
                menuFade,
                menuSlide
        );

        close.setOnFinished(e -> {
            animating = false;
            fireResume();
        });

        close.play();
    }

    private void showSaveMenuPage() {
        if (getb("saveDisabled")) {
            showTextNotice(text("pause.save.disabled"));
            return;
        }

        selectedSubPageButton = null;

        BorderPane page = new BorderPane();
        page.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        VBox left = new VBox(14);
        left.setAlignment(Pos.TOP_LEFT);
        left.setPadding(new Insets(120, 0, 60, 56));
        left.setPrefWidth(340);

        SaveSystem saveSystem = SaveSystem.getInstance();

        left.getChildren().addAll(
                createSelectablePauseButton(text("save.quickSave"), () -> {
                    if (saveSystem != null) {
                        SaveSlotManager.getInstance().quickSave(saveSystem, root);
                        showTextNotice(text("save.quickSave.done"));
                    }
                }, PauseButtonType.PRIMARY),

                createSelectablePauseButton(text("save.saveTo"), () -> {
                    page.setCenter(new SaveSlotPanel(
                            SaveMenuMode.SAVE_TO,
                            saveSystem,
                            null,
                            () -> showTextNotice(text("save.saved")),
                            root
                    ));
                }, PauseButtonType.NORMAL),

                createSelectablePauseButton(text("save.load"), () -> {
                    page.setCenter(new SaveSlotPanel(
                            SaveMenuMode.LOAD,
                            saveSystem,
                            null,
                            () -> {
                                showTextNotice(text("save.loaded"));
                                resumeGame();
                            }
                    ));
                }, PauseButtonType.NORMAL),

                createSelectablePauseButton(text("save.edit"), () -> {
                    page.setCenter(new SaveSlotPanel(
                            SaveMenuMode.EDIT,
                            saveSystem,
                            null,
                            null
                    ));
                }, PauseButtonType.NORMAL),

                createPauseButton(text("menu.common.back"), this::closeSubPage, PauseButtonType.PRIMARY)
        );

        page.setLeft(left);
        page.setCenter(createSaveInfoPanel());

        pageLayer.getChildren().clear();
        pageLayer.getChildren().add(page);
        pageLayer.setVisible(true);
        pageLayer.setPickOnBounds(true);

        FadeTransition pageFade = new FadeTransition(Duration.seconds(0.18), pageLayer);
        pageFade.setFromValue(0);
        pageFade.setToValue(1);

        FadeTransition menuFade = new FadeTransition(Duration.seconds(0.12), menuBox);
        menuFade.setFromValue(menuBox.getOpacity());
        menuFade.setToValue(0);

        TranslateTransition menuSlide = new TranslateTransition(Duration.seconds(0.18), menuBox);
        menuSlide.setFromX(0);
        menuSlide.setToX(-260);
        menuSlide.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition transition = new ParallelTransition(
                pageFade,
                menuFade,
                menuSlide
        );

        transition.setOnFinished(e -> menuBox.setVisible(false));
        transition.play();
    }

    private VBox createSaveInfoPanel() {
        VBox box = createPanelBox();

        box.getChildren().addAll(
                createPageTitle(text("pause.save")),
                createTextBlock(text("save.description"))
        );

        return box;
    }

    // =========================
    // Settings Page
    // =========================

    private void showSettingsPage() {
        selectedSubPageButton = null;

        BorderPane page = new BorderPane();
        page.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        VBox left = new VBox(14);
        left.setAlignment(Pos.TOP_LEFT);
        left.setPadding(new Insets(120, 0, 60, 56));
        left.setPrefWidth(340);

        left.getChildren().addAll(
                createSelectablePauseButton(text("menu.settings.KeyConfig"), () -> page.setCenter(createKeyConfigPanel()), PauseButtonType.NORMAL),
                createSelectablePauseButton(text("menu.settings.volume"), () -> page.setCenter(createVolumePanel()), PauseButtonType.NORMAL),
                createSelectablePauseButton(text("menu.settings.window"), () -> page.setCenter(createWindowPanel()), PauseButtonType.NORMAL),
                createSelectablePauseButton(text("menu.settings.language"), () -> page.setCenter(createLanguagePanel()), PauseButtonType.NORMAL),
                createPauseButton(text("menu.common.back"), this::closeSubPage, PauseButtonType.PRIMARY)
        );

        page.setLeft(left);
        page.setCenter(createSettingsInfoPanel());

        pageLayer.getChildren().clear();
        pageLayer.getChildren().add(page);
        pageLayer.setVisible(true);
        pageLayer.setPickOnBounds(true);

        FadeTransition pageFade = new FadeTransition(Duration.seconds(0.18), pageLayer);
        pageFade.setFromValue(0);
        pageFade.setToValue(1);

        FadeTransition menuFade = new FadeTransition(Duration.seconds(0.12), menuBox);
        menuFade.setFromValue(menuBox.getOpacity());
        menuFade.setToValue(0);

        TranslateTransition menuSlide = new TranslateTransition(Duration.seconds(0.18), menuBox);
        menuSlide.setFromX(0);
        menuSlide.setToX(-260);
        menuSlide.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition transition = new ParallelTransition(
                pageFade,
                menuFade,
                menuSlide
        );

        transition.setOnFinished(e -> menuBox.setVisible(false));
        transition.play();
    }

    private void showSubPage(BorderPane page) {
        pageLayer.getChildren().clear();
        pageLayer.getChildren().add(page);
        pageLayer.setVisible(true);
        pageLayer.setPickOnBounds(true);

        FadeTransition pageFade = new FadeTransition(Duration.seconds(0.18), pageLayer);
        pageFade.setFromValue(0);
        pageFade.setToValue(1);

        FadeTransition menuFade = new FadeTransition(Duration.seconds(0.12), menuBox);
        menuFade.setFromValue(menuBox.getOpacity());
        menuFade.setToValue(0);

        TranslateTransition menuSlide = new TranslateTransition(Duration.seconds(0.18), menuBox);
        menuSlide.setFromX(0);
        menuSlide.setToX(-260);
        menuSlide.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition transition = new ParallelTransition(
                pageFade,
                menuFade,
                menuSlide
        );

        transition.setOnFinished(e -> menuBox.setVisible(false));
        transition.play();
    }

    private void closeSubPage() {
        menuBox.setVisible(true);

        FadeTransition pageFade = new FadeTransition(Duration.seconds(0.14), pageLayer);
        pageFade.setFromValue(pageLayer.getOpacity());
        pageFade.setToValue(0);

        FadeTransition menuFade = new FadeTransition(Duration.seconds(0.18), menuBox);
        menuFade.setFromValue(0);
        menuFade.setToValue(1);

        TranslateTransition menuSlide = new TranslateTransition(Duration.seconds(0.22), menuBox);
        menuSlide.setFromX(-260);
        menuSlide.setToX(0);
        menuSlide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition transition = new ParallelTransition(
                pageFade,
                menuFade,
                menuSlide
        );

        transition.setOnFinished(e -> {
            pageLayer.getChildren().clear();
            pageLayer.setVisible(false);
            pageLayer.setPickOnBounds(false);
        });

        transition.play();
    }

    private VBox createSettingsInfoPanel() {
        VBox box = createPanelBox();
        box.getChildren().addAll(
                createPageTitle(text("menu.settings")),
                createTextBlock(text("menu.settings.description"))
        );
        return box;
    }

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

    private VBox createVolumePanel() {
        VBox box = createPanelBox();

        box.getChildren().addAll(
                createPageTitle(text("menu.settings.volume")),

                createVolumeRow(
                        text("menu.settings.volume.global"),
                        audioSystem.getMasterVolume(),
                        value -> {
                            audioSystem.setMasterVolume(value);
                            MusicSystem.getInstance().applyVolume();
                        }
                ),

                createVolumeRow(
                        text("menu.settings.volume.music"),
                        audioSystem.getMusicVolume(),
                        value -> {
                            audioSystem.setMusicVolume(value);
                            MusicSystem.getInstance().applyVolume();
                        }
                ),

                createVolumeRow(
                        text("menu.settings.volume.sound"),
                        audioSystem.getSfxVolume(),
                        audioSystem::setSfxVolume
                )
        );

        return box;
    }

    private HBox createVolumeRow(String name, double initialValue, VolumeSetter setter) {
        Label label = new Label(name);
        label.setMinWidth(120);
        label.setStyle("""
                -fx-font-size: 19px;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                """);

        Slider slider = new Slider(0, 1, initialValue);
        slider.setPrefWidth(360);
        slider.getStyleClass().add("settings-slider");

        var css = getClass().getResource("/style.css");
        if (css != null) {
            slider.getStylesheets().add(css.toExternalForm());
        }

        Label value = new Label(Math.round(initialValue * 100) + "%");
        value.setMinWidth(60);
        value.setStyle("""
                -fx-font-size: 18px;
                -fx-text-fill: white;
                """);

        slider.valueProperty().addListener((obs, oldV, newV) -> {
            double v = newV.doubleValue();
            setter.set(v);
            value.setText(Math.round(v * 100) + "%");
        });

        HBox row = new HBox(16, label, slider, value);
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

    private VBox createWindowPanel() {
        VBox box = createPanelBox();

        Text current = createTextBlock(text("menu.settings.window.current") + windowSystem.getCurrentLabel());

        ComboBox<WindowMode> modeBox = new ComboBox<>();
        modeBox.getItems().addAll(
                WindowMode.DEFAULT,
                WindowMode.CUSTOM,
                WindowMode.FULLSCREEN
        );

        modeBox.setValue(windowSystem.getMode());
        modeBox.setPrefWidth(280);
        modeBox.getStyleClass().add("settings-combo-box");

        var css = getClass().getResource("/style.css");
        if (css != null) {
            modeBox.getStylesheets().add(css.toExternalForm());
        }

        modeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(WindowMode mode) {
                if (mode == null) {
                    return "";
                }
                return text(mode.getTextKey());
            }

            @Override
            public WindowMode fromString(String string) {
                return null;
            }
        });

        StackPane apply = createSubButton(text("menu.common.apply"), () -> {
            windowSystem.applyMode(modeBox.getValue());
            current.setText(text("menu.settings.window.current") + windowSystem.getCurrentLabel());
        });

        box.getChildren().addAll(
                createPageTitle(text("menu.settings.window")),
                current,
                modeBox,
                apply
        );

        return box;
    }

    private void refreshPauseMenuTexts() {
        buttonStack.getChildren().clear();
        pauseButtons.clear();

        pauseButtons.add(createPauseButton(text("pause.resume"), this::resumeGame, PauseButtonType.PRIMARY));
        pauseButtons.add(createPauseButton(text("pause.save"), this::showSaveMenuPage, PauseButtonType.NORMAL));
        pauseButtons.add(createPauseButton(text("pause.settings"), this::showSettingsPage, PauseButtonType.NORMAL));
        pauseButtons.add(createPauseButton(text("pause.exitToMain"), this::exitToMainMenu, PauseButtonType.EXIT));

        for (StackPane button : pauseButtons) {
            StackPane.setAlignment(button, Pos.CENTER_LEFT);
            buttonStack.getChildren().add(button);
        }

        /*
         * 如果是在暫停選單已經展開後切換語言，
         * 要重新把按鈕放回展開位置。
         */
        double gap = 74;
        double center = (pauseButtons.size() - 1) / 2.0;

        for (int i = 0; i < pauseButtons.size(); i++) {
            StackPane button = pauseButtons.get(i);

            double targetY = (i - center) * gap;

            button.setTranslateY(targetY);
            button.setTranslateX(0);
            button.setOpacity(1.0);
            button.setScaleX(1.0);
            button.setScaleY(1.0);
            button.setDisable(false);
        }

        menuExpanded = true;
    }

    private VBox createLanguagePanel() {
        VBox box = createPanelBox();

        Text current = createTextBlock(
                text("menu.settings.language.current") + languageSystem.getCurrentLanguage()
        );

        ComboBox<Language> languageBox = new ComboBox<>();
        languageBox.getItems().addAll(Language.ZH_TW, Language.EN_US);
        languageBox.setValue(languageSystem.getCurrentLanguage());
        languageBox.setPrefWidth(280);
        languageBox.getStyleClass().add("settings-combo-box");

        var css = getClass().getResource("/style.css");
        if (css != null) {
            languageBox.getStylesheets().add(css.toExternalForm());
        }

        StackPane apply = createSubButton(text("menu.common.apply"), () -> {
            languageSystem.setLanguage(languageBox.getValue());
            showTextNotice(text("menu.settings.language.changed"));
            refreshPauseMenuTexts();
            closeSubPage();
        });

        box.getChildren().addAll(
                createPageTitle(text("menu.settings.language")),
                current,
                languageBox,
                apply
        );

        return box;
    }

    private StackPane createPopupButton(String text, Runnable action, boolean danger) {
        double width = 130;
        double height = 42;

        StackPane button = new StackPane();
        button.setPrefSize(width, height);
        button.setMinSize(width, height);
        button.setMaxSize(width, height);
        button.setPickOnBounds(true);

        Rectangle bg = new Rectangle(width, height);
        bg.setArcWidth(10);
        bg.setArcHeight(10);

        if (danger) {
            bg.setFill(Color.rgb(100, 0, 0, 0.78));
            bg.setStroke(Color.rgb(255, 140, 140, 0.82));
        } else {
            bg.setFill(Color.rgb(0, 0, 0, 0.58));
            bg.setStroke(Color.rgb(255, 255, 255, 0.72));
        }

        bg.setStrokeWidth(1.4);

        Text label = new Text(text);
        label.setStyle("""
            -fx-font-size: 20px;
            -fx-fill: white;
            -fx-font-weight: bold;
            """);

        button.getChildren().addAll(bg, label);

        button.setOnMouseEntered(e -> {
            audioSystem.playButtonSFX(SoundId.BUTTON_HOVER);

            if (danger) {
                bg.setFill(Color.rgb(185, 25, 25, 0.95));
                bg.setStroke(Color.WHITE);
                label.setFill(Color.WHITE);
            } else {
                bg.setFill(Color.rgb(255, 255, 255, 0.58));
                bg.setStroke(Color.WHITE);
                label.setFill(Color.BLACK);
            }

            ScaleTransition st = new ScaleTransition(Duration.seconds(0.08), button);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });

        button.setOnMouseExited(e -> {
            if (danger) {
                bg.setFill(Color.rgb(100, 0, 0, 0.78));
                bg.setStroke(Color.rgb(255, 140, 140, 0.82));
                label.setFill(Color.WHITE);
            } else {
                bg.setFill(Color.rgb(0, 0, 0, 0.58));
                bg.setStroke(Color.rgb(255, 255, 255, 0.72));
                label.setFill(Color.WHITE);
            }

            ScaleTransition st = new ScaleTransition(Duration.seconds(0.08), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        button.setOnMousePressed(e -> {
            button.setScaleX(0.97);
            button.setScaleY(0.96);
        });

        button.setOnMouseReleased(e -> {
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });

        button.setOnMouseClicked(e -> {
            if (action != null) {
                action.run();
            }
        });

        return button;
    }

    private void showExitToMainConfirm() {
        StackPane popupLayer = new StackPane();
        popupLayer.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        popupLayer.setPickOnBounds(true);

        Rectangle dim = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);
        dim.widthProperty().bind(root.widthProperty());
        dim.heightProperty().bind(root.heightProperty());
        dim.setFill(Color.rgb(0, 0, 0, 0.48));

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(28));
        content.setMaxSize(500, 230);

        Rectangle bg = new Rectangle(500, 230);
        bg.setArcWidth(20);
        bg.setArcHeight(20);
        bg.setFill(Color.rgb(0, 0, 0, 0.9));
        bg.setStroke(Color.rgb(255, 255, 255, 0.85));
        bg.setStrokeWidth(1.8);
        bg.setEffect(new DropShadow(18, Color.rgb(0, 0, 0, 0.9)));

        Text title = new Text(text("pause.exitToMain"));
        title.setStyle("""
            -fx-font-size: 30px;
            -fx-fill: white;
            -fx-font-weight: bold;
            """);
        title.setEffect(new DropShadow(8, Color.BLACK));

        Text message = new Text(text("pause.exitToMain.confirm"));
        message.setWrappingWidth(410);
        message.setStyle("""
            -fx-font-size: 20px;
            -fx-fill: rgba(255,255,255,0.86);
            """);

        HBox buttons = new HBox(18);
        buttons.setAlignment(Pos.CENTER);

        StackPane confirmButton = createPopupButton(
                text("menu.common.confirm"),
                () -> {
                    audioSystem.playButtonSFX(SoundId.BUTTON_PRESSED);
                    root.getChildren().remove(popupLayer);

                    musicSystem.stopBGM();

                    /*
                     * 重點：
                     * 不要用 fireExitToMainMenu()，
                     * 否則可能觸發 FXGL 內建確認視窗。
                     */
                    getGameController().gotoMainMenu();
                },
                true
        );

        StackPane cancelButton = createPopupButton(
                text("menu.common.cancel"),
                () -> {
                    audioSystem.playButtonSFX(SoundId.BUTTON_PRESSED);
                    root.getChildren().remove(popupLayer);
                },
                false
        );

        buttons.getChildren().addAll(confirmButton, cancelButton);
        content.getChildren().addAll(title, message, buttons);

        StackPane card = new StackPane(bg, content);
        card.setAlignment(Pos.CENTER);

        popupLayer.getChildren().addAll(dim, card);
        StackPane.setAlignment(card, Pos.CENTER);

        root.getChildren().add(popupLayer);

        popupLayer.setOpacity(0);
        card.setScaleX(0.92);
        card.setScaleY(0.92);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.16), popupLayer);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.16), card);
        scale.setFromX(0.92);
        scale.setFromY(0.92);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, scale).play();
    }

    private void exitToMainMenu() {
        showExitToMainConfirm();
    }

    // =========================
    // UI
    // =========================

    private StackPane createPauseButton(String text, Runnable action, PauseButtonType type) {
        double width = 340;
        double height = 58;
        double cut = 30;

        StackPane button = new StackPane();
        button.setPrefSize(width, height);
        button.setMinSize(width, height);
        button.setMaxSize(width, height);
        button.setPickOnBounds(true);

        Polygon bg = new Polygon(
                cut, 0,
                width, 0,
                width - cut, height,
                0, height
        );

        PauseButtonStyle style = getPauseButtonStyle(type);

        bg.setFill(style.normalFill());
        bg.setStroke(style.normalStroke());
        bg.setStrokeWidth(1.6);

        Text label = new Text(text);
        label.setStyle("""
                -fx-font-size: 23px;
                -fx-font-weight: bold;
                """);
        label.setFill(style.normalText());
        label.setEffect(new DropShadow(5, Color.BLACK));

        StackPane.setAlignment(label, Pos.CENTER_LEFT);
        StackPane.setMargin(label, new Insets(0, 0, 0, 42));

        button.getChildren().addAll(bg, label);
        button.getProperties().put("bg", bg);
        button.getProperties().put("label", label);
        button.getProperties().put("style", style);
        button.getProperties().put("selected", false);

        button.setOnMouseEntered(e -> {
            audioSystem.playButtonSFX(SoundId.BUTTON_HOVER);

            if (!isPauseButtonSelected(button)) {
                bg.setFill(style.hoverFill());
                bg.setStroke(style.hoverStroke());
                label.setFill(style.hoverText());

                TranslateTransition move = new TranslateTransition(Duration.seconds(0.08), button);
                move.setToX(12);
                move.play();
            }
        });

        button.setOnMouseExited(e -> {
            if (isPauseButtonSelected(button)) {
                applyPauseButtonPressedStyle(button);
            } else {
                bg.setFill(style.normalFill());
                bg.setStroke(style.normalStroke());
                label.setFill(style.normalText());

                button.setScaleX(1.0);
                button.setScaleY(1.0);

                TranslateTransition move = new TranslateTransition(Duration.seconds(0.08), button);
                move.setToX(0);
                move.play();
            }


        });

        button.setOnMousePressed(e -> {
            bg.setFill(style.pressedFill());
            label.setFill(style.pressedText());
            button.setScaleX(0.98);
            button.setScaleY(0.96);
        });

        button.setOnMouseReleased(e -> {
            if (isPauseButtonSelected(button)) {
                applyPauseButtonPressedStyle(button);
            } else {
                bg.setFill(style.hoverFill());
                label.setFill(style.hoverText());
                button.setScaleX(1.0);
                button.setScaleY(1.0);
            }
        });

        button.setOnMouseClicked(e -> {
            if (!menuExpanded) {
                return;
            }

            audioSystem.playButtonSFX(SoundId.BUTTON_PRESSED);

            if (action != null) {
                action.run();
            }
        });

        return button;
    }

    private VBox createPanelBox() {
        VBox box = new VBox(18);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(120, 90, 70, 40));
        return box;
    }

    private Text createPageTitle(String value) {
        Text title = new Text(value);
        title.setStyle("""
                -fx-font-size: 36px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);
        title.setEffect(new DropShadow(8, Color.BLACK));
        return title;
    }

    private Text createTextBlock(String value) {
        Text t = new Text(value);
        t.setWrappingWidth(620);
        t.setStyle("""
                -fx-font-size: 22px;
                -fx-fill: rgba(255,255,255,0.86);
                """);
        return t;
    }

    private void showTextNotice(String message) {
        Label notice = new Label(message);
        notice.setStyle("""
                -fx-font-size: 21px;
                -fx-text-fill: white;
                -fx-background-color: rgba(0,0,0,0.84);
                -fx-background-radius: 12;
                -fx-padding: 16 24 16 24;
                """);

        StackPane popup = new StackPane(notice);
        popup.setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setMargin(popup, new Insets(0, 0, 56, 0));

        root.getChildren().add(popup);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.12), popup);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition stay = new PauseTransition(Duration.seconds(1.2));

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.16), popup);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(fadeIn, stay, fadeOut);
        seq.setOnFinished(e -> root.getChildren().remove(popup));
        seq.play();
    }

    private VBox createSubPageLeftMenu(Node... buttons) {
        VBox left = new VBox(14);
        left.setAlignment(Pos.TOP_LEFT);
        left.setPadding(new Insets(90, 0, 48, 56));
        left.setPrefWidth(340);
        left.setMinWidth(340);
        left.setMaxWidth(340);

        left.setBackground(new Background(new BackgroundFill(
                Color.rgb(213, 105, 16, 0.72),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        left.setBorder(new Border(new BorderStroke(
                Color.rgb(255, 255, 255, 0.18),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 1.5, 0, 0)
        )));

        VBox topButtons = new VBox(14);
        topButtons.setAlignment(Pos.TOP_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        for (int i = 0; i < buttons.length; i++) {
            if (i == buttons.length - 1) {
                left.getChildren().addAll(topButtons, spacer, buttons[i]);
            } else {
                topButtons.getChildren().add(buttons[i]);
            }
        }

        return left;
    }

    private StackPane createSubButton(String text, Runnable action) {
        return createSubButton(text, action, false);
    }

    private StackPane createSubButton(String text, Runnable action, boolean exit) {
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

        if (exit) {
            bg.setFill(Color.rgb(80, 0, 0, 0.74));
            bg.setStroke(Color.rgb(255, 120, 120, 0.58));
        } else {
            bg.setFill(Color.rgb(0, 0, 0, 0.58));
            bg.setStroke(Color.rgb(255, 255, 255, 0.72));
        }

        bg.setStrokeWidth(1.4);

        Text label = new Text(text);
        label.setStyle("""
            -fx-font-size: 22px;
            -fx-font-weight: bold;
            """);
        label.setFill(Color.WHITE);

        button.getChildren().addAll(bg, label);

        button.setOnMouseEntered(e -> {
            audioSystem.playButtonSFX(SoundId.BUTTON_HOVER);

            if (exit) {
                bg.setFill(Color.rgb(185, 25, 25, 0.95));
                label.setFill(Color.WHITE);
            } else {
                bg.setFill(Color.rgb(255, 255, 255, 0.58));
                label.setFill(Color.BLACK);
            }

            ScaleTransition st = new ScaleTransition(Duration.seconds(0.08), button);
            st.setToX(1.04);
            st.setToY(1.04);
            st.play();
        });

        button.setOnMouseExited(e -> {
            if (exit) {
                bg.setFill(Color.rgb(80, 0, 0, 0.74));
                bg.setStroke(Color.rgb(255, 120, 120, 0.58));
            } else {
                bg.setFill(Color.rgb(0, 0, 0, 0.58));
                bg.setStroke(Color.rgb(255, 255, 255, 0.72));
            }

            label.setFill(Color.WHITE);

            ScaleTransition st = new ScaleTransition(Duration.seconds(0.08), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        button.setOnMousePressed(e -> {
            button.setScaleX(0.97);
            button.setScaleY(0.97);
        });

        button.setOnMouseReleased(e -> {
            button.setScaleX(1.04);
            button.setScaleY(1.04);
        });

        button.setOnMouseClicked(e -> {
            audioSystem.playButtonSFX(SoundId.BUTTON_PRESSED);

            if (action != null) {
                action.run();
            }
        });

        return button;
    }

    private enum PauseButtonType {
        NORMAL,
        PRIMARY,
        EXIT
    }

    private record PauseButtonStyle(
            Color normalFill,
            Color normalStroke,
            Color normalText,
            Color hoverFill,
            Color hoverStroke,
            Color hoverText,
            Color pressedFill,
            Color pressedText
    ) {
    }

    private PauseButtonStyle getPauseButtonStyle(PauseButtonType type) {
        return switch (type) {
            case PRIMARY -> new PauseButtonStyle(
                    Color.rgb(213, 105, 16, 0.82),
                    Color.rgb(255, 255, 255, 0.62),
                    Color.WHITE,

                    Color.rgb(245, 135, 35, 0.96),
                    Color.WHITE,
                    Color.WHITE,

                    Color.rgb(120, 48, 0, 0.96),
                    Color.WHITE
            );

            case EXIT -> new PauseButtonStyle(
                    Color.rgb(80, 0, 0, 0.74),
                    Color.rgb(255, 120, 120, 0.58),
                    Color.WHITE,

                    Color.rgb(185, 25, 25, 0.95),
                    Color.WHITE,
                    Color.WHITE,

                    Color.rgb(60, 0, 0, 0.96),
                    Color.WHITE
            );

            default -> new PauseButtonStyle(
                    Color.rgb(0, 0, 0, 0.62),
                    Color.rgb(255, 255, 255, 0.5),
                    Color.WHITE,

                    Color.rgb(255, 255, 255, 0.26),
                    Color.WHITE,
                    Color.WHITE,

                    Color.rgb(0, 0, 0, 0.82),
                    Color.WHITE
            );
        };
    }

    @FunctionalInterface
    private interface VolumeSetter {
        void set(double value);
    }
}