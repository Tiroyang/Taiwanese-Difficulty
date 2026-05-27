package ass.example.components;

import ass.example.core.DeathReason;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;

public class PlayerComponent extends Component {

    // Physics
    private PhysicsComponent physics;

    private final AudioSystem audioSystem = AudioSystem.getInstance();

    // 玩家圖片顯示Node
    private final ImageView playerView;

    private final Image standImage;
    private final Image[] walkRightImages;
    private final Image[] walkLeftImages;
    private final Image[] dashLeftImages;
    private final Image[] dashRightImages;
    private final Image deadImage;

    private boolean shoesWorn = true;

    private final Image standShoelessImage;
    private final Image[] walkRightShoelessImages;
    private final Image[] walkLeftShoelessImages;
    private final Image[] dashRightShoelessImages;
    private final Image[] dashLeftShoelessImages;

    // Control State
    private boolean controlEnabled = true;

    private boolean movingLeft = false;
    private boolean movingRight = false;

    // 決定玩家圖片顯示
    private enum Direction {
        NONE,
        LEFT,
        RIGHT
    }
    private Direction currentDirection = Direction.NONE;

    private enum PlayerVisualState {
        STAND,
        WALK_LEFT,
        WALK_RIGHT,
        DEAD
    }
    private PlayerVisualState visualState = PlayerVisualState.STAND;

    private boolean isWalking = false;
    private int walkFrameIndex = 0;
    private double walkAnimTimer = 0;

    // 走路動畫切換時間
    private final double walkFrameDuration = 0.35;
    private final double dashFrameDuration = 0.22;

    // 玩家速度
    private final double moveSpeed = 260;

    // 跳躍狀態
    private boolean jumpHeld = false;
    private boolean isJumping = false;
    private double jumpHoldTimer = 0;

    // 起跳瞬時速率
    private final double jumpSpeed = 560;

    // 長跳加成
    private final double jumpBoostPower = 800;

    // 最多加成時間
    private final double maxJumpHoldTime = 0.22;

    // 短跳削弱
    private final double cutJumpPower = 0.45;

    // 衝刺狀態
    private boolean dashing = false;
    private double dashTimer = 0;
    private double dashCooldownTimer = 0;

    private final double dashDuration = 2;
    private final double dashCooldown = 5;
    private final double dashSpeedScale = 1.5;

    // 衝刺 UI
    private StackPane dashChargeBox;
    private Rectangle dashChargeFill;

    private final double dashChargeWidth = 110;
    private final double dashChargeHeight = 16;

    // groundSensor是一個獨立Entity，放在玩家腳底，用來判斷玩家是否踩在地上。
    private Entity groundSensor;

    // groundSensor目前接觸到多少個地面物件。
    private int groundContacts = 0;

    // groundSensor數值
    private final double sensorWidthRatio = 0.6;
    private final double sensorHeight = 8;
    private final double sensorYOffset = 2;

    // 玩家是否被OneWayPlatformSystem/BedSystem視為站在單向平台上。
    private boolean onOneWayPlatform = false;

    // Constructor
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

    // FXGL Lifecycle
    @Override
    public void onAdded() {
        createGroundSensor();
        createDashChargeUI();

        shoesWorn = getb("shoesWorn");

        visualState = PlayerVisualState.STAND;
        setPlayerImage(shoesWorn ? standImage : standShoelessImage);
    }

    @Override
    public void onRemoved() {
        disposeRuntimeNodes();
    }

    private void createDashChargeUI() {
        Rectangle background = new Rectangle(dashChargeWidth, dashChargeHeight);
        background.setArcWidth(6);
        background.setArcHeight(6);
        background.setFill(Color.rgb(20, 20, 20, 0.75));
        background.setStroke(Color.WHITE);
        background.setStrokeWidth(1.5);

        dashChargeFill = new Rectangle(dashChargeWidth, dashChargeHeight);
        dashChargeFill.setArcWidth(6);
        dashChargeFill.setArcHeight(6);
        dashChargeFill.setFill(Color.rgb(255, 255, 255, 0.9));

        dashChargeBox = new StackPane();
        dashChargeBox.setPrefSize(dashChargeWidth, dashChargeHeight);
        dashChargeBox.setAlignment(Pos.CENTER_LEFT);
        dashChargeBox.getChildren().addAll(background, dashChargeFill);

        dashChargeBox.setVisible(false);

        addUINode(dashChargeBox, 1125, 36);
    }

    private void showDashChargeUI() {
        if (dashChargeBox != null) {
            dashChargeBox.setVisible(true);
            dashChargeBox.setOpacity(1.0);
        }

        if (dashChargeFill != null) {
            dashChargeFill.setWidth(0);
        }
    }

    private void hideDashChargeUI() {
        if (dashChargeBox != null) {
            dashChargeBox.setVisible(false);
        }
    }

    private void disposeRuntimeNodes() {
        /*
         * 移除 dash UI。
         * 因為 dashChargeBox 是 addUINode 加到 UI layer，
         * 不會跟著 player Entity 自動消失。
         */
        if (dashChargeBox != null) {
            removeUINode(dashChargeBox);
            dashChargeBox = null;
            dashChargeFill = null;
        }

        /*
         * 移除腳底感測器。
         * groundSensor 是另外 spawn 出來的 Entity，
         * 不會自動跟著 player 消失。
         */
        if (groundSensor != null) {
            groundSensor.removeFromWorld();
            groundSensor = null;
        }
    }

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

    private void updateDash(double tpf) {
        if (dashCooldownTimer > 0) {
            dashCooldownTimer -= tpf;

            if (dashCooldownTimer < 0) {
                dashCooldownTimer = 0;
            }
        }

        if (!dashing) {
            return;
        }

        dashTimer -= tpf;

        if (dashTimer <= 0) {
            dashing = false;
            dashTimer = 0;
            dashCooldownTimer = dashCooldown;
        }
    }

    private void updateDashChargeUI() {
        if (dashChargeBox == null || dashChargeFill == null) {
            return;
        }

        if (!dashing && dashCooldownTimer <= 0) {
            dashChargeFill.setWidth(dashChargeWidth);
            hideDashChargeUI();
            return;
        }

        dashChargeBox.setVisible(true);

        double ratio;

        if (dashing) {
            ratio = 0;
        } else {
            ratio = 1.0 - dashCooldownTimer / dashCooldown;
        }

        ratio = Math.max(0, Math.min(1, ratio));

        dashChargeFill.setWidth(dashChargeWidth * ratio);
    }

    private void updateMovement() {
        double vx = 0;

        if (movingLeft && !movingRight) {
            vx = -moveSpeed * (dashing ? dashSpeedScale : 1);
            currentDirection = Direction.LEFT;
        } else if (movingRight && !movingLeft) {
            vx = moveSpeed * (dashing ? dashSpeedScale : 1);
            currentDirection = Direction.RIGHT;
        } else {
            currentDirection = Direction.NONE;
        }

        physics.setVelocityX(vx);
    }

    public void moveLeft() {
        if (!controlEnabled) {
            return;
        }

        movingLeft = true;
    }

    public void moveRight() {
        if (!controlEnabled) {
            return;
        }

        movingRight = true;
    }

    public void stopLeft() {
        if (!controlEnabled) {
            return;
        }

        movingLeft = false;
    }

    public void stopRight() {
        if (!controlEnabled) {
            return;
        }

        movingRight = false;
    }

    public void dashPressed() {
        if (!controlEnabled || dashing || dashCooldownTimer > 0) {
            return;
        }

        dashing = true;
        dashTimer = dashDuration;

        showDashChargeUI();

        walkAnimTimer = 0;
        walkFrameIndex = 0;
    }

    private void updateAnimation(double tpf) {
        if (visualState == PlayerVisualState.DEAD) {
            return;
        }

        if (currentDirection == Direction.NONE) {
            showStandImage();
            isWalking = false;
            walkAnimTimer = 0;
            walkFrameIndex = 0;
            return;
        }

        Image[] currentImages;
        PlayerVisualState targetState;

        if (currentDirection == Direction.RIGHT) {
            currentImages = shoesWorn
                    ? (dashing ? dashRightImages : walkRightImages)
                    : (dashing ? dashRightShoelessImages : walkRightShoelessImages);
            targetState = PlayerVisualState.WALK_RIGHT;
        } else {
            currentImages = shoesWorn
                    ? (dashing ? dashLeftImages : walkLeftImages)
                    : (dashing ? dashLeftShoelessImages : walkLeftShoelessImages);
            targetState = PlayerVisualState.WALK_LEFT;
        }

        if (currentImages == null || currentImages.length == 0) {
            return;
        }

        if (visualState != targetState) {
            visualState = targetState;
            walkFrameIndex = 0;
            walkAnimTimer = 0;
            setPlayerImage(currentImages[walkFrameIndex]);
        }

        isWalking = true;
        walkAnimTimer += tpf;

        double currentFrameDuration = dashing
                ? dashFrameDuration
                : walkFrameDuration;

        if (walkAnimTimer >= currentFrameDuration) {
            walkAnimTimer = 0;

            walkFrameIndex++;
            if (walkFrameIndex >= currentImages.length) {
                walkFrameIndex = 0;
            }

            setPlayerImage(currentImages[walkFrameIndex]);

            if (isOnGround()) {
                audioSystem.playSFX(SoundId.FOOTSTEP);
            }
        }
    }

    public void setShoesWorn(boolean shoesWorn) {
        this.shoesWorn = shoesWorn;

        if (visualState == PlayerVisualState.DEAD) {
            return;
        }

        if (currentDirection == Direction.NONE) {
            showStandImage();
        } else {
            visualState = PlayerVisualState.STAND;
            walkFrameIndex = 0;
            walkAnimTimer = 0;
        }
    }

    public boolean isShoesWorn() {
        return shoesWorn;
    }

    /**
     * 設定站立圖
     */
    private void showStandImage() {
        if (visualState == PlayerVisualState.DEAD) {
            return;
        }

        if (visualState == PlayerVisualState.STAND) {
            return;
        }

        visualState = PlayerVisualState.STAND;
        setPlayerImage(shoesWorn ? standImage : standShoelessImage);
    }

    //

    /**
     * 設定死亡圖
     */
    private void showDeadImage() {
        visualState = PlayerVisualState.DEAD;
        setPlayerImage(deadImage);
    }

    /**
     * 更換玩家圖片。
     */
    private void setPlayerImage(Image image) {
        playerView.setScaleX(1);
        playerView.setTranslateX(0);
        playerView.setImage(image);
    }

    /**
     * 跳躍鍵按下。
     * 如果在地上：
     * 1. 立刻給一個向上速度
     * 2. 設定 jumpHeld = true
     * 3. updateVariableJump()
     */
    public void jumpPressed() {
        if (!controlEnabled) {
            return;
        }

        jumpHeld = true;

        if (!isOnGround()) {
            return;
        }

        isJumping = true;
        jumpHoldTimer = 0;

        groundContacts = 0;
        onOneWayPlatform = false;

        physics.setVelocityY(-jumpSpeed);
    }

    /**
     * 跳躍鍵放開。
     * 如果還在往上，削弱上升速度。
     */
    public void jumpReleased() {
        jumpHeld = false;

        if (physics.getVelocityY() < 0) {
            physics.setVelocityY(physics.getVelocityY() * cutJumpPower);
        }

        isJumping = false;
    }

    /**
     * 按住跳躍鍵時，在 maxJumpHoldTime 內持續給向上加成。
     * 放開越早，跳越低。
     */
    private void updateVariableJump(double tpf) {
        if (!isJumping) {
            return;
        }

        boolean stillGoingUp = physics.getVelocityY() < 0;
        boolean canBoost = jumpHeld && jumpHoldTimer < maxJumpHoldTime && stillGoingUp;

        if (canBoost) {
            physics.setVelocityY(physics.getVelocityY() - jumpBoostPower * tpf);
            jumpHoldTimer += tpf;
        }

        if (physics.getVelocityY() >= 0) {
            isJumping = false;
        }
    }

    public boolean isOnGround() {
        return groundContacts > 0 || onOneWayPlatform;
    }

    public void addGroundContact() {
        groundContacts++;

        isJumping = false;
        jumpHoldTimer = 0;
    }

    public void removeGroundContact() {
        groundContacts--;

        if (groundContacts < 0) {
            groundContacts = 0;
        }
    }

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
                .forEach(e -> groundContacts++);
    }

    private boolean isGroundEntity(Entity entity) {
        Object type = entity.getType();

        return type == ass.example.core.EntityType.WALL ||
                type == ass.example.core.EntityType.ONE_WAY_PLATFORM_COLLIDER ||
                type == ass.example.core.EntityType.BED_ONE_WAY_PLATFORM_COLLIDER;
    }

    private boolean isSensorTouchingEntity(Entity ground) {
        double sensorLeft = groundSensor.getBoundingBoxComponent().getMinXWorld();
        double sensorRight = groundSensor.getBoundingBoxComponent().getMaxXWorld();
        double sensorTop = groundSensor.getBoundingBoxComponent().getMinYWorld();
        double sensorBottom = groundSensor.getBoundingBoxComponent().getMaxYWorld();

        double groundLeft = ground.getBoundingBoxComponent().getMinXWorld();
        double groundRight = ground.getBoundingBoxComponent().getMaxXWorld();
        double groundTop = ground.getBoundingBoxComponent().getMinYWorld();
        double groundBottom = ground.getBoundingBoxComponent().getMaxYWorld();

        boolean xOverlap =
                sensorRight > groundLeft &&
                        sensorLeft < groundRight;

        boolean yOverlap =
                sensorBottom >= groundTop &&
                        sensorTop <= groundBottom;

        return xOverlap && yOverlap;
    }

    /**
     * 建立player_ground_sensor
     */
    private void createGroundSensor() {
        double playerWidth = entity.getBoundingBoxComponent().getWidth();
        double sensorWidth = playerWidth * sensorWidthRatio;

        groundSensor = spawn("player_ground_sensor",
                new SpawnData(entity.getX(), entity.getY())
                        .put("width", sensorWidth)
                        .put("height", sensorHeight)
        );

        updateGroundSensorPosition();
    }

    /**
     * 讓sensor跟隨玩家下方
     */
    private void updateGroundSensorPosition() {
        if (groundSensor == null) {
            return;
        }

        double playerMinX = entity.getBoundingBoxComponent().getMinXWorld();
        double playerMaxY = entity.getBoundingBoxComponent().getMaxYWorld();
        double playerWidth = entity.getBoundingBoxComponent().getWidth();

        double sensorWidth = groundSensor.getBoundingBoxComponent().getWidth();

        double sensorX = playerMinX + (playerWidth - sensorWidth) / 2;
        double sensorY = playerMaxY + sensorYOffset;

        groundSensor.setPosition(sensorX, sensorY);
    }

    // One Way Platform
    public void setOnOneWayPlatform(boolean value) {
        onOneWayPlatform = value;
    }

    public boolean isOnOneWayPlatform() {
        return onOneWayPlatform;
    }

    // 玩家死亡/重生

    /**
     * 禁用控制
     */
    public void setControlEnabled(boolean controlEnabled) {
        this.controlEnabled = controlEnabled;
    }

    /**
     * 玩家死亡， 呼叫切換死亡圖片
     */
    public void playerDead() {
        showDeadImage();

        if (groundSensor != null) {
            groundSensor.setVisible(false);
        }

        stopAllMovement();

        controlEnabled = false;
    }

    /**
     * 停止所有移動，但不改變目前圖片。
     */
    public void stopAllMovement() {
        movingLeft = false;
        movingRight = false;
        currentDirection = Direction.NONE;

        jumpHeld = false;
        isJumping = false;
        jumpHoldTimer = 0;

        resetDashState();

        isWalking = false;
        walkAnimTimer = 0;
        walkFrameIndex = 0;

        if (physics != null) {
            physics.setVelocityX(0);
            physics.setVelocityY(0);
        }
    }

    public void resetDashState() {
        dashing = false;
        dashTimer = 0;
        dashCooldownTimer = 0;

        if (dashChargeFill != null) {
            dashChargeFill.setWidth(dashChargeWidth);
        }

        hideDashChargeUI();

        walkAnimTimer = 0;
        walkFrameIndex = 0;
    }

    /**
     * 玩家重生。
     *
     * 1. 清空移動 / 跳躍 / 平台狀態
     * 2. 移動到重生點
     * 3. 強制顯示stand
     * 4. 重新開啟控制
     */
    public void respawnAt(double x, double y) {
        movingLeft = false;
        movingRight = false;
        currentDirection = Direction.NONE;

        resetDashState();

        jumpHeld = false;
        isJumping = false;
        jumpHoldTimer = 0;

        isWalking = false;
        walkAnimTimer = 0;
        walkFrameIndex = 0;

        groundContacts = 0;
        onOneWayPlatform = false;

        if (physics != null) {
            physics.setVelocityX(0);
            physics.setVelocityY(0);
            physics.overwritePosition(new Point2D(x, y));
        } else {
            entity.setPosition(x, y);
        }

        entity.setVisible(true);

        if (groundSensor != null) {
            groundSensor.setVisible(true);
            updateGroundSensorPosition();
        }

        shoesWorn = getb("shoesWorn");
        visualState = PlayerVisualState.STAND;
        setPlayerImage(shoesWorn ? standImage : standShoelessImage);

        refreshGroundContacts();

        controlEnabled = true;
    }

    /**
     * 完全刪除玩家Entity。
     * 一般死亡畫面請用playerDead()。
     */
    public void kill() {
        disposeRuntimeNodes();

        if (entity != null) {
            entity.removeFromWorld();
        }
    }
}