package ass.example.ui.save;

import ass.example.system.LanguageSystem;
import ass.example.system.SaveRequestSystem;
import ass.example.system.SaveSystem;
import ass.example.system.save.SaveSlotData;
import ass.example.system.save.SaveSlotManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class SaveSlotPanel extends VBox {

    private final SaveMenuMode mode;
    private final SaveSlotManager saveSlotManager = SaveSlotManager.getInstance();
    private final LanguageSystem languageSystem = LanguageSystem.getInstance();

    private final SaveSystem saveSystem;
    private final Consumer<Integer> onLoadFromMainMenu;
    private final Runnable onAfterAction;

    private final GridPane grid = new GridPane();

    private final Node nodeToHideBeforeScreenshot;

    public SaveSlotPanel(
            SaveMenuMode mode,
            SaveSystem saveSystem,
            Consumer<Integer> onLoadFromMainMenu,
            Runnable onAfterAction
    ) {
        this(
                mode,
                saveSystem,
                onLoadFromMainMenu,
                onAfterAction,
                null
        );
    }

    public SaveSlotPanel(
            SaveMenuMode mode,
            SaveSystem saveSystem,
            Consumer<Integer> onLoadFromMainMenu,
            Runnable onAfterAction,
            Node nodeToHideBeforeScreenshot
    ) {
        this.mode = mode;
        this.saveSystem = saveSystem;
        this.onLoadFromMainMenu = onLoadFromMainMenu;
        this.onAfterAction = onAfterAction;
        this.nodeToHideBeforeScreenshot = nodeToHideBeforeScreenshot;

        setup();
        refreshSlots();
    }

    private String text(String key) {
        return languageSystem.text(key);
    }

    private void setup() {
        setAlignment(Pos.TOP_LEFT);
        setPadding(new Insets(70, 80, 70, 40));
        setSpacing(22);

        Text title = new Text(getTitleText());
        title.setStyle("""
                -fx-font-size: 34px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);
        title.setEffect(new DropShadow(8, Color.BLACK));

        grid.setHgap(18);
        grid.setVgap(22);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefSize(820, 520);
        scroll.setMaxSize(820, 520);
        scroll.setStyle("""
                -fx-background: transparent;
                -fx-background-color: transparent;
                -fx-padding: 0;
                """);
        scroll.getStyleClass().add("settings-scroll");
        var css = getClass().getResource("/style.css");
        if (css != null) {
            scroll.getStylesheets().add(css.toExternalForm());
        }

        getChildren().addAll(title, scroll);
    }

    private String getTitleText() {
        return switch (mode) {
            case LOAD -> text("save.load");
            case EDIT -> text("save.edit");
            case SAVE_TO -> text("save.saveTo");
        };
    }

    private void refreshSlots() {
        grid.getChildren().clear();

        List<SaveSlotData> slots = saveSlotManager.getSlots();

        int columns = 2;

        for (int i = 0; i < slots.size(); i++) {
            SaveSlotData slot = slots.get(i);

            Node card = createSlotCard(slot);

            int col = i % columns;
            int row = i / columns;

            grid.add(card, col, row);
        }
    }

    private StackPane createSlotCard(SaveSlotData slot) {
        double width = 360;
        double height = 250;
        double imageWidth = 320;
        double imageHeight = 180;

        StackPane card = new StackPane();
        card.setPrefSize(width, height);
        card.setMinSize(width, height);
        card.setMaxSize(width, height);
        card.setPickOnBounds(true);
        card.setPadding(new Insets(10, 0, 0, 50));

        Rectangle bg = new Rectangle(width, height);
        bg.setArcWidth(18);
        bg.setArcHeight(18);
        bg.setFill(slot.exists()
                ? Color.rgb(0, 0, 0, 0.62)
                : Color.rgb(80, 80, 80, 0.38));
        bg.setStroke(slot.exists()
                ? Color.rgb(255, 255, 255, 0.55)
                : Color.rgb(255, 255, 255, 0.22));
        bg.setStrokeWidth(1.4);

        VBox content = new VBox(8);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(14, 14, 12, 14));

        Node thumbnail = createThumbnail(slot, imageWidth, imageHeight);

        HBox info = new HBox(8);
        info.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER_LEFT);

        Text name = new Text(slot.exists()
                ? slot.getSaveName()
                : text("save.emptySlot"));
        name.setWrappingWidth(270);
        name.setStyle("""
                -fx-font-size: 17px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        Text scene = new Text(slot.exists()
                ? text("save.scene") + slot.getSceneName()
                : text("save.noData"));
        scene.setWrappingWidth(270);
        scene.setStyle("""
                -fx-font-size: 13px;
                -fx-fill: rgba(255,255,255,0.72);
                """);

        textBox.getChildren().addAll(name, scene);
        info.getChildren().add(textBox);

        content.getChildren().addAll(thumbnail, info);

        card.getChildren().addAll(bg, content);

        if (slot.exists()) {
            Tooltip.install(card, createSlotTooltip(slot));
        }

        card.setOnMouseEntered(e -> {
            bg.setStroke(Color.WHITE);
            bg.setFill(slot.exists()
                    ? Color.rgb(213, 105, 16, 0.72)
                    : Color.rgb(110, 110, 110, 0.46));
            card.setScaleX(1.025);
            card.setScaleY(1.025);
        });

        card.setOnMouseExited(e -> {
            bg.setStroke(slot.exists()
                    ? Color.rgb(255, 255, 255, 0.55)
                    : Color.rgb(255, 255, 255, 0.22));
            bg.setFill(slot.exists()
                    ? Color.rgb(0, 0, 0, 0.62)
                    : Color.rgb(80, 80, 80, 0.38));
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        card.setOnMouseClicked(e -> handleSlotClicked(slot));

        return card;
    }

    private ImageView createThumbnailView(Image image, double width, double height) {
        ImageView view = new ImageView(image);

        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        view.setSmooth(true);

        /*
         * 裁切成 16:9，避免壓縮變形。
         */
        double imageRatio = image.getWidth() / image.getHeight();
        double targetRatio = width / height;

        if (imageRatio > targetRatio) {
            double newWidth = image.getHeight() * targetRatio;
            double x = (image.getWidth() - newWidth) / 2;

            view.setViewport(new javafx.geometry.Rectangle2D(
                    x,
                    0,
                    newWidth,
                    image.getHeight()
            ));
        } else {
            double newHeight = image.getWidth() / targetRatio;
            double y = (image.getHeight() - newHeight) / 2;

            view.setViewport(new javafx.geometry.Rectangle2D(
                    0,
                    y,
                    image.getWidth(),
                    newHeight
            ));
        }

        return view;
    }

    private Node createThumbnail(SaveSlotData slot, double width, double height) {
        StackPane box = new StackPane();
        box.setPrefSize(width, height);
        box.setMaxSize(width, height);

        Rectangle frame = new Rectangle(width, height);
        frame.setArcWidth(12);
        frame.setArcHeight(12);
        frame.setFill(Color.rgb(25, 25, 25, 0.9));
        frame.setStroke(Color.rgb(255, 255, 255, 0.22));

        box.getChildren().add(frame);

        if (slot.exists() &&
                slot.getThumbnailBase64() != null &&
                !slot.getThumbnailBase64().isBlank()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(slot.getThumbnailBase64());
                Image image = new Image(new ByteArrayInputStream(bytes));

                ImageView view = createThumbnailView(image, width, height);

                box.getChildren().add(view);

            } catch (Exception ignored) {
                box.getChildren().add(createEmptySlotText());
            }
        } else {
            box.getChildren().add(createEmptySlotText());
        }

        return box;
    }

    private Text createEmptySlotText() {
        Text empty = new Text(text("save.empty"));
        empty.setStyle("""
                -fx-font-size: 26px;
                -fx-fill: rgba(255,255,255,0.35);
                -fx-font-weight: bold;
                """);
        return empty;
    }

    private Tooltip createSlotTooltip(SaveSlotData slot) {
        Tooltip tooltip = new Tooltip(
                text("save.name") + slot.getSaveName() + "\n" +
                        text("save.scene") + slot.getSceneName() + "\n" +
                        text("save.createdAt") + formatTime(slot.getCreatedAt()) + "\n" +
                        text("save.savedAt") + formatTime(slot.getSavedAt()) + "\n" +
                        text("save.lastOpenedAt") + formatTime(slot.getLastOpenedAt())
        );

        tooltip.setStyle("""
                -fx-font-size: 14px;
                -fx-background-color: rgba(0,0,0,0.88);
                -fx-text-fill: white;
                """);

        return tooltip;
    }

    private void handleSlotClicked(SaveSlotData slot) {
        switch (mode) {
            case LOAD -> handleLoad(slot);
            case EDIT -> handleEdit(slot);
            case SAVE_TO -> handleSaveTo(slot);
        }
    }

    private void handleLoad(SaveSlotData slot) {
        if (!slot.exists()) {
            return;
        }

        if (saveSystem == null) {
            SaveRequestSystem.requestLoadSlot(slot.getSlotIndex());

            if (onLoadFromMainMenu != null) {
                onLoadFromMainMenu.accept(slot.getSlotIndex());
            }

            return;
        }

        if (saveSlotManager.hasUnsavedChanges(saveSystem)) {
            confirm(
                    text("save.warning.unsaved"),
                    () -> {
                        saveSlotManager.loadSlot(slot.getSlotIndex(), saveSystem);
                        runAfterAction();
                    }
            );
        } else {
            saveSlotManager.loadSlot(slot.getSlotIndex(), saveSystem);
            runAfterAction();
        }
    }

    private void handleEdit(SaveSlotData slot) {
        if (!slot.exists()) {
            return;
        }

        ContextMenu menu = new ContextMenu();

        MenuItem rename = new MenuItem(text("save.rename"));
        rename.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(slot.getSaveName());
            dialog.setTitle(text("save.rename"));
            dialog.setHeaderText(text("save.rename.header"));
            dialog.setContentText(text("save.rename.content"));

            dialog.showAndWait().ifPresent(name -> {
                saveSlotManager.renameSlot(slot.getSlotIndex(), name);
                refreshSlots();
            });
        });

        MenuItem delete = new MenuItem(text("save.delete"));
        delete.setOnAction(e -> {
            confirm(
                    text("save.delete.confirm"),
                    () -> {
                        saveSlotManager.deleteSlot(slot.getSlotIndex());
                        refreshSlots();
                    }
            );
        });

        menu.getItems().addAll(rename, delete);
        menu.show(this, getScene().getWindow().getX() + 500, getScene().getWindow().getY() + 250);
    }

    private void handleSaveTo(SaveSlotData slot) {
        if (saveSystem == null) {
            return;
        }

        Runnable saveAction = () -> {
            TextInputDialog dialog = new TextInputDialog(
                    slot.exists()
                            ? slot.getSaveName()
                            : "Save " + (slot.getSlotIndex() + 1)
            );

            dialog.setTitle(text("save.saveTo"));
            dialog.setHeaderText(text("save.inputName.header"));
            dialog.setContentText(text("save.inputName.content"));

            dialog.showAndWait().ifPresent(name -> {
                saveSlotManager.saveToSlot(
                        slot.getSlotIndex(),
                        name,
                        saveSystem,
                        true,
                        nodeToHideBeforeScreenshot
                );
                refreshSlots();
                runAfterAction();
            });
        };

        if (slot.exists()) {
            confirm(text("save.overwrite.confirm"), saveAction);
        } else {
            saveAction.run();
        }
    }

    private void confirm(String message, Runnable onConfirm) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(text("save.confirm"));
        alert.setHeaderText(message);
        alert.setContentText("");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                onConfirm.run();
            }
        });
    }

    private void runAfterAction() {
        if (onAfterAction != null) {
            onAfterAction.run();
        }
    }

    private String formatTime(long millis) {
        if (millis <= 0) {
            return "-";
        }

        return new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date(millis));
    }
}