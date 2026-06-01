package ass.example.scenes;

import ass.example.core.DeathReason;
import ass.example.core.StreetScene.FallingObjectVariant;
import ass.example.core.StreetScene.StreetApartmentStyle;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import ass.example.system.MusicSystem;
import ass.example.system.StreetEndlessRecordSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.animation.FadeTransition;
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

public class StreetEndlessScene {

    private final SceneConfig config;
    private final DeathSystem deathSystem;
    private final AudioSystem audioSystem;

    private Entity player;

    private Entity cameraRightWall;

    private Entity farBackground;

    private final Random random = new Random();

    private final double farBackgroundWidth = 1983;
    private final double farBackgroundHeight = 793;
    private final double farBackgroundParallax = 0.18;

    private double leftMostFarBaseX = 0;
    private double rightMostFarBaseX = 0;

    private final List<FarBackgroundSegment> farBackgrounds = new ArrayList<>();

    /*
     * 每一段地圖的寬度。
     * 地板、公寓背景、公寓前景都用同一個寬度。
     */
    private final double segmentWidth = 640;
    private final double screenWidth = 1280;
    private final double screenHeight = 720;

    /*
     * 地板碰撞箱 Y。
     */
    private final double floorY = 694;

    /*
     * 地板視覺一段一段生成，
     * 但地板碰撞箱用一條長 wall，避免每段接縫卡住。
     */
    private Entity endlessFloorCollider;

    private final double endlessFloorColliderWidth = 6000;
    private final double endlessFloorColliderHeight = 70;

    // =========================
    // Street Obstacles
    // =========================

    private final List<StreetObstacleGroup> obstacleGroups = new ArrayList<>();

    /*
     * 目前已經往左預先計算到哪裡。
     * 因為玩家往左跑，所以 X 會越來越小。
     */
    private double nextTransformerCheckX = -900;
    private double nextRaisedTileCheckX = -700;

    /*
     * 變電箱：比較大、比較少。
     */
    private final double transformerCheckMinDistance = 700;
    private final double transformerCheckMaxDistance = 1200;
    private final double transformerSpawnChance = 0.45;

    private final double transformerWidth = 90;
    private final double transformerHeight = 145;
    private final double transformerColliderWidth = 78;
    private final double transformerColliderHeight = 132;

    /*
     * 凸起磁磚：比較小、可以比較常見。
     */
    private final double raisedTileCheckMinDistance = 420;
    private final double raisedTileCheckMaxDistance = 760;
    private final double raisedTileSpawnChance = 0.65;

    private final double raisedTileWidth = 75;
    private final double raisedTileHeight = 28;

    /*
     * 物件預生成距離。
     * 玩家還沒到之前，先在左側遠處生成。
     */
    private final double obstacleGenerateAheadDistance = 2600;

    /*
     * 清除右側舊物件距離。
     */
    private final double obstacleCleanupRightPadding = 900;


    // =========================
    // Scooter
    // =========================

    private final List<ScooterInstance> scooters = new ArrayList<>();

    /*
     * 左右兩側獨立計時器。
     */
    private double leftScooterTimer = 4.0;
    private double rightScooterTimer = 6.0;

    /*
     * warning 狀態。
     */
    private boolean leftWarningActive = false;
    private boolean rightWarningActive = false;

    private double leftWarningTimer = 0;
    private double rightWarningTimer = 0;

    private final double scooterWarningDuration = 1.6;

    /*
     * timer 隨機範圍。
     * 你可以之後調整難度。
     */
    private final double scooterMinInterval = 1.0;
    private final double scooterMaxInterval = 15.0;

    /*
     * 摩托車大小與速度。
     */
    private final double scooterWidth = 150;
    private final double scooterHeight = 72;

    private final double scooterHitboxWidth = 140;
    private final double scooterHitboxHeight = 58;

    private final double scooterY = floorY - scooterHeight + 2;
    private final double scooterHitboxOffsetX = 5;
    private final double scooterHitboxOffsetY = 8;

    private final double scooterSpeed = 720;

    // =========================
    // Falling Objects
    // =========================

    private final List<FallingObjectInstance> fallingObjects = new ArrayList<>();

    private double fallingObjectTimer = 3.5;

    private final double fallingObjectMinInterval = 3.8;
    private final double fallingObjectMaxInterval = 7.2;

    /*
     * 生成範圍：
     * cameraX - 左側預生成距離 到 cameraX + 畫面寬度。
     * 因為玩家往左跑，所以 cameraX 左側是即將前進區域。
     */
    private final double fallingSpawnAheadLeftDistance = 1500;
    private final double fallingSpawnRightPadding = 240;

    /*
     * 生成高度：在視窗上方一段距離。
     */
    private final double fallingSpawnMinHeightAboveScreen = 260;
    private final double fallingSpawnMaxHeightAboveScreen = 620;

    /*
     * warning 計算用。
     * 物件距離畫面頂部還有這麼遠時，警告開始從小變大。
     */
    private final double fallingWarningDistance = 620;
    private final double fallingWarningIconSize = 72;

    /*
     * 警告 UI。
     */
    private StackPane leftWarningIcon;
    private StackPane rightWarningIcon;

    private final double warningIconSize = 72;

    /*
     * 已生成到最左邊的位置。
     * 因為玩家往左跑，所以地圖往負 X 方向延伸。
     */
    private double leftMostGeneratedX = 0;

    /*
     * 起始攝影機位置。
     * 不允許攝影機往右超過 0。
     */
    private final double maxCameraX = 0;
    private double lockedCameraX = 0;

    private final List<StreetSegment> segments = new ArrayList<>();

    private Text distanceText;

    private double startPlayerX;
    private double currentRunDistance = 0;
    private double bestDistanceBeforeRun = 0;

    public StreetEndlessScene(
            SceneConfig config,
            DeathSystem deathSystem,
            AudioSystem audioSystem
    ) {
        this.config = config;
        this.deathSystem = deathSystem;
        this.audioSystem = audioSystem;
    }

    public Entity load() {
        set("saveDisabled", true);
        set("achievementDisabled", true);
        set("playerDead", false);

        spawnFarBackground();

        generateInitialSegments();

        spawnEndlessFloorCollider();

        spawnRightBoundary();

        player = spawn("player", config.getPlayerStartX(), config.getPlayerStartY());

        resetObstacleSpawner();

        createScooterWarningUI();
        resetScooterTimers();

        resetFallingObjectSystem();

        startPlayerX = player.getX();
        bestDistanceBeforeRun = StreetEndlessRecordSystem.getInstance().getBestDistance();

        set("streetRunDistance", 0.0);
        set("streetBestDistanceBeforeRun", bestDistanceBeforeRun);
        set("streetBestDistance", bestDistanceBeforeRun);
        set("streetNewRecord", false);

        createDistanceUI();

        setupCamera();

        return player;
    }

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
        generateObstaclesIfNeeded();
        cleanupFarRightSegments();
        cleanupObstacles();
        updateScooterSystem(tpf);
        updateScooterWarningPosition();
        updateFallingObjectSystem(tpf);
    }

    public void resetRuntimeSystems() {
        set("playerOnBedCollider", false);
    }

    private void spawnFarBackground() {
        farBackgrounds.clear();

        /*
         * 先生成三段遠景：
         * -1600 ~ 0
         * 0 ~ 1600
         * 1600 ~ 3200
         *
         * 這樣一開始畫面左右都有緩衝。
         */
        leftMostFarBaseX = -farBackgroundWidth;
        rightMostFarBaseX = farBackgroundWidth;

        spawnFarBackgroundAt(leftMostFarBaseX);
        spawnFarBackgroundAt(0);
        spawnFarBackgroundAt(rightMostFarBaseX);
    }

    private void spawnFarBackgroundAt(double baseX) {
        Entity bg = spawn("street_far_background", new SpawnData(baseX, 0)
                .put("width", farBackgroundWidth)
                .put("height", farBackgroundHeight));

        farBackgrounds.add(new FarBackgroundSegment(baseX, bg));
    }

    private void spawnEndlessFloorCollider() {
        /*
         * 初始放在畫面附近，之後會跟著 lockedCameraX 往左移動。
         * 它很長，所以玩家永遠踩在同一個 floor 上，不會遇到分段接縫。
         */
        double x = lockedCameraX - endlessFloorColliderWidth / 2.0;

            endlessFloorCollider = spawn("floor", new SpawnData(x, floorY)
                .put("width", endlessFloorColliderWidth)
                .put("height", endlessFloorColliderHeight));
    }

    private void updateEndlessFloorCollider() {
        if (endlessFloorCollider == null) {
            return;
        }

        /*
         * 讓長地板碰撞箱永遠覆蓋目前鏡頭左右很大範圍。
         * 因為它是單一 wall，所以沒有接縫。
         */
        double x = lockedCameraX - endlessFloorColliderWidth / 2.0;

        PhysicsComponent physics = endlessFloorCollider.getComponent(PhysicsComponent.class);
        physics.overwritePosition(new javafx.geometry.Point2D(x, floorY));
    }

    private void generateInitialSegments() {
        leftMostGeneratedX = 0;

        /*
         * 從右往左建立，這樣 adjacency 可以逐段判斷。
         */
        generateSegmentAt(640, StreetApartmentStyle.RIGHT);
        generateSegmentAt(0, StreetApartmentStyle.FILL);

        leftMostGeneratedX = -segmentWidth;
        generateRandomSegmentToLeft();

        leftMostGeneratedX = -segmentWidth * 2;
        generateRandomSegmentToLeft();
    }

    private void generateMoreSegmentsIfNeeded() {
        if (player == null) {
            return;
        }

        double playerX = player.getX();

        /*
         * 玩家越往左，playerX 越小。
         * 當玩家接近目前最左邊已生成區域，就繼續往左生成。
         */
        while (playerX - leftMostGeneratedX < segmentWidth * 3.0) {
            leftMostGeneratedX -= segmentWidth;
            generateRandomSegmentToLeft();
        }
    }

    private void generateRandomSegmentToLeft() {
        StreetApartmentStyle rightNeighborStyle = segments.isEmpty()
                ? StreetApartmentStyle.FILL
                : segments.get(0).style();

        StreetApartmentStyle style = randomCompatibleStyleForLeftOf(rightNeighborStyle);

        generateSegmentAt(leftMostGeneratedX, style);
    }

    private StreetApartmentStyle randomCompatibleStyleForLeftOf(StreetApartmentStyle rightNeighbor) {
        List<StreetApartmentStyle> candidates = new ArrayList<>();

        for (StreetApartmentStyle style : StreetApartmentStyle.values()) {
            /*
             * 新生成的區塊在左邊。
             * 它的右側是否銜接，必須符合右邊鄰居的左側是否銜接。
             */
            if (style.connectsRight() == rightNeighbor.connectsLeft()) {
                candidates.add(style);
            }
        }

        if (candidates.isEmpty()) {
            return StreetApartmentStyle.CENTER;
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    private void resetObstacleSpawner() {
        obstacleGroups.clear();

        /*
         * 一開始不要太靠近玩家，避免出生附近直接卡住。
         */
        nextTransformerCheckX = config.getPlayerStartX() - 1000;
        nextRaisedTileCheckX = config.getPlayerStartX() - 750;
    }

    private void generateObstaclesIfNeeded() {
        if (player == null) {
            return;
        }

        /*
         * 玩家往左跑，所以 playerX 會越來越小。
         * 我們要提前在玩家左側更遠處生成障礙物。
         */
        double generateUntilX = player.getX() - obstacleGenerateAheadDistance;

        while (nextTransformerCheckX > generateUntilX) {
            tryGenerateTransformerAt(nextTransformerCheckX);
            nextTransformerCheckX -= randomRange(
                    transformerCheckMinDistance,
                    transformerCheckMaxDistance
            );
        }

        while (nextRaisedTileCheckX > generateUntilX) {
            tryGenerateRaisedTileAt(nextRaisedTileCheckX);
            nextRaisedTileCheckX -= randomRange(
                    raisedTileCheckMinDistance,
                    raisedTileCheckMaxDistance
            );
        }
    }

    private double randomRange(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private void tryGenerateTransformerAt(double x) {
        if (random.nextDouble() > transformerSpawnChance) {
            return;
        }

        /*
         * 稍微加一點隨機偏移，避免每次剛好在檢查點。
         */
        double spawnX = x + randomRange(-80, 80);
        double visualY = floorY - transformerHeight;

        Entity visual = spawn("street_transformer_box", new SpawnData(spawnX, visualY)
                .put("width", transformerWidth)
                .put("height", transformerHeight));

        double colliderX = spawnX + (transformerWidth - transformerColliderWidth) / 2.0;
        double colliderY = floorY - transformerColliderHeight;

        Entity collider = spawn("floor", new SpawnData(colliderX, colliderY)
                .put("width", transformerColliderWidth)
                .put("height", transformerColliderHeight));

        obstacleGroups.add(new StreetObstacleGroup(
                spawnX,
                visual,
                collider
        ));
    }

    private void tryGenerateRaisedTileAt(double x) {
        if (random.nextDouble() > raisedTileSpawnChance) {
            return;
        }

        double spawnX = x + randomRange(-70, 70);
        double visualY = floorY - raisedTileHeight;

        Entity visual = spawn("street_protruding_tile", new SpawnData(spawnX, visualY)
                .put("width", raisedTileWidth)
                .put("height", raisedTileHeight));

        /*
         * 凸起磁磚不擋玩家，但碰到即死。
         * 若你只是想讓玩家撞到跌倒，也可以改成別的 DeathReason。
         */
        Entity trigger = spawn("death_zone", new SpawnData(spawnX, visualY)
                .put("width", raisedTileWidth)
                .put("height", raisedTileHeight)
                .put("deathReason", DeathReason.TRIPPED_BY_SIDEWALK_TILE));

        obstacleGroups.add(new StreetObstacleGroup(
                spawnX,
                visual,
                trigger
        ));
    }

    private void cleanupObstacles() {
        double cameraX = getGameScene().getViewport().getX();
        double removeRightX = cameraX + screenWidth + obstacleCleanupRightPadding;

        obstacleGroups.removeIf(group -> {
            if (group.x() < removeRightX) {
                return false;
            }

            if (group.visual() != null) {
                group.visual().removeFromWorld();
            }

            if (group.colliderOrTrigger() != null) {
                group.colliderOrTrigger().removeFromWorld();
            }

            return true;
        });
    }

    private record StreetObstacleGroup(
            double x,
            Entity visual,
            Entity colliderOrTrigger
    ) {
    }

    private void resetScooterTimers() {
        leftScooterTimer = randomScooterInterval();
        rightScooterTimer = randomScooterInterval();

        leftWarningActive = false;
        rightWarningActive = false;

        leftWarningTimer = 0;
        rightWarningTimer = 0;

        if (leftWarningIcon != null) {
            leftWarningIcon.setVisible(false);
        }

        if (rightWarningIcon != null) {
            rightWarningIcon.setVisible(false);
        }
    }

    private double randomScooterInterval() {
        return scooterMinInterval +
                random.nextDouble() * (scooterMaxInterval - scooterMinInterval);
    }

    private void updateScooterSystem(double tpf) {
        updateScooterSideTimer(tpf, true);
        updateScooterSideTimer(tpf, false);

        updateScooters(tpf);
    }

    private void updateScooterSideTimer(double tpf, boolean fromLeft) {
        if (fromLeft) {
            if (leftWarningActive) {
                leftWarningTimer -= tpf;
                updateWarningIcon(leftWarningIcon, leftWarningTimer);

                if (leftWarningTimer <= 0) {
                    leftWarningActive = false;
                    leftWarningIcon.setVisible(false);

                    spawnScooter(true);
                    leftScooterTimer = randomScooterInterval();
                }

                return;
            }

            leftScooterTimer -= tpf;

            if (leftScooterTimer <= 0) {
                leftWarningActive = true;
                leftWarningTimer = scooterWarningDuration;

                leftWarningIcon.setVisible(true);
                updateWarningIcon(leftWarningIcon, leftWarningTimer);
            }

            return;
        }

        /*
         * fromRight
         */
        if (rightWarningActive) {
            rightWarningTimer -= tpf;
            updateWarningIcon(rightWarningIcon, rightWarningTimer);

            if (rightWarningTimer <= 0) {
                rightWarningActive = false;
                rightWarningIcon.setVisible(false);

                spawnScooter(false);
                rightScooterTimer = randomScooterInterval();
            }

            return;
        }

        rightScooterTimer -= tpf;

        if (rightScooterTimer <= 0) {
            rightWarningActive = true;
            rightWarningTimer = scooterWarningDuration;

            rightWarningIcon.setVisible(true);
            updateWarningIcon(rightWarningIcon, rightWarningTimer);
        }
    }

    private void spawnScooter(boolean fromLeft) {
        double cameraX = getGameScene().getViewport().getX();

        double startX;
        double velocityX;

        if (fromLeft) {
            startX = cameraX - scooterWidth - 80;
            velocityX = scooterSpeed;
        } else {
            startX = cameraX + screenWidth + 80;
            velocityX = -scooterSpeed;
        }

        Entity visual = spawn("street_scooter", new SpawnData(startX, scooterY)
                .put("width", scooterWidth)
                .put("height", scooterHeight)
                .put("fromLeft", fromLeft));

        Entity hitbox = spawn("street_scooter_death_wall", new SpawnData(
                startX + scooterHitboxOffsetX,
                scooterY + scooterHitboxOffsetY
        )
                .put("width", scooterHitboxWidth)
                .put("height", scooterHitboxHeight)
                .put("deathReason", DeathReason.HIT_BY_SCOOTER));

        scooters.add(new ScooterInstance(
                visual,
                hitbox,
                velocityX
        ));
    }

    private void updateScooters(double tpf) {
        double cameraX = getGameScene().getViewport().getX();

        scooters.removeIf(scooter -> {
            double dx = scooter.velocityX() * tpf;

            Entity visual = scooter.visual();
            Entity hitbox = scooter.hitbox();

            visual.setX(visual.getX() + dx);
            hitbox.setX(hitbox.getX() + dx);

            boolean outLeft = visual.getX() + scooterWidth < cameraX - 260;
            boolean outRight = visual.getX() > cameraX + screenWidth + 260;

            if (!outLeft && !outRight) {
                return false;
            }

            visual.removeFromWorld();
            hitbox.removeFromWorld();

            return true;
        });
    }

    private record ScooterInstance(
            Entity visual,
            Entity hitbox,
            double velocityX
    ) {
    }

    private double getScooterWarningScreenY() {
        return scooterY + scooterHeight / 2.0 - warningIconSize / 2.0
                - getGameScene().getViewport().getY();
    }

    private void createScooterWarningUI() {
        leftWarningIcon = createWarningIcon(true);
        rightWarningIcon = createWarningIcon(false);

        leftWarningIcon.setVisible(false);
        rightWarningIcon.setVisible(false);

        double warningY = getScooterWarningScreenY();

        addUINode(leftWarningIcon, 34, warningY);
        addUINode(rightWarningIcon, screenWidth - warningIconSize - 34, warningY);
    }

    private StackPane createWarningIcon(boolean fromLeft) {
        StackPane box = new StackPane();
        box.setPrefSize(warningIconSize, warningIconSize);
        box.setMinSize(warningIconSize, warningIconSize);
        box.setMaxSize(warningIconSize, warningIconSize);
        box.setMouseTransparent(true);

        ImageView dangerView = new ImageView();

        try {
            var url = getClass().getResource("/assets/textures/Scene2/UI/danger.png");

            if (url != null) {
                Image image = new Image(url.toExternalForm());
                dangerView.setImage(image);
            } else {
                System.out.println("Danger icon not found: /assets/textures/Scene2/UI/danger.png");
            }

        } catch (Exception e) {
            System.out.println("Danger icon load failed.");
            e.printStackTrace();
        }

        dangerView.setFitWidth(warningIconSize);
        dangerView.setFitHeight(warningIconSize);
        dangerView.setPreserveRatio(true);
        dangerView.setSmooth(true);

        dangerView.setEffect(new DropShadow(12, Color.BLACK));

        box.getChildren().add(dangerView);

        return box;
    }

    private void updateWarningIcon(StackPane icon, double timer) {
        if (icon == null) {
            return;
        }

        /*
         * timer 從 duration 倒數到 0。
         * progress 越接近 1，表示摩托車越快出現。
         */
        double progress = 1.0 - timer / scooterWarningDuration;
        progress = Math.max(0, Math.min(1, progress));

        double scale = 0.75 + progress * 0.75;

        icon.setScaleX(scale);
        icon.setScaleY(scale);

        /*
         * 越接近越明顯，稍微閃爍。
         */
        double pulse = Math.sin(progress * Math.PI * 8) * 0.12;
        icon.setOpacity(0.72 + progress * 0.28 + pulse);
    }

    private void updateScooterWarningPosition() {
        double warningY = getScooterWarningScreenY();

        if (leftWarningIcon != null) {
            leftWarningIcon.setTranslateY(warningY);
        }

        if (rightWarningIcon != null) {
            rightWarningIcon.setTranslateY(warningY);
        }
    }

    private void resetFallingObjectSystem() {
        fallingObjectTimer = randomRange(
                fallingObjectMinInterval,
                fallingObjectMaxInterval
        );

        fallingObjects.clear();
    }

    private void spawnFallingObject() {
        double cameraX = getGameScene().getViewport().getX();
        double cameraY = getGameScene().getViewport().getY();

        FallingObjectVariant variant = random.nextBoolean()
                ? FallingObjectVariant.FRIDGE
                : FallingObjectVariant.HELI;

        /*
         * 可以生成在目前視窗，也可以生成在左側即將抵達的地方。
         */
        double minX = cameraX - fallingSpawnAheadLeftDistance;
        double maxX = cameraX + screenWidth + fallingSpawnRightPadding;

        double spawnX = randomRange(minX, maxX);

        double aboveDistance = randomRange(
                fallingSpawnMinHeightAboveScreen,
                fallingSpawnMaxHeightAboveScreen
        );

        double spawnY = cameraY - aboveDistance;

        Entity object = spawn("street_falling_object", new SpawnData(spawnX, spawnY)
                .put("variant", variant.name()));

        PhysicsComponent physics = object.getComponent(PhysicsComponent.class);
        physics.setVelocityX(randomRange(-35, 35));

        /*
         * 無物理即死 trigger。
         * 尺寸可以略小於外觀，手感比較合理。
         */
        double triggerWidth = variant.getWidth() * 0.86;
        double triggerHeight = variant.getHeight() * 0.86;

        Entity trigger = spawn("street_falling_object_trigger", new SpawnData(
                spawnX + (variant.getWidth() - triggerWidth) / 2.0,
                spawnY + (variant.getHeight() - triggerHeight) / 2.0
        )
                .put("width", triggerWidth)
                .put("height", triggerHeight)
                .put("deathReason", variant.getDeathReason()));

        StackPane warningIcon = createFallingWarningIcon();

        /*
         * 先放 0,0，下一行 updateOneFallingWarning() 會立刻更新位置。
         */
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

    private void updateFallingObjectSystem(double tpf) {
        fallingObjectTimer -= tpf;

        if (fallingObjectTimer <= 0) {
            spawnFallingObject();

            fallingObjectTimer = randomRange(
                    fallingObjectMinInterval,
                    fallingObjectMaxInterval
            );
        }

        updateFallingObjects(tpf);
    }

    private boolean shouldRemoveFallingObject(
            FallingObjectInstance instance,
            double cameraX,
            double cameraY
    ) {
        Entity object = instance.object();

        PhysicsComponent physics = object.getComponent(PhysicsComponent.class);

        double vx = Math.abs(physics.getVelocityX());
        double vy = Math.abs(physics.getVelocityY());

        double objectScreenX = object.getX() - cameraX;
        double objectScreenY = object.getY() - cameraY;

        boolean stoppedOnGround = vx < 4 && vy < 4 && object.getY() >= floorY - 120;

        boolean outRight = objectScreenX > screenWidth + 260;

        boolean outBottom = objectScreenY > screenHeight + 260;

        return stoppedOnGround || outRight || outBottom;
    }

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

        try {
            PhysicsComponent physics = object.getComponent(PhysicsComponent.class);
            physics.setVelocityX(0);
            physics.setVelocityY(0);
        } catch (Exception ignored) {
        }

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

    private void removeFallingTrigger(FallingObjectInstance instance) {
        Entity trigger = instance.trigger();

        if (trigger != null && trigger.isActive()) {
            trigger.removeFromWorld();
        }
    }

    private StackPane createFallingWarningIcon() {
        StackPane box = new StackPane();
        box.setPrefSize(fallingWarningIconSize, fallingWarningIconSize);
        box.setMinSize(fallingWarningIconSize, fallingWarningIconSize);
        box.setMaxSize(fallingWarningIconSize, fallingWarningIconSize);
        box.setMouseTransparent(true);

        ImageView view = new ImageView();

        try {
            var url = getClass().getResource("/assets/textures/Scene2/UI/danger.png");

            if (url != null) {
                view.setImage(new Image(url.toExternalForm()));
            } else {
                System.out.println("Falling warning icon not found.");
            }

        } catch (Exception e) {
            System.out.println("Falling warning icon load failed.");
            e.printStackTrace();
        }

        view.setFitWidth(fallingWarningIconSize);
        view.setFitHeight(fallingWarningIconSize);
        view.setPreserveRatio(true);
        view.setSmooth(true);

        box.getChildren().add(view);

        return box;
    }

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

            PhysicsComponent physics = object.getComponent(PhysicsComponent.class);

            double vx = Math.abs(physics.getVelocityX());
            double vy = Math.abs(physics.getVelocityY());

            boolean nearlyStopped = vx < 4 && vy < 4;

            if (nearlyStopped) {
                instance.setStoppedTimer(instance.stoppedTimer() + tpf);
            } else {
                instance.setStoppedTimer(0);
            }

            boolean stoppedLongEnough = instance.stoppedTimer() >= 0.35;

            double objectScreenX = object.getX() - cameraX;
            double objectScreenY = object.getY() - cameraY;

            boolean outRight = objectScreenX > screenWidth + 260;
            boolean outBottom = objectScreenY > screenHeight + 260;

            if (stoppedLongEnough || outRight || outBottom) {
                fadeOutAndRemoveFallingObject(instance);
                return true;
            }

            return false;
        });
    }

    private void updateFallingTriggerPosition(FallingObjectInstance instance) {
        Entity object = instance.object();
        Entity trigger = instance.trigger();

        if (object == null || trigger == null || !trigger.isActive()) {
            return;
        }

        FallingObjectVariant variant = instance.variant();

        double triggerWidth = variant.getWidth() * 0.86;
        double triggerHeight = variant.getHeight() * 0.86;

        /*
         * 取得墜落物目前世界中心。
         * 比起 object.getX() / getY() 更能處理旋轉與物理同步後的位置。
         */
        double centerX = object.getBoundingBoxComponent().getCenterWorld().getX();
        double centerY = object.getBoundingBoxComponent().getCenterWorld().getY();

        /*
         * 讓 trigger 中心對準墜落物中心。
         */
        double triggerX = centerX - triggerWidth / 2.0;
        double triggerY = centerY - triggerHeight / 2.0;

        trigger.setPosition(triggerX, triggerY);

        /*
         * 同步旋轉。
         * 如果你的 FXGL 版本 Entity 沒有 getRotation()，改用：
         * object.getTransformComponent().getRotation()
         */
        trigger.setRotation(object.getRotation());
    }

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

        double objectCenterX = object.getBoundingBoxComponent().getCenterWorld().getX();
        double objectScreenX = objectCenterX - cameraX;

        double objectScreenY = object.getY() - cameraY;

        /*
         * 墜落物進入視窗時，警告消失。
         */
        if (objectScreenY >= -object.getBoundingBoxComponent().getHeight()) {
            warning.setVisible(false);
            instance.setWarningHidden(true);
            return;
        }

        /*
         * 警告固定在視窗最上方邊緣，X 跟著墜落物中心。
         */
        double warningX = objectScreenX - fallingWarningIconSize / 2.0;

        warning.setTranslateX(warningX);
        warning.setTranslateY(18);

        /*
         * objectScreenY 是負數。
         * 距離頂部越近，distanceToTop 越小，progress 越大。
         */
        double distanceToTop = Math.max(0, -objectScreenY);
        double progress = 1.0 - distanceToTop / fallingWarningDistance;
        progress = Math.max(0, Math.min(1, progress));

        double scale = 0.65 + progress * 0.85;

        warning.setScaleX(scale);
        warning.setScaleY(scale);

        double pulse = Math.sin(progress * Math.PI * 10) * 0.12;
        warning.setOpacity(0.72 + progress * 0.28 + pulse);

        warning.setVisible(true);
    }

    private void removeFallingWarning(FallingObjectInstance instance) {
        StackPane warning = instance.warningIcon();

        if (warning != null) {
            removeUINode(warning);
        }
    }

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

    private void updateRunDistance() {
        if (player == null) {
            return;
        }

        /*
         * 往左跑時 player.getX() 會變小。
         * 距離 = 起始 X - 目前 X。
         *
         * 往右走不倒扣，所以用 max 保留目前本局最遠距離。
         */
        double distance = startPlayerX - player.getX();

        if (distance < 0) {
            distance = 0;
        }

        currentRunDistance = Math.max(currentRunDistance, distance) / 60;

        set("streetRunDistance", currentRunDistance);

        updateDistanceUI();
    }

    private void updateDistanceUI() {
        if (distanceText == null) {
            return;
        }

        int current = (int) Math.floor(currentRunDistance);
        int best = (int) Math.floor(bestDistanceBeforeRun);

        if (best > 0) {
            distanceText.setText(
                    current + " m " + "Best: " + best + " m"
            );
        } else {
            distanceText.setText(
                    current + " m"
            );
        }
    }

    private void generateSegmentAt(double x, StreetApartmentStyle style) {
        Entity floorVisual = spawn("street_floor", new SpawnData(x, floorY)
                .put("width", segmentWidth)
                .put("height", 70.0));

        Entity apartment = null;
        Entity foreground = null;

        if (style.isVisibleApartment()) {
            apartment = spawn("street_apartment_bg", new SpawnData(x, 150)
                    .put("width", segmentWidth)
                    .put("height", 544.0)
                    .put("style", style.name()));

            if (random.nextBoolean()) {
                foreground = spawn("street_apartment_fg", new SpawnData(x, 150)
                        .put("width", segmentWidth)
                        .put("height", 544.0)
                        .put("style", style.name()));
            }
        }

        StreetSegment segment = new StreetSegment(
                x,
                style,
                floorVisual,
                apartment,
                foreground
        );

        segments.add(0, segment);
    }

    private void cleanupFarRightSegments() {
        double cameraX = getGameScene().getViewport().getX();
        double removeRightX = cameraX + screenWidth + segmentWidth * 3;

        segments.removeIf(segment -> {
            if (segment.x() < removeRightX) {
                return false;
            }

            if (segment.floorVisual() != null) {
                segment.floorVisual().removeFromWorld();
            }

            if (segment.apartment() != null) {
                segment.apartment().removeFromWorld();
            }

            if (segment.foreground() != null) {
                segment.foreground().removeFromWorld();
            }

            return true;
        });
    }

    private void spawnRightBoundary() {
        /*
         * 原本的起始右邊牆。
         * 保留它，避免玩家一開始往右跑出起始畫面。
         */
        spawn("wall", new SpawnData(1278, 0)
                .put("width", 40.0)
                .put("height", 720.0));

        /*
         * 新增：跟著鏡頭右側移動的牆。
         * 當鏡頭往左推進後，玩家回頭往右時會撞到目前畫面右側，
         * 不會因 overwritePosition 產生瞬移感。
         */
        cameraRightWall = spawn("wall", new SpawnData(1278, 0)
                .put("width", 40.0)
                .put("height", 720.0));
    }

    private void setupCamera() {
        getGameScene().getViewport().setBounds(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        getGameScene().getViewport().unbind();
        getGameScene().getViewport().setLazy(false);

        getGameScene().getViewport().setBounds(
                -100000,
                0,
                1280,
                720
        );

        lockedCameraX = 0;

        getGameScene().getViewport().setX(lockedCameraX);
        getGameScene().getViewport().setY(0);
    }

    private void updateCamera() {
        /*
         * 玩家越往左，targetX 越小。
         * 但 cameraX 最大只能是 0，避免鏡頭往右超過起始畫面。
         */
        double targetX = player.getX() - 900;

        double targetCameraX = min(maxCameraX, targetX);

        /*
         * 關鍵：
         * cameraX 越小代表鏡頭越往左。
         * 所以只在 targetCameraX 比目前 lockedCameraX 更小時更新。
         * 玩家回頭往右時 targetCameraX 會變大，此時不更新，鏡頭固定。
         */
        lockedCameraX = min(lockedCameraX, targetCameraX);

        getGameScene().getViewport().setX(lockedCameraX);
        getGameScene().getViewport().setY(0);
    }

    private void updateCameraRightWall() {
        if (cameraRightWall == null) {
            return;
        }

        double wallX = lockedCameraX + screenWidth - 8;

        PhysicsComponent physics = cameraRightWall.getComponent(PhysicsComponent.class);
        physics.overwritePosition(new javafx.geometry.Point2D(wallX, 0));
    }

    private void updateParallaxBackground() {
        double cameraX = getGameScene().getViewport().getX();

        /*
         * 正確公式：
         *
         * screenX = entityX - cameraX
         *
         * 如果希望遠景畫面位置是：
         * screenX = baseX - cameraX * parallax
         *
         * 那 entityX 應該是：
         * entityX = baseX + cameraX * (1 - parallax)
         */
        for (FarBackgroundSegment segment : farBackgrounds) {
            segment.entity().setX(
                    segment.baseX() + cameraX * (1.0 - farBackgroundParallax)
            );
            segment.entity().setY(0);
        }

        generateMoreFarBackgroundIfNeeded();
        cleanupFarBackground();
    }

    private void generateMoreFarBackgroundIfNeeded() {
        double cameraX = getGameScene().getViewport().getX();

        /*
         * 用畫面座標判斷遠景是否快露出空白。
         *
         * screenX = baseX - cameraX * parallax
         */
        double leftMostScreenX = leftMostFarBaseX - cameraX * farBackgroundParallax;

        while (leftMostScreenX > -farBackgroundWidth) {
            leftMostFarBaseX -= farBackgroundWidth;
            spawnFarBackgroundAt(leftMostFarBaseX);

            leftMostScreenX = leftMostFarBaseX - cameraX * farBackgroundParallax;
        }

        double rightMostScreenX = rightMostFarBaseX - cameraX * farBackgroundParallax;

        while (rightMostScreenX + farBackgroundWidth < screenWidth + farBackgroundWidth) {
            rightMostFarBaseX += farBackgroundWidth;
            spawnFarBackgroundAt(rightMostFarBaseX);

            rightMostScreenX = rightMostFarBaseX - cameraX * farBackgroundParallax;
        }
    }

    private void cleanupFarBackground() {
        double cameraX = getGameScene().getViewport().getX();

        farBackgrounds.removeIf(segment -> {
            double screenX = segment.baseX() - cameraX * farBackgroundParallax;

            boolean tooFarRight = screenX > screenWidth + farBackgroundWidth * 2;
            boolean tooFarLeft = screenX + farBackgroundWidth < -farBackgroundWidth * 2;

            if (!tooFarRight && !tooFarLeft) {
                return false;
            }

            segment.entity().removeFromWorld();
            return true;
        });

        /*
         * 重新計算目前最左 / 最右 baseX。
         */
        if (!farBackgrounds.isEmpty()) {
            leftMostFarBaseX = farBackgrounds.stream()
                    .mapToDouble(FarBackgroundSegment::baseX)
                    .min()
                    .orElse(0);

            rightMostFarBaseX = farBackgrounds.stream()
                    .mapToDouble(FarBackgroundSegment::baseX)
                    .max()
                    .orElse(0);
        }
    }

    public void cleanup() {
        for (ScooterInstance scooter : scooters) {
            if (scooter.visual() != null) {
                scooter.visual().removeFromWorld();
            }

            if (scooter.hitbox() != null) {
                scooter.hitbox().removeFromWorld();
            }
        }

        scooters.clear();

        if (endlessFloorCollider != null) {
            endlessFloorCollider.removeFromWorld();
            endlessFloorCollider = null;
        }

        if (leftWarningIcon != null) {
            removeUINode(leftWarningIcon);
            leftWarningIcon = null;
        }

        if (rightWarningIcon != null) {
            removeUINode(rightWarningIcon);
            rightWarningIcon = null;
        }

        if (distanceText != null) {
            removeUINode(distanceText);
            distanceText = null;
        }

        for (StreetObstacleGroup group : obstacleGroups) {
            if (group.visual() != null) {
                group.visual().removeFromWorld();
            }

            if (group.colliderOrTrigger() != null) {
                group.colliderOrTrigger().removeFromWorld();
            }
        }

        obstacleGroups.clear();

        for (FallingObjectInstance instance : fallingObjects) {
            removeFallingWarning(instance);
            removeFallingTrigger(instance);

            if (instance.object() != null && instance.object().isActive()) {
                instance.object().removeFromWorld();
            }
        }

        fallingObjects.clear();
    }

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
}