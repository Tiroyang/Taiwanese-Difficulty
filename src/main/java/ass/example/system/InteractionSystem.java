package ass.example.system;

import ass.example.components.InteractableComponent;
import ass.example.core.EntityType;
import com.almasb.fxgl.entity.Entity;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Comparator;
import java.util.Optional;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * InteractionSystem
 *
 * 場景互動系統。
 *
 * 功能：
 * 1. 偵測玩家附近最近的 InteractableComponent。
 * 2. 顯示互動提示 UI。
 * 3. 根據物件設定，讓提示顯示在固定位置或物件上方。
 * 4. 處理玩家按下互動鍵後執行互動行為。
 * 5. 支援互動冷卻，避免連續觸發。
 * 6. 支援全域互動鎖，避免轉場前後連續觸發互動。
 */
public class InteractionSystem {

    // =========================================================
    // UI Constants
    // =========================================================

    /**
     * 互動提示框左右 padding。
     */
    private static final double PROMPT_PADDING_X = 12.0;

    /**
     * 固定提示框 X。
     *
     * 當 InteractableComponent.isPromptOnEntity() 為 false 時使用。
     */
    private static final double DEFAULT_PROMPT_X = 640.0;

    /**
     * 固定提示框 Y。
     *
     * 當 InteractableComponent.isPromptOnEntity() 為 false 時使用。
     */
    private static final double DEFAULT_PROMPT_Y = 620.0;

    /**
     * 按鍵圖示路徑。
     */
    private static final String KEY_ICON_PATH = "/assets/textures/ui/keys/key-f.png";


    // =========================================================
    // Animation Constants
    // =========================================================

    /**
     * 提示框出現動畫秒數。
     */
    private static final double PROMPT_APPEAR_SECONDS = 0.14;

    /**
     * 提示框出現時的 Y 偏移量。
     */
    private static final double PROMPT_APPEAR_OFFSET_Y = 12.0;

    /**
     * 提示框出現時的初始縮放。
     */
    private static final double PROMPT_APPEAR_START_SCALE = 0.92;

    /**
     * 呼吸動畫放大比例。
     */
    private static final double PROMPT_PULSE_SCALE = 1.035;

    /**
     * 呼吸動畫半段時間。
     */
    private static final double PROMPT_PULSE_HALF_SECONDS = 0.65;

    /**
     * 呼吸動畫完整週期時間。
     */
    private static final double PROMPT_PULSE_FULL_SECONDS = 1.3;


    // =========================================================
    // Interaction Constants
    // =========================================================

    /**
     * 互動冷卻時間。
     *
     * 避免玩家按住互動鍵時連續觸發同一物件。
     */
    private static final double INTERACT_COOLDOWN_SECONDS = 0.25;


    // =========================================================
    // Global Interaction Lock
    // =========================================================

    /**
     * 全域互動鎖結束時間。
     *
     * 單位：System.nanoTime()
     */
    private static long globalInteractLockedUntilNanos = 0L;


    // =========================================================
    // Dependencies
    // =========================================================

    /**
     * 目前場景的玩家 Entity。
     */
    private final Entity player;


    // =========================================================
    // UI Nodes
    // =========================================================

    /**
     * 提示框根容器。
     */
    private final StackPane promptBox = new StackPane();

    /**
     * 提示框內容。
     *
     * 包含按鍵圖示與文字。
     */
    private final HBox promptContent = new HBox(10);

    /**
     * F 鍵圖示。
     */
    private final ImageView keyIcon = new ImageView();

    /**
     * 互動提示文字。
     */
    private final Text actionText = new Text();


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * 目前正在顯示提示的目標。
     *
     * 用來判斷是否換了互動目標。
     */
    private Entity currentPromptTarget = null;

    /**
     * 提示框呼吸動畫。
     */
    private Timeline pulseAnimation;

    /**
     * 互動冷卻計時。
     */
    private double interactTimer = 0.0;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立互動系統。
     *
     * @param player 目前場景玩家
     */
    public InteractionSystem(Entity player) {
        this.player = player;

        setupPromptUI();

        addUINode(promptBox, 0, 0);
    }


    // =========================================================
    // Update
    // =========================================================

    /**
     * 每幀更新互動系統。
     *
     * 流程：
     * 1. 玩家死亡時隱藏提示。
     * 2. 全域互動鎖啟用時隱藏提示。
     * 3. 更新互動冷卻。
     * 4. 尋找最近可互動目標。
     * 5. 若有目標，顯示並更新提示位置。
     * 6. 若沒有目標，隱藏提示。
     *
     * @param tpf time per frame
     */
    public void update(double tpf) {
        if (shouldDisableInteraction()) {
            hidePrompt();
            return;
        }

        updateInteractCooldown(tpf);

        Optional<Entity> nearestInteractable = findNearestInteractable();

        if (nearestInteractable.isEmpty()) {
            hidePrompt();
            return;
        }

        Entity target = nearestInteractable.get();
        InteractableComponent component = target.getComponent(InteractableComponent.class);

        showPrompt(target, component);
        updatePromptPosition(target, component);
    }

    /**
     * 更新互動冷卻。
     */
    private void updateInteractCooldown(double tpf) {
        if (interactTimer <= 0) {
            return;
        }

        interactTimer -= tpf;

        if (interactTimer < 0) {
            interactTimer = 0;
        }
    }


    // =========================================================
    // Interact
    // =========================================================

    /**
     * 嘗試執行互動。
     *
     * 從 SceneManager 或 Scene 呼叫。
     *
     * 流程：
     * 1. 玩家死亡時不互動。
     * 2. 全域互動鎖啟用時不互動。
     * 3. 冷卻中不互動。
     * 4. 尋找最近可互動物件。
     * 5. 再次確認該物件 canInteract()。
     * 6. 先進入冷卻，再執行 action。
     */
    public void interact() {
        if (shouldDisableInteraction()) {
            hidePrompt();
            return;
        }

        if (interactTimer > 0) {
            return;
        }

        findNearestInteractable().ifPresent(target -> {
            InteractableComponent component =
                    target.getComponent(InteractableComponent.class);

            if (!component.canInteract()) {
                return;
            }

            /*
            先進入冷卻，再執行 action，否則可能會重複觸發。
             */
            interactTimer = INTERACT_COOLDOWN_SECONDS;

            component.interact();
        });
    }


    // =========================================================
    // Prompt UI Setup
    // =========================================================

    /**
     * 建立提示 UI。
     */
    private void setupPromptUI() {
        promptBox.setVisible(false);
        promptBox.setMouseTransparent(true);

        promptBox.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        promptBox.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        promptBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        setupKeyIcon();
        setupActionText();
        setupPromptContent();

        promptBox.getChildren().add(promptContent);

        createPulseAnimation();
    }

    /**
     * 設定按鍵圖示。
     */
    private void setupKeyIcon() {
        Image image = loadImage(KEY_ICON_PATH);

        if (image != null) {
            keyIcon.setImage(image);
        }

        keyIcon.setFitHeight(24);
        keyIcon.setFitWidth(24);
        keyIcon.setPreserveRatio(true);
        keyIcon.setSmooth(true);
        keyIcon.setEffect(new DropShadow(5, Color.BLACK));
    }

    /**
     * 設定互動文字。
     */
    private void setupActionText() {
        actionText.setStyle("""
                -fx-font-size: 18px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        actionText.setEffect(new DropShadow(5, Color.BLACK));
    }

    /**
     * 設定提示框內容容器。
     */
    private void setupPromptContent() {
        promptContent.setAlignment(Pos.CENTER_LEFT);
        promptContent.setPadding(
                new Insets(
                        7,
                        PROMPT_PADDING_X,
                        7,
                        PROMPT_PADDING_X
                )
        );

        promptContent.getChildren().addAll(
                keyIcon,
                actionText
        );

        promptContent.setStyle("""
                -fx-background-color: rgba(0, 0, 0, 0.2);
                -fx-background-radius: 16px;
                -fx-border-color: rgba(255, 255, 255, 0.75);
                -fx-border-width: 1.4px;
                -fx-border-radius: 16px;
                """);

        promptContent.setEffect(
                new DropShadow(12, Color.rgb(255, 255, 255, 0.20))
        );
    }


    // =========================================================
    // Prompt Display
    // =========================================================

    /**
     * 顯示互動提示。
     *
     * @param target 互動目標
     * @param component 互動元件
     */
    private void showPrompt(
            Entity target,
            InteractableComponent component
    ) {
        actionText.setText(component.getPromptText());

        if (currentPromptTarget != target) {
            currentPromptTarget = target;
            playPromptAppearAnimation();
        }

        promptBox.setVisible(true);
        playPulseAnimationIfNeeded();
    }

    /**
     * 隱藏互動提示。
     */
    private void hidePrompt() {
        currentPromptTarget = null;
        promptBox.setVisible(false);

        stopPulseAnimation();
    }

    /**
     * 更新提示框位置。
     *
     * 分兩種模式：
     * 1. promptOnEntity = false：
     *    固定顯示在畫面中下方。
     *
     * 2. promptOnEntity = true：
     *    顯示在互動物件上方。
     */
    private void updatePromptPosition(
            Entity target,
            InteractableComponent component
    ) {
        updatePromptLayoutSize();

        double promptWidth = promptBox.getLayoutBounds().getWidth();
        double promptHeight = promptBox.getLayoutBounds().getHeight();

        if (!component.isPromptOnEntity()) {
            setPromptScreenPosition(
                    DEFAULT_PROMPT_X - promptWidth / 2.0,
                    DEFAULT_PROMPT_Y
            );
            return;
        }

        double worldX = target.getBoundingBoxComponent()
                        .getCenterWorld()
                        .getX();

        double worldY = target.getBoundingBoxComponent()
                        .getMinYWorld()
                        - component.getPromptOffsetY();

        double screenX = worldX - getGameScene().getViewport().getX();

        double screenY = worldY - getGameScene().getViewport().getY();

        setPromptScreenPosition(
                screenX - promptWidth / 2.0,
                screenY - promptHeight
        );
    }

    /**
     * 更新提示框 layout 尺寸。
     *
     * 若不先 applyCss / autosize，
     * getLayoutBounds() 可能會取到上一幀尺寸。
     */
    private void updatePromptLayoutSize() {
        promptBox.applyCss();
        promptBox.autosize();
    }

    /**
     * 設定提示框螢幕位置。
     */
    private void setPromptScreenPosition(
            double x,
            double y
    ) {
        promptBox.setTranslateX(x);
        promptBox.setTranslateY(y);
    }


    // =========================================================
    // Prompt Animations
    // =========================================================

    /**
     * 建立提示框呼吸動畫。
     */
    private void createPulseAnimation() {
        pulseAnimation = new Timeline(
                new KeyFrame(
                        Duration.ZERO,
                        new KeyValue(promptBox.scaleXProperty(), 1.0),
                        new KeyValue(promptBox.scaleYProperty(), 1.0)
                ),
                new KeyFrame(
                        Duration.seconds(PROMPT_PULSE_HALF_SECONDS),
                        new KeyValue(promptBox.scaleXProperty(), PROMPT_PULSE_SCALE),
                        new KeyValue(promptBox.scaleYProperty(), PROMPT_PULSE_SCALE)
                ),
                new KeyFrame(
                        Duration.seconds(PROMPT_PULSE_FULL_SECONDS),
                        new KeyValue(promptBox.scaleXProperty(), 1.0),
                        new KeyValue(promptBox.scaleYProperty(), 1.0)
                )
        );

        pulseAnimation.setCycleCount(Animation.INDEFINITE);
    }

    /**
     * 播放提示框出現動畫。
     */
    private void playPromptAppearAnimation() {
        promptBox.setOpacity(0);
        promptBox.setTranslateY(promptBox.getTranslateY() + PROMPT_APPEAR_OFFSET_Y);
        promptBox.setScaleX(PROMPT_APPEAR_START_SCALE);
        promptBox.setScaleY(PROMPT_APPEAR_START_SCALE);

        FadeTransition fade = new FadeTransition(
                Duration.seconds(PROMPT_APPEAR_SECONDS),
                promptBox
        );
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition move = new TranslateTransition(
                Duration.seconds(PROMPT_APPEAR_SECONDS),
                promptBox
        );
        move.setByY(-PROMPT_APPEAR_OFFSET_Y);
        move.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(
                Duration.seconds(PROMPT_APPEAR_SECONDS),
                promptBox
        );
        scale.setFromX(PROMPT_APPEAR_START_SCALE);
        scale.setFromY(PROMPT_APPEAR_START_SCALE);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(
                fade,
                move,
                scale
        ).play();
    }

    /**
     * 若呼吸動畫尚未播放，開始播放。
     */
    private void playPulseAnimationIfNeeded() {
        if (pulseAnimation == null) {
            return;
        }

        if (pulseAnimation.getStatus() != Animation.Status.RUNNING) {
            pulseAnimation.play();
        }
    }

    /**
     * 停止呼吸動畫。
     */
    private void stopPulseAnimation() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
        }

        promptBox.setScaleX(1.0);
        promptBox.setScaleY(1.0);
    }


    // =========================================================
    // Interactable Search
    // =========================================================

    /**
     * 尋找玩家附近最近的可互動物件。
     *
     * 條件：
     * 1. 擁有 InteractableComponent。
     * 2. 若玩家在床上，不允許互動門。
     * 3. 玩家與物件中心 X 距離小於互動距離。
     * 4. component.canInteract() 為 true。
     *
     * @return 最近互動物件
     */
    private Optional<Entity> findNearestInteractable() {
        return getGameWorld()
                .getEntitiesByComponent(InteractableComponent.class)
                .stream()
                .filter(this::isInteractableAllowedInCurrentState)
                .filter(this::isPlayerInInteractRange)
                .filter(this::canInteractWithEntity)
                .min(Comparator.comparingDouble(this::distanceXFromPlayerCenter));
    }

    /**
     * 判斷此互動物件在目前狀態下是否允許互動。
     *
     * 目前規則：
     * - 玩家站在床上時，不允許互動門。
     */
    private boolean isInteractableAllowedInCurrentState(Entity entity) {
        boolean playerOnBedCollider = getb("playerOnBedCollider");

        if (!playerOnBedCollider) {
            return true;
        }

        return entity.getType() != EntityType.DOOR;
    }

    /**
     * 判斷玩家是否在互動範圍內。
     */
    private boolean isPlayerInInteractRange(Entity entity) {
        InteractableComponent component =
                entity.getComponent(InteractableComponent.class);

        return distanceXBetweenCenters(entity, player) <
                component.getInteractRange();
    }

    /**
     * 判斷互動物件目前是否可以互動。
     */
    private boolean canInteractWithEntity(Entity entity) {
        InteractableComponent component =
                entity.getComponent(InteractableComponent.class);

        return component.canInteract();
    }

    /**
     * 取得指定 Entity 與玩家中心 X 距離。
     */
    private double distanceXFromPlayerCenter(Entity entity) {
        return distanceXBetweenCenters(entity, player);
    }

    /**
     * 計算兩個 Entity 的中心 X 距離。
     */
    private double distanceXBetweenCenters(
            Entity a,
            Entity b
    ) {
        double ax =
                a.getBoundingBoxComponent()
                        .getCenterWorld()
                        .getX();

        double bx =
                b.getBoundingBoxComponent()
                        .getCenterWorld()
                        .getX();

        return Math.abs(ax - bx);
    }


    // =========================================================
    // Global Interaction Lock
    // =========================================================

    /**
     * 鎖住所有互動一段時間。
     *
     * 用途：
     * - 場景轉場
     * - 對話結束後短時間避免誤觸
     *
     * @param seconds 鎖定秒數
     */
    public static void lockAllInteractions(double seconds) {
        globalInteractLockedUntilNanos = System.nanoTime() + (long) (seconds * 1_000_000_000L);
    }

    /**
     * 判斷目前是否全域互動鎖定中。
     *
     * @return true 表示互動鎖定中
     */
    public static boolean isGlobalInteractionLocked() {
        return System.nanoTime() < globalInteractLockedUntilNanos;
    }


    // =========================================================
    // Lifecycle
    // =========================================================

    /**
     * 釋放互動系統。
     *
     * 場景 cleanup 時呼叫。
     *
     * 會：
     * 1. 隱藏提示。
     * 2. 停止動畫。
     * 3. 從 UI layer 移除 promptBox。
     */
    public void dispose() {
        hidePrompt();

        if (pulseAnimation != null) {
            pulseAnimation.stop();
            pulseAnimation = null;
        }

        removeUINode(promptBox);
    }


    // =========================================================
    // Helpers
    // =========================================================

    /**
     * 判斷目前是否應停用互動。
     */
    private boolean shouldDisableInteraction() {
        return getb("playerDead") || isGlobalInteractionLocked();
    }

    /**
     * 載入圖片。
     *
     * @param path 資源路徑
     * @return Image；若載入失敗則回傳 null
     */
    private Image loadImage(String path) {
        try {
            var url = getClass().getResource(path);

            if (url == null) {
                System.out.println("Interaction key icon not found: " + path);
                return null;
            }

            return new Image(url.toExternalForm());

        } catch (Exception exception) {
            System.out.println("Interaction key icon load failed: " + path);
            exception.printStackTrace();
            return null;
        }
    }
}