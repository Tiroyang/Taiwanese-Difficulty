package ass.example.ui.save;

/**
 * SaveMenuMode
 *
 * 存檔面板模式。
 *
 * LOAD：
 * - 讀取存檔。
 * - 點擊已有存檔槽後載入。
 *
 * EDIT：
 * - 編輯存檔。
 * - 點擊已有存檔槽後可重新命名或刪除。
 *
 * SAVE_TO：
 * - 儲存到指定槽位。
 * - 點擊槽位後輸入名稱並儲存。
 */
public enum SaveMenuMode {
    LOAD,
    EDIT,
    SAVE_TO
}