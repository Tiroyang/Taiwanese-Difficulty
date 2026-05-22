package ass.example.scenes;

import ass.example.components.PlayerComponent;
import ass.example.core.SceneType;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import ass.example.system.SaveSystem;
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
                        402
                )
        )

        // 在此新增場景SceneConfig
        ;
    }

    public void setDeathSystem(DeathSystem deathSystem) { this.deathSystem = deathSystem; }
    public void setAudioSystem(AudioSystem audioSystem) { this.audioSystem = audioSystem; }
    public void setSaveSystem(SaveSystem saveSystem) { this.saveSystem = saveSystem; }

    public void loadSceneByType(SceneType sceneType) {
        switch (sceneType) {
            case HOUSE -> loadHouseScene();

            // case STREET -> loadStreetScene();
            // case ENDLESS -> loadEndlessScene();

            default -> loadHouseScene();
        }
    }

    /**
     * 載入家中場景。
     *
     * 清空目前 GameWorld 裡所有 Entity，再重新建立 HouseScene。
     */
    public void loadHouseScene() {
        currentSceneType = SceneType.HOUSE;

        if (houseScene != null) {
            houseScene.cleanup();
            houseScene = null;
        }

        getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);

        SceneConfig homeConfig = getCurrentSceneConfig();

        houseScene = new HouseScene(homeConfig, deathSystem, audioSystem);
        player = houseScene.load();
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

        SceneConfig config = getCurrentSceneConfig();

        PlayerComponent pc = player.getComponent(PlayerComponent.class);

        pc.respawnAt(
                config.getPlayerStartX(),
                config.getPlayerStartY()
        );

        resetCurrentSceneRuntimeSystems();

        pc.setControlEnabled(true);
    }

    /**
     * 重設目前場景的暫時狀態。
     */
    private void resetCurrentSceneRuntimeSystems() {
        if (currentSceneType == SceneType.HOUSE && houseScene != null) {
            houseScene.resetRuntimeSystems();
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