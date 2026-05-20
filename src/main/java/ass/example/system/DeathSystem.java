package ass.example.system;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReasons;
import ass.example.core.SoundId;
import ass.example.scenes.SceneManager;
import ass.example.ui.DeathScreen;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * 死亡系統
 */
public class DeathSystem {

    private final SceneManager sceneManager;
    private final DeathScreen deathScreen;

    private boolean dead = false;
    private DeathReasons currentReason;

    private final AudioSystem audioSystem;

    public DeathSystem(SceneManager sceneManager) {
        this.sceneManager = sceneManager;

        this.audioSystem = new AudioSystem();

        deathScreen = new DeathScreen(
                this::respawn,
                this::goToMainMenu
        );

        addUINode(deathScreen, 0, 0);
    }

    public void die(DeathReasons reason) {
        if (dead) {
            return;
        }

        audioSystem.playSFX(SoundId.DEATH);

        dead = true;
        currentReason = reason;

        set("playerDead", true);
        inc("deathCount", +1);
        set("death_" + reason.name(), true);

        sceneManager.onPlayerDied();

        Entity player = sceneManager.getPlayer();
        if (player != null) {
            PlayerComponent playerComponent =
                    player.getComponent(PlayerComponent.class);

            playerComponent.stopAllMovement();
            playerComponent.setControlEnabled(false);
            playerComponent.playerDead();
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

    private void goToMainMenu() {
        dead = false;
        set("playerDead", false);

        deathScreen.hide();

        getGameController().gotoMainMenu();
    }

    public boolean isDead() {
        return dead;
    }

    public DeathReasons getCurrentReason() {
        return currentReason;
    }
}