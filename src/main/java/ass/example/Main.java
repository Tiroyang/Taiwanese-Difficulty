package ass.example;

import ass.example.components.LethalComponent;
import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.EntityType;
import ass.example.core.SaveKey;
import ass.example.core.SceneType;
import ass.example.factories.CommonFactory;
import ass.example.factories.HouseFactory;
import ass.example.factories.PlayerFactory;
import ass.example.factories.StreetFactory;
import ass.example.scenes.system.SceneManager;
import ass.example.system.AchievementSystem;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import ass.example.system.save.SaveSlotManager;
import ass.example.system.save.SaveSystem;
import ass.example.system.CursorManager;
import ass.example.ui.MainMenu;
import ass.example.ui.PauseMenu;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.core.serialization.Bundle;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.profile.DataFile;
import com.almasb.fxgl.profile.SaveLoadHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.Map;
import java.util.function.Consumer;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * Main
 *
 * 遊戲主入口。
 *
 * 負責：
 * 1. 設定 FXGL 遊戲視窗與選單。
 * 2. 初始化 EntityFactory。
 * 3. 初始化全域系統。
 * 4. 載入起始場景 / 存檔場景 / 指定模式場景。
 * 5. 註冊物理碰撞事件。
 * 6. 註冊玩家輸入事件。
 * 7. 每幀轉交 update 給 SceneManager。
 */
public class Main extends GameApplication {

    // =========================================================
    // Constants
    // =========================================================

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;

    private static final String GAME_TITLE = "Taiwanese Difficulty";
    private static final String GAME_VERSION = "2.1";


    // =========================================================
    // Global Runtime Flags
    // =========================================================

    /**
     * 開發模式。
     *
     * true 時：
     * - Factory 會顯示碰撞箱色塊。
     */
    public static boolean devMode = false;


    // =========================================================
    // Core Systems
    // =========================================================

    private SceneManager sceneManager;
    private DeathSystem deathSystem;
    private AudioSystem audioSystem;
    private SaveSystem saveSystem;
    private AchievementSystem achievementSystem;


    // =========================================================
    // Input State
    // =========================================================

    /**
     * 跳躍鍵同時綁定 Space / W / Up。
     *
     * 使用計數器避免：
     * - 同時按住多個跳躍鍵時重複觸發 jumpPressed()
     * - 放開其中一顆鍵就提前觸發 jumpReleased()
     */
    private int jumpKeyHoldCount = 0;


    // =========================================================
    // FXGL Settings
    // =========================================================

    /**
     * 設定遊戲視窗、標題、版本與選單。
     */
    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(WINDOW_WIDTH);
        settings.setHeight(WINDOW_HEIGHT);
        settings.setTitle(GAME_TITLE);
        settings.setVersion(GAME_VERSION);

        settings.setMainMenuEnabled(true);
        settings.setGameMenuEnabled(true);

        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(false);

        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() {
                return new MainMenu();
            }

            @Override
            public FXGLMenu newGameMenu() {
                return new PauseMenu();
            }
        });
    }


    // =========================================================
    // FXGL Save / Load Hook
    // =========================================================

    /**
     * 註冊 FXGL 內建 SaveLoadService 的處理器。
     *
     * 注意：
     * onPreInit 會早於 initGame。
     * 這裡只註冊 handler，不立刻使用 saveSystem。
     * 真正 onSave / onLoad 被呼叫時，saveSystem 應已在 initGame() 初始化。
     */
    @Override
    protected void onPreInit() {
        getSaveLoadService().addHandler(new SaveLoadHandler() {
            @Override
            public void onSave(DataFile dataFile) {
                if (saveSystem == null) {
                    return;
                }

                Bundle bundle = saveSystem.createSaveBundle();
                dataFile.putBundle(bundle);
            }

            @Override
            public void onLoad(DataFile dataFile) {
                if (saveSystem == null) {
                    return;
                }

                Bundle bundle = dataFile.getBundle(SaveKey.BUNDLE_NAME);
                saveSystem.loadFromBundle(bundle);
            }
        });
    }


    // =========================================================
    // Game Initialization
    // =========================================================

    /**
     * 初始化遊戲內容。
     *
     * 執行順序：
     * 1. 註冊 EntityFactory。
     * 2. 初始化系統。
     * 3. 處理主選單要求的讀檔。
     * 4. 處理主選單要求的指定起始場景。
     * 5. 預設載入 HouseScene。
     * 6. 安裝自訂游標。
     */
    @Override
    protected void initGame() {
        registerEntityFactories();
        initializeSystems();

        if (tryLoadPendingSaveSlot()) {
            installCustomCursor();
            return;
        }

        if (tryLoadPendingStartScene()) {
            installCustomCursor();
            return;
        }

        sceneManager.loadHouseScene(false);

        installCustomCursor();
    }

    /**
     * 註冊所有 EntityFactory。
     */
    private void registerEntityFactories() {
        getGameWorld().addEntityFactory(new PlayerFactory());
        getGameWorld().addEntityFactory(new CommonFactory());
        getGameWorld().addEntityFactory(new HouseFactory());
        getGameWorld().addEntityFactory(new StreetFactory());
    }

    /**
     * 初始化所有核心系統。
     *
     * 注意：
     * 不要宣告區域變數遮蔽欄位。
     * 否則 this.achievementSystem 不會被正確設定。
     */
    private void initializeSystems() {
        audioSystem = AudioSystem.getInstance();
        achievementSystem = AchievementSystem.getInstance();

        sceneManager = SceneManager.getInstance();

        deathSystem = new DeathSystem(
                sceneManager,
                audioSystem,
                achievementSystem
        );
        DeathSystem.init(deathSystem);

        saveSystem = new SaveSystem(sceneManager);
        SaveSystem.init(saveSystem);
    }

    /**
     * 如果主選單要求讀取某個存檔槽，就讀取該存檔。
     *
     * @return true 表示已處理讀檔，initGame 不應再載入預設場景。
     */
    private boolean tryLoadPendingSaveSlot() {
        SaveSlotManager saveSlotManager = SaveSlotManager.getInstance();

        if (!saveSlotManager.hasPendingLoadSlot()) {
            return false;
        }

        int slotIndex = saveSlotManager.consumePendingLoadSlot();
        saveSlotManager.loadSlot(slotIndex, saveSystem);

        return true;
    }

    /**
     * 如果主選單要求啟動特定場景或模式，就載入該場景。
     *
     * @return true 表示已載入指定場景。
     */
    private boolean tryLoadPendingStartScene() {
        if (!SceneManager.hasPendingStartScene()) {
            return false;
        }

        SceneType sceneType = SceneManager.consumePendingStartScene();
        sceneManager.loadSceneByTypeForNewGame(sceneType);

        return true;
    }

    /**
     * 安裝自訂游標。
     */
    private void installCustomCursor() {
        CursorManager.install(getGameScene().getRoot());
    }


    // =========================================================
    // Game Variables
    // =========================================================

    /**
     * 初始化 FXGL 全域 vars。
     *
     * 這些 vars 會被：
     * - SaveSystem 存取
     * - SceneManager 判斷場景狀態
     * - Component / System 判斷任務、死亡、互動狀態
     */
    @Override
    protected void initGameVars(Map<String, Object> vars) {
        initDeathVars(vars);
        initHouseVars(vars);
        initStreetEndlessVars(vars);
        initSystemControlVars(vars);
    }

    /**
     * 死亡與成就相關 vars。
     */
    private void initDeathVars(Map<String, Object> vars) {
        vars.put(SaveKey.PLAYER_DEAD, false);
        vars.put(SaveKey.DEATH_COUNT, 0);
        vars.put(SaveKey.LAST_DEATH_REASON, "");

        for (DeathReason reason : DeathReason.values()) {
            vars.put("death_" + reason.name(), false);
        }
    }

    /**
     * HouseScene 劇情與互動狀態相關 vars。
     */
    private void initHouseVars(Map<String, Object> vars) {
        vars.put(SaveKey.QUILT_FOLDED, false);
        vars.put(SaveKey.WATER_DRUNK, false);
        vars.put(SaveKey.TEETH_BRUSHED, false);
        vars.put(SaveKey.SHOES_WORN, false);
        vars.put(SaveKey.PLAYER_ON_BED_COLLIDER, false);

        vars.put(SaveKey.ROOM_LIVING_ROOM_REVEALED, false);
        vars.put(SaveKey.ROOM_TOILET_REVEALED, false);

        vars.put(SaveKey.DOOR_1_OPENED, false);
        vars.put(SaveKey.DOOR_2_OPENED, false);
    }

    /**
     * Street Endless MiniGame 狀態相關 vars。
     */
    private void initStreetEndlessVars(Map<String, Object> vars) {
        vars.put("streetEndlessMode", false);
        vars.put("streetRunDistance", 0.0);
        vars.put("streetBestDistanceBeforeRun", 0.0);
        vars.put("streetBestDistance", 0.0);
        vars.put("streetNewRecord", false);
    }

    /**
     * 系統控制用 vars。
     */
    private void initSystemControlVars(Map<String, Object> vars) {
        vars.put("saveDisabled", false);
        vars.put("achievementDisabled", false);
    }


    // =========================================================
    // Physics
    // =========================================================

    /**
     * 初始化物理世界與碰撞事件。
     */
    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 1600);

        registerGroundSensorCollision(EntityType.WALL);
        registerGroundSensorCollision(EntityType.FLOOR);
        registerGroundSensorCollision(EntityType.BED_ONE_WAY_PLATFORM_COLLIDER);

        registerDeathZoneCollision();
    }

    /**
     * 註冊 player_ground_sensor 與指定地面類型的碰撞。
     *
     * 含
     * - WALL
     * - FLOOR
     * - BED_ONE_WAY_PLATFORM_COLLIDER
     *
     * 功能：
     * - sensor 進入地面時，groundContacts + 1。
     * - sensor 離開地面時，groundContacts - 1。
     */
    private void registerGroundSensorCollision(EntityType groundType) {
        getPhysicsWorld().addCollisionHandler(new CollisionHandler(
                EntityType.PLAYER_GROUND_SENSOR,
                groundType
        ) {
            @Override
            protected void onCollisionBegin(Entity sensor, Entity ground) {
                if (getb(SaveKey.PLAYER_DEAD)) {
                    return;
                }

                withPlayerComponent(PlayerComponent::addGroundContact);
            }

            @Override
            protected void onCollisionEnd(Entity sensor, Entity ground) {
                if (getb(SaveKey.PLAYER_DEAD)) {
                    return;
                }

                withPlayerComponent(PlayerComponent::removeGroundContact);
            }
        });
    }

    /**
     * 註冊玩家碰到死亡區域時死亡。
     */
    private void registerDeathZoneCollision() {
        getPhysicsWorld().addCollisionHandler(new CollisionHandler(
                EntityType.PLAYER,
                EntityType.DEATH_ZONE
        ) {
            @Override
            protected void onCollisionBegin(Entity player, Entity deathZone) {
                if (getb(SaveKey.PLAYER_DEAD)) {
                    return;
                }

                if (!deathZone.hasComponent(LethalComponent.class)) {
                    return;
                }

                LethalComponent lethal =
                        deathZone.getComponent(LethalComponent.class);

                deathSystem.die(lethal.getDeathReason());
            }
        });
    }


    // =========================================================
    // Input
    // =========================================================

    /**
     * 註冊玩家輸入。
     */
    @Override
    protected void initInput() {
        registerMovementInput();
        registerJumpInput();
        registerDropInput();
        registerDashInput();
        registerInteractInput();
    }

    /**
     * 左右移動。
     */
    private void registerMovementInput() {
        addHoldAction(
                "Move Left A",
                KeyCode.A,
                PlayerComponent::moveLeft,
                PlayerComponent::stopLeft
        );

        addHoldAction(
                "Move Left Arrow",
                KeyCode.LEFT,
                PlayerComponent::moveLeft,
                PlayerComponent::stopLeft
        );

        addHoldAction(
                "Move Right D",
                KeyCode.D,
                PlayerComponent::moveRight,
                PlayerComponent::stopRight
        );

        addHoldAction(
                "Move Right Arrow",
                KeyCode.RIGHT,
                PlayerComponent::moveRight,
                PlayerComponent::stopRight
        );
    }

    /**
     * 跳躍。
     *
     * Space / W / Up 都視為同一組跳躍輸入。
     */
    private void registerJumpInput() {
        addJumpAction("Jump Space", KeyCode.SPACE);
        addJumpAction("Jump W", KeyCode.W);
        addJumpAction("Jump Arrow", KeyCode.UP);
    }

    /**
     * 從單向平台下落。
     */
    private void registerDropInput() {
        getInput().addAction(new UserAction("Drop S") {
            @Override
            protected void onActionBegin() {
                if (sceneManager != null) {
                    sceneManager.dropThroughOneWayPlatform();
                }
            }
        }, KeyCode.S);

        getInput().addAction(new UserAction("Drop Down") {
            @Override
            protected void onActionBegin() {
                if (sceneManager != null) {
                    sceneManager.dropThroughOneWayPlatform();
                }
            }
        }, KeyCode.DOWN);
    }

    /**
     * 衝刺。
     *
     * 由於 Shift 是功能鍵，不能使用 addAction 。
     *
     * 使用 EventFilter 是為了攔截 Shift，
     * 避免被其他 UI 或 FXGL Menu 消耗。
     */
    private void registerDashInput() {
        getPrimaryStage().getScene().addEventFilter(
                KeyEvent.KEY_PRESSED,
                event -> {
                    if (event.getCode() != KeyCode.SHIFT) {
                        return;
                    }

                    withPlayerComponent(PlayerComponent::dashPressed);

                    event.consume();
                }
        );
    }

    /**
     * 互動。
     */
    private void registerInteractInput() {
        getInput().addAction(new UserAction("Interact F") {
            @Override
            protected void onActionBegin() {
                if (sceneManager != null) {
                    sceneManager.tryInteract();
                }
            }
        }, KeyCode.F);
    }

    /**
     * 建立一組按下 / 放開型輸入。
     */
    private void addHoldAction(
            String name,
            KeyCode keyCode,
            Consumer<PlayerComponent> onBegin,
            Consumer<PlayerComponent> onEnd
    ) {
        getInput().addAction(new UserAction(name) {
            @Override
            protected void onActionBegin() {
                withPlayerComponent(onBegin);
            }

            @Override
            protected void onActionEnd() {
                withPlayerComponent(onEnd);
            }
        }, keyCode);
    }

    /**
     * 建立跳躍輸入。
     */
    private void addJumpAction(String name, KeyCode keyCode) {
        getInput().addAction(new UserAction(name) {
            @Override
            protected void onActionBegin() {
                pressJumpKey();

                if (sceneManager != null) {
                    sceneManager.onPlayerJumpPressed();
                }
            }

            @Override
            protected void onActionEnd() {
                releaseJumpKey();
            }
        }, keyCode);
    }

    /**
     * 跳躍鍵按下。
     *
     * 多個跳躍鍵同時按住時，只在第一顆按下時觸發 jumpPressed()。
     */
    private void pressJumpKey() {
        jumpKeyHoldCount++;

        if (jumpKeyHoldCount != 1) {
            return;
        }

        withPlayerComponent(PlayerComponent::jumpPressed);
    }

    /**
     * 跳躍鍵放開。
     *
     * 只有全部跳躍鍵都放開時才觸發 jumpReleased()。
     */
    private void releaseJumpKey() {
        jumpKeyHoldCount--;

        if (jumpKeyHoldCount < 0) {
            jumpKeyHoldCount = 0;
        }

        if (jumpKeyHoldCount != 0) {
            return;
        }

        withPlayerComponent(PlayerComponent::jumpReleased);
    }


    // =========================================================
    // Player Helpers
    // =========================================================

    /**
     * 安全取得目前玩家的 PlayerComponent。
     */
    private PlayerComponent getPlayerComponent() {
        if (sceneManager == null) {
            return null;
        }

        Entity player = sceneManager.getPlayer();

        if (player == null || !player.hasComponent(PlayerComponent.class)) {
            return null;
        }

        return player.getComponent(PlayerComponent.class);
    }

    /**
     * 若玩家存在，就對 PlayerComponent 執行指定 action。
     */
    private void withPlayerComponent(Consumer<PlayerComponent> action) {
        PlayerComponent playerComponent = getPlayerComponent();

        if (playerComponent == null || action == null) {
            return;
        }

        action.accept(playerComponent);
    }

    // =========================================================
    // Update
    // =========================================================

    /**
     * 每幀更新。
     */
    @Override
    protected void onUpdate(double tpf) {
        if (sceneManager != null) {
            sceneManager.onUpdate(tpf);
        }
    }


    // =========================================================
    // Public Accessors
    // =========================================================

    public DeathSystem getDeathSystem() {
        return deathSystem;
    }


    // =========================================================
    // Main Entry
    // =========================================================

    public static void main(String[] args) {
        launch(args);
    }
}