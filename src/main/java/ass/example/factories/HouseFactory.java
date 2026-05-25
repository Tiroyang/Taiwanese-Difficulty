package ass.example.factories;

import ass.example.Main;
import ass.example.components.HouseScene.*;
import ass.example.components.InteractableComponent;
import ass.example.components.LethalComponent;
import ass.example.components.OneWayPlatformComponent;
import ass.example.core.CollisionCategory;
import ass.example.core.DeathReason;
import ass.example.core.EntityType;
import ass.example.core.FixtureFilterUtil;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
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

/**
 * 場景Entity生成處理器
 */
public class HouseFactory implements EntityFactory {
    @Spawns("house_background")
    public Entity newHouseBackground(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view("/Scene1/map/Background.png")
                .zIndex(-200)
                .build();
    }

    @Spawns("house_floor")
    public Entity newHouseFloor(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view("/Scene1/map/Floor.png")
                .zIndex(1)
                .build();
    }

    @Spawns("house_ceiling")
    public Entity newHouseCeiling(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view("/Scene1/map/Ceiling.png")
                .zIndex(-2)
                .build();
    }

    @Spawns("window_view")
    public Entity newWindowView(SpawnData data) {
        double baseX = data.getX();
        double baseY = data.getY();

        String texture = data.get("texture");
        double parallaxFactor = data.get("parallaxFactor");

        return entityBuilder(data)
                .view(texture)
                .with(new ParallaxWindowComponent(baseX, baseY, parallaxFactor))
                .zIndex(-300)
                .build();
    }

    @Spawns("house_foreground")
    public Entity newHouseForeground(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
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

        AudioSystem audioSystem = data.get("audioSystem");

        DoorComponent doorComponent = new DoorComponent(
                id,
                closedTexture,
                openTexture,
                colliderOffsetX,
                colliderOffsetY,
                colliderWidth,
                colliderHeight,
                audioSystem
        );

        return entityBuilder(data)
                .type(EntityType.DOOR)
                .view(closedTexture)
                .bbox(new HitBox(BoundingShape.box(100, 180)))
                .with(doorComponent)
                .with(new InteractableComponent(
                        () -> doorComponent.isOpened() ? "story.house.openDoor" : "story.house.closeDoor",
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

        physics.setFixtureDef(createWallFixtureDef());

        return entityBuilder(data)
                .type(EntityType.DOOR_COLLIDER)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode ? new Rectangle(width, height, javafx.scene.paint.Color.rgb(37, 255, 0, 0.5)) : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(physics)
                .with(new CollidableComponent(true))
                .zIndex(1000)
                .build();
    }

    @Spawns("quilt")
    public Entity newQuiltVisual(SpawnData data) {
        String messyTexture = "Scene1/props/Quilt.png";
        String foldedTexture = "Scene1/props/Quilt_folded.png";

        return entityBuilder(data)
                .type(EntityType.INTERACTABLE)
                .view(messyTexture)
                .zIndex(-3)
                .build();
    }

    @Spawns("quilt_trigger")
    public Entity newQuiltTrigger(SpawnData data) {
        String defaultTexture = "Scene1/props/Quilt.png";
        String foldedTexture = "Scene1/props/Quilt_folded.png";

        Entity quiltVisual = data.get("visual");

        double width = data.get("width");
        double height = data.get("height");

        double interactRange = data.get("interactRange");
        boolean promptOnEntity = data.get("promptOnEntity");
        double promptOffsetY = data.get("promptOffsetY");

        QuiltComponent quiltComponent = new QuiltComponent(
                quiltVisual,
                defaultTexture,
                foldedTexture
        );

        return entityBuilder(data)
                .type(EntityType.TRIGGER)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode ? new Rectangle(width, height, Color.rgb(255, 255, 0, 0.35)) : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(quiltComponent)
                .with(new InteractableComponent(
                        () -> "story.house.foldQuilt",
                        quiltComponent::fold,
                        interactRange,
                        promptOnEntity,
                        promptOffsetY
                ))
                .zIndex(1000)
                .build();
    }

    @Spawns("bed")
    public Entity newBed(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.PROP)
                .view("Scene1/props/Bed.png")
                .zIndex(-3)
                .build();
    }

    @Spawns("bed_one_way_platform")
    public Entity newBedOneWayPlatform(SpawnData data) {
        String id = data.get("id");

        double width = data.get("width");
        double height = data.get("height");

        double collider1OffsetX = data.get("collider1OffsetX");
        double collider1OffsetY = data.get("collider1OffsetY");
        double collider1Width = data.get("collider1Width");
        double collider1Height = data.get("collider1Height");

        double collider2OffsetX = data.hasKey("collider2OffsetX") ? data.get("collider2OffsetX") : 0.0;
        double collider2OffsetY = data.hasKey("collider2OffsetY") ? data.get("collider2OffsetY") : 0.0;
        double collider2Width = data.hasKey("collider2Width") ? data.get("collider2Width") : 0.0;
        double collider2Height = data.hasKey("collider2Height") ? data.get("collider2Height") : 0.0;

        int playerZIndexOnBed = ((Number) data.get("playerZIndexOnBed")).intValue();
        int normalPlayerZIndex = ((Number) data.get("normalPlayerZIndex")).intValue();

        DeathReason deathReason = data.get("deathReason");

        return entityBuilder(data)
                .type(EntityType.BED_ONE_WAY_PLATFORM)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode ? new Rectangle(width, height, javafx.scene.paint.Color.rgb(0, 180, 255, 0.35)) : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(new CollidableComponent(true))
                .with(new BedComponent(
                        BedComponent.Role.PLATFORM,
                        id,
                        width,
                        height,

                        collider1OffsetX,
                        collider1OffsetY,
                        collider1Width,
                        collider1Height,

                        collider2OffsetX,
                        collider2OffsetY,
                        collider2Width,
                        collider2Height,

                        playerZIndexOnBed,
                        normalPlayerZIndex,
                        deathReason
                ))
                .zIndex(1000)
                .build();
    }

    @Spawns("bed_one_way_platform_collider")
    public Entity newBedOneWayPlatformCollider(SpawnData data) {
        String id = data.get("id");

        double width = data.get("width");
        double height = data.get("height");

        int playerZIndexOnBed = ((Number) data.get("playerZIndexOnBed")).intValue();
        int normalPlayerZIndex = ((Number) data.get("normalPlayerZIndex")).intValue();

        DeathReason deathReason = data.get("deathReason");

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        FixtureDef floorShapeDef = createFloorFixtureDef();

        floorShapeDef.friction(0.8f)
                .restitution(0.5f);

        physics.setFixtureDef(floorShapeDef);

        return entityBuilder(data)
                .type(EntityType.BED_ONE_WAY_PLATFORM_COLLIDER)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode ? new Rectangle(width, height, javafx.scene.paint.Color.rgb(0, 255, 196, 0.35)) : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(physics)
                .with(new CollidableComponent(true))
                .with(new BedComponent(
                        BedComponent.Role.COLLIDER,
                        id,
                        0,
                        0,

                        0,
                        0,
                        width,
                        height,

                        0,
                        0,
                        0,
                        0,

                        playerZIndexOnBed,
                        normalPlayerZIndex,
                        deathReason
                ))
                .zIndex(1000)
                .build();
    }

    @Spawns("cabinet")
    public Entity newCabinet(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.PROP)
                .view("/Scene1/props/Cabinet.png")
                .zIndex(-1)
                .build();
    }

    @Spawns("water")
    public Entity newWater(SpawnData data) {
        String texture = "Scene1/props/Water.png";

        return entityBuilder(data)
                .type(EntityType.INTERACTABLE)
                .view(texture)
                .zIndex(-2)
                .build();
    }

    @Spawns("water_trigger")
    public Entity newWaterTrigger(SpawnData data) {
        Entity visual = data.get("visual");

        Entity player = data.get("player");

        DeathSystem deathSystem = data.get("deathSystem");

        AudioSystem audioSystem = data.hasKey("audioSystem")
                ? data.get("audioSystem")
                : null;

        double width = data.get("width");
        double height = data.get("height");

        double interactRange = data.get("interactRange");
        boolean promptOnEntity = data.get("promptOnEntity");
        double promptOffsetY = data.get("promptOffsetY");

        WaterComponent waterComponent = new WaterComponent(
                visual,
                player,
                deathSystem,
                audioSystem
        );

        return entityBuilder(data)
                .type(EntityType.INTERACTABLE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode ? new Rectangle(width, height, Color.rgb(255, 255, 0, 0.35)) : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(waterComponent)
                .with(new InteractableComponent(
                        () -> "story.house.drinkWater",
                        waterComponent::drink,
                        interactRange,
                        promptOnEntity,
                        promptOffsetY
                ))
                .zIndex(30)
                .build();
    }

    @Spawns("bathtub_sensor")
    public Entity newBathtubSensor(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        Entity player = data.get("player");
        DeathSystem deathSystem = data.get("deathSystem");
        DeathReason deathReason = data.get("deathReason");

        return entityBuilder(data)
                .type(EntityType.TRIGGER)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode
                        ? new Rectangle(width, height, Color.rgb(0, 180, 255, 0.32))
                        : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(new CollidableComponent(true))
                .with(new BathtubComponent(
                        player,
                        deathSystem,
                        deathReason
                ))
                .zIndex(1000)
                .build();
    }

    private FixtureDef createWallFixtureDef() {
        FixtureDef fixtureDef = new FixtureDef()
                .friction(0.0f)
                .restitution(0.0f);

        FixtureFilterUtil.applyFilter(
                fixtureDef,
                CollisionCategory.WALL,
                CollisionCategory.PLAYER
        );

        return fixtureDef;
    }

    private FixtureDef createFloorFixtureDef() {
        FixtureDef fixtureDef = new FixtureDef()
                .friction(0.8f)
                .restitution(0.1f);

        FixtureFilterUtil.applyFilter(
                fixtureDef,
                CollisionCategory.FLOOR,
                (short) (CollisionCategory.PLAYER | CollisionCategory.FALLING_OBJECT)
        );

        return fixtureDef;
    }
}
