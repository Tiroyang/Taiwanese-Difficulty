package ass.example.components.HouseScene;

import ass.example.components.LoadSaveComponent;
import ass.example.core.QuestType;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.QuestSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * QuiltComponent
 *
 * 家中場景的棉被互動 Component。
 *
 * 功能：
 * 1. 控制棉被是否已折好。
 * 2. 玩家互動後，將棉被貼圖從 messyTexture 切換成 foldedTexture。
 * 3. 玩家折棉被後，更新 game var：quiltFolded。
 * 4. 玩家折棉被後，完成 FOLD_QUILT 任務。
 * 5. 玩家折棉被後，播放折棉被音效。
 * 6. 玩家折棉被後，移除 trigger Entity，避免重複互動。
 * 7. 讀檔時根據 quiltFolded 還原棉被狀態。
 */
public class QuiltComponent extends Component implements LoadSaveComponent {

    // =========================================================
    // Game Var Keys
    // =========================================================

    /**
     * 棉被是否已折好的 game var key。
     *
     * SaveSystem 需要儲存這個值，讀檔後 applySavedState() 會根據此值還原棉被狀態。
     */
    private static final String VAR_QUILT_FOLDED = "quiltFolded";


    // =========================================================
    // Dependencies
    // =========================================================

    /**
     * 棉被的視覺 Entity。
     */
    private final Entity visualEntity;

    /**
     * 音效系統。
     *
     * 用於玩家折棉被時播放音效。
     */
    private final AudioSystem audioSystem = AudioSystem.getInstance();


    // =========================================================
    // Texture Settings
    // =========================================================

    /**
     * 棉被尚未折好時的貼圖。
     */
    private final String messyTexture;

    /**
     * 棉被折好後的貼圖。
     */
    private final String foldedTexture;


    // =========================================================
    // Runtime State
    // =========================================================

    /**
     * 棉被目前是否已折好。
     *
     * true：
     * - 顯示 foldedTexture。
     * - trigger 已被移除。
     *
     * false：
     * - 顯示 messyTexture。
     * - 玩家仍可互動折棉被。
     */
    private boolean folded = false;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立棉被 Component。
     *
     * @param visualEntity 棉被視覺 Entity
     * @param messyTexture 未折棉被貼圖
     * @param foldedTexture 已折棉被貼圖
     */
    public QuiltComponent(
            Entity visualEntity,
            String messyTexture,
            String foldedTexture
    ) {
        this.visualEntity = visualEntity;
        this.messyTexture = messyTexture;
        this.foldedTexture = foldedTexture;
    }


    // =========================================================
    // FXGL Lifecycle
    // =========================================================

    /**
     * Component 被加入 Entity 時呼叫。
     *
     * 預設狀態：
     * - 棉被尚未折好。
     * - 顯示 messyTexture。
     */
    @Override
    public void onAdded() {
        restoreMessyState();
    }


    // =========================================================
    // Save / Load
    // =========================================================

    /**
     * 套用存檔中的棉被狀態。
     *
     * 若 quiltFolded == true：
     * - 還原為已折好狀態。
     * - 顯示 foldedTexture。
     * - 移除互動 trigger。
     *
     * 若 quiltFolded == false：
     * - 還原為未折狀態。
     *
     * 這裡完整處理 true / false 兩種情況，
     * 避免重開場景或讀檔時狀態殘留。
     */
    @Override
    public void applySavedState() {
        if (getb(VAR_QUILT_FOLDED)) {
            restoreFoldedState();
        } else {
            restoreMessyState();
        }
    }


    // =========================================================
    // Public API
    // =========================================================

    /**
     * 玩家執行折棉被互動時呼叫。
     *
     * 流程：
     * 1. 若棉被已折好，直接返回。
     * 2. 將 folded 設為 true。
     * 3. 更新 game var：quiltFolded = true。
     * 4. 完成 FOLD_QUILT 任務。
     * 5. 播放折棉被音效。
     * 6. 切換成 foldedTexture。
     * 7. 移除互動 trigger，避免重複互動。
     */
    public void fold() {
        if (folded) {
            return;
        }

        folded = true;

        set(VAR_QUILT_FOLDED, true);

        completeFoldQuiltQuest();
        playFoldSound();

        showFoldedTexture();

        removeTriggerFromWorld();
    }


    // =========================================================
    // Restore State
    // =========================================================

    /**
     * 還原為未折棉被狀態。
     *
     * - 不播放音效。
     * - 不完成任務。
     * - 不移除 trigger。
     */
    private void restoreMessyState() {
        folded = false;
        showMessyTexture();
    }

    /**
     * 還原為已折棉被狀態。
     *
     * - 不播放音效。
     * - 不重複完成任務。
     * - 不更新 game var。
     */
    private void restoreFoldedState() {
        folded = true;

        showFoldedTexture();

        removeTriggerFromWorld();
    }


    // =========================================================
    // Quest / Audio
    // =========================================================

    /**
     * 完成折棉被任務。
     */
    private void completeFoldQuiltQuest() {
        QuestSystem.getInstance().completeQuest(QuestType.FOLD_QUILT);
    }

    /**
     * 播放折棉被音效。
     */
    private void playFoldSound() {
        if (audioSystem != null) {
            audioSystem.playSFX(SoundId.FOLDING_QUILT);
        }
    }


    // =========================================================
    // View Management
    // =========================================================

    /**
     * 顯示未折棉被貼圖。
     */
    private void showMessyTexture() {
        setVisualTexture(messyTexture);
    }

    /**
     * 顯示已折棉被貼圖。
     */
    private void showFoldedTexture() {
        setVisualTexture(foldedTexture);
    }

    /**
     * 更新棉被視覺 Entity 的貼圖。
     *
     * 這裡會先清除 visualEntity 原本的 view children，再加入新的貼圖。
     *
     * @param texturePath 貼圖路徑
     */
    private void setVisualTexture(String texturePath) {
        if (visualEntity == null) {
            return;
        }

        visualEntity.getViewComponent().clearChildren();
        visualEntity.getViewComponent().addChild(texture(texturePath));
    }


    // =========================================================
    // Entity Management
    // =========================================================

    /**
     * 移除目前掛載 QuiltComponent 的 trigger Entity。
     *
     * 棉被折好後不需要再互動，因此移除 trigger 可避免重複觸發。
     */
    private void removeTriggerFromWorld() {
        entity.removeFromWorld();
    }


    // =========================================================
    // Getters
    // =========================================================

    /**
     * 取得棉被目前是否已折好。
     *
     * @return true 表示棉被已折好
     */
    public boolean isFolded() {
        return folded;
    }
}