package ass.example.ui;

import ass.example.core.DeathReason;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import ass.example.system.MusicSystem;
import ass.example.system.dialogue.DialogueSystem;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.almasb.fxgl.dsl.FXGL.addUINode;
import static com.almasb.fxgl.dsl.FXGL.removeUINode;

/**
 * MomBattleMiniGame。
 *
 * 功能：
 * 1. 播放逆時針過場。
 * 2. 顯示 Undertale 風格戰鬥畫面。
 * 3. 玩家控制 Heart 在白框內移動。
 * 4. 隨機產生橫向或縱向攻擊。
 * 5. Heart 碰到正式攻擊判定後死亡。
 * 6. 根據存活時間決定死亡原因。
 */
public class MomBattleMiniGame extends StackPane {
 
    // Screen Constants 

    private static final double SCREEN_WIDTH = 1280.0;
    private static final double SCREEN_HEIGHT = 720.0;

 
    // Asset Paths 

    private static final String MOM_BATTLE_IMAGE = "/assets/textures/characters/mom/mom_battle.png";

    private static final String HEART_IMAGE = "/assets/textures/scene1/UI/heart.png";

    private static final String HEART_DIE_GIF = "/assets/textures/scene1/UI/heart_die.gif";

    private static final String BATTLE_START_BGM = "/assets/music/dialogue/mombattlestart.mp3";

    private static final String BATTLE_LOOP_BGM = "/assets/music/dialogue/Dark Souls III OST 10 - Vordt of the Boreal Valley.mp3";

 
    // Arena Constants 

    /**
     * 戰鬥白框大小。
     *
     * 攻擊區域會用 3 x 3 方式切分這個白框。
     */
    private static final double ARENA_SIZE = 330.0;

    private static final double ARENA_X =
            (SCREEN_WIDTH - ARENA_SIZE) / 2.0;

    private static final double ARENA_Y = 360.0;

 
    // Heart Constants 

    /**
     * heart.png 原圖很大，但有效圖案在中心。
     *
     * 這裡保留大圖顯示，碰撞則使用較小 hitbox。
     */
    private static final double HEART_VIEW_SIZE = 520.0;

    /**
     * Heart 實際碰撞大小。
     */
    private static final double HEART_HITBOX_SIZE = 42.0;

    /**
     * 從 ImageView 左上角偏移到有效 Heart hitbox 的位置。
     */
    private static final double HEART_HITBOX_X_OFFSET = 239.0;
    private static final double HEART_HITBOX_Y_OFFSET = 232.0;

    private static final double HEART_SPEED = 250.0;

 
    // Transition Constants 

    private static final int TRANSITION_COLUMNS = 22;
    private static final int TRANSITION_ROWS = 12;

    private static final double TRANSITION_STEP_SECONDS = 0.010;
    private static final double TRANSITION_BLACK_HOLD_SECONDS = 0.25;

 
    // Attack Constants 

    private static final double ATTACK_INTERVAL_SECONDS = 1.15;

    private static final double WARNING_FLASH_SECONDS = 0.08;
    private static final double WARNING_HOLD_SECONDS = 0.30;

    private static final double AIM_LINE_WIDTH = 2.0;
    private static final double AIM_LINE_HOLD_SECONDS = 0.10;

    private static final double ATTACK_EXPAND_SECONDS = 0.06;
    private static final double ATTACK_ACTIVE_SECONDS = 0.48;
    private static final double ATTACK_FADE_SECONDS = 0.16;

    private static final String ACTIVE_ATTACK_TAG = "ACTIVE_ATTACK";

 
    // Death Thresholds 

    /**
     * 存活不到 60 秒：死亡 A。
     * 存活 60 到 120 秒：死亡 B。
     * 存活超過 120 秒：死亡 C。
     */
    private static final double DEATH_A_TIME_LIMIT = 60.0;
    private static final double DEATH_C_TIME_LIMIT = 120.0;

 
    // Systems 

    private final AudioSystem audioSystem =
            AudioSystem.getInstance();

    private final DeathSystem deathSystem =
            DeathSystem.getInstance();

    private final DialogueSystem dialogueSystem =
            DialogueSystem.getInstance();

    private final MusicSystem musicSystem =
            MusicSystem.getInstance();

 
    // Death Result 

    private final DeathReason deathA;
    private final DeathReason deathB;
    private final DeathReason deathC;

 
    // Runtime State 

    private final Random random = new Random();
    private final Set<KeyCode> pressedKeys = new HashSet<>();

    private boolean battleStarted = false;
    private boolean ended = false;

    private double heartX;
    private double heartY;

    private double aliveTime = 0.0;

 
    // Layers 

    private final Pane battleLayer = new Pane();
    private final Pane attackLayer = new Pane();
    private final Pane transitionLayer = new Pane();

 
    // UI Nodes 

    private ImageView momView;
    private ImageView heartView;

    private Rectangle arenaBorder;

 
    // Animations / Music 

    private AnimationTimer gameLoop;
    private Timeline attackLoop;

    private MediaPlayer startMusic;
    private MediaPlayer battleMusic;

 
    // Constructor 

    public MomBattleMiniGame(
            DeathReason deathA,
            DeathReason deathB,
            DeathReason deathC
    ) {
        this.deathA = deathA;
        this.deathB = deathB;
        this.deathC = deathC;

        setupRoot();
        setupLayers();
        setupKeyboardInput();
    }

 
    // Initial Setup 

    private void setupRoot() {
        setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        setMinSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        setMaxSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        setFocusTraversable(true);
        setPickOnBounds(true);

        setStyle("-fx-background-color: transparent;");
    }

    private void setupLayers() {
        battleLayer.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        attackLayer.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        transitionLayer.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        battleLayer.setVisible(false);
        attackLayer.setMouseTransparent(true);
        transitionLayer.setMouseTransparent(true);

        getChildren().addAll(
                battleLayer,
                transitionLayer
        );
    }

    private void setupKeyboardInput() {
        setOnKeyPressed(event ->
                pressedKeys.add(event.getCode())
        );

        setOnKeyReleased(event ->
                pressedKeys.remove(event.getCode())
        );
    }

 
    // Public API 

    /**
     * 開始媽媽戰鬥。
     */
    public void start() {
        requestFocus();

        /*
         * 停止對話 BGM，播戰鬥音樂。
         */
        musicSystem.stopBGM();

        playStartMusic();
        playEncounterTransition();
    }

    /**
     * 移除小遊戲時呼叫。
     */
    public void dispose() {
        ended = true;
        battleStarted = false;

        stopAttackLoop();
        stopGameLoop();
        stopOwnMusic();

        pressedKeys.clear();
        attackLayer.getChildren().clear();
        transitionLayer.getChildren().clear();
    }

 
    // Music 

    private void playStartMusic() {
        stopOwnMusic();

        startMusic = createMediaPlayer(BATTLE_START_BGM, false);

        if (startMusic == null) {
            playBattleLoopMusic();
            return;
        }

        startMusic.setOnEndOfMedia(this::playBattleLoopMusic);
        startMusic.setOnReady(() -> {
            applyOwnMusicVolume(startMusic);
            startMusic.play();
        });
    }

    private void playBattleLoopMusic() {
        stopBattleLoopMusic();

        battleMusic = createMediaPlayer(BATTLE_LOOP_BGM, true);

        if (battleMusic == null) {
            return;
        }

        battleMusic.setOnReady(() -> {
            applyOwnMusicVolume(battleMusic);
            battleMusic.play();
        });
    }

    private MediaPlayer createMediaPlayer(
            String path,
            boolean loop
    ) {
        try {
            URL url = getClass().getResource(path);

            if (url == null) {
                System.out.println("Music not found: " + path);
                return null;
            }

            MediaPlayer player = new MediaPlayer(
                    new Media(url.toExternalForm())
            );

            player.setCycleCount(
                    loop
                            ? MediaPlayer.INDEFINITE
                            : 1
            );

            return player;

        } catch (Exception exception) {
            System.out.println("Music load failed: " + path);
            exception.printStackTrace();
            return null;
        }
    }

    private void applyOwnMusicVolume(MediaPlayer player) {
        if (player != null) {
            player.setVolume(audioSystem.getEffectiveMusicVolume());
        }
    }

    private void stopOwnMusic() {
        stopStartMusic();
        stopBattleLoopMusic();
    }

    private void stopStartMusic() {
        if (startMusic == null) {
            return;
        }

        startMusic.stop();
        startMusic.dispose();
        startMusic = null;
    }

    private void stopBattleLoopMusic() {
        if (battleMusic == null) {
            return;
        }

        battleMusic.stop();
        battleMusic.dispose();
        battleMusic = null;
    }

 
    // Encounter Transition 

    /**
     * 播放由外向內的逆時針過場。
     */
    private void playEncounterTransition() {
        transitionLayer.getChildren().clear();

        double cellWidth = SCREEN_WIDTH / TRANSITION_COLUMNS;
        double cellHeight = SCREEN_HEIGHT / TRANSITION_ROWS;

        Canvas canvas = new Canvas(SCREEN_WIDTH, SCREEN_HEIGHT);
        GraphicsContext graphics = canvas.getGraphicsContext2D();

        transitionLayer.getChildren().add(canvas);

        List<GridCell> spiralOrder = createCounterClockwiseSpiralOrder(
                TRANSITION_ROWS,
                TRANSITION_COLUMNS
        );

        Timeline transition = new Timeline();

        double delay = 0.0;

        for (GridCell cell : spiralOrder) {
            transition.getKeyFrames().add(
                    new KeyFrame(
                            Duration.seconds(delay),
                            event -> drawBlackCell(
                                    graphics,
                                    cell,
                                    cellWidth,
                                    cellHeight
                            )
                    )
            );

            delay += TRANSITION_STEP_SECONDS;
        }

        transition.setOnFinished(event -> {
            setupBattleScreen();

            PauseTransition holdBlack = new PauseTransition(
                    Duration.seconds(TRANSITION_BLACK_HOLD_SECONDS)
            );

            holdBlack.setOnFinished(done -> {
                transitionLayer.getChildren().clear();
                startBattle();
            });

            holdBlack.play();
        });

        transition.play();
    }

    private void drawBlackCell(
            GraphicsContext graphics,
            GridCell cell,
            double cellWidth,
            double cellHeight
    ) {
        graphics.setFill(Color.BLACK);

        /*
         * +0.5 是為了補掉 Canvas 繪製時可能出現的細縫。
         */
        graphics.fillRect(
                cell.column() * cellWidth,
                cell.row() * cellHeight,
                cellWidth + 0.5,
                cellHeight + 0.5
        );
    }

    /**
     * 建立逆時針螺旋順序。
     *
     * 順序：
     * 1. 左邊往下
     * 2. 下邊往右
     * 3. 右邊往上
     * 4. 上邊往左
     */
    private List<GridCell> createCounterClockwiseSpiralOrder(
            int rows,
            int columns
    ) {
        List<GridCell> result = new ArrayList<>();

        int top = 0;
        int bottom = rows - 1;
        int left = 0;
        int right = columns - 1;

        while (top <= bottom && left <= right) {
            for (int row = top; row <= bottom; row++) {
                result.add(new GridCell(row, left));
            }
            left++;

            for (int column = left; column <= right; column++) {
                result.add(new GridCell(bottom, column));
            }
            bottom--;

            if (left <= right) {
                for (int row = bottom; row >= top; row--) {
                    result.add(new GridCell(row, right));
                }
                right--;
            }

            if (top <= bottom) {
                for (int column = right; column >= left; column--) {
                    result.add(new GridCell(top, column));
                }
                top++;
            }
        }

        return result;
    }

    private record GridCell(
            int row,
            int column
    ) {
    }

 
    // Battle Screen Setup 

    private void setupBattleScreen() {
        battleLayer.getChildren().clear();
        attackLayer.getChildren().clear();

        Rectangle background = createBattleBackground();
        StackPane momBox = createMomBox();

        arenaBorder = createArenaBorder();

        heartView = createHeartView();
        resetHeartToArenaCenter();

        battleLayer.getChildren().addAll(
                background,
                momBox,
                arenaBorder,
                attackLayer,
                heartView
        );

        battleLayer.setVisible(true);

        playMomAppearAnimation();
    }

    private Rectangle createBattleBackground() {
        Rectangle background = new Rectangle(
                SCREEN_WIDTH,
                SCREEN_HEIGHT
        );

        background.setFill(Color.BLACK);

        return background;
    }

    private StackPane createMomBox() {
        momView = new ImageView(loadImage(MOM_BATTLE_IMAGE));

        momView.setFitHeight(300);
        momView.setPreserveRatio(true);
        momView.setSmooth(false);

        momView.setOpacity(0);
        momView.setTranslateY(-30);
        momView.setScaleX(0.75);
        momView.setScaleY(0.75);

        StackPane momBox = new StackPane(momView);
        momBox.setPrefSize(SCREEN_WIDTH, 260);
        momBox.setLayoutX(0);
        momBox.setLayoutY(50);

        return momBox;
    }

    private Rectangle createArenaBorder() {
        Rectangle border = new Rectangle(
                ARENA_SIZE,
                ARENA_SIZE
        );

        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.WHITE);
        border.setStrokeWidth(4);

        border.setLayoutX(ARENA_X);
        border.setLayoutY(ARENA_Y);

        return border;
    }

    private ImageView createHeartView() {
        ImageView view = new ImageView(loadImage(HEART_IMAGE));

        view.setFitWidth(HEART_VIEW_SIZE);
        view.setFitHeight(HEART_VIEW_SIZE);
        view.setPreserveRatio(true);
        view.setSmooth(false);

        return view;
    }

    private void resetHeartToArenaCenter() {
        heartX =
                ARENA_X +
                        ARENA_SIZE / 2.0 -
                        HEART_VIEW_SIZE / 2.0;

        heartY =
                ARENA_Y +
                        ARENA_SIZE / 2.0 -
                        HEART_VIEW_SIZE / 2.0;

        updateHeartPosition();
    }

    private void playMomAppearAnimation() {
        FadeTransition fade = new FadeTransition(
                Duration.seconds(0.28),
                momView
        );
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition move = new TranslateTransition(
                Duration.seconds(0.28),
                momView
        );
        move.setFromY(-30);
        move.setToY(0);
        move.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(
                Duration.seconds(0.28),
                momView
        );
        scale.setFromX(0.75);
        scale.setFromY(0.75);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(
                fade,
                move,
                scale
        ).play();
    }

 
    // Battle Start / Loops 

    private void startBattle() {
        battleStarted = true;
        ended = false;
        aliveTime = 0.0;

        requestFocus();

        if (battleMusic == null &&
                (startMusic == null ||
                        startMusic.getStatus() != MediaPlayer.Status.PLAYING)) {
            playBattleLoopMusic();
        }

        startGameLoop();
        startAttackLoop();
    }

    private void startGameLoop() {
        stopGameLoop();

        gameLoop = new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (ended || !battleStarted) {
                    lastTime = now;
                    return;
                }

                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double tpf = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                updateBattle(tpf);
            }
        };

        gameLoop.start();
    }

    private void startAttackLoop() {
        stopAttackLoop();

        attackLoop = new Timeline(
                new KeyFrame(
                        Duration.seconds(ATTACK_INTERVAL_SECONDS),
                        event -> spawnRandomAttack()
                )
        );

        attackLoop.setCycleCount(Animation.INDEFINITE);
        attackLoop.play();
    }

    private void stopGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
        }
    }

    private void stopAttackLoop() {
        if (attackLoop != null) {
            attackLoop.stop();
            attackLoop = null;
        }
    }

 
    // Battle Update 

    private void updateBattle(double tpf) {
        aliveTime += tpf;

        updateHeartMovement(tpf);
        checkAttackCollision();
    }

    private void updateHeartMovement(double tpf) {
        double dx = 0.0;
        double dy = 0.0;

        if (isPressed(KeyCode.LEFT, KeyCode.A)) {
            dx -= 1.0;
        }

        if (isPressed(KeyCode.RIGHT, KeyCode.D)) {
            dx += 1.0;
        }

        if (isPressed(KeyCode.UP, KeyCode.W)) {
            dy -= 1.0;
        }

        if (isPressed(KeyCode.DOWN, KeyCode.S)) {
            dy += 1.0;
        }

        if (dx != 0 && dy != 0) {
            double inverseSqrtTwo = 1.0 / Math.sqrt(2.0);
            dx *= inverseSqrtTwo;
            dy *= inverseSqrtTwo;
        }

        heartX += dx * HEART_SPEED * tpf;
        heartY += dy * HEART_SPEED * tpf;

        clampHeartInsideArena();

        updateHeartPosition();
    }

    private boolean isPressed(KeyCode first, KeyCode second) {
        return pressedKeys.contains(first) ||
                pressedKeys.contains(second);
    }

    /**
     * 將 Heart 的有效 hitbox 限制在白框內。
     */
    private void clampHeartInsideArena() {
        double hitboxX = heartX + HEART_HITBOX_X_OFFSET;
        double hitboxY = heartY + HEART_HITBOX_Y_OFFSET;

        hitboxX = clamp(
                hitboxX,
                ARENA_X,
                ARENA_X + ARENA_SIZE - HEART_HITBOX_SIZE
        );

        hitboxY = clamp(
                hitboxY,
                ARENA_Y,
                ARENA_Y + ARENA_SIZE - HEART_HITBOX_SIZE
        );

        heartX = hitboxX - HEART_HITBOX_X_OFFSET;
        heartY = hitboxY - HEART_HITBOX_Y_OFFSET;
    }

    private void updateHeartPosition() {
        if (heartView == null) {
            return;
        }

        heartView.setLayoutX(heartX);
        heartView.setLayoutY(heartY);
    }

 
    // Attack Spawning 

    private void spawnRandomAttack() {
        if (ended) {
            return;
        }

        boolean rowAttack = random.nextBoolean();
        int index = random.nextInt(3);

        Rectangle attack = createAttackRectangle(rowAttack, index);

        AttackShape originalShape = AttackShape.from(attack);

        playAttackWarningSequence(
                attack,
                rowAttack,
                originalShape
        );
    }

    private Rectangle createAttackRectangle(
            boolean rowAttack,
            int index
    ) {
        double cellSize = ARENA_SIZE / 3.0;

        Rectangle attack;

        if (rowAttack) {
            attack = new Rectangle(SCREEN_WIDTH, cellSize);
            attack.setLayoutX(0);
            attack.setLayoutY(ARENA_Y + index * cellSize);
        } else {
            attack = new Rectangle(cellSize, SCREEN_HEIGHT);
            attack.setLayoutX(ARENA_X + index * cellSize);
            attack.setLayoutY(0);
        }

        attack.setMouseTransparent(true);
        attack.setOpacity(1.0);

        return attack;
    }

    private void playAttackWarningSequence(
            Rectangle attack,
            boolean rowAttack,
            AttackShape originalShape
    ) {
        attack.setFill(Color.rgb(255, 0, 0, 0.48));
        attackLayer.getChildren().add(attack);

        SequentialTransition warning = new SequentialTransition(
                createWarningFlash(attack, 0.25, 0.80),
                createWarningFlash(attack, 0.80, 0.25),
                createWarningFlash(attack, 0.25, 0.85),
                new PauseTransition(Duration.seconds(WARNING_HOLD_SECONDS))
        );

        warning.setOnFinished(event ->
                showAimLineBeforeAttack(
                        attack,
                        rowAttack,
                        originalShape
                )
        );

        warning.play();
    }

    private FadeTransition createWarningFlash(
            Rectangle attack,
            double fromOpacity,
            double toOpacity
    ) {
        FadeTransition flash = new FadeTransition(
                Duration.seconds(WARNING_FLASH_SECONDS),
                attack
        );

        flash.setFromValue(fromOpacity);
        flash.setToValue(toOpacity);

        return flash;
    }

    /**
     * 警告結束後，先將攻擊範圍縮成細線。
     *
     * 這段期間不算正式攻擊判定。
     */
    private void showAimLineBeforeAttack(
            Rectangle attack,
            boolean rowAttack,
            AttackShape originalShape
    ) {
        attack.setFill(Color.WHITE);
        attack.setOpacity(1.0);

        if (rowAttack) {
            attack.setHeight(AIM_LINE_WIDTH);
            attack.setLayoutY(
                    originalShape.y() +
                            (originalShape.height() - AIM_LINE_WIDTH) / 2.0
            );
        } else {
            attack.setWidth(AIM_LINE_WIDTH);
            attack.setLayoutX(
                    originalShape.x() +
                            (originalShape.width() - AIM_LINE_WIDTH) / 2.0
            );
        }

        PauseTransition lineHold = new PauseTransition(
                Duration.seconds(AIM_LINE_HOLD_SECONDS)
        );

        lineHold.setOnFinished(event ->
                expandAimLineIntoActiveAttack(
                        attack,
                        originalShape
                )
        );

        lineHold.play();
    }

    /**
     * 將細線快速擴展回完整攻擊範圍。
     *
     * 加入正式攻擊判定。
     */
    private void expandAimLineIntoActiveAttack(
            Rectangle attack,
            AttackShape originalShape
    ) {
        attack.setUserData(ACTIVE_ATTACK_TAG);

        Timeline expand = new Timeline(
                new KeyFrame(
                        Duration.seconds(ATTACK_EXPAND_SECONDS),
                        new KeyValue(attack.layoutXProperty(), originalShape.x()),
                        new KeyValue(attack.layoutYProperty(), originalShape.y()),
                        new KeyValue(attack.widthProperty(), originalShape.width()),
                        new KeyValue(attack.heightProperty(), originalShape.height()),
                        new KeyValue(attack.fillProperty(), Color.rgb(255, 255, 255, 0.82))
                )
        );

        expand.setOnFinished(event ->
                holdActiveAttackThenFadeOut(attack)
        );

        expand.play();
    }

    private void holdActiveAttackThenFadeOut(Rectangle attack) {
        PauseTransition activeTime = new PauseTransition(
                Duration.seconds(ATTACK_ACTIVE_SECONDS)
        );

        activeTime.setOnFinished(event ->
                fadeOutAndRemoveAttack(attack)
        );

        activeTime.play();
    }

    private void fadeOutAndRemoveAttack(Rectangle attack) {
        FadeTransition fade = new FadeTransition(
                Duration.seconds(ATTACK_FADE_SECONDS),
                attack
        );

        fade.setFromValue(attack.getOpacity());
        fade.setToValue(0.0);

        fade.setOnFinished(event ->
                attackLayer.getChildren().remove(attack)
        );

        fade.play();
    }

    private record AttackShape(
            double x,
            double y,
            double width,
            double height
    ) {
        private static AttackShape from(Rectangle rectangle) {
            return new AttackShape(
                    rectangle.getLayoutX(),
                    rectangle.getLayoutY(),
                    rectangle.getWidth(),
                    rectangle.getHeight()
            );
        }
    }

 
    // Collision 

    private void checkAttackCollision() {
        if (ended || heartView == null) {
            return;
        }

        Rectangle heartHitbox = createHeartHitbox();

        for (Node node : attackLayer.getChildren()) {
            if (!(node instanceof Rectangle attack)) {
                continue;
            }

            if (!ACTIVE_ATTACK_TAG.equals(attack.getUserData())) {
                continue;
            }

            Rectangle attackHitbox = createAttackHitbox(attack);

            if (heartHitbox
                    .getBoundsInParent()
                    .intersects(attackHitbox.getBoundsInParent())) {
                triggerGameOver();
                return;
            }
        }
    }

    private Rectangle createHeartHitbox() {
        return new Rectangle(
                heartX + HEART_HITBOX_X_OFFSET,
                heartY + HEART_HITBOX_Y_OFFSET,
                HEART_HITBOX_SIZE,
                HEART_HITBOX_SIZE
        );
    }

    /**
     * 攻擊判定略微縮小，避免看起來沒碰到卻死亡。
     */
    private Rectangle createAttackHitbox(Rectangle attack) {
        return new Rectangle(
                attack.getLayoutX() + 2,
                attack.getLayoutY() + 2,
                Math.max(0, attack.getWidth() - 4),
                Math.max(0, attack.getHeight() - 4)
        );
    }

 
    // Game Over / Death 

    private void triggerGameOver() {
        if (ended) {
            return;
        }

        ended = true;
        battleStarted = false;

        stopAttackLoop();
        stopGameLoop();

        pressedKeys.clear();

        if (heartView != null) {
            heartView.setImage(loadImage(HEART_DIE_GIF));
        }

        stopOwnMusic();
        audioSystem.playSFX(SoundId.MOM_BATTLE_DEATH);

        PauseTransition waitForDeathGif = new PauseTransition(
                Duration.seconds(2.3)
        );

        waitForDeathGif.setOnFinished(event ->
                finishAndTriggerDeath()
        );

        waitForDeathGif.play();
    }

    private void finishAndTriggerDeath() {
        dispose();

        removeUINode(this);

        /*
         * 結束對話。
         *
         * 若 DialogueSystem.endDialogue() 會恢復場景 BGM，
         * DeathSystem.die() 之後死亡畫面會接管狀態。
         */
        if (dialogueSystem.isActive()) {
            dialogueSystem.endDialogue();
        }

        deathSystem.die(selectDeathReasonByAliveTime());
    }

    private DeathReason selectDeathReasonByAliveTime() {
        if (aliveTime < DEATH_A_TIME_LIMIT) {
            return deathA;
        }

        if (aliveTime >= DEATH_C_TIME_LIMIT) {
            return deathC;
        }

        return deathB;
    }

 
    // Utility 

    private Image loadImage(String path) {
        try {
            URL url = getClass().getResource(path);

            if (url == null) {
                System.out.println("Image not found: " + path);
                return null;
            }

            return new Image(url.toExternalForm());

        } catch (Exception exception) {
            System.out.println("Image load failed: " + path);
            exception.printStackTrace();
            return null;
        }
    }

    private double clamp(
            double value,
            double min,
            double max
    ) {
        return Math.max(min, Math.min(max, value));
    }
}