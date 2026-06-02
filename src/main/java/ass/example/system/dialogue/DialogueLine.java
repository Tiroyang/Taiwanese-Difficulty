package ass.example.system.dialogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DialogueLine
 *
 * 單一句對話資料。
 *
 * 功能：
 * 1. 保存對話 ID。
 * 2. 保存角色立繪。
 * 3. 保存角色名稱語言 key。
 * 4. 保存對話文字語言 key。
 * 5. 控制是否允許點擊進入下一句。
 * 6. 保存下一句對話 ID。
 * 7. 判斷此句是否為對話結束句。
 * 8. 保存此句結束時要執行的事件。
 * 9. 保存此句可選的選項按鈕。
 *
 * 設計說明：
 * - DialogueLine 是資料類別，不負責顯示 UI。
 * - DialogueUI 負責讀取 DialogueLine 並顯示。
 * - DialogueSystem 負責切換 DialogueLine。
 */
public class DialogueLine {

    // =========================================================
    // Basic Data
    // =========================================================

    /**
     * 對話 ID。
     *
     * 例如：
     * - mom_001
     * - mom_002
     */
    private final String id;

    /**
     * 預設立繪路徑。
     *
     * 通常是角色沒有說話時的表情。
     */
    private final String defaultPortraitPath;

    /**
     * 說話中立繪路徑。
     *
     * 通常是角色正在說話時的表情。
     */
    private final String speakingPortraitPath;

    /**
     * 角色名稱語言 key。
     *
     * 若為 null，可代表此句不顯示角色名稱。
     */
    private final String characterNameKey;

    /**
     * 對話文字語言 key。
     */
    private final String textKey;


    // =========================================================
    // Flow Settings
    // =========================================================

    /**
     * 是否允許玩家點擊進入下一句。
     *
     * 若此句有選項，通常設為 false。
     */
    private final boolean allowClickNext;

    /**
     * 下一句對話 ID。
     *
     * 若為 null，代表沒有指定下一句。
     */
    private final String nextId;

    /**
     * 此句結束後是否直接結束整段對話。
     */
    private final boolean endDialogue;


    // =========================================================
    // Runtime Callback
    // =========================================================

    /**
     * 此句結束時要執行的事件。
     *
     * 例如：
     * - 完成任務
     * - 觸發小遊戲
     * - 觸發死亡
     */
    private Runnable onFinish;


    // =========================================================
    // Buttons
    // =========================================================

    /**
     * 此句對話的選項按鈕。
     */
    private final List<DialogueButton> buttons = new ArrayList<>();


    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 建立對話行。
     *
     * @param id 對話 ID
     * @param defaultPortraitPath 預設立繪路徑
     * @param speakingPortraitPath 說話立繪路徑
     * @param characterNameKey 角色名稱語言 key
     * @param textKey 對話文字語言 key
     * @param allowClickNext 是否允許點擊下一句
     * @param nextId 下一句 ID
     * @param endDialogue 是否結束對話
     */
    public DialogueLine(
            String id,
            String defaultPortraitPath,
            String speakingPortraitPath,
            String characterNameKey,
            String textKey,
            boolean allowClickNext,
            String nextId,
            boolean endDialogue
    ) {
        this.id = id;
        this.defaultPortraitPath = defaultPortraitPath;
        this.speakingPortraitPath = speakingPortraitPath;
        this.characterNameKey = characterNameKey;
        this.textKey = textKey;
        this.allowClickNext = allowClickNext;
        this.nextId = nextId;
        this.endDialogue = endDialogue;
    }


    // =========================================================
    // Builder Methods
    // =========================================================

    /**
     * 設定此句結束時要執行的事件。
     *
     * @param onFinish 結束事件
     * @return this，方便鏈式呼叫
     */
    public DialogueLine onFinish(Runnable onFinish) {
        this.onFinish = onFinish;
        return this;
    }

    /**
     * 新增選項按鈕。
     *
     * @param button 選項按鈕
     * @return this，方便鏈式呼叫
     */
    public DialogueLine addButton(DialogueButton button) {
        if (button != null) {
            buttons.add(button);
        }

        return this;
    }

    /**
     * 直接用 textKey 與 action 新增選項按鈕。
     *
     * @param textKey 選項文字語言 key
     * @param action 選項行為
     * @return this，方便鏈式呼叫
     */
    public DialogueLine addButton(
            String textKey,
            Runnable action
    ) {
        return addButton(new DialogueButton(textKey, action));
    }


    // =========================================================
    // Runtime Methods
    // =========================================================

    /**
     * 執行此句結束事件。
     */
    public void runOnFinish() {
        if (onFinish != null) {
            onFinish.run();
        }
    }

    /**
     * 是否有選項按鈕。
     *
     * @return true 表示此句有選項
     */
    public boolean hasButtons() {
        return !buttons.isEmpty();
    }


    // =========================================================
    // Getters
    // =========================================================

    public String getId() {
        return id;
    }

    public String getDefaultPortraitPath() {
        return defaultPortraitPath;
    }

    public String getSpeakingPortraitPath() {
        return speakingPortraitPath;
    }

    public String getCharacterNameKey() {
        return characterNameKey;
    }

    public String getTextKey() {
        return textKey;
    }

    public boolean isAllowClickNext() {
        return allowClickNext;
    }

    public String getNextId() {
        return nextId;
    }

    public boolean isEndDialogue() {
        return endDialogue;
    }

    /**
     * 取得不可直接修改的按鈕清單。
     *
     * 若外部需要新增按鈕，應使用 addButton(...)。
     *
     * @return 按鈕清單
     */
    public List<DialogueButton> getButtons() {
        return Collections.unmodifiableList(buttons);
    }


    // =========================================================
    // Nested Class - DialogueButton
    // =========================================================

    /**
     * DialogueButton
     *
     * 對話選項按鈕資料。
     *
     * 原本是獨立 DialogueButton.java，
     * 現在合併到 DialogueLine 中，因為它只服務 DialogueLine。
     */
    public static class DialogueButton {

        /**
         * 選項文字語言 key。
         */
        private final String textKey;

        /**
         * 選項被按下時執行的行為。
         */
        private final Runnable action;

        /**
         * 建立對話選項按鈕。
         *
         * @param textKey 選項文字語言 key
         * @param action 選項行為
         */
        public DialogueButton(
                String textKey,
                Runnable action
        ) {
            this.textKey = textKey;
            this.action = action;
        }

        /**
         * 取得選項文字語言 key。
         *
         * @return 選項文字語言 key
         */
        public String getTextKey() {
            return textKey;
        }

        /**
         * 執行選項行為。
         */
        public void run() {
            if (action != null) {
                action.run();
            }
        }
    }
}