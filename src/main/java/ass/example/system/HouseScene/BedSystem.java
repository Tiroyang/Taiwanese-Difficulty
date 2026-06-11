package ass.example.system.HouseScene;

import ass.example.components.HouseScene.BedComponent;
import ass.example.components.PlayerComponent;
import ass.example.core.EntityType;
import ass.example.system.DeathSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.physics.PhysicsComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * BedSystem
 *
 * 專門處理 HouseScene 中「床」的一方通行平台邏輯。
 *
 * 這個系統由 HouseScene 或其他場景系統每幀呼叫 update(tpf)。
 *
 *   ----
 * 床的基本設計
 *   ----
 *
 * 一張床主要分成兩種 Entity：
 *
 * 1. BED_ONE_WAY_PLATFORM
 *    - 固定存在於場景中。
 *    - 作為玩家「從上方落到床上」的偵測入口。
 *    - 玩家真正踩上床後，才會生成實體 collider。
 *
 * 2. BED_ONE_WAY_PLATFORM_COLLIDER
 *    - 玩家成功落到床後動態生成。
 *    - 作為真正支撐玩家站在床上的實體碰撞區。
 *    - 玩家離開床範圍、按 Shift 下落、死亡重生時會被移除。
 *
 *   ----
 * 主要功能
 *   ----
 *
 * 1. 玩家從床平台上方落下時，判定為上床。
 * 2. 上床後，生成一個或多個床面 collider。
 * 3. 玩家站在床上時，設定 PlayerComponent 為 one way platform 狀態。
 * 4. 玩家站在床上時，切換玩家 zIndex。
 * 5. 玩家在床上按跳躍後，記錄床上跳躍次數。
 * 6. 玩家跳起後再次落回床上，依照次數觸發死亡。
 * 7. 玩家按 Shift 時，可從床上往下落。
 * 8. 玩家離開床 collider 範圍後，清除床狀態。
 * 9. 玩家重生時，清除所有動態生成的床 collider 與狀態。
 * 10. 讀檔時，如果玩家原本在床上，可以還原床 collider。
 */
public class BedSystem {
 
    // Game Var Keys 

    /**
     * 玩家是否死亡的全域變數名稱。
     */
    private static final String VAR_PLAYER_DEAD = "playerDead";

    /**
     * 玩家目前是否站在床 collider 上的全域變數名稱。
     *
     * 這個值可被 SaveSystem 儲存，讀檔後再由 applySavedState() 還原床狀態。
     */
    private static final String VAR_PLAYER_ON_BED_COLLIDER = "playerOnBedCollider";

 
    // Spawn Names 

    /**
     * 動態生成床 collider 時使用的 spawn name。
     *
     * 需要在 EntityFactory 裡有對應 @Spawns("bed_one_way_platform_collider")
     */
    private static final String SPAWN_BED_COLLIDER = "bed_one_way_platform_collider";

 
    // Tuning Constants 

    /**
     * 玩家按 Shift 往下落後，暫時忽略床平台吸附的時間。
     *
     * 避免玩家剛穿過床平台時，
     * 下一幀又立刻被判定重新站回床上。
     */
    private static final double DROP_IGNORE_DURATION = 0.28;

    /**
     * 玩家按 Shift 往下落時給予的向下速度。
     *
     * 讓玩家能確實離開床 collider。
     */
    private static final double DROP_VELOCITY_Y = 260.0;

    /**
     * 玩家重生後，暫時忽略床落地判定的時間。
     *
     * 避免 previousPlayerBottom 還殘留死亡前的位置，
     * 導致重生後第一幀誤判為落到床上。
     */
    private static final double RESET_IGNORE_DURATION = 0.25;

    /**
     * 落地判定容許誤差。
     *
     * 不可為0，否則將無法判定落床。
     */
    private static final double LANDING_TOLERANCE = 2.0;

    /**
     * 水平判定內縮值。
     *
     * 避免玩家只是碰到床平台邊緣，就被判定為站在床上。
     */
    private static final double SIDE_PADDING = 8.0;

    /**
     * 觸發死亡所需的床上跳躍次數。
     */
    private static final int REQUIRED_BED_JUMPS_BEFORE_DEATH = 2;

    /**
     * reset() 時使用的預設玩家 zIndex。
     * 這裡保留一個安全預設值。
     */
    private static final int DEFAULT_PLAYER_Z_INDEX = 0;

 
    // Dependencies 

    /**
     * 玩家 Entity。
     */
    private final Entity player;

    /**
     * 死亡系統。
     *
     * 當玩家達成床上跳躍死亡條件時，透過 deathSystem.die(...) 觸發死亡。
     */
    private final DeathSystem deathSystem = DeathSystem.getInstance();

 
    // Current Bed State 

    /**
     * 玩家目前所在的床入口平台。
     *
     * 此 Entity 類型應該是：
     * EntityType.BED_ONE_WAY_PLATFORM
     *
     * 若為 null，代表玩家目前不在床系統管理中的床上。
     */
    private Entity currentBedPlatform;

    /**
     * 目前由床平台動態生成出的實體床面 collider。
     *
     * 一張床可能有一個或兩個 collider，
     * 所以使用 List 保存。
     */
    private final List<Entity> currentBedColliders = new ArrayList<>();

 
    // Runtime Detection State 

    /**
     * 上一幀玩家底部 Y 座標。
     *
     * 用來判斷玩家是否從平台上方穿越平台頂部。
     */
    private double previousPlayerBottom;

    /**
     * Shift 下落後的忽略計時器。
     *
     * 大於 0 時，不進行床平台落地判定。
     */
    private double dropIgnoreTimer = 0;

    /**
     * 重生後的忽略計時器。
     *
     * 大於 0 時，不進行床平台落地判定。
     */
    private double resetIgnoreTimer = 0;

 
    // Bed Jump State 

    /**
     * 玩家是否已經確定落在床上。
     *
     * 只有這個值為 true 時，玩家按跳躍鍵才會被視為「從床上跳起」。
     */
    private boolean hasLandedOnBed = false;

    /**
     * 玩家在床上跳躍的次數。
     *
     * 每次玩家站在床上按跳躍鍵時 +1。
     */
    private int bedJumpCount = 0;

    /**
     * 是否正在等待玩家跳起後再次落回床上。
     *
     * 避免玩家在空中連續按跳躍鍵，導致 bedJumpCount 被重複計算。
     */
    private boolean waitingForLandingAfterBedJump = false;

 
    // Constructor 

    /**
     * 建立床系統。
     *
     * @param player 玩家 Entity
     */
    public BedSystem(Entity player) {
        this.player = player;
        this.previousPlayerBottom = getPlayerBottom();
    }

 
    // Public API 

    /**
     * 每幀更新床系統。
     *
     * 更新流程：
     * 1. 玩家死亡時不更新床系統。
     * 2. 重生後短時間內不偵測床，避免誤判。
     * 3. Shift 下落期間不重新吸附床。
     * 4. 如果玩家目前已在床 collider 上，更新床上狀態。
     * 5. 如果玩家尚未在床上，尋找可落下的床平台。
     * 6. 每幀最後更新 previousPlayerBottom。
     *
     * @param tpf time per frame
     */
    public void update(double tpf) {
        if (isPlayerDead()) {
            return;
        }

        if (updateResetIgnoreTimer(tpf)) {
            updatePreviousPlayerBottom();
            return;
        }

        updateDropIgnoreTimer(tpf);

        if (isIgnoringPlatformBecauseDropping()) {
            updatePreviousPlayerBottom();
            return;
        }

        if (isPlayerOnGeneratedBedCollider()) {
            updatePlayerOnBedCollider();
            updatePreviousPlayerBottom();
            return;
        }

        findBedPlatformToLandOn().ifPresent(this::landOnBedPlatform);

        updatePreviousPlayerBottom();
    }

    /**
     * 讀檔後套用床狀態。
     *
     * SaveSystem 若儲存了 playerOnBedCollider == true 代表玩家存檔時站在床上。
     *
     * 讀檔後需要：
     * 1. 找到離玩家最近且合理的 BED_ONE_WAY_PLATFORM。
     * 2. 重新生成床 collider。
     * 3. 恢復玩家 one way platform 狀態。
     * 4. 恢復玩家床上 zIndex。
     *
     * 如果找不到可還原的床平台，
     * 則清除 playerOnBedCollider 狀態。
     */
    public void applySavedState() {
        if (!getb(VAR_PLAYER_ON_BED_COLLIDER)) {
            return;
        }

        Optional<Entity> bedPlatform = findNearestBedPlatformForRestore();

        if (bedPlatform.isEmpty()) {
            clearSavedBedState();
            return;
        }

        restorePlayerOnBed(bedPlatform.get());
    }

    /**
     * 玩家按下跳躍鍵時呼叫。
     *
     * 由場景在玩家按跳時呼叫。
     *
     * 判定：
     * 1. 玩家必須目前正在床 collider 上。
     * 2. 玩家必須已經確實落在床上。
     * 3. 滿足條件後，記錄一次床上跳躍。
     */
    public void onPlayerJumpPressed() {
        if (!isPlayerOnGeneratedBedCollider()) {
            return;
        }

        if (!hasLandedOnBed) {
            return;
        }

        bedJumpCount++;
        waitingForLandingAfterBedJump = true;
        hasLandedOnBed = false;
    }

    /**
     * 玩家按 Shift 往下穿過床。
     *
     * 條件：
     * 1. 玩家目前必須在床 collider 上。
     * 2. 玩家仍位於目前床平台的水平範圍內。
     *
     * 成功下落後：
     * 1. 移除動態生成的床 collider。
     * 2. 清除玩家 one way platform 狀態。
     * 3. 還原玩家 zIndex。
     * 4. 清除床跳躍狀態。
     * 5. 啟動 dropIgnoreTimer。
     * 6. 給玩家一個向下速度。
     */
    public void dropThrough() {
        if (!isPlayerOnGeneratedBedCollider()) {
            return;
        }

        if (!isPlayerAboveCurrentBedPlatform()) {
            return;
        }

        BedComponent bed = getCurrentBedComponent();

        removeCurrentBedColliders();

        getPlayerComponent().setOnOneWayPlatform(false);
        player.setZIndex(bed.getNormalPlayerZIndex());

        clearCurrentBedState();
        set(VAR_PLAYER_ON_BED_COLLIDER, false);

        dropIgnoreTimer = DROP_IGNORE_DURATION;

        getPhysics().setVelocityY(DROP_VELOCITY_Y);
    }

    /**
     * 重設床系統。
     *
     * 會清除：
     * 1. 目前追蹤的床平台。
     * 2. 目前動態生成的床 collider。
     * 3. 世界中殘留的 BED_ONE_WAY_PLATFORM_COLLIDER。
     * 4. Shift 下落與重生忽略計時器。
     * 5. 床上跳躍狀態。
     * 6. playerOnBedCollider game var。
     * 7. PlayerComponent 的 one way platform 狀態。
     * 8. 玩家 zIndex。
     */
    public void reset() {
        removeCurrentBedColliders();
        removeAllResidualBedCollidersFromWorld();

        clearCurrentBedState();

        dropIgnoreTimer = 0;
        resetIgnoreTimer = RESET_IGNORE_DURATION;

        updatePreviousPlayerBottom();

        set(VAR_PLAYER_ON_BED_COLLIDER, false);

        getPlayerComponent().setOnOneWayPlatform(false);
        player.setZIndex(DEFAULT_PLAYER_Z_INDEX);
    }

 
    // Update Helpers 

    /**
     * 判斷玩家是否死亡。
     *
     * @return true 表示玩家目前處於死亡狀態
     */
    private boolean isPlayerDead() {
        return getb(VAR_PLAYER_DEAD);
    }

    /**
     * 更新重生後忽略計時器。
     *
     * @param tpf time per frame
     * @return true 表示目前仍在重生忽略期間
     */
    private boolean updateResetIgnoreTimer(double tpf) {
        if (resetIgnoreTimer <= 0) {
            return false;
        }

        resetIgnoreTimer -= tpf;

        if (resetIgnoreTimer < 0) {
            resetIgnoreTimer = 0;
        }

        return true;
    }

    /**
     * 更新 Shift 下落忽略計時器。
     *
     * @param tpf time per frame
     */
    private void updateDropIgnoreTimer(double tpf) {
        if (dropIgnoreTimer <= 0) {
            return;
        }

        dropIgnoreTimer -= tpf;

        if (dropIgnoreTimer < 0) {
            dropIgnoreTimer = 0;
        }
    }

    /**
     * 判斷目前是否因為 Shift 下落而暫時忽略床平台吸附。
     *
     * @return true 表示暫時不應判定落到床上
     */
    private boolean isIgnoringPlatformBecauseDropping() {
        return dropIgnoreTimer > 0;
    }

    /**
     * 更新 previousPlayerBottom。
     */
    private void updatePreviousPlayerBottom() {
        previousPlayerBottom = getPlayerBottom();
    }

 
    // Save / Restore 

    /**
     * 尋找讀檔後最適合還原玩家床狀態的床平台。
     *
     * 會從所有 BED_ONE_WAY_PLATFORM 中找出玩家水平位置最接近且合理重疊的床。
     *
     * @return 最適合還原的床平台
     */
    private Optional<Entity> findNearestBedPlatformForRestore() {
        return getGameWorld()
                .getEntitiesByType(EntityType.BED_ONE_WAY_PLATFORM)
                .stream()
                .filter(this::isPlayerNearBedPlatformForRestore)
                .min(Comparator.comparingDouble(bedPlatform -> bedPlatform.distance(player)));
    }

    /**
     * 判斷玩家目前位置是否接近指定床平台。
     *
     * 讀檔時玩家可能站在：
     * 1. 原始平台範圍上方。
     * 2. 第一個 collider 範圍上方。
     * 3. 第二個 collider 範圍上方。
     *
     * @param bedPlatform 床平台 Entity
     * @return true 表示玩家位置接近此床
     */
    private boolean isPlayerNearBedPlatformForRestore(Entity bedPlatform) {
        BedComponent bed = bedPlatform.getComponent(BedComponent.class);

        boolean abovePlatform = isPlayerHorizontallyOverArea(
                bedPlatform.getX(),
                bed.getPlatformWidth()
        );

        boolean aboveFirstCollider = isPlayerHorizontallyOverArea(
                bedPlatform.getX() + bed.getFirstColliderArea().getOffsetX(),
                bed.getFirstColliderArea().getWidth()
        );

        boolean aboveSecondCollider =
                bed.hasSecondColliderArea() &&
                        isPlayerHorizontallyOverArea(
                                bedPlatform.getX() + bed.getSecondColliderArea().getOffsetX(),
                                bed.getSecondColliderArea().getWidth()
                        );

        return abovePlatform || aboveFirstCollider || aboveSecondCollider;
    }

    /**
     * 將玩家還原到床上狀態。
     *
     * @param bedPlatform 讀檔後找到的床平台
     */
    private void restorePlayerOnBed(Entity bedPlatform) {
        BedComponent bed = bedPlatform.getComponent(BedComponent.class);

        removeCurrentBedColliders();

        currentBedPlatform = bedPlatform;
        createCurrentBedColliders(bedPlatform, bed);

        getPlayerComponent().setOnOneWayPlatform(true);

        set(VAR_PLAYER_ON_BED_COLLIDER, true);

        player.setZIndex(bed.getPlayerZIndexOnBed());

        hasLandedOnBed = true;
        bedJumpCount = 0;
        waitingForLandingAfterBedJump = false;

        updatePreviousPlayerBottom();
    }

    /**
     * 找不到可還原床平台時，清除存檔床狀態。
     */
    private void clearSavedBedState() {
        set(VAR_PLAYER_ON_BED_COLLIDER, false);

        getPlayerComponent().setOnOneWayPlatform(false);
        player.setZIndex(DEFAULT_PLAYER_Z_INDEX);

        clearCurrentBedState();
    }

 
    // Landing Detection 

    /**
     * 尋找玩家這一幀是否可以落到某個床平台。
     *
     * 條件：
     * 1. 玩家不能正在向上跳。
     * 2. 玩家必須與床平台水平重疊。
     * 3. 玩家必須從床平台頂部上方穿越到頂部附近或下方。
     *
     * @return 可落下的床平台
     */
    private Optional<Entity> findBedPlatformToLandOn() {
        if (getPhysics().getVelocityY() < 0) {
            return Optional.empty();
        }

        return getGameWorld()
                .getEntitiesByType(EntityType.BED_ONE_WAY_PLATFORM)
                .stream()
                .filter(this::canLandOnBedPlatform)
                .min(Comparator.comparingDouble(Entity::getY));
    }

    /**
     * 判斷玩家是否可落到指定床平台。
     *
     * @param bedPlatform 床平台 Entity
     * @return true 表示玩家可落到此床平台
     */
    private boolean canLandOnBedPlatform(Entity bedPlatform) {
        BedComponent bed = bedPlatform.getComponent(BedComponent.class);

        double platformTop = bedPlatform.getY();
        double playerBottom = getPlayerBottom();

        boolean horizontalOverlap = isPlayerHorizontallyOverArea(
                bedPlatform.getX(),
                bed.getPlatformWidth()
        );

        boolean crossedPlatformTop = hasPlayerCrossedTopSurface(
                platformTop,
                playerBottom
        );

        return horizontalOverlap && crossedPlatformTop;
    }

    /**
     * 玩家成功落到床平台。
     *
     * 處理內容：
     * 1. 記錄目前床平台。
     * 2. 生成床 collider。
     * 3. 設定玩家為 one way platform 狀態。
     * 4. 設定 playerOnBedCollider game var。
     * 5. 切換玩家 zIndex。
     * 6. 標記玩家已落到床上。
     *
     * @param bedPlatform 玩家落到的床平台
     */
    private void landOnBedPlatform(Entity bedPlatform) {
        BedComponent bed = bedPlatform.getComponent(BedComponent.class);

        currentBedPlatform = bedPlatform;

        createCurrentBedColliders(bedPlatform, bed);

        getPlayerComponent().setOnOneWayPlatform(true);

        set(VAR_PLAYER_ON_BED_COLLIDER, true);
        player.setZIndex(bed.getPlayerZIndexOnBed());

        hasLandedOnBed = true;
    }

 
    // On Bed Update 

    /**
     * 玩家已經在床 collider 上時，每幀更新床狀態。
     *
     * 流程：
     * 1. 如果玩家已不在任何床 collider 上方，完整離開床。
     * 2. 如果玩家仍在床上，維持 playerOnBedCollider 狀態。
     * 3. 維持玩家床上 zIndex。
     * 4. 判斷玩家是否剛剛落回床 collider。
     * 5. 如果剛落回床，處理床上跳躍死亡邏輯。
     */
    private void updatePlayerOnBedCollider() {
        BedComponent bed = getCurrentBedComponent();

        if (!isPlayerAboveCurrentBedColliders()) {
            leaveCurrentBed(bed);
            return;
        }

        set(VAR_PLAYER_ON_BED_COLLIDER, true);
        player.setZIndex(bed.getPlayerZIndexOnBed());

        if (hasJustLandedOnCurrentBedCollider()) {
            handleLandingBackOnBed(bed);
        }
    }

    /**
     * 處理玩家從床上跳起後再次落回床上的邏輯。
     *
     * 若玩家不是從床上跳起，就只更新 hasLandedOnBed。
     *
     * 若玩家是從床上跳起並落回床上：
     * 1. 關閉 waitingForLandingAfterBedJump。
     * 2. 檢查床上跳躍次數。
     * 3. 若達到 REQUIRED_BED_JUMPS_BEFORE_DEATH，觸發死亡。
     *
     * @param bed 目前床資料
     */
    private void handleLandingBackOnBed(BedComponent bed) {
        hasLandedOnBed = true;

        if (!waitingForLandingAfterBedJump) {
            return;
        }

        waitingForLandingAfterBedJump = false;

        if (bedJumpCount >= REQUIRED_BED_JUMPS_BEFORE_DEATH) {
            deathSystem.die(bed.getDeathReasonOnSecondLanding());
        }
    }

    /**
     * 玩家完整離開目前床。
     *
     * 會清除：
     * 1. 動態床 collider。
     * 2. PlayerComponent one way platform 狀態。
     * 3. 玩家 zIndex。
     * 4. 目前床狀態。
     * 5. playerOnBedCollider game var。
     *
     * @param bed 目前床資料
     */
    private void leaveCurrentBed(BedComponent bed) {
        removeCurrentBedColliders();

        getPlayerComponent().setOnOneWayPlatform(false);
        player.setZIndex(bed.getNormalPlayerZIndex());

        clearCurrentBedState();

        set(VAR_PLAYER_ON_BED_COLLIDER, false);
    }

 
    // Bed Collider Creation / Removal 

    /**
     * 根據 BedComponent 建立目前床需要的 collider。
     *
     * 目前支援：
     * 1. 第一組 collider。
     * 2. 第二組 collider，可選。
     *
     * @param bedPlatform 床平台 Entity
     * @param bed 床資料 Component
     */
    private void createCurrentBedColliders(Entity bedPlatform, BedComponent bed) {
        removeCurrentBedColliders();

        spawnBedColliderIfValid(
                bedPlatform,
                bed,
                bed.getBedId() + "_collider_1",
                bed.getFirstColliderArea()
        );

        if (bed.hasSecondColliderArea()) {
            spawnBedColliderIfValid(
                    bedPlatform,
                    bed,
                    bed.getBedId() + "_collider_2",
                    bed.getSecondColliderArea()
            );
        }
    }

    /**
     * 如果 colliderArea 有效，生成床 collider。
     *
     * @param bedPlatform 床平台 Entity
     * @param bed 床資料 Component
     * @param colliderId collider ID
     * @param colliderArea collider 位置與尺寸資料
     */
    private void spawnBedColliderIfValid(
            Entity bedPlatform,
            BedComponent bed,
            String colliderId,
            BedComponent.ColliderArea colliderArea
    ) {
        if (colliderArea == null || !colliderArea.isValid()) {
            return;
        }

        Entity collider = spawnBedCollider(
                bedPlatform,
                bed,
                colliderId,
                colliderArea
        );

        currentBedColliders.add(collider);
    }

    /**
     * 生成單一床 collider。
     *
     * @param bedPlatform 床平台 Entity
     * @param bed 床資料 Component
     * @param colliderId collider ID
     * @param colliderArea collider 位置與尺寸資料
     * @return 生成出的 collider Entity
     */
    private Entity spawnBedCollider(
            Entity bedPlatform,
            BedComponent bed,
            String colliderId,
            BedComponent.ColliderArea colliderArea
    ) {
        double colliderX = bedPlatform.getX() + colliderArea.getOffsetX();
        double colliderY = bedPlatform.getY() + colliderArea.getOffsetY();

        return spawn(SPAWN_BED_COLLIDER, new SpawnData(colliderX, colliderY)
                .put("id", colliderId)
                .put("width", colliderArea.getWidth())
                .put("height", colliderArea.getHeight())
                .put("playerZIndexOnBed", bed.getPlayerZIndexOnBed())
                .put("normalPlayerZIndex", bed.getNormalPlayerZIndex())
                .put("deathReason", bed.getDeathReasonOnSecondLanding()));
    }

    /**
     * 移除目前床動態生成出的所有 collider。
     */
    private void removeCurrentBedColliders() {
        currentBedColliders.forEach(Entity::removeFromWorld);
        currentBedColliders.clear();
    }

    /**
     * 移除世界中所有殘留的床 collider。
     *
     * 用途：
     * reset() 時防止因為死亡、讀檔或場景切換，
     * 造成動態生成的 collider 殘留在世界中。
     */
    private void removeAllResidualBedCollidersFromWorld() {
        getGameWorld()
                .getEntitiesByType(EntityType.BED_ONE_WAY_PLATFORM_COLLIDER)
                .forEach(Entity::removeFromWorld);
    }

 
    // State Helpers 

    /**
     * 判斷玩家目前是否在動態生成的床 collider 上。
     *
     * @return true 表示目前有床平台與床 collider 正在被追蹤
     */
    private boolean isPlayerOnGeneratedBedCollider() {
        return currentBedPlatform != null && !currentBedColliders.isEmpty();
    }

    /**
     * 清除目前床相關狀態。
     *
     * 若要移除床 collider，要先呼叫 removeCurrentBedColliders()。
     */
    private void clearCurrentBedState() {
        currentBedPlatform = null;
        currentBedColliders.clear();

        hasLandedOnBed = false;
        bedJumpCount = 0;
        waitingForLandingAfterBedJump = false;
    }

    /**
     * 取得目前床平台上的 BedComponent。
     *
     * 呼叫前應確保 currentBedPlatform 不為 null。
     *
     * @return 目前床資料 Component
     */
    private BedComponent getCurrentBedComponent() {
        return currentBedPlatform.getComponent(BedComponent.class);
    }

 
    // Geometry Checks 

    /**
     * 判斷玩家是否仍在目前床平台的水平範圍上方。
     *
     * 主要給 Shift 下落使用。
     *
     * @return true 表示玩家仍在目前床平台上方
     */
    private boolean isPlayerAboveCurrentBedPlatform() {
        if (currentBedPlatform == null) {
            return false;
        }

        BedComponent bed = getCurrentBedComponent();

        return isPlayerHorizontallyOverArea(
                currentBedPlatform.getX(),
                bed.getPlatformWidth()
        );
    }

    /**
     * 判斷玩家是否仍在目前任一床 collider 的水平範圍上方。
     *
     * @return true 表示玩家仍在床 collider 上方
     */
    private boolean isPlayerAboveCurrentBedColliders() {
        if (currentBedColliders.isEmpty()) {
            return false;
        }

        return currentBedColliders.stream()
                .anyMatch(this::isPlayerHorizontallyOverEntity);
    }

    /**
     * 判斷玩家是否剛剛從上方落回目前床 collider。
     *
     * 用於床上跳躍死亡判定。
     *
     * @return true 表示玩家剛落回床面
     */
    private boolean hasJustLandedOnCurrentBedCollider() {
        if (currentBedColliders.isEmpty()) {
            return false;
        }

        if (getPhysics().getVelocityY() < 0) {
            return false;
        }

        double playerBottom = getPlayerBottom();

        Entity firstCollider = currentBedColliders.get(0);
        double colliderTop = firstCollider.getY();

        return hasPlayerCrossedTopSurface(colliderTop, playerBottom);
    }

    /**
     * 判斷玩家是否水平重疊某個 Entity。
     *
     * @param targetEntity 目標 Entity
     * @return true 表示玩家水平範圍與目標 Entity 重疊
     */
    private boolean isPlayerHorizontallyOverEntity(Entity targetEntity) {
        double targetLeft = targetEntity.getBoundingBoxComponent().getMinXWorld();
        double targetRight = targetEntity.getBoundingBoxComponent().getMaxXWorld();

        return isPlayerHorizontallyOverRange(targetLeft, targetRight);
    }

    /**
     * 判斷玩家是否水平重疊某個區域。
     *
     * @param areaLeft 區域左側 X
     * @param areaWidth 區域寬度
     * @return true 表示玩家水平範圍與該區域重疊
     */
    private boolean isPlayerHorizontallyOverArea(double areaLeft, double areaWidth) {
        double areaRight = areaLeft + areaWidth;

        return isPlayerHorizontallyOverRange(areaLeft, areaRight);
    }

    /**
     * 判斷玩家是否水平重疊指定 X 範圍。
     *
     * 這裡會套用 SIDE_PADDING，避免玩家只碰到邊緣就被判定為站在床上。
     *
     * @param rangeLeft 範圍左側 X
     * @param rangeRight 範圍右側 X
     * @return true 表示玩家水平範圍與該範圍重疊
     */
    private boolean isPlayerHorizontallyOverRange(double rangeLeft, double rangeRight) {
        double playerLeft = player.getBoundingBoxComponent().getMinXWorld();
        double playerRight = player.getBoundingBoxComponent().getMaxXWorld();

        return playerRight > rangeLeft + SIDE_PADDING &&
                playerLeft < rangeRight - SIDE_PADDING;
    }

    /**
     * 判斷玩家底部是否從上一幀到這一幀穿越指定頂部表面。
     *
     * 用途：
     * 1. 判斷玩家是否落到 BED_ONE_WAY_PLATFORM。
     * 2. 判斷玩家是否再次落回床 collider。
     *
     * @param surfaceTop 目標表面的 Y 座標
     * @param currentPlayerBottom 目前玩家底部 Y 座標
     * @return true 表示玩家剛穿越該表面
     */
    private boolean hasPlayerCrossedTopSurface(
            double surfaceTop,
            double currentPlayerBottom
    ) {
        return previousPlayerBottom <= surfaceTop + LANDING_TOLERANCE &&
                currentPlayerBottom >= surfaceTop - LANDING_TOLERANCE;
    }

 
    // Component Getters 

    /**
     * 取得玩家底部世界座標。
     *
     * @return 玩家 bounding box 的最大 Y
     */
    private double getPlayerBottom() {
        return player.getBoundingBoxComponent().getMaxYWorld();
    }

    /**
     * 取得玩家 PlayerComponent。
     *
     * @return PlayerComponent
     */
    private PlayerComponent getPlayerComponent() {
        return player.getComponent(PlayerComponent.class);
    }

    /**
     * 取得玩家 PhysicsComponent。
     *
     * @return PhysicsComponent
     */
    private PhysicsComponent getPhysics() {
        return player.getComponent(PhysicsComponent.class);
    }
}