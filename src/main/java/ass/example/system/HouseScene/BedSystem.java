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
 * 專門處理「床」的平台邏輯。
 *
 * 床分成三種 Entity：
 * 1. BED_ONE_WAY_PLATFORM
 *    - 玩家從這個區域上方落下時，會觸發上床
 *    - 可以在這個區域上方按Shift下落
 * 2. BED_ONE_WAY_PLATFORM_COLLIDER1
 *    - 觸發上床才生成的實體床面
 *    - 支撐玩家站在床上
 *    - 觸發死亡邏輯
 * 3. BED_ONE_WAY_PLATFORM_COLLIDER2
 *    - 觸發上床才生成的實體床面
 *    - 支撐玩家站在床上
 *
 * 主要功能：
 * 1. 玩家從 BED_ONE_WAY_PLATFORM 上方落下時，生成床面 collider
 * 2. 玩家站在床上時，改變玩家 zIndex
 * 3. 玩家在床上按跳躍後，記錄 jumpedFromBed
 * 4. 玩家再次落回床面時，觸發死亡
 * 5. 玩家離開床 collider 上方時，恢復普通狀態
 * 6. 重生時清除所有暫時生成的床 collider 與床狀態
 */
public class BedSystem {

    private final Entity player;
    private final DeathSystem deathSystem;

    /**
     * 玩家目前是從哪一個BED_ONE_WAY_PLATFORM上床的。
     *
     * 若為 null，代表目前沒有站在床相關平台上。
     */
    private Entity currentBedPlatform;
    /**
     * 玩家跳上床後動態生成的實體床面。
     * 玩家只要還在第一個 collider 上方，就視為仍在床上。
     */
    private final List<Entity> currentBedColliders = new ArrayList<>();

    /**
     * 上一幀玩家底部 Y 座標。
     * 用來判斷玩家是否「從上方穿越平台頂部」。
     */
    private double previousPlayerBottom;

    /**
     * 按 Shift 下落後的暫時忽略時間。
     */
    private double dropTimer = 0;
    private final double dropDuration = 0.28;

    /**
     * 落到床上的誤差。
     */
    private final double landingTolerance = 2;
    /**
     * 水平判定內縮值。
     * 避免玩家只是碰到平台邊緣，就被判定落到床上。
     */
    private final double sidePadding = 8;

    /**
     * 是否已經曾經落到床上。
     */
    private boolean hasLandedOnBed = false;

    /**
     * 是否曾經從床上跳起。
     *
     * 如果玩家在床上跳起，再次落回床上，觸發死亡。
     */
    private boolean jumpedFromBed = false;

    /**
     * 重生後短時間內不做床落地判定，避免previousPlayerBottom還殘留死亡前的位置導致誤判。
     */
    private double resetIgnoreTimer = 0;
    private final double resetIgnoreDuration = 0.25;

    /**
     * 玩家的默認zIndex。
     */
    private final int normalPlayerZIndex = 0;

    // Constructor
    public BedSystem(Entity player, DeathSystem deathSystem) {
        this.player = player;
        this.deathSystem = deathSystem;
        this.previousPlayerBottom = getPlayerBottom();
    }

    /**
     * 隨時更新床系統。
     *
     * 流程：
     * 1. 玩家死亡時不更新
     * 2. 重生後短時間內不偵測床
     * 3. Shift下落期間不重新吸附床
     * 4. 如果已經有床collider，更新床上狀態
     * 5. 如果尚未上床，尋找是否可從BED_ONE_WAY_PLATFORM上床
     */
    public void update(double tpf) {
        if (getb("playerDead")) {
            return;
        }

        if (resetIgnoreTimer > 0) {
            resetIgnoreTimer -= tpf;

            if (resetIgnoreTimer < 0) {
                resetIgnoreTimer = 0;
            }

            previousPlayerBottom = getPlayerBottom();
            return;
        }

        updateDropTimer(tpf);

        if (isDroppingThrough()) {
            previousPlayerBottom = getPlayerBottom();
            return;
        }

        if (!currentBedColliders.isEmpty()) {
            updateWhileOnBedCollider();
            previousPlayerBottom = getPlayerBottom();
            return;
        }

        Optional<Entity> bedPlatform = findBedPlatformToLandOn();

        bedPlatform.ifPresent(this::landOnBedPlatform);

        previousPlayerBottom = getPlayerBottom();
    }

    public void applySavedState() {
        if (!getb("playerOnBedCollider")) {
            return;
        }

        Optional<Entity> bedPlatform = findNearestBedPlatformForSavedPlayer();

        if (bedPlatform.isEmpty()) {
            set("playerOnBedCollider", false);
            getPlayerComponent().setOnOneWayPlatform(false);
            player.setZIndex(normalPlayerZIndex);
            return;
        }

        restorePlayerOnBed(bedPlatform.get());
    }

    private Optional<Entity> findNearestBedPlatformForSavedPlayer() {
        return getGameWorld()
                .getEntitiesByType(EntityType.BED_ONE_WAY_PLATFORM)
                .stream()
                .filter(this::isPlayerNearBedPlatformForRestore)
                .min(Comparator.comparingDouble(e -> e.distance(player)));
    }

    private boolean isPlayerNearBedPlatformForRestore(Entity bedPlatform) {
        BedComponent bed = bedPlatform.getComponent(BedComponent.class);

        double platformLeft = bedPlatform.getX();
        double platformRight = bedPlatform.getX() + bed.getPlatformWidth();

        double collider1Left = bedPlatform.getX() + bed.getCollider1OffsetX();
        double collider1Right = collider1Left + bed.getCollider1Width();

        double collider2Left = bedPlatform.getX() + bed.getCollider2OffsetX();
        double collider2Right = collider2Left + bed.getCollider2Width();

        double playerLeft = player.getBoundingBoxComponent().getMinXWorld();
        double playerRight = player.getBoundingBoxComponent().getMaxXWorld();

        boolean abovePlatform =
                playerRight > platformLeft + sidePadding &&
                        playerLeft < platformRight - sidePadding;

        boolean aboveCollider1 =
                playerRight > collider1Left + sidePadding &&
                        playerLeft < collider1Right - sidePadding;

        boolean aboveCollider2 =
                bed.hasSecondCollider() &&
                        playerRight > collider2Left + sidePadding &&
                        playerLeft < collider2Right - sidePadding;

        return abovePlatform || aboveCollider1 || aboveCollider2;
    }

    private void restorePlayerOnBed(Entity bedPlatform) {
        BedComponent bed = bedPlatform.getComponent(BedComponent.class);

        removeBedCollider();

        currentBedPlatform = bedPlatform;

        createBedCollider(bedPlatform, bed);

        getPlayerComponent().setOnOneWayPlatform(true);

        set("playerOnBedCollider", true);

        player.setZIndex(bed.getPlayerZIndexOnBed());

        hasLandedOnBed = true;

        jumpedFromBed = false;

        previousPlayerBottom = getPlayerBottom();
    }

    /**
     * 玩家按下跳躍鍵時呼叫。
     *
     * 如果玩家是在床上按跳，代表他從床上跳起。
     * 之後再次落回床上就會死亡。
     */
    public void onPlayerJumpPressed() {
        if (currentBedPlatform == null || currentBedColliders.isEmpty()) {
            return;
        }

        if (hasLandedOnBed) {
            jumpedFromBed = true;
        }
    }

    /**
     * Shift 下落。
     * 只有玩家還在BED_ONE_WAY_PLATFORM上方時，才允許下落。
     */
    public void dropThrough() {
        if (currentBedPlatform == null || currentBedColliders.isEmpty()) {
            return;
        }

        if (!isPlayerAboveCurrentBedPlatform()) {
            return;
        }

        BedComponent bed = currentBedPlatform.getComponent(BedComponent.class);

        removeBedCollider();

        PlayerComponent pc = getPlayerComponent();
        PhysicsComponent physics = getPhysics();

        pc.setOnOneWayPlatform(false);

        player.setZIndex(bed.getNormalPlayerZIndex());

        currentBedPlatform = null;
        jumpedFromBed = false;

        set("playerOnBedCollider", false);

        dropTimer = dropDuration;

        physics.setVelocityY(260);
    }

    /**
     * 尋找符合玩家可落到的BED_ONE_WAY_PLATFORM。
     *
     * 條件：
     * 1. 玩家不能正在往上跳
     * 2. 玩家必須從平台上方落下
     * 3. 玩家水平位置必須與平台重疊
     */
    private Optional<Entity> findBedPlatformToLandOn() {
        PhysicsComponent physics = getPhysics();

        if (physics.getVelocityY() < 0) {
            return Optional.empty();
        }

        return getGameWorld()
                .getEntitiesByType(EntityType.BED_ONE_WAY_PLATFORM)
                .stream()
                .filter(this::canLandOnBedPlatform)
                .min(Comparator.comparingDouble(Entity::getY));
    }

    /**
     * 判斷玩家是否可以落到指定BED_ONE_WAY_PLATFORM。
     */
    private boolean canLandOnBedPlatform(Entity bedPlatform) {
        BedComponent bed = bedPlatform.getComponent(BedComponent.class);

        double platformTop = bedPlatform.getY();
        double platformLeft = bedPlatform.getX();
        double platformRight = bedPlatform.getX() + bed.getPlatformWidth();

        double playerLeft = player.getBoundingBoxComponent().getMinXWorld();
        double playerRight = player.getBoundingBoxComponent().getMaxXWorld();
        double playerBottom = getPlayerBottom();

        boolean xOverlap =
                playerRight > platformLeft + sidePadding &&
                        playerLeft < platformRight - sidePadding;

        boolean crossedPlatformTop =
                previousPlayerBottom <= platformTop + landingTolerance &&
                        playerBottom >= platformTop - landingTolerance;

        return xOverlap && crossedPlatformTop;
    }

    /**
     * 玩家成功落到 BED_ONE_WAY_PLATFORM。
     *
     * 1. 記錄目前床入口平台
     * 2. 生成床面collider
     * 3. 設定玩家狀態在one way platform上
     * 4. 改變玩家zIndex
     * 5. 判斷是否觸發二次落床死亡
     */
    private void landOnBedPlatform(Entity bedPlatform) {
        BedComponent bed = bedPlatform.getComponent(BedComponent.class);

        currentBedPlatform = bedPlatform;

        createBedCollider(bedPlatform, bed);

        getPlayerComponent().setOnOneWayPlatform(true);

        set("playerOnBedCollider", true);
        player.setZIndex(bed.getPlayerZIndexOnBed());

        if (jumpedFromBed) {
            deathSystem.die(bed.getDeathReasonOnSecondLanding());
            return;
        }

        hasLandedOnBed = true;
    }

    /**
     * 根據BedComponent的資料生成床面collider。
     * 目前支援二組
     * 若hasSecondCollider() == false，就只生成第一組。
     */
    private Entity spawnBedCollider(
            Entity bedPlatform,
            BedComponent bed,
            String id,
            double offsetX,
            double offsetY,
            double width,
            double height
    ) {
        double colliderX = bedPlatform.getX() + offsetX;
        double colliderY = bedPlatform.getY() + offsetY;

        return spawn("bed_one_way_platform_collider", new SpawnData(colliderX, colliderY)
                .put("id", id)
                .put("width", width)
                .put("height", height)
                .put("playerZIndexOnBed", bed.getPlayerZIndexOnBed())
                .put("normalPlayerZIndex", bed.getNormalPlayerZIndex())
                .put("deathReason", bed.getDeathReasonOnSecondLanding()));
    }

    private void createBedCollider(Entity bedPlatform, BedComponent bed) {
        removeBedCollider();

        Entity collider1 = spawnBedCollider(
                bedPlatform,
                bed,
                bed.getBedId() + "_collider_1",
                bed.getCollider1OffsetX(),
                bed.getCollider1OffsetY(),
                bed.getCollider1Width(),
                bed.getCollider1Height()
        );

        currentBedColliders.add(collider1);

        if (bed.hasSecondCollider()) {
            Entity collider2 = spawnBedCollider(
                    bedPlatform,
                    bed,
                    bed.getBedId() + "_collider_2",
                    bed.getCollider2OffsetX(),
                    bed.getCollider2OffsetY(),
                    bed.getCollider2Width(),
                    bed.getCollider2Height()
            );

            currentBedColliders.add(collider2);
        }
    }

    /**
     * 玩家已經在床collider上時，每幀更新床狀態。
     *
     * 如果玩家離開collider的上方：
     * - 移除床collider
     * - 恢復zIndex
     * - 清除床狀態
     *
     * 如果玩家仍在床 collider 上方：
     * - 持續維持床上zIndex
     * - 若jumpedFromBed且再次落到床上，觸發死亡
     */
    private void updateWhileOnBedCollider() {
        BedComponent bed = currentBedPlatform.getComponent(BedComponent.class);

        boolean stillAboveBedCollider = isPlayerAboveCurrentBedCollider();

        if (!stillAboveBedCollider) {
            leaveBedFully(bed);
            return;
        }

        set("playerOnBedCollider", true);
        player.setZIndex(bed.getPlayerZIndexOnBed());

        hasJustLandedOnCurrentBedCollider();

        if (jumpedFromBed && hasJustLandedOnCurrentBedCollider()) {
            deathSystem.die(bed.getDeathReasonOnSecondLanding());
        }
    }

    private void leaveBedFully(BedComponent bed) {
        removeBedCollider();

        getPlayerComponent().setOnOneWayPlatform(false);

        player.setZIndex(bed.getNormalPlayerZIndex());

        currentBedPlatform = null;
        hasLandedOnBed = false;

        set("playerOnBedCollider", false);
    }

    /**
     * 判斷玩家是否仍在BED_ONE_WAY_PLATFORM上方，給Shift下落使用。
     */
    private boolean isPlayerAboveCurrentBedPlatform() {
        if (currentBedPlatform == null) {
            return false;
        }

        BedComponent bed = currentBedPlatform.getComponent(BedComponent.class);

        double platformLeft = currentBedPlatform.getBoundingBoxComponent().getMinXWorld();
        double platformRight = currentBedPlatform.getBoundingBoxComponent().getMinXWorld()
                + bed.getPlatformWidth();

        double playerLeft = player.getBoundingBoxComponent().getMinXWorld();
        double playerRight = player.getBoundingBoxComponent().getMaxXWorld();

        return playerRight > platformLeft + sidePadding &&
                playerLeft < platformRight - sidePadding;
    }

    private void updateDropTimer(double tpf) {
        if (dropTimer > 0) {
            dropTimer -= tpf;

            if (dropTimer < 0) {
                dropTimer = 0;
            }
        }
    }

    /**
     * 判斷玩家是否仍在床collider上方。
     */
    private boolean isPlayerAboveCurrentBedCollider() {
        if (currentBedColliders.isEmpty()) {
            return false;
        }

        double playerLeft = player.getBoundingBoxComponent().getMinXWorld();
        double playerRight = player.getBoundingBoxComponent().getMaxXWorld();

        return currentBedColliders.stream().anyMatch(collider -> {
            double colliderLeft = collider.getBoundingBoxComponent().getMinXWorld();
            double colliderRight = collider.getBoundingBoxComponent().getMaxXWorld();

            return playerRight > colliderLeft + sidePadding &&
                    playerLeft < colliderRight - sidePadding;
        });
    }

    /**
     * 判斷玩家是否剛剛從上方再次落到床collider，用於二次落床死亡判定。
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

        boolean result = previousPlayerBottom <= colliderTop + landingTolerance &&
                playerBottom >= colliderTop - landingTolerance;

        return result;
    }

    /**
     * 移除由目前床生成出的所有collider。
     */
    private void removeBedCollider() {
        currentBedColliders.forEach(Entity::removeFromWorld);
        currentBedColliders.clear();
    }

    /**
     * 重設床系統。
     *
     * 清除：
     * 1. 目前追蹤的bed platform
     * 2. 目前生成出的bed colliders
     * 3. 世界中殘留的BED_ONE_WAY_PLATFORM_COLLIDER
     * 4. jumpedFromBed / hasLandedOnBed 狀態
     * 5. playerOnBedCollider game var
     * 6. 玩家one way platform狀態與zIndex
     */
    public void reset() {
        removeBedCollider();

        getGameWorld()
                .getEntitiesByType(EntityType.BED_ONE_WAY_PLATFORM_COLLIDER)
                .forEach(Entity::removeFromWorld);

        currentBedPlatform = null;
        currentBedColliders.clear();

        dropTimer = 0;
        resetIgnoreTimer = resetIgnoreDuration;

        previousPlayerBottom = getPlayerBottom();

        hasLandedOnBed = false;
        jumpedFromBed = false;

        set("playerOnBedCollider", false);

        getPlayerComponent().setOnOneWayPlatform(false);

        player.setZIndex(normalPlayerZIndex);
    }

    private boolean isDroppingThrough() {
        return dropTimer > 0;
    }

    private double getPlayerBottom() {
        return player.getBoundingBoxComponent().getMaxYWorld();
    }

    private PlayerComponent getPlayerComponent() {
        return player.getComponent(PlayerComponent.class);
    }

    private PhysicsComponent getPhysics() {
        return player.getComponent(PhysicsComponent.class);
    }
}