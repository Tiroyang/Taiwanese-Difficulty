package ass.example.scenes;

import ass.example.component.InteractableComponent;
import com.almasb.fxgl.entity.Entity;

import java.util.Comparator;
import java.util.Optional;

import static com.almasb.fxgl.dsl.FXGL.*;

public class HomeScene {

    private Entity player;
    private Entity currentInteractable;

    public Entity load() {
        spawnBackground();
        spawnCollisions();
        spawnInteractables();
        spawnAnimatedProps();

        player = spawn("player", 120, 520);

        setupCamera();

        return player;
    }

    private void spawnBackground() {
        spawn("house_background", 0, 0);
    }

    private void spawnCollisions() {
        // 地板
        spawn("wall", 0, 650, 3200, 70);

        // 左右邊界
        spawn("wall", 0, 0, 40, 720);
        spawn("wall", 3160, 0, 40, 720);

        // 範例：家具或牆壁碰撞
        spawn("wall", 740, 420, 40, 230);    // 門旁牆
        spawn("wall", 1700, 540, 300, 80);   // 沙發碰撞
    }

    private void spawnInteractables() {
        spawn("door", 680, 430, "frontDoor", "進入走道");
        spawn("door", 2200, 430, "bedroomDoor", "進入臥室");

        spawn("window", 1350, 250, "livingRoomWindow", "打開窗戶");
        spawn("window", 2550, 250, "bedroomWindow", "打開窗戶");
    }

    private void spawnAnimatedProps() {
        // 如果只是循環動畫物件，也可以另外 spawn
    }

    private void setupCamera() {
        getGameScene().getViewport().setBounds(0, 0, 3200, 720);
        getGameScene().getViewport().bindToEntity(player, 640, 360);
        getGameScene().getViewport().setLazy(true);
    }

    public void tryInteract() {
        Optional<Entity> nearest = getGameWorld()
                .getEntitiesByComponent(InteractableComponent.class)
                .stream()
                .filter(e -> e.distance(player) < 120)
                .min(Comparator.comparingDouble(e -> e.distance(player)));

        nearest.ifPresent(e -> {
            e.getComponent(InteractableComponent.class).interact();
        });
    }
}