package ass.example.scenes;

import ass.example.core.DeathReason;
import ass.example.core.StreetScene.FallingObjectVariant;
import ass.example.core.StreetScene.StreetApartmentStyle;
import ass.example.scenes.system.SceneConfig;
import ass.example.scenes.system.SceneManager;
import ass.example.system.InteractionSystem;
import ass.example.ui.QuestHUD;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.animation.FadeTransition;
import javafx.geometry.Point2D;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * StreetScene
 *
 * 故事模式用的街道場景。
 *
 * 主要職責：
 * 1. 建立街道背景、地板、街景區塊與邊界牆。
 * 2. 生成玩家與返回家中的互動門。
 * 3. 管理 InteractionSystem 與 QuestHUD。
 * 4. 動態生成街道障礙物，例如變電箱、凸起磁磚。
 * 5. 管理街道事件，例如機車橫向衝撞、墜落物。
 * 6. 管理攝影機與遠景 parallax。
 * 7. 提供街道區塊與障礙物的存檔 / 讀檔字串。
 */
public class StreetScene {
 
    // Scene Dependencies 

    /**
     * 場景設定。
     *
     * 保存地圖大小、玩家起始位置等資料。
     */
    private final SceneConfig config;

    /**
     * 場景管理器。
     *
     * entrance_door 需要透過它切回 HouseScene。
     */
    private final SceneManager sceneManager;

    /**
     * 隨機數產生器。
     *
     * 用於街景樣式、障礙物、機車、墜落物等隨機生成。
     */
    private final Random random = new Random();

 
    // Runtime Entities / Systems 

    /**
     * 玩家 Entity。
     */
    private Entity player;

    /**
     * 互動系統。
     *
     * 負責偵測附近互動物件與處理 F 鍵互動。
     */
    private InteractionSystem interactionSystem;

    /**
     * 任務 HUD。
     *
     * StreetScene 為故事模式的一部分，因此仍保留任務 HUD。
     */
    private QuestHUD questHUD;

 
    // Screen / Map Constants 

    private static final double SCREEN_WIDTH = 1280.0;
    private static final double SCREEN_HEIGHT = 720.0;

    /**
     * 每一段街道區塊寬度。
     *
     * 地板、公寓背景、公寓前景共用此寬度。
     */
    private static final double SEGMENT_WIDTH = 640.0;

    /**
     * 地板碰撞箱的 Y 位置。
     */
    private static final double FLOOR_Y = 694.0;

    /**
     * 故事模式街道最多生成幾段。
     *
     * 達到上限後會生成左邊界牆，避免玩家繼續往左走。
     */
    private static final int MAX_SEGMENT_COUNT = 12;

 
    // Camera Constants / State 

    /**
     * 攝影機最右邊界。
     *
     * 0 代表鏡頭不能往右超過出生畫面。
     */
    private static final double MAX_CAMERA_X = 0.0;

    /**
     * 目前鎖定的攝影機 X。
     */
    private double lockedCameraX = 0.0;

 
    // Far Background Constants / State 

    private static final double FAR_BACKGROUND_WIDTH = 1983.0;
    private static final double FAR_BACKGROUND_HEIGHT = 793.0;
    private static final double FAR_BACKGROUND_PARALLAX = 0.18;

    /**
     * 目前最左 / 最右的遠景 baseX。
     *
     * baseX 是遠景自己的基準位置，不是套用 parallax 後的實際 entityX。
     */
    private double leftMostFarBaseX = 0.0;
    private double rightMostFarBaseX = 0.0;

    /**
     * 遠景分段清單。
     */
    private final List<FarBackgroundSegment> farBackgrounds = new ArrayList<>();

 
    // Segment Generation State 

    /**
     * 已生成到最左側的街道區塊 X。
     *
     * 因為玩家往左走，所以地圖會往負 X 方向延伸。
     */
    private double leftMostGeneratedX = 0.0;

    /**
     * 是否已達到故事街道的最大生成段數。
     */
    private boolean segmentLimitReached = false;

    /**
     * 達到街道段數上限後，用來阻止玩家繼續往左的牆。
     */
    private Entity leftBoundaryWall;

    /**
     * 街道區塊資料。
     *
     * 由左到右排序時，越左邊的區塊會放在 index 0。
     */
    private final List<StreetSegment> segments = new ArrayList<>();

 
    // Floor Collider 

    /**
     * 長地板碰撞箱。
     *
     * 雖然地板視覺是一段一段生成，
     * 但碰撞箱使用單一超長 floor，避免玩家踩到分段接縫時卡住。
     */
    private Entity endlessFloorCollider;

    private static final double ENDLESS_FLOOR_COLLIDER_WIDTH = 6000.0;
    private static final double ENDLESS_FLOOR_COLLIDER_HEIGHT = 70.0;

 
    // Obstacle Constants / State 

    /**
     * 街道障礙物清單。
     *
     * 每組障礙物通常由 visual + collider / trigger 組成。
     */
    private final List<StreetObstacleGroup> obstacleGroups = new ArrayList<>();

    /**
     * 下一次檢查是否生成變電箱的位置。
     */
    private double nextTransformerCheckX = -900.0;

    /**
     * 下一次檢查是否生成凸起磁磚的位置。
     */
    private double nextRaisedTileCheckX = -700.0;

    private static final double TRANSFORMER_CHECK_MIN_DISTANCE = 700.0;
    private static final double TRANSFORMER_CHECK_MAX_DISTANCE = 1200.0;
    private static final double TRANSFORMER_SPAWN_CHANCE = 0.45;

    private static final double TRANSFORMER_WIDTH = 90.0;
    private static final double TRANSFORMER_HEIGHT = 145.0;
    private static final double TRANSFORMER_COLLIDER_WIDTH = 78.0;
    private static final double TRANSFORMER_COLLIDER_HEIGHT = 132.0;

    private static final double RAISED_TILE_CHECK_MIN_DISTANCE = 420.0;
    private static final double RAISED_TILE_CHECK_MAX_DISTANCE = 760.0;
    private static final double RAISED_TILE_SPAWN_CHANCE = 0.65;

    private static final double RAISED_TILE_WIDTH = 75.0;
    private static final double RAISED_TILE_HEIGHT = 28.0;

    /**
     * 障礙物預先生成距離。
     *
     * 玩家往左跑，因此會在玩家左方更遠處預先生成障礙物。
     */
    private static final double OBSTACLE_GENERATE_AHEAD_DISTANCE = 2600.0;

 
    // Scooter Constants / State 

    /**
     * 場上目前存在的機車。
     */
    private final List<ScooterInstance> scooters = new ArrayList<>();

    /**
     * 左右兩側機車各自獨立計時。
     */
    private double leftScooterTimer = 4.0;
    private double rightScooterTimer = 6.0;

    /**
     * 左右兩側 warning 是否正在播放。
     */
    private boolean leftWarningActive = false;
    private boolean rightWarningActive = false;

    private double leftWarningTimer = 0.0;
    private double rightWarningTimer = 0.0;

    private static final double SCOOTER_WARNING_DURATION = 1.6;

    /**
     * 機車出現間隔範圍。
     *
     * 之後可以調整成隨遊戲進度逐漸變短。
     */
    private static final double SCOOTER_MIN_INTERVAL = 1.0;
    private static final double SCOOTER_MAX_INTERVAL = 15.0;

    private static final double SCOOTER_WIDTH = 150.0;
    private static final double SCOOTER_HEIGHT = 72.0;

    private static final double SCOOTER_HITBOX_WIDTH = 140.0;
    private static final double SCOOTER_HITBOX_HEIGHT = 58.0;

    private static final double SCOOTER_HITBOX_OFFSET_X = 5.0;
    private static final double SCOOTER_HITBOX_OFFSET_Y = 8.0;

    private static final double SCOOTER_SPEED = 720.0;

    private static final double WARNING_ICON_SIZE = 72.0;

    /**
     * 機車在地面上的 Y 座標。
     */
    private static final double SCOOTER_Y = FLOOR_Y - SCOOTER_HEIGHT + 2.0;

    /**
     * 左右兩邊機車 warning UI。
     */
    private StackPane leftWarningIcon;
    private StackPane rightWarningIcon;

 
    // Falling Object Constants / State 

    /**
     * 場上目前存在的墜落物。
     */
    private final List<FallingObjectInstance> fallingObjects = new ArrayList<>();

    private double fallingObjectTimer = 3.5;

    private static final double FALLING_OBJECT_MIN_INTERVAL = 3.8;
    private static final double FALLING_OBJECT_MAX_INTERVAL = 7.2;

    /**
     * 墜落物生成範圍：
     * cameraX - 左側預生成距離 到 cameraX + 畫面寬度 + 右側 padding。
     */
    private static final double FALLING_SPAWN_AHEAD_LEFT_DISTANCE = 1500.0;
    private static final double FALLING_SPAWN_RIGHT_PADDING = 240.0;

    /**
     * 墜落物生成高度。
     */
    private static final double FALLING_SPAWN_MIN_HEIGHT_ABOVE_SCREEN = 260.0;
    private static final double FALLING_SPAWN_MAX_HEIGHT_ABOVE_SCREEN = 620.0;

    /**
     * 墜落物距離畫面頂部多近時，warning icon 逐漸變大。
     */
    private static final double FALLING_WARNING_DISTANCE = 620.0;
    private static final double FALLING_WARNING_ICON_SIZE = 72.0;

 
    // Constructor 

    public StreetScene(
            SceneConfig config,
            SceneManager sceneManager
    ) {
        this.config = config;
        this.sceneManager = sceneManager;
    }

 
    // Load / Cleanup 

    /**
     * 載入 StreetScene。
     *
     * 載入流程：
     * 1. 初始化遊戲變數。
     * 2. 生成遠景背景。
     * 3. 生成初始街道區塊。
     * 4. 生成長地板碰撞箱。
     * 5. 生成右側邊界。
     * 6. 生成玩家。
     * 7. 初始化互動系統。
     * 8. 生成返回家中的互動門。
     * 9. 初始化障礙物、機車、墜落物系統。
     * 10. 建立任務 HUD。
     * 11. 設定攝影機。
     *
     * @return 生成出的玩家 Entity
     */
    public Entity load() {
        initSceneVars();

        spawnFarBackground();
        generateInitialSegments();
        spawnEndlessFloorCollider();
        spawnRightBoundary();

        player = spawn("player", config.getPlayerStartX(), config.getPlayerStartY());

        initInteractionSystem();
        spawnInteractableProps();

        resetObstacleSpawner();
        createScooterWarningUI();
        resetScooterTimers();
        resetFallingObjectSystem();

        initQuestHUD();
        setupCamera();

        return player;
    }

    /**
     * 初始化 StreetScene 使用的 FXGL 變數。
     */
    private void initSceneVars() {
        set("saveDisabled", false);
        set("achievementDisabled", false);
        set("playerDead", false);
    }

    /**
     * 初始化互動系統。
     */
    private void initInteractionSystem() {
        interactionSystem = new InteractionSystem(player);
    }

    /**
     * 初始化任務 HUD。
     */
    private void initQuestHUD() {
        questHUD = new QuestHUD();
        addUINode(questHUD, 0, 0);
    }

    /**
     * 清理 StreetScene。
     *
     * 通常在離開此場景或重新載入場景時呼叫。
     */
    public void cleanup() {
        cleanupInteractionSystem();
        cleanupQuestHUD();

        cleanupScootersForRespawn();
        cleanupFallingObjectsForRespawn();

        removeEndlessFloorCollider();
        removeScooterWarningUI();

        clearObstacles();
        clearSegments();
        clearFarBackgrounds();

        getGameScene().getViewport().unbind();
        getGameScene().getViewport().setLazy(false);
    }

    private void cleanupInteractionSystem() {
        if (interactionSystem == null) {
            return;
        }

        interactionSystem.dispose();
        interactionSystem = null;
    }

    private void cleanupQuestHUD() {
        if (questHUD == null) {
            return;
        }

        removeUINode(questHUD);
        questHUD = null;
    }

    private void removeEndlessFloorCollider() {
        if (endlessFloorCollider == null) {
            return;
        }

        endlessFloorCollider.removeFromWorld();
        endlessFloorCollider = null;
    }

    private void removeScooterWarningUI() {
        if (leftWarningIcon != null) {
            removeUINode(leftWarningIcon);
            leftWarningIcon = null;
        }

        if (rightWarningIcon != null) {
            removeUINode(rightWarningIcon);
            rightWarningIcon = null;
        }
    }

    private void clearFarBackgrounds() {
        for (FarBackgroundSegment segment : farBackgrounds) {
            if (segment.entity() != null && segment.entity().isActive()) {
                segment.entity().removeFromWorld();
            }
        }

        farBackgrounds.clear();

        leftMostFarBaseX = 0;
        rightMostFarBaseX = 0;
    }

 
    // Public Runtime API 

    /**
     * 每幀更新。
     *
     * 更新順序：
     * 1. 死亡時只更新攝影機。
     * 2. 更新互動提示。
     * 3. 更新攝影機。
     * 4. 更新地板碰撞箱位置。
     * 5. 更新遠景 parallax。
     * 6. 生成更多街道區塊。
     * 7. 生成障礙物。
     * 8. 更新機車系統。
     * 9. 更新墜落物系統。
     * 10. 更新任務 HUD。
     */
    public void onUpdate(double tpf) {
        if (player == null) {
            return;
        }

        if (getb("playerDead")) {
            updateCamera();
            return;
        }

        updateInteractionSystem(tpf);
        updateCamera();
        updateEndlessFloorCollider();
        updateParallaxBackground();
        generateMoreSegmentsIfNeeded();
        generateObstaclesIfNeeded();
        updateScooterSystem(tpf);
        updateScooterWarningPosition();
        updateFallingObjectSystem(tpf);
        updateQuestHUD();
    }

    /**
     * 嘗試互動。
     *
     * 由 SceneManager 在玩家按下 F 時呼叫。
     */
    public void tryInteract() {
        if (interactionSystem != null) {
            interactionSystem.interact();
        }
    }

    /**
     * 重生時重置 runtime 系統。
     *
     * 注意：
     * 不會清除地圖區塊與固定障礙物，只清除動態危險物件。
     */
    public void resetRuntimeSystems() {
        set("playerOnBedCollider", false);

        cleanupScootersForRespawn();
        cleanupFallingObjectsForRespawn();

        resetScooterTimers();
        resetFallingObjectSystem();
    }

    /**
     * 套用存檔後狀態。
     *
     * 目前 StreetScene 的主要還原邏輯由：
     * - restoreSegmentsFromSaveString()
     * - restoreObstaclesFromSaveString()
     *
     * 在 SceneManager 或 SaveSystem 中呼叫。
     */
    public void applySavedState() {
        updateQuestHUD();
    }

    private void updateInteractionSystem(double tpf) {
        if (interactionSystem != null) {
            interactionSystem.update(tpf);
        }
    }

    private void updateQuestHUD() {
        if (questHUD != null) {
            questHUD.update();
        }
    }

 
    // Scene Spawning - Interactable Props 

    /**
     * 生成街道中的互動物件。
     */
    private void spawnInteractableProps() {
        spawnEntranceDoor();
    }

    /**
     * 生成返回家中的門。
     *
     * 這個門放在街道出生點附近，玩家可互動後回 HouseScene。
     */
    private void spawnEntranceDoor() {
        spawn("entrance_door", new SpawnData(1240, FLOOR_Y - 318)
                .put("width", 40.0)
                .put("height", 318.0)
                .put("interactRange", 180.0)
                .put("promptOnEntity", false)
                .put("promptOffsetY", 45.0)
                .put("sceneManager", sceneManager));
    }

 
    // Scene Spawning - Floor / Boundary 

    /**
     * 生成長地板碰撞箱。
     */
    private void spawnEndlessFloorCollider() {
        double x = lockedCameraX - ENDLESS_FLOOR_COLLIDER_WIDTH / 2.0;

        endlessFloorCollider = spawn("floor", new SpawnData(x, FLOOR_Y)
                .put("width", ENDLESS_FLOOR_COLLIDER_WIDTH)
                .put("height", ENDLESS_FLOOR_COLLIDER_HEIGHT));
    }

    /**
     * 更新長地板碰撞箱位置，使玩家永遠踩在同一條長 floor 上。
     */
    private void updateEndlessFloorCollider() {
        if (endlessFloorCollider == null) {
            return;
        }

        double x = lockedCameraX - ENDLESS_FLOOR_COLLIDER_WIDTH / 2.0;

        PhysicsComponent physics =
                endlessFloorCollider.getComponent(PhysicsComponent.class);

        physics.overwritePosition(new Point2D(x, FLOOR_Y));
    }

    /**
     * 生成起始畫面右側邊界牆。
     *
     * 避免玩家一開始往右跑出起始畫面。
     */
    private void spawnRightBoundary() {
        spawn("wall", new SpawnData(1278, 0)
                .put("width", 40.0)
                .put("height", SCREEN_HEIGHT));
    }

    /**
     * 達到最大街道區塊數後，在最左側生成邊界牆。
     */
    private void spawnLeftBoundaryWallIfNeeded() {
        if (leftBoundaryWall != null) {
            return;
        }

        double wallX = leftMostGeneratedX - 40;

        leftBoundaryWall = spawn("wall", new SpawnData(wallX, 0)
                .put("width", 40.0)
                .put("height", SCREEN_HEIGHT));
    }

 
    // Segment Generation 

    /**
     * 生成初始街道區塊。
     *
     * 初始會生成出生點附近與左側預備區塊。
     */
    private void generateInitialSegments() {
        leftMostGeneratedX = 0;

        generateSegmentAt(SEGMENT_WIDTH, StreetApartmentStyle.RIGHT);
        generateSegmentAt(0, StreetApartmentStyle.FILL);

        leftMostGeneratedX = -SEGMENT_WIDTH;
        generateRandomSegmentToLeft();

        leftMostGeneratedX = -SEGMENT_WIDTH * 2;
        generateRandomSegmentToLeft();
    }

    /**
     * 玩家接近目前最左生成區域時，繼續往左生成新街道區塊。
     */
    private void generateMoreSegmentsIfNeeded() {
        if (player == null || segmentLimitReached) {
            return;
        }

        double playerX = player.getX();

        while (playerX - leftMostGeneratedX < SEGMENT_WIDTH * 3.0) {
            if (segments.size() >= MAX_SEGMENT_COUNT) {
                segmentLimitReached = true;
                spawnLeftBoundaryWallIfNeeded();
                return;
            }

            leftMostGeneratedX -= SEGMENT_WIDTH;
            generateRandomSegmentToLeft();
        }
    }

    /**
     * 根據右側鄰居樣式，生成一段可銜接的左側街道區塊。
     */
    private void generateRandomSegmentToLeft() {
        StreetApartmentStyle rightNeighborStyle = segments.isEmpty()
                ? StreetApartmentStyle.FILL
                : segments.get(0).style();

        StreetApartmentStyle style =
                randomCompatibleStyleForLeftOf(rightNeighborStyle);

        generateSegmentAt(leftMostGeneratedX, style);
    }

    /**
     * 從所有街道樣式中，挑出可以接在指定右鄰居左側的樣式。
     */
    private StreetApartmentStyle randomCompatibleStyleForLeftOf(
            StreetApartmentStyle rightNeighbor
    ) {
        List<StreetApartmentStyle> candidates = new ArrayList<>();

        for (StreetApartmentStyle style : StreetApartmentStyle.values()) {
            if (style.connectsRight() == rightNeighbor.connectsLeft()) {
                candidates.add(style);
            }
        }

        if (candidates.isEmpty()) {
            return StreetApartmentStyle.CENTER;
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * 生成一段街道。
     *
     * 前景是否出現由隨機決定。
     */
    private void generateSegmentAt(double x, StreetApartmentStyle style) {
        boolean hasForeground =
                style.isVisibleApartment() && random.nextBoolean();

        generateSegmentAt(x, style, hasForeground);
    }

    /**
     * 生成一段街道。
     *
     * @param x             區塊 X 座標
     * @param style         公寓樣式
     * @param hasForeground 是否生成前景
     */
    private void generateSegmentAt(
            double x,
            StreetApartmentStyle style,
            boolean hasForeground
    ) {
        Entity floorVisual = spawn("street_floor", new SpawnData(x, FLOOR_Y)
                .put("width", SEGMENT_WIDTH)
                .put("height", 70.0));

        Entity apartment = null;
        Entity foreground = null;

        if (style.isVisibleApartment()) {
            apartment = spawn("street_apartment_bg", new SpawnData(x, 150)
                    .put("width", SEGMENT_WIDTH)
                    .put("height", 544.0)
                    .put("style", style.name()));

            if (hasForeground) {
                foreground = spawn("street_apartment_fg", new SpawnData(x, 150)
                        .put("width", SEGMENT_WIDTH)
                        .put("height", 544.0)
                        .put("style", style.name()));
            }
        }

        StreetSegment segment = new StreetSegment(
                x,
                style,
                hasForeground,
                floorVisual,
                apartment,
                foreground
        );

        /*
         * 因為街道往左生成，所以越左邊的區塊放在 index 0。
         */
        segments.add(0, segment);
    }

    /**
     * 清除所有街道區塊。
     */
    private void clearSegments() {
        for (StreetSegment segment : segments) {
            removeEntityIfActive(segment.floorVisual());
            removeEntityIfActive(segment.apartment());
            removeEntityIfActive(segment.foreground());
        }

        segments.clear();

        removeEntityIfActive(leftBoundaryWall);
        leftBoundaryWall = null;

        segmentLimitReached = false;
    }

 
    // Obstacle Generation 

    /**
     * 重設障礙物生成器。
     */
    private void resetObstacleSpawner() {
        obstacleGroups.clear();

        /*
         * 一開始不要讓障礙物太靠近玩家。
         */
        nextTransformerCheckX = config.getPlayerStartX() - 1000;
        nextRaisedTileCheckX = config.getPlayerStartX() - 750;
    }

    /**
     * 根據玩家位置，預先生成左側遠處障礙物。
     */
    private void generateObstaclesIfNeeded() {
        if (player == null) {
            return;
        }

        double generateUntilX =
                player.getX() - OBSTACLE_GENERATE_AHEAD_DISTANCE;

        while (nextTransformerCheckX > generateUntilX) {
            tryGenerateTransformerAt(nextTransformerCheckX);

            nextTransformerCheckX -= randomRange(
                    TRANSFORMER_CHECK_MIN_DISTANCE,
                    TRANSFORMER_CHECK_MAX_DISTANCE
            );
        }

        while (nextRaisedTileCheckX > generateUntilX) {
            tryGenerateRaisedTileAt(nextRaisedTileCheckX);

            nextRaisedTileCheckX -= randomRange(
                    RAISED_TILE_CHECK_MIN_DISTANCE,
                    RAISED_TILE_CHECK_MAX_DISTANCE
            );
        }
    }

    /**
     * 嘗試在指定 X 附近生成變電箱。
     */
    private void tryGenerateTransformerAt(double x) {
        if (random.nextDouble() > TRANSFORMER_SPAWN_CHANCE) {
            return;
        }

        double spawnX = x + randomRange(-80, 80);
        spawnTransformerAt(spawnX);
    }

    /**
     * 生成變電箱。
     *
     * 變電箱會有 visual 與 floor collider。
     */
    private void spawnTransformerAt(double spawnX) {
        double visualY = FLOOR_Y - TRANSFORMER_HEIGHT;

        Entity visual = spawn("street_transformer_box", new SpawnData(spawnX, visualY)
                .put("width", TRANSFORMER_WIDTH)
                .put("height", TRANSFORMER_HEIGHT));

        double colliderX = spawnX + (TRANSFORMER_WIDTH - TRANSFORMER_COLLIDER_WIDTH) / 2.0;
        double colliderY = FLOOR_Y - TRANSFORMER_COLLIDER_HEIGHT;

        Entity collider = spawn("floor", new SpawnData(colliderX, colliderY)
                .put("width", TRANSFORMER_COLLIDER_WIDTH)
                .put("height", TRANSFORMER_COLLIDER_HEIGHT));

        obstacleGroups.add(new StreetObstacleGroup(
                StreetObstacleType.TRANSFORMER,
                spawnX,
                visual,
                collider
        ));
    }

    /**
     * 嘗試在指定 X 附近生成凸起磁磚。
     */
    private void tryGenerateRaisedTileAt(double x) {
        if (random.nextDouble() > RAISED_TILE_SPAWN_CHANCE) {
            return;
        }

        double spawnX = x + randomRange(-70, 70);
        spawnRaisedTileAt(spawnX);
    }

    /**
     * 生成凸起磁磚。
     *
     * 凸起磁磚碰到即死。
     */
    private void spawnRaisedTileAt(double spawnX) {
        double visualY = FLOOR_Y - RAISED_TILE_HEIGHT;

        Entity visual = spawn("street_protruding_tile", new SpawnData(spawnX, visualY)
                .put("width", RAISED_TILE_WIDTH)
                .put("height", RAISED_TILE_HEIGHT));

        Entity trigger = spawn("death_zone", new SpawnData(spawnX, visualY)
                .put("width", RAISED_TILE_WIDTH)
                .put("height", RAISED_TILE_HEIGHT)
                .put("deathReason", DeathReason.TRIPPED_BY_SIDEWALK_TILE));

        obstacleGroups.add(new StreetObstacleGroup(
                StreetObstacleType.RAISED_TILE,
                spawnX,
                visual,
                trigger
        ));
    }

    /**
     * 清除所有障礙物。
     */
    private void clearObstacles() {
        for (StreetObstacleGroup group : obstacleGroups) {
            removeEntityIfActive(group.visual());
            removeEntityIfActive(group.colliderOrTrigger());
        }

        obstacleGroups.clear();
    }

    /**
     * 讀檔後重新設定下一次障礙物生成檢查點。
     */
    private void resetObstacleSpawnerAfterRestore() {
        double leftMostObstacleX = obstacleGroups.stream()
                .mapToDouble(StreetObstacleGroup::x)
                .min()
                .orElse(leftMostGeneratedX);

        nextTransformerCheckX =
                leftMostObstacleX - TRANSFORMER_CHECK_MAX_DISTANCE;

        nextRaisedTileCheckX =
                leftMostObstacleX - RAISED_TILE_CHECK_MAX_DISTANCE;
    }

 
    // Scooter System 

    /**
     * 重設機車計時器與警告狀態。
     */
    private void resetScooterTimers() {
        leftScooterTimer = randomScooterInterval();
        rightScooterTimer = randomScooterInterval();

        leftWarningActive = false;
        rightWarningActive = false;

        leftWarningTimer = 0;
        rightWarningTimer = 0;

        hideWarningIcon(leftWarningIcon);
        hideWarningIcon(rightWarningIcon);
    }

    /**
     * 更新機車系統。
     */
    private void updateScooterSystem(double tpf) {
        updateScooterSideTimer(tpf, true);
        updateScooterSideTimer(tpf, false);
        updateScooters(tpf);
    }

    /**
     * 更新單側機車計時。
     *
     * @param fromLeft true = 左側機車；false = 右側機車
     */
    private void updateScooterSideTimer(double tpf, boolean fromLeft) {
        if (fromLeft) {
            leftScooterTimer = updateOneScooterSide(
                    tpf,
                    true,
                    leftScooterTimer,
                    leftWarningActive,
                    leftWarningTimer,
                    leftWarningIcon
            );
            return;
        }

        rightScooterTimer = updateOneScooterSide(
                tpf,
                false,
                rightScooterTimer,
                rightWarningActive,
                rightWarningTimer,
                rightWarningIcon
        );
    }

    /**
     * 更新單側機車。
     *
     * 這裡會同步更新對應 side 的 warning 狀態欄位。
     */
    private double updateOneScooterSide(
            double tpf,
            boolean fromLeft,
            double timer,
            boolean warningActive,
            double warningTimer,
            StackPane warningIcon
    ) {
        if (warningActive) {
            warningTimer -= tpf;
            updateWarningIcon(warningIcon, warningTimer);

            if (warningTimer <= 0) {
                warningActive = false;
                hideWarningIcon(warningIcon);

                spawnScooter(fromLeft);
                timer = randomScooterInterval();
            }

            setScooterWarningState(fromLeft, warningActive, warningTimer);

            return timer;
        }

        timer -= tpf;

        if (timer <= 0) {
            warningActive = true;
            warningTimer = SCOOTER_WARNING_DURATION;

            if (warningIcon != null) {
                warningIcon.setVisible(true);
            }

            updateWarningIcon(warningIcon, warningTimer);
        }

        setScooterWarningState(fromLeft, warningActive, warningTimer);

        return timer;
    }

    /**
     * 同步寫回左右 warning 狀態。
     */
    private void setScooterWarningState(
            boolean fromLeft,
            boolean active,
            double timer
    ) {
        if (fromLeft) {
            leftWarningActive = active;
            leftWarningTimer = timer;
        } else {
            rightWarningActive = active;
            rightWarningTimer = timer;
        }
    }

    /**
     * 生成摩托車。
     */
    private void spawnScooter(boolean fromLeft) {
        double cameraX = getGameScene().getViewport().getX();

        double startX;
        double velocityX;

        if (fromLeft) {
            startX = cameraX - SCOOTER_WIDTH - 80;
            velocityX = SCOOTER_SPEED;
        } else {
            startX = cameraX + SCREEN_WIDTH + 80;
            velocityX = -SCOOTER_SPEED;
        }

        Entity visual = spawn("street_scooter", new SpawnData(startX, SCOOTER_Y)
                .put("width", SCOOTER_WIDTH)
                .put("height", SCOOTER_HEIGHT)
                .put("fromLeft", fromLeft));

        Entity hitbox = spawn("street_scooter_death_wall", new SpawnData(
                startX + SCOOTER_HITBOX_OFFSET_X,
                SCOOTER_Y + SCOOTER_HITBOX_OFFSET_Y
        )
                .put("width", SCOOTER_HITBOX_WIDTH)
                .put("height", SCOOTER_HITBOX_HEIGHT)
                .put("deathReason", DeathReason.HIT_BY_SCOOTER));

        scooters.add(new ScooterInstance(
                visual,
                hitbox,
                velocityX
        ));
    }

    /**
     * 更新場上所有機車位置。
     */
    private void updateScooters(double tpf) {
        double cameraX = getGameScene().getViewport().getX();

        scooters.removeIf(scooter -> {
            double dx = scooter.velocityX() * tpf;

            Entity visual = scooter.visual();
            Entity hitbox = scooter.hitbox();

            visual.setX(visual.getX() + dx);
            hitbox.setX(hitbox.getX() + dx);

            boolean outLeft =
                    visual.getX() + SCOOTER_WIDTH < cameraX - 260;

            boolean outRight =
                    visual.getX() > cameraX + SCREEN_WIDTH + 260;

            if (!outLeft && !outRight) {
                return false;
            }

            removeEntityIfActive(visual);
            removeEntityIfActive(hitbox);

            return true;
        });
    }

    /**
     * 清除摩托車。
     */
    private void cleanupScootersForRespawn() {
        for (ScooterInstance scooter : scooters) {
            removeEntityIfActive(scooter.visual());
            removeEntityIfActive(scooter.hitbox());
        }

        scooters.clear();

        leftWarningActive = false;
        rightWarningActive = false;

        leftWarningTimer = 0;
        rightWarningTimer = 0;

        resetWarningIcon(leftWarningIcon);
        resetWarningIcon(rightWarningIcon);
    }

    private double randomScooterInterval() {
        return randomRange(SCOOTER_MIN_INTERVAL, SCOOTER_MAX_INTERVAL);
    }

 
    // Warning UI 

    /**
     * 建立左右機車警告 UI。
     */
    private void createScooterWarningUI() {
        leftWarningIcon = createDangerWarningIcon();
        rightWarningIcon = createDangerWarningIcon();

        leftWarningIcon.setVisible(false);
        rightWarningIcon.setVisible(false);

        double warningY = getScooterWarningScreenY();

        addUINode(leftWarningIcon, 34, warningY);
        addUINode(rightWarningIcon, SCREEN_WIDTH - WARNING_ICON_SIZE - 34, warningY);
    }

    /**
     * 建立危險警告 icon。
     */
    private StackPane createDangerWarningIcon() {
        StackPane box = new StackPane();

        box.setPrefSize(WARNING_ICON_SIZE, WARNING_ICON_SIZE);
        box.setMinSize(WARNING_ICON_SIZE, WARNING_ICON_SIZE);
        box.setMaxSize(WARNING_ICON_SIZE, WARNING_ICON_SIZE);
        box.setMouseTransparent(true);

        ImageView dangerView = new ImageView();

        try {
            var url = getClass().getResource("/assets/textures/scene2/UI/danger.png");

            if (url != null) {
                dangerView.setImage(new Image(url.toExternalForm()));
            } else {
                System.out.println("Danger icon not found: /assets/textures/Scene2/UI/danger.png");
            }

        } catch (Exception e) {
            System.out.println("Danger icon load failed.");
            e.printStackTrace();
        }

        dangerView.setFitWidth(WARNING_ICON_SIZE);
        dangerView.setFitHeight(WARNING_ICON_SIZE);
        dangerView.setPreserveRatio(true);
        dangerView.setSmooth(true);
        dangerView.setEffect(new DropShadow(12, Color.BLACK));

        box.getChildren().add(dangerView);

        return box;
    }

    /**
     * 更新機車 warning icon 的縮放與透明度。
     */
    private void updateWarningIcon(StackPane icon, double timer) {
        if (icon == null) {
            return;
        }

        double progress = 1.0 - timer / SCOOTER_WARNING_DURATION;
        progress = max(0, min(1, progress));

        double scale = 0.75 + progress * 0.75;

        icon.setScaleX(scale);
        icon.setScaleY(scale);

        double pulse = Math.sin(progress * Math.PI * 8) * 0.12;
        icon.setOpacity(0.72 + progress * 0.28 + pulse);
    }

    /**
     * 更新 warning icon 的畫面位置。
     */
    private void updateScooterWarningPosition() {
        double warningY = getScooterWarningScreenY();

        if (leftWarningIcon != null) {
            leftWarningIcon.setTranslateY(warningY);
        }

        if (rightWarningIcon != null) {
            rightWarningIcon.setTranslateY(warningY);
        }
    }

    private double getScooterWarningScreenY() {
        return SCOOTER_Y + SCOOTER_HEIGHT / 2.0 - WARNING_ICON_SIZE / 2.0
                - getGameScene().getViewport().getY();
    }

    private void hideWarningIcon(StackPane icon) {
        if (icon != null) {
            icon.setVisible(false);
        }
    }

    private void resetWarningIcon(StackPane icon) {
        if (icon == null) {
            return;
        }

        icon.setVisible(false);
        icon.setOpacity(1);
        icon.setScaleX(1);
        icon.setScaleY(1);
    }

 
    // Falling Object System 

    /**
     * 重設墜落物計時器。
     *
     * 注意：
     * 這裡不清空 fallingObjects。
     * 若要清空物件，請呼叫 cleanupFallingObjectsForRespawn()。
     */
    private void resetFallingObjectSystem() {
        fallingObjectTimer = randomRange(
                FALLING_OBJECT_MIN_INTERVAL,
                FALLING_OBJECT_MAX_INTERVAL
        );
    }

    /**
     * 更新墜落物系統。
     */
    private void updateFallingObjectSystem(double tpf) {
        fallingObjectTimer -= tpf;

        if (fallingObjectTimer <= 0) {
            spawnFallingObject();

            fallingObjectTimer = randomRange(
                    FALLING_OBJECT_MIN_INTERVAL,
                    FALLING_OBJECT_MAX_INTERVAL
            );
        }

        updateFallingObjects(tpf);
    }

    /**
     * 生成墜落物。
     */
    private void spawnFallingObject() {
        double cameraX = getGameScene().getViewport().getX();
        double cameraY = getGameScene().getViewport().getY();

        FallingObjectVariant variant = random.nextBoolean()
                ? FallingObjectVariant.FRIDGE
                : FallingObjectVariant.HELI;

        double minX = cameraX - FALLING_SPAWN_AHEAD_LEFT_DISTANCE;
        double maxX = cameraX + SCREEN_WIDTH + FALLING_SPAWN_RIGHT_PADDING;

        double spawnX = randomRange(minX, maxX);

        double aboveDistance = randomRange(
                FALLING_SPAWN_MIN_HEIGHT_ABOVE_SCREEN,
                FALLING_SPAWN_MAX_HEIGHT_ABOVE_SCREEN
        );

        double spawnY = cameraY - aboveDistance;

        Entity object = spawn("street_falling_object", new SpawnData(spawnX, spawnY)
                .put("variant", variant.name()));

        Entity trigger = spawnFallingObjectTrigger(
                spawnX,
                spawnY,
                variant
        );

        StackPane warningIcon = createFallingWarningIcon();
        addUINode(warningIcon, 0, 0);

        FallingObjectInstance instance = new FallingObjectInstance(
                object,
                trigger,
                warningIcon,
                variant
        );

        fallingObjects.add(instance);
        updateFallingWarning(instance);
    }

    /**
     * 生成墜落物的即死 trigger。
     */
    private Entity spawnFallingObjectTrigger(
            double objectX,
            double objectY,
            FallingObjectVariant variant
    ) {
        double triggerWidth = variant.getWidth() * 0.86;
        double triggerHeight = variant.getHeight() * 0.86;

        return spawn("street_falling_object_trigger", new SpawnData(
                objectX + (variant.getWidth() - triggerWidth) / 2.0,
                objectY + (variant.getHeight() - triggerHeight) / 2.0
        )
                .put("width", triggerWidth)
                .put("height", triggerHeight)
                .put("deathReason", variant.getDeathReason()));
    }

    /**
     * 更新所有墜落物。
     */
    private void updateFallingObjects(double tpf) {
        double cameraX = getGameScene().getViewport().getX();
        double cameraY = getGameScene().getViewport().getY();

        fallingObjects.removeIf(instance -> {
            Entity object = instance.object();

            if (object == null || !object.isActive()) {
                removeFallingWarning(instance);
                removeFallingTrigger(instance);
                return true;
            }

            updateFallingTriggerPosition(instance);
            updateFallingWarning(instance);
            updateFallingStoppedTimer(instance, tpf);

            if (shouldRemoveFallingObject(instance, cameraX, cameraY)) {
                fadeOutAndRemoveFallingObject(instance);
                return true;
            }

            return false;
        });
    }

    /**
     * 更新墜落物停止計時。
     */
    private void updateFallingStoppedTimer(
            FallingObjectInstance instance,
            double tpf
    ) {
        PhysicsComponent physics =
                instance.object().getComponent(PhysicsComponent.class);

        double vx = Math.abs(physics.getVelocityX());
        double vy = Math.abs(physics.getVelocityY());

        boolean nearlyStopped = vx < 4 && vy < 4;

        if (nearlyStopped) {
            instance.setStoppedTimer(instance.stoppedTimer() + tpf);
        } else {
            instance.setStoppedTimer(0);
        }
    }

    /**
     * 判斷墜落物是否應該移除。
     */
    private boolean shouldRemoveFallingObject(
            FallingObjectInstance instance,
            double cameraX,
            double cameraY
    ) {
        Entity object = instance.object();

        boolean stoppedLongEnough =
                instance.stoppedTimer() >= 0.35;

        double objectScreenX = object.getX() - cameraX;
        double objectScreenY = object.getY() - cameraY;

        boolean outRight =
                objectScreenX > SCREEN_WIDTH + 260;

        boolean outBottom =
                objectScreenY > SCREEN_HEIGHT + 260;

        return stoppedLongEnough || outRight || outBottom;
    }

    /**
     * 讓墜落物淡出後移除。
     */
    private void fadeOutAndRemoveFallingObject(FallingObjectInstance instance) {
        if (instance.removing()) {
            return;
        }

        instance.setRemoving(true);

        removeFallingWarning(instance);
        removeFallingTrigger(instance);

        Entity object = instance.object();

        if (object == null || !object.isActive()) {
            return;
        }

        stopFallingObjectPhysics(object);

        if (object.getViewComponent().getChildren().isEmpty()) {
            object.removeFromWorld();
            return;
        }

        FadeTransition fade = new FadeTransition(
                Duration.seconds(0.35),
                object.getViewComponent().getChildren().get(0)
        );

        fade.setFromValue(1);
        fade.setToValue(0);

        fade.setOnFinished(e -> {
            if (object.isActive()) {
                object.removeFromWorld();
            }
        });

        fade.play();
    }

    private void stopFallingObjectPhysics(Entity object) {
        try {
            PhysicsComponent physics =
                    object.getComponent(PhysicsComponent.class);

            physics.setVelocityX(0);
            physics.setVelocityY(0);

        } catch (Exception ignored) {
        }
    }

    /**
     * 同步墜落物 trigger 位置與旋轉。
     */
    private void updateFallingTriggerPosition(FallingObjectInstance instance) {
        Entity object = instance.object();
        Entity trigger = instance.trigger();

        if (object == null || trigger == null || !trigger.isActive()) {
            return;
        }

        FallingObjectVariant variant = instance.variant();

        double triggerWidth = variant.getWidth() * 0.86;
        double triggerHeight = variant.getHeight() * 0.86;

        double centerX = object.getBoundingBoxComponent().getCenterWorld().getX();
        double centerY = object.getBoundingBoxComponent().getCenterWorld().getY();

        double triggerX = centerX - triggerWidth / 2.0;
        double triggerY = centerY - triggerHeight / 2.0;

        trigger.setPosition(triggerX, triggerY);
        trigger.setRotation(object.getRotation());
    }

    /**
     * 建立墜落物 warning icon。
     */
    private StackPane createFallingWarningIcon() {
        StackPane box = new StackPane();

        box.setPrefSize(FALLING_WARNING_ICON_SIZE, FALLING_WARNING_ICON_SIZE);
        box.setMinSize(FALLING_WARNING_ICON_SIZE, FALLING_WARNING_ICON_SIZE);
        box.setMaxSize(FALLING_WARNING_ICON_SIZE, FALLING_WARNING_ICON_SIZE);
        box.setMouseTransparent(true);

        ImageView view = new ImageView();

        try {
            var url = getClass().getResource("/assets/textures/scene2/UI/danger.png");

            if (url != null) {
                view.setImage(new Image(url.toExternalForm()));
            } else {
                System.out.println("Falling warning icon not found.");
            }

        } catch (Exception e) {
            System.out.println("Falling warning icon load failed.");
            e.printStackTrace();
        }

        view.setFitWidth(FALLING_WARNING_ICON_SIZE);
        view.setFitHeight(FALLING_WARNING_ICON_SIZE);
        view.setPreserveRatio(true);
        view.setSmooth(true);

        box.getChildren().add(view);

        return box;
    }

    /**
     * 更新墜落物 warning 位置與動畫。
     */
    private void updateFallingWarning(FallingObjectInstance instance) {
        if (instance.warningHidden()) {
            return;
        }

        Entity object = instance.object();
        StackPane warning = instance.warningIcon();

        if (object == null || warning == null) {
            return;
        }

        double cameraX = getGameScene().getViewport().getX();
        double cameraY = getGameScene().getViewport().getY();

        double objectCenterX =
                object.getBoundingBoxComponent().getCenterWorld().getX();

        double objectScreenX = objectCenterX - cameraX;
        double objectScreenY = object.getY() - cameraY;

        /*
         * 墜落物進入視窗後，warning 消失。
         */
        if (objectScreenY >= -object.getBoundingBoxComponent().getHeight()) {
            warning.setVisible(false);
            instance.setWarningHidden(true);
            return;
        }

        double warningX =
                objectScreenX - FALLING_WARNING_ICON_SIZE / 2.0;

        warning.setTranslateX(warningX);
        warning.setTranslateY(18);

        double distanceToTop = max(0, -objectScreenY);
        double progress = 1.0 - distanceToTop / FALLING_WARNING_DISTANCE;
        progress = max(0, min(1, progress));

        double scale = 0.65 + progress * 0.85;

        warning.setScaleX(scale);
        warning.setScaleY(scale);

        double pulse = Math.sin(progress * Math.PI * 10) * 0.12;
        warning.setOpacity(0.72 + progress * 0.28 + pulse);

        warning.setVisible(true);
    }

    private void removeFallingTrigger(FallingObjectInstance instance) {
        removeEntityIfActive(instance.trigger());
    }

    private void removeFallingWarning(FallingObjectInstance instance) {
        StackPane warning = instance.warningIcon();

        if (warning != null) {
            removeUINode(warning);
        }
    }

    /**
     * 清除所有墜落物。
     */
    private void cleanupFallingObjectsForRespawn() {
        for (FallingObjectInstance instance : fallingObjects) {
            removeFallingWarning(instance);
            removeFallingTrigger(instance);
            removeEntityIfActive(instance.object());
        }

        fallingObjects.clear();
    }

 
    // Far Background / Parallax 

    /**
     * 生成初始遠景背景。
     */
    private void spawnFarBackground() {
        farBackgrounds.clear();

        leftMostFarBaseX = -FAR_BACKGROUND_WIDTH;
        rightMostFarBaseX = FAR_BACKGROUND_WIDTH;

        spawnFarBackgroundAt(leftMostFarBaseX);
        spawnFarBackgroundAt(0);
        spawnFarBackgroundAt(rightMostFarBaseX);
    }

    /**
     * 在指定 baseX 生成遠景背景。
     */
    private void spawnFarBackgroundAt(double baseX) {
        Entity bg = spawn("street_far_background", new SpawnData(
                baseX,
                0 - (FAR_BACKGROUND_WIDTH - SCREEN_HEIGHT)
        )
                .put("width", FAR_BACKGROUND_WIDTH)
                .put("height", FAR_BACKGROUND_HEIGHT));

        farBackgrounds.add(new FarBackgroundSegment(baseX, bg));
    }

    /**
     * 更新遠景 parallax。
     */
    private void updateParallaxBackground() {
        double cameraX = getGameScene().getViewport().getX();

        /*
         * screenX = entityX - cameraX
         *
         * 希望遠景畫面位置：
         * screenX = baseX - cameraX * parallax
         *
         * 所以：
         * entityX = baseX + cameraX * (1 - parallax)
         */
        for (FarBackgroundSegment segment : farBackgrounds) {
            segment.entity().setX(
                    segment.baseX() + cameraX * (1.0 - FAR_BACKGROUND_PARALLAX)
            );

            segment.entity().setY(0);
        }

        generateMoreFarBackgroundIfNeeded();
        cleanupFarBackground();
    }

    /**
     * 若遠景快露出空白，往左右補新遠景。
     */
    private void generateMoreFarBackgroundIfNeeded() {
        double cameraX = getGameScene().getViewport().getX();

        double leftMostScreenX =
                leftMostFarBaseX - cameraX * FAR_BACKGROUND_PARALLAX;

        while (leftMostScreenX > -FAR_BACKGROUND_WIDTH) {
            leftMostFarBaseX -= FAR_BACKGROUND_WIDTH;
            spawnFarBackgroundAt(leftMostFarBaseX);

            leftMostScreenX =
                    leftMostFarBaseX - cameraX * FAR_BACKGROUND_PARALLAX;
        }

        double rightMostScreenX =
                rightMostFarBaseX - cameraX * FAR_BACKGROUND_PARALLAX;

        while (rightMostScreenX + FAR_BACKGROUND_WIDTH
                < SCREEN_WIDTH + FAR_BACKGROUND_WIDTH) {

            rightMostFarBaseX += FAR_BACKGROUND_WIDTH;
            spawnFarBackgroundAt(rightMostFarBaseX);

            rightMostScreenX =
                    rightMostFarBaseX - cameraX * FAR_BACKGROUND_PARALLAX;
        }
    }

    /**
     * 清除太遠的遠景背景。
     */
    private void cleanupFarBackground() {
        double cameraX = getGameScene().getViewport().getX();

        farBackgrounds.removeIf(segment -> {
            double screenX =
                    segment.baseX() - cameraX * FAR_BACKGROUND_PARALLAX;

            boolean tooFarRight =
                    screenX > SCREEN_WIDTH + FAR_BACKGROUND_WIDTH * 2;

            boolean tooFarLeft =
                    screenX + FAR_BACKGROUND_WIDTH < -FAR_BACKGROUND_WIDTH * 2;

            if (!tooFarRight && !tooFarLeft) {
                return false;
            }

            removeEntityIfActive(segment.entity());
            return true;
        });

        recalculateFarBackgroundBounds();
    }

    /**
     * 重新計算目前最左與最右遠景 baseX。
     */
    private void recalculateFarBackgroundBounds() {
        if (farBackgrounds.isEmpty()) {
            return;
        }

        leftMostFarBaseX = farBackgrounds.stream()
                .mapToDouble(FarBackgroundSegment::baseX)
                .min()
                .orElse(0);

        rightMostFarBaseX = farBackgrounds.stream()
                .mapToDouble(FarBackgroundSegment::baseX)
                .max()
                .orElse(0);
    }

 
    // Camera 

    /**
     * 設定攝影機。
     *
     * StreetScene 使用手動更新攝影機，不使用 bindToEntity。
     */
    private void setupCamera() {
        getGameScene().getViewport().setBounds(
                Integer.MIN_VALUE,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        );

        getGameScene().getViewport().unbind();
        getGameScene().getViewport().setLazy(false);

        getGameScene().getViewport().setBounds(
                -100000,
                0,
                (int)SCREEN_WIDTH,
                (int)SCREEN_HEIGHT
        );

        lockedCameraX = 0;

        getGameScene().getViewport().setX(lockedCameraX);
        getGameScene().getViewport().setY(0);
    }

    /**
     * 更新攝影機位置。
     */
    private void updateCamera() {
        double targetX = player.getX() - SCREEN_WIDTH / 2.0;

        double cameraX = min(MAX_CAMERA_X, targetX);
        cameraX = max(getMinCameraX(), cameraX);

        lockedCameraX = cameraX;

        getGameScene().getViewport().setX(lockedCameraX);
        getGameScene().getViewport().setY(0);
    }

    /**
     * 取得攝影機最左邊界。
     *
     * 還沒達到最大街道段數前，鏡頭可以繼續往左。
     * 達到上限後，鏡頭最左只能停在最後一段街道的最左側。
     */
    private double getMinCameraX() {
        if (!segmentLimitReached) {
            return -100000;
        }

        return leftMostGeneratedX;
    }

 
    // Save / Load - Segments 

    /**
     * 建立街道區塊存檔字串。
     *
     * 格式：
     * x,style,hasForeground;x,style,hasForeground
     */
    public String createSegmentSaveString() {
        StringBuilder builder = new StringBuilder();

        for (StreetSegment segment : segments) {
            if (!builder.isEmpty()) {
                builder.append(";");
            }

            builder.append(segment.x())
                    .append(",")
                    .append(segment.style().name())
                    .append(",")
                    .append(segment.hasForeground());
        }

        return builder.toString();
    }

    /**
     * 從存檔字串還原街道區塊。
     */
    public void restoreSegmentsFromSaveString(String data) {
        if (data == null || data.isBlank()) {
            return;
        }

        clearSegments();

        List<SavedStreetSegment> savedSegments = parseSavedSegments(data);

        /*
         * 從右到左生成，因為 generateSegmentAt() 會 segments.add(0, segment)。
         */
        savedSegments.sort((a, b) -> Double.compare(b.x(), a.x()));

        for (SavedStreetSegment saved : savedSegments) {
            generateSegmentAt(
                    saved.x(),
                    saved.style(),
                    saved.hasForeground()
            );
        }

        leftMostGeneratedX = savedSegments.stream()
                .mapToDouble(SavedStreetSegment::x)
                .min()
                .orElse(0);

        if (segments.size() >= MAX_SEGMENT_COUNT) {
            segmentLimitReached = true;
            spawnLeftBoundaryWallIfNeeded();
        }
    }

    /**
     * 解析街道區塊存檔字串。
     */
    private List<SavedStreetSegment> parseSavedSegments(String data) {
        List<SavedStreetSegment> savedSegments = new ArrayList<>();

        String[] segmentTokens = data.split(";");

        for (String token : segmentTokens) {
            String[] parts = token.split(",");

            if (parts.length < 3) {
                continue;
            }

            try {
                double x = Double.parseDouble(parts[0]);
                StreetApartmentStyle style =
                        StreetApartmentStyle.valueOf(parts[1]);

                boolean hasForeground =
                        Boolean.parseBoolean(parts[2]);

                savedSegments.add(new SavedStreetSegment(
                        x,
                        style,
                        hasForeground
                ));

            } catch (Exception e) {
                System.out.println("Invalid street segment save data: " + token);
            }
        }

        return savedSegments;
    }

 
    // Save / Load - Obstacles 

    /**
     * 建立障礙物存檔字串。
     *
     * 格式：
     * type,x;type,x
     */
    public String createObstacleSaveString() {
        StringBuilder builder = new StringBuilder();

        for (StreetObstacleGroup group : obstacleGroups) {
            if (!builder.isEmpty()) {
                builder.append(";");
            }

            builder.append(group.type().name())
                    .append(",")
                    .append(group.x());
        }

        return builder.toString();
    }

    /**
     * 從存檔字串還原障礙物。
     */
    public void restoreObstaclesFromSaveString(String data) {
        clearObstacles();

        if (data == null || data.isBlank()) {
            resetObstacleSpawnerAfterRestore();
            return;
        }

        String[] tokens = data.split(";");

        for (String token : tokens) {
            restoreOneObstacleFromToken(token);
        }

        resetObstacleSpawnerAfterRestore();
    }

    /**
     * 還原單一障礙物 token。
     */
    private void restoreOneObstacleFromToken(String token) {
        String[] parts = token.split(",");

        if (parts.length < 2) {
            return;
        }

        try {
            StreetObstacleType type =
                    StreetObstacleType.valueOf(parts[0]);

            double x = Double.parseDouble(parts[1]);

            switch (type) {
                case TRANSFORMER -> spawnTransformerAt(x);
                case RAISED_TILE -> spawnRaisedTileAt(x);
            }

        } catch (Exception e) {
            System.out.println("Invalid street obstacle save data: " + token);
        }
    }

 
    // Common Helpers 

    private double randomRange(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private void removeEntityIfActive(Entity entity) {
        if (entity != null && entity.isActive()) {
            entity.removeFromWorld();
        }
    }

 
    // Data Records / Inner Classes 

    private enum StreetObstacleType {
        TRANSFORMER,
        RAISED_TILE
    }

    private record FarBackgroundSegment(
            double baseX,
            Entity entity
    ) {
    }

    private record StreetSegment(
            double x,
            StreetApartmentStyle style,
            boolean hasForeground,
            Entity floorVisual,
            Entity apartment,
            Entity foreground
    ) {
    }

    private record SavedStreetSegment(
            double x,
            StreetApartmentStyle style,
            boolean hasForeground
    ) {
    }

    private record StreetObstacleGroup(
            StreetObstacleType type,
            double x,
            Entity visual,
            Entity colliderOrTrigger
    ) {
    }

    private record ScooterInstance(
            Entity visual,
            Entity hitbox,
            double velocityX
    ) {
    }

    /**
     * 墜落物 runtime 資料。
     *
     * 這裡不使用 record，因為 warningHidden、removing、stoppedTimer 會在更新時改變。
     */
    private static class FallingObjectInstance {

        private final Entity object;
        private final Entity trigger;
        private final StackPane warningIcon;
        private final FallingObjectVariant variant;

        private boolean warningHidden;
        private boolean removing;
        private double stoppedTimer;

        private FallingObjectInstance(
                Entity object,
                Entity trigger,
                StackPane warningIcon,
                FallingObjectVariant variant
        ) {
            this.object = object;
            this.trigger = trigger;
            this.warningIcon = warningIcon;
            this.variant = variant;
        }

        public Entity object() {
            return object;
        }

        public Entity trigger() {
            return trigger;
        }

        public StackPane warningIcon() {
            return warningIcon;
        }

        public FallingObjectVariant variant() {
            return variant;
        }

        public boolean warningHidden() {
            return warningHidden;
        }

        public void setWarningHidden(boolean warningHidden) {
            this.warningHidden = warningHidden;
        }

        public boolean removing() {
            return removing;
        }

        public void setRemoving(boolean removing) {
            this.removing = removing;
        }

        public double stoppedTimer() {
            return stoppedTimer;
        }

        public void setStoppedTimer(double stoppedTimer) {
            this.stoppedTimer = stoppedTimer;
        }
    }
}
