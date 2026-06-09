package ass.example.components.HouseScene;

import ass.example.components.LoadSaveComponent;
import ass.example.components.PlayerComponent;
import ass.example.core.QuestType;
import ass.example.core.SoundId;
import ass.example.system.AudioSystem;
import ass.example.system.QuestSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * ShoeComponent
 *
 * 家中場景的鞋櫃穿脫鞋 Component。
 *
 * 功能：
 * 1. 控制玩家目前是否穿鞋。
 * 2. 玩家互動後，可在「穿鞋」與「脫鞋」之間切換。
 * 3. 穿鞋時，將 shoesWorn game var 設為 true。
 * 4. 脫鞋時，將 shoesWorn game var 設為 false。
 * 5. 根據穿鞋狀態切換鞋櫃貼圖。
 * 6. 根據穿鞋狀態同步 PlayerComponent 的玩家外觀。
 * 7. 穿鞋時完成 WEAR_SHOES 任務。
 * 8. 穿鞋 / 脫鞋時播放裝備音效。
 * 9. 讀檔時根據 shoesWorn 還原鞋櫃與玩家狀態。
 */
public class ShoeComponent extends Component implements LoadSaveComponent {
 
    // Game Var Keys 

    /**
     * 玩家是否穿鞋的 game var key。
     *
     * SaveSystem 需要儲存這個值，讀檔後 applySavedState() 會根據此值還原狀態。
     */
    private static final String VAR_SHOES_WORN = "shoesWorn";

 
    // Dependencies 

    /**
     * 鞋櫃或鞋子視覺 Entity。
     */
    private final Entity shoeVisual;

    /**
     * 玩家 Entity。
     *
     * 用於同步 PlayerComponent 中的穿鞋狀態，
     * 讓玩家站立、走路等貼圖能切換成穿鞋 / 赤腳版本。
     */
    private final Entity player;

    /**
     * 音效系統。
     *
     * 用於穿鞋與脫鞋時播放音效。
     */
    private final AudioSystem audioSystem = AudioSystem.getInstance();

 
    // Texture Settings 

    /**
     * 預設鞋櫃貼圖。
     *
     * 代表鞋子仍在鞋櫃中，玩家尚未穿鞋。
     */
    private final String defaultTexture;

    /**
     * 穿鞋後的鞋櫃貼圖。
     *
     * 代表鞋子已被玩家穿走。
     */
    private final String wornTexture;

 
    // Runtime State 

    /**
     * 玩家目前是否穿鞋。
     *
     * true：
     * - shoesWorn game var 為 true。
     * - 鞋櫃顯示 wornTexture。
     * - PlayerComponent 顯示穿鞋外觀。
     *
     * false：
     * - shoesWorn game var 為 false。
     * - 鞋櫃顯示 defaultTexture。
     * - PlayerComponent 顯示赤腳外觀。
     */
    private boolean worn = false;

 
    // Constructor 

    /**
     * 建立鞋櫃穿脫鞋 Component。
     *
     * @param shoeVisual 鞋櫃視覺 Entity
     * @param player 玩家 Entity
     * @param defaultTexture 未穿鞋時的鞋櫃貼圖
     * @param wornTexture 穿鞋後的鞋櫃貼圖
     */
    public ShoeComponent(
            Entity shoeVisual,
            Entity player,
            String defaultTexture,
            String wornTexture
    ) {
        this.shoeVisual = shoeVisual;
        this.player = player;
        this.defaultTexture = defaultTexture;
        this.wornTexture = wornTexture;
    }

 
    // FXGL Lifecycle 

    /**
     * Component 被加入 Entity 時呼叫。
     *
     * 會先套用目前 game var 中的 shoesWorn 狀態。
     *
     * 用途：
     * - 第一次載入 HouseScene 時，通常 shoesWorn 會是 false。
     * - 讀檔或切回場景時，可以直接依照現有變數還原狀態。
     */
    @Override
    public void onAdded() {
        applySavedState();
    }

 
    // Save / Load 

    /**
     * 套用存檔中的穿鞋狀態。
     *
     * 若 shoesWorn == true：
     * - 還原為穿鞋狀態。
     * - 顯示 wornTexture。
     * - 同步玩家穿鞋外觀。
     *
     * 若 shoesWorn == false：
     * - 還原為未穿鞋狀態。
     * - 顯示 defaultTexture。
     * - 同步玩家赤腳外觀。
     */
    @Override
    public void applySavedState() {
        if (getb(VAR_SHOES_WORN)) {
            restoreWornState();
        } else {
            restoreDefaultState();
        }
    }

 
    // Public API 

    /**
     * 切換穿鞋狀態。
     *
     * 若目前已穿鞋：
     * - 執行 takeOff()。
     *
     * 若目前未穿鞋：
     * - 執行 wear()。
     */
    public void toggle() {
        if (worn) {
            takeOff();
        } else {
            wear();
        }
    }

    /**
     * 玩家穿鞋。
     *
     * 流程：
     * 1. 若已經穿鞋，直接返回。
     * 2. 切換成穿鞋狀態。
     * 3. 更新 game var。
     * 4. 切換鞋櫃貼圖。
     * 5. 同步 PlayerComponent 穿鞋外觀。
     * 6. 播放穿鞋音效。
     * 7. 完成 WEAR_SHOES 任務。
     */
    public void wear() {
        if (worn) {
            return;
        }

        setWornState(true);

        set(VAR_SHOES_WORN, true);

        showWornTexture();
        syncPlayerShoesState();

        playEquipSound();

        completeWearShoesQuest();
    }

    /**
     * 玩家脫鞋。
     *
     * 流程：
     * 1. 若目前未穿鞋，直接返回。
     * 2. 切換成未穿鞋狀態。
     * 3. 更新 game var。
     * 4. 切換鞋櫃貼圖。
     * 5. 同步 PlayerComponent 赤腳外觀。
     * 6. 播放脫鞋音效。
     *
     * 注意：
     * 脫鞋不會取消以完成的 WEAR_SHOES 任務。
     */
    public void takeOff() {
        if (!worn) {
            return;
        }

        setWornState(false);

        set(VAR_SHOES_WORN, false);

        showDefaultTexture();
        syncPlayerShoesState();

        playEquipSound();
    }

 
    // Restore State 

    /**
     * 還原為穿鞋狀態。
     *
     * 與 wear() 不同：
     * - 不播放音效。
     * - 不完成任務。
     * - 不更新 game var。
     *
     * 適合用於：
     * - 讀檔。
     * - 場景初始化後套用狀態。
     */
    private void restoreWornState() {
        setWornState(true);

        showWornTexture();
        syncPlayerShoesState();
    }

    /**
     * 還原為未穿鞋狀態。
     *
     * 與 takeOff() 不同：
     * - 不播放音效。
     * - 不更新 game var。
     *
     * 適合用於：
     * - 第一次載入場景。
     * - 讀檔。
     * - HouseScene 重生後重置狀態。
     */
    private void restoreDefaultState() {
        setWornState(false);

        showDefaultTexture();
        syncPlayerShoesState();
    }

 
    // State 

    /**
     * 設定內部穿鞋狀態。
     *
     * 這個方法只改變 Component 內部的 worn 欄位，
     * 不會更新 game var，也不會切換貼圖或播放音效。
     *
     * @param worn true 表示穿鞋，false 表示未穿鞋
     */
    private void setWornState(boolean worn) {
        this.worn = worn;
    }

 
    // Quest / Audio 

    /**
     * 完成穿鞋任務。
     */
    private void completeWearShoesQuest() {
        QuestSystem.getInstance().completeQuest(QuestType.WEAR_SHOES);
    }

    /**
     * 播放穿鞋 / 脫鞋音效。
     *
     * 若 audioSystem 為 null，則不播放。
     */
    private void playEquipSound() {
        if (audioSystem != null) {
            audioSystem.playSFX(SoundId.EQUIP);
        }
    }

 
    // View Management 

    /**
     * 顯示未穿鞋時的鞋櫃貼圖。
     */
    private void showDefaultTexture() {
        setVisualTexture(defaultTexture);
    }

    /**
     * 顯示穿鞋後的鞋櫃貼圖。
     */
    private void showWornTexture() {
        setVisualTexture(wornTexture);
    }

    /**
     * 更新鞋櫃視覺 Entity 的貼圖。
     *
     * 這裡會先清除 shoeVisual 原本的 view children，
     * 再加入新的貼圖。
     *
     * @param texturePath 貼圖路徑
     */
    private void setVisualTexture(String texturePath) {
        if (shoeVisual == null) {
            return;
        }

        shoeVisual.getViewComponent().clearChildren();
        shoeVisual.getViewComponent().addChild(texture(texturePath));
    }

 
    // Player Visual Sync 

    /**
     * 同步玩家的穿鞋狀態。
     *
     * PlayerComponent 會根據 shoesWorn 狀態決定要顯示穿鞋或赤腳版本的站立、走路圖片。
     */
    private void syncPlayerShoesState() {
        if (player == null || !player.hasComponent(PlayerComponent.class)) {
            return;
        }

        player.getComponent(PlayerComponent.class).setShoesWorn(worn);
    }

 
    // Getters 

    /**
     * 取得玩家目前是否穿鞋。
     *
     * @return true 表示玩家目前穿鞋
     */
    public boolean isWorn() {
        return worn;
    }
}