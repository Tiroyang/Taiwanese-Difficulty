package ass.example.system;

import ass.example.components.OneWayPlatformComponent;
import ass.example.components.PlayerComponent;
import ass.example.core.EntityType;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.physics.PhysicsComponent;

import java.util.Comparator;
import java.util.Optional;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * OneWayPlatformSystem
 *
 * 普通單向平台系統。
 *
 * 單向平台規則：
 * 1. 玩家從下方往上跳時，可以穿過平台。
 * 2. 玩家從上方落下時，可以站在平台上。
 * 3. 玩家站上平台後，系統會生成一個實體 collider 讓玩家真正踩住。
 * 4. 玩家跳起、離開平台、或按 Shift 下落時，移除這個實體 collider。
 *
 * 注意：
 * 此系統只處理普通 one_way_platform。
 * 床的特殊平台邏輯由 BedSystem 處理。
 *
 * 單例判斷：
 * OneWayPlatformSystem 不適合做成單例。
 *
 * 原因：
 * - 它持有目前場景的 player Entity。
 * - 它持有目前站上的平台 currentPlatform。
 * - 它會動態生成 currentSolidCollider。
 * - 切換場景或重生時，這些狀態都必須跟著場景重新建立或清除。
 */
public class OneWayPlatformSystem {
 
    // Constants 

    /**
     * 玩家按下 Shift 下落後，短時間內忽略平台吸附。
     *
     * 避免玩家剛移除 collider 後，又立刻被判定重新落回同一平台。
     */
    private static final double DROP_THROUGH_DURATION = 0.28;

    /**
     * 玩家按 Shift 下落時給的向下速度。
     */
    private static final double DROP_THROUGH_VELOCITY_Y = 260.0;

    /**
     * 玩家落到平台頂部的垂直容許誤差。
     *
     * 目前設定為 0，代表必須穿越平台頂部才會觸發。
     */
    private static final double LANDING_TOLERANCE = 0.0;

    /**
     * 判斷水平重疊時，平台左右內縮距離。
     *
     * 避免玩家只是擦到平台邊緣，也被判定站上平台。
     */
    private static final double SIDE_PADDING = 8.0;

    /**
     * 玩家仍被視為站在平台上的垂直容許範圍。
     */
    private static final double STANDING_VERTICAL_TOLERANCE = 8.0;

 
    // Dependencies 

    /**
     * 目前場景的玩家 Entity。
     */
    private final Entity player;

 
    // Runtime State 

    /**
     * 玩家目前站上的 one_way_platform。
     *
     * 若為 null，代表玩家目前不在普通單向平台上。
     */
    private Entity currentPlatform;

    /**
     * 玩家站上平台後生成的實體碰撞平台。
     *
     * 這個 Entity 會用 one_way_platform_collider 生成，
     * 讓玩家真的能站在平台上。
     */
    private Entity currentSolidCollider;

    /**
     * 上一幀玩家底部 Y 座標。
     *
     * 用來判斷玩家是否從上方穿越平台頂部。
     */
    private double previousPlayerBottom;

    /**
     * Shift 下落忽略平台判定倒數。
     */
    private double dropThroughTimer = 0.0;

 
    // Constructor 

    /**
     * 建立普通單向平台系統。
     *
     * @param player 玩家 Entity
     */
    public OneWayPlatformSystem(Entity player) {
        this.player = player;
        this.previousPlayerBottom = getPlayerBottom();
    }

 
    // Update 

    /**
     * 每幀更新單向平台系統。
     *
     * 流程：
     * 1. 玩家死亡時不更新。
     * 2. 更新 Shift 下落 timer。
     * 3. 若正在下落穿透期間，不偵測平台。
     * 4. 若目前已站在平台上，更新平台狀態。
     * 5. 若目前沒有站在平台上，尋找可落下的平台。
     *
     * @param tpf time per frame
     */
    public void update(double tpf) {
        if (isPlayerDead()) {
            return;
        }

        PlayerComponent playerComponent = getPlayerComponent();

        if (playerComponent == null) {
            return;
        }

        updateDropThroughTimer(tpf);

        if (isDroppingThrough()) {
            playerComponent.setOnOneWayPlatform(false);
            updatePreviousPlayerBottom();
            return;
        }

        if (currentPlatform != null) {
            updateCurrentPlatformState(playerComponent);
            updatePreviousPlayerBottom();
            return;
        }

        tryLandOnNearestPlatform(playerComponent);

        updatePreviousPlayerBottom();
    }

    /**
     * 更新目前已站上的平台狀態。
     *
     * @param playerComponent 玩家元件
     */
    private void updateCurrentPlatformState(PlayerComponent playerComponent) {
        if (isPlayerJumpingUpFromCurrentPlatform()) {
            leaveCurrentPlatform(playerComponent);
            return;
        }

        if (isPlayerStillOnCurrentPlatform()) {
            playerComponent.setOnOneWayPlatform(true);
            return;
        }

        leaveCurrentPlatform(playerComponent);
    }

    /**
     * 嘗試尋找最近可落下的平台並站上去。
     *
     * @param playerComponent 玩家元件
     */
    private void tryLandOnNearestPlatform(PlayerComponent playerComponent) {
        Optional<Entity> platform = findPlatformToLandOn();

        if (platform.isPresent()) {
            landOnPlatform(platform.get(), playerComponent);
            return;
        }

        playerComponent.setOnOneWayPlatform(false);
    }

 
    // Platform Search 

    /**
     * 尋找玩家目前可以落上的單向平台。
     *
     * 條件：
     * 1. 玩家不能正在往上跳。
     * 2. 玩家必須從平台上方穿越平台頂部。
     * 3. 玩家水平位置必須與平台重疊。
     *
     * @return 最近可落下的平台
     */
    private Optional<Entity> findPlatformToLandOn() {
        PhysicsComponent physics = getPhysics();

        if (physics == null) {
            return Optional.empty();
        }

        if (physics.getVelocityY() < 0) {
            return Optional.empty();
        }

        return getGameWorld()
                .getEntitiesByType(EntityType.ONE_WAY_PLATFORM)
                .stream()
                .filter(this::canLandOnPlatform)
                .min(Comparator.comparingDouble(Entity::getY));
    }

    /**
     * 判斷玩家是否可以落到指定平台。
     *
     * @param platform 目標平台
     * @return true 表示可以落上去
     */
    private boolean canLandOnPlatform(Entity platform) {
        OneWayPlatformComponent component =
                platform.getComponent(OneWayPlatformComponent.class);

        double platformTop = platform.getY();
        double platformLeft = platform.getX();
        double platformRight = platform.getX() + component.getWidth();

        double playerLeft = getPlayerLeft();
        double playerRight = getPlayerRight();
        double playerBottom = getPlayerBottom();

        boolean xOverlap = isHorizontallyOverlapping(
                playerLeft,
                playerRight,
                platformLeft,
                platformRight
        );

        boolean crossedPlatformTop =
                previousPlayerBottom <= platformTop + LANDING_TOLERANCE &&
                        playerBottom >= platformTop - LANDING_TOLERANCE;

        return xOverlap && crossedPlatformTop;
    }

 
    // Land / Leave Platform 

    /**
     * 讓玩家站上指定平台。
     *
     * 會：
     * 1. 移除原本的 solid collider。
     * 2. 記錄目前平台。
     * 3. 生成新的 solid collider。
     * 4. 清掉玩家垂直速度。
     * 5. 設定玩家目前站在單向平台上。
     *
     * @param platform 目標平台
     * @param playerComponent 玩家元件
     */
    private void landOnPlatform(
            Entity platform,
            PlayerComponent playerComponent
    ) {
        if (currentPlatform == platform && currentSolidCollider != null) {
            playerComponent.setOnOneWayPlatform(true);
            return;
        }

        OneWayPlatformComponent platformComponent =
                platform.getComponent(OneWayPlatformComponent.class);

        removeCurrentSolidCollider();

        currentPlatform = platform;
        currentSolidCollider = spawnSolidColliderForPlatform(
                platform,
                platformComponent
        );

        PhysicsComponent physics = getPhysics();

        if (physics != null) {
            physics.setVelocityY(0);
        }

        playerComponent.setOnOneWayPlatform(true);
    }

    /**
     * 玩家離開目前平台。
     *
     * @param playerComponent 玩家元件
     */
    private void leaveCurrentPlatform(PlayerComponent playerComponent) {
        playerComponent.setOnOneWayPlatform(false);

        currentPlatform = null;
        removeCurrentSolidCollider();
    }

    /**
     * 根據 one_way_platform 生成真正可踩踏的 collider。
     *
     * @param platform 原本的單向平台感測 Entity
     * @param platformComponent 平台資料元件
     * @return 新生成的實體 collider
     */
    private Entity spawnSolidColliderForPlatform(
            Entity platform,
            OneWayPlatformComponent platformComponent
    ) {
        return spawn(
                "one_way_platform_collider",
                new SpawnData(
                        platform.getX(),
                        platform.getY()
                )
                        .put("width", platformComponent.getWidth())
                        .put("height", platformComponent.getHeight())
        );
    }

    /**
     * 移除目前生成的 solid collider。
     */
    private void removeCurrentSolidCollider() {
        if (currentSolidCollider == null) {
            return;
        }

        if (currentSolidCollider.isActive()) {
            currentSolidCollider.removeFromWorld();
        }

        currentSolidCollider = null;
    }

 
    // Current Platform Checks 

    /**
     * 判斷玩家是否正在從目前平台往上跳。
     *
     * @return true 表示玩家正在往上跳
     */
    private boolean isPlayerJumpingUpFromCurrentPlatform() {
        if (currentPlatform == null) {
            return false;
        }

        PhysicsComponent physics = getPhysics();

        return physics != null && physics.getVelocityY() < 0;
    }

    /**
     * 判斷玩家是否仍站在目前平台上。
     *
     * 條件：
     * 1. 水平範圍仍與平台重疊。
     * 2. 玩家底部仍接近平台頂部。
     *
     * @return true 表示仍在平台上
     */
    private boolean isPlayerStillOnCurrentPlatform() {
        if (currentPlatform == null) {
            return false;
        }

        OneWayPlatformComponent component =
                currentPlatform.getComponent(OneWayPlatformComponent.class);

        double platformTop = currentPlatform.getY();
        double platformLeft = currentPlatform.getX();
        double platformRight = currentPlatform.getX() + component.getWidth();

        double playerLeft = getPlayerLeft();
        double playerRight = getPlayerRight();
        double playerBottom = getPlayerBottom();

        boolean xOverlap = isHorizontallyOverlapping(
                playerLeft,
                playerRight,
                platformLeft,
                platformRight
        );

        boolean nearPlatformTop =
                Math.abs(playerBottom - platformTop) <=
                        LANDING_TOLERANCE + STANDING_VERTICAL_TOLERANCE;

        return xOverlap && nearPlatformTop;
    }

 
    // Player Input Hooks 

    /**
     * 玩家按下跳躍鍵時呼叫。
     *
     * 如果玩家目前站在普通單向平台上，
     * 立刻移除 solid collider，讓玩家可以往上跳離平台。
     */
    public void onPlayerJumpPressed() {
        if (currentPlatform == null) {
            return;
        }

        PlayerComponent playerComponent = getPlayerComponent();

        if (playerComponent == null) {
            return;
        }

        leaveCurrentPlatform(playerComponent);
    }

    /**
     * 玩家按下 Shift 下落時呼叫。
     *
     * 如果玩家目前站在普通單向平台上：
     * 1. 移除 solid collider。
     * 2. 短時間忽略平台吸附。
     * 3. 給玩家一個向下速度。
     */
    public void dropThrough() {
        if (currentPlatform == null) {
            return;
        }

        PlayerComponent playerComponent = getPlayerComponent();
        PhysicsComponent physics = getPhysics();

        if (playerComponent == null || physics == null) {
            return;
        }

        leaveCurrentPlatform(playerComponent);

        dropThroughTimer = DROP_THROUGH_DURATION;

        physics.setVelocityY(DROP_THROUGH_VELOCITY_Y);
    }

 
    // Drop Through Timer 

    /**
     * 更新 Shift 下落忽略計時。
     *
     * @param tpf time per frame
     */
    private void updateDropThroughTimer(double tpf) {
        if (dropThroughTimer <= 0) {
            return;
        }

        dropThroughTimer -= tpf;

        if (dropThroughTimer < 0) {
            dropThroughTimer = 0;
        }
    }

    /**
     * 是否正在 Shift 下落穿透期間。
     *
     * @return true 表示正在下落穿透
     */
    private boolean isDroppingThrough() {
        return dropThroughTimer > 0;
    }

 
    // Reset 

    /**
     * 重設普通單向平台系統。
     *
     * 用於：
     * - 玩家死亡
     * - 玩家重生
     * - 場景重新載入
     *
     * 會：
     * 1. 移除目前生成的 solid collider。
     * 2. 移除世界中殘留的 ONE_WAY_PLATFORM_COLLIDER。
     * 3. 清除目前平台狀態。
     * 4. 重置 Shift 下落 timer。
     * 5. 更新 previousPlayerBottom。
     * 6. 清除 PlayerComponent 的 one-way platform 狀態。
     */
    public void reset() {
        removeCurrentSolidCollider();
        removeAllGeneratedSolidColliders();

        currentPlatform = null;
        currentSolidCollider = null;

        dropThroughTimer = 0;
        previousPlayerBottom = getPlayerBottom();

        PlayerComponent playerComponent = getPlayerComponent();

        if (playerComponent != null) {
            playerComponent.setOnOneWayPlatform(false);
        }
    }

    /**
     * 移除所有由普通單向平台生成出的 collider。
     */
    private void removeAllGeneratedSolidColliders() {
        getGameWorld()
                .getEntitiesByType(EntityType.ONE_WAY_PLATFORM_COLLIDER)
                .forEach(entity -> {
                    if (entity.isActive()) {
                        entity.removeFromWorld();
                    }
                });
    }

 
    // Geometry Helpers 

    /**
     * 判斷玩家與平台是否在 X 軸上重疊。
     *
     * @param playerLeft 玩家左側
     * @param playerRight 玩家右側
     * @param platformLeft 平台左側
     * @param platformRight 平台右側
     * @return true 表示水平重疊
     */
    private boolean isHorizontallyOverlapping(
            double playerLeft,
            double playerRight,
            double platformLeft,
            double platformRight
    ) {
        return playerRight > platformLeft + SIDE_PADDING &&
                playerLeft < platformRight - SIDE_PADDING;
    }

    /**
     * 更新上一幀玩家底部 Y。
     */
    private void updatePreviousPlayerBottom() {
        previousPlayerBottom = getPlayerBottom();
    }

    /**
     * 取得玩家底部世界 Y。
     *
     * @return 玩家底部 Y
     */
    private double getPlayerBottom() {
        if (player == null) {
            return 0;
        }

        return player
                .getBoundingBoxComponent()
                .getMaxYWorld();
    }

    /**
     * 取得玩家左側世界 X。
     *
     * @return 玩家左側 X
     */
    private double getPlayerLeft() {
        return player
                .getBoundingBoxComponent()
                .getMinXWorld();
    }

    /**
     * 取得玩家右側世界 X。
     *
     * @return 玩家右側 X
     */
    private double getPlayerRight() {
        return player
                .getBoundingBoxComponent()
                .getMaxXWorld();
    }

 
    // Component Helpers 

    /**
     * 取得 PlayerComponent。
     *
     * @return PlayerComponent；若玩家不存在或沒有該元件，回傳 null
     */
    private PlayerComponent getPlayerComponent() {
        if (player == null || !player.hasComponent(PlayerComponent.class)) {
            return null;
        }

        return player.getComponent(PlayerComponent.class);
    }

    /**
     * 取得玩家 PhysicsComponent。
     *
     * @return PhysicsComponent；若玩家不存在或沒有該元件，回傳 null
     */
    private PhysicsComponent getPhysics() {
        if (player == null || !player.hasComponent(PhysicsComponent.class)) {
            return null;
        }

        return player.getComponent(PhysicsComponent.class);
    }

 
    // Game State Helpers 

    /**
     * 判斷玩家是否死亡。
     *
     * @return true 表示玩家死亡
     */
    private boolean isPlayerDead() {
        return getb("playerDead");
    }
}