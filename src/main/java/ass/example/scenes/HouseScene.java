package ass.example.scenes;

import ass.example.components.HouseScene.DoorComponent;
import ass.example.core.DeathReasons;
import ass.example.core.HouseScene.RoomType;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import ass.example.system.HouseScene.BedSystem;
import ass.example.system.OneWayPlatformSystem;
import ass.example.system.HouseScene.RoomSystem;
import ass.example.system.InteractionSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
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
        spawnCollisions();

        player = spawn("player", config.getPlayerStartX(), config.getPlayerStartY());

        initSystems();

        spawnRoomCovers();
        spawnProps();
        spawnAnimatedProps();

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
        spawn("wall", new SpawnData(476, 664)
                .put("width", 2724.0)
                .put("height", 70.0));
        spawn("wall", new SpawnData(0, 694)
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

        // props
        spawn("wall", new SpawnData(3034, 555)
                .put("width", 140.0)
                .put("height", 111.0));

        //DEATH_WALL(碰撞即死)
        // ceiling
        spawn("death_wall", new SpawnData(0, 0)
                .put("width", 3200.0)
                .put("height", 225.0)
                .put("deathReason", DeathReasons.HIT_CEILING));

        // props
        spawn("death_wall", new SpawnData(3037, 246)
                .put("width", 14.0)
                .put("height", 14.0)
                .put("deathReason", DeathReasons.HIT_SHOWER_CURTAIN_ROD));
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
                .put("quiltVisual", quiltVisual)
                .put("defaultTexture", "Scene1/props/Quilt.png")
                .put("foldedTexture", "Scene1/props/Quilt_folded.png")
                .put("width", 120.0)
                .put("height", 80.0)
                .put("interactRange", 150.0)
                .put("promptOnEntity", true)
                .put("promptOffsetY", 40.0));

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
                .put("collider2OffsetY", -61.0)
                .put("collider2Width", 6.0)
                .put("collider2Height", 71.0)

                .put("playerZIndexOnBed", -2)
                .put("normalPlayerZIndex", 0)
                .put("deathReason", DeathReasons.JUMPING_ON_BED));

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