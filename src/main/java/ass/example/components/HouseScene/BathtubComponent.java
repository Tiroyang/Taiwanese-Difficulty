package ass.example.components.HouseScene;

import ass.example.components.PlayerComponent;
import ass.example.core.DeathReason;
import ass.example.system.DeathSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;

import static com.almasb.fxgl.dsl.FXGL.getb;

public class BathtubComponent extends Component {

    private final Entity player;
    private final DeathSystem deathSystem;
    private final DeathReason deathReason;

    private boolean playerInside = false;
    private boolean hasEnteredOnce = false;
    private boolean hasLeftAfterEnter = false;
    private boolean triggeredDeath = false;

    /*
     * 用來偵測死亡狀態是否從 true 回到 false。
     */
    private boolean wasPlayerDead = false;

    public BathtubComponent(
            Entity player,
            DeathSystem deathSystem,
            DeathReason deathReason
    ) {
        this.player = player;
        this.deathSystem = deathSystem;
        this.deathReason = deathReason;
    }

    @Override
    public void onUpdate(double tpf) {
        if (player == null || deathSystem == null) {
            return;
        }

        boolean playerDead = getb("playerDead");

        /*
         * 死亡中不偵測。
         */
        if (playerDead) {
            wasPlayerDead = true;
            return;
        }

        /*
         * 如果上一幀還是死亡狀態，現在已經不是死亡，
         * 代表玩家重生了，重置浴缸偵測器。
         */
        if (wasPlayerDead) {
            resetState();
            wasPlayerDead = false;
        }

        if (triggeredDeath) {
            return;
        }

        boolean nowInside = entity.isColliding(player);

        if (nowInside && !playerInside) {
            onPlayerEnterSensor();
        }

        if (!nowInside && playerInside) {
            onPlayerExitSensor();
        }

        playerInside = nowInside;
    }

    private void onPlayerEnterSensor() {
        if (!hasEnteredOnce) {
            hasEnteredOnce = true;
            return;
        }

        if (hasLeftAfterEnter) {
            triggeredDeath = true;
            deathSystem.die(deathReason);
        }
    }

    private void onPlayerExitSensor() {
        if (hasEnteredOnce) {
            hasLeftAfterEnter = true;
        }
    }

    private void resetState() {
        playerInside = false;
        hasEnteredOnce = false;
        hasLeftAfterEnter = false;
        triggeredDeath = false;
    }
}