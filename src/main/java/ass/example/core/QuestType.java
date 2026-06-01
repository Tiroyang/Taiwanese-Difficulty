package ass.example.core;

public enum QuestType {

    FOLD_QUILT(
            "quest.fold_quilt",
            1
    ),

    BRUSH_TEETH(
            "quest.brush_teeth",
            1
    ),

    TALK_TO_MOM(
            "quest.talk_to_mom",
            1
    ),

    WEAR_SHOES(
            "quest.wear_shoes",
            1
    ),

    EXIT_HOUSE(
            "quest.exit_house",
            1
    );

    private final String titleKey;
    private final int requiredAmount;

    QuestType(String titleKey, int requiredAmount) {
        this.titleKey = titleKey;
        this.requiredAmount = requiredAmount;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }
}