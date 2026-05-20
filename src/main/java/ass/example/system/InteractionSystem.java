package ass.example.system;

import ass.example.components.InteractableComponent;
import ass.example.core.EntityTypes;
import com.almasb.fxgl.entity.Entity;
import javafx.scene.text.Text;

import java.util.Comparator;
import java.util.Optional;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * 可互動物件系統
 */
public class InteractionSystem {

    private final Entity player;
    private final Text promptText;

    private final double defaultPromptX = 500;
    private final double defaultPromptY = 620;

    // 防連按
    private double interactCooldown = 0.25;
    private double interactTimer = 0;

    public InteractionSystem(Entity player) {
        this.player = player;

        promptText = new Text("");
        promptText.setStyle("-fx-font-size: 24px; -fx-fill: white; -fx-stroke: black; -fx-stroke-width: 1px;");
        promptText.setVisible(false);

        addUINode(promptText, defaultPromptX, defaultPromptY);
    }

    public void update(double tpf) {
        if (getb("playerDead")) {
            promptText.setVisible(false);
            return;
        }
        if (interactTimer > 0) {
            interactTimer -= tpf;
        }

        Optional<Entity> nearest = findNearestInteractable();

        if (nearest.isPresent()) {
            Entity target = nearest.get();
            InteractableComponent component = target.getComponent(InteractableComponent.class);

            promptText.setText(component.getPrompt());
            promptText.setVisible(true);

            updatePromptPosition(target, component);
        } else {
            promptText.setVisible(false);
        }
    }

    public void interact() {
        if (getb("playerDead")) {
            promptText.setVisible(false);
            return;
        }
        if (interactTimer > 0) {
            return;
        }

        findNearestInteractable().ifPresent(e -> {
            e.getComponent(InteractableComponent.class).interact();

            // 互動成功後才進入冷卻
            interactTimer = interactCooldown;
        });
    }

    private Optional<Entity> findNearestInteractable() {
        return getGameWorld()
                .getEntitiesByComponent(InteractableComponent.class)
                .stream()
                .filter(e -> {
                    if (getb("playerOnBedCollider") &&
                            e.getType() == EntityTypes.DOOR) {
                        return false;
                    }

                    return true;
                })
                .filter(e -> {
                    InteractableComponent component = e.getComponent(InteractableComponent.class);
                    return distanceXBetweenCenters(e, player) < component.getInteractRange();
                })
                .min(Comparator.comparingDouble(e ->
                        distanceXBetweenCenters(e, player)
                ));
    }

    private double distanceXBetweenCenters(Entity a, Entity b) {
        double ax = a.getBoundingBoxComponent().getCenterWorld().getX();
        double bx = b.getBoundingBoxComponent().getCenterWorld().getX();

        return Math.abs(ax - bx);
    }

    private void updatePromptPosition(Entity target, InteractableComponent component) {
        if (!component.isPromptOnEntity()) {
            promptText.setTranslateX(defaultPromptX);
            promptText.setTranslateY(defaultPromptY);
            return;
        }

        double worldX = target.getBoundingBoxComponent().getCenterWorld().getX();
        double worldY = target.getBoundingBoxComponent().getMinYWorld() - component.getPromptOffsetY();

        double screenX = worldX - getGameScene().getViewport().getX();
        double screenY = worldY - getGameScene().getViewport().getY();

        promptText.setTranslateX(screenX - promptText.getLayoutBounds().getWidth() / 2);
        promptText.setTranslateY(screenY);
    }
}