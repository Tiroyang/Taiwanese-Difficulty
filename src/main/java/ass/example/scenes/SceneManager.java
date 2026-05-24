package ass.example.scenes;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.SceneType;
import ass.example.system.*;
import com.almasb.fxgl.entity.Entity;
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

    public void loadSceneByType(SceneType sceneType) {
        switch (sceneType) {
            case HOUSE -> loadHouseScene();

            // MiniGame
            case STREET_ENDLESS -> loadStreetEndlessScene();


            default -> loadHouseScene();
        }
    }

    /**
     * 載入家中場景。
     */
    public void loadHouseScene() {
        currentSceneType = SceneType.HOUSE;

        cleanupCurrentScene();
        clearCurrentWorld();

        MusicSystem.getInstance().playBGM(
                "/assets/music/scene1/Kobo Kanaeru - HELP!! (No Vocal).mp3.wav",
                true
        );

        /*
         * 一般故事場景：
         * 允許存檔、允許成就。
         */
        set("saveDisabled", false);
        set("achievementDisabled", false);
        set("playerDead", false);
        set("lastDeathReason", "");
        set("playerOnBedCollider", false);

        SceneConfig homeConfig = getCurrentSceneConfig();

        houseScene = new HouseScene(homeConfig, deathSystem, audioSystem);
        player = houseScene.load();
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

        SceneConfig config = getCurrentSceneConfig();

        streetEndlessScene = new StreetEndlessScene(config, deathSystem, audioSystem);
        player = streetEndlessScene.load();

        refreshPlayerGroundContacts();
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
    }

    /**
     * 通知Scene持續更新場景，由Main呼叫。
     */
    public void onUpdate(double tpf) {
        if (houseScene != null) {
            houseScene.onUpdate(tpf);
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

        if (currentSceneType == SceneType.STREET_ENDLESS && streetEndlessScene != null) {
            streetEndlessScene.resetRuntimeSystems();
        }
    }

    public void restoreDeathFromSave(DeathReason reason) {
        if (deathSystem != null) {
            deathSystem.restoreDeathFromSave(reason);
        }
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
}