package ass.example.system;

import ass.example.core.QuestType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * QuestSystem
 *
 * 故事任務系統。
 *
 * 功能：
 * 1. 保存故事模式任務順序。
 * 2. 保存每個 QuestType 對應的 QuestState。
 * 3. 重置任務進度。
 * 4. 完成任務。
 * 5. 增加任務進度。
 * 6. 判斷任務是否完成。
 * 7. 判斷所有故事任務是否完成。
 * 8. 管理 QuestHUD 目前可見任務。
 * 9. 管理任務完成動畫播放狀態。
 */
public class QuestSystem {
 
    // Singleton 

    /**
     * QuestSystem 單例。
     */
    private static final QuestSystem INSTANCE = new QuestSystem();

    /**
     * 取得 QuestSystem 單例。
     *
     * @return QuestSystem
     */
    public static QuestSystem getInstance() {
        return INSTANCE;
    }

 
    // Quest Display Settings 

    /**
     * 預設同時顯示幾個任務。
     */
    private static final int DEFAULT_VISIBLE_QUEST_COUNT = 1;

 
    // Quest Data 

    /**
     * 故事任務順序。
     *
     * QuestHUD 會依照此順序顯示任務。
     */
    private final List<QuestType> storyQuests = new ArrayList<>();

    /**
     * 每個任務對應的狀態。
     *
     * 使用 EnumMap：
     * - key 是 enum 時，EnumMap 比 HashMap 更適合。
     */
    private final Map<QuestType, QuestState> states =
            new EnumMap<>(QuestType.class);

 
    // Runtime UI State 

    /**
     * 目前 QuestHUD 從第幾個任務開始顯示。
     */
    private int visibleStartIndex = 0;

    /**
     * QuestHUD 同時顯示幾個任務。
     */
    private int visibleQuestCount = DEFAULT_VISIBLE_QUEST_COUNT;

 
    // Constructor 

    /**
     * 建立任務系統。
     *
     * private：
     * - 確保外部只能透過 getInstance() 使用同一份 QuestSystem。
     */
    private QuestSystem() {
        registerStoryQuests();
        resetRuntimeState();
    }

 
    // Quest Registration 

    /**
     * 註冊故事任務順序。
     *
     * 若之後要新增故事任務，需要在這裡調整順序。
     */
    private void registerStoryQuests() {
        storyQuests.clear();

        storyQuests.add(QuestType.FOLD_QUILT);
        storyQuests.add(QuestType.BRUSH_TEETH);
        storyQuests.add(QuestType.TALK_TO_MOM);
        storyQuests.add(QuestType.WEAR_SHOES);
        storyQuests.add(QuestType.EXIT_HOUSE);
    }

 
    // Runtime Reset 

    /**
     * 重置任務執行狀態。
     *
     * 用途：
     * - 新遊戲開始。
     * - 回到故事模式起點。
     *
     * 注意：
     * - 會清除所有任務進度。
     * - 會把 QuestHUD 顯示起點重置為 0。
     */
    public void resetRuntimeState() {
        states.clear();

        for (QuestType quest : storyQuests) {
            states.put(quest, new QuestState());
        }

        visibleStartIndex = 0;
        visibleQuestCount = DEFAULT_VISIBLE_QUEST_COUNT;
    }

 
    // Quest Progress 

    /**
     * 直接完成指定任務。
     *
     * @param quest 任務類型
     * @return true 表示本次成功完成；false 表示任務不存在或本來已完成
     */
    public boolean completeQuest(QuestType quest) {
        QuestState state = getState(quest);

        if (state == null || state.isCompleted()) {
            return false;
        }

        state.setAmount(quest.getRequiredAmount());
        state.setCompleted(true);

        return true;
    }

    /**
     * 增加指定任務進度。
     *
     * 若進度達到 requiredAmount，任務會自動完成。
     *
     * @param quest 任務類型
     * @param amount 增加進度
     * @return true 表示本次增加後任務完成；false 表示尚未完成或無法增加
     */
    public boolean addQuestProgress(
            QuestType quest,
            int amount
    ) {
        QuestState state = getState(quest);

        if (state == null || state.isCompleted()) {
            return false;
        }

        state.addAmount(amount);

        if (state.getAmount() < quest.getRequiredAmount()) {
            return false;
        }

        state.setAmount(quest.getRequiredAmount());
        state.setCompleted(true);

        return true;
    }

    /**
     * 判斷指定任務是否已完成。
     *
     * @param quest 任務類型
     * @return true 表示已完成
     */
    public boolean isCompleted(QuestType quest) {
        QuestState state = getState(quest);

        return state != null && state.isCompleted();
    }

 
    // Quest HUD - Visible Quests 

    /**
     * 取得目前 QuestHUD 應該顯示的任務。
     *
     * @return 可見任務清單
     */
    public List<QuestType> getVisibleQuests() {
        List<QuestType> result = new ArrayList<>();

        int index = visibleStartIndex;

        while (index < storyQuests.size() &&
                result.size() < visibleQuestCount) {
            result.add(storyQuests.get(index));
            index++;
        }

        return result;
    }

    /**
     * 將 visibleStartIndex 往後推進，跳過已完成且完成動畫已播放的任務。
     *
     * 條件：
     * - 任務存在。
     * - 任務已完成。
     * - 任務完成動畫已播放。
     *
     * 若遇到尚未完成或動畫尚未播放的任務，就停止推進。
     */
    public void advancePastCompletedQuests() {
        while (visibleStartIndex < storyQuests.size()) {
            QuestType quest = storyQuests.get(visibleStartIndex);
            QuestState state = getState(quest);

            if (state == null) {
                break;
            }

            if (!state.isCompleted()) {
                break;
            }

            if (!state.isCompletionAnimationPlayed()) {
                break;
            }

            visibleStartIndex++;
        }
    }

    /**
     * 取得目前可見任務中，下一個完成但尚未播放完成動畫的任務。
     *
     * QuestHUD 可用此方法決定要播放哪個任務完成動畫。
     *
     * @return 任務類型；若沒有等待動畫的任務則回傳 null
     */
    public QuestType getNextCompletedQuestWaitingForAnimation() {
        for (QuestType quest : getVisibleQuests()) {
            QuestState state = getState(quest);

            if (state == null) {
                continue;
            }

            if (state.isCompleted() &&
                    !state.isCompletionAnimationPlayed()) {
                return quest;
            }
        }

        return null;
    }

    /**
     * 標記指定任務的完成動畫已播放。
     *
     * 標記後會嘗試推進 visibleStartIndex。
     *
     * @param quest 任務類型
     */
    public void markCompletionAnimationPlayed(QuestType quest) {
        QuestState state = getState(quest);

        if (state != null) {
            state.setCompletionAnimationPlayed(true);
        }

        advancePastCompletedQuests();
    }

 
    // Save / Load Support 

    /**
     * 取得目前任務顯示起始 index。
     *
     * SaveSystem 可用於存檔。
     *
     * @return 顯示起始 index
     */
    public int getVisibleStartIndex() {
        return visibleStartIndex;
    }

    /**
     * 設定目前任務顯示起始 index。
     *
     * SaveSystem 可用於讀檔。
     *
     * @param visibleStartIndex 顯示起始 index
     */
    public void setVisibleStartIndex(int visibleStartIndex) {
        this.visibleStartIndex = clampVisibleStartIndex(visibleStartIndex);
    }
 
    // Getters 

    /**
     * 取得故事任務順序。
     *
     * @return 不可修改的故事任務清單
     */
    public List<QuestType> getStoryQuests() {
        return Collections.unmodifiableList(storyQuests);
    }

    /**
     * 取得指定任務狀態。
     *
     * @param quest 任務類型
     * @return 任務狀態；若不存在則回傳 null
     */
    public QuestState getState(QuestType quest) {
        return states.get(quest);
    }

 
    // Helpers 

    /**
     * 限制 visibleStartIndex 不小於 0，
     * 且不超過故事任務數量。
     *
     * @param value 原始 index
     * @return 合法 index
     */
    private int clampVisibleStartIndex(int value) {
        return Math.max(
                0,
                Math.min(value, storyQuests.size())
        );
    }

    public static class QuestState {

        private int amount = 0;
        private boolean completed = false;
        private boolean completionAnimationPlayed = false;

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = Math.max(0, amount);
        }

        public void addAmount(int value) {
            setAmount(this.amount + value);
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public boolean isCompletionAnimationPlayed() {
            return completionAnimationPlayed;
        }

        public void setCompletionAnimationPlayed(boolean completionAnimationPlayed) {
            this.completionAnimationPlayed = completionAnimationPlayed;
        }
    }
}