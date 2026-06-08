package ass.example.scenes;

import ass.example.core.DeathReason;
import ass.example.core.StreetScene.FallingObjectVariant;
import ass.example.core.StreetScene.StreetApartmentStyle;
import ass.example.scenes.system.SceneConfig;
import ass.example.system.StreetEndlessRecordSystem;
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
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * StreetEndlessScene
 *
 * 街道無盡模式場景。
 *
 * 功能：
 * 1. 生成無盡街道地圖。
 * 2. 生成遠景視差背景。
 * 3. 生成地板視覺與長地板碰撞箱。
 * 4. 生成隨機公寓背景。
 * 5. 生成街道障礙物。
 * 6. 生成左右來車與警告 UI。
 * 7. 生成掉落物與掉落警告 UI。
 * 8. 鎖定攝影機只能往左推進。
 * 9. 計算本局跑步距離與最佳距離。
 * 10. 清理本場景所有 runtime Entity 與 UI。
 */
public class StreetEndlessScene {

    // =========================================================
    // Scene / View Constants
    // =========================================================

    private static final double SCREEN_WIDTH = 1280;
    private static final double SCREEN_HEIGHT = 720;

    private static final double SEGMENT_WIDTH = 640;

    private static final double FLOOR_Y = 694.0;
    private static final double FLOOR_VISUAL_HEIGHT = 70;

    private static final double MAX_CAMERA_X = 0.0;


    // =========================================================
    // Far Background Constants
    // =========================================================

    private static final double FAR_BACKGROUND_WIDTH = 1983;
    private static final double FAR_BACKGROUND_HEIGHT = 793;
    private static final double FAR_BACKGROUND_PARALLAX = 0.18;


    // =========================================================
    // Endless Floor Collider Constants
    // =========================================================

    private static final double ENDLESS_FLOOR_COLLIDER_WIDTH = 6000;
    private static final double ENDLESS_FLOOR_COLLIDER_HEIGHT = 70;


    // =========================================================
    // Obstacle Constants
    // =========================================================

    private static final double OBSTACLE_GENERATE_AHEAD_DISTANCE = 2600.0;
    private static final double OBSTACLE_CLEANUP_RIGHT_PADDING = 900.0;

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


    // =========================================================
    // Scooter Constants
    // =========================================================

    private static final double SCOOTER_WARNING_DURATION = 1.6;

    private static final double SCOOTER_MIN_INTERVAL = 1.0;
    private static final double SCOOTER_MAX_INTERVAL = 15.0;

    private static final double SCOOTER_WIDTH = 150;
    private static final double SCOOTER_HEIGHT = 72;

    private static final double SCOOTER_HITBOX_WIDTH = 140;
    private static final double SCOOTER_HITBOX_HEIGHT = 58;

    private static final double SCOOTER_HITBOX_OFFSET_X = 5.0;
    private static final double SCOOTER_HITBOX_OFFSET_Y = 8.0;

    private static final double SCOOTER_SPEED = 720.0;

    private static final double WARNING_ICON_SIZE = 72.0;


    // =========================================================
    // Falling Object Constants
    // =========================================================

    private static final double FALLING_OBJECT_MIN_INTERVAL = 3.8;
    private static final double FALLING_OBJECT_MAX_INTERVAL = 7.2;

    private static final double FALLING_SPAWN_AHEAD_LEFT_DISTANCE = 1500.0;
    private static final double FALLING_SPAWN_RIGHT_PADDING = 240.0;

    private static final double FALLING_SPAWN_MIN_HEIGHT_ABOVE_SCREEN = 260.0;
    private static final double FALLING_SPAWN_MAX_HEIGHT_ABOVE_SCREEN = 620.0;

    private static final double FALLING_WARNING_DISTANCE = 620.0;
    private static final double FALLING_WARNING_ICON_SIZE = 72.0;

    private static final double FALLING_TRIGGER_SCALE = 0.86;

    private static final double FALLING_STOP_SPEED_THRESHOLD = 4.0;
    private static final double FALLING_STOP_REMOVE_SECONDS = 0.35;

    private static final double FALLING_REMOVE_PADDING = 260.0;

    private static final double FALLING_FADE_OUT_SECONDS = 0.35;


    // =========================================================
    // UI Paths
    // =========================================================

    private static final String DANGER_ICON_PATH = "/assets/textures/Scene2/UI/danger.png";


    // =========================================================
    // Dependencies
    // =========================================================

    /**
     * 場景設定。
     */
    private final SceneConfig config;


    // =========================================================
    // Runtime References
    // =========================================================

    private Entity player;

    private Entity cameraRightWall;
    private Entity endlessFloorCollider;

    private Text distanceText;

    private StackPane leftWarningIcon;
    private StackPane rightWarningIcon;


    // =========================================================
    // Runtime State - Random / Camera / Distance
    // =========================================================

    private final Random random = new Random();

    private double lockedCameraX = 0.0;

    private double startPlayerX;
    private double currentRunDistance = 0.0;
    private double bestDistanceBeforeRun = 0.0;


    // =========================================================
    // Runtime State - Far Background
    // =========================================================

    private double leftMostFarBaseX = 0.0;
    private double rightMostFarBaseX = 0.0;

    private final List<FarBackgroundSegment> farBackgrounds = new ArrayList<>();


    // =========================================================
    // Runtime State - Segments
    // =========================================================

    /**
     * 已生成到最左邊的位置。
     *
     * 玩家往左跑，所以地圖往負 X 方向延伸。
     */
    private double leftMostGeneratedX = 0.0;

    private final List<StreetSegment> segments = new ArrayList<>();


    // =========================================================
    // Runtime State - Obstacles
    // =========================================================

    private final List<StreetObstacleGroup> obstacleGroups = new ArrayList<>();

    private double nextTransformerCheckX = -900.0;
    private double nextRaisedTileCheckX = -700.0;


    // =========================================================
    // Runtime State - Scooters
    // =========================================================

    private final List<ScooterInstance> scooters = new ArrayList<>();

    private double leftScooterTimer = 4.0;
    private double rightScooterTimer = 6.0;

    private boolean leftWarningActive = false;
    private boolean rightWarningActive = false;

    private double leftWarningTimer = 0.0;
    private double rightWarningTimer = 0.0;


    // =========================================================
    // Runtime State - Falling Objects
    // =========================================================

    private final List<FallingObjectInstance> fallingObjects = new ArrayList<>();

    private double fallingObjectTimer = 3.5;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立 Street Endless 場景。
     *
     * @param config 場景設定
     */
    public StreetEndlessScene(SceneConfig config) {
        this.config = config;
    }


    // =========================================================
    // Load
    // =========================================================

    /**
     * 載入 Street Endless 場景。
     *
     * 流程：
     * 1. 初始化 game vars。
     * 2. 生成遠景。
     * 3. 生成初始地圖區段。
     * 4. 生成長地板碰撞箱。
     * 5. 生成右側邊界。
     * 6. 生成玩家。
     * 7. 重置障礙物 / 機車 / 掉落物系統。
     * 8. 初始化距離紀錄。
     * 9. 建立距離 UI。
     * 10. 設定攝影機。
     *
     * @return 玩家 Entity
     */
    public Entity load() {
        setupGameVarsForLoad();

        spawnFarBackground();
        generateInitialSegments();
        spawnEndlessFloorCollider();
        spawnRightBoundary();

        player = spawn(
                "player",
                config.getPlayerStartX(),
                config.getPlayerStartY()
        );

        resetObstacleSpawner();

        createScooterWarningUI();
        resetScooterTimers();

        resetFallingObjectSystem();

        setupDistanceState();
        createDistanceUI();

        setupCamera();

        return player;
    }

    /**
     * 初始化 Street Endless 需要的 game vars。
     */
    private void setupGameVarsForLoad() {
        set("saveDisabled", true);
        set("achievementDisabled", true);
        set("playerDead", false);
        set("playerOnBedCollider", false);
    }

    /**
     * 初始化距離相關狀態。
     */
    private void setupDistanceState() {
        startPlayerX = player.getX();
        currentRunDistance = 0.0;

        bestDistanceBeforeRun = StreetEndlessRecordSystem
                .getInstance()
                .getBestDistance();

        set("streetRunDistance", 0.0);
        set("streetBestDistanceBeforeRun", bestDistanceBeforeRun);
        set("streetBestDistance", bestDistanceBeforeRun);
        set("streetNewRecord", false);
    }


    // =========================================================
    // Update
    // =========================================================

    /**
     * 每幀更新 Street Endless。
     *
     * @param tpf time per frame
     */
    public void onUpdate(double tpf) {
        if (player == null) {
            return;
        }

        if (getb("playerDead")) {
            updateCamera();
            return;
        }

        updateRunDistance();
        updateCamera();

        updateEndlessFloorCollider();
        updateCameraRightWall();
        updateParallaxBackground();

        generateMoreSegmentsIfNeeded();
        cleanupFarRightSegments();

        generateObstaclesIfNeeded();
        cleanupObstacles();

        updateScooterSystem(tpf);
        updateScooterWarningPosition();

        updateFallingObjectSystem(tpf);
    }

    /**
     * 死亡或重生時重設本場景 runtime 系統。
     *
     * Street Endless 目前沒有床平台，只需要清除 playerOnBedCollider。
     */
    public void resetRuntimeSystems() {
        set("playerOnBedCollider", false);
    }


    // =========================================================
    // Far Background
    // =========================================================

    /**
     * 生成初始遠景。
     *
     * 初始生成三段：
     * - 左側
     * - 中央
     * - 右側
     *
     * 避免一開始鏡頭左右露出空白。
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
     * 在指定 baseX 生成遠景。
     */
    private void spawnFarBackgroundAt(double baseX) {
        Entity background = spawn(
                "street_far_background",
                new SpawnData(baseX, 0)
                        .put("width", FAR_BACKGROUND_WIDTH)
                        .put("height", FAR_BACKGROUND_HEIGHT)
        );

        farBackgrounds.add(
                new FarBackgroundSegment(baseX, background)
        );
    }

    /**
     * 更新遠景視差。
     */
    private void updateParallaxBackground() {
        double cameraX = getGameScene().getViewport().getX();

        for (FarBackgroundSegment segment : farBackgrounds) {
            double x = segment.baseX() +
                    cameraX * (1.0 - FAR_BACKGROUND_PARALLAX);

            segment.entity().setX(x);
            segment.entity().setY(0);
        }

        generateMoreFarBackgroundIfNeeded();
        cleanupFarBackground();
    }

    /**
     * 若鏡頭左側或右側快露出空白，補上新的遠景。
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

        while (rightMostScreenX + FAR_BACKGROUND_WIDTH <
                SCREEN_WIDTH + FAR_BACKGROUND_WIDTH) {
            rightMostFarBaseX += FAR_BACKGROUND_WIDTH;
            spawnFarBackgroundAt(rightMostFarBaseX);

            rightMostScreenX =
                    rightMostFarBaseX - cameraX * FAR_BACKGROUND_PARALLAX;
        }
    }

    /**
     * 清除離鏡頭太遠的遠景。
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

            segment.entity().removeFromWorld();
            return true;
        });

        refreshFarBackgroundBounds();
    }

    /**
     * 重新計算目前最左 / 最右遠景 baseX。
     */
    private void refreshFarBackgroundBounds() {
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


    // =========================================================
    // Street Segments
    // =========================================================

    /**
     * 生成初始街道區段。
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
     * 玩家接近目前最左生成位置時，繼續往左生成區段。
     */
    private void generateMoreSegmentsIfNeeded() {
        double playerX = player.getX();

        while (playerX - leftMostGeneratedX < SEGMENT_WIDTH * 3.0) {
            leftMostGeneratedX -= SEGMENT_WIDTH;
            generateRandomSegmentToLeft();
        }
    }

    /**
     * 往左生成隨機相容的街道區段。
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
     * 根據右側鄰居挑選可銜接的左側區段樣式。
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
     * 在指定 X 生成一段街道。
     */
    private void generateSegmentAt(
            double x,
            StreetApartmentStyle style
    ) {
        Entity floorVisual = spawn(
                "street_floor",
                new SpawnData(x, FLOOR_Y)
                        .put("width", SEGMENT_WIDTH)
                        .put("height", FLOOR_VISUAL_HEIGHT)
        );

        Entity apartment = null;
        Entity foreground = null;

        if (style.isVisibleApartment()) {
            apartment = spawn(
                    "street_apartment_bg",
                    new SpawnData(x, 150)
                            .put("width", SEGMENT_WIDTH)
                            .put("height", 544.0)
                            .put("style", style.name())
            );

            if (random.nextBoolean()) {
                foreground = spawn(
                        "street_apartment_fg",
                        new SpawnData(x, 150)
                                .put("width", SEGMENT_WIDTH)
                                .put("height", 544.0)
                                .put("style", style.name())
                );
            }
        }

        segments.add(
                0,
                new StreetSegment(
                        x,
                        style,
                        floorVisual,
                        apartment,
                        foreground
                )
        );
    }

    /**
     * 清除鏡頭右側太遠的舊區段。
     */
    private void cleanupFarRightSegments() {
        double cameraX = getGameScene().getViewport().getX();
        double removeRightX = cameraX + SCREEN_WIDTH + SEGMENT_WIDTH * 3;

        segments.removeIf(segment -> {
            if (segment.x() < removeRightX) {
                return false;
            }

            removeEntity(segment.floorVisual());
            removeEntity(segment.apartment());
            removeEntity(segment.foreground());

            return true;
        });
    }


    // =========================================================
    // Endless Floor Collider / Boundary
    // =========================================================

    /**
     * 生成超長地板碰撞箱。
     *
     * 使用單一長 floor 避免分段地板接縫卡住玩家。
     */
    private void spawnEndlessFloorCollider() {
        double x = lockedCameraX - ENDLESS_FLOOR_COLLIDER_WIDTH / 2.0;

        endlessFloorCollider = spawn(
                "floor",
                new SpawnData(x, FLOOR_Y)
                        .put("width", ENDLESS_FLOOR_COLLIDER_WIDTH)
                        .put("height", ENDLESS_FLOOR_COLLIDER_HEIGHT)
        );
    }

    /**
     * 讓長地板碰撞箱跟著鏡頭移動。
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
     * 生成右側邊界。
     */
    private void spawnRightBoundary() {
        spawn(
                "wall",
                new SpawnData(1278, 0)
                        .put("width", 40.0)
                        .put("height", SCREEN_HEIGHT)
        );

        cameraRightWall = spawn(
                "wall",
                new SpawnData(1278, 0)
                        .put("width", 40.0)
                        .put("height", SCREEN_HEIGHT)
        );
    }

    /**
     * 讓右側牆跟著鏡頭右側移動。
     */
    private void updateCameraRightWall() {
        if (cameraRightWall == null) {
            return;
        }

        double wallX = lockedCameraX + SCREEN_WIDTH - 8;

        PhysicsComponent physics =
                cameraRightWall.getComponent(PhysicsComponent.class);

        physics.overwritePosition(new Point2D(wallX, 0));
    }


    // =========================================================
    // Obstacles
    // =========================================================

    /**
     * 重置障礙物生成器。
     */
    private void resetObstacleSpawner() {
        obstacleGroups.clear();

        nextTransformerCheckX = config.getPlayerStartX() - 1000;
        nextRaisedTileCheckX = config.getPlayerStartX() - 750;
    }

    /**
     * 根據玩家位置預生成障礙物。
     */
    private void generateObstaclesIfNeeded() {
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
     * 嘗試在指定位置生成變電箱。
     */
    private void tryGenerateTransformerAt(double x) {
        if (random.nextDouble() > TRANSFORMER_SPAWN_CHANCE) {
            return;
        }

        double spawnX = x + randomRange(-80, 80);
        double visualY = FLOOR_Y - TRANSFORMER_HEIGHT;

        Entity visual = spawn(
                "street_transformer_box",
                new SpawnData(spawnX, visualY)
                        .put("width", TRANSFORMER_WIDTH)
                        .put("height", TRANSFORMER_HEIGHT)
        );

        double colliderX =
                spawnX + (TRANSFORMER_WIDTH - TRANSFORMER_COLLIDER_WIDTH) / 2.0;

        double colliderY =
                FLOOR_Y - TRANSFORMER_COLLIDER_HEIGHT;

        Entity collider = spawn(
                "floor",
                new SpawnData(colliderX, colliderY)
                        .put("width", TRANSFORMER_COLLIDER_WIDTH)
                        .put("height", TRANSFORMER_COLLIDER_HEIGHT)
        );

        obstacleGroups.add(
                new StreetObstacleGroup(
                        spawnX,
                        visual,
                        collider
                )
        );
    }

    /**
     * 嘗試在指定位置生成凸起磁磚。
     */
    private void tryGenerateRaisedTileAt(double x) {
        if (random.nextDouble() > RAISED_TILE_SPAWN_CHANCE) {
            return;
        }

        double spawnX = x + randomRange(-70, 70);
        double visualY = FLOOR_Y - RAISED_TILE_HEIGHT;

        Entity visual = spawn(
                "street_protruding_tile",
                new SpawnData(spawnX, visualY)
                        .put("width", RAISED_TILE_WIDTH)
                        .put("height", RAISED_TILE_HEIGHT)
        );

        Entity trigger = spawn(
                "death_zone",
                new SpawnData(spawnX, visualY)
                        .put("width", RAISED_TILE_WIDTH)
                        .put("height", RAISED_TILE_HEIGHT)
                        .put("deathReason", DeathReason.TRIPPED_BY_SIDEWALK_TILE)
        );

        obstacleGroups.add(
                new StreetObstacleGroup(
                        spawnX,
                        visual,
                        trigger
                )
        );
    }

    /**
     * 清除鏡頭右側太遠的障礙物。
     */
    private void cleanupObstacles() {
        double cameraX = getGameScene().getViewport().getX();

        double removeRightX =
                cameraX + SCREEN_WIDTH + OBSTACLE_CLEANUP_RIGHT_PADDING;

        obstacleGroups.removeIf(group -> {
            if (group.x() < removeRightX) {
                return false;
            }

            removeEntity(group.visual());
            removeEntity(group.colliderOrTrigger());

            return true;
        });
    }


    // =========================================================
    // Scooter System
    // =========================================================

    /**
     * 重置左右機車計時器與警告 UI。
     */
    private void resetScooterTimers() {
        leftScooterTimer = randomScooterInterval();
        rightScooterTimer = randomScooterInterval();

        leftWarningActive = false;
        rightWarningActive = false;

        leftWarningTimer = 0;
        rightWarningTimer = 0;

        setVisibleIfNotNull(leftWarningIcon, false);
        setVisibleIfNotNull(rightWarningIcon, false);
    }

    /**
     * 更新左右機車系統。
     */
    private void updateScooterSystem(double tpf) {
        updateScooterSideTimer(tpf, true);
        updateScooterSideTimer(tpf, false);

        updateScooters(tpf);
    }

    /**
     * 更新指定方向的機車生成倒數。
     *
     * @param fromLeft true 表示左側來車，false 表示右側來車
     */
    private void updateScooterSideTimer(
            double tpf,
            boolean fromLeft
    ) {
        if (fromLeft) {
            leftScooterTimer = updateSingleScooterSide(
                    tpf,
                    true,
                    leftScooterTimer,
                    leftWarningActive,
                    leftWarningTimer,
                    leftWarningIcon
            );

            return;
        }

        rightScooterTimer = updateSingleScooterSide(
                tpf,
                false,
                rightScooterTimer,
                rightWarningActive,
                rightWarningTimer,
                rightWarningIcon
        );
    }

    /**
     * 更新單側機車狀態。
     *
     * 由於 Java primitive 傳值不會回寫，
     * 這裡會在內部依照 fromLeft 更新對應欄位，
     * 並回傳新的 sideTimer。
     */
    private double updateSingleScooterSide(
            double tpf,
            boolean fromLeft,
            double sideTimer,
            boolean warningActive,
            double warningTimer,
            StackPane warningIcon
    ) {
        if (warningActive) {
            warningTimer -= tpf;
            updateWarningIcon(warningIcon, warningTimer);

            if (warningTimer <= 0) {
                warningActive = false;
                setVisibleIfNotNull(warningIcon, false);

                spawnScooter(fromLeft);
                sideTimer = randomScooterInterval();
            }

            setScooterWarningState(fromLeft, warningActive, warningTimer);
            return sideTimer;
        }

        sideTimer -= tpf;

        if (sideTimer <= 0) {
            warningActive = true;
            warningTimer = SCOOTER_WARNING_DURATION;

            setVisibleIfNotNull(warningIcon, true);
            updateWarningIcon(warningIcon, warningTimer);
        }

        setScooterWarningState(fromLeft, warningActive, warningTimer);
        return sideTimer;
    }

    /**
     * 回寫指定方向的 warning 狀態。
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
     * 生成機車與死亡 hitbox。
     */
    private void spawnScooter(boolean fromLeft) {
        double cameraX = getGameScene().getViewport().getX();

        double scooterY = getScooterY();

        double startX;
        double velocityX;

        if (fromLeft) {
            startX = cameraX - SCOOTER_WIDTH - 80;
            velocityX = SCOOTER_SPEED;
        } else {
            startX = cameraX + SCREEN_WIDTH + 80;
            velocityX = -SCOOTER_SPEED;
        }

        Entity visual = spawn(
                "street_scooter",
                new SpawnData(startX, scooterY)
                        .put("width", SCOOTER_WIDTH)
                        .put("height", SCOOTER_HEIGHT)
                        .put("fromLeft", fromLeft)
        );

        Entity hitbox = spawn(
                "street_scooter_death_wall",
                new SpawnData(
                        startX + SCOOTER_HITBOX_OFFSET_X,
                        scooterY + SCOOTER_HITBOX_OFFSET_Y
                )
                        .put("width", SCOOTER_HITBOX_WIDTH)
                        .put("height", SCOOTER_HITBOX_HEIGHT)
                        .put("deathReason", DeathReason.HIT_BY_SCOOTER)
        );

        scooters.add(
                new ScooterInstance(
                        visual,
                        hitbox,
                        velocityX
                )
        );
    }

    /**
     * 更新所有機車位置並清除離開畫面的機車。
     */
    private void updateScooters(double tpf) {
        double cameraX = getGameScene().getViewport().getX();

        scooters.removeIf(scooter -> {
            double dx = scooter.velocityX() * tpf;

            scooter.visual().setX(scooter.visual().getX() + dx);
            scooter.hitbox().setX(scooter.hitbox().getX() + dx);

            boolean outLeft =
                    scooter.visual().getX() + SCOOTER_WIDTH < cameraX - 260;

            boolean outRight =
                    scooter.visual().getX() > cameraX + SCREEN_WIDTH + 260;

            if (!outLeft && !outRight) {
                return false;
            }

            removeEntity(scooter.visual());
            removeEntity(scooter.hitbox());

            return true;
        });
    }

    /**
     * 建立左右來車警告 UI。
     */
    private void createScooterWarningUI() {
        leftWarningIcon = createWarningIcon();
        rightWarningIcon = createWarningIcon();

        leftWarningIcon.setVisible(false);
        rightWarningIcon.setVisible(false);

        double warningY = getScooterWarningScreenY();

        addUINode(leftWarningIcon, 34, warningY);
        addUINode(
                rightWarningIcon,
                SCREEN_WIDTH - WARNING_ICON_SIZE - 34,
                warningY
        );
    }

    /**
     * 更新來車警告 UI 的 Y 位置。
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

    /**
     * 取得機車警告 UI 的螢幕 Y。
     */
    private double getScooterWarningScreenY() {
        return getScooterY() + SCOOTER_HEIGHT / 2.0 -
                WARNING_ICON_SIZE / 2.0 -
                getGameScene().getViewport().getY();
    }

    /**
     * 取得機車 Y 座標。
     */
    private double getScooterY() {
        return FLOOR_Y - SCOOTER_HEIGHT + 2;
    }

    /**
     * 取得隨機機車間隔。
     */
    private double randomScooterInterval() {
        return randomRange(SCOOTER_MIN_INTERVAL, SCOOTER_MAX_INTERVAL);
    }


    // =========================================================
    // Warning Icon
    // =========================================================

    /**
     * 建立通用危險警告 Icon。
     */
    private StackPane createWarningIcon() {
        StackPane box = new StackPane();

        box.setPrefSize(WARNING_ICON_SIZE, WARNING_ICON_SIZE);
        box.setMinSize(WARNING_ICON_SIZE, WARNING_ICON_SIZE);
        box.setMaxSize(WARNING_ICON_SIZE, WARNING_ICON_SIZE);
        box.setMouseTransparent(true);

        ImageView dangerView = new ImageView(loadDangerIcon());

        dangerView.setFitWidth(WARNING_ICON_SIZE);
        dangerView.setFitHeight(WARNING_ICON_SIZE);
        dangerView.setPreserveRatio(true);
        dangerView.setSmooth(true);
        dangerView.setEffect(new DropShadow(12, Color.BLACK));

        box.getChildren().add(dangerView);

        return box;
    }

    /**
     * 載入危險警告圖示。
     */
    private Image loadDangerIcon() {
        var url = getClass().getResource(DANGER_ICON_PATH);

        if (url == null) {
            System.out.println("Danger icon not found: " + DANGER_ICON_PATH);
            return null;
        }

        return new Image(url.toExternalForm());
    }

    /**
     * 更新倒數警告 Icon 的縮放與透明度。
     */
    private void updateWarningIcon(
            StackPane icon,
            double timer
    ) {
        if (icon == null) {
            return;
        }

        double progress =
                1.0 - timer / SCOOTER_WARNING_DURATION;

        progress = clamp01(progress);

        double scale = 0.75 + progress * 0.75;

        icon.setScaleX(scale);
        icon.setScaleY(scale);

        double pulse =
                Math.sin(progress * Math.PI * 8) * 0.12;

        icon.setOpacity(0.72 + progress * 0.28 + pulse);
    }


    // =========================================================
    // Falling Object System
    // =========================================================

    /**
     * 重置掉落物系統。
     */
    private void resetFallingObjectSystem() {
        fallingObjectTimer = randomRange(
                FALLING_OBJECT_MIN_INTERVAL,
                FALLING_OBJECT_MAX_INTERVAL
        );

        fallingObjects.clear();
    }

    /**
     * 更新掉落物系統。
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
     * 生成掉落物、死亡 trigger 與警告 UI。
     */
    private void spawnFallingObject() {
        double cameraX = getGameScene().getViewport().getX();
        double cameraY = getGameScene().getViewport().getY();

        FallingObjectVariant variant = randomFallingObjectVariant();

        double minX = cameraX - FALLING_SPAWN_AHEAD_LEFT_DISTANCE;
        double maxX = cameraX + SCREEN_WIDTH + FALLING_SPAWN_RIGHT_PADDING;

        double spawnX = randomRange(minX, maxX);

        double aboveDistance = randomRange(
                FALLING_SPAWN_MIN_HEIGHT_ABOVE_SCREEN,
                FALLING_SPAWN_MAX_HEIGHT_ABOVE_SCREEN
        );

        double spawnY = cameraY - aboveDistance;

        Entity object = spawn(
                "street_falling_object",
                new SpawnData(spawnX, spawnY)
                        .put("variant", variant.name())
        );

        Entity trigger = spawnFallingObjectTrigger(
                spawnX,
                spawnY,
                variant
        );

        StackPane warningIcon = createFallingWarningIcon();
        addUINode(warningIcon, 0, 0);

        FallingObjectInstance instance =
                new FallingObjectInstance(
                        object,
                        trigger,
                        warningIcon,
                        variant
                );

        fallingObjects.add(instance);

        updateFallingWarning(instance);
    }

    /**
     * 隨機選擇掉落物種類。
     */
    private FallingObjectVariant randomFallingObjectVariant() {
        return random.nextBoolean()
                ? FallingObjectVariant.FRIDGE
                : FallingObjectVariant.HELI;
    }

    /**
     * 生成掉落物死亡 trigger。
     */
    private Entity spawnFallingObjectTrigger(
            double objectX,
            double objectY,
            FallingObjectVariant variant
    ) {
        double triggerWidth = getFallingTriggerWidth(variant);
        double triggerHeight = getFallingTriggerHeight(variant);

        return spawn(
                "street_falling_object_trigger",
                new SpawnData(
                        objectX + (variant.getWidth() - triggerWidth) / 2.0,
                        objectY + (variant.getHeight() - triggerHeight) / 2.0
                )
                        .put("width", triggerWidth)
                        .put("height", triggerHeight)
                        .put("deathReason", variant.getDeathReason())
        );
    }

    /**
     * 更新所有掉落物。
     */
    private void updateFallingObjects(double tpf) {
        double cameraX = getGameScene().getViewport().getX();
        double cameraY = getGameScene().getViewport().getY();

        fallingObjects.removeIf(instance ->
                updateSingleFallingObject(
                        instance,
                        tpf,
                        cameraX,
                        cameraY
                )
        );
    }

    /**
     * 更新單一掉落物。
     *
     * @return true 表示應從清單移除
     */
    private boolean updateSingleFallingObject(
            FallingObjectInstance instance,
            double tpf,
            double cameraX,
            double cameraY
    ) {
        Entity object = instance.object();

        if (object == null || !object.isActive()) {
            removeFallingObjectRuntimeNodes(instance);
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
    }

    /**
     * 更新掉落物停止計時。
     */
    private void updateFallingStoppedTimer(
            FallingObjectInstance instance,
            double tpf
    ) {
        PhysicsComponent physics =
                instance.object().getComponent(PhysicsComponent.class);

        double vx = Math.abs(physics.getVelocityX());
        double vy = Math.abs(physics.getVelocityY());

        boolean nearlyStopped =
                vx < FALLING_STOP_SPEED_THRESHOLD &&
                        vy < FALLING_STOP_SPEED_THRESHOLD;

        if (nearlyStopped) {
            instance.setStoppedTimer(instance.stoppedTimer() + tpf);
        } else {
            instance.setStoppedTimer(0);
        }
    }

    /**
     * 判斷掉落物是否該移除。
     */
    private boolean shouldRemoveFallingObject(
            FallingObjectInstance instance,
            double cameraX,
            double cameraY
    ) {
        Entity object = instance.object();

        double objectScreenX = object.getX() - cameraX;
        double objectScreenY = object.getY() - cameraY;

        boolean stoppedLongEnough =
                instance.stoppedTimer() >= FALLING_STOP_REMOVE_SECONDS;

        boolean outRight =
                objectScreenX > SCREEN_WIDTH + FALLING_REMOVE_PADDING;

        boolean outBottom =
                objectScreenY > SCREEN_HEIGHT + FALLING_REMOVE_PADDING;

        return stoppedLongEnough || outRight || outBottom;
    }

    /**
     * 淡出並移除掉落物。
     */
    private void fadeOutAndRemoveFallingObject(
            FallingObjectInstance instance
    ) {
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
                Duration.seconds(FALLING_FADE_OUT_SECONDS),
                object.getViewComponent().getChildren().get(0)
        );

        fade.setFromValue(1);
        fade.setToValue(0);

        fade.setOnFinished(event -> {
            if (object.isActive()) {
                object.removeFromWorld();
            }
        });

        fade.play();
    }

    /**
     * 停止掉落物物理速度。
     */
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
     * 更新掉落物 trigger 位置與旋轉。
     */
    private void updateFallingTriggerPosition(
            FallingObjectInstance instance
    ) {
        Entity object = instance.object();
        Entity trigger = instance.trigger();

        if (object == null || trigger == null || !trigger.isActive()) {
            return;
        }

        FallingObjectVariant variant = instance.variant();

        double triggerWidth = getFallingTriggerWidth(variant);
        double triggerHeight = getFallingTriggerHeight(variant);

        double centerX =
                object.getBoundingBoxComponent().getCenterWorld().getX();

        double centerY =
                object.getBoundingBoxComponent().getCenterWorld().getY();

        trigger.setPosition(
                centerX - triggerWidth / 2.0,
                centerY - triggerHeight / 2.0
        );

        trigger.setRotation(object.getRotation());
    }

    /**
     * 建立掉落警告 UI。
     */
    private StackPane createFallingWarningIcon() {
        StackPane box = new StackPane();

        box.setPrefSize(
                FALLING_WARNING_ICON_SIZE,
                FALLING_WARNING_ICON_SIZE
        );
        box.setMinSize(
                FALLING_WARNING_ICON_SIZE,
                FALLING_WARNING_ICON_SIZE
        );
        box.setMaxSize(
                FALLING_WARNING_ICON_SIZE,
                FALLING_WARNING_ICON_SIZE
        );

        box.setMouseTransparent(true);

        ImageView view = new ImageView(loadDangerIcon());

        view.setFitWidth(FALLING_WARNING_ICON_SIZE);
        view.setFitHeight(FALLING_WARNING_ICON_SIZE);
        view.setPreserveRatio(true);
        view.setSmooth(true);

        box.getChildren().add(view);

        return box;
    }

    /**
     * 更新掉落物警告 UI。
     */
    private void updateFallingWarning(
            FallingObjectInstance instance
    ) {
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

        if (objectScreenY >= -object.getBoundingBoxComponent().getHeight()) {
            warning.setVisible(false);
            instance.setWarningHidden(true);
            return;
        }

        double warningX =
                objectScreenX - FALLING_WARNING_ICON_SIZE / 2.0;

        warning.setTranslateX(warningX);
        warning.setTranslateY(18);

        double distanceToTop = Math.max(0, -objectScreenY);

        double progress =
                1.0 - distanceToTop / FALLING_WARNING_DISTANCE;

        progress = clamp01(progress);

        double scale = 0.65 + progress * 0.85;

        warning.setScaleX(scale);
        warning.setScaleY(scale);

        double pulse =
                Math.sin(progress * Math.PI * 10) * 0.12;

        warning.setOpacity(0.72 + progress * 0.28 + pulse);
        warning.setVisible(true);
    }

    /**
     * 移除掉落物相關 runtime nodes。
     */
    private void removeFallingObjectRuntimeNodes(
            FallingObjectInstance instance
    ) {
        removeFallingWarning(instance);
        removeFallingTrigger(instance);
    }

    /**
     * 移除掉落警告 UI。
     */
    private void removeFallingWarning(
            FallingObjectInstance instance
    ) {
        StackPane warning = instance.warningIcon();

        if (warning != null) {
            removeUINode(warning);
        }
    }

    /**
     * 移除掉落物死亡 trigger。
     */
    private void removeFallingTrigger(
            FallingObjectInstance instance
    ) {
        removeEntity(instance.trigger());
    }

    private double getFallingTriggerWidth(FallingObjectVariant variant) {
        return variant.getWidth() * FALLING_TRIGGER_SCALE;
    }

    private double getFallingTriggerHeight(FallingObjectVariant variant) {
        return variant.getHeight() * FALLING_TRIGGER_SCALE;
    }


    // =========================================================
    // Distance UI
    // =========================================================

    /**
     * 建立距離 UI。
     */
    private void createDistanceUI() {
        distanceText = new Text();

        distanceText.setStyle("""
                -fx-font-size: 24px;
                -fx-fill: white;
                -fx-font-weight: bold;
                """);

        distanceText.setEffect(new DropShadow(6, Color.BLACK));

        addUINode(distanceText, 32, 42);

        updateDistanceUI();
    }

    /**
     * 更新本局跑步距離。
     */
    private void updateRunDistance() {
        double distance = startPlayerX - player.getX();

        if (distance < 0) {
            distance = 0;
        }

        currentRunDistance = Math.max(currentRunDistance, distance) / 60.0;

        set("streetRunDistance", currentRunDistance);

        updateDistanceUI();
    }

    /**
     * 更新距離文字。
     */
    private void updateDistanceUI() {
        if (distanceText == null) {
            return;
        }

        int current = (int) Math.floor(currentRunDistance);
        int best = (int) Math.floor(bestDistanceBeforeRun);

        if (best > 0) {
            distanceText.setText(current + " m Best: " + best + " m");
        } else {
            distanceText.setText(current + " m");
        }
    }


    // =========================================================
    // Camera
    // =========================================================

    /**
     * 設定無盡模式攝影機。
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
     * 更新攝影機。
     *
     * 玩家只能推進鏡頭往左；
     * 若玩家回頭往右，鏡頭不會倒退。
     */
    private void updateCamera() {
        double targetX = player.getX() - 900;
        double targetCameraX = min(MAX_CAMERA_X, targetX);

        lockedCameraX = min(lockedCameraX, targetCameraX);

        getGameScene().getViewport().setX(lockedCameraX);
        getGameScene().getViewport().setY(0);
    }


    // =========================================================
    // Cleanup
    // =========================================================

    /**
     * 清理 Street Endless 場景。
     *
     * 需要手動移除：
     * 1. 機車與 hitbox。
     * 2. 長地板。
     * 3. 警告 UI。
     * 4. 距離 UI。
     * 5. 障礙物。
     * 6. 掉落物與掉落警告。
     */
    public void cleanup() {
        cleanupScooters();
        cleanupEndlessFloor();
        cleanupWarningUI();
        cleanupDistanceUI();
        cleanupObstacleGroups();
        cleanupFallingObjects();
    }

    private void cleanupScooters() {
        for (ScooterInstance scooter : scooters) {
            removeEntity(scooter.visual());
            removeEntity(scooter.hitbox());
        }

        scooters.clear();
    }

    private void cleanupEndlessFloor() {
        removeEntity(endlessFloorCollider);
        endlessFloorCollider = null;
    }

    private void cleanupWarningUI() {
        removeUINodeIfNotNull(leftWarningIcon);
        removeUINodeIfNotNull(rightWarningIcon);

        leftWarningIcon = null;
        rightWarningIcon = null;
    }

    private void cleanupDistanceUI() {
        removeUINodeIfNotNull(distanceText);
        distanceText = null;
    }

    private void cleanupObstacleGroups() {
        for (StreetObstacleGroup group : obstacleGroups) {
            removeEntity(group.visual());
            removeEntity(group.colliderOrTrigger());
        }

        obstacleGroups.clear();
    }

    private void cleanupFallingObjects() {
        for (FallingObjectInstance instance : fallingObjects) {
            removeFallingWarning(instance);
            removeFallingTrigger(instance);
            removeEntity(instance.object());
        }

        fallingObjects.clear();
    }


    // =========================================================
    // Utility
    // =========================================================

    private double randomRange(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private double clamp01(double value) {
        return max(0, min(1, value));
    }

    private void setVisibleIfNotNull(
            StackPane node,
            boolean visible
    ) {
        if (node != null) {
            node.setVisible(visible);
        }
    }

    private void removeUINodeIfNotNull(javafx.scene.Node node) {
        if (node != null) {
            removeUINode(node);
        }
    }

    private void removeEntity(Entity entity) {
        if (entity != null && entity.isActive()) {
            entity.removeFromWorld();
        }
    }


    // =========================================================
    // Data Records
    // =========================================================

    private record FarBackgroundSegment(
            double baseX,
            Entity entity
    ) {
    }

    private record StreetSegment(
            double x,
            StreetApartmentStyle style,
            Entity floorVisual,
            Entity apartment,
            Entity foreground
    ) {
    }

    private record StreetObstacleGroup(
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


    // =========================================================
    // Mutable Data Class
    // =========================================================

    /**
     * FallingObjectInstance
     *
     * 掉落物 runtime 狀態。
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