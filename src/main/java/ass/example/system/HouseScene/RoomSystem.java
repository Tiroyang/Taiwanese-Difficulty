package ass.example.system.HouseScene;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.HouseScene.RoomType;
import ass.example.system.DeathSystem;
import com.almasb.fxgl.entity.Entity;
import javafx.animation.FadeTransition;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * 追蹤目前所在房間
 */
public class RoomSystem {

    private final Entity player;
    private final DeathSystem deathSystem = DeathSystem.getInstance();

    private RoomType currentRoom = RoomType.NONE;
    private RoomType previousRoom = RoomType.NONE;

    private final Map<RoomType, Entity> roomCovers = new HashMap<>();
    private final Map<RoomType, Group> roomCoverViews = new HashMap<>();

    public RoomSystem(Entity player) {
        this.player = player;
        this.currentRoom = getRoomByPlayerX();
        this.previousRoom = currentRoom;
    }

    public void update(double tpf) {
        if (getb("playerDead")) {
            return;
        }

        previousRoom = currentRoom;
        currentRoom = getRoomByPlayerX();


        checkBedroomExit();
        checkInLivingRoom();
        checkEnterHallway();
    }

    private RoomType getRoomByPlayerX() {
        double playerCenterX = player.getBoundingBoxComponent()
                .getCenterWorld()
                .getX();

        if (playerCenterX >= 2788 && playerCenterX < 3200) {
            return RoomType.TOILET;
        }

        if (playerCenterX >= 2092 && playerCenterX < 2788) {
            return RoomType.BEDROOM;
        }

        if (playerCenterX >= 1671 && playerCenterX < 2092) {
            return RoomType.HALLWAY;
        }

        if (playerCenterX >= 481 && playerCenterX <= 1671) {
            return RoomType.LIVING_ROOM;
        }

        if (playerCenterX >= 0 && playerCenterX <= 481) {
            return RoomType.FOYER;
        }

        return RoomType.NONE;
    }

    private boolean isPlayerOnGround() {
        if (player == null || !player.hasComponent(PlayerComponent.class)) {
            return false;
        }

        return player.getComponent(PlayerComponent.class).isOnGround();
    }

    private void checkBedroomExit() {
        boolean justLeftBedroom =
                previousRoom == RoomType.BEDROOM &&
                        currentRoom != RoomType.BEDROOM;

        if (!justLeftBedroom) {
            return;
        }

        if (!getb("quiltFolded")) {
            triggerDeathBecauseQuiltNotFolded();
        }
    }

    private void checkEnterHallway() {
        boolean justEnteredLivingRoom =
                previousRoom != RoomType.HALLWAY &&
                        currentRoom == RoomType.HALLWAY;

        if (!justEnteredLivingRoom) {
            return;
        }

        if (!getb("teethBrushed")) {
            triggerDeathBecauseDidNotBrushTeeth();
        }
    }

    private void checkInLivingRoom() {
        boolean inLivingRoom = currentRoom == RoomType.LIVING_ROOM;

        if (!inLivingRoom) {
            return;
        }

        if (getb("shoesWorn") && isPlayerOnGround()) {
            triggerDeathBecauseWanderInRoomWithShoes();
        }
    }

    private void triggerDeathBecauseQuiltNotFolded() {
        deathSystem.die(DeathReason.LEFT_BEDROOM_WITHOUT_FOLDING_QUILT);
    }

    private void triggerDeathBecauseDidNotBrushTeeth() {
        deathSystem.die(DeathReason.LEFT_WITHOUT_BRUSHING_TEETH);
    }

    private void triggerDeathBecauseWanderInRoomWithShoes() {
        deathSystem.die(DeathReason.ENTER_LIVING_ROOM_WITH_SHOES);
    }

    public RoomType getCurrentRoom() {
        return currentRoom;
    }

    // Room Cover / 房間遮罩
    private Group createFeatheredCover(double width, double height, double featherSize) {
        Group group = new Group();

        // 中央實心區
        Rectangle center = new Rectangle(
                featherSize,
                0,
                width - featherSize * 2,
                height
        );
        center.setFill(Color.BLACK);

        // 左側羽化
        Rectangle left = new Rectangle(
                0,
                0,
                featherSize,
                height
        );
        left.setFill(new javafx.scene.paint.LinearGradient(
                0, 0, 1, 0,
                true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.TRANSPARENT),
                new javafx.scene.paint.Stop(1, Color.BLACK)
        ));

        // 右側羽化
        Rectangle right = new Rectangle(
                width - featherSize,
                0,
                featherSize,
                height
        );
        right.setFill(new javafx.scene.paint.LinearGradient(
                0, 0, 1, 0,
                true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.BLACK),
                new javafx.scene.paint.Stop(1, Color.TRANSPARENT)
        ));

        group.getChildren().addAll(center, left, right);

        return group;
    }

    public void addRoomCover(
            RoomType roomType,
            double x,
            double y,
            double width,
            double height
    ) {
        if (roomCovers.containsKey(roomType)) {
            return;
        }

        Group coverView = createFeatheredCover(width, height, 10);

        Entity cover = entityBuilder()
                .at(x, y)
                .view(coverView)
                .zIndex(999)
                .buildAndAttach();

        roomCovers.put(roomType, cover);
        roomCoverViews.put(roomType, coverView);
    }

    public void revealRoom(RoomType roomType) {
        if (roomType == null || roomType == RoomType.NONE) {
            return;
        }

        String key = "room_" + roomType.name() + "_revealed";

        if (getb(key)) {
            return;
        }

        set(key, true);

        Entity cover = roomCovers.get(roomType);
        Group coverView = roomCoverViews.get(roomType);

        if (cover == null || coverView == null) {
            roomCovers.remove(roomType);
            roomCoverViews.remove(roomType);
            return;
        }

        /*
         * 先移除紀錄，避免淡出動畫還沒結束時，
         * 玩家再次開門或讀檔 applySavedState 又呼叫 revealRoom。
         */
        roomCovers.remove(roomType);
        roomCoverViews.remove(roomType);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.4), coverView);
        fade.setFromValue(coverView.getOpacity());
        fade.setToValue(0.0);

        fade.setOnFinished(e -> {
            if (cover.isActive()) {
                cover.removeFromWorld();
            }
        });

        fade.play();
    }

    public void revealRoomNoAnimation(RoomType roomType) {
        Entity cover = roomCovers.get(roomType);

        if (cover != null) {
            cover.removeFromWorld();
            roomCovers.remove(roomType);
            roomCoverViews.remove(roomType);
        }
    }

    public boolean isRoomRevealed(RoomType roomType) {
        return !roomCovers.containsKey(roomType);
    }

    public void clearRoomCovers() {
        roomCovers.values().forEach(Entity::removeFromWorld);
        roomCovers.clear();
        roomCoverViews.clear();
    }
}