package ass.example.system;

import ass.example.components.InteractableComponent;
import ass.example.core.EntityType;
import com.almasb.fxgl.entity.Entity;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Comparator;
import java.util.Optional;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * 可互動物件系統
 */
public class InteractionSystem {

    private final Entity player;

    private final StackPane promptBox = new StackPane();
    private final HBox promptContent = new HBox(10);
    private final ImageView keyIcon = new ImageView();
    private final Text actionText = new Text();

    private final double promptMinWidth = 0;
    private final double promptHeight = 46;
    private final double promptPaddingX = 12;

    private final double defaultPromptX = 640;
    private final double defaultPromptY = 620;

    private Entity currentPromptTarget = null;
    private Timeline pulseAnimation;

    // 防連按
    private final double interactCooldown = 0.25;
    private double interactTimer = 0;

    public InteractionSystem(Entity player) {
        this.player = player;

        setupPromptUI();

        addUINode(promptBox, 0, 0);
    }

    private void setupPromptUI() {
        promptBox.setVisible(false);
        promptBox.setMouseTransparent(true);

        /*
         * promptBox 不固定寬度，讓內容自動決定大小。
         */
        promptBox.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        promptBox.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        promptBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        setupKeyIcon();

        actionText.setStyle("""
            -fx-font-size: 18px;
            -fx-fill: white;
            -fx-font-weight: bold;
            """);
        actionText.setEffect(new DropShadow(5, Color.BLACK));

        /*
         * 重點：
         * 直接讓 HBox 自己當作背景容器。
         * 不再用 Rectangle 綁定寬高，避免無限放大。
         */
        promptContent.setAlignment(Pos.CENTER_LEFT);
        promptContent.setPadding(new Insets(7, promptPaddingX, 7, promptPaddingX));
        promptContent.getChildren().addAll(keyIcon, actionText);

        promptContent.setStyle("""
            -fx-background-color: rgba(0, 0, 0, 0.2);
            -fx-background-radius: 16px;
            -fx-border-color: rgba(255, 255, 255, 0.75);
            -fx-border-width: 1.4px;
            -fx-border-radius: 16px;
            """);

        promptContent.setEffect(
                new DropShadow(12, Color.rgb(255, 255, 255, 0.20))
        );

        promptBox.getChildren().add(promptContent);

        createPulseAnimation();
    }

    private void setupKeyIcon() {
        Image image = loadImage("/assets/textures/ui/keys/key-f.png");

        if (image != null) {
            keyIcon.setImage(image);
        }

        keyIcon.setFitHeight(24);
        keyIcon.setFitWidth(24);
        keyIcon.setPreserveRatio(true);
        keyIcon.setSmooth(true);

        keyIcon.setEffect(new DropShadow(5, Color.BLACK));
    }

    private Image loadImage(String path) {
        try {
            var url = getClass().getResource(path);

            if (url == null) {
                System.out.println("Interaction key icon not found: " + path);
                return null;
            }

            return new Image(url.toExternalForm());

        } catch (Exception e) {
            System.out.println("Interaction key icon load failed: " + path);
            return null;
        }
    }

    private void createPulseAnimation() {
        pulseAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(promptBox.scaleXProperty(), 1.0),
                        new KeyValue(promptBox.scaleYProperty(), 1.0)
                ),
                new KeyFrame(Duration.seconds(0.65),
                        new KeyValue(promptBox.scaleXProperty(), 1.035),
                        new KeyValue(promptBox.scaleYProperty(), 1.035)
                ),
                new KeyFrame(Duration.seconds(1.3),
                        new KeyValue(promptBox.scaleXProperty(), 1.0),
                        new KeyValue(promptBox.scaleYProperty(), 1.0)
                )
        );

        pulseAnimation.setCycleCount(Animation.INDEFINITE);
    }

    public void update(double tpf) {
        if (getb("playerDead")) {
            hidePrompt();
            return;
        }

        if (interactTimer > 0) {
            interactTimer -= tpf;
        }

        Optional<Entity> nearest = findNearestInteractable();

        if (nearest.isPresent()) {
            Entity target = nearest.get();
            InteractableComponent component = target.getComponent(InteractableComponent.class);

            showPrompt(target, component);
            updatePromptPosition(target, component);

        } else {
            hidePrompt();
        }
    }

    private void showPrompt(Entity target, InteractableComponent component) {
        actionText.setText(component.getPromptText());

        /*
         * 換目標時才播放出現動畫，避免每幀重播。
         */
        if (currentPromptTarget != target) {
            currentPromptTarget = target;
            playPromptAppearAnimation();
        }

        promptBox.setVisible(true);

        if (pulseAnimation != null && pulseAnimation.getStatus() != Animation.Status.RUNNING) {
            pulseAnimation.play();
        }
    }

    private void hidePrompt() {
        currentPromptTarget = null;
        promptBox.setVisible(false);

        if (pulseAnimation != null) {
            pulseAnimation.stop();
        }
    }

    private void playPromptAppearAnimation() {
        promptBox.setOpacity(0);
        promptBox.setTranslateY(promptBox.getTranslateY() + 12);
        promptBox.setScaleX(0.92);
        promptBox.setScaleY(0.92);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.14), promptBox);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition move = new TranslateTransition(Duration.seconds(0.14), promptBox);
        move.setByY(-12);
        move.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.14), promptBox);
        scale.setFromX(0.92);
        scale.setFromY(0.92);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, move, scale).play();
    }

    public void interact() {
        if (getb("playerDead")) {
            hidePrompt();
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
                            e.getType() == EntityType.DOOR) {
                        return false;
                    }

                    return true;
                })
                .filter(e -> {
                    InteractableComponent component = e.getComponent(InteractableComponent.class);
                    return distanceXBetweenCenters(e, player) < component.getInteractRange();
                })
                .filter(e -> {
                    InteractableComponent component = e.getComponent(InteractableComponent.class);
                    return component.canInteract();
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
        /*
         * 先更新尺寸，避免 getLayoutBounds() 取到舊值。
         */
        promptBox.applyCss();
        promptBox.autosize();

        double promptWidth = promptBox.getLayoutBounds().getWidth();
        double promptHeight = promptBox.getLayoutBounds().getHeight();

        if (!component.isPromptOnEntity()) {
            promptBox.setTranslateX(defaultPromptX - promptWidth / 2);
            promptBox.setTranslateY(defaultPromptY);
            return;
        }

        double worldX = target.getBoundingBoxComponent().getCenterWorld().getX();
        double worldY = target.getBoundingBoxComponent().getMinYWorld() - component.getPromptOffsetY();

        double screenX = worldX - getGameScene().getViewport().getX();
        double screenY = worldY - getGameScene().getViewport().getY();

        promptBox.setTranslateX(screenX - promptWidth / 2);
        promptBox.setTranslateY(screenY - promptHeight);
    }

    public void dispose() {
        hidePrompt();

        if (pulseAnimation != null) {
            pulseAnimation.stop();
            pulseAnimation = null;
        }

        removeUINode(promptBox);
    }
}