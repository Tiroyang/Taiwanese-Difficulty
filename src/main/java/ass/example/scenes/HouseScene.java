package ass.example.scenes;

import ass.example.components.LoadSaveComponent;
import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.HouseScene.RoomType;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import ass.example.system.InteractionSystem;
import ass.example.system.MusicSystem;
import ass.example.system.OneWayPlatformSystem;
import ass.example.system.HouseScene.BedSystem;
import ass.example.system.HouseScene.RoomSystem;
import ass.example.ui.QuestHUD;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.*;
import static java.lang.Math.clamp;

/**
 * HouseScene
 *
 * 負責家中場景的建立、更新、互動、存檔還原與場景狀態管理。
 *
 * 主要職責：
 * 1. 生成 HouseScene 所需的背景、地板、牆壁、死亡區與互動物件。
 * 2. 生成玩家並初始化與玩家相關的系統。
 * 3. 管理房間黑幕、門、被子、水龍頭、鞋櫃、媽媽等互動物件。
 * 4. 管理 HouseScene 專用系統，例如 RoomSystem、BedSystem、OneWayPlatformSystem。
 * 5. 設定橫向卷軸攝影機。
 * 6. 處理讀檔後的場景狀態還原。
 * 7. 處理起床開場動畫。
 *
 * 注意：
 * HouseScene 本身不是 FXGL Scene，
 * 而是由 SceneManager 持有並呼叫 load()、onUpdate()、tryInteract() 等方法。
 */
public class HouseScene {

    // =========================================================
    // Screen Constants
    // =========================================================

    /**
     * 遊戲視窗寬度。
     *
     * 目前專案使用 1280 x 720。
     */
    private static final double SCREEN_WIDTH = 1280.0;

    /**
     * 遊戲視窗高度。
     */
    private static final double SCREEN_HEIGHT = 720.0;

    /**
     * 攝影機跟隨玩家時，玩家在畫面中的水平位置。
     *
     * 640 表示玩家位於畫面中央。
     */
    private static final double CAMERA_FOLLOW_X = 640.0;

    /**
     * 攝影機跟隨玩家時，玩家在畫面中的垂直位置。
     */
    private static final double CAMERA_FOLLOW_Y = 360.0;


    // =========================================================
    // Music Paths
    // =========================================================

    /**
     * 起床開場時播放的早晨環境音。
     */
    private static final String BGM_MORNING_SOUND = "/assets/music/scene1/morningsound.mp3";

    /**
     * HouseScene 正常遊玩時的背景音樂。
     */
    private static final String BGM_HOUSE_SCENE = "/assets/music/scene1/Kobo Kanaeru - HELP!! (No Vocal).mp3";

    /**
     * 媽媽對話時切換用的 BGM。
     */
    private static final String BGM_MOM_DIALOGUE = "/assets/music/dialogue/MiSide OST.mp3";


    // =========================================================
    // Wake Up Intro Constants
    // =========================================================

    /**
     * 起床動畫第一段，從黑畫面淡入的時間。
     */
    private static final double WAKE_FADE_IN_DURATION = 1.5;

    /**
     * 玩家躺在床上的停頓時間。
     */
    private static final double WAKE_LIE_PAUSE_DURATION = 1.0;

    /**
     * 起床前再次淡黑的時間。
     */
    private static final double WAKE_FADE_OUT_DURATION = 0.65;

    /**
     * 黑畫面停頓時間。
     *
     * 玩家位置會在黑畫面期間瞬移到正式起始點。
     */
    private static final double WAKE_BLACK_PAUSE_DURATION = 1.0;

    /**
     * 起床後從黑畫面淡回遊戲畫面的時間。
     */
    private static final double WAKE_FADE_BACK_IN_DURATION = 0.75;


    // =========================================================
    // Bathtub Constants
    // =========================================================

    /**
     * 浴缸外框牆壁厚度。
     */
    private static final double BATHTUB_WALL_THICKNESS = 20.0;

    /**
     * 浴缸內部曲線牆厚度。
     */
    private static final double BATHTUB_CURVE_THICKNESS = 9.0;

    /**
     * 浴缸曲線切成幾段 slope_wall。
     *
     * 數值越大，曲線越平滑，但碰撞 Entity 也越多。
     */
    private static final int BATHTUB_CURVE_PIECES = 12;

    /**
     * 浴缸死亡 sensor 的寬度比例。
     */
    private static final double BATHTUB_SENSOR_WIDTH_RATIO = 0.55;

    /**
     * 浴缸死亡 sensor 高度。
     */
    private static final double BATHTUB_SENSOR_HEIGHT = 18.0;

    /**
     * 玩家掉進浴缸時的死亡速度門檻。
     */
    private static final double BATHTUB_DEATH_SPEED_THRESHOLD = 520.0;


    // =========================================================
    // Scene Dependencies
    // =========================================================

    /**
     * 場景設定。
     *
     * 保存地圖大小、玩家起始位置等資料。
     */
    private final SceneConfig config;

    /**
     * 死亡系統。
     *
     * 提供死亡觸發、死亡畫面、重生流程等功能。
     */
    private final DeathSystem deathSystem;

    /**
     * 音效系統。
     *
     * 提供互動音效、門音效、起床音效等。
     */
    private final AudioSystem audioSystem;

    /**
     * 場景管理器。
     *
     * exit_door 等物件需要透過它切換場景。
     */
    private final SceneManager sceneManager;


    // =========================================================
    // Runtime Entities
    // =========================================================

    /**
     * 玩家 Entity。
     *
     * load() 時生成。
     */
    private Entity player;


    // =========================================================
    // Runtime Systems
    // =========================================================

    /**
     * 互動系統。
     *
     * 負責偵測附近可互動物件與處理 F 鍵互動。
     */
    private InteractionSystem interactionSystem;

    /**
     * 房間系統。
     *
     * 負責：
     * 1. 房間黑幕。
     * 2. 離開房間未完成任務的死亡判定。
     * 3. 開門後 reveal room。
     */
    private RoomSystem roomSystem;

    /**
     * 一般單向平台系統。
     *
     * 床因為有特殊跳床死亡邏輯，
     * 所以獨立交給 BedSystem 處理。
     */
    private OneWayPlatformSystem oneWayPlatformSystem;

    /**
     * 床系統。
     *
     * 負責床的一方通行平台、床 collider、跳床死亡與 Shift 下落。
     */
    private BedSystem bedSystem;


    // =========================================================
    // UI
    // =========================================================

    /**
     * 任務 HUD。
     */
    private QuestHUD questHUD;


    // =========================================================
    // Wake Up Intro State
    // =========================================================

    /**
     * 是否正在播放起床開場動畫。
     *
     * 播放期間：
     * 1. 不更新互動。
     * 2. 不接受玩家互動。
     * 3. 不更新房間、床、平台等 runtime system。
     */
    private boolean wakeUpIntroPlaying = false;

    /**
     * 起床動畫使用的黑色 UI 遮罩。
     */
    private Rectangle wakeUpBlackOverlay;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立 HouseScene。
     *
     * @param config 場景設定
     * @param deathSystem 死亡系統
     * @param audioSystem 音效系統
     * @param sceneManager 場景管理器
     */
    public HouseScene(
            SceneConfig config,
            DeathSystem deathSystem,
            AudioSystem audioSystem,
            SceneManager sceneManager
    ) {
        this.config = config;
        this.deathSystem = deathSystem;
        this.audioSystem = audioSystem;
        this.sceneManager = sceneManager;
    }


    // =========================================================
    // Load / Cleanup
    // =========================================================

    /**
     * 載入 HouseScene。
     *
     * 預設不播放起床動畫。
     *
     * @return 生成出的玩家 Entity
     */
    public Entity load() {
        return load(false);
    }

    /**
     * 載入 HouseScene。
     *
     * 載入流程：
     * 1. 生成測試物件。
     * 2. 生成背景。
     * 3. 生成玩家。
     * 4. 生成場景碰撞與死亡區。
     * 5. 初始化系統。
     * 6. 生成房間黑幕。
     * 7. 生成互動物件與視覺物件。
     * 8. 生成動畫物件。
     * 9. 建立任務 HUD。
     * 10. 設定攝影機。
     * 11. 視需求播放起床動畫。
     *
     * @param playWakeUpIntro 是否播放起床開場動畫
     * @return 生成出的玩家 Entity
     */
    public Entity load(boolean playWakeUpIntro) {
        spawnTestObjects();
        spawnBackgroundLayers();

        player = spawn("player", config.getPlayerStartX(), config.getPlayerStartY());

        spawnStaticCollisions();

        initRuntimeSystems();

        spawnRoomCovers();
        spawnInteractableProps();
        spawnAnimatedProps();

        initQuestHUD();
        setupCamera();

        if (playWakeUpIntro) {
            playWakeUpIntroAnimation();
        }

        return player;
    }

    /**
     * 清理 HouseScene。
     *
     * 通常在離開 HouseScene 或切換場景時呼叫。
     *
     * 清理內容：
     * 1. 移除起床動畫黑幕。
     * 2. 停止起床動畫狀態。
     * 3. dispose InteractionSystem。
     * 4. 移除 QuestHUD。
     * 5. 解除攝影機跟隨。
     * 6. 關閉攝影機 lazy 狀態。
     */
    public void cleanup() {
        removeWakeUpOverlayIfPresent();

        wakeUpIntroPlaying = false;

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


    // =========================================================
    // Public Runtime API
    // =========================================================

    /**
     * 每幀更新 HouseScene。
     *
     * 由 SceneManager 呼叫。
     *
     * 更新順序：
     * 1. 起床動畫期間不更新任何 runtime system。
     * 2. 更新 InteractionSystem。
     * 3. 更新 RoomSystem。
     * 4. 更新 OneWayPlatformSystem。
     * 5. 更新 BedSystem。
     * 6. 更新 QuestHUD。
     *
     * @param tpf time per frame
     */
    public void onUpdate(double tpf) {
        if (wakeUpIntroPlaying) {
            return;
        }

        updateInteractionSystem(tpf);
        updateRoomSystem(tpf);
        updateOneWayPlatformSystem(tpf);
        updateBedSystem(tpf);
        updateQuestHUD();
    }

    /**
     * 嘗試與附近可互動物件互動。
     *
     * 由 SceneManager 在玩家按下互動鍵時呼叫。
     */
    public void tryInteract() {
        if (wakeUpIntroPlaying) {
            return;
        }

        if (interactionSystem != null) {
            interactionSystem.interact();
        }
    }

    /**
     * 玩家按下跳躍鍵時呼叫。
     *
     * 用途：
     * 通知床系統與一般單向平台系統，
     * 玩家可能從一方通行平台上跳起。
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
     * 玩家按下下落鍵時呼叫。
     *
     * 用途：
     * 通知床系統與一般單向平台系統，
     * 玩家可能要向下穿過一方通行平台。
     *
     * 注意：
     * 建議先處理 BedSystem，
     * 因為床的狀態較特殊，且有自己的 collider 管理。
     */
    public void dropThroughOneWayPlatform() {
        if (bedSystem != null) {
            bedSystem.dropThrough();
        }

        if (oneWayPlatformSystem != null) {
            oneWayPlatformSystem.dropThrough();
        }
    }

    /**
     * 重生或死亡流程結束時重設 HouseScene 的 runtime system。
     *
     * 目前會重設：
     * 1. 一般單向平台系統。
     * 2. 床系統。
     */
    public void resetRuntimeSystems() {
        if (oneWayPlatformSystem != null) {
            oneWayPlatformSystem.reset();
        }

        if (bedSystem != null) {
            bedSystem.reset();
        }
    }


    // =========================================================
    // Save / Load State
    // =========================================================

    /**
     * 套用存檔狀態。
     *
     * 建議在 load() 完成所有 Entity 生成後呼叫。
     *
     * 還原順序：
     * 1. 還原房間黑幕狀態。
     * 2. 套用所有實作 LoadSaveComponent 的元件狀態。
     * 3. 還原 BedSystem 狀態。
     *
     * 注意：
     * BedSystem 不是 Component，
     * 所以不會被 applyStateToLoadSaveComponents() 掃描到，
     * 需要另外呼叫。
     */
    public void applySavedState() {
        applyRoomCoverState();
        applyStateToLoadSaveComponents();
        applyBedSystemSavedState();
    }

    /**
     * 根據存檔變數還原房間黑幕。
     */
    private void applyRoomCoverState() {
        if (getb("room_LIVING_ROOM_revealed")) {
            roomSystem.revealRoomNoAnimation(RoomType.LIVING_ROOM);
        }

        if (getb("room_TOILET_revealed")) {
            roomSystem.revealRoomNoAnimation(RoomType.TOILET);
        }
    }

    /**
     * 套用所有 Entity 上的 LoadSaveComponent 狀態。
     *
     * 例如：
     * - DoorComponent
     * - QuiltComponent
     * - WaterComponent
     * - ShoeComponent
     *
     * 好處：
     * 新增可存檔互動物件時，
     * 只要該 Component 實作 LoadSaveComponent，
     * 這裡就會自動套用。
     */
    private void applyStateToLoadSaveComponents() {
        getGameWorld()
                .getEntitiesCopy()
                .forEach(entity -> {
                    for (Component component : entity.getComponents()) {
                        if (component instanceof LoadSaveComponent loadSaveComponent) {
                            loadSaveComponent.applySavedState();
                        }
                    }
                });
    }

    /**
     * 套用 BedSystem 的存檔狀態。
     *
     * BedSystem 不是 FXGL Component，
     * 因此需要獨立還原。
     */
    private void applyBedSystemSavedState() {
        if (bedSystem != null) {
            bedSystem.applySavedState();
        }
    }


    // =========================================================
    // System Initialization
    // =========================================================

    /**
     * 初始化 HouseScene 運行時系統。
     *
     * 必須在 player 生成後呼叫。
     */
    private void initRuntimeSystems() {
        interactionSystem = new InteractionSystem(player);
        roomSystem = new RoomSystem(player, deathSystem);
        oneWayPlatformSystem = new OneWayPlatformSystem(player);
        bedSystem = new BedSystem(player, deathSystem);
    }

    /**
     * 初始化任務 HUD。
     */
    private void initQuestHUD() {
        questHUD = new QuestHUD();
        addUINode(questHUD, 0, 0);
    }


    // =========================================================
    // Update Helpers
    // =========================================================

    private void updateInteractionSystem(double tpf) {
        if (interactionSystem != null) {
            interactionSystem.update(tpf);
        }
    }

    private void updateRoomSystem(double tpf) {
        if (roomSystem != null) {
            roomSystem.update(tpf);
        }
    }

    private void updateOneWayPlatformSystem(double tpf) {
        if (oneWayPlatformSystem != null) {
            oneWayPlatformSystem.update(tpf);
        }
    }

    private void updateBedSystem(double tpf) {
        if (bedSystem != null) {
            bedSystem.update(tpf);
        }
    }

    private void updateQuestHUD() {
        if (questHUD != null) {
            questHUD.update();
        }
    }


    // =========================================================
    // Scene Spawning - Background / Collisions
    // =========================================================

    /**
     * 生成測試用物件。
     *
     * 目前保留空方法，方便日後快速測試平台、陷阱或互動物件。
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
     * 生成家中場景的背景圖層。
     *
     * 這些 Entity 通常只負責顯示，不負責碰撞。
     */
    private void spawnBackgroundLayers() {
        spawn("window_view", new SpawnData(0, 0)
                .put("texture", "/Scene1/map/window_view.png")
                .put("parallaxFactor", 0.01));

        spawn("house_background", 0, 0);
        spawn("house_floor", 0, 0);
        spawn("house_ceiling", 0, 0);
        spawn("house_foreground", 0, 0);
    }

    /**
     * 生成 HouseScene 的固定碰撞與死亡區。
     *
     * 包含：
     * 1. 地板。
     * 2. 牆壁。
     * 3. 浴缸。
     * 4. 天花板死亡區。
     * 5. 門框死亡區。
     * 6. 浴簾桿死亡區。
     */
    private void spawnStaticCollisions() {
        spawnFloors();
        spawnWalls();
        spawnBathtub(3034, 555, 140, 111);
        spawnDeathZones();
    }

    /**
     * 生成地板碰撞。
     */
    private void spawnFloors() {
        spawn("floor", new SpawnData(476, 664)
                .put("width", 2724.0)
                .put("height", 70.0));

        spawn("floor", new SpawnData(0, 694)
                .put("width", 3200.0)
                .put("height", 70.0));
    }

    /**
     * 生成牆壁碰撞。
     */
    private void spawnWalls() {
        spawn("wall", new SpawnData(-25, 0)
                .put("width", 50.0)
                .put("height", 345.0));

        spawn("wall", new SpawnData(-25, 345)
                .put("width", 45.0)
                .put("height", 575.0));

        spawn("wall", new SpawnData(2079, 0)
                .put("width", 27.0)
                .put("height", 324.0));

        spawn("wall", new SpawnData(2776, 0)
                .put("width", 27.0)
                .put("height", 324.0));

        spawn("wall", new SpawnData(3169, 0)
                .put("width", 50.0)
                .put("height", 720.0));
    }

    /**
     * 生成碰到即死的區域。
     */
    private void spawnDeathZones() {
        spawnDeathWall(
                0,
                0,
                3200,
                225,
                DeathReason.HIT_CEILING
        );

        spawnDeathWall(
                2769,
                311,
                38,
                23,
                DeathReason.HIT_DOORFRAME
        );

        spawnDeathWall(
                2073,
                311,
                38,
                23,
                DeathReason.HIT_DOORFRAME
        );

        spawnDeathWall(
                -12,
                345,
                38,
                23,
                DeathReason.HIT_DOORFRAME
        );

        spawnDeathWall(
                3037,
                246,
                14,
                14,
                DeathReason.HIT_SHOWER_CURTAIN_ROD
        );
    }

    /**
     * 生成單一死亡牆。
     *
     * @param x X 座標
     * @param y Y 座標
     * @param width 寬度
     * @param height 高度
     * @param deathReason 死亡原因
     */
    private void spawnDeathWall(
            double x,
            double y,
            double width,
            double height,
            DeathReason deathReason
    ) {
        spawn("death_wall", new SpawnData(x, y)
                .put("width", width)
                .put("height", height)
                .put("deathReason", deathReason));
    }


    // =========================================================
    // Scene Spawning - Room Covers
    // =========================================================

    /**
     * 生成房間黑色遮罩。
     *
     * 開啟對應門後，
     * DoorComponent 的 onOpen callback 會呼叫 RoomSystem.revealRoom()，
     * 讓黑幕淡出。
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


    // =========================================================
    // Scene Spawning - Interactable Props
    // =========================================================

    /**
     * 生成 HouseScene 中所有互動物件與主要視覺物件。
     */
    private void spawnInteractableProps() {
        spawnExitDoor();
        spawnRoomDoors();
        spawnQuilt();
        spawnBed();
        spawnWater();
        spawnToothbrush();
        spawnShoeCabinet();
        spawnMom();
        spawnStaticVisualProps();
    }

    /**
     * 生成離開家門。
     *
     * 玩家互動後會透過 SceneManager 切換到 StreetScene。
     */
    private void spawnExitDoor() {
        spawn("exit_door", new SpawnData(0, 376)
                .put("width", 20.0)
                .put("height", 318.0)
                .put("interactRange", 180.0)
                .put("promptOnEntity", false)
                .put("promptOffsetY", 45.0)
                .put("sceneManager", sceneManager));
    }

    /**
     * 生成房間門。
     *
     * Door1：
     * - 開啟後顯示客廳。
     *
     * Door2：
     * - 開啟後顯示廁所。
     */
    private void spawnRoomDoors() {
        Entity livingRoomDoor = spawnDoor(
                2054,
                290,
                "Door1",
                "/Scene1/props/Door_1_closed.png",
                "/Scene1/props/Door_1_opened.png",
                27,
                32,
                21,
                378
        );

        livingRoomDoor.getComponent(ass.example.components.HouseScene.DoorComponent.class)
                .setOnOpen(() -> roomSystem.revealRoom(RoomType.LIVING_ROOM));

        Entity toiletDoor = spawnDoor(
                2755,
                290,
                "Door2",
                "/Scene1/props/Door_2_closed.png",
                "/Scene1/props/Door_2_opened.png",
                24,
                32,
                21,
                378
        );

        toiletDoor.getComponent(ass.example.components.HouseScene.DoorComponent.class)
                .setOnOpen(() -> roomSystem.revealRoom(RoomType.TOILET));
    }

    /**
     * 生成單一房間門。
     *
     * @return 生成出的門 Entity
     */
    private Entity spawnDoor(
            double x,
            double y,
            String id,
            String closedTexture,
            String openTexture,
            double colliderOffsetX,
            double colliderOffsetY,
            double colliderWidth,
            double colliderHeight
    ) {
        return spawn("door", new SpawnData(x, y)
                .put("id", id)
                .put("closedTexture", closedTexture)
                .put("openTexture", openTexture)
                .put("colliderOffsetX", colliderOffsetX)
                .put("colliderOffsetY", colliderOffsetY)
                .put("colliderWidth", colliderWidth)
                .put("colliderHeight", colliderHeight)
                .put("interactRange", 120.0)
                .put("promptOnEntity", false)
                .put("promptOffsetY", 35.0)
                .put("audioSystem", audioSystem));
    }

    /**
     * 生成棉被與棉被互動區。
     */
    private void spawnQuilt() {
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
    }

    /**
     * 生成床視覺圖與床的一方通行平台資料。
     */
    private void spawnBed() {
        spawn("bed", 0, 0);

        spawn("bed_one_way_platform", new SpawnData(2445, 581)
                .put("id", "Bed1")
                .put("width", 223.0 - player.getBoundingBoxComponent().getWidth())
                .put("height", 10.0)

                // 第一組 bed collider。
                .put("collider1OffsetX", 0.0)
                .put("collider1OffsetY", 0.0)
                .put("collider1Width", 321.0)
                .put("collider1Height", 10.0)

                // 第二組 bed collider。
                .put("collider2OffsetX", 316.0)
                .put("collider2OffsetY", -244.0)
                .put("collider2Width", 6.0)
                .put("collider2Height", 254.0)

                .put("playerZIndexOnBed", -3)
                .put("normalPlayerZIndex", 0)
                .put("deathReason", DeathReason.JUMPING_ON_BED));
    }

    /**
     * 生成水龍頭水流與互動區。
     */
    private void spawnWater() {
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
    }

    /**
     * 生成牙刷互動區。
     */
    private void spawnToothbrush() {
        spawn("toothbrush_trigger", new SpawnData(2928, 513)
                .put("width", 80.0)
                .put("height", 120.0)
                .put("interactRange", 180.0)
                .put("promptOnEntity", true)
                .put("promptOffsetY", 50.0)
                .put("audioSystem", audioSystem));
    }

    /**
     * 生成鞋櫃視覺物件與穿鞋互動區。
     */
    private void spawnShoeCabinet() {
        Entity shoeCabinet = spawn("shoe_cabinet", new SpawnData(0, 0));

        spawn("shoe_trigger", new SpawnData(132, 371)
                .put("width", 300.0)
                .put("height", 322.0)
                .put("visual", shoeCabinet)
                .put("player", player)
                .put("interactRange", 180.0)
                .put("promptOnEntity", true)
                .put("promptOffsetY", 50.0)
                .put("audioSystem", audioSystem));
    }

    /**
     * 生成媽媽角色與對話互動區。
     */
    private void spawnMom() {
        spawn("mom", new SpawnData(690, 384)
                .put("height", 282.0));

        spawn("mom_trigger", new SpawnData(686, 384)
                .put("width", 90.0)
                .put("height", 282.0)
                .put("player", player)
                .put("interactRange", 220.0)
                .put("promptOnEntity", true)
                .put("promptOffsetY", 55.0)
                .put("sceneBgmPath", BGM_HOUSE_SCENE)
                .put("dialogueBgmPath", BGM_MOM_DIALOGUE));
    }

    /**
     * 生成沒有互動邏輯的前景或背景視覺物件。
     */
    private void spawnStaticVisualProps() {
        spawn("cabinet", 0, 0);
        spawn("kitchen", new SpawnData(0, 0));
    }

    /**
     * 生成動畫物件。
     *
     * 之後可以放：
     * 1. 吊扇動畫。
     * 2. 窗簾晃動。
     * 3. 背景角色動畫。
     */
    private void spawnAnimatedProps() {
        // 循環動畫物件可放在這裡。
    }


    // =========================================================
    // Scene Spawning - Bathtub
    // =========================================================

    /**
     * 生成浴缸測試碰撞。
     *
     * 浴缸由以下部分組成：
     * 1. 左牆。
     * 2. 右牆。
     * 3. 底部牆。
     * 4. 多段 slope_wall 組成的 U 型凹面。
     * 5. 浴缸死亡 sensor。
     *
     * @param x 浴缸外框左上角 X
     * @param y 浴缸外框左上角 Y
     * @param width 浴缸外框寬度
     * @param height 浴缸外框高度
     */
    private void spawnBathtub(
            double x,
            double y,
            double width,
            double height
    ) {
        spawnBathtubOuterWalls(x, y, width, height);
        spawnBathtubCurve(x, y, width, height);
        spawnBathtubSensor(x, y, width, height);
    }

    /**
     * 生成浴缸外框牆壁。
     */
    private void spawnBathtubOuterWalls(
            double x,
            double y,
            double width,
            double height
    ) {
        // 左外牆。
        spawn("wall", new SpawnData(x, y)
                .put("width", BATHTUB_WALL_THICKNESS)
                .put("height", height));

        // 右外牆。
        spawn("wall", new SpawnData(x + width - BATHTUB_WALL_THICKNESS, y)
                .put("width", BATHTUB_WALL_THICKNESS)
                .put("height", height));

        // 底部外牆。
        spawn("wall", new SpawnData(x, y + height - BATHTUB_WALL_THICKNESS)
                .put("width", width)
                .put("height", BATHTUB_WALL_THICKNESS));
    }

    /**
     * 用多段 slope_wall 生成浴缸內部 U 型曲線。
     *
     * 曲線公式：
     *
     * t = -1 時位於左上。
     * t = 0 時位於最底。
     * t = 1 時位於右上。
     *
     * curveY = topY + (1 - t^2) * depth
     */
    private void spawnBathtubCurve(
            double x,
            double y,
            double width,
            double height
    ) {
        double centerX = x + width / 2.0;

        // 凹面左右範圍，數值越大凹面越寬。
        double radiusX = 62.0;

        // 凹面起點與最深點。
        double topY = y + 24.0;
        double bottomY = y + 74.0;
        double depth = bottomY - topY;

        for (int i = 0; i < BATHTUB_CURVE_PIECES; i++) {
            double t1 = -1.0 + 2.0 * i / BATHTUB_CURVE_PIECES;
            double t2 = -1.0 + 2.0 * (i + 1) / BATHTUB_CURVE_PIECES;

            double x1 = centerX + t1 * radiusX;
            double y1 = topY + (1.0 - t1 * t1) * depth;

            double x2 = centerX + t2 * radiusX;
            double y2 = topY + (1.0 - t2 * t2) * depth;

            spawnSlopeBetweenPoints(
                    x1,
                    y1,
                    x2,
                    y2,
                    BATHTUB_CURVE_THICKNESS
            );
        }
    }

    /**
     * 在兩點之間生成一段斜牆。
     *
     * slope_wall 的角度由兩點計算而來。
     *
     * @param x1 起點 X
     * @param y1 起點 Y
     * @param x2 終點 X
     * @param y2 終點 Y
     * @param thickness 斜牆厚度
     */
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

        // 避免太短的斜面造成奇怪碰撞。
        if (length < 4.0) {
            return;
        }

        double angle = Math.toDegrees(Math.atan2(dy, dx));

        // 使用線段中心作為基準，讓斜牆與曲線更貼合。
        double centerX = (x1 + x2) / 2.0;
        double centerY = (y1 + y2) / 2.0;

        double spawnX = centerX - length / 2.0;
        double spawnY = centerY - thickness / 2.0;

        spawn("slope_wall", new SpawnData(spawnX, spawnY)
                .put("width", length)
                .put("height", thickness)
                .put("angle", angle));
    }

    /**
     * 生成浴缸死亡 sensor。
     *
     * 玩家進入 sensor 且速度達到門檻時，
     * BathtubComponent 會觸發死亡。
     */
    private void spawnBathtubSensor(
            double x,
            double y,
            double width,
            double height
    ) {
        double sensorWidth = width * BATHTUB_SENSOR_WIDTH_RATIO;
        double sensorHeight = BATHTUB_SENSOR_HEIGHT;

        // 放在浴缸 U 型底部附近。
        double sensorX = x + (width - sensorWidth) / 2.0;
        double sensorY = y + height * 0.48;

        spawn("bathtub_sensor", new SpawnData(sensorX, sensorY)
                .put("width", sensorWidth)
                .put("height", sensorHeight)
                .put("player", player)
                .put("deathSystem", deathSystem)
                .put("deathReason", DeathReason.JUMPED_IN_BATHTUB)
                .put("deathSpeedThreshold", BATHTUB_DEATH_SPEED_THRESHOLD));
    }


    // =========================================================
    // Wake Up Intro
    // =========================================================

    /**
     * 播放起床開場動畫。
     *
     * 流程：
     * 1. 停止玩家移動並鎖住控制。
     * 2. 玩家顯示躺在床上的姿勢。
     * 3. 加入黑色 UI 遮罩。
     * 4. 播放早晨環境音。
     * 5. 從黑畫面淡入。
     * 6. 停頓顯示玩家躺床。
     * 7. 畫面淡黑。
     * 8. 黑畫面期間播放起床音效並瞬移玩家到正式起始點。
     * 9. 恢復玩家正常外觀。
     * 10. 從黑畫面淡回遊戲。
     * 11. 移除黑幕。
     * 12. 解鎖玩家控制。
     * 13. 開啟攝影機 lazy。
     * 14. 切回正式 HouseScene BGM。
     */
    private void playWakeUpIntroAnimation() {
        if (player == null) {
            return;
        }

        wakeUpIntroPlaying = true;

        PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);

        preparePlayerForWakeUpIntro(playerComponent);
        createWakeUpBlackOverlay();

        MusicSystem.getInstance().playBGM(BGM_MORNING_SOUND, true);

        FadeTransition fadeInFromBlack = createFadeTransition(
                wakeUpBlackOverlay,
                WAKE_FADE_IN_DURATION,
                1.0,
                0.0
        );

        PauseTransition liePause = createPauseTransition(WAKE_LIE_PAUSE_DURATION);

        FadeTransition fadeOutToBlack = createFadeTransition(
                wakeUpBlackOverlay,
                WAKE_FADE_OUT_DURATION,
                0.0,
                1.0
        );

        PauseTransition blackPause = createPauseTransition(WAKE_BLACK_PAUSE_DURATION);

        FadeTransition fadeBackIn = createFadeTransition(
                wakeUpBlackOverlay,
                WAKE_FADE_BACK_IN_DURATION,
                1.0,
                0.0
        );

        fadeOutToBlack.setOnFinished(event -> movePlayerOutOfBedDuringBlackScreen(playerComponent));

        SequentialTransition sequence = new SequentialTransition(
                fadeInFromBlack,
                liePause,
                fadeOutToBlack,
                blackPause,
                fadeBackIn
        );

        sequence.setOnFinished(event -> finishWakeUpIntro(playerComponent));
        sequence.play();
    }

    /**
     * 起床動畫開始前，停止玩家動作並切換成躺床姿勢。
     */
    private void preparePlayerForWakeUpIntro(PlayerComponent playerComponent) {
        playerComponent.stopAllMovement();
        playerComponent.setControlEnabled(false);
        playerComponent.showWakeUpBedPose();
    }

    /**
     * 建立起床動畫用黑色遮罩。
     */
    private void createWakeUpBlackOverlay() {
        wakeUpBlackOverlay = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);
        wakeUpBlackOverlay.setFill(Color.BLACK);
        wakeUpBlackOverlay.setOpacity(1.0);
        wakeUpBlackOverlay.setMouseTransparent(false);

        addUINode(wakeUpBlackOverlay, 0, 0);
    }

    /**
     * 黑畫面期間將玩家從床上移動到正式起始點。
     *
     * 因為此時畫面全黑，
     * 所以玩家瞬移不會被看到。
     */
    private void movePlayerOutOfBedDuringBlackScreen(PlayerComponent playerComponent) {
        audioSystem.playSFX(SoundId.FOLDING_QUILT);

        playerComponent.moveInstantlyTo(
                config.getPlayerStartX(),
                config.getPlayerStartY()
        );

        playerComponent.restoreAfterWakeUpIntro();

        snapCameraToPlayer();
    }

    /**
     * 結束起床動畫。
     *
     * 會移除黑幕、解鎖玩家控制並切換回正常 BGM。
     */
    private void finishWakeUpIntro(PlayerComponent playerComponent) {
        removeWakeUpOverlayIfPresent();

        wakeUpIntroPlaying = false;

        playerComponent.setControlEnabled(true);

        getGameScene().getViewport().setLazy(true);

        MusicSystem.getInstance().playBGM(BGM_HOUSE_SCENE, true);
    }

    /**
     * 建立 FadeTransition。
     */
    private FadeTransition createFadeTransition(
            Rectangle target,
            double seconds,
            double fromOpacity,
            double toOpacity
    ) {
        FadeTransition fadeTransition = new FadeTransition(
                Duration.seconds(seconds),
                target
        );

        fadeTransition.setFromValue(fromOpacity);
        fadeTransition.setToValue(toOpacity);

        return fadeTransition;
    }

    /**
     * 建立 PauseTransition。
     */
    private PauseTransition createPauseTransition(double seconds) {
        return new PauseTransition(Duration.seconds(seconds));
    }

    /**
     * 移除起床動畫黑幕。
     *
     * 若黑幕不存在，則不做任何事。
     */
    private void removeWakeUpOverlayIfPresent() {
        if (wakeUpBlackOverlay == null) {
            return;
        }

        removeUINode(wakeUpBlackOverlay);
        wakeUpBlackOverlay = null;
    }


    // =========================================================
    // Camera
    // =========================================================

    /**
     * 設定橫向卷軸攝影機。
     *
     * 攝影機設定：
     * 1. 限制攝影機範圍不超出地圖。
     * 2. 初始位置對準玩家起始點。
     * 3. 綁定玩家。
     * 4. 啟用 lazy 平滑跟隨。
     */
    private void setupCamera() {
        getGameScene().getViewport().setBounds(
                0,
                0,
                config.getMapWidth(),
                config.getMapHeight()
        );

        getGameScene().getViewport().setX(getCameraXForPlayerX(config.getPlayerStartX()));
        getGameScene().getViewport().setY(0);

        getGameScene().getViewport().bindToEntity(
                player,
                CAMERA_FOLLOW_X,
                CAMERA_FOLLOW_Y
        );

        getGameScene().getViewport().setLazy(true);
    }

    /**
     * 立即將攝影機移動到玩家目前位置。
     *
     * 用於起床動畫中玩家瞬移後，
     * 讓攝影機也立刻對準玩家。
     */
    private void snapCameraToPlayer() {
        if (player == null) {
            return;
        }

        getGameScene().getViewport().setX(getCameraXForPlayerX(player.getX()));
        getGameScene().getViewport().setY(0);
    }

    /**
     * 根據玩家 X 座標計算攝影機 X 座標。
     *
     * @param playerX 玩家 X 座標
     * @return 攝影機 X 座標，已限制在地圖範圍內
     */
    private double getCameraXForPlayerX(double playerX) {
        return clamp(
                playerX - SCREEN_WIDTH / 2.0,
                0,
                config.getMapWidth() - SCREEN_WIDTH
        );
    }
}