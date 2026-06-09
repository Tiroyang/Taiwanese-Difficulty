package ass.example.ui.save;

import ass.example.system.LanguageSystem;
import ass.example.system.save.SaveSystem;
import ass.example.system.save.SaveSlotManager;
import ass.example.system.save.SaveSlotManager.SaveSlotData;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * SaveSlotPanel
 *
 * 存檔槽位 UI 面板。
 *
 * 功能：
 * 1. 依照 SaveMenuMode 顯示不同標題與行為。
 * 2. 以卡片形式顯示所有存檔槽。
 * 3. 支援讀取存檔。
 * 4. 支援編輯存檔，包括重新命名與刪除。
 * 5. 支援儲存到指定槽位。
 * 6. 支援顯示存檔縮圖。
 * 7. 支援顯示存檔詳細資訊 Tooltip。
 */
public class SaveSlotPanel extends VBox {
 
    // Layout Constants 

    /**
     * 面板 padding。
     */
    private static final Insets PANEL_PADDING =
            new Insets(70, 80, 70, 40);

    /**
     * 標題與 ScrollPane 間距。
     */
    private static final double PANEL_SPACING = 22.0;

    /**
     * 存檔卡片欄數。
     */
    private static final int GRID_COLUMNS = 2;

    /**
     * 卡片水平間距。
     */
    private static final double GRID_HGAP = 18.0;

    /**
     * 卡片垂直間距。
     */
    private static final double GRID_VGAP = 22.0;

    /**
     * ScrollPane 寬度。
     */
    private static final double SCROLL_WIDTH = 820.0;

    /**
     * ScrollPane 高度。
     */
    private static final double SCROLL_HEIGHT = 520.0;

 
    // Slot Card Constants 

    private static final double CARD_WIDTH = 360.0;
    private static final double CARD_HEIGHT = 250.0;

    private static final double THUMBNAIL_WIDTH = 320.0;
    private static final double THUMBNAIL_HEIGHT = 180.0;

    private static final double CARD_ARC = 18.0;
    private static final double THUMBNAIL_ARC = 12.0;

 
    // Dependencies 

    /**
     * 目前存檔面板模式。
     */
    private final SaveMenuMode mode;

    /**
     * 存檔槽位管理器。
     */
    private final SaveSlotManager saveSlotManager = SaveSlotManager.getInstance();

    /**
     * 語言系統。
     */
    private final LanguageSystem languageSystem = LanguageSystem.getInstance();

    /**
     * SaveSystem。
     *
     * 若此面板在主選單中使用，可能為 null。
     * 主選單讀檔時，通常是先 requestLoadSlot，之後進遊戲再載入。
     */
    private final SaveSystem saveSystem;

    /**
     * 從主選單讀取存檔時的 callback。
     *
     * 只有 saveSystem == null 時會使用。
     */
    private final Consumer<Integer> onLoadFromMainMenu;

    /**
     * 任意操作完成後呼叫。
     *
     * 例如：
     * - 讀檔後關閉 PauseMenu。
     * - 存檔後刷新 UI。
     */
    private final Runnable onAfterAction;

    /**
     * 截圖前需要暫時隱藏的 UI。
     *
     * 例如：
     * - 存檔選單本身。
     * - 暫停選單 overlay。
     */
    private final Node nodeToHideBeforeScreenshot;

 
    // UI Nodes 

    /**
     * 存檔槽位 Grid。
     */
    private final GridPane grid = new GridPane();

 
    // Constructor 

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

        setupPanel();
        refreshSlots();
    }

 
    // Setup 

    /**
     * 建立整體面板 UI。
     */
    private void setupPanel() {
        setAlignment(Pos.TOP_LEFT);
        setPadding(PANEL_PADDING);
        setSpacing(PANEL_SPACING);

        Text title = createTitleText();
        ScrollPane scrollPane = createScrollPane();

        getChildren().addAll(title, scrollPane);
    }

    /**
     * 建立標題文字。
     */
    private Text createTitleText() {
        Text title = new Text(getTitleText());

        title.setStyle("""
                -fx-font-size: 34px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        title.setEffect(new DropShadow(8, Color.BLACK));

        return title;
    }

    /**
     * 建立存檔槽位 ScrollPane。
     */
    private ScrollPane createScrollPane() {
        grid.setHgap(GRID_HGAP);
        grid.setVgap(GRID_VGAP);

        ScrollPane scrollPane = new ScrollPane(grid);

        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefSize(SCROLL_WIDTH, SCROLL_HEIGHT);
        scrollPane.setMaxSize(SCROLL_WIDTH, SCROLL_HEIGHT);

        scrollPane.setStyle("""
                -fx-background: transparent;
                -fx-background-color: transparent;
                -fx-padding: 0;
                """);

        scrollPane.getStyleClass().add("settings-scroll");
        loadScrollPaneCss(scrollPane);

        return scrollPane;
    }

    /**
     * 載入 ScrollPane 樣式。
     */
    private void loadScrollPaneCss(ScrollPane scrollPane) {
        var css = getClass().getResource("/style.css");

        if (css != null) {
            scrollPane.getStylesheets().add(css.toExternalForm());
        }
    }

    /**
     * 根據目前模式取得面板標題。
     */
    private String getTitleText() {
        return switch (mode) {
            case LOAD -> text("save.load");
            case EDIT -> text("save.edit");
            case SAVE_TO -> text("save.saveTo");
        };
    }

 
    // Refresh Slots 

    /**
     * 重新讀取並顯示所有存檔槽。
     */
    private void refreshSlots() {
        grid.getChildren().clear();

        List<SaveSlotData> slots = saveSlotManager.getSlots();

        for (int i = 0; i < slots.size(); i++) {
            SaveSlotData slot = slots.get(i);

            Node card = createSlotCard(slot);

            int col = i % GRID_COLUMNS;
            int row = i / GRID_COLUMNS;

            grid.add(card, col, row);
        }
    }

 
    // Slot Card 

    /**
     * 建立單一存檔槽位卡片。
     */
    private StackPane createSlotCard(SaveSlotData slot) {
        StackPane card = createCardRoot();

        Rectangle background = createCardBackground(slot);
        VBox content = createCardContent(slot);

        card.getChildren().addAll(background, content);

        if (slot.exists()) {
            Tooltip.install(card, createSlotTooltip(slot));
        }

        setupCardHoverEffect(card, background, slot);
        card.setOnMouseClicked(event -> handleSlotClicked(slot));

        return card;
    }

    /**
     * 建立卡片根容器。
     */
    private StackPane createCardRoot() {
        StackPane card = new StackPane();

        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        card.setPickOnBounds(true);

        /*
         * 保留原本視覺偏移。
         * 若你之後希望卡片完全置中，可以移除這個 padding。
         */
        card.setPadding(new Insets(10, 0, 0, 50));

        return card;
    }

    /**
     * 建立卡片背景。
     */
    private Rectangle createCardBackground(SaveSlotData slot) {
        Rectangle background = new Rectangle(CARD_WIDTH, CARD_HEIGHT);

        background.setArcWidth(CARD_ARC);
        background.setArcHeight(CARD_ARC);
        applyCardNormalStyle(background, slot);

        return background;
    }

    /**
     * 建立卡片內容。
     */
    private VBox createCardContent(SaveSlotData slot) {
        VBox content = new VBox(8);

        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(14, 14, 12, 14));

        Node thumbnail = createThumbnail(slot);
        HBox info = createSlotInfo(slot);

        content.getChildren().addAll(thumbnail, info);

        return content;
    }

    /**
     * 建立存檔資訊區。
     */
    private HBox createSlotInfo(SaveSlotData slot) {
        HBox info = new HBox(8);
        info.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER_LEFT);

        Text name = createSlotNameText(slot);
        Text scene = createSlotSceneText(slot);

        textBox.getChildren().addAll(name, scene);
        info.getChildren().add(textBox);

        return info;
    }

    /**
     * 建立存檔名稱文字。
     */
    private Text createSlotNameText(SaveSlotData slot) {
        Text name = new Text(
                slot.exists()
                        ? slot.getSaveName()
                        : text("save.emptySlot")
        );

        name.setWrappingWidth(270);
        name.setStyle("""
                -fx-font-size: 17px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        return name;
    }

    /**
     * 建立場景資訊文字。
     */
    private Text createSlotSceneText(SaveSlotData slot) {
        Text scene = new Text(
                slot.exists()
                        ? text("save.scene") + slot.getSceneName()
                        : text("save.noData")
        );

        scene.setWrappingWidth(270);
        scene.setStyle("""
                -fx-font-size: 13px;
                -fx-fill: rgba(255,255,255,0.72);
                """);

        return scene;
    }

 
    // Card Style 

    /**
     * 套用卡片正常樣式。
     */
    private void applyCardNormalStyle(
            Rectangle background,
            SaveSlotData slot
    ) {
        background.setFill(
                slot.exists()
                        ? Color.rgb(0, 0, 0, 0.62)
                        : Color.rgb(80, 80, 80, 0.38)
        );

        background.setStroke(
                slot.exists()
                        ? Color.rgb(255, 255, 255, 0.55)
                        : Color.rgb(255, 255, 255, 0.22)
        );

        background.setStrokeWidth(1.4);
    }

    /**
     * 套用卡片 hover 樣式。
     */
    private void applyCardHoverStyle(
            Rectangle background,
            SaveSlotData slot
    ) {
        background.setStroke(Color.WHITE);

        background.setFill(
                slot.exists()
                        ? Color.rgb(213, 105, 16, 0.72)
                        : Color.rgb(110, 110, 110, 0.46)
        );
    }

    /**
     * 設定卡片 hover 效果。
     */
    private void setupCardHoverEffect(
            StackPane card,
            Rectangle background,
            SaveSlotData slot
    ) {
        card.setOnMouseEntered(event -> {
            applyCardHoverStyle(background, slot);
            card.setScaleX(1.025);
            card.setScaleY(1.025);
        });

        card.setOnMouseExited(event -> {
            applyCardNormalStyle(background, slot);
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });
    }

 
    // Thumbnail 

    /**
     * 建立存檔縮圖區。
     */
    private Node createThumbnail(SaveSlotData slot) {
        StackPane box = new StackPane();

        box.setPrefSize(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        box.setMaxSize(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

        Rectangle frame = createThumbnailFrame();

        box.getChildren().add(frame);

        Node content = createThumbnailContent(slot);
        box.getChildren().add(content);

        return box;
    }

    /**
     * 建立縮圖外框。
     */
    private Rectangle createThumbnailFrame() {
        Rectangle frame = new Rectangle(
                THUMBNAIL_WIDTH,
                THUMBNAIL_HEIGHT
        );

        frame.setArcWidth(THUMBNAIL_ARC);
        frame.setArcHeight(THUMBNAIL_ARC);
        frame.setFill(Color.rgb(25, 25, 25, 0.9));
        frame.setStroke(Color.rgb(255, 255, 255, 0.22));

        return frame;
    }

    /**
     * 建立縮圖內容。
     *
     * 若沒有縮圖或解析失敗，顯示 EMPTY。
     */
    private Node createThumbnailContent(SaveSlotData slot) {
        if (!hasThumbnail(slot)) {
            return createEmptySlotText();
        }

        try {
            byte[] bytes = Base64
                    .getDecoder()
                    .decode(slot.getThumbnailBase64());

            Image image = new Image(new ByteArrayInputStream(bytes));

            return createThumbnailView(image);

        } catch (Exception exception) {
            return createEmptySlotText();
        }
    }

    /**
     * 判斷槽位是否有縮圖。
     */
    private boolean hasThumbnail(SaveSlotData slot) {
        return slot.exists() &&
                slot.getThumbnailBase64() != null &&
                !slot.getThumbnailBase64().isBlank();
    }

    /**
     * 建立縮圖 ImageView。
     */
    private ImageView createThumbnailView(Image image) {
        ImageView view = new ImageView(image);

        view.setFitWidth(THUMBNAIL_WIDTH);
        view.setFitHeight(THUMBNAIL_HEIGHT);
        view.setPreserveRatio(false);
        view.setSmooth(true);

        applyCenterCropViewport(image, view);

        return view;
    }

    /**
     * 對 ImageView 套用中央裁切。
     *
     * 目標比例為 THUMBNAIL_WIDTH : THUMBNAIL_HEIGHT。
     */
    private void applyCenterCropViewport(
            Image image,
            ImageView view
    ) {
        double imageRatio = image.getWidth() / image.getHeight();
        double targetRatio = THUMBNAIL_WIDTH / THUMBNAIL_HEIGHT;

        if (imageRatio > targetRatio) {
            double newWidth = image.getHeight() * targetRatio;
            double x = (image.getWidth() - newWidth) / 2.0;

            view.setViewport(new Rectangle2D(
                    x,
                    0,
                    newWidth,
                    image.getHeight()
            ));

            return;
        }

        double newHeight = image.getWidth() / targetRatio;
        double y = (image.getHeight() - newHeight) / 2.0;

        view.setViewport(new Rectangle2D(
                0,
                y,
                image.getWidth(),
                newHeight
        ));
    }

    /**
     * 建立 EMPTY 文字。
     */
    private Text createEmptySlotText() {
        Text empty = new Text(text("save.empty"));

        empty.setStyle("""
                -fx-font-size: 26px;
                -fx-fill: rgba(255,255,255,0.35);
                -fx-font-weight: bold;
                """);

        return empty;
    }

 
    // Tooltip 

    /**
     * 建立存檔槽 Tooltip。
     */
    private Tooltip createSlotTooltip(SaveSlotData slot) {
        Tooltip tooltip = new Tooltip(createSlotTooltipText(slot));

        tooltip.setStyle("""
                -fx-font-size: 14px;
                -fx-background-color: rgba(0,0,0,0.88);
                -fx-text-fill: white;
                """);

        return tooltip;
    }

    /**
     * 建立 Tooltip 文字。
     */
    private String createSlotTooltipText(SaveSlotData slot) {
        return text("save.name") + slot.getSaveName() + "\n" +
                text("save.scene") + slot.getSceneName() + "\n" +
                text("save.createdAt") + formatTime(slot.getCreatedAt()) + "\n" +
                text("save.savedAt") + formatTime(slot.getSavedAt()) + "\n" +
                text("save.lastOpenedAt") + formatTime(slot.getLastOpenedAt());
    }

 
    // Slot Click Handling 

    /**
     * 根據目前模式處理槽位點擊。
     */
    private void handleSlotClicked(SaveSlotData slot) {
        switch (mode) {
            case LOAD -> handleLoad(slot);
            case EDIT -> handleEdit(slot);
            case SAVE_TO -> handleSaveTo(slot);
        }
    }

    /**
     * LOAD 模式：
     * 點擊已有存檔後載入。
     */
    private void handleLoad(SaveSlotData slot) {
        if (!slot.exists()) {
            return;
        }

        if (saveSystem == null) {
            requestLoadFromMainMenu(slot);
            return;
        }

        loadSlotInGame(slot);
    }

    /**
     * 主選單讀取存檔。
     *
     * 因為主選單可能還沒有完整 SaveSystem，所以先把欲載入槽位記到 SaveSlotManager。
     */
    private void requestLoadFromMainMenu(SaveSlotData slot) {
        saveSlotManager.requestLoadSlot(slot.getSlotIndex());

        if (onLoadFromMainMenu != null) {
            onLoadFromMainMenu.accept(slot.getSlotIndex());
        }
    }

    /**
     * 遊戲內讀取存檔。
     *
     * 若目前有未儲存變更，會先跳確認視窗。
     */
    private void loadSlotInGame(SaveSlotData slot) {
        Runnable loadAction = () -> {
            saveSlotManager.loadSlot(slot.getSlotIndex(), saveSystem);
            runAfterAction();
        };

        if (saveSlotManager.hasUnsavedChanges(saveSystem)) {
            confirm(text("save.warning.unsaved"), loadAction);
            return;
        }

        loadAction.run();
    }

    /**
     * EDIT 模式：
     * 點擊已有存檔後顯示重新命名 / 刪除選單。
     */
    private void handleEdit(SaveSlotData slot) {
        if (!slot.exists()) {
            return;
        }

        ContextMenu menu = createEditContextMenu(slot);

        menu.show(
                this,
                getScene().getWindow().getX() + 500,
                getScene().getWindow().getY() + 250
        );
    }

    /**
     * SAVE_TO 模式：
     * 點擊槽位後儲存。
     *
     * 若槽位已有資料，會先確認是否覆蓋。
     */
    private void handleSaveTo(SaveSlotData slot) {
        if (saveSystem == null) {
            return;
        }

        Runnable saveAction = () -> showSaveNameDialogAndSave(slot);

        if (slot.exists()) {
            confirm(text("save.overwrite.confirm"), saveAction);
            return;
        }

        saveAction.run();
    }

 
    // Edit Actions 

    /**
     * 建立編輯存檔用 ContextMenu。
     */
    private ContextMenu createEditContextMenu(SaveSlotData slot) {
        ContextMenu menu = new ContextMenu();

        MenuItem rename = createRenameMenuItem(slot);
        MenuItem delete = createDeleteMenuItem(slot);

        menu.getItems().addAll(rename, delete);

        return menu;
    }

    /**
     * 建立重新命名選項。
     */
    private MenuItem createRenameMenuItem(SaveSlotData slot) {
        MenuItem rename = new MenuItem(text("save.rename"));

        rename.setOnAction(event -> showRenameDialog(slot));

        return rename;
    }

    /**
     * 建立刪除選項。
     */
    private MenuItem createDeleteMenuItem(SaveSlotData slot) {
        MenuItem delete = new MenuItem(text("save.delete"));

        delete.setOnAction(event ->
                confirm(
                        text("save.delete.confirm"),
                        () -> {
                            saveSlotManager.deleteSlot(slot.getSlotIndex());
                            refreshSlots();
                        }
                )
        );

        return delete;
    }

    /**
     * 顯示重新命名對話框。
     */
    private void showRenameDialog(SaveSlotData slot) {
        TextInputDialog dialog = new TextInputDialog(slot.getSaveName());

        dialog.setTitle(text("save.rename"));
        dialog.setHeaderText(text("save.rename.header"));
        dialog.setContentText(text("save.rename.content"));

        dialog.showAndWait().ifPresent(name -> {
            saveSlotManager.renameSlot(slot.getSlotIndex(), name);
            refreshSlots();
        });
    }

 
    // Save Actions 

    /**
     * 顯示輸入存檔名稱對話框，並儲存到指定槽位。
     */
    private void showSaveNameDialogAndSave(SaveSlotData slot) {
        TextInputDialog dialog = new TextInputDialog(getDefaultSaveName(slot));

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
    }

    /**
     * 取得存檔名稱輸入框的預設值。
     */
    private String getDefaultSaveName(SaveSlotData slot) {
        if (slot.exists()) {
            return slot.getSaveName();
        }

        return "Save " + (slot.getSlotIndex() + 1);
    }

 
    // Dialog Helpers 

    /**
     * 顯示確認視窗。
     *
     * @param message 顯示訊息
     * @param onConfirm 按下 OK 後執行
     */
    private void confirm(
            String message,
            Runnable onConfirm
    ) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        alert.setTitle(text("save.confirm"));
        alert.setHeaderText(message);
        alert.setContentText("");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK && onConfirm != null) {
                onConfirm.run();
            }
        });
    }

    /**
     * 執行操作完成後 callback。
     */
    private void runAfterAction() {
        if (onAfterAction != null) {
            onAfterAction.run();
        }
    }

 
    // Text / Format Helpers 

    /**
     * 取得翻譯文字。
     */
    private String text(String key) {
        return languageSystem.text(key);
    }

    /**
     * 格式化時間。
     *
     * @param millis timestamp
     * @return yyyy/MM/dd HH:mm；若 millis <= 0 回傳 "-"
     */
    private String formatTime(long millis) {
        if (millis <= 0) {
            return "-";
        }

        return new SimpleDateFormat("yyyy/MM/dd HH:mm")
                .format(new Date(millis));
    }
}