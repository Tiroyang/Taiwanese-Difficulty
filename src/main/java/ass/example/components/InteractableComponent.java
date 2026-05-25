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

    private final Supplier<Boolean> canInteractSupplier;

    public InteractableComponent(String prompt, Runnable action) {
        this(
                () -> prompt,
                action,
                220,
                false,
                30,
                () -> true
        );
    }

    public InteractableComponent(String prompt, Runnable action, double interactRange, boolean promptOnEntity) {
        this(
                () -> prompt,
                action,
                interactRange,
                promptOnEntity,
                30,
                () -> true
        );
    }

    public InteractableComponent(
            Supplier<String> promptSupplier,
            Runnable action,
            double interactRange,
            boolean promptOnEntity,
            double promptOffsetY
    ) {
        this(
                promptSupplier,
                action,
                interactRange,
                promptOnEntity,
                promptOffsetY,
                () -> true
        );
    }

    public InteractableComponent(
            Supplier<String> promptKeySupplier,
            Runnable action,
            double interactRange,
            boolean promptOnEntity,
            double promptOffsetY,
            Supplier<Boolean> canInteractSupplier
    ) {
        this.promptKeySupplier = promptKeySupplier;
        this.action = action;
        this.interactRange = interactRange;
        this.promptOnEntity = promptOnEntity;
        this.promptOffsetY = promptOffsetY;
        this.canInteractSupplier = canInteractSupplier;
    }

    public boolean canInteract() {
        return canInteractSupplier == null || canInteractSupplier.get();
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