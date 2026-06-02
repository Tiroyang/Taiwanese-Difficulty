package ass.example.core;

/**
 * WindowMode
 *
 * 遊戲視窗模式。
 *
 * 用途：
 * 1. 設定選單顯示視窗模式選項。
 * 2. WindowSystem 根據模式切換視窗大小或全螢幕。
 * 3. LanguageSystem 根據 textKey 顯示目前語言文字。
 */
public enum WindowMode {

    /**
     * 預設視窗大小。
     */
    DEFAULT(
            "menu.settings.window.defaultSize"
    ),

    /**
     * 自訂視窗大小。
     */
    CUSTOM(
            "menu.settings.window.customSize"
    ),

    /**
     * 全螢幕。
     */
    FULLSCREEN(
            "menu.settings.window.fullscreenSize"
    );


    // =========================================================
    // Text Settings
    // =========================================================

    /**
     * UI 顯示文字語言 key。
     */
    private final String textKey;


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立視窗模式資料。
     *
     * @param textKey UI 文字語言 key
     */
    WindowMode(String textKey) {
        this.textKey = textKey;
    }


    // =========================================================
    // Getters
    // =========================================================

    /**
     * 取得 UI 文字語言 key。
     *
     * @return 語言 key
     */
    public String getTextKey() {
        return textKey;
    }
}