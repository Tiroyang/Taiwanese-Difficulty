package ass.example.component;

import com.almasb.fxgl.entity.component.Component;

public class InteractableComponent extends Component {

    private final String prompt;
    private final Runnable action;

    public InteractableComponent(String prompt, Runnable action) {
        this.prompt = prompt;
        this.action = action;
    }

    public String getPrompt() {
        return prompt;
    }

    public void interact() {
        if (action != null) {
            action.run();
        }
    }
}