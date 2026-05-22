package ass.example.components.HouseScene;

import ass.example.components.LoadSaveComponent;
import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import static com.almasb.fxgl.dsl.FXGL.set;
import static com.almasb.fxgl.dsl.FXGLForKtKt.getb;

public class WaterComponent extends Component implements LoadSaveComponent {

    private final Entity player;
    private final Entity visualEntity;
    private final DeathSystem deathSystem;
    private final AudioSystem audioSystem;

    private boolean used = false;

    public WaterComponent(
            Entity visualEntity,
            Entity player,
            DeathSystem deathSystem,
            AudioSystem audioSystem
    ) {
        this.visualEntity = visualEntity;
        this.player = player;
        this.deathSystem = deathSystem;
        this.audioSystem = audioSystem;
    }

    @Override
    public void applySavedState() {
        if (getb("waterDrunk")) {
            restoreDrunkState();
        }
    }

    private void restoreDrunkState() {
        used = true;

        if (visualEntity != null) {
            visualEntity.removeFromWorld();
        }

        entity.removeFromWorld();
    }

    public void drink() {
        if (used) {
            return;
        }

        used = true;
        set("waterDrunk", true);

        if (visualEntity != null) {
            visualEntity.removeFromWorld();
        }

        entity.removeFromWorld();

        if (audioSystem != null) {
            audioSystem.playSFX(SoundId.EATING);
        }

        if (player != null) {
            PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);
            playerComponent.stopAllMovement();
            playerComponent.setControlEnabled(false);
        }

        PauseTransition delay = new PauseTransition(Duration.seconds(1.6));
        delay.setOnFinished(e -> {
            if (deathSystem != null) {
                deathSystem.die(DeathReason.DRINK_WATER);
            }
        });
        delay.play();
    }

    public boolean isUsed() {
        return used;
    }
}