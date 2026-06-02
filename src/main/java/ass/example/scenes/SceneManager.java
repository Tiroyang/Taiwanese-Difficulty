package ass.example.scenes;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.SaveKey;
import ass.example.core.SceneType;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import ass.example.system.InteractionSystem;
import ass.example.system.MusicSystem;
import ass.example.system.SaveSystem;
import ass.example.system.StreetEndlessRecordSystem;
import ass.example.system.quest.QuestSystem;
import com.almasb.fxgl.core.serialization.Bundle;
import com.almasb.fxgl.entity.Entity;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * SceneManager
 *
 * 遊戲場景總管理器。
 *
 * 功能：
 * 1. 記錄目前所在 SceneType。
 * 2. 載入 HouseScene、StreetScene、StreetEndlessScene。
 * 3. 切換場景並播放黑幕轉場。
 * 4. 保存目前場景中的玩家 Entity。
 * 5. 將玩家輸入事件轉交給目前場景。
 * 6. 處理玩家死亡、重生與讀檔死亡還原。
 * 7. 協助 SaveSystem 儲存 / 還原目前場景額外資料。
 *
 * 注意：
 * SceneManager 可以設計成單例，因為整個遊戲通常只需要一個場景管理器。
 * 但 HouseScene / StreetScene / Runtime System 不建議單例化，
 * 因為它們綁定目前場景載入時生成的 player 與臨時 Entity。
 */
public class SceneManager {

    private static final SceneManager INSTANCE = new SceneManager();

    public static SceneManager getInstance() {
        return INSTANCE;
    }

    // =========================================================
    // View / Transition Constants
    // =========================================================

    /**
     * 遊戲視窗寬度。
     */
    private static final double VIEW_WIDTH = 1280.0;

    /**
     * 遊戲視窗高度。
     */
    private static final double VIEW_HEIGHT = 720.0;

    /**
     * 場景轉場淡入黑幕時間。
     */
    private static final double TRANSITION_FADE_TO_BLACK_SECONDS = 0.55;

    /**
     * 黑幕停留時間。
     */
    private static final double TRANSITION_BLACK_PAUSE_SECONDS = 0.18;

    /**
     * 黑幕淡出時間。
     */
    private static final double TRANSITION_FADE_FROM_BLACK_SECONDS = 0.55;

    /**
     * 轉場後暫時鎖住互動時間。
     *
     * 用途：
     * 避免玩家長按互動鍵，
     * 切場景後立刻觸發另一個入口或出口。
     */
    private static final double INTERACTION_LOCK_SECONDS = 1.2;


    // =========================================================
    // Music Paths
    // =========================================================

    /**
     * Story Street 與 Street Endless 使用的 BGM。
     */
    private static final String STREET_BGM_PATH =
            "/assets/music/scene2/轟はじめ OP.mp3";


    // =========================================================
    // House Return Position
    // =========================================================

    /**
     * 從街道回家時，玩家出現在家中的 X。
     */
    private static final double HOUSE_RETURN_X = 43.0;

    /**
     * 從街道回家時，玩家出現在家中的 Y。
     */
    private static final double HOUSE_RETURN_Y = 452.0;


    // =========================================================
    // Pending Start Scene
    // =========================================================

    /**
     * 待啟動的場景。
     *
     * 這是 static，通常給 MainMenu 或 SaveMenu 在正式進入遊戲前指定起始場景。
     */
    private static SceneType pendingStartSceneType = null;


    // =========================================================
    // Scene Runtime References
    // =========================================================

    /**
     * 目前場景中的玩家 Entity。
     *
     * 每次載入新場景後都會更新。
     */
    private Entity player;

    /**
     * 家中場景。
     */
    private HouseScene houseScene;

    /**
     * 故事模式街道場景。
     */
    private StreetScene streetScene;

    /**
     * 街道無盡模式場景。
     */
    private StreetEndlessScene streetEndlessScene;

    /**
     * 目前所在場景類型。
     */
    private SceneType currentSceneType;


    // =========================================================
    // External Systems
    // =========================================================

    /**
     * 死亡系統。
     */
    private DeathSystem deathSystem;

    /**
     * 音效系統。
     */
    private AudioSystem audioSystem;

    /**
     * 存檔系統。
     */
    private SaveSystem saveSystem;


    // =========================================================
    // Scene Configs
    // =========================================================

    /**
     * 每個 SceneType 對應的場景設定。
     *
     * SceneConfig 包含：
     * 1. 地圖寬度。
     * 2. 地圖高度。
     * 3. 玩家起始 X。
     * 4. 玩家起始 Y。
     */
    private final Map<SceneType, SceneConfig> sceneConfigs = new HashMap<>();


    // =========================================================
    // Runtime Flags
    // =========================================================

    /**
     * 是否正在播放場景轉場。
     *
     * true 時不允許再次觸發轉場。
     */
    private boolean sceneTransitionPlaying = false;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立 SceneManager。
     *
     * 建立時會註冊所有場景設定。
     */
    private SceneManager() {
        registerSceneConfigs();
    }


    // =========================================================
    // Dependency Injection
    // =========================================================

    /**
     * 設定 DeathSystem。
     *
     * @param deathSystem 死亡系統
     */
    public void setDeathSystem(DeathSystem deathSystem) {
        this.deathSystem = deathSystem;
    }

    /**
     * 設定 AudioSystem。
     *
     * @param audioSystem 音效系統
     */
    public void setAudioSystem(AudioSystem audioSystem) {
        this.audioSystem = audioSystem;
    }

    /**
     * 設定 SaveSystem。
     *
     * @param saveSystem 存檔系統
     */
    public void setSaveSystem(SaveSystem saveSystem) {
        this.saveSystem = saveSystem;
    }


    // =========================================================
    // Scene Config Registration
    // =========================================================

    /**
     * 註冊所有場景設定。
     */
    private void registerSceneConfigs() {
        sceneConfigs.put(
                SceneType.HOUSE,
                new SceneConfig(
                        3200,
                        720,
                        2500,
                        422.0
                )
        );

        sceneConfigs.put(
                SceneType.STREET,
                new SceneConfig(
                        1280,
                        720,
                        1120,
                        452.0
                )
        );

        sceneConfigs.put(
                SceneType.STREET_ENDLESS,
                new SceneConfig(
                        1280,
                        720,
                        1120,
                        452.0
                )
        );
    }


    // =========================================================
    // Pending Start Scene API
    // =========================================================

    /**
     * 指定待啟動場景。
     *
     * 通常給主選單、新遊戲、讀檔流程使用。
     *
     * @param sceneType 要啟動的場景
     */
    public static void requestStartScene(SceneType sceneType) {
        pendingStartSceneType = sceneType;
    }

    /**
     * 是否有待啟動場景。
     *
     * @return true 表示有待啟動場景
     */
    public static boolean hasPendingStartScene() {
        return pendingStartSceneType != null;
    }

    /**
     * 取出並清除待啟動場景。
     *
     * @return 原本待啟動的場景
     */
    public static SceneType consumePendingStartScene() {
        SceneType result = pendingStartSceneType;
        pendingStartSceneType = null;
        return result;
    }

    /**
     * 清除待啟動場景。
     */
    public static void clearPendingStartScene() {
        pendingStartSceneType = null;
    }


    // =========================================================
    // Scene Loading - Entry Points
    // =========================================================

    /**
     * 依照讀檔中的 SceneType 載入場景。
     *
     * @param sceneType 存檔中的場景類型
     */
    public void loadSceneByTypeFromSave(SceneType sceneType) {
        switch (sceneType) {
            case HOUSE -> loadHouseScene(true);
            case STREET -> loadStreetScene(true);
            default -> loadHouseScene(true);
        }
    }

    /**
     * 新遊戲依照指定 SceneType 載入場景。
     *
     * @param sceneType 新遊戲起始場景
     */
    public void loadSceneByTypeForNewGame(SceneType sceneType) {
        switch (sceneType) {
            case HOUSE -> loadHouseScene(false);
            case STREET_ENDLESS -> loadStreetEndlessScene();
            default -> loadHouseScene(false);
        }
    }


    // =========================================================
    // Scene Loading - House
    // =========================================================

    /**
     * 載入 HouseScene。
     *
     * @param fromSave 是否由讀檔進入
     */
    public void loadHouseScene(boolean fromSave) {
        loadHouseSceneInternal(
                fromSave,
                null,
                null,
                !fromSave
        );
    }

    /**
     * 載入 HouseScene 並指定玩家位置。
     *
     * 通常用於從 StreetScene 回家。
     *
     * @param playerX 玩家 X
     * @param playerY 玩家 Y
     */
    public void loadHouseSceneAt(double playerX, double playerY) {
        loadHouseSceneInternal(
                true,
                playerX,
                playerY,
                false
        );
    }

    /**
     * HouseScene 實際載入流程。
     *
     * @param fromSave 是否由讀檔進入
     * @param overridePlayerX 指定玩家 X，可為 null
     * @param overridePlayerY 指定玩家 Y，可為 null
     * @param playWakeUpIntro 是否播放起床過場
     */
    private void loadHouseSceneInternal(
            boolean fromSave,
            Double overridePlayerX,
            Double overridePlayerY,
            boolean playWakeUpIntro
    ) {
        currentSceneType = SceneType.HOUSE;

        if (playWakeUpIntro) {
            QuestSystem.getInstance().resetRuntimeState();
        }

        prepareWorldForSceneLoad();

        setupCommonStorySceneVars();
        setupHouseVarsForLoad(fromSave, overridePlayerX, overridePlayerY);

        SceneConfig config = getCurrentSceneConfig();

        /*
         * 如果你的 HouseScene 建構子仍然需要 AudioSystem，
         * 請改成：
         *
         * houseScene = new HouseScene(config, deathSystem, audioSystem, this);
         */
        houseScene = new HouseScene(config, this);
        player = houseScene.load(playWakeUpIntro);

        movePlayerToOverridePositionIfNeeded(overridePlayerX, overridePlayerY);

        applySavedState();
        refreshPlayerGroundContacts();
    }

    /**
     * 設定 HouseScene 載入時需要的變數。
     *
     * 新遊戲進家中時會重置 shoesWorn。
     * 從街道回家或讀檔時，不一定要重置鞋子。
     */
    private void setupHouseVarsForLoad(
            boolean fromSave,
            Double overridePlayerX,
            Double overridePlayerY
    ) {
        set(SaveKey.PLAYER_ON_BED_COLLIDER, false);

        boolean isNewGameHouseStart =
                !fromSave &&
                        overridePlayerX == null &&
                        overridePlayerY == null;

        if (isNewGameHouseStart) {
            set(SaveKey.SHOES_WORN, false);
        }
    }

    /**
     * 若有指定玩家位置，載入場景後立刻移動玩家。
     */
    private void movePlayerToOverridePositionIfNeeded(
            Double overridePlayerX,
            Double overridePlayerY
    ) {
        if (overridePlayerX == null || overridePlayerY == null || player == null) {
            return;
        }

        PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);
        playerComponent.respawnAt(overridePlayerX, overridePlayerY);
    }


    // =========================================================
    // Scene Loading - Story Street
    // =========================================================

    /**
     * 載入 Story Mode StreetScene。
     *
     * @param fromSave 是否由讀檔進入
     */
    public void loadStreetScene(boolean fromSave) {
        currentSceneType = SceneType.STREET;

        prepareWorldForSceneLoad();

        playStreetBGM();

        setupCommonStorySceneVars();
        setupStoryStreetVarsForLoad();

        SceneConfig config = getCurrentSceneConfig();

        streetScene = new StreetScene(config, this);
        player = streetScene.load();

        if (fromSave) {
            applySavedState();
        }

        refreshPlayerGroundContacts();
    }

    /**
     * 設定 Story Street 載入時的變數。
     */
    private void setupStoryStreetVarsForLoad() {
        set("streetEndlessMode", false);
        set(SaveKey.SHOES_WORN, true);
        set(SaveKey.PLAYER_ON_BED_COLLIDER, false);
    }


    // =========================================================
    // Scene Loading - Street Endless
    // =========================================================

    /**
     * 載入 Street Endless 小遊戲。
     *
     * Street Endless：
     * - 禁用存檔。
     * - 禁用成就。
     * - 使用無盡街道距離紀錄。
     * - 重新初始化距離與最佳紀錄資料。
     */
    public void loadStreetEndlessScene() {
        currentSceneType = SceneType.STREET_ENDLESS;

        prepareWorldForSceneLoad();

        playStreetBGM();

        setupStreetEndlessVarsForLoad();

        SceneConfig config = getCurrentSceneConfig();

        streetEndlessScene = new StreetEndlessScene(config, deathSystem, audioSystem);
        player = streetEndlessScene.load();

        refreshPlayerGroundContacts();
    }

    /**
     * 設定 Street Endless 載入時的變數。
     */
    private void setupStreetEndlessVarsForLoad() {
        set("saveDisabled", true);
        set("achievementDisabled", true);

        set(SaveKey.PLAYER_DEAD, false);
        set(SaveKey.LAST_DEATH_REASON, "");
        set(SaveKey.PLAYER_ON_BED_COLLIDER, false);

        set("streetEndlessMode", true);
        set("streetRunDistance", 0.0);

        double bestDistance = StreetEndlessRecordSystem
                .getInstance()
                .getBestDistance();

        set("streetBestDistanceBeforeRun", bestDistance);
        set("streetBestDistance", bestDistance);
        set("streetNewRecord", false);

        set(SaveKey.SHOES_WORN, true);
    }


    // =========================================================
    // Scene Loading - Shared Helpers
    // =========================================================

    /**
     * 載入任何新場景前都要先做的清理。
     */
    private void prepareWorldForSceneLoad() {
        cleanupCurrentScene();
        clearCurrentWorld();
    }

    /**
     * 設定故事模式場景共用變數。
     *
     * 適用：
     * - HouseScene
     * - Story StreetScene
     *
     * 不適用：
     * - StreetEndlessScene
     */
    private void setupCommonStorySceneVars() {
        set("saveDisabled", false);
        set("achievementDisabled", false);

        set(SaveKey.PLAYER_DEAD, false);
        set(SaveKey.LAST_DEATH_REASON, "");
    }

    /**
     * 播放 Street 相關 BGM。
     */
    private void playStreetBGM() {
        MusicSystem.getInstance().playBGM(
                STREET_BGM_PATH,
                true
        );
    }


    // =========================================================
    // Scene Transition
    // =========================================================

    /**
     * 播放 House -> Street 轉場。
     *
     * @param beforeLoadStreetScene 黑幕時、載入街道前要執行的事件，可為 null
     */
    public void playHouseToStreetTransition(Runnable beforeLoadStreetScene) {
        playBlackScreenTransition(
                beforeLoadStreetScene,
                () -> loadStreetScene(true)
        );
    }

    /**
     * 播放 Street -> House 轉場。
     *
     * @param beforeLoadHouseScene 黑幕時、載入家中前要執行的事件，可為 null
     */
    public void playStreetToHouseTransition(Runnable beforeLoadHouseScene) {
        playBlackScreenTransition(
                beforeLoadHouseScene,
                () -> loadHouseSceneAt(HOUSE_RETURN_X, HOUSE_RETURN_Y)
        );
    }

    /**
     * 共用黑幕轉場。
     *
     * 流程：
     * 1. 若已在轉場中，直接返回。
     * 2. 鎖住互動。
     * 3. 暫停玩家控制。
     * 4. 建立黑幕。
     * 5. 淡入黑幕。
     * 6. 黑幕中播放門音效、執行 beforeLoad、載入新場景。
     * 7. 黑幕淡出。
     * 8. 移除黑幕。
     * 9. 恢復新場景玩家控制。
     *
     * @param beforeLoad 黑幕中載入場景前要執行的事件，可為 null
     * @param loadTargetScene 真正載入目標場景的方法
     */
    private void playBlackScreenTransition(
            Runnable beforeLoad,
            Runnable loadTargetScene
    ) {
        if (sceneTransitionPlaying) {
            return;
        }

        sceneTransitionPlaying = true;

        InteractionSystem.lockAllInteractions(INTERACTION_LOCK_SECONDS);
        disableCurrentPlayerControl();

        Rectangle blackOverlay = createBlackOverlay();
        addUINode(blackOverlay, 0, 0);

        FadeTransition fadeToBlack = createFadeTransition(
                blackOverlay,
                TRANSITION_FADE_TO_BLACK_SECONDS,
                0,
                1
        );

        PauseTransition blackPause = new PauseTransition(
                Duration.seconds(TRANSITION_BLACK_PAUSE_SECONDS)
        );

        FadeTransition fadeFromBlack = createFadeTransition(
                blackOverlay,
                TRANSITION_FADE_FROM_BLACK_SECONDS,
                1,
                0
        );

        fadeToBlack.setOnFinished(event -> {
            playTransitionDoorSound();
            runIfPresent(beforeLoad);
            runIfPresent(loadTargetScene);
        });

        SequentialTransition sequence = new SequentialTransition(
                fadeToBlack,
                blackPause,
                fadeFromBlack
        );

        sequence.setOnFinished(event -> {
            removeUINode(blackOverlay);
            sceneTransitionPlaying = false;
            enableCurrentPlayerControl();
        });

        sequence.play();
    }

    /**
     * 建立黑幕節點。
     */
    private Rectangle createBlackOverlay() {
        Rectangle blackOverlay = new Rectangle(VIEW_WIDTH, VIEW_HEIGHT);

        blackOverlay.setFill(Color.BLACK);
        blackOverlay.setOpacity(0);
        blackOverlay.setMouseTransparent(false);

        return blackOverlay;
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
        FadeTransition transition = new FadeTransition(
                Duration.seconds(seconds),
                target
        );

        transition.setFromValue(fromOpacity);
        transition.setToValue(toOpacity);

        return transition;
    }

    /**
     * 播放轉場門音效。
     */
    private void playTransitionDoorSound() {
        if (audioSystem != null) {
            audioSystem.playSFX(SoundId.DOOR_OPEN);
        }
    }

    /**
     * 若 runnable 不為 null，則執行。
     */
    private void runIfPresent(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
    }


    // =========================================================
    // Player Control Helpers
    // =========================================================

    /**
     * 暫停目前玩家控制。
     */
    private void disableCurrentPlayerControl() {
        PlayerComponent playerComponent = getCurrentPlayerComponent();

        if (playerComponent == null) {
            return;
        }

        playerComponent.stopAllMovement();
        playerComponent.setControlEnabled(false);
    }

    /**
     * 恢復目前玩家控制。
     */
    private void enableCurrentPlayerControl() {
        PlayerComponent playerComponent = getCurrentPlayerComponent();

        if (playerComponent != null) {
            playerComponent.setControlEnabled(true);
        }
    }

    /**
     * 重新整理玩家腳底地面接觸。
     */
    private void refreshPlayerGroundContacts() {
        PlayerComponent playerComponent = getCurrentPlayerComponent();

        if (playerComponent != null) {
            playerComponent.refreshGroundContacts();
        }
    }

    /**
     * 取得目前玩家的 PlayerComponent。
     *
     * @return PlayerComponent；若玩家不存在或沒有該 Component 則回傳 null
     */
    private PlayerComponent getCurrentPlayerComponent() {
        if (player == null || !player.hasComponent(PlayerComponent.class)) {
            return null;
        }

        return player.getComponent(PlayerComponent.class);
    }


    // =========================================================
    // Cleanup
    // =========================================================

    /**
     * 清理目前場景物件與 UI。
     */
    private void cleanupCurrentScene() {
        if (houseScene != null) {
            houseScene.cleanup();
            houseScene = null;
        }

        if (streetScene != null) {
            streetScene.cleanup();
            streetScene = null;
        }

        if (streetEndlessScene != null) {
            streetEndlessScene.cleanup();
            streetEndlessScene = null;
        }
    }

    /**
     * 清空目前 GameWorld。
     */
    private void clearCurrentWorld() {
        getGameWorld()
                .getEntitiesCopy()
                .forEach(Entity::removeFromWorld);
    }


    // =========================================================
    // Save / Load State
    // =========================================================

    /**
     * 套用目前場景的存檔狀態。
     */
    public void applySavedState() {
        if (currentSceneType == SceneType.HOUSE && houseScene != null) {
            houseScene.applySavedState();
            return;
        }

        if (currentSceneType == SceneType.STREET && streetScene != null) {
            streetScene.applySavedState();
        }
    }

    /**
     * 儲存目前場景額外資料。
     *
     * 目前只有 Story StreetScene 需要儲存：
     * - 街道區段
     * - 街道障礙物
     *
     * @param bundle SaveSystem 使用的 Bundle
     */
    public void saveCurrentSceneExtraState(Bundle bundle) {
        if (currentSceneType != SceneType.STREET || streetScene == null) {
            return;
        }

        bundle.put(
                SaveKey.STREET_SEGMENTS,
                streetScene.createSegmentSaveString()
        );

        bundle.put(
                SaveKey.STREET_OBSTACLES,
                streetScene.createObstacleSaveString()
        );
    }

    /**
     * 載入目前場景額外資料。
     *
     * 目前只有 Story StreetScene 需要還原：
     * - 街道區段
     * - 街道障礙物
     *
     * @param bundle SaveSystem 使用的 Bundle
     */
    public void loadCurrentSceneExtraState(Bundle bundle) {
        if (currentSceneType != SceneType.STREET || streetScene == null) {
            return;
        }

        restoreStreetSegments(bundle);
        restoreStreetObstacles(bundle);
    }

    /**
     * 還原 StreetScene 區段資料。
     */
    private void restoreStreetSegments(Bundle bundle) {
        try {
            String segmentData = bundle.get(SaveKey.STREET_SEGMENTS);
            streetScene.restoreSegmentsFromSaveString(segmentData);
        } catch (Exception ignored) {
            streetScene.restoreSegmentsFromSaveString("");
        }
    }

    /**
     * 還原 StreetScene 障礙物資料。
     */
    private void restoreStreetObstacles(Bundle bundle) {
        try {
            String obstacleData = bundle.get(SaveKey.STREET_OBSTACLES);
            streetScene.restoreObstaclesFromSaveString(obstacleData);
        } catch (Exception exception) {
            exception.printStackTrace();
            streetScene.restoreObstaclesFromSaveString("");
        }
    }


    // =========================================================
    // Per Frame Update
    // =========================================================

    /**
     * 每幀更新目前場景。
     *
     * 由 Main 呼叫。
     *
     * @param tpf time per frame
     */
    public void onUpdate(double tpf) {
        if (houseScene != null) {
            houseScene.onUpdate(tpf);
        }

        if (streetScene != null) {
            streetScene.onUpdate(tpf);
        }

        if (streetEndlessScene != null) {
            streetEndlessScene.onUpdate(tpf);
        }
    }


    // =========================================================
    // Input Forwarding
    // =========================================================

    /**
     * 嘗試與目前場景附近物件互動。
     *
     * 由 Main 呼叫。
     */
    public void tryInteract() {
        if (houseScene != null) {
            houseScene.tryInteract();
        }

        if (streetScene != null) {
            streetScene.tryInteract();
        }
    }

    /**
     * 玩家按下跳躍鍵。
     *
     * 目前只有 HouseScene 需要額外通知床與單向平台系統。
     */
    public void onPlayerJumpPressed() {
        if (currentSceneType == SceneType.HOUSE && houseScene != null) {
            houseScene.onPlayerJumpPressed();
        }
    }

    /**
     * 玩家按下往下穿過單向平台鍵。
     *
     * 目前只有 HouseScene 有單向平台與床平台。
     */
    public void dropThroughOneWayPlatform() {
        if (currentSceneType == SceneType.HOUSE && houseScene != null) {
            houseScene.dropThroughOneWayPlatform();
        }
    }


    // =========================================================
    // Death / Respawn
    // =========================================================

    /**
     * 玩家死亡時呼叫。
     *
     * 用於通知目前場景重設暫時狀態。
     */
    public void onPlayerDied() {
        resetCurrentSceneRuntimeSystems();
    }

    /**
     * 玩家重生。
     *
     * Street Endless：
     * - 直接重載整個小遊戲場景。
     *
     * 一般 Story Scene：
     * - 清理死亡狀態。
     * - 玩家回到目前場景起點。
     * - 重設目前場景的 runtime systems。
     */
    public void respawnPlayer() {
        if (player == null) {
            return;
        }

        if (currentSceneType == SceneType.STREET_ENDLESS) {
            loadStreetEndlessScene();
            return;
        }

        resetCurrentSceneStateForRespawn();
        clearDeathStateForLoad();

        SceneConfig config = getCurrentSceneConfig();

        PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);
        playerComponent.respawnAt(
                config.getPlayerStartX(),
                config.getPlayerStartY()
        );

        resetCurrentSceneRuntimeSystems();

        set(SaveKey.PLAYER_DEAD, false);
        set(SaveKey.LAST_DEATH_REASON, "");

        playerComponent.setControlEnabled(true);
    }

    /**
     * 清除死亡狀態。
     *
     * 用於：
     * - 讀檔前。
     * - 重生前。
     */
    public void clearDeathStateForLoad() {
        set(SaveKey.PLAYER_DEAD, false);
        set(SaveKey.LAST_DEATH_REASON, "");

        if (deathSystem != null) {
            deathSystem.clearDeathScreenForLoad();
        }
    }

    /**
     * 從存檔還原死亡狀態。
     *
     * @param reason 存檔中的死亡原因
     */
    public void restoreDeathFromSave(DeathReason reason) {
        if (deathSystem != null) {
            deathSystem.restoreDeathFromSave(reason);
        }
    }

    /**
     * 重設目前場景的執行期系統。
     */
    public void resetCurrentSceneRuntimeSystems() {
        if (currentSceneType == SceneType.HOUSE && houseScene != null) {
            houseScene.resetRuntimeSystems();
            return;
        }

        if (currentSceneType == SceneType.STREET && streetScene != null) {
            streetScene.resetRuntimeSystems();
            return;
        }

        if (currentSceneType == SceneType.STREET_ENDLESS && streetEndlessScene != null) {
            streetEndlessScene.resetRuntimeSystems();
        }
    }

    /**
     * 重生前重設目前場景狀態。
     */
    public void resetCurrentSceneStateForRespawn() {
        if (currentSceneType == SceneType.HOUSE) {
            resetHouseSceneStateForRespawn();
            return;
        }

        if (currentSceneType == SceneType.STREET) {
            resetStreetSceneStateForRespawn();
        }
    }

    /**
     * HouseScene 死亡重生前狀態重設。
     *
     * 目前規則：
     * - 鞋子重置為未穿。
     * - 重新套用可存檔 Component 外觀。
     */
    private void resetHouseSceneStateForRespawn() {
        set(SaveKey.SHOES_WORN, false);

        applySavedState();

        refreshPlayerGroundContacts();
    }

    /**
     * Story Street 死亡重生前狀態重設。
     *
     * 目前規則：
     * - 清除床平台狀態。
     * - 街道預設穿鞋。
     */
    private void resetStreetSceneStateForRespawn() {
        set(SaveKey.PLAYER_ON_BED_COLLIDER, false);
        set(SaveKey.SHOES_WORN, true);

        refreshPlayerGroundContacts();
    }


    // =========================================================
    // Getters
    // =========================================================

    /**
     * 取得目前場景設定。
     *
     * @return SceneConfig
     */
    public SceneConfig getCurrentSceneConfig() {
        return sceneConfigs.get(currentSceneType);
    }

    /**
     * 取得目前場景類型。
     *
     * @return SceneType
     */
    public SceneType getCurrentSceneType() {
        return currentSceneType;
    }

    /**
     * 取得目前玩家 Entity。
     *
     * @return 玩家 Entity
     */
    public Entity getPlayer() {
        return player;
    }
}