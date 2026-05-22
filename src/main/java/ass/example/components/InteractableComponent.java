package ass.example.components;

import ass.example.system.LanguageSystem;
import com.almasb.fxgl.entity.component.Component;

import java.util.function.Supplier;

public class InteractableComponent extends Component {

    private final Supplier<String> promptKeySupplier;

    private final Runnable action;

    private final double interactRange;
    private final boolean promptOnEntity;
    private final double promptOffsetY;

    public InteractableComponent(String promptKey, Runnable action) {
        this(() -> promptKey, action, 220, false, 30);
    }

    public InteractableComponent(String promptKey, Runnable action, double interactRange, boolean promptOnEntity) {
        this(() -> promptKey, action, interactRange, promptOnEntity, 30);
    }

    public InteractableComponent(
            Supplier<String> promptKeySupplier,
            Runnable action,
            double interactRange,
            boolean promptOnEntity,
            double promptOffsetY
    ) {
        this.promptKeySupplier = promptKeySupplier;
        this.action = action;
        this.interactRange = interactRange;
        this.promptOnEntity = promptOnEntity;
        this.promptOffsetY = promptOffsetY;
    }

    public String getPromptKey() {
        return promptKeySupplier.get();
    }

    public String getPromptText() {
        return LanguageSystem.getInstance().text(getPromptKey());
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