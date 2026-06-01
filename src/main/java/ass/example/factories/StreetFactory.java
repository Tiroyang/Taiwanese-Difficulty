package ass.example.factories;

import ass.example.Main;
import ass.example.components.InteractableComponent;
import ass.example.components.LethalComponent;
import ass.example.core.*;
import ass.example.core.StreetScene.FallingObjectVariant;
import ass.example.core.StreetScene.StreetApartmentStyle;
import ass.example.scenes.SceneManager;
import ass.example.system.InteractionSystem;
import ass.example.system.LanguageSystem;
import ass.example.system.quest.QuestSystem;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;
import static com.almasb.fxgl.dsl.FXGL.getb;

public class StreetFactory implements EntityFactory {

    @Spawns("street_far_background")
    public Entity newStreetFarBackground(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view(new Rectangle(width, height, Color.rgb(140, 95, 130)))
                .zIndex(-500)
                .build();
    }

    @Spawns("street_floor")
    public Entity newStreetFloor(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        /*
         * 目前先用色塊代替。
         * 之後有素材時可以改成：
         * .view("SceneStreet/floor.png")
         */
        Rectangle view = new Rectangle(width, height);
        view.setFill(Color.rgb(52, 52, 52));

        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view(view)
                .zIndex(10)
                .build();
    }

    @Spawns("street_apartment_bg")
    public Entity newStreetApartmentBackground(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        StreetApartmentStyle style = StreetApartmentStyle.valueOf(data.get("style"));

        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view(new Rectangle(width, height, getApartmentColor(style)))
                .zIndex(-200)
                .build();
    }

    @Spawns("street_apartment_fg")
    public Entity newStreetApartmentForeground(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        StreetApartmentStyle style = StreetApartmentStyle.valueOf(data.get("style"));

        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view(new Rectangle(width, height, getForegroundColor(style)))
                .zIndex(-100)
                .build();
    }

    @Spawns("street_transformer_box")
    public Entity newStreetTransformerBoxVisual(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        /*
         * 暫時用色塊。
         * 之後有素材可以改成：
         * .view("SceneStreet/props/TransformerBox.png")
         */
        Rectangle view = new Rectangle(width, height);
        view.setArcWidth(10);
        view.setArcHeight(10);
        view.setFill(Color.rgb(55, 95, 85));
        view.setStroke(Color.rgb(210, 230, 120, 0.9));
        view.setStrokeWidth(2);

        return entityBuilder(data)
                .type(EntityType.PROP)
                .view(view)
                .zIndex(9)
                .build();
    }

    @Spawns("street_protruding_tile")
    public Entity newStreetProtrudingTile(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        /*
         * 暫時用色塊。
         * 之後有素材可以改成：
         * .view("SceneStreet/props/RepairZone.png")
         */
        Rectangle view = new Rectangle(width, height);
        view.setArcWidth(8);
        view.setArcHeight(8);
        view.setFill(Color.rgb(255, 190, 35, 0.55));
        view.setStroke(Color.rgb(255, 255, 255, 0.85));
        view.setStrokeWidth(2);

        return entityBuilder(data)
                .type(EntityType.PROP)
                .view(view)
                .zIndex(-149)
                .build();
    }

    @Spawns("street_scooter")
    public Entity newStreetScooter(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        boolean fromLeft = data.hasKey("fromLeft")
                ? data.get("fromLeft")
                : true;

        Rectangle view = new Rectangle(width, height);
        view.setArcWidth(16);
        view.setArcHeight(16);
        view.setFill(Color.rgb(35, 35, 35));
        view.setStroke(Color.rgb(255, 80, 80, 0.95));
        view.setStrokeWidth(3);

        /*
         * 色塊時其實不需要翻轉。
         * 之後換成 ImageView 時，可以讓圖片節點 scaleX = -1，
         * 但不要再 translateX(width)。
         */
        view.setScaleX(fromLeft ? 1 : -1);

        return entityBuilder(data)
                .type(EntityType.PROP)
                .view(view)
                .zIndex(-150)
                .build();
    }

    @Spawns("street_scooter_death_wall")
    public Entity newScooterDeathWall(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        DeathReason deathReason = data.get("deathReason");

        return entityBuilder(data)
                .type(EntityType.DEATH_ZONE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode
                        ? new Rectangle(width, height, Color.rgb(255, 0, 0, 0.42))
                        : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(new CollidableComponent(true))
                .with(new LethalComponent(deathReason))
                .zIndex(1000)
                .build();
    }

    @Spawns("street_falling_object")
    public Entity newStreetFallingObject(SpawnData data) {
        FallingObjectVariant variant = FallingObjectVariant.valueOf(data.get("variant"));

        double width = variant.getWidth();
        double height = variant.getHeight();

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);

        FixtureDef fixtureDef = new FixtureDef()
                .density(0.9f)
                .friction(0.65f)
                .restitution(0.0f);

        FixtureFilterUtil.applyFilter(
                fixtureDef,
                CollisionCategory.FALLING_OBJECT,
                CollisionCategory.FLOOR
        );

        physics.setFixtureDef(fixtureDef);

        Rectangle view = new Rectangle(width, height);

        switch (variant) {
            case FRIDGE -> {
                view.setFill(Color.rgb(190, 60, 55));
                view.setStroke(Color.rgb(255, 240, 180));
            }
            case HELI -> {
                view.setFill(Color.rgb(120, 70, 45));
                view.setStroke(Color.rgb(255, 170, 110));
            }
        }

        view.setArcWidth(8);
        view.setArcHeight(8);
        view.setStrokeWidth(2.2);

        return entityBuilder(data)
                .type(EntityType.PROP)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(view)
                .with(physics)
                .zIndex(130)
                .build();
    }

    @Spawns("street_falling_object_trigger")
    public Entity newStreetFallingObjectTrigger(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        DeathReason deathReason = data.get("deathReason");

        return entityBuilder(data)
                .type(EntityType.DEATH_ZONE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode
                        ? new Rectangle(width, height, Color.rgb(255, 0, 0, 0.35))
                        : new Rectangle(0, 0, Color.TRANSPARENT))
                /*
                 * 沒有 PhysicsComponent：
                 * 不會擋住玩家，不會推擠玩家，不會撞右側牆。
                 * 但仍然可被 FXGL collision handler 偵測。
                 */
                .with(new CollidableComponent(true))
                .with(new LethalComponent(deathReason))
                .zIndex(1000)
                .build();
    }

    @Spawns("entrance_door")
    public Entity newEntranceDoor(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        double interactRange = data.hasKey("interactRange")
                ? data.get("interactRange")
                : 180.0;

        boolean promptOnEntity = data.hasKey("promptOnEntity")
                ? data.get("promptOnEntity")
                : false;

        double promptOffsetY = data.hasKey("promptOffsetY")
                ? data.get("promptOffsetY")
                : 35.0;

        SceneManager sceneManager = data.get("sceneManager");

        QuestSystem questSystem = QuestSystem.getInstance();

        return entityBuilder(data)
                .type(EntityType.INTERACTABLE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode
                        ? new Rectangle(width, height, Color.rgb(255, 255, 0, 0.35))
                        : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(new InteractableComponent(
                        () -> "story.street.enter",
                        () -> {
                            if (getb("playerDead")) {
                                return;
                            }

                            InteractionSystem.lockAllInteractions(0.65);

                            sceneManager.loadHouseSceneAt(43.0, 452.0);
                        },
                        interactRange,
                        promptOnEntity,
                        promptOffsetY
                ))
                .zIndex(1000)
                .build();
    }

    private Color getApartmentColor(StreetApartmentStyle style) {
        return switch (style) {
            case LEFT -> Color.rgb(150, 95, 80);
            case RIGHT -> Color.rgb(110, 130, 160);
            case CENTER -> Color.rgb(130, 110, 150);
            case FILL -> Color.rgb(145, 145, 105);
            case EMPTY -> Color.TRANSPARENT;
        };
    }

    private Color getForegroundColor(StreetApartmentStyle style) {
        return switch (style) {
            case LEFT -> Color.rgb(220, 160, 120, 0.45);
            case RIGHT -> Color.rgb(170, 210, 240, 0.45);
            case CENTER -> Color.rgb(220, 180, 240, 0.45);
            case FILL -> Color.rgb(230, 230, 160, 0.45);
            case EMPTY -> Color.TRANSPARENT;
        };
    }
}