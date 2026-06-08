package ass.example.components;

/**
 * LoadSaveComponent
 *
 * 可套用存檔狀態的 Component 介面。
 *
 * 功能：
 * 讓場景或存檔系統可以統一呼叫：
 *
 * component.applySavedState();
 *
 * 來還原該 Component 的狀態。
 *
 * ------------------------------------------------------------
 * 使用情境
 * ------------------------------------------------------------
 *
 * 例如 HouseScene 讀檔後，會掃描場景中所有 Entity：
 *
 * for (Component component : entity.getComponents()) {
 *     if (component instanceof LoadSaveComponent loadSaveComponent) {
 *         loadSaveComponent.applySavedState();
 *     }
 * }
 *
 * 只要某個 Component 實作 LoadSaveComponent，就能被場景統一還原狀態。
 *
 * ------------------------------------------------------------
 * 實作類別
 * ------------------------------------------------------------
 *
 * 1. DoorComponent
 *    - 根據 door_{id}_opened 還原門的開關狀態。
 *
 * 2. QuiltComponent
 *    - 根據 quiltFolded 還原棉被是否已折。
 *
 * 3. WaterComponent
 *    - 根據 waterDrunk 還原水是否已被喝掉。
 *
 * 4. ShoeComponent
 *    - 根據 shoesWorn 還原玩家是否穿鞋。
 */
public interface LoadSaveComponent {

    /**
     * 套用目前 game vars 或存檔資料中的狀態。
     *
     * 實作時通常會：
     * 1. 讀取 FXGL game vars。
     * 2. 還原貼圖或外觀。
     * 3. 還原內部 boolean 狀態。
     * 4. 視需要移除已完成互動的 trigger。
     *
     * 注意：
     * 此方法不應播放音效或觸發新的遊戲事件。
     */
    void applySavedState();
}