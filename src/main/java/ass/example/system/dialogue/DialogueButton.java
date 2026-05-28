package ass.example.system.dialogue;

public class DialogueButton {

    private final String text;
    private final Runnable action;

    public DialogueButton(String text, Runnable action) {
        this.text = text;
        this.action = action;
    }

    public String getText() {
        return text;
    }

    public void run() {
        if (action != null) {
            action.run();
        }
    }
}