package ass.example.components.HouseScene;

import ass.example.core.DeathReason;
import ass.example.system.DeathSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;

import static com.almasb.fxgl.dsl.FXGL.getb;

public class BathtubComponent extends Component {

    private final Entity player;
    private final DeathSystem deathSystem;
    private final DeathReason deathReason;

    /*
     * 速度門檻。
     * FXGL velocity 通常是 px/s。
     * 例如：
     * - 250：慢走也可能觸發
     * - 450：跳落或高速移動才觸發
     * - 600：比較高衝擊才觸發
     */
    private final double deathSpeedThreshold;

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
        this(player, deathSystem, deathReason, 520.0);
    }

    public BathtubComponent(
            Entity player,
            DeathSystem deathSystem,
            DeathReason deathReason,
            double deathSpeedThreshold
    ) {
        this.player = player;
        this.deathSystem = deathSystem;
        this.deathReason = deathReason;
        this.deathSpeedThreshold = deathSpeedThreshold;
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
         * 重生後重置。
         */
        if (wasPlayerDead) {
            resetState();
            wasPlayerDead = false;
        }

        if (triggeredDeath) {
            return;
        }

        /*
         * 不在浴缸 sensor 裡，不判斷。
         */
        if (!entity.isColliding(player)) {
            return;
        }

        PhysicsComponent physics = player.getComponent(PhysicsComponent.class);

        double vx = physics.getVelocityX();
        double vy = physics.getVelocityY();

        if (Math.max(0, vy) >= deathSpeedThreshold || Math.abs(vx) > 260) {
            triggeredDeath = true;
            deathSystem.die(deathReason);
        }
    }

    private void resetState() {
        triggeredDeath = false;
    }
}