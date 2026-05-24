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

        set("playerDead", true);
        set("playerOnBedCollider", false);
        set("lastDeathReason", reason.name());
        inc("deathCount", +1);

        boolean newlyUnlocked = false;

        if (achievementSystem != null) {
            newlyUnlocked = achievementSystem.unlockDeathReason(reason);
        }

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

        deathScreen.show(reason, 0);

        if (newlyUnlocked) {
            deathScreen.showAchievementUnlock(reason);
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