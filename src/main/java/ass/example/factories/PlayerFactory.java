package ass.example.factories;

import ass.example.Main;
import ass.example.components.PlayerComponent;
import ass.example.core.EntityType;
import ass.example.core.physics.CollisionCategory;
import ass.example.core.physics.FixtureFilterUtil;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
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
 * PlayerFactory
 *
 * 玩家 Entity 生成工廠。
 *
 * 負責生成：
 *
 * 1. player
 *    - 玩家本體。
 *    - 擁有 PhysicsComponent。
 *    - 擁有 CollidableComponent。
 *    - 擁有 PlayerComponent。
 *    - 使用 ImageView 顯示玩家圖片。
 *
 * 2. player_ground_sensor
 *    - 玩家腳底感測器。
 *    - 由 PlayerComponent 動態生成。
 *    - 用於判斷玩家是否站在地面上。
 *
 * 設計原則：
 * - Factory 只負責 Entity 組裝。
 * - 玩家控制、動畫、跳躍、衝刺邏輯交給 PlayerComponent。
 */
public class PlayerFactory implements EntityFactory {

    // =========================================================
    // Player View Settings
    // =========================================================

    /**
     * 玩家圖片顯示高度。
     *
     * ImageView 會 preserveRatio，
     * 所以寬度會依照圖片比例自動計算。
     */
    private static final double PLAYER_VIEW_HEIGHT = 242.0;

    /**
     * 玩家 Entity 預設 zIndex。
     */
    private static final int PLAYER_Z_INDEX = 0;


    // =========================================================
    // Player HitBox Settings
    // =========================================================

    /**
     * 玩家主碰撞箱名稱。
     */
    private static final String PLAYER_BODY_HITBOX_NAME = "PLAYER_BODY";

    /**
     * 玩家碰撞箱相對圖片左上角的 X 偏移。
     */
    private static final double PLAYER_BODY_OFFSET_X = 50.0;

    /**
     * 玩家碰撞箱相對圖片左上角的 Y 偏移。
     */
    private static final double PLAYER_BODY_OFFSET_Y = 0.0;

    /**
     * 玩家碰撞箱寬度。
     */
    private static final double PLAYER_BODY_WIDTH = 39.0;

    /**
     * 玩家碰撞箱高度。
     */
    private static final double PLAYER_BODY_HEIGHT = 242.0;


    // =========================================================
    // Player Physics Settings
    // =========================================================

    /**
     * 玩家密度。
     */
    private static final float PLAYER_DENSITY = 1.0f;

    /**
     * 玩家摩擦力。
     *
     * 目前設為 0，避免玩家貼牆或移動時卡住。
     */
    private static final float PLAYER_FRICTION = 0.0f;

    /**
     * 玩家彈性。
     */
    private static final float PLAYER_RESTITUTION = 0.0f;


    // =========================================================
    // Ground Sensor Settings
    // =========================================================

    /**
     * 腳底感測器 zIndex。
     *
     * devMode 下放在最上層方便確認位置。
     */
    private static final int GROUND_SENSOR_Z_INDEX = 4000;

    /**
     * 腳底感測器 devMode 顏色。
     */
    private static final Color GROUND_SENSOR_DEBUG_COLOR =
            Color.rgb(37, 255, 0, 0.5);


    // =========================================================
    // Spawn - Player
    // =========================================================

    /**
     * 生成玩家 Entity。
     *
     * 玩家包含：
     * 1. 玩家圖片 ImageView。
     * 2. 玩家主碰撞箱。
     * 3. PhysicsComponent。
     * 4. CollidableComponent。
     * 5. PlayerComponent。
     *
     * @param data SpawnData，通常包含玩家初始座標
     * @return 玩家 Entity
     */
    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        PlayerImages images = loadPlayerImages();

        ImageView playerView = createPlayerView(images.stand);

        PhysicsComponent physics = createPlayerPhysics();

        PlayerComponent playerComponent = createPlayerComponent(
                playerView,
                images
        );

        return entityBuilder(data)
                .type(EntityType.PLAYER)
                .view(playerView)
                .bbox(createPlayerBodyHitBox())
                .with(physics)
                .with(new CollidableComponent(true))
                .with(playerComponent)
                .zIndex(PLAYER_Z_INDEX)
                .build();
    }


    // =========================================================
    // Spawn - Ground Sensor
    // =========================================================

    /**
     * 生成玩家腳底感測器。
     *
     * 此 Entity 通常由 PlayerComponent.createGroundSensor() 動態生成。
     *
     * 功能：
     * - 不負責物理阻擋。
     * - 只負責透過 bbox + CollidableComponent 偵測玩家是否接觸地面。
     *
     * SpawnData 需要：
     * - width
     * - height
     *
     * @param data SpawnData
     * @return 腳底感測器 Entity
     */
    @Spawns("player_ground_sensor")
    public Entity newPlayerGroundSensor(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        return entityBuilder(data)
                .type(EntityType.PLAYER_GROUND_SENSOR)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createDebugView(width, height, GROUND_SENSOR_DEBUG_COLOR))
                .with(new CollidableComponent(true))
                .zIndex(GROUND_SENSOR_Z_INDEX)
                .build();
    }


    // =========================================================
    // Player Component Creation
    // =========================================================

    /**
     * 建立 PlayerComponent。
     *
     * 這裡會將所有玩家圖片資源組合成 PlayerComponent 需要的格式。
     *
     * @param playerView 玩家 ImageView
     * @param images 玩家圖片資源
     * @return PlayerComponent
     */
    private PlayerComponent createPlayerComponent(
            ImageView playerView,
            PlayerImages images
    ) {
        return new PlayerComponent(
                playerView,

                images.stand,
                images.walkRight,
                images.walkLeft,
                images.dashRight,
                images.dashLeft,

                images.standShoeless,
                images.walkRightShoeless,
                images.walkLeftShoeless,
                images.dashRightShoeless,
                images.dashLeftShoeless,

                images.dead
        );
    }


    // =========================================================
    // Player View Creation
    // =========================================================

    /**
     * 建立玩家 ImageView。
     *
     * 設定：
     * - 高度固定為 PLAYER_VIEW_HEIGHT。
     * - 保持圖片比例。
     * - 關閉 smooth，保留像素風格。
     *
     * @param standImage 玩家站立圖片
     * @return ImageView
     */
    private ImageView createPlayerView(Image standImage) {
        ImageView playerView = new ImageView(standImage);

        playerView.setFitHeight(PLAYER_VIEW_HEIGHT);
        playerView.setPreserveRatio(true);
        playerView.setSmooth(false);

        return playerView;
    }

    /**
     * 建立玩家主碰撞箱。
     *
     * 碰撞箱依照角色實際大小調整，
     * 不一定等於整張圖片大小。
     *
     * @return HitBox
     */
    private HitBox createPlayerBodyHitBox() {
        return new HitBox(
                PLAYER_BODY_HITBOX_NAME,
                new Point2D(
                        PLAYER_BODY_OFFSET_X,
                        PLAYER_BODY_OFFSET_Y
                ),
                BoundingShape.box(
                        PLAYER_BODY_WIDTH,
                        PLAYER_BODY_HEIGHT
                )
        );
    }


    // =========================================================
    // Player Physics Creation
    // =========================================================

    /**
     * 建立玩家 PhysicsComponent。
     *
     * 設定：
     * - BodyType.DYNAMIC。
     * - 固定旋轉，避免玩家碰撞後歪掉。
     * - 套用玩家碰撞分類。
     *
     * @return PhysicsComponent
     */
    private PhysicsComponent createPlayerPhysics() {
        PhysicsComponent physics = new PhysicsComponent();

        physics.setBodyDef(createPlayerBodyDef());
        physics.setBodyType(BodyType.DYNAMIC);
        physics.setFixtureDef(createPlayerFixtureDef());

        return physics;
    }

    /**
     * 建立玩家 BodyDef。
     *
     * fixedRotation = true：
     * - 防止玩家因物理碰撞旋轉。
     *
     * @return BodyDef
     */
    private BodyDef createPlayerBodyDef() {
        BodyDef bodyDef = new BodyDef();

        bodyDef.setFixedRotation(true);

        return bodyDef;
    }

    /**
     * 建立玩家 FixtureDef。
     *
     * category：
     * - PLAYER
     *
     * mask：
     * - FLOOR
     * - WALL
     *
     * 代表玩家會與地板、牆壁碰撞。
     *
     * @return FixtureDef
     */
    private FixtureDef createPlayerFixtureDef() {
        FixtureDef fixtureDef = new FixtureDef()
                .density(PLAYER_DENSITY)
                .friction(PLAYER_FRICTION)
                .restitution(PLAYER_RESTITUTION);

        FixtureFilterUtil.applyFilter(
                fixtureDef,
                CollisionCategory.PLAYER,
                (short) (CollisionCategory.FLOOR | CollisionCategory.WALL)
        );

        return fixtureDef;
    }


    // =========================================================
    // Player Image Loading
    // =========================================================

    /**
     * 載入所有玩家圖片。
     *
     * 分成：
     * 1. 穿鞋圖片。
     * 2. 赤腳圖片。
     * 3. 特殊圖片。
     *
     * @return PlayerImages
     */
    private PlayerImages loadPlayerImages() {
        Image stand = image("characters/player/stand.png");

        Image walkRight1 = image("characters/player/walkr1.png");
        Image walkRight2 = image("characters/player/walkr2.png");
        Image walkRight3 = image("characters/player/walkr3.png");

        Image walkLeft1 = image("characters/player/walkl1.png");
        Image walkLeft2 = image("characters/player/walkl2.png");
        Image walkLeft3 = image("characters/player/walkl3.png");

        Image dashRight1 = image("characters/player/dashr1.png");
        Image dashRight2 = image("characters/player/dashr2.png");

        Image dashLeft1 = image("characters/player/dashl1.png");
        Image dashLeft2 = image("characters/player/dashl2.png");

        Image standShoeless = image("characters/player/stand_shoeless.png");

        Image walkRightShoeless1 = image("characters/player/walkr1_shoeless.png");
        Image walkRightShoeless2 = image("characters/player/walkr2_shoeless.png");
        Image walkRightShoeless3 = image("characters/player/walkr3_shoeless.png");

        Image walkLeftShoeless1 = image("characters/player/walkl1_shoeless.png");
        Image walkLeftShoeless2 = image("characters/player/walkl2_shoeless.png");
        Image walkLeftShoeless3 = image("characters/player/walkl3_shoeless.png");

        Image dashRightShoeless1 = image("characters/player/dashr1_shoeless.png");
        Image dashRightShoeless2 = image("characters/player/dashr2_shoeless.png");

        Image dashLeftShoeless1 = image("characters/player/dashl1_shoeless.png");
        Image dashLeftShoeless2 = image("characters/player/dashl2_shoeless.png");

        Image dead = image("characters/player/dead.png");

        return new PlayerImages(
                stand,

                new Image[]{
                        walkRight1,
                        walkRight2,
                        walkRight3,
                        walkRight2
                },
                new Image[]{
                        walkLeft1,
                        walkLeft2,
                        walkLeft3,
                        walkLeft2
                },
                new Image[]{
                        dashRight1,
                        dashRight2
                },
                new Image[]{
                        dashLeft1,
                        dashLeft2
                },

                standShoeless,

                new Image[]{
                        walkRightShoeless1,
                        walkRightShoeless2,
                        walkRightShoeless3,
                        walkRightShoeless2
                },
                new Image[]{
                        walkLeftShoeless1,
                        walkLeftShoeless2,
                        walkLeftShoeless3,
                        walkLeftShoeless2
                },
                new Image[]{
                        dashRightShoeless1,
                        dashRightShoeless2
                },
                new Image[]{
                        dashLeftShoeless1,
                        dashLeftShoeless2
                },

                dead
        );
    }


    // =========================================================
    // Debug View Helpers
    // =========================================================

    /**
     * 建立 devMode 用 debug 顯示框。
     *
     * devMode == true：
     * - 顯示指定大小與顏色的半透明矩形。
     *
     * devMode == false：
     * - 顯示透明 0x0 Rectangle。
     *
     * @param width 寬度
     * @param height 高度
     * @param debugColor debug 顯示顏色
     * @return Rectangle
     */
    private Rectangle createDebugView(
            double width,
            double height,
            Color debugColor
    ) {
        if (Main.devMode) {
            return new Rectangle(width, height, debugColor);
        }

        return new Rectangle(0, 0, Color.TRANSPARENT);
    }


    // =========================================================
    // Image Data Holder
    // =========================================================

    /**
     * PlayerImages
     *
     * 玩家圖片資料容器。
     *
     * 用途：
     * - 避免 newPlayer() 中出現大量零散 Image 變數。
     * - 讓 PlayerComponent 建立時更容易閱讀。
     */
    private static class PlayerImages {

        // =====================================================
        // Images - With Shoes
        // =====================================================

        private final Image stand;
        private final Image[] walkRight;
        private final Image[] walkLeft;
        private final Image[] dashRight;
        private final Image[] dashLeft;


        // =====================================================
        // Images - Shoeless
        // =====================================================

        private final Image standShoeless;
        private final Image[] walkRightShoeless;
        private final Image[] walkLeftShoeless;
        private final Image[] dashRightShoeless;
        private final Image[] dashLeftShoeless;


        // =====================================================
        // Images - Special
        // =====================================================

        private final Image dead;


        /**
         * 建立玩家圖片資料容器。
         */
        private PlayerImages(
                Image stand,

                Image[] walkRight,
                Image[] walkLeft,
                Image[] dashRight,
                Image[] dashLeft,

                Image standShoeless,

                Image[] walkRightShoeless,
                Image[] walkLeftShoeless,
                Image[] dashRightShoeless,
                Image[] dashLeftShoeless,

                Image dead
        ) {
            this.stand = stand;

            this.walkRight = walkRight;
            this.walkLeft = walkLeft;
            this.dashRight = dashRight;
            this.dashLeft = dashLeft;

            this.standShoeless = standShoeless;

            this.walkRightShoeless = walkRightShoeless;
            this.walkLeftShoeless = walkLeftShoeless;
            this.dashRightShoeless = dashRightShoeless;
            this.dashLeftShoeless = dashLeftShoeless;

            this.dead = dead;
        }
    }
}