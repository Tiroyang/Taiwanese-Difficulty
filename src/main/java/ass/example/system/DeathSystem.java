package ass.example.system;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.SoundId;
import ass.example.scenes.SceneManager;
import ass.example.ui.DeathScreen;
import com.almasb.fxgl.entity.Entity;
import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * 死亡系統
 */
public class DeathSystem {

    private static DeathSystem INSTANCE;

    public static void init(DeathSystem deathSystem) {
        INSTANCE = deathSystem;
    }

    public static DeathSystem getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("DeathSystem has not been initialized.");
        }
        return INSTANCE;
    }

    private final SceneManager sceneManager;
    private final DeathScreen deathScreen;

    private boolean dead = false;
    private DeathReason currentReason;

    private final AudioSystem audioSystem;
    private final AchievementSystem achievementSystem;

    public DeathSystem(
            SceneManager sceneManager,
            AudioSystem audioSystem,
            AchievementSystem achievementSystem
    ) {
        this.sceneManager = sceneManager;
        this.audioSystem = audioSystem;
        this.achievementSystem = achievementSystem;

        deathScreen = new DeathScreen(
                this::respawn,
                this::goToMainMenu
        );

        addUINode(deathScreen, 0, 0);
    }

    public void die(DeathReason reason) {
        if (dead) {
            return;
        }

        dead = true;
        currentReason = reason;

        set("playerDead", true);
        set("playerOnBedCollider", false);
        set("lastDeathReason", reason.name());

        boolean streetMode = false;

        try {
            streetMode = getb("streetEndlessMode");
        } catch (Exception ignored) {
        }

        /*
         * 故事模式才累加死亡次數。
         * Street Endless 不需要把死亡次數混進去。
         */
        if (!streetMode) {
            inc("deathCount", +1);
        }

        /*
         * 重點：
         * Street Endless 的最高分一定要在 deathScreen.show() 前更新。
         */
        handleStreetEndlessRecordIfNeeded();

        if (audioSystem != null) {
            audioSystem.playSFX(SoundId.DEATH);
        }

        sceneManager.onPlayerDied();

        Entity player = sceneManager.getPlayer();
        if (player != null) {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            pc.stopAllMovement();
            pc.setControlEnabled(false);
            pc.playerDead();
        }

        /*
         * 不要傳 0。
         * 故事模式會顯示死亡次數。
         * Street Endless 則會在 DeathScreen 裡讀 streetRunDistance / streetBestDistance。
         */
        deathScreen.show(reason, geti("deathCount"));

        boolean achievementDisabled = false;

        try {
            achievementDisabled = getb("achievementDisabled");
        } catch (Exception ignored) {
        }

        /*
         * Street Endless 已經設 achievementDisabled = true，
         * 所以不會解鎖成就。
         */
        if (!achievementDisabled && achievementSystem != null) {
            boolean newlyUnlocked = achievementSystem.unlockDeathReason(reason);

            if (newlyUnlocked) {
                deathScreen.showAchievementUnlock(reason);
            }
        }
    }

    public void restoreDeathFromSave(DeathReason reason) {
        if (reason == null) {
            return;
        }

        set("playerDead", true);
        set("pauseDisabled", true);
        set("lastDeathReason", reason.name());

        Entity player = sceneManager.getPlayer();

        if (player != null) {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);

            pc.stopAllMovement();
            pc.setControlEnabled(false);

            /*
             * 如果你已經有死亡圖片方法，請用你的方法名稱。
             * 例如 showDeadImage() / showDead() / setDeadImage()
             */
            pc.playerDead();

            dead = true;
            currentReason = reason;
        }

        deathScreen.show(reason, geti("deathCount"));
    }

    private void handleStreetEndlessRecordIfNeeded() {
        boolean streetMode = false;

        try {
            streetMode = getb("streetEndlessMode");
        } catch (Exception ignored) {
        }

        if (!streetMode) {
            return;
        }

        double currentDistance = 0;

        try {
            currentDistance = getd("streetRunDistance");
        } catch (Exception ignored) {
        }

        double oldBest = StreetEndlessRecordSystem.getInstance().getBestDistance();

        set("streetBestDistanceBeforeRun", oldBest);

        boolean newRecord = StreetEndlessRecordSystem
                .getInstance()
                .tryUpdateBestDistance(currentDistance);

        double newBest = StreetEndlessRecordSystem.getInstance().getBestDistance();

        set("streetBestDistance", newBest);
        set("streetNewRecord", newRecord);
    }

    public void respawn() {
        if (!dead) {
            return;
        }

        dead = false;

        set("playerDead", false);

        deathScreen.hide();

        sceneManager.respawnPlayer();
    }

    public void clearDeathScreenForLoad() {
        set("playerDead", false);
        set("lastDeathReason", "");

        if (deathScreen != null) {
            deathScreen.hide();
        }
    }

    private void goToMainMenu() {
        dead = false;
        set("playerDead", false);

        deathScreen.hide();

        getGameController().gotoMainMenu();
    }

    public boolean isDead() {
        return dead;
    }

    public DeathReason getCurrentReason() {
        return currentReason;
    }
}