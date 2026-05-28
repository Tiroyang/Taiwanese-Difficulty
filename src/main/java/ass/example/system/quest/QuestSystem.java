package ass.example.system.quest;

import ass.example.system.quest.QuestState;
import ass.example.core.QuestType;

import java.util.*;

public class QuestSystem {

    private static final QuestSystem INSTANCE = new QuestSystem();

    public static QuestSystem getInstance() {
        return INSTANCE;
    }

    private final List<QuestType> storyQuests = new ArrayList<>();
    private final Map<QuestType, QuestState> states = new EnumMap<>(QuestType.class);

    private int visibleStartIndex = 0;
    private int visibleQuestCount = 1;

    private QuestSystem() {
        setupStoryQuests();
        resetRuntimeState();
    }

    private void setupStoryQuests() {
        storyQuests.clear();

        storyQuests.add(QuestType.FOLD_QUILT);
        storyQuests.add(QuestType.BRUSH_TEETH);
        storyQuests.add(QuestType.TALK_TO_MOM);
        storyQuests.add(QuestType.WEAR_SHOES);
    }

    public void resetRuntimeState() {
        states.clear();

        for (QuestType quest : storyQuests) {
            states.put(quest, new ass.example.system.quest.QuestState());
        }

        visibleStartIndex = 0;
    }

    public List<QuestType> getStoryQuests() {
        return storyQuests;
    }

    public QuestState getState(QuestType quest) {
        return states.get(quest);
    }

    public void setVisibleQuestCount(int visibleQuestCount) {
        this.visibleQuestCount = Math.max(1, visibleQuestCount);
    }

    public List<QuestType> getVisibleQuests() {
        List<QuestType> result = new ArrayList<>();

        int index = visibleStartIndex;

        while (index < storyQuests.size() && result.size() < visibleQuestCount) {
            result.add(storyQuests.get(index));
            index++;
        }

        return result;
    }

    public boolean completeQuest(QuestType quest) {
        QuestState state = states.get(quest);

        if (state == null || state.isCompleted()) {
            return false;
        }

        state.setAmount(quest.getRequiredAmount());
        state.setCompleted(true);

        return true;
    }

    public boolean addQuestProgress(QuestType quest, int amount) {
        QuestState state = states.get(quest);

        if (state == null || state.isCompleted()) {
            return false;
        }

        state.addAmount(amount);

        if (state.getAmount() >= quest.getRequiredAmount()) {
            state.setAmount(quest.getRequiredAmount());
            state.setCompleted(true);
            return true;
        }

        return false;
    }

    public boolean isCompleted(QuestType quest) {
        QuestState state = states.get(quest);
        return state != null && state.isCompleted();
    }

    public boolean isAllStoryQuestsCompleted() {
        for (QuestType quest : storyQuests) {
            QuestState state = states.get(quest);

            if (state == null || !state.isCompleted()) {
                return false;
            }
        }

        return true;
    }

    public boolean canEnterNextScene() {
        return isAllStoryQuestsCompleted();
    }

    public void advancePastCompletedQuests() {
        while (visibleStartIndex < storyQuests.size()) {
            QuestType quest = storyQuests.get(visibleStartIndex);
            QuestState state = states.get(quest);

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

    public QuestType getNextCompletedQuestWaitingForAnimation() {
        for (QuestType quest : getVisibleQuests()) {
            QuestState state = states.get(quest);

            if (state != null
                    && state.isCompleted()
                    && !state.isCompletionAnimationPlayed()) {
                return quest;
            }
        }

        return null;
    }

    public void markCompletionAnimationPlayed(QuestType quest) {
        QuestState state = states.get(quest);

        if (state != null) {
            state.setCompletionAnimationPlayed(true);
        }

        advancePastCompletedQuests();
    }

    public int getVisibleStartIndex() {
        return visibleStartIndex;
    }

    public void setVisibleStartIndex(int visibleStartIndex) {
        this.visibleStartIndex = Math.max(0, visibleStartIndex);
    }

    public Map<QuestType, QuestState> getStates() {
        return states;
    }
}