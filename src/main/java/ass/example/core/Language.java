package ass.example.core;

/**
 * Language
 *
 * 遊戲支援的語言列表。
 *
 * 用途：
 * 1. 設定目前遊戲語言。
 * 2. 顯示在設定選單的語言下拉選單。
 * 3. 供 LanguageSystem 讀取對應語系文字。
 */
public enum Language {

    /**
     * 繁體中文。
     */
    ZH_TW("繁體中文"),

    /**
     * English。
     */
    EN_US("English");

 
    // Display Settings 

    /**
     * 顯示在 UI 上的語言名稱。
     */
    private final String displayName;

 
    // Constructor 

    /**
     * 建立語言資料。
     *
     * @param displayName UI 顯示名稱
     */
    Language(String displayName) {
        this.displayName = displayName;
    }

 
    // Getters 

    /**
     * 取得 UI 顯示名稱。
     *
     * @return 語言顯示名稱
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * ComboBox 等 JavaFX 控制項會呼叫 toString()
     * 取得顯示文字。
     *
     * @return 語言顯示名稱
     */
    @Override
    public String toString() {
        return displayName;
    }
}