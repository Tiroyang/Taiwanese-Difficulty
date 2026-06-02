package ass.example.factories;

import ass.example.Main;
import ass.example.components.LethalComponent;
import ass.example.components.OneWayPlatformComponent;
import ass.example.core.physics.CollisionCategory;
import ass.example.core.DeathReason;
import ass.example.core.EntityType;
import ass.example.core.physics.FixtureFilterUtil;
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

public class CommonFactory implements EntityFactory {

    @Spawns("floor")
    public Entity newFloor(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        FixtureDef fixtureDef = new FixtureDef()
                .friction(0.8f)
                .restitution(0.0f);

        /*
         * floor 的碰撞分類：
         *
         * category = FLOOR
         * mask = PLAYER + FALLING_OBJECT
         *
         * 代表：
         * 玩家會踩在地板上。
         * 墜落物會撞到地板。
         * 但普通 wall / 右側牆 / 變電箱不一定要跟它互撞。
         */
        FixtureFilterUtil.applyFilter(
                fixtureDef,
                CollisionCategory.FLOOR,
                (short) (CollisionCategory.PLAYER | CollisionCategory.FALLING_OBJECT)
        );

        physics.setFixtureDef(fixtureDef);

        return entityBuilder(data)
                .type(EntityType.WALL)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode ? new Rectangle(width, height, Color.rgb(0, 255, 120, 0.35)) : new Rectangle(0, 0, Color.TRANSPARENT))
                .zIndex(1000)
                .with(physics)
                .with(new CollidableComponent(true))
                .build();
    }

    @Spawns("wall")
    public Entity newWall(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        /*
         * 預設 wall 是普通牆：
         * 只跟玩家碰撞，不跟墜落物碰撞。
         */
        short categoryBits = data.hasKey("categoryBits")
                ? ((Number) data.get("categoryBits")).shortValue()
                : CollisionCategory.WALL;

        short maskBits = data.hasKey("maskBits")
                ? ((Number) data.get("maskBits")).shortValue()
                : CollisionCategory.PLAYER;

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        FixtureDef fixtureDef = new FixtureDef()
                .friction(0.0f)
                .restitution(0.0f);

        FixtureFilterUtil.applyFilter(
                fixtureDef,
                categoryBits,
                maskBits
        );

        physics.setFixtureDef(fixtureDef);

        return entityBuilder(data)
                .type(EntityType.WALL)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode ? new Rectangle(width, height, javafx.scene.paint.Color.rgb(255, 0, 0, 0.5)) : new Rectangle(0, 0, Color.TRANSPARENT))
                .zIndex(1000)
                .with(physics)
                .with(new CollidableComponent(true))
                .build();
    }

    @Spawns("slope_wall")
    public Entity newSlopeWall(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");
        double angle = data.get("angle");

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        FixtureDef fixtureDef = new FixtureDef()
                .friction(0.0f)
                .restitution(0.0f);

        FixtureFilterUtil.applyFilter(
                fixtureDef,
                CollisionCategory.WALL,
                CollisionCategory.PLAYER
        );

        physics.setFixtureDef(fixtureDef);

        Rectangle view = Main.devMode
                ? new Rectangle(width, height, Color.rgb(0, 255, 255, 0.35))
                : new Rectangle(0, 0, Color.TRANSPARENT);

        return entityBuilder(data)
                .type(EntityType.WALL)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(view)
                .rotate(angle)
                .with(physics)
                .with(new CollidableComponent(true))
                .zIndex(1000)
                .build();
    }

    @Spawns("death_zone")
    public Entity newDeathZone(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        DeathReason deathReason = data.get("deathReason");

        return entityBuilder(data)
                .type(EntityType.DEATH_ZONE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode
                        ? new Rectangle(width, height, Color.rgb(255, 0, 255, 0.35))
                        : new Rectangle(0, 0, Color.TRANSPARENT))
                /*
                 * 重點：
                 * 不加 PhysicsComponent，所以它不會擋住玩家。
                 * 只用 bbox + CollidableComponent + LethalComponent 做觸發死亡。
                 */
                .with(new CollidableComponent(true))
                .with(new LethalComponent(deathReason))
                .zIndex(1000)
                .build();
    }

    @Spawns("death_wall")
    public Entity newDeathWall(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        DeathReason deathReason = data.get("deathReason");

        short categoryBits = data.hasKey("categoryBits")
                ? ((Number) data.get("categoryBits")).shortValue()
                : CollisionCategory.WALL;

        short maskBits = data.hasKey("maskBits")
                ? ((Number) data.get("maskBits")).shortValue()
                : CollisionCategory.PLAYER;

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        FixtureDef fixtureDef = new FixtureDef()
                .friction(0.0f)
                .restitution(0.0f);

        FixtureFilterUtil.applyFilter(
                fixtureDef,
                categoryBits,
                maskBits
        );

        physics.setFixtureDef(fixtureDef);

        return entityBuilder(data)
                .type(EntityType.DEATH_ZONE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode ? new Rectangle(width, height, javafx.scene.paint.Color.rgb(115, 0, 255, 0.5)) : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(physics)
                .with(new CollidableComponent(true))
                .with(new LethalComponent(deathReason))
                .zIndex(1000)
                .build();
    }

    @Spawns("one_way_platform")
    public Entity newOneWayPlatform(SpawnData data) {
        String id = data.get("id");

        double width = data.get("width");
        double height = data.get("height");

        int playerZIndexOnTop = data.hasKey("playerZIndexOnTop")
                ? ((Number) data.get("playerZIndexOnTop")).intValue()
                : 10;

        return entityBuilder(data)
                .type(EntityType.ONE_WAY_PLATFORM)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode ? new Rectangle(width, height, javafx.scene.paint.Color.rgb(0, 180, 255, 0.35)) : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(new CollidableComponent(true))
                .with(new OneWayPlatformComponent(
                        id,
                        width,
                        height,
                        playerZIndexOnTop
                ))
                .zIndex(1000)
                .build();
    }

    @Spawns("one_way_platform_collider")
    public Entity newOneWayPlatformCollider(SpawnData data) {
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
                .type(EntityType.ONE_WAY_PLATFORM_COLLIDER)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode ? new Rectangle(width, height, javafx.scene.paint.Color.rgb(0, 255, 196, 0.35)) : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(physics)
                .with(new CollidableComponent(true))
                .zIndex(1000)
                .build();
    }
}