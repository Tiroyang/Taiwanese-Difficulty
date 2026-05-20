package ass.example.components;

import com.almasb.fxgl.entity.component.Component;
import java.util.function.Supplier;

public class InteractableComponent extends Component {

    private final Supplier<String> promptSupplier;
    private final Runnable action;

    private final double interactRange;
    private final boolean promptOnEntity;
    private final double promptOffsetY;

    public InteractableComponent(String prompt, Runnable action) {
        this(() -> prompt, action, 220, false, 30);
    }

    public InteractableComponent(String prompt, Runnable action, double interactRange, boolean promptOnEntity) {
        this(() -> prompt, action, interactRange, promptOnEntity, 30);
    }

    public InteractableComponent(
            Supplier<String> promptSupplier,
            Runnable action,
            double interactRange,
            boolean promptOnEntity,
            double promptOffsetY
    ) {
        this.promptSupplier = promptSupplier;
        this.action = action;
        this.interactRange = interactRange;
        this.promptOnEntity = promptOnEntity;
        this.promptOffsetY = promptOffsetY;
    }

    public String getPrompt() {
        return promptSupplier.get();
    }

    public void interact() {
        if (action != null) {
            action.run();
        }
    }

    public double getInteractRange() {
        return interactRange;
    }

    public boolean isPromptOnEntity() {
        return promptOnEntity;
    }

    public double getPromptOffsetY() {
        return promptOffsetY;
    }
}