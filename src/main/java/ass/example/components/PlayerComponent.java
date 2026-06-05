package ass.example.components;

import ass.example.core.EntityType;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * PlayerComponent
 *
 * 玩家控制與外觀管理 Component。
 *
 * 功能：
 * 1. 處理玩家左右移動。
 * 2. 處理可變高度跳躍。
 * 3. 處理衝刺與衝刺冷卻 UI。
 * 4. 處理玩家走路 / 衝刺 / 站立 / 死亡圖片，並支援穿鞋與赤腳兩套外觀。
 * 6. 建立並更新玩家腳底 ground sensor。
 * 7. 判斷玩家是否站在地上或單向平台上。
 * 8. 支援死亡、重生、瞬移。
 * 9. 支援起床過場與特殊強制外觀動畫。
 */
public class PlayerComponent extends Component {

    // =========================================================
    // Game Var Keys
    // =========================================================

    /**
     * 玩家是否穿鞋的 game var key。
     */
    private static final String VAR_SHOES_WORN = "shoesWorn";


    // =========================================================
    // Movement Constants
    // =========================================================

    /**
     * 玩家一般水平移動速度。
     */
    private static final double MOVE_SPEED = 260.0;


    // =========================================================
    // Jump Constants
    // =========================================================

    /**
     * 玩家起跳瞬間的向上速度。
     */
    private static final double JUMP_SPEED = 560.0;

    /**
     * 長按跳躍時，每秒額外往上的加速度。
     */
    private static final double JUMP_BOOST_POWER = 800.0;

    /**
     * 長按跳躍最多可加成的時間。
     */
    private static final double MAX_JUMP_HOLD_TIME = 0.22;

    /**
     * 放開跳躍鍵時，若玩家仍在上升，
     * 將目前上升速度乘上此係數，形成短跳效果。
     */
    private static final double CUT_JUMP_POWER = 0.45;


    // =========================================================
    // Dash Constants
    // =========================================================

    /**
     * 衝刺持續時間。
     */
    private static final double DASH_DURATION = 2.0;

    /**
     * 衝刺冷卻時間。
     */
    private static final double DASH_COOLDOWN = 5.0;

    /**
     * 衝刺時的速度倍率。
     */
    private static final double DASH_SPEED_SCALE = 1.5;

    /**
     * 衝刺 UI 寬度。
     */
    private static final double DASH_CHARGE_WIDTH = 110.0;

    /**
     * 衝刺 UI 高度。
     */
    private static final double DASH_CHARGE_HEIGHT = 16.0;


    // =========================================================
    // Animation Constants
    // =========================================================

    /**
     * 一般走路動畫每張圖的切換時間。
     */
    private static final double WALK_FRAME_DURATION = 0.35;

    /**
     * 衝刺動畫每張圖的切換時間。
     */
    private static final double DASH_FRAME_DURATION = 0.22;

    /**
     * 媽媽跳舞制裁動畫每張圖的切換時間。
     */
    private static final double MOM_DANCE_FRAME_DURATION = 0.35;


    // =========================================================
    // Ground Sensor Constants
    // =========================================================

    /**
     * 腳底感測器寬度相對玩家寬度的比例。
     */
    private static final double SENSOR_WIDTH_RATIO = 0.6;

    /**
     * 腳底感測器高度。
     */
    private static final double SENSOR_HEIGHT = 8.0;

    /**
     * 腳底感測器相對玩家底部的 Y 偏移。
     */
    private static final double SENSOR_Y_OFFSET = 2.0;


    // =========================================================
    // UI Constants
    // =========================================================

    /**
     * 衝刺 UI 的畫面 X 位置。
     */
    private static final double DASH_UI_X = 1125.0;

    /**
     * 衝刺 UI 的畫面 Y 位置。
     */
    private static final double DASH_UI_Y = 36.0;


    // =========================================================
    // Wake Up Intro Constants
    // =========================================================

    /**
     * 起床過場中，玩家圖片旋轉角度。
     */
    private static final double WAKE_UP_ROTATION = 90.0;

    /**
     * 起床過場中，躺下圖片 X 微調。
     */
    private static final double WAKE_UP_TRANSLATE_X = 50.0;

    /**
     * 起床過場中，躺下圖片 Y 微調。
     */
    private static final double WAKE_UP_TRANSLATE_Y = 25.0;

    /**
     * 起床過場中，玩家 view zIndex。
     */
    private static final int WAKE_UP_VIEW_Z_INDEX = -2;

    /**
     * 玩家正常 view zIndex。
     */
    private static final int NORMAL_VIEW_Z_INDEX = 0;


    // =========================================================
    // Enums
    // =========================================================

    /**
     * 玩家目前水平面向或移動方向。
     */
    private enum Direction {
        NONE,
        LEFT,
        RIGHT
    }

    /**
     * 玩家目前外觀狀態。
     *
     * 用於避免每幀重複設定同一張圖片。
     */
    private enum PlayerVisualState {
        STAND,
        WALK_LEFT,
        WALK_RIGHT,
        DEAD
    }


    // =========================================================
    // Dependencies
    // =========================================================

    /**
     * FXGL PhysicsComponent。
     */
    private PhysicsComponent physics;

    /**
     * 音效系統。
     */
    private final AudioSystem audioSystem = AudioSystem.getInstance();


    // =========================================================
    // View References
    // =========================================================

    /**
     * 玩家圖片顯示用 ImageView。
     */
    private final ImageView playerView;


    // =========================================================
    // Images - With Shoes
    // =========================================================

    private final Image standImage;
    private final Image[] walkRightImages;
    private final Image[] walkLeftImages;
    private final Image[] dashRightImages;
    private final Image[] dashLeftImages;


    // =========================================================
    // Images - Shoeless
    // =========================================================

    private final Image standShoelessImage;
    private final Image[] walkRightShoelessImages;
    private final Image[] walkLeftShoelessImages;
    private final Image[] dashRightShoelessImages;
    private final Image[] dashLeftShoelessImages;


    // =========================================================
    // Images - Special
    // =========================================================

    /**
     * 玩家死亡圖片。
     */
    private final Image deadImage;


    // =========================================================
    // Control State
    // =========================================================

    /**
     * 玩家是否可被操作。
     *
     * false 時：
     * - 不接受移動、跳躍、衝刺輸入。
     * - onUpdate 中不更新移動與跳躍。
     */
    private boolean controlEnabled = true;

    /**
     * 左移鍵是否被按住。
     */
    private boolean movingLeft = false;

    /**
     * 右移鍵是否被按住。
     */
    private boolean movingRight = false;


    // =========================================================
    // Movement / Visual State
    // =========================================================

    /**
     * 玩家目前方向。
     */
    private Direction currentDirection = Direction.NONE;

    /**
     * 玩家目前外觀狀態。
     */
    private PlayerVisualState visualState = PlayerVisualState.STAND;

    /**
     * 玩家目前是否正在播放走路動畫。
     */
    private boolean walking = false;

    /**
     * 走路動畫目前第幾張。
     */
    private int walkFrameIndex = 0;

    /**
     * 走路動畫計時器。
     */
    private double walkAnimationTimer = 0;


    // =========================================================
    // Shoes State
    // =========================================================

    /**
     * 玩家目前是否穿鞋。
     */
    private boolean shoesWorn = true;


    // =========================================================
    // Jump State
    // =========================================================

    /**
     * 跳躍鍵是否仍被按住。
     */
    private boolean jumpHeld = false;

    /**
     * 玩家是否正在跳躍加成階段。
     */
    private boolean jumping = false;

    /**
     * 長按跳躍加成累積時間。
     */
    private double jumpHoldTimer = 0;


    // =========================================================
    // Dash State
    // =========================================================

    /**
     * 玩家是否正在衝刺。
     */
    private boolean dashing = false;

    /**
     * 衝刺剩餘時間。
     */
    private double dashTimer = 0;

    /**
     * 衝刺冷卻剩餘時間。
     */
    private double dashCooldownTimer = 0;


    // =========================================================
    // Dash UI
    // =========================================================

    /**
     * 衝刺冷卻 UI 容器。
     */
    private StackPane dashChargeBox;

    /**
     * 衝刺冷卻 UI 填充條。
     */
    private Rectangle dashChargeFill;


    // =========================================================
    // Ground Sensor
    // =========================================================

    /**
     * 腳底感測器 Entity。
     *
     * 這是獨立 spawn 出來的 Entity，
     * 用於判斷玩家是否踩在地面上。
     */
    private Entity groundSensor;

    /**
     * 腳底感測器目前接觸到的地面數量。
     */
    private int groundContacts = 0;

    /**
     * 玩家是否被 OneWayPlatformSystem 或 BedSystem 視為站在單向平台上。
     */
    private boolean onOneWayPlatform = false;


    // =========================================================
    // Forced Visual Animation
    // =========================================================

    /**
     * 強制外觀動畫 Timeline。
     */
    private Timeline forcedVisualTimeline;

    /**
     * 是否正在播放強制外觀動畫。
     */
    private boolean forcedVisualPlaying = false;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立 PlayerComponent。
     *
     * @param playerView 玩家 ImageView
     * @param standImage 穿鞋站立圖
     * @param walkRightImages 穿鞋右走圖組
     * @param walkLeftImages 穿鞋左走圖組
     * @param dashRightImages 穿鞋右衝刺圖組
     * @param dashLeftImages 穿鞋左衝刺圖組
     * @param standShoelessImage 赤腳站立圖
     * @param walkRightShoelessImages 赤腳右走圖組
     * @param walkLeftShoelessImages 赤腳左走圖組
     * @param dashRightShoelessImages 赤腳右衝刺圖組
     * @param dashLeftShoelessImages 赤腳左衝刺圖組
     * @param deadImage 死亡圖
     */
    public PlayerComponent(
            ImageView playerView,

            Image standImage,
            Image[] walkRightImages,
            Image[] walkLeftImages,
            Image[] dashRightImages,
            Image[] dashLeftImages,

            Image standShoelessImage,
            Image[] walkRightShoelessImages,
            Image[] walkLeftShoelessImages,
            Image[] dashRightShoelessImages,
            Image[] dashLeftShoelessImages,

            Image deadImage
    ) {
        this.playerView = playerView;

        this.standImage = standImage;
        this.walkRightImages = walkRightImages;
        this.walkLeftImages = walkLeftImages;
        this.dashRightImages = dashRightImages;
        this.dashLeftImages = dashLeftImages;

        this.standShoelessImage = standShoelessImage;
        this.walkRightShoelessImages = walkRightShoelessImages;
        this.walkLeftShoelessImages = walkLeftShoelessImages;
        this.dashRightShoelessImages = dashRightShoelessImages;
        this.dashLeftShoelessImages = dashLeftShoelessImages;

        this.deadImage = deadImage;
    }


    // =========================================================
    // FXGL Lifecycle
    // =========================================================

    /**
     * Component 被加入 Entity 時呼叫。
     *
     * 初始化內容：
     * 1. 建立腳底感測器。
     * 2. 建立衝刺 UI。
     * 3. 從 game var 讀取玩家是否穿鞋。
     * 4. 顯示站立圖片。
     */
    @Override
    public void onAdded() {
        createGroundSensor();
        createDashChargeUI();

        shoesWorn = getb(VAR_SHOES_WORN);

        visualState = PlayerVisualState.STAND;
        setPlayerImage(getStandImage());
    }

    /**
     * Component 被移除時呼叫。
     *
     * 需要清理：
     * 1. 衝刺 UI。
     * 2. 腳底感測器。
     * 3. 強制外觀動畫。
     */
    @Override
    public void onRemoved() {
        disposeRuntimeNodes();
    }

    /**
     * 每幀更新玩家狀態。
     *
     * 更新順序：
     * 1. 更新衝刺狀態。
     * 2. 若可控制，更新移動、跳躍與動畫。
     * 3. 更新腳底感測器位置。
     * 4. 更新衝刺 UI。
     *
     * @param tpf time per frame
     */
    @Override
    public void onUpdate(double tpf) {
        updateDash(tpf);

        if (controlEnabled) {
            updateMovement();
            updateVariableJump(tpf);
            updateAnimation(tpf);
        }

        updateGroundSensorPosition();
        updateDashChargeUI();
    }


    // =========================================================
    // Movement Input
    // =========================================================

    /**
     * 玩家按下左移。
     */
    public void moveLeft() {
        if (!controlEnabled) {
            return;
        }

        movingLeft = true;
    }

    /**
     * 玩家按下右移。
     */
    public void moveRight() {
        if (!controlEnabled) {
            return;
        }

        movingRight = true;
    }

    /**
     * 玩家放開左移。
     */
    public void stopLeft() {
        if (!controlEnabled) {
            return;
        }

        movingLeft = false;
    }

    /**
     * 玩家放開右移。
     */
    public void stopRight() {
        if (!controlEnabled) {
            return;
        }

        movingRight = false;
    }


    // =========================================================
    // Movement Update
    // =========================================================

    /**
     * 根據目前左右輸入更新玩家水平速度。
     */
    private void updateMovement() {
        double velocityX = 0;

        if (movingLeft && !movingRight) {
            velocityX = -getCurrentMoveSpeed();
            currentDirection = Direction.LEFT;
        } else if (movingRight && !movingLeft) {
            velocityX = getCurrentMoveSpeed();
            currentDirection = Direction.RIGHT;
        } else {
            currentDirection = Direction.NONE;
        }

        physics.setVelocityX(velocityX);
    }

    /**
     * 取得目前水平移動速度。
     *
     * 若正在衝刺，會乘上衝刺倍率。
     *
     * @return 目前移動速度
     */
    private double getCurrentMoveSpeed() {
        return MOVE_SPEED * (dashing ? DASH_SPEED_SCALE : 1.0);
    }


    // =========================================================
    // Jump Input / Update
    // =========================================================

    /**
     * 玩家按下跳躍鍵。
     *
     * 若玩家在地上：
     * 1. 播放跳躍音效。
     * 2. 進入跳躍狀態。
     * 3. 清除地面接觸與單向平台狀態。
     * 4. 給予向上速度。
     */
    public void jumpPressed() {
        if (!controlEnabled) {
            return;
        }

        jumpHeld = true;

        if (!isOnGround()) {
            return;
        }

        audioSystem.playSFX(SoundId.JUMP);

        jumping = true;
        jumpHoldTimer = 0;

        groundContacts = 0;
        onOneWayPlatform = false;

        physics.setVelocityY(-JUMP_SPEED);
    }

    /**
     * 玩家放開跳躍鍵。
     *
     * 若玩家仍在上升，
     * 立即削弱上升速度，形成短跳效果。
     */
    public void jumpReleased() {
        jumpHeld = false;

        if (physics.getVelocityY() < 0) {
            physics.setVelocityY(physics.getVelocityY() * CUT_JUMP_POWER);
        }

        jumping = false;
    }

    /**
     * 更新可變高度跳躍。
     *
     * 玩家按住跳躍鍵時，
     * 在 MAX_JUMP_HOLD_TIME 內持續給予向上加成。
     *
     * @param tpf time per frame
     */
    private void updateVariableJump(double tpf) {
        if (!jumping) {
            return;
        }

        boolean stillGoingUp = physics.getVelocityY() < 0;
        boolean canBoost = jumpHeld &&
                jumpHoldTimer < MAX_JUMP_HOLD_TIME &&
                stillGoingUp;

        if (canBoost) {
            physics.setVelocityY(physics.getVelocityY() - JUMP_BOOST_POWER * tpf);
            jumpHoldTimer += tpf;
        }

        if (physics.getVelocityY() >= 0) {
            jumping = false;
        }
    }


    // =========================================================
    // Dash Input / Update
    // =========================================================

    /**
     * 玩家按下衝刺鍵。
     *
     * 條件：
     * 1. 玩家可控制。
     * 2. 目前沒有正在衝刺。
     * 3. 衝刺不在冷卻中。
     */
    public void dashPressed() {
        if (!controlEnabled || dashing || dashCooldownTimer > 0) {
            return;
        }

        dashing = true;
        dashTimer = DASH_DURATION;

        showDashChargeUI();

        resetWalkAnimation();
    }

    /**
     * 每幀更新衝刺與冷卻時間。
     *
     * @param tpf time per frame
     */
    private void updateDash(double tpf) {
        updateDashCooldown(tpf);

        if (!dashing) {
            return;
        }

        dashTimer -= tpf;

        if (dashTimer <= 0) {
            stopDashAndStartCooldown();
        }
    }

    /**
     * 更新衝刺冷卻計時器。
     */
    private void updateDashCooldown(double tpf) {
        if (dashCooldownTimer <= 0) {
            return;
        }

        dashCooldownTimer -= tpf;

        if (dashCooldownTimer < 0) {
            dashCooldownTimer = 0;
        }
    }

    /**
     * 結束衝刺並開始冷卻。
     */
    private void stopDashAndStartCooldown() {
        dashing = false;
        dashTimer = 0;
        dashCooldownTimer = DASH_COOLDOWN;
    }

    /**
     * 重置衝刺狀態。
     *
     * 用於：
     * - 停止所有移動。
     * - 玩家死亡。
     * - 玩家重生。
     */
    public void resetDashState() {
        dashing = false;
        dashTimer = 0;
        dashCooldownTimer = 0;

        if (dashChargeFill != null) {
            dashChargeFill.setWidth(DASH_CHARGE_WIDTH);
        }

        hideDashChargeUI();

        resetWalkAnimation();
    }


    // =========================================================
    // Dash UI
    // =========================================================

    /**
     * 建立衝刺冷卻 UI。
     *
     * 此 UI 透過 addUINode 加到 UI layer，
     * 不會跟著 player Entity 自動移除，
     * 因此必須在 disposeRuntimeNodes() 手動移除。
     */
    private void createDashChargeUI() {
        Rectangle background = new Rectangle(DASH_CHARGE_WIDTH, DASH_CHARGE_HEIGHT);
        background.setArcWidth(6);
        background.setArcHeight(6);
        background.setFill(Color.rgb(20, 20, 20, 0.75));
        background.setStroke(Color.WHITE);
        background.setStrokeWidth(1.5);

        dashChargeFill = new Rectangle(DASH_CHARGE_WIDTH, DASH_CHARGE_HEIGHT);
        dashChargeFill.setArcWidth(6);
        dashChargeFill.setArcHeight(6);
        dashChargeFill.setFill(Color.rgb(255, 255, 255, 0.9));

        dashChargeBox = new StackPane();
        dashChargeBox.setPrefSize(DASH_CHARGE_WIDTH, DASH_CHARGE_HEIGHT);
        dashChargeBox.setAlignment(Pos.CENTER_LEFT);
        dashChargeBox.getChildren().addAll(background, dashChargeFill);
        dashChargeBox.setVisible(false);

        addUINode(dashChargeBox, DASH_UI_X, DASH_UI_Y);
    }

    /**
     * 顯示衝刺 UI，並將填充條歸零。
     */
    private void showDashChargeUI() {
        if (dashChargeBox != null) {
            dashChargeBox.setVisible(true);
            dashChargeBox.setOpacity(1.0);
        }

        if (dashChargeFill != null) {
            dashChargeFill.setWidth(0);
        }
    }

    /**
     * 隱藏衝刺 UI。
     */
    private void hideDashChargeUI() {
        if (dashChargeBox != null) {
            dashChargeBox.setVisible(false);
        }
    }

    /**
     * 更新衝刺 UI 填充比例。
     *
     * 狀態：
     * - 沒有衝刺且冷卻完成：隱藏 UI。
     * - 衝刺中：填充為 0。
     * - 冷卻中：依照冷卻進度填充。
     */
    private void updateDashChargeUI() {
        if (dashChargeBox == null || dashChargeFill == null) {
            return;
        }

        if (!dashing && dashCooldownTimer <= 0) {
            dashChargeFill.setWidth(DASH_CHARGE_WIDTH);
            hideDashChargeUI();
            return;
        }

        dashChargeBox.setVisible(true);

        double ratio = dashing
                ? 0
                : 1.0 - dashCooldownTimer / DASH_COOLDOWN;

        ratio = clamp01(ratio);

        dashChargeFill.setWidth(DASH_CHARGE_WIDTH * ratio);
    }


    // =========================================================
    // Animation Update
    // =========================================================

    /**
     * 更新玩家圖片動畫。
     *
     * 若玩家死亡，直接停止更新。
     * 若玩家沒有移動，顯示站立圖。
     * 若玩家正在移動，根據方向、是否衝刺、是否穿鞋選擇圖片組。
     *
     * @param tpf time per frame
     */
    private void updateAnimation(double tpf) {
        if (visualState == PlayerVisualState.DEAD || forcedVisualPlaying) {
            return;
        }

        if (currentDirection == Direction.NONE) {
            showStandImage();
            walking = false;
            resetWalkAnimation();
            return;
        }

        Image[] currentImages = getCurrentMovementImages();
        PlayerVisualState targetState = getTargetWalkVisualState();

        if (currentImages == null || currentImages.length == 0) {
            return;
        }

        startNewWalkAnimationIfNeeded(targetState, currentImages);

        walking = true;
        walkAnimationTimer += tpf;

        if (walkAnimationTimer >= getCurrentFrameDuration()) {
            advanceWalkFrame(currentImages);
        }
    }

    /**
     * 取得目前應使用的移動圖片組。
     */
    private Image[] getCurrentMovementImages() {
        if (currentDirection == Direction.RIGHT) {
            return shoesWorn
                    ? (dashing ? dashRightImages : walkRightImages)
                    : (dashing ? dashRightShoelessImages : walkRightShoelessImages);
        }

        return shoesWorn
                ? (dashing ? dashLeftImages : walkLeftImages)
                : (dashing ? dashLeftShoelessImages : walkLeftShoelessImages);
    }

    /**
     * 取得目前方向對應的走路外觀狀態。
     */
    private PlayerVisualState getTargetWalkVisualState() {
        return currentDirection == Direction.RIGHT
                ? PlayerVisualState.WALK_RIGHT
                : PlayerVisualState.WALK_LEFT;
    }

    /**
     * 若目前外觀狀態與目標狀態不同，
     * 則重新開始走路動畫。
     */
    private void startNewWalkAnimationIfNeeded(
            PlayerVisualState targetState,
            Image[] currentImages
    ) {
        if (visualState == targetState) {
            return;
        }

        visualState = targetState;
        walkFrameIndex = 0;
        walkAnimationTimer = 0;
        setPlayerImage(currentImages[walkFrameIndex]);
    }

    /**
     * 取得目前動畫每張圖的持續時間。
     */
    private double getCurrentFrameDuration() {
        return dashing ? DASH_FRAME_DURATION : WALK_FRAME_DURATION;
    }

    /**
     * 切到下一張走路 / 衝刺圖片。
     */
    private void advanceWalkFrame(Image[] currentImages) {
        walkAnimationTimer = 0;

        walkFrameIndex++;

        if (walkFrameIndex >= currentImages.length) {
            walkFrameIndex = 0;
        }

        setPlayerImage(currentImages[walkFrameIndex]);

        if (isOnGround()) {
            audioSystem.playSFX(SoundId.FOOTSTEP);
        }
    }

    /**
     * 重置走路動畫計數。
     */
    private void resetWalkAnimation() {
        walkAnimationTimer = 0;
        walkFrameIndex = 0;
    }

    /**
     * 顯示站立圖片。
     */
    private void showStandImage() {
        if (visualState == PlayerVisualState.DEAD) {
            return;
        }

        if (visualState == PlayerVisualState.STAND) {
            return;
        }

        visualState = PlayerVisualState.STAND;
        setPlayerImage(getStandImage());
    }

    /**
     * 顯示死亡圖片。
     *
     * 改成 public 是為了讓 DeathSystem 或讀檔死亡還原流程可以直接呼叫。
     */
    public void showDeadImage() {
        visualState = PlayerVisualState.DEAD;
        setPlayerImage(deadImage);
    }

    /**
     * 取得目前鞋子狀態對應的站立圖片。
     */
    private Image getStandImage() {
        return shoesWorn ? standImage : standShoelessImage;
    }

    /**
     * 更換玩家圖片。
     *
     * 這裡會重置 scaleX 與 translateX，
     * 避免特殊動畫留下的位移影響一般圖片。
     *
     * @param image 要顯示的圖片
     */
    private void setPlayerImage(Image image) {
        if (image == null) {
            return;
        }

        playerView.setScaleX(1);
        playerView.setTranslateX(0);
        playerView.setImage(image);
    }


    // =========================================================
    // Shoes State
    // =========================================================

    /**
     * 設定玩家是否穿鞋。
     *
     * ShoeComponent 會呼叫此方法同步玩家外觀。
     *
     * @param shoesWorn true 表示穿鞋
     */
    public void setShoesWorn(boolean shoesWorn) {
        this.shoesWorn = shoesWorn;

        if (visualState == PlayerVisualState.DEAD) {
            return;
        }

        if (currentDirection == Direction.NONE) {
            showStandImage();
        } else {
            visualState = PlayerVisualState.STAND;
            resetWalkAnimation();
        }
    }

    /**
     * 取得玩家是否穿鞋。
     */
    public boolean isShoesWorn() {
        return shoesWorn;
    }


    // =========================================================
    // Wake Up Intro / Special Visuals
    // =========================================================

    /**
     * 顯示起床過場中躺在床上的姿勢。
     *
     * 這裡只旋轉 playerView，
     * 不旋轉 Entity 本身，避免影響 PhysicsComponent。
     */
    public void showWakeUpBedPose() {
        stopAllMovement();
        setControlEnabled(false);

        setPlayerImage(getWakeUpPoseImage());

        playerView.setRotate(WAKE_UP_ROTATION);
        playerView.setTranslateX(WAKE_UP_TRANSLATE_X);
        playerView.setTranslateY(WAKE_UP_TRANSLATE_Y);

        entity.getViewComponent().setZIndex(WAKE_UP_VIEW_Z_INDEX);
    }

    /**
     * 起床過場結束後還原玩家外觀。
     *
     * 注意：
     * 這裡最後仍維持 controlEnabled = false。
     * 因為 HouseScene 會在整段過場完全結束後再重新開啟控制。
     */
    public void restoreAfterWakeUpIntro() {
        stopAllMovement();

        playerView.setRotate(0);
        playerView.setTranslateX(0);
        playerView.setTranslateY(0);

        entity.getViewComponent().setZIndex(NORMAL_VIEW_Z_INDEX);

        visualState = PlayerVisualState.STAND;
        setPlayerImage(getStandImage());

        setControlEnabled(false);
    }

    /**
     * 取得起床過場躺床使用的圖片。
     *
     * 使用赤腳左走第 2 張。
     */
    private Image getWakeUpPoseImage() {
        if (walkLeftShoelessImages != null && walkLeftShoelessImages.length > 1) {
            return walkLeftShoelessImages[1];
        }

        return standShoelessImage;
    }

    /**
     * 播放媽媽制裁動畫。
     *
     * @param seconds 動畫秒數
     */
    public void playMomDanceOffAnimation(double seconds) {
        stopForcedVisualAnimation();

        stopAllMovement();
        setControlEnabled(false);

        forcedVisualPlaying = true;

        Image leftImage = getSafeImage(walkLeftShoelessImages, 0, standShoelessImage);
        Image rightImage = getSafeImage(walkRightShoelessImages, 0, standShoelessImage);

        final int[] frame = {0};

        forcedVisualTimeline = new Timeline(
                new KeyFrame(Duration.seconds(MOM_DANCE_FRAME_DURATION), event -> {
                    if (frame[0] % 2 == 0) {
                        setPlayerImage(rightImage);
                    } else {
                        setPlayerImage(leftImage);
                    }

                    frame[0]++;
                })
        );

        setPlayerImage(leftImage);

        forcedVisualTimeline.setCycleCount(
                (int) Math.ceil(seconds / MOM_DANCE_FRAME_DURATION)
        );

        forcedVisualTimeline.setOnFinished(event -> {
            forcedVisualPlaying = false;
            showDeadImage();
        });

        forcedVisualTimeline.play();
    }

    /**
     * 停止任何強制外觀動畫。
     */
    public void stopForcedVisualAnimation() {
        if (forcedVisualTimeline != null) {
            forcedVisualTimeline.stop();
            forcedVisualTimeline = null;
        }

        forcedVisualPlaying = false;
    }

    /**
     * 安全取得圖片陣列中的圖片。
     *
     * @param images 圖片陣列
     * @param index 目標 index
     * @param fallback 備用圖片
     * @return 圖片
     */
    private Image getSafeImage(Image[] images, int index, Image fallback) {
        if (images == null || images.length <= index) {
            return fallback;
        }

        return images[index];
    }


    // =========================================================
    // Ground Detection
    // =========================================================

    /**
     * 判斷玩家是否在地上。
     *
     * 條件：
     * 1. groundContacts > 0。
     * 2. 或目前被單向平台系統視為站在平台上。
     *
     * @return true 表示玩家在地上或平台上
     */
    public boolean isOnGround() {
        return groundContacts > 0 || onOneWayPlatform;
    }

    /**
     * 增加一個地面接觸。
     *
     * 通常由 ground sensor 碰撞事件呼叫。
     */
    public void addGroundContact() {
        groundContacts++;

        jumping = false;
        jumpHoldTimer = 0;
    }

    /**
     * 移除一個地面接觸。
     *
     * 若接觸數小於 0，會自動修正回 0。
     */
    public void removeGroundContact() {
        groundContacts--;

        if (groundContacts < 0) {
            groundContacts = 0;
        }
    }

    /**
     * 重新掃描目前 ground sensor 接觸到的所有地面。
     *
     * 用於：
     * - 玩家瞬移後。
     * - 玩家重生後。
     * - 需要重新同步地面狀態時。
     */
    public void refreshGroundContacts() {
        if (groundSensor == null) {
            groundContacts = 0;
            return;
        }

        updateGroundSensorPosition();

        groundContacts = 0;

        getGameWorld()
                .getEntitiesCopy()
                .stream()
                .filter(this::isGroundEntity)
                .filter(this::isSensorTouchingEntity)
                .forEach(entity -> groundContacts++);
    }

    /**
     * 建立腳底感測器。
     *
     * groundSensor 是獨立 Entity，
     * 不會自動跟著玩家 Entity 移除，
     * 因此需要在 disposeRuntimeNodes() 中手動刪除。
     */
    private void createGroundSensor() {
        double playerWidth = entity.getBoundingBoxComponent().getWidth();
        double sensorWidth = playerWidth * SENSOR_WIDTH_RATIO;

        groundSensor = spawn("player_ground_sensor",
                new SpawnData(entity.getX(), entity.getY())
                        .put("width", sensorWidth)
                        .put("height", SENSOR_HEIGHT)
        );

        updateGroundSensorPosition();
    }

    /**
     * 更新腳底感測器位置，使其跟隨玩家腳底。
     */
    private void updateGroundSensorPosition() {
        if (groundSensor == null) {
            return;
        }

        double playerMinX = entity.getBoundingBoxComponent().getMinXWorld();
        double playerMaxY = entity.getBoundingBoxComponent().getMaxYWorld();
        double playerWidth = entity.getBoundingBoxComponent().getWidth();

        double sensorWidth = groundSensor.getBoundingBoxComponent().getWidth();

        double sensorX = playerMinX + (playerWidth - sensorWidth) / 2.0;
        double sensorY = playerMaxY + SENSOR_Y_OFFSET;

        groundSensor.setPosition(sensorX, sensorY);
    }

    /**
     * 判斷指定 Entity 是否可被視為地面。
     */
    private boolean isGroundEntity(Entity entity) {
        Object type = entity.getType();

        return type == EntityType.WALL ||
                type == EntityType.FLOOR ||
                type == EntityType.ONE_WAY_PLATFORM_COLLIDER ||
                type == EntityType.BED_ONE_WAY_PLATFORM_COLLIDER;
    }

    /**
     * 判斷 ground sensor 是否與指定地面 Entity 重疊。
     */
    private boolean isSensorTouchingEntity(Entity ground) {
        double sensorLeft = groundSensor.getBoundingBoxComponent().getMinXWorld();
        double sensorRight = groundSensor.getBoundingBoxComponent().getMaxXWorld();
        double sensorTop = groundSensor.getBoundingBoxComponent().getMinYWorld();
        double sensorBottom = groundSensor.getBoundingBoxComponent().getMaxYWorld();

        double groundLeft = ground.getBoundingBoxComponent().getMinXWorld();
        double groundRight = ground.getBoundingBoxComponent().getMaxXWorld();
        double groundTop = ground.getBoundingBoxComponent().getMinYWorld();
        double groundBottom = ground.getBoundingBoxComponent().getMaxYWorld();

        boolean xOverlap = sensorRight > groundLeft &&
                sensorLeft < groundRight;

        boolean yOverlap = sensorBottom >= groundTop &&
                sensorTop <= groundBottom;

        return xOverlap && yOverlap;
    }


    // =========================================================
    // One Way Platform
    // =========================================================

    /**
     * 設定玩家是否站在單向平台上。
     *
     * OneWayPlatformSystem 或 BedSystem 會呼叫此方法。
     *
     * @param value true 表示站在單向平台上
     */
    public void setOnOneWayPlatform(boolean value) {
        onOneWayPlatform = value;
    }

    /**
     * 取得玩家是否站在單向平台上。
     */
    public boolean isOnOneWayPlatform() {
        return onOneWayPlatform;
    }


    // =========================================================
    // Death / Respawn / Teleport
    // =========================================================

    /**
     * 設定玩家是否可控制。
     *
     * @param controlEnabled true 表示可控制
     */
    public void setControlEnabled(boolean controlEnabled) {
        this.controlEnabled = controlEnabled;
    }

    /**
     * 玩家死亡時呼叫。
     *
     * 流程：
     * 1. 停止強制外觀動畫。
     * 2. 顯示死亡圖片。
     * 3. 隱藏 ground sensor。
     * 4. 停止所有移動。
     * 5. 禁用控制。
     */
    public void playerDead() {
        stopForcedVisualAnimation();
        showDeadImage();

        if (groundSensor != null) {
            groundSensor.setVisible(false);
        }

        stopAllMovement();

        controlEnabled = false;
    }

    /**
     * 停止所有移動與動作狀態。
     *
     * 注意：
     * 這個方法不會改變目前圖片。
     */
    public void stopAllMovement() {
        movingLeft = false;
        movingRight = false;
        currentDirection = Direction.NONE;

        jumpHeld = false;
        jumping = false;
        jumpHoldTimer = 0;

        resetDashState();

        walking = false;
        resetWalkAnimation();

        if (physics != null) {
            physics.setVelocityX(0);
            physics.setVelocityY(0);
        }
    }

    /**
     * 立即移動玩家到指定位置。
     *
     * 用於：
     * - 起床過場黑幕中瞬移。
     * - 場景切換後校正位置。
     *
     * @param x 目標 X
     * @param y 目標 Y
     */
    public void moveInstantlyTo(double x, double y) {
        stopAllMovement();

        resetGroundState();

        movePhysicsOrEntityTo(x, y);

        updateGroundSensorPosition();
        refreshGroundContacts();
    }

    /**
     * 玩家重生到指定位置。
     *
     * 流程：
     * 1. 停止強制動畫。
     * 2. 清空移動、跳躍、衝刺、平台狀態。
     * 3. 移動到重生點。
     * 4. 顯示玩家 Entity。
     * 5. 顯示並更新 ground sensor。
     * 6. 根據 shoesWorn 顯示站立圖。
     * 7. 重新掃描地面接觸。
     * 8. 重新開啟控制。
     *
     * @param x 重生 X
     * @param y 重生 Y
     */
    public void respawnAt(double x, double y) {
        stopForcedVisualAnimation();

        stopAllMovement();
        resetGroundState();

        movePhysicsOrEntityTo(x, y);

        entity.setVisible(true);

        if (groundSensor != null) {
            groundSensor.setVisible(true);
            updateGroundSensorPosition();
        }

        shoesWorn = getb(VAR_SHOES_WORN);
        visualState = PlayerVisualState.STAND;
        setPlayerImage(getStandImage());

        runOnce(() -> {
            resetGroundState();
            updateGroundSensorPosition();
            refreshGroundContacts();
            System.out.println("Ground contacts refreshed(Player)");
        }, Duration.seconds(0.02));

        controlEnabled = true;
    }

    /**
     * 完全刪除玩家 Entity。
     */
    public void kill() {
        disposeRuntimeNodes();

        if (entity != null) {
            entity.removeFromWorld();
        }
    }

    /**
     * 重置地面與平台狀態。
     */
    private void resetGroundState() {
        groundContacts = 0;
        onOneWayPlatform = false;
    }

    /**
     * 使用 PhysicsComponent 或 Entity 直接移動玩家。
     *
     * 若 physics 存在，使用 overwritePosition。
     * 否則直接設定 Entity 位置。
     */
    private void movePhysicsOrEntityTo(double x, double y) {
        if (physics != null) {
            physics.setVelocityX(0);
            physics.setVelocityY(0);
            physics.overwritePosition(new Point2D(x, y));
        } else {
            entity.setPosition(x, y);
        }
    }


    // =========================================================
    // Cleanup
    // =========================================================

    /**
     * 清理 PlayerComponent 建立的執行期節點。
     *
     * 需要清除：
     * 1. 衝刺 UI。
     * 2. 腳底感測器。
     * 3. 強制外觀動畫。
     */
    private void disposeRuntimeNodes() {
        stopForcedVisualAnimation();

        removeDashChargeUI();
        removeGroundSensor();
    }

    /**
     * 移除衝刺 UI。
     */
    private void removeDashChargeUI() {
        if (dashChargeBox == null) {
            return;
        }

        removeUINode(dashChargeBox);

        dashChargeBox = null;
        dashChargeFill = null;
    }

    /**
     * 移除腳底感測器。
     */
    private void removeGroundSensor() {
        if (groundSensor == null) {
            return;
        }

        groundSensor.removeFromWorld();
        groundSensor = null;
    }


    // =========================================================
    // Utility
    // =========================================================

    /**
     * 將數值限制在 0 到 1。
     */
    private double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}