package ass.example.ui;

import ass.example.core.DeathReason;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import ass.example.system.MusicSystem;
import ass.example.system.dialogue.DialogueSystem;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

import static com.almasb.fxgl.dsl.FXGL.*;

public class MomBattleMiniGame extends StackPane {

    private static final double SCREEN_WIDTH = 1280;
    private static final double SCREEN_HEIGHT = 720;

    private static final String MOM_BATTLE_IMAGE =
            "/assets/textures/characters/mom/mom_battle.png";

    private static final String HEART_IMAGE =
            "/assets/textures/Scene1/UI/heart.png";

    private static final String HEART_DIE_GIF =
            "/assets/textures/Scene1/UI/heart_die.gif";

    private static final String BATTLE_START_BGM =
            "/assets/music/dialogue/mombattlestart.mp3";

    private static final String BATTLE_LOOP_BGM =
            "/assets/music/dialogue/Dark Souls III OST 10 - Vordt of the Boreal Valley.mp3";

    /*
     * 方框尺寸。
     * 3 x 3 攻擊區域會依這個尺寸平均切割。
     */
    private static final double ARENA_SIZE = 330;
    private static final double ARENA_X = (SCREEN_WIDTH - ARENA_SIZE) / 2.0;
    private static final double ARENA_Y = 360;

    /*
     * heart.png 原圖 520x520，但有效圖案大約 40x40。
     * 這裡讓整張圖縮成 52x52，實際碰撞再用比較小的範圍。
     */
    private static final double HEART_VIEW_SIZE = 520;
    private static final double HEART_HITBOX_SIZE = 42;

    private static final double HEART_HITBOX_X_OFFSET = 239;
    private static final double HEART_HITBOX_Y_OFFSET = 232;

    private final AudioSystem audioSystem = AudioSystem.getInstance();
    private final DeathSystem deathSystem = DeathSystem.getInstance();
    private final DialogueSystem dialogueSystem;

    private final DeathReason deathA;
    private final DeathReason deathB;
    private final DeathReason deathC;

    private final Random random = new Random();

    private Rectangle battleBackground;

    private final Pane gameLayer = new Pane();
    private final Pane battleLayer = new Pane();
    private final Pane attackLayer = new Pane();
    private final Pane transitionLayer = new Pane();

    private ImageView momView;
    private ImageView heartView;

    private Rectangle arenaBorder;

    private AnimationTimer gameLoop;
    private Timeline attackLoop;

    private MediaPlayer startMusic;
    private MediaPlayer battleMusic;

    private final Set<KeyCode> pressedKeys = new HashSet<>();

    private boolean battleStarted = false;
    private boolean ended = false;

    private double heartX;
    private double heartY;

    private double aliveTime = 0;

    private double heartSpeed = 250;

    public MomBattleMiniGame(
            DialogueSystem dialogueSystem,
            DeathReason deathA,
            DeathReason deathB,
            DeathReason deathC
    ) {
        this.dialogueSystem = dialogueSystem;
        this.deathA = deathA;
        this.deathB = deathB;
        this.deathC = deathC;

        setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        setMinSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        setMaxSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        setFocusTraversable(true);
        setPickOnBounds(true);

        setupRoot();
        setupKeyInput();
    }

    private void setupRoot() {
        /*
         * 不要一開始就放黑底。
         * 否則 Transition 的黑色格子會蓋在黑底上，看起來完全沒有動畫。
         */
        setStyle("-fx-background-color: transparent;");

        gameLayer.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        battleLayer.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        attackLayer.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        transitionLayer.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        battleLayer.setVisible(false);

        getChildren().addAll(
                gameLayer,
                battleLayer,
                transitionLayer
        );
    }

    private void setupKeyInput() {
        setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
    }

    public void start() {
        requestFocus();

        /*
         * 停止對話 BGM。
         */
        MusicSystem.getInstance().stopBGM();

        playStartMusic();
        playEncounterTransition();
    }

    private void playStartMusic() {
        stopOwnMusic();

        try {
            URL url = getClass().getResource(BATTLE_START_BGM);

            if (url == null) {
                System.out.println("Battle start music not found: " + BATTLE_START_BGM);
                return;
            }

            startMusic = new MediaPlayer(new Media(url.toExternalForm()));
            startMusic.setOnReady(startMusic::play);
            startMusic.setOnEndOfMedia(this::playBattleLoopMusic);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playBattleLoopMusic() {
        try {
            if (battleMusic != null) {
                battleMusic.stop();
                battleMusic.dispose();
            }

            URL url = getClass().getResource(BATTLE_LOOP_BGM);

            if (url == null) {
                System.out.println("Battle loop music not found: " + BATTLE_LOOP_BGM);
                return;
            }

            battleMusic = new MediaPlayer(new Media(url.toExternalForm()));
            battleMusic.setCycleCount(MediaPlayer.INDEFINITE);
            battleMusic.setOnReady(battleMusic::play);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playEncounterTransition() {
        transitionLayer.getChildren().clear();

        int cols = 22;
        int rows = 12;

        double cellW = SCREEN_WIDTH / cols;
        double cellH = SCREEN_HEIGHT / rows;

        // 1. 建立 Canvas 代替 GridPane
        Canvas canvas = new Canvas(SCREEN_WIDTH, SCREEN_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        transitionLayer.getChildren().add(canvas);

        // 2. 依然建立這個陣列，只是為了丟進你原本的螺旋演算法
        Rectangle[][] cells = new Rectangle[rows][cols];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                Rectangle r = new Rectangle();
                GridPane.setColumnIndex(r, x); // 把 X 座標存進去
                GridPane.setRowIndex(r, y);    // 把 Y 座標存進去
                cells[y][x] = r;
            }
        }

        // 3. 沿用你原本的排序列
        List<Rectangle> order = createCounterClockwiseSpiralOrder(cells, rows, cols);

        Timeline timeline = new Timeline();
        double delay = 0;
        double step = 0.010;

        for (Rectangle cell : order) {
            // 從實體中取出當初存的 X 和 Y 網格座標
            int x = GridPane.getColumnIndex(cell);
            int y = GridPane.getRowIndex(cell);

            timeline.getKeyFrames().add(
                    new KeyFrame(Duration.seconds(delay), e -> {
                        gc.setFill(Color.BLACK);
                        // 關鍵：網格座標 * 單一格子寬高 = 繪製畫面的起點。 寬高 + 0.5 像素徹底補滿縫隙
                        gc.fillRect(x * cellW, y * cellH, cellW + 0.5, cellH + 0.5);
                    })
            );
            delay += step;
        }

        timeline.setOnFinished(e -> {
            setupBattleScreen();

            PauseTransition wait = new PauseTransition(Duration.seconds(0.25));
            wait.setOnFinished(event -> {
                transitionLayer.getChildren().clear();
                startBattle();
            });
            wait.play();
        });

        timeline.play();
    }



    private List<Rectangle> createCounterClockwiseSpiralOrder(
            Rectangle[][] cells,
            int rows,
            int cols
    ) {
        List<Rectangle> result = new ArrayList<>();

        int top = 0;
        int bottom = rows - 1;
        int left = 0;
        int right = cols - 1;

        while (top <= bottom && left <= right) {
            /*
             * 左邊往下。
             */
            for (int y = top; y <= bottom; y++) {
                result.add(cells[y][left]);
            }
            left++;

            /*
             * 下邊往右。
             */
            for (int x = left; x <= right; x++) {
                result.add(cells[bottom][x]);
            }
            bottom--;

            /*
             * 右邊往上。
             */
            if (left <= right) {
                for (int y = bottom; y >= top; y--) {
                    result.add(cells[y][right]);
                }
                right--;
            }

            /*
             * 上邊往左。
             */
            if (top <= bottom) {
                for (int x = right; x >= left; x--) {
                    result.add(cells[top][x]);
                }
                top++;
            }
        }

        return result;
    }

    private void setupBattleScreen() {
        battleLayer.getChildren().clear();

        battleBackground = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);
        battleBackground.setFill(Color.BLACK);

        momView = new ImageView(loadImage(MOM_BATTLE_IMAGE));
        momView.setFitHeight(300);
        momView.setPreserveRatio(true);
        momView.setSmooth(false);
        momView.setOpacity(0);
        momView.setTranslateY(-30);

        StackPane momBox = new StackPane(momView);
        momBox.setPrefSize(SCREEN_WIDTH, 260);
        momBox.setLayoutX(0);
        momBox.setLayoutY(50);

        arenaBorder = new Rectangle(ARENA_SIZE, ARENA_SIZE);
        arenaBorder.setFill(Color.TRANSPARENT);
        arenaBorder.setStroke(Color.WHITE);
        arenaBorder.setStrokeWidth(4);
        arenaBorder.setLayoutX(ARENA_X);
        arenaBorder.setLayoutY(ARENA_Y);

        attackLayer.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        attackLayer.setMouseTransparent(true);

        heartView = new ImageView(loadImage(HEART_IMAGE));
        heartView.setFitWidth(HEART_VIEW_SIZE);
        heartView.setFitHeight(HEART_VIEW_SIZE);
        heartView.setPreserveRatio(true);
        heartView.setSmooth(false);

        heartX = ARENA_X + ARENA_SIZE / 2.0 - HEART_VIEW_SIZE / 2.0;
        heartY = ARENA_Y + ARENA_SIZE / 2.0 - HEART_VIEW_SIZE / 2.0;

        updateHeartPosition();

        battleLayer.getChildren().addAll(
                battleBackground,
                momBox,
                arenaBorder,
                attackLayer,
                heartView
        );

        battleLayer.setVisible(true);

        ParallelTransition appear = new ParallelTransition();

        FadeTransition fade = new FadeTransition(Duration.seconds(0.28), momView);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition popMove = new TranslateTransition(Duration.seconds(0.28), momView);
        popMove.setFromY(-30);
        popMove.setToY(0);
        popMove.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition popScale = new ScaleTransition(Duration.seconds(0.28), momView);
        popScale.setFromX(0.75);
        popScale.setFromY(0.75);
        popScale.setToX(1.0);
        popScale.setToY(1.0);
        popScale.setInterpolator(Interpolator.EASE_OUT);

        appear.getChildren().addAll(fade, popMove, popScale);
        appear.play();
    }

    private void startBattle() {
        battleStarted = true;
        aliveTime = 0;
        requestFocus();

        if (battleMusic == null && (startMusic == null || startMusic.getStatus() != MediaPlayer.Status.PLAYING)) {
            playBattleLoopMusic();
        }

        startGameLoop();
        startAttackLoop();
    }

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            private long last = 0;

            @Override
            public void handle(long now) {
                if (ended || !battleStarted) {
                    last = now;
                    return;
                }

                if (last == 0) {
                    last = now;
                    return;
                }

                double tpf = (now - last) / 1_000_000_000.0;
                last = now;

                updateBattle(tpf);
            }
        };

        gameLoop.start();
    }

    private void updateBattle(double tpf) {
        aliveTime += tpf;

        updateHeartMovement(tpf);
        checkAttackCollision();
    }

    private void updateHeartMovement(double tpf) {
        double dx = 0;
        double dy = 0;

        if (pressedKeys.contains(KeyCode.LEFT) || pressedKeys.contains(KeyCode.A)) {
            dx -= 1;
        }

        if (pressedKeys.contains(KeyCode.RIGHT) || pressedKeys.contains(KeyCode.D)) {
            dx += 1;
        }

        if (pressedKeys.contains(KeyCode.UP) || pressedKeys.contains(KeyCode.W)) {
            dy -= 1;
        }

        if (pressedKeys.contains(KeyCode.DOWN) || pressedKeys.contains(KeyCode.S)) {
            dy += 1;
        }

        if (dx != 0 && dy != 0) {
            double inv = 1.0 / Math.sqrt(2);
            dx *= inv;
            dy *= inv;
        }

        heartX += dx * heartSpeed * tpf;
        heartY += dy * heartSpeed * tpf;

        heartX = clamp(heartX + HEART_HITBOX_X_OFFSET, ARENA_X, ARENA_X + ARENA_SIZE - HEART_HITBOX_SIZE) - HEART_HITBOX_X_OFFSET;
        heartY = clamp(heartY + HEART_HITBOX_Y_OFFSET, ARENA_Y, ARENA_Y + ARENA_SIZE - HEART_HITBOX_SIZE) - HEART_HITBOX_Y_OFFSET;

        updateHeartPosition();
    }

    private void updateHeartPosition() {
        if (heartView != null) {
            heartView.setLayoutX(heartX);
            heartView.setLayoutY(heartY);
        }
    }

    private void startAttackLoop() {
        attackLoop = new Timeline(
                new KeyFrame(Duration.seconds(1.15), e -> spawnRandomAttack())
        );

        attackLoop.setCycleCount(Animation.INDEFINITE);
        attackLoop.play();
    }

    private void spawnRandomAttack() {
        if (ended) {
            return;
        }

        boolean rowAttack = random.nextBoolean();
        int index = random.nextInt(3);

        Rectangle attackRect = createAttackRectangle(rowAttack, index);

        // 紀錄原本的完整攻擊尺寸與位置，供後續瞬間變寬時還原
        final double originalX = attackRect.getLayoutX();
        final double originalY = attackRect.getLayoutY();
        final double originalWidth = attackRect.getWidth();
        final double originalHeight = attackRect.getHeight();

        /*
         * 紅色警告。
         */
        attackRect.setFill(Color.rgb(255, 0, 0, 0.48));
        attackLayer.getChildren().add(attackRect);

        FadeTransition redFlash1 = new FadeTransition(Duration.seconds(0.08), attackRect);
        redFlash1.setFromValue(0.25);
        redFlash1.setToValue(0.8);

        FadeTransition redFlash2 = new FadeTransition(Duration.seconds(0.08), attackRect);
        redFlash2.setFromValue(0.8);
        redFlash2.setToValue(0.25);

        FadeTransition redFlash3 = new FadeTransition(Duration.seconds(0.08), attackRect);
        redFlash3.setFromValue(0.25);
        redFlash3.setToValue(0.85);

        PauseTransition warningHold = new PauseTransition(Duration.seconds(0.3));

        SequentialTransition warning = new SequentialTransition(
                redFlash1,
                redFlash2,
                redFlash3,
                warningHold
        );

        // 警告動畫結束
        warning.setOnFinished(e -> {
            // 1. 將顏色改為指示線顏色（例如白色），不透明度設為 1.0
            attackRect.setFill(Color.rgb(255, 255, 255, 1.0));
            attackRect.setOpacity(1.0);

            // 2. 將 Rectangle 變形為一條跨越畫面的細線 (厚度 2.0 像素)
            double lineWidth = 2.0;
            if (rowAttack) {
                // 橫向攻擊：高度變細，Y 座標下移至該區域的中心
                attackRect.setHeight(lineWidth);
                attackRect.setLayoutY(originalY + (originalHeight - lineWidth) / 2.0);
            } else {
                // 縱向攻擊：寬度變細，X 座標右移至該區域的中心
                attackRect.setWidth(lineWidth);
                attackRect.setLayoutX(originalX + (originalWidth - lineWidth) / 2.0);
            }

            // 3. 預備線顯示時間（此期間不算攻擊判定，維持 0.2 秒，可自訂）
            PauseTransition lineHold = new PauseTransition(Duration.seconds(0.1));

            lineHold.setOnFinished(lineEvent -> {
                // 設定擴展動畫的時間（例如 0.06 秒，可依手感調整）
                Duration expandDuration = Duration.seconds(0.06);

                // 4. 正式進入攻擊判定期間（動畫開始時就加入判定，或動畫結束再加，這裡選擇開始時加入）
                attackRect.setUserData("ACTIVE_ATTACK");

                // 5. 使用 Timeline 製作極短時間的擴展動畫
                KeyValue kvX = new KeyValue(attackRect.layoutXProperty(), originalX);
                KeyValue kvY = new KeyValue(attackRect.layoutYProperty(), originalY);
                KeyValue kvW = new KeyValue(attackRect.widthProperty(), originalWidth);
                KeyValue kvH = new KeyValue(attackRect.heightProperty(), originalHeight);
                // 顏色在動畫期間漸變為原本攻擊的半透明白色
                KeyValue kvFill = new KeyValue(attackRect.fillProperty(), Color.rgb(255, 255, 255, 0.82));

                KeyFrame kf = new KeyFrame(expandDuration, kvX, kvY, kvW, kvH, kvFill);
                Timeline expandTimeline = new Timeline(kf);

                expandTimeline.setOnFinished(expandDoneEvent -> {
                    // 6. 擴展動畫結束後，維持攻擊判定的時間（原 0.48 秒）
                    PauseTransition activeTime = new PauseTransition(Duration.seconds(0.48));
                    activeTime.setOnFinished(event -> {
                        FadeTransition fade = new FadeTransition(Duration.seconds(0.16), attackRect);
                        fade.setFromValue(attackRect.getOpacity());
                        fade.setToValue(0);
                        fade.setOnFinished(done -> attackLayer.getChildren().remove(attackRect));
                        fade.play();
                    });
                    activeTime.play();
                });

                expandTimeline.play();
            });

            lineHold.play();
        });

        warning.play();
    }

    private Rectangle createAttackRectangle(boolean rowAttack, int index) {
        double cell = ARENA_SIZE / 3.0;

        Rectangle rect;

        if (rowAttack) {
            rect = new Rectangle(SCREEN_WIDTH, cell);
            rect.setLayoutX(0);
            rect.setLayoutY(ARENA_Y + index * cell);
        } else {
            rect = new Rectangle(cell, SCREEN_HEIGHT);
            rect.setLayoutX(ARENA_X + index * cell);
            rect.setLayoutY(0);
        }

        rect.setMouseTransparent(true);
        rect.setOpacity(1.0);

        return rect;
    }

    private void checkAttackCollision() {
        if (ended || heartView == null) {
            return;
        }

        // 【修改這裡】改用您定義的固定偏移量（Offset）來計算 Hitbox 座標
        double hx = heartX + HEART_HITBOX_X_OFFSET;
        double hy = heartY + HEART_HITBOX_Y_OFFSET;

        Rectangle heartHitbox = new Rectangle(
                hx,
                hy,
                HEART_HITBOX_SIZE,
                HEART_HITBOX_SIZE
        );

        for (Node node : attackLayer.getChildren()) {
            if (!(node instanceof Rectangle attack)) {
                continue;
            }

            if (!"ACTIVE_ATTACK".equals(attack.getUserData())) {
                continue;
            }

            Rectangle attackBounds = new Rectangle(
                    attack.getLayoutX() + 2,
                    attack.getLayoutY() + 2,
                    attack.getWidth() - 4,
                    attack.getHeight() - 4
            );

            if (heartHitbox.getBoundsInParent().intersects(attackBounds.getBoundsInParent())) {
                triggerGameOver();
                return;
            }
        }
    }

    private void triggerGameOver() {
        if (ended) {
            return;
        }

        ended = true;

        if (attackLoop != null) {
            attackLoop.stop();
        }

        if (gameLoop != null) {
            gameLoop.stop();
        }

        pressedKeys.clear();

        if (heartView != null) {
            heartView.setImage(loadImage(HEART_DIE_GIF));
        }

        stopOwnMusic();
        audioSystem.playSFX(SoundId.MOM_BATTLE_DEATH);

        PauseTransition waitGif = new PauseTransition(Duration.seconds(2.3));
        waitGif.setOnFinished(e -> finishAndDeath());
        waitGif.play();
    }

    private void finishAndDeath() {
        /*
         * 先移除小遊戲。
         */
        removeUINode(this);

        /*
         * 結束對話。
         * 注意：如果你的 DialogueSystem.endDialogue() 會播回 scene BGM，
         * 這裡後面會再 stop 一次，避免死亡時又短暫播回場景音樂。
         */
        if (dialogueSystem != null && dialogueSystem.isActive()) {
            dialogueSystem.endDialogue();
        }

        DeathReason reason = (aliveTime < 60.0) ? deathA : ((aliveTime >= 120.0) ? deathC : deathB);
        deathSystem.die(reason);
    }

    public void dispose() {
        ended = true;

        if (attackLoop != null) {
            attackLoop.stop();
            attackLoop = null;
        }

        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
        }

        stopOwnMusic();
        pressedKeys.clear();
    }

    private void stopOwnMusic() {
        if (startMusic != null) {
            startMusic.stop();
            startMusic.dispose();
            startMusic = null;
        }

        if (battleMusic != null) {
            battleMusic.stop();
            battleMusic.dispose();
            battleMusic = null;
        }
    }

    private Image loadImage(String path) {
        try {
            URL url = getClass().getResource(path);

            if (url == null) {
                System.out.println("Image not found: " + path);
                return null;
            }

            return new Image(url.toExternalForm());

        } catch (Exception e) {
            System.out.println("Image load failed: " + path);
            e.printStackTrace();
            return null;
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}