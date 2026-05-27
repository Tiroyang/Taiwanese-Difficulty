package ass.example.factories;

import ass.example.Main;
import ass.example.components.PlayerComponent;
import ass.example.core.CollisionCategory;
import ass.example.core.EntityType;
import ass.example.core.FixtureFilterUtil;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.*;
import com.almasb.fxgl.physics.box2d.dynamics.BodyDef;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * 玩家Entity生成處理器
 */
public class PlayerFactory implements EntityFactory {

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();

        BodyDef bodyDef = new BodyDef();
        bodyDef.setFixedRotation(true);
        physics.setBodyDef(bodyDef);

        physics.setBodyType(BodyType.DYNAMIC);

        FixtureDef fixtureDef = new FixtureDef()
                        .density(1.0f)
                        .friction(0.0f)
                        .restitution(0.0f);

        FixtureFilterUtil.applyFilter(
                fixtureDef,
                CollisionCategory.PLAYER,
                (short) (CollisionCategory.FLOOR | CollisionCategory.WALL)
        );

        physics.setFixtureDef(fixtureDef);

        Image stand = image("characters/player/stand.png");
        Image walkr0 = image("characters/player/walkr1.png");
        Image walkr1 = image("characters/player/walkr2.png");
        Image walkr2 = image("characters/player/walkr3.png");
        Image walkl0 = image("characters/player/walkl1.png");
        Image walkl1 = image("characters/player/walkl2.png");
        Image walkl2 = image("characters/player/walkl3.png");
        Image dashr0 = image("characters/player/dashr1.png");
        Image dashr1 = image("characters/player/dashr2.png");
        Image dashl0 = image("characters/player/dashl1.png");
        Image dashl1 = image("characters/player/dashl2.png");
        Image dead = image("characters/player/dead.png");
        Image standShoeless = image("characters/player/stand_shoeless.png");
        Image walkr0Shoeless = image("characters/player/walkr1_shoeless.png");
        Image walkr1Shoeless = image("characters/player/walkr2_shoeless.png");
        Image walkr2Shoeless = image("characters/player/walkr3_shoeless.png");
        Image walkl0Shoeless = image("characters/player/walkl1_shoeless.png");
        Image walkl1Shoeless = image("characters/player/walkl2_shoeless.png");
        Image walkl2Shoeless = image("characters/player/walkl3_shoeless.png");
        Image dashr0Shoeless = image("characters/player/dashr1_shoeless.png");
        Image dashr1Shoeless = image("characters/player/dashr2_shoeless.png");
        Image dashl0Shoeless = image("characters/player/dashl1_shoeless.png");
        Image dashl1Shoeless = image("characters/player/dashl2_shoeless.png");

        ImageView playerView = new ImageView(stand);
        playerView.setFitHeight(242);
        playerView.setPreserveRatio(true);
        playerView.setSmooth(false);

        /*
         * 碰撞箱依角色實際大小調整。
         */
        double bodyOffsetX = 50;
        double bodyOffsetY = 0;
        double bodyWidth = 39;
        double bodyHeight = 242;

        return entityBuilder(data)
                .type(EntityType.PLAYER)
                .view(playerView)
                .bbox(new HitBox(
                        "PLAYER_BODY",
                        new Point2D(bodyOffsetX, bodyOffsetY),
                        BoundingShape.box(bodyWidth, bodyHeight)
                ))
                .with(physics)
                .with(new CollidableComponent(true))
                .with(new PlayerComponent(
                        playerView,
                        stand,
                        new Image[]{walkr0, walkr1, walkr2, walkr1},
                        new Image[]{walkl0, walkl1, walkl2, walkl1},
                        new Image[]{dashr0, dashr1},
                        new Image[]{dashl0, dashl1},
                        standShoeless,
                        new Image[]{walkr0Shoeless, walkr1Shoeless, walkr2Shoeless, walkr1Shoeless},
                        new Image[]{walkl0Shoeless, walkl1Shoeless, walkl2Shoeless, walkl1Shoeless},
                        new Image[]{dashr0Shoeless, dashr1Shoeless},
                        new Image[]{dashl0Shoeless, dashl1Shoeless},
                        dead
                ))
                .zIndex(0)
                .build();
    }

    @Spawns("player_ground_sensor")
    public Entity newPlayerGroundSensor(SpawnData data) {

        double width = data.get("width");
        double height = data.get("height");

        return entityBuilder(data)
                .type(EntityType.PLAYER_GROUND_SENSOR)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(Main.devMode
                        ? new Rectangle(width, height, Color.rgb(37, 255, 0, 0.5))
                        : new Rectangle(0, 0, Color.TRANSPARENT))
                .with(new CollidableComponent(true))
                .zIndex(4000)
                .build();
    }
}