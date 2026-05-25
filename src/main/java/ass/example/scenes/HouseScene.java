package ass.example.scenes;

import ass.example.components.HouseScene.DoorComponent;
import ass.example.components.HouseScene.QuiltComponent;
import ass.example.components.HouseScene.WaterComponent;
import ass.example.components.LoadSaveComponent;
import ass.example.core.DeathReason;
import ass.example.core.HouseScene.RoomType;
import ass.example.system.*;
import ass.example.system.HouseScene.BedSystem;
import ass.example.system.HouseScene.RoomSystem;
import ass.example.ui.QuestHUD;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import static com.almasb.fxgl.dsl.FXGL.*;
import static java.lang.Math.clamp;

/**
 * 負責HouseScene的生成與更新。
 *
 * 1. 呼叫生成Entities
 * 2. 設定攝影機卷軸範圍
 * 3. 持續更新各系統
 */
public class HouseScene {
    // 玩家 Entity。
    private Entity player;

    // 目前場景設定(地圖寬高、玩家起始位置)
    private final SceneConfig config;

    // 死亡系統
    private final DeathSystem deathSystem;

    // 互動系統
    private InteractionSystem interactionSystem;

    // 音效系統
    private final AudioSystem audioSystem;

    // 房間判斷系統
    private RoomSystem roomSystem;

    // 單向平台系統，床獨立出一個系統
    private OneWayPlatformSystem oneWayPlatformSystem;
    private BedSystem bedSystem;

    // 任務系統
    private QuestHUD questHUD;

    // Constructor
    public HouseScene(
            SceneConfig config,
            DeathSystem deathSystem,
            AudioSystem audioSystem
    ) {
        this.config = config;
        this.deathSystem = deathSystem;
        this.audioSystem = audioSystem;
    }

    public void cleanup() {
        if (interactionSystem != null) {
            interactionSystem.dispose();
            interactionSystem = null;
        }

        if (questHUD != null) {
            removeUINode(questHUD);
            questHUD = null;
        }

        getGameScene().getViewport().unbind();
        getGameScene().getViewport().setLazy(false);
    }

    /**
     * 載入HouseScene。
     *
     * 1. 生成背景與碰撞
     * 2. 生成玩家
     * 3. 建立系統
     * 4. 生成物件
     * 5. 設定攝影機
     */
    public Entity load() {
        spawnTestObjects();
        spawnBackground();

        player = spawn("player", config.getPlayerStartX(), config.getPlayerStartY());

        spawnCollisions();

        initSystems();

        spawnRoomCovers();
        spawnProps();
        spawnAnimatedProps();

        questHUD = new QuestHUD();
        addUINode(questHUD, 0, 0);

        setupCamera();

        return player;
    }

    private void initSystems() {
        interactionSystem = new InteractionSystem(player);
        roomSystem = new RoomSystem(player, deathSystem);
        oneWayPlatformSystem = new OneWayPlatformSystem(player);
        bedSystem = new BedSystem(player, deathSystem);
    }

    /**
     * 測試用物件
     */
    private void spawnTestObjects() {
        /*
        spawn("one_way_platform", new SpawnData(1800, 520)
                .put("id", "platform_01")
                .put("width", 300.0)
                .put("height", 20.0)
                .put("isBed", false)
                .put("playerZIndexOnTop", 50));
         */
    }

    /**
     * 生成家中場景的背景圖層
     * 只負責顯示
     */
    private void spawnBackground() {
        spawn("window_view", new SpawnData(0, 0)
                .put("texture", "/Scene1/map/window_view.png")
                .put("parallaxFactor", 0.01));
        spawn("house_background", 0, 0);
        spawn("house_floor", 0, 0);
        spawn("house_ceiling", 0, 0);
        spawn("house_foreground", 0, 0);
    }

    /**
     * 生成場景實體碰撞物
     */
    private void spawnCollisions() {
        // WALL
        // floor
        spawn("floor", new SpawnData(476, 664)
                .put("width", 2724.0)
                .put("height", 70.0));
        spawn("floor", new SpawnData(0, 694)
                .put("width", 3200.0)
                .put("height", 70.0));

        // walls
        spawn("wall", new SpawnData(-25, 0)
                .put("width", 50.0)
                .put("height", 720.0));
        spawn("wall", new SpawnData(2079, 0)
                .put("width", 27.0)
                .put("height", 324.0));
        spawn("wall", new SpawnData(2776, 0)
                .put("width", 27.0)
                .put("height", 324.0));
        spawn("wall", new SpawnData(3169, 0)
                .put("width", 50.0)
                .put("height", 720.0));

        spawnBathtubTest(3034, 555, 140, 111, player, deathSystem);

        //DEATH_ZONE(碰撞即死)
        // ceiling
        spawn("death_wall", new SpawnData(0, 0)
                .put("width", 3200.0)
                .put("height", 225.0)
                .put("deathReason", DeathReason.HIT_CEILING));

        // door frames
        spawn("death_wall", new SpawnData(2769, 311)
                .put("width", 38.0)
                .put("height", 23.0)
                .put("deathReason", DeathReason.HIT_DOORFRAME));
        spawn("death_wall", new SpawnData(2073, 311)
                .put("width", 38.0)
                .put("height", 23.0)
                .put("deathReason", DeathReason.HIT_DOORFRAME));

        // props
        spawn("death_wall", new SpawnData(3037, 246)
                .put("width", 14.0)
                .put("height", 14.0)
                .put("deathReason", DeathReason.HIT_SHOWER_CURTAIN_ROD));
    }

    private void spawnBathtubTest(
            double x,
            double y,
            double width,
            double height,
            Entity player,
            DeathSystem deathSystem
    ) {
        double thickness = 20;

        /*
         * 左外牆。
         */
        spawn("wall", new SpawnData(x, y)
                .put("width", thickness)
                .put("height", height));

        /*
         * 右外牆。
         */
        spawn("wall", new SpawnData(x + width - thickness, y)
                .put("width", thickness)
                .put("height", height));

        /*
         * 底部外牆。
         */
        spawn("wall", new SpawnData(x, y + height - thickness)
                .put("width", width)
                .put("height", thickness));

        /*
         * 多段 slope_wall 組成平滑凹面。
         */
        spawnSmoothBathtubCurve(x, y, width, height);
        spawnBathtubSensor(x, y, width, height, player, deathSystem);
    }

    private void spawnSmoothBathtubCurve(
            double x,
            double y,
            double width,
            double height
    ) {
        /*
         * U 型凹面參數。
         * x, y 是浴缸外框左上角。
         */
        double centerX = x + width / 2.0;

        /*
         * 凹面左右範圍。
         * 數值越大，凹面越寬。
         */
        double radiusX = 62;

        /*
         * 凹面起點與最深點。
         */
        double topY = y + 24;
        double bottomY = y + 74;
        double depth = bottomY - topY;

        /*
         * 每段斜面的厚度。
         */
        double thickness = 9;

        /*
         * 段數越多越平滑。
         * 建議 10～16 之間。
         */
        int pieces = 12;

        /*
         * 使用拋物線：
         * t = -1 時在左上
         * t = 0 時在最底
         * t = 1 時在右上
         *
         * curveY = topY + (1 - t^2) * depth
         */
        for (int i = 0; i < pieces; i++) {
            double t1 = -1.0 + 2.0 * i / pieces;
            double t2 = -1.0 + 2.0 * (i + 1) / pieces;

            double x1 = centerX + t1 * radiusX;
            double y1 = topY + (1.0 - t1 * t1) * depth;

            double x2 = centerX + t2 * radiusX;
            double y2 = topY + (1.0 - t2 * t2) * depth;

            spawnSlopeBetweenPoints(x1, y1, x2, y2, thickness);
        }
    }

    private void spawnSlopeBetweenPoints(
            double x1,
            double y1,
            double x2,
            double y2,
            double thickness
    ) {
        double dx = x2 - x1;
        double dy = y2 - y1;

        double length = Math.sqrt(dx * dx + dy * dy);

        /*
         * 避免太短的斜面造成奇怪碰撞。
         */
        if (length < 4) {
            return;
        }

        double angle = Math.toDegrees(Math.atan2(dy, dx));

        /*
         * 以線段中心作為斜面的中心點。
         * 這樣比直接用左上角準確，較不會整體偏右。
         */
        double centerX = (x1 + x2) / 2.0;
        double centerY = (y1 + y2) / 2.0;

        double spawnX = centerX - length / 2.0;
        double spawnY = centerY - thickness / 2.0;

        spawn("slope_wall", new SpawnData(spawnX, spawnY)
                .put("width", length)
                .put("height", thickness)
                .put("angle", angle));
    }

    private void spawnBathtubSensor(
            double x,
            double y,
            double width,
            double height,
            Entity player,
            DeathSystem deathSystem
    ) {
        double sensorWidth = width * 0.55;
        double sensorHeight = 18;

        /*
         * 放在浴缸 U 型底部附近。
         * 如果太容易觸發，可以把 sensorY 往下移。
         * 如果很難觸發，可以把 sensorHeight 加高。
         */
        double sensorX = x + (width - sensorWidth) / 2.0;
        double sensorY = y + height * 0.48;

        spawn("bathtub_sensor", new SpawnData(sensorX, sensorY)
                .put("width", sensorWidth)
                .put("height", sensorHeight)
                .put("player", player)
                .put("deathSystem", deathSystem)
                .put("deathReason", DeathReason.JUMPED_IN_BATHTUB));
    }

    /**
     * 生成道具
     */
    private void spawnProps() {
        // INTERACTABLES
        // doors
        Entity door1 = spawn("door", new SpawnData(2054, 290)
                .put("id", "Door1")
                .put("closedTexture", "/Scene1/props/Door_1_closed.png")
                .put("openTexture", "/Scene1/props/Door_1_opened.png")
                .put("colliderOffsetX", 27.0)
                .put("colliderOffsetY", 32.0)
                .put("colliderWidth", 21.0)
                .put("colliderHeight", 378.0)
                .put("interactRange", 120.0)
                .put("promptOnEntity", false)
                .put("promptOffsetY", 35.0)
                .put("audioSystem", audioSystem));
        door1.getComponent(DoorComponent.class).setOnOpen(() -> {
            roomSystem.revealRoom(RoomType.LIVING_ROOM);
        });

        Entity door2 = spawn("door", new SpawnData(2755, 290)
                .put("id", "Door2")
                .put("closedTexture", "/Scene1/props/Door_2_closed.png")
                .put("openTexture", "/Scene1/props/Door_2_opened.png")
                .put("colliderOffsetX", 24.0)
                .put("colliderOffsetY", 32.0)
                .put("colliderWidth", 21.0)
                .put("colliderHeight", 378.0)
                .put("interactRange", 120.0)
                .put("promptOnEntity", false)
                .put("promptOffsetY", 35.0)
                .put("audioSystem", audioSystem));
        door2.getComponent(DoorComponent.class).setOnOpen(() -> {
            roomSystem.revealRoom(RoomType.TOILET);
        });

        // quilt
        Entity quiltVisual = spawn("quilt", new SpawnData(0, 0));
        spawn("quilt_trigger", new SpawnData(2460, 500)
                .put("visual", quiltVisual)
                .put("defaultTexture", "Scene1/props/Quilt.png")
                .put("foldedTexture", "Scene1/props/Quilt_folded.png")
                .put("width", 120.0)
                .put("height", 80.0)
                .put("interactRange", 150.0)
                .put("promptOnEntity", true)
                .put("promptOffsetY", 40.0)
                .put("audioSystem", audioSystem));

        // bed
        spawn("bed", 0, 0);
        spawn("bed_one_way_platform", new SpawnData(2445, 581)
                .put("id", "Bed1")
                .put("width", 223.0 - player.getBoundingBoxComponent().getWidth())
                .put("height", 10.0)

                // 第一組 bed collider
                .put("collider1OffsetX", 0.0)
                .put("collider1OffsetY", 0.0)
                .put("collider1Width", 321.0)
                .put("collider1Height", 10.0)

                // 第二組 bed collider
                .put("collider2OffsetX", 316.0)
                .put("collider2OffsetY", -244.0)
                .put("collider2Width", 6.0)
                .put("collider2Width", 6.0)
                .put("collider2Height", 254.0)

                .put("playerZIndexOnBed", -3)
                .put("normalPlayerZIndex", 0)
                .put("deathReason", DeathReason.JUMPING_ON_BED));

        // water
        Entity waterVisual = spawn("water", new SpawnData(0, 0));
        spawn("water_trigger", new SpawnData(2672, 528)
                .put("visual", waterVisual)
                .put("player", player)
                .put("deathSystem", deathSystem)
                .put("texture", "Scene1/props/Water.png")
                .put("width", 16.0)
                .put("height", 28.0)
                .put("interactRange", 130.0)
                .put("promptOnEntity", true)
                .put("promptOffsetY", 40.0)
                .put("audioSystem", audioSystem));

        // toothbrush
        spawn("toothbrush_trigger", new SpawnData(2928, 513)
                .put("width", 80.0)
                .put("height", 120.0)
                .put("interactRange", 180.0)
                .put("promptOnEntity", true)
                .put("promptOffsetY", 50.0)
                .put("audioSystem", audioSystem));

        // VISUALS
        spawn("cabinet", 0, 0);
    }

    /**
     * 生成房間遮罩
     * 打開對應的門後RoomSystem.revealRoom()會讓黑幕淡出
     */
    private void spawnRoomCovers() {
        roomSystem.addRoomCover(
                RoomType.LIVING_ROOM,
                -5,
                0,
                2093,
                733
        );

        roomSystem.addRoomCover(
                RoomType.TOILET,
                2793,
                0,
                422,
                733
        );
    }

    /**
     * 動畫物件。
     * 之後可以放：
     * 1. 吊扇動畫
     * 3. 窗簾晃動
     * 2. 背景角色動畫
     * ...
     */
    private void spawnAnimatedProps() {
        // 循環動畫物件
    }

    public void applySavedState() {
        applyRoomCoverState();
        applyPropsState();
    }

    private void applyRoomCoverState() {
        if (getb("room_LIVING_ROOM_revealed")) {
            roomSystem.revealRoomNoAnimation(RoomType.LIVING_ROOM);
        }

        if (getb("room_TOILET_revealed")) {
            roomSystem.revealRoomNoAnimation(RoomType.TOILET);
        }
    }

    private <T extends Component & LoadSaveComponent> void applyStateToComponents(Class<T> componentClass) {
        getGameWorld()
                .getEntitiesByComponent(componentClass)
                .forEach(entity -> {
                    entity.getComponent(componentClass).applySavedState();
                });
    }

    private void applyPropsState() {
        applyStateToComponents(DoorComponent.class);
        applyStateToComponents(QuiltComponent.class);
        applyStateToComponents(WaterComponent.class);

        if (bedSystem != null) {
            bedSystem.applySavedState();
        }
    }

    /**
     * 設定卷軸攝影機。
     *
     * bounds：限制攝影機不超出地圖範圍。
     * setX：設定開場畫面。
     * bindToEntity：攝影機跟隨玩家。
     * setLazy：攝影機平滑。
     */
    private void setupCamera() {
        getGameScene().getViewport().setBounds(0, 0, config.getMapWidth(), config.getMapHeight());
        getGameScene().getViewport().setX(clamp(config.getPlayerStartX() - 1280 / 2.0, 0, config.getMapWidth() - 1280));
        getGameScene().getViewport().bindToEntity(player, 640, 360);
        getGameScene().getViewport().setLazy(true);
    }

    /**
     * 時刻更新場景系統，從SceneManager呼叫。
     */
    public void onUpdate(double tpf) {
        if (interactionSystem != null) {
            interactionSystem.update(tpf);
        }

        if (roomSystem != null) {
            roomSystem.update(tpf);
        }

        if (oneWayPlatformSystem != null) {
            oneWayPlatformSystem.update(tpf);
        }

        if (bedSystem != null) {
            bedSystem.update(tpf);
        }

        if (questHUD != null) {
            questHUD.update();
        }
    }

    /**
     * 嘗試與附近物件互動，從SceneManager呼叫。
     */
    public void tryInteract() {
        if (interactionSystem != null) {
            interactionSystem.interact();
        }
    }

    /**
     * 玩家按下跳躍鍵。
     * 用途：通知 BedSystem / OneWayPlatformSystem：玩家是否從單向平台跳起\
     * 從SceneManager呼叫。
     */
    public void onPlayerJumpPressed() {
        if (bedSystem != null) {
            bedSystem.onPlayerJumpPressed();
        }

        if (oneWayPlatformSystem != null) {
            oneWayPlatformSystem.onPlayerJumpPressed();
        }
    }

    /**
     * 玩家按下墜落鍵。
     * 用途：通知 BedSystem / OneWayPlatformSystem：玩家是否從單向平台墜落
     * 從SceneManager呼叫。
     */
    public void dropThroughOneWayPlatform() {
        if (oneWayPlatformSystem != null) {
            oneWayPlatformSystem.dropThrough();
        }

        if (bedSystem != null) {
            bedSystem.dropThrough();
        }
    }

    /**
     * 重生或死亡時重設狀態。
     *
     * 清除單向平台
     */
    public void resetRuntimeSystems() {
        if (oneWayPlatformSystem != null) {
            oneWayPlatformSystem.reset();
        }

        if (bedSystem != null) {
            bedSystem.reset();
        }
    }
}