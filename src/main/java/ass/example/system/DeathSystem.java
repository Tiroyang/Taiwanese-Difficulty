package ass.example.system;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.SaveKey;
import ass.example.core.SoundId;
import ass.example.scenes.system.SceneManager;
import ass.example.ui.DeathScreen;
import com.almasb.fxgl.entity.Entity;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * DeathSystem
 *
 * 遊戲死亡系統。
 *
 * 功能：
 * 1. 處理玩家死亡流程。
 * 2. 記錄死亡狀態與死亡原因。
 * 3. 更新死亡相關 game vars。
 * 4. 播放死亡音效。
 * 5. 通知目前場景重設 runtime 狀態。
 * 6. 停止玩家控制並切換死亡外觀。
 * 7. 顯示死亡畫面。
 * 8. 解鎖死亡成就。
 * 9. 處理 Street Endless 的距離紀錄。
 * 10. 處理重生。
 * 11. 支援從存檔還原死亡狀態。
 *
 * 需要在遊戲初始化時呼叫：
 * DeathSystem.init(deathSystem);
 */
public class DeathSystem {

    // =========================================================
    // Singleton
    // =========================================================

    /**
     * DeathSystem 單例實體。
     */
    private static DeathSystem INSTANCE;

    /**
     * 初始化 DeathSystem 單例。
     *
     * 在 Main 初始化系統時呼叫。
     *
     * @param deathSystem 已建立好的 DeathSystem
     */
    public static void init(DeathSystem deathSystem) {
        INSTANCE = deathSystem;
    }

    /**
     * 取得 DeathSystem 單例。
     *
     * @return DeathSystem 單例
     * @throws IllegalStateException 若尚未呼叫 init(...)
     */
    public static DeathSystem getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("DeathSystem has not been initialized.");
        }

        return INSTANCE;
    }

    /**
     * 判斷 DeathSystem 是否已初始化。
     *
     * 適合給特殊測試或安全檢查使用。
     *
     * @return true 表示已初始化
     */
    public static boolean isInitialized() {
        return INSTANCE != null;
    }


    // =========================================================
    // Dependencies
    // =========================================================

    /**
     * 場景管理器。
     *
     * 用於：
     * 1. 取得目前玩家。
     * 2. 通知目前場景玩家死亡。
     * 3. 執行重生流程。
     */
    private final SceneManager sceneManager = SceneManager.getInstance();

    /**
     * 死亡畫面 UI。
     *
     * DeathSystem 建立時會建立 DeathScreen，
     * 並加入 UI layer。
     */
    private final DeathScreen deathScreen;

    /**
     * 音效系統。
     *
     * 用於播放死亡音效。
     */
    private final AudioSystem audioSystem = AudioSystem.getInstance();

    /**
     * 成就系統。
     *
     * 用於解鎖死亡原因成就。
     */
    private final AchievementSystem achievementSystem = AchievementSystem.getInstance();


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * 玩家目前是否處於死亡狀態。
     *
     * true：
     * - 不可再次觸發死亡。
     * - DeathScreen 應該正在顯示。
     */
    private boolean dead = false;

    /**
     * 目前死亡原因。
     */
    private DeathReason currentReason;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立死亡系統。
     *
     * 建立時會：
     * 1. 保存必要系統參考。
     * 2. 建立死亡畫面。
     * 3. 將死亡畫面加入 UI layer。
     */
    public DeathSystem() {
        this.deathScreen = new DeathScreen(
                this::respawn);

        addUINode(deathScreen, 0, 0);
    }


    // =========================================================
    // Death Entry
    // =========================================================

    /**
     * 觸發玩家死亡。
     *
     * 流程：
     * 1. 若已死亡，直接返回。
     * 2. 更新 DeathSystem 內部狀態。
     * 3. 更新死亡相關 game vars。
     * 4. 若不是 Street Endless，累加死亡次數。
     * 5. 若是 Street Endless，更新距離紀錄。
     * 6. 播放死亡音效。
     * 7. 通知目前場景玩家死亡。
     * 8. 停止玩家移動與控制。
     * 9. 顯示死亡畫面。
     * 10. 嘗試解鎖死亡成就。
     *
     * @param reason 死亡原因
     */
    public void die(DeathReason reason) {
        if (dead) {
            return;
        }

        setDeadState(reason);

        applyDeathGameVars(reason);
        increaseDeathCountIfNeeded();
        handleStreetEndlessRecordIfNeeded();

        playDeathSound();

        notifyScenePlayerDied();
        killCurrentPlayerVisualState();

        showDeathScreen(reason);
        unlockDeathAchievementIfNeeded(reason);
    }

    /**
     * 設定死亡系統內部狀態。
     *
     * @param reason 死亡原因
     */
    private void setDeadState(DeathReason reason) {
        dead = true;
        currentReason = reason;
    }

    /**
     * 寫入死亡相關 game vars。
     *
     * @param reason 死亡原因
     */
    private void applyDeathGameVars(DeathReason reason) {
        set(SaveKey.PLAYER_DEAD, true);
        set(SaveKey.PLAYER_ON_BED_COLLIDER, false);
        set(SaveKey.LAST_DEATH_REASON, reason.name());
    }

    /**
     * 若目前不是 Street Endless，累加故事模式死亡次數。
     */
    private void increaseDeathCountIfNeeded() {
        if (isStreetEndlessMode()) {
            return;
        }

        inc(SaveKey.DEATH_COUNT, +1);
    }

    /**
     * 播放死亡音效。
     */
    private void playDeathSound() {
        if (audioSystem != null) {
            audioSystem.playSFX(SoundId.DEATH);
        }
    }

    /**
     * 通知目前場景玩家已死亡。
     */
    private void notifyScenePlayerDied() {
        if (sceneManager != null) {
            sceneManager.onPlayerDied();
        }
    }

    /**
     * 停止目前玩家移動、禁用控制，並切換死亡外觀。
     */
    private void killCurrentPlayerVisualState() {
        PlayerComponent playerComponent = getCurrentPlayerComponent();

        if (playerComponent == null) {
            return;
        }

        playerComponent.stopAllMovement();
        playerComponent.setControlEnabled(false);
        playerComponent.playerDead();
    }

    /**
     * 顯示死亡畫面。
     *
     * Story Mode：
     * - DeathScreen 使用 deathCount。
     *
     * Street Endless：
     * - DeathScreen 內部可讀取 streetRunDistance / streetBestDistance。
     *
     * @param reason 死亡原因
     */
    private void showDeathScreen(DeathReason reason) {
        deathScreen.show(reason, geti(SaveKey.DEATH_COUNT));
    }


    // =========================================================
    // Restore Death From Save
    // =========================================================

    /**
     * 從存檔還原死亡狀態。
     *
     * - 不播放死亡音效。
     * - 不增加死亡次數。
     * - 不重複解鎖成就。
     * - 不更新 Street Endless 紀錄。
     *
     * @param reason 存檔中的死亡原因
     */
    public void restoreDeathFromSave(DeathReason reason) {
        if (reason == null) {
            return;
        }

        setDeadState(reason);

        set(SaveKey.PLAYER_DEAD, true);
        set("pauseDisabled", true);
        set(SaveKey.LAST_DEATH_REASON, reason.name());

        killCurrentPlayerVisualState();

        deathScreen.show(reason, geti(SaveKey.DEATH_COUNT));
    }


    // =========================================================
    // Street Endless Record
    // =========================================================

    /**
     * 若目前是 Street Endless 模式，更新最佳距離紀錄。
     *
     * 必須在 deathScreen.show(...) 前執行。
     */
    private void handleStreetEndlessRecordIfNeeded() {
        if (!isStreetEndlessMode()) {
            return;
        }

        double currentDistance = getDoubleVarSafely(
                "streetRunDistance",
                0.0
        );

        StreetEndlessRecordSystem recordSystem =
                StreetEndlessRecordSystem.getInstance();

        double oldBest = recordSystem.getBestDistance();

        set("streetBestDistanceBeforeRun", oldBest);

        boolean newRecord = recordSystem.tryUpdateBestDistance(currentDistance);
        double newBest = recordSystem.getBestDistance();

        set("streetBestDistance", newBest);
        set("streetNewRecord", newRecord);
    }


    // =========================================================
    // Achievement
    // =========================================================

    /**
     * 若成就沒有被禁用，嘗試解鎖死亡原因成就。
     *
     * @param reason 死亡原因
     */
    private void unlockDeathAchievementIfNeeded(DeathReason reason) {
        if (isAchievementDisabled()) {
            return;
        }

        if (achievementSystem == null) {
            return;
        }

        boolean newlyUnlocked = achievementSystem.unlockDeathReason(reason);

        if (newlyUnlocked) {
            deathScreen.showAchievementUnlock(reason);
        }
    }


    // =========================================================
    // Respawn / Exit
    // =========================================================

    /**
     * 玩家按下死亡畫面的重生按鈕時呼叫。
     *
     * 流程：
     * 1. 若目前不是死亡狀態，直接返回。
     * 2. 清除 DeathSystem 內部死亡狀態。
     * 3. 清除 playerDead game var。
     * 4. 隱藏 DeathScreen。
     * 5. 呼叫 SceneManager 重生玩家。
     */
    public void respawn() {
        if (!dead) {
            return;
        }

        clearInternalDeathState();

        set(SaveKey.PLAYER_DEAD, false);

        deathScreen.hide();

        if (sceneManager != null) {
            sceneManager.respawnPlayer();
        }
    }

    /**
     * 清除 DeathSystem 內部死亡狀態。
     */
    private void clearInternalDeathState() {
        dead = false;
        currentReason = null;
    }

    /**
     * 讀檔或切換流程中清除死亡畫面。
     *
     * 用途：
     * - 讀檔前清除舊死亡畫面。
     * - 從死亡狀態讀取非死亡存檔。
     */
    public void clearDeathScreenForLoad() {
        clearInternalDeathState();

        set(SaveKey.PLAYER_DEAD, false);
        set(SaveKey.LAST_DEATH_REASON, "");

        deathScreen.hide();
    }


    // =========================================================
    // Current Player Helpers
    // =========================================================

    /**
     * 取得目前玩家的 PlayerComponent。
     *
     * @return PlayerComponent；若玩家不存在或沒有 PlayerComponent 則回傳 null
     */
    private PlayerComponent getCurrentPlayerComponent() {
        if (sceneManager == null) {
            return null;
        }

        Entity player = sceneManager.getPlayer();

        if (player == null || !player.hasComponent(PlayerComponent.class)) {
            return null;
        }

        return player.getComponent(PlayerComponent.class);
    }


    // =========================================================
    // Game Var Helpers
    // =========================================================

    /**
     * 判斷目前是否為 Street Endless 模式。
     *
     * 若 game var 尚未建立，回傳 false。
     *
     * @return true 表示目前是 Street Endless
     */
    private boolean isStreetEndlessMode() {
        return getBooleanVarSafely("streetEndlessMode", false);
    }

    /**
     * 判斷成就是否被禁用。
     *
     * 若 game var 尚未建立，回傳 false。
     *
     * @return true 表示成就被禁用
     */
    private boolean isAchievementDisabled() {
        return getBooleanVarSafely("achievementDisabled", false);
    }

    /**
     * 安全讀取 boolean game var。
     *
     * 若 key 不存在或讀取失敗，回傳 defaultValue。
     */
    private boolean getBooleanVarSafely(
            String key,
            boolean defaultValue
    ) {
        try {
            return getb(key);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    /**
     * 安全讀取 double game var。
     *
     * 若 key 不存在或讀取失敗，回傳 defaultValue。
     */
    private double getDoubleVarSafely(
            String key,
            double defaultValue
    ) {
        try {
            return getd(key);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }


    // =========================================================
    // Getters
    // =========================================================

    /**
     * 取得目前是否死亡。
     *
     * @return true 表示目前死亡
     */
    public boolean isDead() {
        return dead;
    }

    /**
     * 取得目前死亡原因。
     *
     * @return 目前死亡原因，若尚未死亡可為 null
     */
    public DeathReason getCurrentReason() {
        return currentReason;
    }
}