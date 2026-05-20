package ass.example.factories;

import ass.example.Main;
import ass.example.components.DoorComponent;
import ass.example.components.InteractableComponent;
import ass.example.components.ParallaxWindowComponent;
import ass.example.components.WindowComponent;
import ass.example.core.EntityTypes;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;

public class HouseFactory implements EntityFactory {


    @Spawns("window_view1")
    public Entity newWindowView(SpawnData data) {
        return entityBuilder(data)
                .type(EntityTypes.MAP_BACKGROUND)
                .view("/Scene1/map/Window_view_1.png")
                .zIndex(-1000)
                .build();
    }

    @Spawns("house_background")
    public Entity newHouseBackground(SpawnData data) {
        return entityBuilder(data)
                .type(EntityTypes.MAP_BACKGROUND)
                .view("/Scene1/map/Background.png")
                .zIndex(-200)
                .build();
    }

    @Spawns("house_floor")
    public Entity newHouseFloor(SpawnData data) {
        return entityBuilder(data)
                .type(EntityTypes.MAP_BACKGROUND)
                .view("/Scene1/map/Floor.png")
                .zIndex(1)
                .build();
    }

    @Spawns("house_ceiling")
    public Entity newHouseCeiling(SpawnData data) {
        return entityBuilder(data)
                .type(EntityTypes.MAP_BACKGROUND)
                .view("/Scene1/map/Ceiling.png")
                .zIndex(-2)
                .build();
    }

    @Spawns("wall")
    public Entity newWall(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        return entityBuilder(data)
                .type(EntityTypes.WALL)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode ? new Rectangle(width, height, javafx.scene.paint.Color.rgb(255, 0, 0, 0.5)) : new Rectangle(0, 0, Color.TRANSPARENT))
                .zIndex(1000)
                .with(physics)
                .with(new CollidableComponent(true))
                .build();
    }

    @Spawns("house_foreground")
    public Entity newHouseForeground(SpawnData data) {
        return entityBuilder(data)
                .type(EntityTypes.MAP_BACKGROUND)
                .view("/Scene1/map/Foreground.png")
                .zIndex(200)
                .build();
    }

    @Spawns("door")
    public Entity newDoor(SpawnData data) {
        String id = data.get("id");

        String closedTexture = data.get("closedTexture");
        String openTexture = data.get("openTexture");

        double colliderOffsetX = data.get("colliderOffsetX");
        double colliderOffsetY = data.get("colliderOffsetY");
        double colliderWidth = data.get("colliderWidth");
        double colliderHeight = data.get("colliderHeight");

        double interactRange = data.get("interactRange");
        boolean promptOnEntity = data.get("promptOnEntity");
        double promptOffsetY = data.get("promptOffsetY");

        DoorComponent doorComponent = new DoorComponent(
                id,
                closedTexture,
                openTexture,
                colliderOffsetX,
                colliderOffsetY,
                colliderWidth,
                colliderHeight
        );

        return entityBuilder(data)
                .type(EntityTypes.DOOR)
                .view(closedTexture)
                .bbox(new HitBox(BoundingShape.box(100, 180)))
                .with(doorComponent)
                .with(new InteractableComponent(
                        () -> doorComponent.isOpened() ? "按 F 關門" : "按 F 開門",
                        doorComponent::toggle,
                        interactRange,
                        promptOnEntity,
                        promptOffsetY
                ))
                .zIndex(-1)
                .build();
    }

    @Spawns("door_collider")
    public Entity newDoorCollider(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        physics.setFixtureDef(
                new FixtureDef()
                        .friction(0.0f)
                        .restitution(0.0f)
        );

        return entityBuilder(data)
                .type(EntityTypes.DOOR_COLLIDER)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode ? new Rectangle(width, height, javafx.scene.paint.Color.rgb(37, 255, 0, 0.5)) : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(physics)
                .with(new CollidableComponent(true))
                .zIndex(1000)
                .build();
    }
}
