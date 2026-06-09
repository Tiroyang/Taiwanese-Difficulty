package ass.example.components.HouseScene;

import ass.example.core.DeathReason;
import ass.example.system.DeathSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;
import static com.almasb.fxgl.dsl.FXGL.getb;

/**
 * BathtubComponent
 *
 * 浴缸死亡判定元件。
 *
 * 功能：
 * 1. 偵測玩家是否進入浴缸 sensor。
 * 2. 玩家高速落入浴缸時觸發死亡。
 * 3. 玩家高速水平撞入浴缸時觸發死亡。
 * 4. 玩家死亡期間暫停偵測。
 * 5. 玩家重生後重置觸發狀態，讓浴缸可以再次造成死亡。
 */
public class BathtubComponent extends Component {
 
    // Constants 

    /**
     * 預設垂直下墜死亡速度門檻。
     */
    private static final double DEFAULT_FALL_DEATH_SPEED_THRESHOLD = 520.0;

    /**
     * 水平撞擊死亡速度門檻。
     */
    private static final double HORIZONTAL_DEATH_SPEED_THRESHOLD = 260.0;

 
    // Dependencies 

    /**
     * 玩家 Entity。
     */
    private final Entity player;

    /**
     * 死亡系統。
     */
    private final DeathSystem deathSystem = DeathSystem.getInstance();

    /**
     * 浴缸造成的死亡原因。
     */
    private final DeathReason deathReason;

 
    // Settings 

    /**
     * 垂直下墜死亡速度門檻。
     *
     * 可透過建構子自訂。
     */
    private final double fallDeathSpeedThreshold;

 
    // Runtime State 

    /**
     * 是否已經觸發過死亡。
     *
     * 避免玩家停留在浴缸 sensor 內時，每一幀都重複呼叫 deathSystem.die()。
     */
    private boolean deathTriggered = false;

    /**
     * 前一幀玩家是否處於死亡狀態。
     *
     * 用於判斷玩家是否剛完成重生。
     */
    private boolean wasPlayerDeadLastFrame = false;

 
    // Constructors 

    /**
     * 使用自訂速度門檻建立浴缸元件。
     *
     * @param player 玩家 Entity
     * @param deathReason 浴缸死亡原因
     * @param fallDeathSpeedThreshold 垂直下墜死亡速度門檻
     */
    public BathtubComponent(
            Entity player,
            DeathReason deathReason,
            double fallDeathSpeedThreshold
    ) {
        this.player = player;
        this.deathReason = deathReason;
        this.fallDeathSpeedThreshold = fallDeathSpeedThreshold;
    }

 
    // FXGL Lifecycle 

    /**
     * 每幀更新浴缸死亡判定。
     *
     * 判定流程：
     * 1. 檢查必要物件是否存在。
     * 2. 處理玩家死亡與重生狀態。
     * 3. 若玩家已經死亡，本幀停止偵測。
     * 4. 若浴缸已經觸發過死亡，本幀停止偵測。
     * 5. 若玩家不在浴缸 sensor 裡，本幀停止偵測。
     * 6. 取得玩家速度。
     * 7. 判斷速度是否達到死亡條件。
     * 8. 若符合條件，觸發死亡。
     *
     * @param tpf time per frame
     */
    @Override
    public void onUpdate(double tpf) {
        if (hasInvalidDependencies()) {
            return;
        }

        if (isPlayerDeadOrJustRespawned()) {
            return;
        }

        if (deathTriggered) {
            return;
        }

        if (!isPlayerInsideBathtub()) {
            return;
        }

        PhysicsComponent physics = player.getComponent(PhysicsComponent.class);

        double velocityX = physics.getVelocityX();
        double velocityY = physics.getVelocityY();

        if (isImpactDeadly(velocityX, velocityY)) {
            triggerDeath();
        }
    }

 
    // Validation 

    /**
     * 檢查必要相依物件是否有效。
     *
     * @return true 表示缺少必要物件，本幀不應繼續偵測
     */
    private boolean hasInvalidDependencies() {
        return player == null || deathSystem == null;
    }

 
    // Player Death State 

    /**
     * 處理玩家死亡與重生狀態。
     *
     * 回傳 true 代表本幀應停止浴缸偵測。
     *
     * 情況一：玩家正在死亡
     * - 記錄玩家上一幀死亡狀態。
     * - 停止浴缸偵測。
     *
     * 情況二：玩家剛重生
     * - wasPlayerDeadLastFrame 原本為 true。
     * - 但目前 playerDead 已經變成 false。
     * - 代表死亡流程結束，玩家已重生。
     * - 重置浴缸狀態，允許之後再次觸發死亡。
     *
     * @return true 表示本幀停止偵測
     */
    private boolean isPlayerDeadOrJustRespawned() {
        boolean playerDead = getb("playerDead");

        if (playerDead) {
            wasPlayerDeadLastFrame = true;
            return true;
        }

        if (wasPlayerDeadLastFrame) {
            resetAfterRespawn();
            wasPlayerDeadLastFrame = false;
        }

        return false;
    }

 
    // Bathtub Detection 

    /**
     * 判斷玩家是否位於浴缸 sensor 內。
     *
     * 這裡的 entity 代表掛載此 Component 的浴缸 sensor Entity。
     *
     * @return true 表示玩家正在浴缸感應區內
     */
    private boolean isPlayerInsideBathtub() {
        return entity.isColliding(player);
    }

    /**
     * 判斷玩家目前速度是否達到浴缸死亡條件。
     *
     * 死亡條件：
     * 1. 垂直向下速度 >= fallDeathSpeedThreshold
     * 2. 水平速度絕對值 > HORIZONTAL_DEATH_SPEED_THRESHOLD
     *
     * velocityY：
     * - 通常正值代表向下移動。
     * - 使用 Math.max(0, velocityY)，避免玩家向上跳時被誤判。
     *
     * velocityX：
     * - 左右方向都可能撞入浴缸。
     * - 因此使用 Math.abs(velocityX)。
     *
     * @param velocityX 玩家水平速度
     * @param velocityY 玩家垂直速度
     * @return true 表示撞擊速度足以觸發死亡
     */
    private boolean isImpactDeadly(double velocityX, double velocityY) {
        double fallingSpeed = Math.max(0, velocityY);
        double horizontalSpeed = Math.abs(velocityX);

        boolean fallingTooFast = fallingSpeed >= fallDeathSpeedThreshold;
        boolean crashingTooFast = horizontalSpeed > HORIZONTAL_DEATH_SPEED_THRESHOLD;

        return fallingTooFast || crashingTooFast;
    }

 
    // Death Trigger 

    /**
     * 觸發浴缸死亡。
     *
     * 先將 deathTriggered 設為 true，
     * 避免死亡流程尚未完成前重複觸發。
     */
    private void triggerDeath() {
        deathTriggered = true;
        deathSystem.die(deathReason);
    }

 
    // Reset 

    /**
     * 玩家重生後重置浴缸狀態。
     *
     * 目前只需要重置 deathTriggered。
     * 之後若浴缸有新增動畫、音效、提示等狀態，
     * 也可以集中在這裡一起重置。
     */
    private void resetAfterRespawn() {
        deathTriggered = false;
    }
}