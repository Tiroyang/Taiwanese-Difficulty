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

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * WaterComponent
 *
 * 家中場景的喝水互動 Component。
 *
 * 功能：
 * 1. 玩家與水互動後，視為喝水。
 * 2. 喝水後將 waterDrunk game var 設為 true。
 * 3. 喝水後移除水的視覺 Entity。
 * 4. 喝水後移除目前互動 trigger。
 * 5. 喝水後播放音效。
 * 6. 喝水後暫停玩家移動與控制。
 * 7. 延遲一段時間後觸發死亡。
 * 8. 讀檔時若 waterDrunk == true，會還原成水已被喝掉的狀態。
 *
 * 結構通常是：
 *
 * 1. water
 *    - 純視覺 Entity。
 *    - 顯示水。
 *
 * 2. water_trigger
 *    - 互動用 Entity。
 *    - 掛載 WaterComponent。
 *    - 玩家互動後會移除 water 與 water_trigger。
 *
 * 注意：
 * 這個 Component 的死亡設計是「互動後延遲死亡」。
 * 因此 drink() 不是單純喝水，而是完整處理死亡前的演出流程。
 */
public class WaterComponent extends Component implements LoadSaveComponent {

    // =========================================================
    // Game Var Keys
    // =========================================================

    /**
     * 水是否已經被喝掉的 game var key。
     *
     * SaveSystem 需要儲存這個值。
     * 讀檔後 applySavedState() 會根據此值還原水的狀態。
     */
    private static final String VAR_WATER_DRUNK = "waterDrunk";


    // =========================================================
    // Timing
    // =========================================================

    /**
     * 喝水後延遲死亡的秒數。
     *
     * 用途：
     * 讓玩家先看到喝水 / 停頓效果，
     * 再進入死亡流程。
     */
    private static final double DEATH_DELAY_SECONDS = 1.6;


    // =========================================================
    // Death Settings
    // =========================================================

    /**
     * 喝水後觸發的死亡原因。
     */
    private static final DeathReason WATER_DEATH_REASON = DeathReason.DRINK_WATER;


    // =========================================================
    // Dependencies
    // =========================================================

    /**
     * 水的視覺 Entity。
     *
     * 此 Component 通常掛在 water_trigger 上，
     * 真正顯示水圖片的是 visualEntity。
     */
    private final Entity visualEntity;

    /**
     * 玩家 Entity。
     *
     * 喝水後會暫停玩家移動與操作。
     */
    private final Entity player;

    /**
     * 死亡系統。
     *
     * 延遲結束後，透過 deathSystem.die(...) 觸發死亡。
     */
    private final DeathSystem deathSystem;

    /**
     * 音效系統。
     *
     * 用於喝水時播放音效。
     */
    private final AudioSystem audioSystem;


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * 水是否已被使用。
     *
     * true：
     * - 水已被喝掉。
     * - visualEntity 已被移除。
     * - trigger Entity 已被移除。
     * - 不可再次互動。
     *
     * false：
     * - 玩家仍可與水互動。
     */
    private boolean used = false;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立喝水 Component。
     *
     * @param visualEntity 水的視覺 Entity
     * @param player 玩家 Entity
     * @param deathSystem 死亡系統
     * @param audioSystem 音效系統，可為 null
     */
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


    // =========================================================
    // Save / Load
    // =========================================================

    /**
     * 套用存檔中的喝水狀態。
     *
     * 若 waterDrunk == true：
     * - 還原為水已被喝掉。
     * - 移除水視覺 Entity。
     * - 移除互動 trigger。
     *
     * 若 waterDrunk == false：
     * - 保持可互動狀態。
     *
     * 注意：
     * 這裡不需要播放音效、不需要停用玩家控制，
     * 也不需要重新觸發死亡。
     */
    @Override
    public void applySavedState() {
        if (getb(VAR_WATER_DRUNK)) {
            restoreDrunkState();
        }
    }


    // =========================================================
    // Public API
    // =========================================================

    /**
     * 玩家執行喝水互動時呼叫。
     *
     * 流程：
     * 1. 若水已被使用，直接返回。
     * 2. 標記水已使用。
     * 3. 更新 game var：waterDrunk = true。
     * 4. 移除水視覺與 trigger。
     * 5. 播放喝水音效。
     * 6. 停止玩家移動並禁用控制。
     * 7. 延遲後觸發死亡。
     */
    public void drink() {
        if (used) {
            return;
        }

        setUsedState(true);
        set(VAR_WATER_DRUNK, true);

        removeWaterEntities();

        playDrinkSound();

        disablePlayerControl();

        scheduleWaterDeath();
    }


    // =========================================================
    // Restore State
    // =========================================================

    /**
     * 還原為已喝水狀態。
     *
     * 與 drink() 不同：
     * - 不播放音效。
     * - 不禁用玩家控制。
     * - 不觸發死亡。
     * - 不更新 game var。
     *
     * 適合用於：
     * - 讀檔。
     * - 場景初始化後套用狀態。
     */
    private void restoreDrunkState() {
        setUsedState(true);
        removeWaterEntities();
    }


    // =========================================================
    // State
    // =========================================================

    /**
     * 設定內部使用狀態。
     *
     * 這個方法只改 used 欄位，
     * 不會更新 game var，也不會移除 Entity。
     *
     * @param used true 表示水已被使用
     */
    private void setUsedState(boolean used) {
        this.used = used;
    }


    // =========================================================
    // Player Control
    // =========================================================

    /**
     * 停止玩家移動並禁用控制。
     *
     * 若 player 為 null，
     * 或玩家沒有 PlayerComponent，
     * 則不做任何事。
     */
    private void disablePlayerControl() {
        if (player == null || !player.hasComponent(PlayerComponent.class)) {
            return;
        }

        PlayerComponent playerComponent = player.getComponent(PlayerComponent.class);

        playerComponent.stopAllMovement();
        playerComponent.setControlEnabled(false);
    }


    // =========================================================
    // Death
    // =========================================================

    /**
     * 排程喝水後的延遲死亡。
     *
     * 延遲 DEATH_DELAY_SECONDS 秒後，
     * 呼叫 deathSystem.die(WATER_DEATH_REASON)。
     */
    private void scheduleWaterDeath() {
        PauseTransition delay = new PauseTransition(
                Duration.seconds(DEATH_DELAY_SECONDS)
        );

        delay.setOnFinished(event -> triggerWaterDeath());
        delay.play();
    }

    /**
     * 觸發喝水死亡。
     *
     * 若 deathSystem 為 null，則不做任何事。
     */
    private void triggerWaterDeath() {
        if (deathSystem != null) {
            deathSystem.die(WATER_DEATH_REASON);
        }
    }


    // =========================================================
    // Entity Management
    // =========================================================

    /**
     * 移除水相關 Entity。
     *
     * 會移除：
     * 1. visualEntity：水的視覺物件。
     * 2. entity：目前掛載 WaterComponent 的 trigger。
     */
    private void removeWaterEntities() {
        removeVisualEntity();
        removeTriggerEntity();
    }

    /**
     * 移除水的視覺 Entity。
     */
    private void removeVisualEntity() {
        if (visualEntity != null) {
            visualEntity.removeFromWorld();
        }
    }

    /**
     * 移除喝水互動 trigger。
     */
    private void removeTriggerEntity() {
        entity.removeFromWorld();
    }


    // =========================================================
    // Audio
    // =========================================================

    /**
     * 播放喝水音效。
     *
     * 目前沿用 SoundId.EATING。
     * 如果之後有專用喝水音效，可在這裡替換。
     */
    private void playDrinkSound() {
        if (audioSystem != null) {
            audioSystem.playSFX(SoundId.EATING);
        }
    }


    // =========================================================
    // Getters
    // =========================================================

    /**
     * 取得水是否已被使用。
     *
     * @return true 表示水已經被喝掉
     */
    public boolean isUsed() {
        return used;
    }
}