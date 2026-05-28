package ass.example.system.dialogue;

import java.util.ArrayList;
import java.util.List;

public class DialogueLine {

    private final String id;

    private final String defaultPortraitPath;
    private final String speakingPortraitPath;

    private final String characterName;
    private final String text;

    private final boolean allowClickNext;
    private final String nextId;
    private final boolean endDialogue;

    private Runnable onFinish;

    private final List<DialogueButton> buttons = new ArrayList<>();

    public DialogueLine(
            String id,
            String defaultPortraitPath,
            String speakingPortraitPath,
            String characterName,
            String text,
            boolean allowClickNext,
            String nextId,
            boolean endDialogue
    ) {
        this.id = id;
        this.defaultPortraitPath = defaultPortraitPath;
        this.speakingPortraitPath = speakingPortraitPath;
        this.characterName = characterName;
        this.text = text;
        this.allowClickNext = allowClickNext;
        this.nextId = nextId;
        this.endDialogue = endDialogue;
    }

    public DialogueLine onFinish(Runnable onFinish) {
        this.onFinish = onFinish;
        return this;
    }

    public void runOnFinish() {
        if (onFinish != null) {
            onFinish.run();
        }
    }

    public DialogueLine addButton(DialogueButton button) {
        buttons.add(button);
        return this;
    }

    public String getId() {
        return id;
    }

    public String getDefaultPortraitPath() {
        return defaultPortraitPath;
    }

    public String getSpeakingPortraitPath() {
        return speakingPortraitPath;
    }

    public String getCharacterName() {
        return characterName;
    }

    public String getText() {
        return text;
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

    public List<DialogueButton> getButtons() {
        return buttons;
    }

    public boolean hasButtons() {
        return !buttons.isEmpty();
    }
}