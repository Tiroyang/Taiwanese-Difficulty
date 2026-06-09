package ass.example.core;

/**
 * QuestType
 *
 * 遊戲任務類型列表。
 *
 * 每個任務包含：
 * 1. titleKey：
 *    - 任務標題語言 key。
 *
 * 2. requiredAmount：
 *    - 完成任務所需進度。
 *
 * QuestSystem 可依照 QuestType 管理：
 * - 任務進度
 * - 任務完成狀態
 * - 任務完成動畫是否播放
 */
public enum QuestType {
 
    // HouseScene - Morning Routine 

    /**
     * 折棉被。
     */
    FOLD_QUILT(
            "quest.fold_quilt",
            1
    ),

    /**
     * 刷牙。
     */
    BRUSH_TEETH(
            "quest.brush_teeth",
            1
    ),

    /**
     * 跟媽媽說話。
     */
    TALK_TO_MOM(
            "quest.talk_to_mom",
            1
    ),

    /**
     * 穿鞋。
     */
    WEAR_SHOES(
            "quest.wear_shoes",
            1
    ),

    /**
     * 離開家。
     */
    EXIT_HOUSE(
            "quest.exit_house",
            1
    );

 
    // Quest Settings 

    /**
     * 任務標題語言 key。
     */
    private final String titleKey;

    /**
     * 任務完成所需數量。
     */
    private final int requiredAmount;

 
    // Constructor 

    /**
     * 建立任務類型資料。
     *
     * @param titleKey 任務標題語言 key
     * @param requiredAmount 任務完成所需數量
     */
    QuestType(
            String titleKey,
            int requiredAmount
    ) {
        this.titleKey = titleKey;
        this.requiredAmount = requiredAmount;
    }

 
    // Getters 

    /**
     * 取得任務標題語言 key。
     *
     * @return 任務標題語言 key
     */
    public String getTitleKey() {
        return titleKey;
    }

    /**
     * 取得任務完成所需數量。
     *
     * @return 任務完成所需數量
     */
    public int getRequiredAmount() {
        return requiredAmount;
    }
}