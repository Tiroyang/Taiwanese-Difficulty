package ass.example.components;

import ass.example.core.DeathReason;
import com.almasb.fxgl.entity.component.Component;

/**
 * LethalComponent
 *
 * 致命物件 Component。
 *
 * 功能：
 * 1. 標記某個 Entity 是會造成玩家死亡的物件。
 * 2. 保存該 Entity 對應的死亡原因 DeathReason。
 * 3. 讓死亡判定系統可以透過此 Component 取得死亡原因。
 *
 * 常見使用情境：
 *
 * 1. 死亡牆：
 *
 * spawn("death_wall", new SpawnData(x, y)
 *         .put("width", width)
 *         .put("height", height)
 *         .put("deathReason", DeathReason.HIT_CEILING));
 *
 * 2. 碰撞系統偵測到玩家撞到此 Entity 時：
 *
 * DeathReason reason = lethalComponent.getDeathReason();
 * deathSystem.die(reason);
 */
public class LethalComponent extends Component {

    // =========================================================
    // Death Settings
    // =========================================================

    /**
     * 此致命物件對應的死亡原因。
     */
    private final DeathReason deathReason;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立致命物件 Component。
     *
     * @param deathReason 此物件造成的死亡原因
     */
    public LethalComponent(DeathReason deathReason) {
        this.deathReason = deathReason;
    }


    // =========================================================
    // Getters
    // =========================================================

    /**
     * 取得此物件造成的死亡原因。
     *
     * @return 死亡原因
     */
    public DeathReason getDeathReason() {
        return deathReason;
    }

    /**
     * 取得死亡原因 ID。
     *
     * 這通常用於：
     * - 顯示死亡畫面。
     * - 查詢語言 key。
     * - 儲存或除錯死亡原因。
     *
     * @return DeathReason 的 ID
     */
    public String getDeathId() {
        return deathReason.getId();
    }
}