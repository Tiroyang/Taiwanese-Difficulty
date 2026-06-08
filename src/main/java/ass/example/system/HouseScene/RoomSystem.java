package ass.example.system.HouseScene;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.HouseScene.RoomType;
import ass.example.system.DeathSystem;
import com.almasb.fxgl.entity.Entity;
import javafx.animation.FadeTransition;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * RoomSystem
 *
 * HouseScene 的房間判斷與房間遮罩系統。
 *
 * 功能：
 * 1. 根據玩家 X 座標判斷目前所在房間。
 * 2. 偵測玩家是否離開臥室。
 * 3. 偵測玩家是否進入走廊。
 * 4. 偵測玩家是否穿鞋進入客廳。
 * 5. 管理房間黑幕遮罩。
 * 6. 支援房間遮罩淡出與立即移除。
 */
public class RoomSystem {

    // =========================================================
    // Room Boundaries
    // =========================================================

    /**
     * 玄關範圍。
     */
    private static final double FOYER_MIN_X = 0.0;
    private static final double FOYER_MAX_X = 481.0;

    /**
     * 客廳範圍。
     */
    private static final double LIVING_ROOM_MIN_X = 481.0;
    private static final double LIVING_ROOM_MAX_X = 1671.0;

    /**
     * 走廊範圍。
     */
    private static final double HALLWAY_MIN_X = 1671.0;
    private static final double HALLWAY_MAX_X = 2092.0;

    /**
     * 臥室範圍。
     */
    private static final double BEDROOM_MIN_X = 2092.0;
    private static final double BEDROOM_MAX_X = 2788.0;

    /**
     * 廁所範圍。
     */
    private static final double TOILET_MIN_X = 2788.0;
    private static final double TOILET_MAX_X = 3200.0;


    // =========================================================
    // Room Cover Settings
    // =========================================================

    /**
     * 房間遮罩 zIndex。
     *
     * 需要蓋在場景上方，但不能蓋過 UI。
     */
    private static final int ROOM_COVER_Z_INDEX = 999;

    /**
     * 房間遮罩左右羽化寬度。
     */
    private static final double ROOM_COVER_FEATHER_SIZE = 10.0;

    /**
     * 房間揭露淡出時間。
     */
    private static final double ROOM_REVEAL_FADE_SECONDS = 0.4;


    // =========================================================
    // Dependencies
    // =========================================================

    /**
     * 目前 HouseScene 的玩家 Entity。
     *
     * RoomSystem 依賴此 Entity 判斷玩家所在房間。
     */
    private final Entity player;


    // =========================================================
    // Runtime State - Room Tracking
    // =========================================================

    /**
     * 玩家目前所在房間。
     */
    private RoomType currentRoom = RoomType.NONE;

    /**
     * 玩家上一幀所在房間。
     *
     * 用來偵測「剛離開」或「剛進入」某個房間。
     */
    private RoomType previousRoom = RoomType.NONE;


    // =========================================================
    // Runtime State - Room Covers
    // =========================================================

    /**
     * 每個房間對應的遮罩 Entity。
     */
    private final Map<RoomType, Entity> roomCovers = new HashMap<>();

    /**
     * 每個房間對應的遮罩 View。
     *
     * 淡出動畫作用在 Group 上。
     */
    private final Map<RoomType, Group> roomCoverViews = new HashMap<>();


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立房間系統。
     *
     * @param player 目前 HouseScene 的玩家 Entity
     */
    public RoomSystem(Entity player) {
        this.player = player;

        this.currentRoom = getRoomByPlayerX();
        this.previousRoom = currentRoom;
    }


    // =========================================================
    // Update
    // =========================================================

    /**
     * 每幀更新房間系統。
     *
     * 流程：
     * 1. 玩家死亡時不檢查房間規則。
     * 2. 更新上一幀與目前房間。
     * 3. 檢查離開臥室是否未折棉被。
     * 4. 檢查進入走廊是否未刷牙。
     * 5. 檢查是否穿鞋進入客廳。
     *
     * @param tpf time per frame
     */
    public void update(double tpf) {
        if (getb("playerDead")) {
            return;
        }

        updateCurrentRoom();

        checkBedroomExitRule();
        checkHallwayEnterRule();
        checkLivingRoomShoesRule();
    }

    /**
     * 更新目前房間狀態。
     */
    private void updateCurrentRoom() {
        previousRoom = currentRoom;
        currentRoom = getRoomByPlayerX();
    }


    // =========================================================
    // Room Detection
    // =========================================================

    /**
     * 根據玩家中心 X 座標判斷所在房間。
     *
     * @return 玩家目前所在房間
     */
    private RoomType getRoomByPlayerX() {
        if (player == null) {
            return RoomType.NONE;
        }

        double playerCenterX = player
                .getBoundingBoxComponent()
                .getCenterWorld()
                .getX();

        if (isInRange(playerCenterX, TOILET_MIN_X, TOILET_MAX_X)) {
            return RoomType.TOILET;
        }

        if (isInRange(playerCenterX, BEDROOM_MIN_X, BEDROOM_MAX_X)) {
            return RoomType.BEDROOM;
        }

        if (isInRange(playerCenterX, HALLWAY_MIN_X, HALLWAY_MAX_X)) {
            return RoomType.HALLWAY;
        }

        if (isInRangeInclusive(playerCenterX, LIVING_ROOM_MIN_X, LIVING_ROOM_MAX_X)) {
            return RoomType.LIVING_ROOM;
        }

        if (isInRangeInclusive(playerCenterX, FOYER_MIN_X, FOYER_MAX_X)) {
            return RoomType.FOYER;
        }

        return RoomType.NONE;
    }

    /**
     * 判斷數值是否在 [min, max) 範圍內。
     */
    private boolean isInRange(
            double value,
            double min,
            double max
    ) {
        return value >= min && value < max;
    }

    /**
     * 判斷數值是否在 [min, max] 範圍內。
     */
    private boolean isInRangeInclusive(
            double value,
            double min,
            double max
    ) {
        return value >= min && value <= max;
    }

    /**
     * 判斷玩家是否站在地面上。
     *
     * 穿鞋進客廳死亡規則需要確認玩家在地上，
     * 避免玩家從空中短暫經過客廳範圍就觸發。
     *
     * @return true 表示玩家在地上
     */
    private boolean isPlayerOnGround() {
        if (player == null || !player.hasComponent(PlayerComponent.class)) {
            return false;
        }

        return player
                .getComponent(PlayerComponent.class)
                .isOnGround();
    }


    // =========================================================
    // Room Rule Checks
    // =========================================================

    /**
     * 檢查離開臥室規則。
     *
     * 若玩家剛離開臥室且尚未折棉被，觸發死亡。
     */
    private void checkBedroomExitRule() {
        boolean justLeftBedroom =
                previousRoom == RoomType.BEDROOM &&
                        currentRoom != RoomType.BEDROOM;

        if (!justLeftBedroom) {
            return;
        }

        if (!getb("quiltFolded")) {
            triggerDeath(DeathReason.LEFT_BEDROOM_WITHOUT_FOLDING_QUILT);
        }
    }

    /**
     * 檢查進入走廊規則。
     *
     * 若玩家剛進入走廊且尚未刷牙，觸發死亡。
     */
    private void checkHallwayEnterRule() {
        boolean justEnteredHallway =
                previousRoom != RoomType.HALLWAY &&
                        currentRoom == RoomType.HALLWAY;

        if (!justEnteredHallway) {
            return;
        }

        if (!getb("teethBrushed")) {
            triggerDeath(DeathReason.LEFT_WITHOUT_BRUSHING_TEETH);
        }
    }

    /**
     * 檢查客廳穿鞋規則。
     *
     * 若玩家在客廳、穿著鞋，且站在地上，觸發死亡。
     */
    private void checkLivingRoomShoesRule() {
        boolean inLivingRoom = currentRoom == RoomType.LIVING_ROOM;

        if (!inLivingRoom) {
            return;
        }

        boolean shoesWorn = getb("shoesWorn");

        if (shoesWorn && isPlayerOnGround()) {
            triggerDeath(DeathReason.ENTER_LIVING_ROOM_WITH_SHOES);
        }
    }

    /**
     * 觸發指定死亡原因。
     *
     * @param reason 死亡原因
     */
    private void triggerDeath(DeathReason reason) {
        DeathSystem.getInstance().die(reason);
    }


    // =========================================================
    // Room Cover - Public API
    // =========================================================

    /**
     * 新增指定房間遮罩。
     *
     * 若該房間已經有遮罩，則不重複建立。
     *
     * @param roomType 房間類型
     * @param x 遮罩世界 X
     * @param y 遮罩世界 Y
     * @param width 遮罩寬度
     * @param height 遮罩高度
     */
    public void addRoomCover(
            RoomType roomType,
            double x,
            double y,
            double width,
            double height
    ) {
        if (roomType == null || roomType == RoomType.NONE) {
            return;
        }

        if (roomCovers.containsKey(roomType)) {
            return;
        }

        Group coverView = createFeatheredCover(
                width,
                height,
                ROOM_COVER_FEATHER_SIZE
        );

        Entity cover = entityBuilder()
                .at(x, y)
                .view(coverView)
                .zIndex(ROOM_COVER_Z_INDEX)
                .buildAndAttach();

        roomCovers.put(roomType, cover);
        roomCoverViews.put(roomType, coverView);
    }

    /**
     * 揭露指定房間。
     *
     * 會：
     * 1. 將 room_{ROOM}_revealed 設為 true。
     * 2. 播放遮罩淡出動畫。
     * 3. 動畫結束後移除遮罩 Entity。
     *
     * @param roomType 要揭露的房間
     */
    public void revealRoom(RoomType roomType) {
        if (roomType == null || roomType == RoomType.NONE) {
            return;
        }

        String key = getRoomRevealedKey(roomType);

        if (getb(key)) {
            return;
        }

        set(key, true);

        fadeOutAndRemoveRoomCover(roomType);
    }

    /**
     * 不播放動畫，立即揭露指定房間。
     *
     * 用於讀檔套用狀態。
     *
     * @param roomType 要揭露的房間
     */
    public void revealRoomNoAnimation(RoomType roomType) {
        removeRoomCover(roomType);
    }

    /**
     * 判斷指定房間是否已揭露。
     *
     * @param roomType 房間類型
     * @return true 表示該房間沒有遮罩
     */
    public boolean isRoomRevealed(RoomType roomType) {
        return !roomCovers.containsKey(roomType);
    }

    /**
     * 清除所有房間遮罩。
     *
     * 用於 HouseScene cleanup。
     */
    public void clearRoomCovers() {
        roomCovers.values().forEach(this::removeEntitySafely);
        roomCovers.clear();
        roomCoverViews.clear();
    }


    // =========================================================
    // Room Cover - Internal Operations
    // =========================================================

    /**
     * 淡出並移除指定房間遮罩。
     *
     * @param roomType 房間類型
     */
    private void fadeOutAndRemoveRoomCover(RoomType roomType) {
        Entity cover = roomCovers.get(roomType);
        Group coverView = roomCoverViews.get(roomType);

        if (cover == null || coverView == null) {
            roomCovers.remove(roomType);
            roomCoverViews.remove(roomType);
            return;
        }

        /*
         * 先移除紀錄，避免淡出動畫還沒結束時，玩家再次開門或讀檔 applySavedState 又呼叫 revealRoom。
         */
        roomCovers.remove(roomType);
        roomCoverViews.remove(roomType);

        FadeTransition fade = new FadeTransition(
                Duration.seconds(ROOM_REVEAL_FADE_SECONDS),
                coverView
        );

        fade.setFromValue(coverView.getOpacity());
        fade.setToValue(0.0);

        fade.setOnFinished(event -> removeEntitySafely(cover));

        fade.play();
    }

    /**
     * 立即移除指定房間遮罩。
     *
     * @param roomType 房間類型
     */
    private void removeRoomCover(RoomType roomType) {
        Entity cover = roomCovers.remove(roomType);

        if (cover != null) {
            removeEntitySafely(cover);
        }

        roomCoverViews.remove(roomType);
    }

    /**
     * 建立左右羽化的黑色遮罩。
     *
     * @param width 遮罩寬度
     * @param height 遮罩高度
     * @param featherSize 左右羽化寬度
     * @return 遮罩 Group
     */
    private Group createFeatheredCover(
            double width,
            double height,
            double featherSize
    ) {
        Group group = new Group();

        Rectangle center = createCenterCoverRect(
                width,
                height,
                featherSize
        );

        Rectangle leftFeather = createLeftFeatherRect(
                height,
                featherSize
        );

        Rectangle rightFeather = createRightFeatherRect(
                width,
                height,
                featherSize
        );

        group.getChildren().addAll(
                center,
                leftFeather,
                rightFeather
        );

        return group;
    }

    /**
     * 建立遮罩中央實心區。
     */
    private Rectangle createCenterCoverRect(
            double width,
            double height,
            double featherSize
    ) {
        Rectangle center = new Rectangle(
                featherSize,
                0,
                width - featherSize * 2,
                height
        );

        center.setFill(Color.BLACK);

        return center;
    }

    /**
     * 建立左側羽化區。
     */
    private Rectangle createLeftFeatherRect(
            double height,
            double featherSize
    ) {
        Rectangle left = new Rectangle(
                0,
                0,
                featherSize,
                height
        );

        left.setFill(new LinearGradient(
                0,
                0,
                1,
                0,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(1, Color.BLACK)
        ));

        return left;
    }

    /**
     * 建立右側羽化區。
     */
    private Rectangle createRightFeatherRect(
            double width,
            double height,
            double featherSize
    ) {
        Rectangle right = new Rectangle(
                width - featherSize,
                0,
                featherSize,
                height
        );

        right.setFill(new LinearGradient(
                0,
                0,
                1,
                0,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.BLACK),
                new Stop(1, Color.TRANSPARENT)
        ));

        return right;
    }


    // =========================================================
    // Helpers
    // =========================================================

    /**
     * 取得房間揭露狀態的 game var key。
     *
     * @param roomType 房間類型
     * @return game var key
     */
    private String getRoomRevealedKey(RoomType roomType) {
        return "room_" + roomType.name() + "_revealed";
    }

    /**
     * 安全移除 Entity。
     *
     * @param entity 要移除的 Entity
     */
    private void removeEntitySafely(Entity entity) {
        if (entity != null && entity.isActive()) {
            entity.removeFromWorld();
        }
    }


    // =========================================================
    // Getters
    // =========================================================

    /**
     * 取得目前玩家所在房間。
     *
     * @return 目前房間
     */
    public RoomType getCurrentRoom() {
        return currentRoom;
    }

    /**
     * 取得上一幀玩家所在房間。
     *
     * @return 上一幀房間
     */
    public RoomType getPreviousRoom() {
        return previousRoom;
    }
}