package ass.example.scenes;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.SaveKey;
import ass.example.core.SceneType;
import ass.example.core.SoundId;
import ass.example.system.*;
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
 * SceneManager負責：
 * 1. 記錄目前是哪一個場景
 * 2. 載入 / 切換場景
 * 3. 保存目前場景的玩家 Entity
 * 4. 根據目前場景，把輸入事件轉交給對應 Scene
 */
public class SceneManager {
    private Entity player;
    /**
     * 匯入場景。
     */
    private HouseScene houseScene;
    private StreetScene streetScene;
    private StreetEndlessScene streetEndlessScene;
    // 在此新增匯入場景

    /**
     * 目前所在SceneType。
     */
    private SceneType currentSceneType;

    private DeathSystem deathSystem;
    private AudioSystem audioSystem;
    private SaveSystem saveSystem;

    /**
     * 每個場景對應一組SceneConfig。
     * 目前包含：
     * 1. 地圖寬度
     * 2. 地圖高度
     * 3. 玩家起始 X
     * 4. 玩家起始 Y
     */
    private final Map<SceneType, SceneConfig> sceneConfigs = new HashMap<>();

    private static SceneType pendingStartSceneType = null;

    private boolean sceneTransitionPlaying = false;

    // Constructor
    public SceneManager() {
        registerSceneConfigs();
    }

    /**
     * 註冊SceneConfig。
     */
    private void registerSceneConfigs() {
        sceneConfigs.put(SceneType.HOUSE,
                new SceneConfig(
                        3200,
                        720,
                        2500,
                        422.0
                )
        );

        sceneConfigs.put(SceneType.STREET,
                new SceneConfig(
                        1280,
                        720,
                        1120,
                        452.0
                )
        );

        // MiniGame
        sceneConfigs.put(SceneType.STREET_ENDLESS,
                new SceneConfig(
                        1280,
                        720,
                        1120,
                        452.0
                )
        );

        // 在此新增場景SceneConfig
    }

    public void setDeathSystem(DeathSystem deathSystem) { this.deathSystem = deathSystem; }
    public void setAudioSystem(AudioSystem audioSystem) { this.audioSystem = audioSystem; }
    public void setSaveSystem(SaveSystem saveSystem) { this.saveSystem = saveSystem; }

    public static void requestStartScene(SceneType sceneType) {
        pendingStartSceneType = sceneType;
    }

    public static boolean hasPendingStartScene() {
        return pendingStartSceneType != null;
    }

    public static SceneType consumePendingStartScene() {
        SceneType result = pendingStartSceneType;
        pendingStartSceneType = null;
        return result;
    }

    public static void clearPendingStartScene() {
        pendingStartSceneType = null;
    }

    public void loadSceneByTypeFromSave(SceneType sceneType) {
        switch (sceneType) {
            case HOUSE -> loadHouseScene(true);
            case STREET -> loadStreetScene(true);
            default -> loadHouseScene(true);
        }
    }

    public void loadSceneByTypeForNewGame(SceneType sceneType) {
        switch (sceneType) {
            case HOUSE -> loadHouseScene(false);
            case STREET_ENDLESS -> loadStreetEndlessScene();
            default -> loadHouseScene(false);
        }
    }

    public void loadHouseScene(boolean fromSave) {
        loadHouseSceneInternal(
                fromSave,
                null,
                null,
                !fromSave
        );
    }

    public void loadHouseSceneAt(double playerX, double playerY) {
        loadHouseSceneInternal(
                true,
                playerX,
                playerY,
                false
        );
    }

    /**
     * 載入家中場景。
     */
    private void loadHouseSceneInternal(
            boolean fromSave,
            Double overridePlayerX,
            Double overridePlayerY,
            boolean playWakeUpIntro
    ) {
        currentSceneType = SceneType.HOUSE;

        /*
         * 只有新遊戲才重置任務。
         * 從街道回家、讀檔，都不應該重置任務進度。
         */
        if (playWakeUpIntro) {
            QuestSystem.getInstance().resetRuntimeState();
        }

        cleanupCurrentScene();
        clearCurrentWorld();

        set("saveDisabled", false);
        set("achievementDisabled", false);
        set("playerDead", false);
        set("lastDeathReason", "");
        set("playerOnBedCollider", false);

        /*
         * 只有新遊戲才重置鞋子。
         * 從街道回家不一定要重置。
         */
        if (!fromSave && overridePlayerX == null && overridePlayerY == null) {
            set("shoesWorn", false);
        }

        SceneConfig homeConfig = getCurrentSceneConfig();

        houseScene = new HouseScene(homeConfig, deathSystem, audioSystem, this);
        player = houseScene.load(!fromSave);

        /*
         * 如果有指定座標，載入完成後立刻移動玩家。
         */
        if (overridePlayerX != null && overridePlayerY != null && player != null) {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            pc.respawnAt(overridePlayerX, overridePlayerY);
        }

        applySavedState();
        refreshPlayerGroundContacts();
    }

    /**
     * 載入 Story Mode 街道場景。
     */
    public void loadStreetScene(boolean fromSave) {
        currentSceneType = SceneType.STREET;

        cleanupCurrentScene();
        clearCurrentWorld();

        MusicSystem.getInstance().playBGM(
                "/assets/music/scene2/轟はじめ OP.mp3",
                true
        );

        /*
         * Story Mode Street：
         * 允許存檔、允許成就。
         * 不使用 Street Endless 距離紀錄。
         */
        set("saveDisabled", false);
        set("achievementDisabled", false);
        set("playerDead", false);
        set("lastDeathReason", "");
        set("playerOnBedCollider", false);

        set("streetEndlessMode", false);
        set("shoesWorn", true);

        SceneConfig config = getCurrentSceneConfig();

        streetScene = new StreetScene(config, deathSystem, audioSystem, this);
        player = streetScene.load();

        refreshPlayerGroundContacts();
    }

    // MiniGame
    public void loadStreetEndlessScene() {
        currentSceneType = SceneType.STREET_ENDLESS;

        cleanupCurrentScene();
        clearCurrentWorld();

        MusicSystem.getInstance().playBGM(
                "/assets/music/scene2/轟はじめ OP.mp3",
                true
        );

        set("saveDisabled", true);
        set("achievementDisabled", true);
        set("playerDead", false);
        set("lastDeathReason", "");
        set("playerOnBedCollider", false);

        set("streetEndlessMode", true);
        set("streetRunDistance", 0.0);
        set("streetBestDistanceBeforeRun", StreetEndlessRecordSystem.getInstance().getBestDistance());
        set("streetBestDistance", StreetEndlessRecordSystem.getInstance().getBestDistance());
        set("streetNewRecord", false);

        set("shoesWorn", true);

        SceneConfig config = getCurrentSceneConfig();

        streetEndlessScene = new StreetEndlessScene(config, deathSystem, audioSystem);
        player = streetEndlessScene.load();

        refreshPlayerGroundContacts();
    }

    public void playHouseToStreetTransition(Runnable beforeLoadStreetScene) {
        if (sceneTransitionPlaying) {
            return;
        }

        sceneTransitionPlaying = true;

        /*
         * 避免玩家長按 F，切場景後又立刻觸發 entrance_door。
         */
        InteractionSystem.lockAllInteractions(1.2);

        /*
         * 暫時關閉玩家控制。
         */
        if (player != null && player.hasComponent(PlayerComponent.class)) {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            pc.stopAllMovement();
            pc.setControlEnabled(false);
        }

        Rectangle blackOverlay = new Rectangle(1280, 720);
        blackOverlay.setFill(Color.BLACK);
        blackOverlay.setOpacity(0);
        blackOverlay.setMouseTransparent(false);

        addUINode(blackOverlay, 0, 0);

        FadeTransition fadeToBlack = new FadeTransition(Duration.seconds(0.55), blackOverlay);
        fadeToBlack.setFromValue(0);
        fadeToBlack.setToValue(1);

        PauseTransition blackPause = new PauseTransition(Duration.seconds(0.18));

        FadeTransition fadeFromBlack = new FadeTransition(Duration.seconds(0.55), blackOverlay);
        fadeFromBlack.setFromValue(1);
        fadeFromBlack.setToValue(0);

        fadeToBlack.setOnFinished(e -> {
            /*
             * 換成你的實際出門音效 SoundId。
             * 如果還沒有，可以先用 DOOR_OPEN 或 BUTTON_PRESSED 測試。
             */
            audioSystem.playSFX(SoundId.DOOR_OPEN);

            if (beforeLoadStreetScene != null) {
                beforeLoadStreetScene.run();
            }

            /*
             * 這裡建議用 false。
             * 這不是讀檔，而是故事模式切場景。
             */
            loadStreetScene(true);
        });

        SequentialTransition sequence = new SequentialTransition(
                fadeToBlack,
                blackPause,
                fadeFromBlack
        );

        sequence.setOnFinished(e -> {
            removeUINode(blackOverlay);

            sceneTransitionPlaying = false;

            /*
             * loadStreetScene(false) 後 player 已經變成 StreetScene 的玩家。
             */
            if (player != null && player.hasComponent(PlayerComponent.class)) {
                PlayerComponent pc = player.getComponent(PlayerComponent.class);
                pc.setControlEnabled(true);
            }
        });

        sequence.play();
    }

    public void playStreetToHouseTransition(Runnable beforeLoadStreetScene) {
        if (sceneTransitionPlaying) {
            return;
        }

        sceneTransitionPlaying = true;

        /*
         * 避免玩家長按 F，切場景後又立刻觸發 entrance_door。
         */
        InteractionSystem.lockAllInteractions(1.2);

        /*
         * 暫時關閉玩家控制。
         */
        if (player != null && player.hasComponent(PlayerComponent.class)) {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            pc.stopAllMovement();
            pc.setControlEnabled(false);
        }

        Rectangle blackOverlay = new Rectangle(1280, 720);
        blackOverlay.setFill(Color.BLACK);
        blackOverlay.setOpacity(0);
        blackOverlay.setMouseTransparent(false);

        addUINode(blackOverlay, 0, 0);

        FadeTransition fadeToBlack = new FadeTransition(Duration.seconds(0.55), blackOverlay);
        fadeToBlack.setFromValue(0);
        fadeToBlack.setToValue(1);

        PauseTransition blackPause = new PauseTransition(Duration.seconds(0.18));

        FadeTransition fadeFromBlack = new FadeTransition(Duration.seconds(0.55), blackOverlay);
        fadeFromBlack.setFromValue(1);
        fadeFromBlack.setToValue(0);

        fadeToBlack.setOnFinished(e -> {
            /*
             * 換成你的實際出門音效 SoundId。
             * 如果還沒有，可以先用 DOOR_OPEN 或 BUTTON_PRESSED 測試。
             */
            audioSystem.playSFX(SoundId.DOOR_OPEN);

            if (beforeLoadStreetScene != null) {
                beforeLoadStreetScene.run();
            }

            /*
             * 這裡建議用 false。
             * 這不是讀檔，而是故事模式切場景。
             */
            loadHouseSceneAt(43.0, 452.0);
        });

        SequentialTransition sequence = new SequentialTransition(
                fadeToBlack,
                blackPause,
                fadeFromBlack
        );

        sequence.setOnFinished(e -> {
            removeUINode(blackOverlay);

            sceneTransitionPlaying = false;

            /*
             * loadStreetScene(false) 後 player 已經變成 StreetScene 的玩家。
             */
            if (player != null && player.hasComponent(PlayerComponent.class)) {
                PlayerComponent pc = player.getComponent(PlayerComponent.class);
                pc.setControlEnabled(true);
            }
        });

        sequence.play();
    }

    private void refreshPlayerGroundContacts() {
        if (player == null) {
            return;
        }

        if (!player.hasComponent(PlayerComponent.class)) {
            return;
        }

        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        pc.refreshGroundContacts();
    }

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

    private void clearCurrentWorld() {
        getGameWorld()
                .getEntitiesCopy()
                .forEach(Entity::removeFromWorld);
    }

    public void applySavedState() {
        if (currentSceneType == SceneType.HOUSE && houseScene != null) {
            houseScene.applySavedState();
        }

        if (currentSceneType == SceneType.STREET && streetScene != null) {
            streetScene.applySavedState();
        }
    }

    /**
     * 通知Scene持續更新場景，由Main呼叫。
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

    /**
     * 通知Scene嘗試互動，由Main呼叫。
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
     * 通知Scene玩家按下跳躍鍵，從Main呼叫。
     */
    public void onPlayerJumpPressed() {
        if (currentSceneType == SceneType.HOUSE && houseScene != null) {
            houseScene.onPlayerJumpPressed();
        }
    }

    /**
     * 通知Scene玩家按下墜落鍵，從Main呼叫。
     */
    public void dropThroughOneWayPlatform() {
        if (currentSceneType == SceneType.HOUSE && houseScene != null) {
            houseScene.dropThroughOneWayPlatform();
        }
    }

    /**
     * 玩家死亡，通知Scene重設暫時狀態。
     */
    public void onPlayerDied() {
        if (currentSceneType == SceneType.HOUSE && houseScene != null) {
            houseScene.resetRuntimeSystems();
        }

        if (currentSceneType == SceneType.STREET && streetScene != null) {
        }
    }

    /**
     * 玩家重生。
     */
    public void respawnPlayer() {
        if (player == null) {
            return;
        }

        if (currentSceneType == SceneType.STREET_ENDLESS) {
            /*
             * Street Endless 是小遊戲模式。
             * 重生時直接重新載入整個場景：
             * - 清除街景
             * - 清除障礙物
             * - 重置距離
             * - 重置鏡頭
             * - 重新生成玩家
             */
            loadStreetEndlessScene();
            return;
        }

        resetCurrentSceneStateForRespawn();

        clearDeathStateForLoad();

        SceneConfig config = getCurrentSceneConfig();

        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        pc.respawnAt(config.getPlayerStartX(), config.getPlayerStartY());

        resetCurrentSceneRuntimeSystems();

        set("playerDead", false);
        set("lastDeathReason", "");

        pc.setControlEnabled(true);
    }

    public void clearDeathStateForLoad() {
        set("playerDead", false);
        set("lastDeathReason", "");

        if (deathSystem != null) {
            deathSystem.clearDeathScreenForLoad();
        }
    }

    /**
     * 重設目前場景的暫時狀態。
     */
    public void resetCurrentSceneRuntimeSystems() {
        if (currentSceneType == SceneType.HOUSE && houseScene != null) {
            houseScene.resetRuntimeSystems();
        }

        if (currentSceneType == SceneType.STREET && streetScene != null) {
            streetScene.resetRuntimeSystems();
        }

        if (currentSceneType == SceneType.STREET_ENDLESS && streetEndlessScene != null) {
            streetEndlessScene.resetRuntimeSystems();
        }
    }

    public void restoreDeathFromSave(DeathReason reason) {
        if (deathSystem != null) {
            deathSystem.restoreDeathFromSave(reason);
        }
    }

    public void resetCurrentSceneStateForRespawn() {
        if (currentSceneType == SceneType.HOUSE) {
            resetHouseSceneStateForRespawn();
            return;
        }

        if (currentSceneType == SceneType.STREET) {
            resetStreetSceneStateForRespawn();
            return;
        }
    }

    private void resetHouseSceneStateForRespawn() {
        /*
         * HouseScene 死亡重生時，鞋子重置為未穿。
         */
        set("shoesWorn", false);

        /*
         * 讓鞋櫃、門、被子、水等有 LoadSaveComponent 的物件
         * 根據目前 vars 重新刷新外觀。
         *
         * shoesWorn=false 後，ShoeComponent.applySavedState()
         * 應該會把鞋櫃外觀恢復成 Shoes.png。
         */
        applySavedState();

        refreshPlayerGroundContacts();
    }

    private void resetStreetSceneStateForRespawn() {
        /*
         * Story Mode Street 死亡重生時的暫時狀態。
         * 不清除任務、不清除已存的 story vars。
         */
        set("playerOnBedCollider", false);

        /*
         * 街道預設穿鞋。
         * 如果你希望保留玩家進入街道前的鞋子狀態，可以刪掉這行。
         */
        set("shoesWorn", true);

        refreshPlayerGroundContacts();
    }

    /**
     * 取得目前場景設定。
     */
    public SceneConfig getCurrentSceneConfig() {
        return sceneConfigs.get(currentSceneType);
    }

    public SceneType getCurrentSceneType() {
        return currentSceneType;
    }

    public Entity getPlayer() {
        return player;
    }

    public void saveCurrentSceneExtraState(Bundle bundle) {
        if (currentSceneType == SceneType.STREET && streetScene != null) {

            bundle.put(SaveKey.STREET_SEGMENTS, streetScene.createSegmentSaveString());
            bundle.put(SaveKey.STREET_OBSTACLES, streetScene.createObstacleSaveString());
        }
    }

    public void loadCurrentSceneExtraState(Bundle bundle) {
        if (currentSceneType == SceneType.STREET && streetScene != null) {
            try {
                String segmentData = bundle.get(SaveKey.STREET_SEGMENTS);
                streetScene.restoreSegmentsFromSaveString(segmentData);
            } catch (Exception ignored) {
            }

            try {
                streetScene.restoreObstaclesFromSaveString(bundle.get(SaveKey.STREET_OBSTACLES));

            } catch (Exception e) {
                e.printStackTrace();

                streetScene.restoreObstaclesFromSaveString("");
            }
        }
    }
}