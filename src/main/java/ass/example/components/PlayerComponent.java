package ass.example.components;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import static com.almasb.fxgl.dsl.FXGL.*;

public class PlayerComponent extends Component {

    // Physics
    private PhysicsComponent physics;

    // 玩家圖片顯示Node
    private final ImageView playerView;

    private final Image standImage;
    private final Image[] walkRightImages;
    private final Image[] walkLeftImages;
    private final Image deadImage;

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
    private final double walkFrameDuration = 0.12;

    // 玩家速度
    private final double moveSpeed = 260;

    // 跳躍狀態
    private boolean jumpHeld = false;
    private boolean isJumping = false;
    private double jumpHoldTimer = 0;

    // 起跳瞬時速率
    private final double jumpSpeed = 520;

    // 長跳加成
    private final double jumpBoostPower = 800;

    // 最多加成時間
    private final double maxJumpHoldTime = 0.22;

    // 短跳削弱
    private final double cutJumpPower = 0.45;

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
            Image deadImage
    ) {
        this.playerView = playerView;
        this.standImage = standImage;
        this.walkRightImages = walkRightImages;
        this.walkLeftImages = walkLeftImages;
        this.deadImage = deadImage;
    }

    // FXGL Lifecycle
    @Override
    public void onAdded() {
        createGroundSensor();
        showStandImage();
    }

    @Override
    public void onUpdate(double tpf) {
        if (controlEnabled) {
            updateMovement();
            updateVariableJump(tpf);
            updateAnimation(tpf);
        }

        updateGroundSensorPosition();
    }

    /**
     * 根據目前按鍵狀態更新水平速度。
     * A / 左鍵 → movingLeft = true
     * D / 右鍵 → movingRight = true
     * 同時按左右時，角色不移動。
     */
    private void updateMovement() {
        double vx = 0;

        if (movingLeft && !movingRight) {
            vx = -moveSpeed;
            currentDirection = Direction.LEFT;
        } else if (movingRight && !movingLeft) {
            vx = moveSpeed;
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

    /**
     * 根據 currentDirection 更新圖片。
     * 不移動：stand
     * 往右：walkr1 / walkr2 / walkr3
     * 往左：walkl1 / walkl2 / walkl3
     * 死亡狀態時，不被其他狀態覆蓋
     */
    private void updateAnimation(double tpf) {
        if (visualState == PlayerVisualState.DEAD) {
            return;
        }

        if (currentDirection == Direction.NONE) {
            showStandImage();
            resetWalkAnimation();
            return;
        }

        Image[] currentWalkImages;

        if (currentDirection == Direction.RIGHT) {
            currentWalkImages = walkRightImages;

            if (visualState != PlayerVisualState.WALK_RIGHT) {
                visualState = PlayerVisualState.WALK_RIGHT;
                resetWalkAnimation();
                setPlayerImage(currentWalkImages[walkFrameIndex]);
            }
        } else {
            currentWalkImages = walkLeftImages;

            if (visualState != PlayerVisualState.WALK_LEFT) {
                visualState = PlayerVisualState.WALK_LEFT;
                resetWalkAnimation();
                setPlayerImage(currentWalkImages[walkFrameIndex]);
            }
        }

        isWalking = true;
        walkAnimTimer += tpf;

        if (walkAnimTimer >= walkFrameDuration) {
            walkAnimTimer = 0;

            walkFrameIndex++;
            if (walkFrameIndex >= currentWalkImages.length) {
                walkFrameIndex = 0;
            }

            setPlayerImage(currentWalkImages[walkFrameIndex]);
        }
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
        setPlayerImage(standImage);
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
     * 重製走路動畫
     */
    private void resetWalkAnimation() {
        isWalking = false;
        walkAnimTimer = 0;
        walkFrameIndex = 0;
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

    /**
     * 是否在地上
     * groundContacts > 0：腳底 sensor 碰到普通地板 / 牆 / 平台
     * onOneWayPlatform：OneWayPlatformSystem 或 BedSystem 判定玩家站在特殊平台上
     */
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

        resetWalkAnimation();

        if (physics != null) {
            physics.setVelocityX(0);
            physics.setVelocityY(0);
        }
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

        jumpHeld = false;
        isJumping = false;
        jumpHoldTimer = 0;

        resetWalkAnimation();

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

        visualState = PlayerVisualState.STAND;
        setPlayerImage(standImage);

        controlEnabled = true;
    }

    /**
     * 完全刪除玩家Entity。
     * 一般死亡畫面請用playerDead()。
     */
     public void kill() {
        if (groundSensor != null) {
            groundSensor.removeFromWorld();
        }

        entity.removeFromWorld();
    }
}