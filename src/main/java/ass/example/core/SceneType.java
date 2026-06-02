package ass.example.core;

/**
 * SceneType
 *
 * 遊戲場景類型列表。
 *
 * 用途：
 * 1. SceneManager 判斷目前場景。
 * 2. SaveSystem 儲存目前玩家所在場景。
 * 3. LoadSystem 依照存檔決定要載入哪個場景。
 */
public enum SceneType {

    /**
     * 家中劇情場景。
     */
    HOUSE,

    /**
     * 街道劇情場景。
     */
    STREET,

    /**
     * 街道無盡模式。
     */
    STREET_ENDLESS
}