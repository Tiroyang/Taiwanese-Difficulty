package ass.example.system.dialogue;

public class DialogueButton {

    private final String textKey;
    private final Runnable action;

    public DialogueButton(String textKey, Runnable action) {
        this.textKey = textKey;
        this.action = action;
    }

    public String getTextKey() {
        return textKey;
    }

    public void run() {
        if (action != null) {
            action.run();
        }
    }
}