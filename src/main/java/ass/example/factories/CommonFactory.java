package ass.example.factories;

import ass.example.Main;
import ass.example.components.LethalComponent;
import ass.example.components.OneWayPlatformComponent;
import ass.example.core.DeathReason;
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
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

/**
 * CommonFactory
 *
 * 通用 EntityFactory。
 *
 * 負責生成多個場景都可能會使用的基礎 Entity：
 *
 * 1. 地板：
 *    - floor
 *
 * 2. 牆壁：
 *    - wall
 *    - slope_wall
 *
 * 3. 死亡區：
 *    - death_zone
 *    - death_wall
 *
 * 4. 單向平台：
 *    - one_way_platform
 *    - one_way_platform_collider
 *
 * 設計原則：
 * - CommonFactory 只負責建立通用物件。
 * - 場景專用物件應放到各自的 Factory，例如 HouseFactory、StreetFactory。
 * - 物理碰撞分類統一透過 CollisionCategory 與 FixtureFilterUtil 設定。
 */
public class CommonFactory implements EntityFactory {
 
    // Z Index 

    /**
     * Debug 碰撞框與觸發區的顯示層級。
     *
     * devMode 下希望顯示在最上層方便除錯。
     */
    private static final int Z_DEBUG_COLLIDER = 1000;

 
    // Default Physics Settings 

    /**
     * 地板摩擦力。
     *
     * 玩家站在地板上需要一定摩擦，
     * 避免過度滑動。
     */
    private static final float FLOOR_FRICTION = 0.8f;

    /**
     * 地板彈性。
     */
    private static final float FLOOR_RESTITUTION = 0.0f;

    /**
     * 牆壁摩擦力。
     */
    private static final float WALL_FRICTION = 0.0f;

    /**
     * 牆壁彈性。
     */
    private static final float WALL_RESTITUTION = 0.0f;

    /**
     * 單向平台實體 collider 摩擦力。
     */
    private static final float ONE_WAY_PLATFORM_FRICTION = 0.0f;

    /**
     * 單向平台實體 collider 彈性。
     */
    private static final float ONE_WAY_PLATFORM_RESTITUTION = 0.0f;

 
    // Default Spawn Settings 

    /**
     * 單向平台預設玩家 zIndex。
     *
     * 玩家站到平台上時，
     * OneWayPlatformSystem 可用此數值調整玩家圖層。
     */
    private static final int DEFAULT_PLAYER_Z_INDEX_ON_PLATFORM = 10;

 
    // Dev View Colors 

    private static final Color COLOR_FLOOR_DEBUG = Color.rgb(0, 255, 120, 0.35);
    private static final Color COLOR_WALL_DEBUG = Color.rgb(255, 0, 0, 0.5);
    private static final Color COLOR_SLOPE_WALL_DEBUG = Color.rgb(0, 255, 255, 0.35);
    private static final Color COLOR_DEATH_ZONE_DEBUG = Color.rgb(255, 0, 255, 0.35);
    private static final Color COLOR_DEATH_WALL_DEBUG = Color.rgb(115, 0, 255, 0.5);
    private static final Color COLOR_ONE_WAY_PLATFORM_DEBUG = Color.rgb(0, 180, 255, 0.35);
    private static final Color COLOR_ONE_WAY_PLATFORM_COLLIDER_DEBUG = Color.rgb(0, 255, 196, 0.35);

 
    // Spawn - Floor / Walls 

    /**
     * 生成地板。
     *
     * floor 的物理設定：
     *
     * category：
     * - FLOOR
     *
     * mask：
     * - PLAYER
     * - FALLING_OBJECT
     *
     * SpawnData 需要：
     * - width
     * - height
     */
    @Spawns("floor")
    public Entity newFloor(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        PhysicsComponent physics = createStaticPhysics(createFloorFixtureDef());

        return createSolidEntity(
                data,
                EntityType.FLOOR,
                width,
                height,
                COLOR_FLOOR_DEBUG,
                physics
        );
    }

    /**
     * 生成一般牆壁。
     *
     * 預設 wall：
     * - category = WALL
     * - mask = PLAYER
     *
     * 代表：
     * - 牆壁只阻擋玩家。
     * - 不會與墜落物碰撞。
     *
     * 若需要特殊碰撞規則，
     * 可以從 SpawnData 傳入：
     * - categoryBits
     * - maskBits
     *
     * SpawnData 需要：
     * - width
     * - height
     */
    @Spawns("wall")
    public Entity newWall(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        short categoryBits = getShort(
                data,
                "categoryBits",
                CollisionCategory.WALL
        );

        short maskBits = getShort(
                data,
                "maskBits",
                CollisionCategory.PLAYER
        );

        PhysicsComponent physics = createStaticPhysics(
                createWallFixtureDef(categoryBits, maskBits)
        );

        return createSolidEntity(
                data,
                EntityType.WALL,
                width,
                height,
                COLOR_WALL_DEBUG,
                physics
        );
    }

    /**
     * 生成斜牆。
     *
     * 主要用於：
     * - 斜坡
     * - 浴缸內凹曲線
     * - 其他需要旋轉碰撞箱的牆壁
     *
     * SpawnData 需要：
     * - width
     * - height
     * - angle
     */
    @Spawns("slope_wall")
    public Entity newSlopeWall(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");
        double angle = data.get("angle");

        PhysicsComponent physics = createStaticPhysics(
                createWallFixtureDef(
                        CollisionCategory.WALL,
                        CollisionCategory.PLAYER
                )
        );

        return entityBuilder(data)
                .type(EntityType.WALL)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createDebugView(width, height, COLOR_SLOPE_WALL_DEBUG))
                .rotate(angle)
                .with(physics)
                .with(new CollidableComponent(true))
                .zIndex(Z_DEBUG_COLLIDER)
                .build();
    }

 
    // Spawn - Death Zones 

    /**
     * 生成不阻擋玩家的死亡觸發區。
     *
     * death_zone 特性：
     * - 有 bbox。
     * - 有 CollidableComponent。
     * - 有 LethalComponent。
     * - 沒有 PhysicsComponent。
     *
     * 因此它不會擋住玩家，
     * 只會在碰撞系統偵測到接觸時觸發死亡。
     *
     * SpawnData 需要：
     * - width
     * - height
     * - deathReason
     */
    @Spawns("death_zone")
    public Entity newDeathZone(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");
        DeathReason deathReason = data.get("deathReason");

        return entityBuilder(data)
                .type(EntityType.DEATH_ZONE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createDebugView(width, height, COLOR_DEATH_ZONE_DEBUG))
                .with(new CollidableComponent(true))
                .with(new LethalComponent(deathReason))
                .zIndex(Z_DEBUG_COLLIDER)
                .build();
    }

    /**
     * 生成會阻擋玩家的死亡牆。
     *
     * death_wall 特性：
     * - 有 bbox。
     * - 有 PhysicsComponent。
     * - 有 CollidableComponent。
     * - 有 LethalComponent。
     *
     * 既會阻擋玩家，也會在玩家碰撞時觸發死亡。
     *
     * 預設：
     * - category = WALL
     * - mask = PLAYER
     *
     * SpawnData 需要：
     * - width
     * - height
     * - deathReason
     *
     * 可選：
     * - categoryBits
     * - maskBits
     */
    @Spawns("death_wall")
    public Entity newDeathWall(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");
        DeathReason deathReason = data.get("deathReason");

        short categoryBits = getShort(
                data,
                "categoryBits",
                CollisionCategory.WALL
        );

        short maskBits = getShort(
                data,
                "maskBits",
                CollisionCategory.PLAYER
        );

        PhysicsComponent physics = createStaticPhysics(
                createWallFixtureDef(categoryBits, maskBits)
        );

        return entityBuilder(data)
                .type(EntityType.DEATH_ZONE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createDebugView(width, height, COLOR_DEATH_WALL_DEBUG))
                .with(physics)
                .with(new CollidableComponent(true))
                .with(new LethalComponent(deathReason))
                .zIndex(Z_DEBUG_COLLIDER)
                .build();
    }

 
    // Spawn - One Way Platform 

    /**
     * 生成單向平台入口。
     *
     * 這個 Entity 通常不直接作為真正地板使用。
     * OneWayPlatformSystem 會讀取它的位置與大小，
     * 判斷玩家是否從上方落下。
     *
     * 當玩家成功落到平台時，系統再動態生成 one_way_platform_collider 作為實體支撐。
     *
     * SpawnData 需要：
     * - id
     * - width
     * - height
     *
     * 可選：
     * - playerZIndexOnTop
     */
    @Spawns("one_way_platform")
    public Entity newOneWayPlatform(SpawnData data) {
        String id = data.get("id");

        double width = data.get("width");
        double height = data.get("height");

        int playerZIndexOnTop = getInt(
                data,
                "playerZIndexOnTop",
                DEFAULT_PLAYER_Z_INDEX_ON_PLATFORM
        );

        OneWayPlatformComponent oneWayPlatformComponent = new OneWayPlatformComponent(
                id,
                width,
                height,
                playerZIndexOnTop
        );

        return entityBuilder(data)
                .type(EntityType.ONE_WAY_PLATFORM)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createDebugView(width, height, COLOR_ONE_WAY_PLATFORM_DEBUG))
                .with(new CollidableComponent(true))
                .with(oneWayPlatformComponent)
                .zIndex(Z_DEBUG_COLLIDER)
                .build();
    }

    /**
     * 生成單向平台實體 collider。
     *
     * 此 Entity 通常由 OneWayPlatformSystem 動態生成。
     *
     * 用途：
     * - 玩家真正站在平台上時，支撐玩家。
     * - 玩家離開平台或下落時，由系統移除。
     *
     * SpawnData 需要：
     * - width
     * - height
     */
    @Spawns("one_way_platform_collider")
    public Entity newOneWayPlatformCollider(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        PhysicsComponent physics = createStaticPhysics(
                createOneWayPlatformFixtureDef()
        );

        return createSolidEntity(
                data,
                EntityType.ONE_WAY_PLATFORM_COLLIDER,
                width,
                height,
                COLOR_ONE_WAY_PLATFORM_COLLIDER_DEBUG,
                physics
        );
    }

 
    // Shared Entity Builders 

    /**
     * 建立具有實體碰撞的矩形 Entity。
     *
     * 適用於：
     * - floor
     * - wall
     * - one_way_platform_collider
     *
     * 共同特性：
     * - 有 bbox。
     * - 有 debug view。
     * - 有 PhysicsComponent。
     * - 有 CollidableComponent。
     *
     * @param data SpawnData
     * @param entityType Entity 類型
     * @param width 碰撞寬度
     * @param height 碰撞高度
     * @param debugColor devMode 顯示顏色
     * @param physics PhysicsComponent
     * @return 建立完成的 Entity
     */
    private Entity createSolidEntity(
            SpawnData data,
            EntityType entityType,
            double width,
            double height,
            Color debugColor,
            PhysicsComponent physics
    ) {
        return entityBuilder(data)
                .type(entityType)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createDebugView(width, height, debugColor))
                .with(physics)
                .with(new CollidableComponent(true))
                .zIndex(Z_DEBUG_COLLIDER)
                .build();
    }

 
    // Physics Helpers 

    /**
     * 建立 STATIC PhysicsComponent。
     *
     * @param fixtureDef FixtureDef
     * @return 靜態 PhysicsComponent
     */
    private PhysicsComponent createStaticPhysics(FixtureDef fixtureDef) {
        PhysicsComponent physics = new PhysicsComponent();

        physics.setBodyType(BodyType.STATIC);
        physics.setFixtureDef(fixtureDef);

        return physics;
    }

    /**
     * 建立地板 FixtureDef。
     *
     * category：
     * - FLOOR
     *
     * mask：
     * - PLAYER
     * - FALLING_OBJECT
     */
    private FixtureDef createFloorFixtureDef() {
        FixtureDef fixtureDef = new FixtureDef()
                .friction(FLOOR_FRICTION)
                .restitution(FLOOR_RESTITUTION);

        FixtureFilterUtil.applyFilter(
                fixtureDef,
                CollisionCategory.FLOOR,
                (short) (CollisionCategory.PLAYER | CollisionCategory.FALLING_OBJECT)
        );

        return fixtureDef;
    }

    /**
     * 建立牆壁 FixtureDef。
     *
     * @param categoryBits 此牆壁自己的碰撞分類
     * @param maskBits 此牆壁會碰撞的對象分類
     * @return FixtureDef
     */
    private FixtureDef createWallFixtureDef(
            short categoryBits,
            short maskBits
    ) {
        FixtureDef fixtureDef = new FixtureDef()
                .friction(WALL_FRICTION)
                .restitution(WALL_RESTITUTION);

        FixtureFilterUtil.applyFilter(
                fixtureDef,
                categoryBits,
                maskBits
        );

        return fixtureDef;
    }

    /**
     * 建立單向平台實體 collider FixtureDef。
     *
     * 注意：
     * 這裡保留原本行為：
     * - 沒有套用 FixtureFilterUtil。
     *
     * 如果你希望 one_way_platform_collider 也遵守碰撞分類，
     * 可以改成：
     *
     * FixtureFilterUtil.applyFilter(
     *         fixtureDef,
     *         CollisionCategory.FLOOR,
     *         CollisionCategory.PLAYER
     * );
     */
    private FixtureDef createOneWayPlatformFixtureDef() {
        return new FixtureDef()
                .friction(ONE_WAY_PLATFORM_FRICTION)
                .restitution(ONE_WAY_PLATFORM_RESTITUTION);
    }

 
    // SpawnData Helpers 

    /**
     * 從 SpawnData 讀取 int。
     *
     * FXGL SpawnData 中的數字可能是 Integer、Double 或其他 Number，
     * 因此統一透過 Number 轉型。
     *
     * @param data SpawnData
     * @param key 欄位名稱
     * @param defaultValue 預設值
     * @return int 數值
     */
    private int getInt(
            SpawnData data,
            String key,
            int defaultValue
    ) {
        if (!data.hasKey(key)) {
            return defaultValue;
        }

        return ((Number) data.get(key)).intValue();
    }

    /**
     * 從 SpawnData 讀取 short。
     *
     * 用於讀取 categoryBits / maskBits。
     *
     * @param data SpawnData
     * @param key 欄位名稱
     * @param defaultValue 預設值
     * @return short 數值
     */
    private short getShort(
            SpawnData data,
            String key,
            short defaultValue
    ) {
        if (!data.hasKey(key)) {
            return defaultValue;
        }

        return ((Number) data.get(key)).shortValue();
    }

 
    // Debug View Helpers 

    /**
     * 建立 devMode 用的矩形顯示。
     *
     * devMode == true：
     * - 回傳指定大小與顏色的半透明矩形。
     *
     * devMode == false：
     * - 回傳透明 0x0 矩形，不顯示碰撞框。
     *
     * @param width 寬度
     * @param height 高度
     * @param debugColor devMode 顯示顏色
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
}