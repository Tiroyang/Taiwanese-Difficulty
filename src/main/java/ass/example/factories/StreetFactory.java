package ass.example.factories;

import ass.example.Main;
import ass.example.components.InteractableComponent;
import ass.example.components.LethalComponent;
import ass.example.core.DeathReason;
import ass.example.core.EntityType;
import ass.example.core.StreetScene.FallingObjectVariant;
import ass.example.core.StreetScene.StreetApartmentStyle;
import ass.example.core.physics.CollisionCategory;
import ass.example.core.physics.FixtureFilterUtil;
import ass.example.scenes.system.SceneManager;
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

/**
 * StreetFactory
 *
 * StreetScene 專用 EntityFactory。
 *
 * 負責生成街道場景中的 Entity：
 *
 * 1. 背景圖層：
 *    - street_far_background
 *    - street_floor
 *    - street_apartment_bg
 *    - street_apartment_fg
 *
 * 2. 街道障礙物視覺：
 *    - street_transformer_box
 *    - street_protruding_tile
 *    - street_scooter
 *
 * 3. 街道死亡區：
 *    - street_scooter_death_wall
 *    - street_falling_object_trigger
 *
 * 4. 掉落物：
 *    - street_falling_object
 *
 * 5. 入口互動：
 *    - entrance_door
 *
 * - Factory 只負責組裝 Entity。
 * - 物件移動、生成時機、警告 UI、無盡街道邏輯由 StreetScene 或相關 System 處理。
 * - 致命判定透過 LethalComponent 保存 DeathReason。
 */
public class StreetFactory implements EntityFactory {

    // =========================================================
    // Texture Paths
    // =========================================================

    /**
     * 街道遠景背景貼圖。
     */
    private static final String TEXTURE_STREET_BACKGROUND =
            "/Scene2/map/Background.png";


    // =========================================================
    // Z Index
    // =========================================================

    /**
     * 遠景背景圖層。
     */
    private static final int Z_FAR_BACKGROUND = -500;

    /**
     * 公寓背景圖層。
     */
    private static final int Z_APARTMENT_BACKGROUND = -200;

    /**
     * 公寓前景圖層。
     */
    private static final int Z_APARTMENT_FOREGROUND = -100;

    /**
     * 街道路面視覺圖層。
     */
    private static final int Z_STREET_FLOOR = 10;

    /**
     * 變電箱圖層。
     */
    private static final int Z_TRANSFORMER_BOX = 9;

    /**
     * 凸起地磚圖層。
     */
    private static final int Z_PROTRUDING_TILE = -149;

    /**
     * 摩托車圖層。
     */
    private static final int Z_SCOOTER = -150;

    /**
     * 掉落物圖層。
     */
    private static final int Z_FALLING_OBJECT = 130;

    /**
     * debug 觸發區圖層。
     */
    private static final int Z_DEBUG_TRIGGER = 1000;


    // =========================================================
    // Default Interaction Settings
    // =========================================================

    private static final double DEFAULT_INTERACT_RANGE = 180.0;
    private static final boolean DEFAULT_PROMPT_ON_ENTITY = false;
    private static final double DEFAULT_PROMPT_OFFSET_Y = 35.0;


    // =========================================================
    // Physics Settings - Falling Object
    // =========================================================

    /**
     * 掉落物密度。
     */
    private static final float FALLING_OBJECT_DENSITY = 0.9f;

    /**
     * 掉落物摩擦力。
     */
    private static final float FALLING_OBJECT_FRICTION = 0.65f;

    /**
     * 掉落物彈性。
     */
    private static final float FALLING_OBJECT_RESTITUTION = 0.0f;


    // =========================================================
    // Dev View Colors
    // =========================================================

    private static final Color COLOR_DEBUG_INTERACTABLE =
            Color.rgb(255, 255, 0, 0.35);

    private static final Color COLOR_DEBUG_DEATH_WALL =
            Color.rgb(255, 0, 0, 0.42);

    private static final Color COLOR_DEBUG_FALLING_OBJECT_TRIGGER =
            Color.rgb(255, 0, 0, 0.35);


    // =========================================================
    // Visual Colors - Temporary Props
    // =========================================================

    private static final Color COLOR_STREET_FLOOR =
            Color.rgb(52, 52, 52);

    private static final Color COLOR_TRANSFORMER_BOX =
            Color.rgb(55, 95, 85);

    private static final Color COLOR_TRANSFORMER_BOX_STROKE =
            Color.rgb(210, 230, 120, 0.9);

    private static final Color COLOR_PROTRUDING_TILE =
            Color.rgb(255, 190, 35, 0.55);

    private static final Color COLOR_PROTRUDING_TILE_STROKE =
            Color.rgb(255, 255, 255, 0.85);

    private static final Color COLOR_SCOOTER =
            Color.rgb(35, 35, 35);

    private static final Color COLOR_SCOOTER_STROKE =
            Color.rgb(255, 80, 80, 0.95);


    // =========================================================
    // Spawn - Background / Floor
    // =========================================================

    /**
     * 生成街道遠景背景。
     *
     * SpawnData 需要：
     * - width
     * - height
     *
     * 注意：
     * 目前 width / height 沒直接用於 view，之後可用於縮放背景。
     */
    @Spawns("street_far_background")
    public Entity newStreetFarBackground(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view(TEXTURE_STREET_BACKGROUND)
                .zIndex(Z_FAR_BACKGROUND)
                .build();
    }

    /**
     * 生成街道路面視覺。
     *
     * 目前以色塊暫代，之後有素材時再改。
     *
     * SpawnData 需要：
     * - width
     * - height
     */
    @Spawns("street_floor")
    public Entity newStreetFloor(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        Rectangle view = createFilledRectangle(
                width,
                height,
                COLOR_STREET_FLOOR
        );

        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view(view)
                .zIndex(Z_STREET_FLOOR)
                .build();
    }


    // =========================================================
    // Spawn - Apartment Layers
    // =========================================================

    /**
     * 生成街道公寓背景層。
     *
     * SpawnData 需要：
     * - width
     * - height
     * - style：StreetApartmentStyle 名稱
     */
    @Spawns("street_apartment_bg")
    public Entity newStreetApartmentBackground(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        StreetApartmentStyle style = getApartmentStyle(data);

        Rectangle view = createFilledRectangle(
                width,
                height,
                getApartmentBackgroundColor(style)
        );

        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view(view)
                .zIndex(Z_APARTMENT_BACKGROUND)
                .build();
    }

    /**
     * 生成街道公寓前景層。
     *
     * SpawnData 需要：
     * - width
     * - height
     * - style：StreetApartmentStyle 名稱
     */
    @Spawns("street_apartment_fg")
    public Entity newStreetApartmentForeground(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        StreetApartmentStyle style = getApartmentStyle(data);

        Rectangle view = createFilledRectangle(
                width,
                height,
                getApartmentForegroundColor(style)
        );

        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view(view)
                .zIndex(Z_APARTMENT_FOREGROUND)
                .build();
    }


    // =========================================================
    // Spawn - Street Props Visuals
    // =========================================================

    /**
     * 生成街道變電箱視覺。
     *
     * 目前以色塊暫代，之後可以替換成圖片。
     *
     * SpawnData 需要：
     * - width
     * - height
     */
    @Spawns("street_transformer_box")
    public Entity newStreetTransformerBoxVisual(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        Rectangle view = createRoundedStrokeRectangle(
                width,
                height,
                10,
                COLOR_TRANSFORMER_BOX,
                COLOR_TRANSFORMER_BOX_STROKE,
                2
        );

        return entityBuilder(data)
                .type(EntityType.PROP)
                .view(view)
                .zIndex(Z_TRANSFORMER_BOX)
                .build();
    }

    /**
     * 生成街道凸起磁磚視覺。
     *
     * 目前以色塊暫代。
     *
     * SpawnData 需要：
     * - width
     * - height
     */
    @Spawns("street_protruding_tile")
    public Entity newStreetProtrudingTile(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        Rectangle view = createRoundedStrokeRectangle(
                width,
                height,
                8,
                COLOR_PROTRUDING_TILE,
                COLOR_PROTRUDING_TILE_STROKE,
                2
        );

        return entityBuilder(data)
                .type(EntityType.PROP)
                .view(view)
                .zIndex(Z_PROTRUDING_TILE)
                .build();
    }

    /**
     * 生成街道機車視覺。
     *
     * SpawnData 需要：
     * - width
     * - height
     *
     * 可選：
     * - fromLeft：機車是否從左往右來
     *
     * 注意：
     * 之後換成 ImageView 時，可沿用 fromLeft 控制方向。
     */
    @Spawns("street_scooter")
    public Entity newStreetScooter(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        boolean fromLeft = getBoolean(data, "fromLeft", true);

        Rectangle view = createRoundedStrokeRectangle(
                width,
                height,
                16,
                COLOR_SCOOTER,
                COLOR_SCOOTER_STROKE,
                3
        );

        view.setScaleX(fromLeft ? 1 : -1);

        return entityBuilder(data)
                .type(EntityType.PROP)
                .view(view)
                .zIndex(Z_SCOOTER)
                .build();
    }


    // =========================================================
    // Spawn - Death Zones
    // =========================================================

    /**
     * 生成機車死亡牆。
     *
     * 特性：
     * - 不含 PhysicsComponent。
     * - 不阻擋玩家。
     * - 只透過 bbox + CollidableComponent + LethalComponent 觸發死亡。
     *
     * SpawnData 需要：
     * - width
     * - height
     * - deathReason
     */
    @Spawns("street_scooter_death_wall")
    public Entity newScooterDeathWall(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        DeathReason deathReason = data.get("deathReason");

        return createLethalTrigger(
                data,
                width,
                height,
                deathReason,
                COLOR_DEBUG_DEATH_WALL
        );
    }

    /**
     * 生成掉落物死亡 trigger。
     *
     * 特性：
     * - 沒有 PhysicsComponent。
     * - 不會擋住玩家。
     * - 不會推擠玩家。
     * - 不會撞右側牆。
     * - 可被 FXGL collision handler 偵測。
     *
     * SpawnData 需要：
     * - width
     * - height
     * - deathReason
     */
    @Spawns("street_falling_object_trigger")
    public Entity newStreetFallingObjectTrigger(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        DeathReason deathReason = data.get("deathReason");

        return createLethalTrigger(
                data,
                width,
                height,
                deathReason,
                COLOR_DEBUG_FALLING_OBJECT_TRIGGER
        );
    }


    // =========================================================
    // Spawn - Falling Objects
    // =========================================================

    /**
     * 生成街道掉落物。
     *
     * 掉落物本身：
     * - 有 DYNAMIC PhysicsComponent。
     * - 只與 FLOOR 碰撞。
     * - 不直接作為死亡區使用。
     *
     * 玩家死亡通常由另一個 street_falling_object_trigger 處理。
     *
     * SpawnData 需要：
     * - variant：FallingObjectVariant 名稱
     */
    @Spawns("street_falling_object")
    public Entity newStreetFallingObject(SpawnData data) {
        FallingObjectVariant variant = getFallingObjectVariant(data);

        double width = variant.getWidth();
        double height = variant.getHeight();

        PhysicsComponent physics = createFallingObjectPhysics();

        Rectangle view = createFallingObjectView(width, height, variant);

        return entityBuilder(data)
                .type(EntityType.PROP)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(view)
                .with(physics)
                .zIndex(Z_FALLING_OBJECT)
                .build();
    }


    // =========================================================
    // Spawn - Entrance Door
    // =========================================================

    /**
     * 生成街道進屋入口互動區。
     *
     * 玩家互動後：
     * - 若玩家已死亡，直接忽略。
     * - 否則播放 Street -> House 轉場。
     *
     * SpawnData 需要：
     * - width
     * - height
     * - sceneManager
     *
     * 可選：
     * - interactRange
     * - promptOnEntity
     * - promptOffsetY
     */
    @Spawns("entrance_door")
    public Entity newEntranceDoor(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        double interactRange = getDouble(
                data,
                "interactRange",
                DEFAULT_INTERACT_RANGE
        );

        boolean promptOnEntity = getBoolean(
                data,
                "promptOnEntity",
                DEFAULT_PROMPT_ON_ENTITY
        );

        double promptOffsetY = getDouble(
                data,
                "promptOffsetY",
                DEFAULT_PROMPT_OFFSET_Y
        );

        SceneManager sceneManager = data.get("sceneManager");

        return entityBuilder(data)
                .type(EntityType.INTERACTABLE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createDebugView(width, height, COLOR_DEBUG_INTERACTABLE))
                .with(new InteractableComponent(
                        () -> "story.street.enter",
                        () -> enterHouse(sceneManager),
                        interactRange,
                        promptOnEntity,
                        promptOffsetY
                ))
                .zIndex(Z_DEBUG_TRIGGER)
                .build();
    }

    /**
     * 執行從街道回到家中的轉場。
     *
     * 若玩家已死亡，不執行轉場。
     *
     * @param sceneManager 場景管理器
     */
    private void enterHouse(SceneManager sceneManager) {
        if (getb("playerDead")) {
            return;
        }

        sceneManager.playStreetToHouseTransition(null);
    }


    // =========================================================
    // Shared Entity Builders
    // =========================================================

    /**
     * 建立不阻擋玩家的死亡 trigger。
     *
     * 共用於：
     * - street_scooter_death_wall
     * - street_falling_object_trigger
     *
     * @param data SpawnData
     * @param width 寬度
     * @param height 高度
     * @param deathReason 死亡原因
     * @param debugColor devMode 顯示顏色
     * @return Entity
     */
    private Entity createLethalTrigger(
            SpawnData data,
            double width,
            double height,
            DeathReason deathReason,
            Color debugColor
    ) {
        return entityBuilder(data)
                .type(EntityType.DEATH_ZONE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createDebugView(width, height, debugColor))
                .with(new CollidableComponent(true))
                .with(new LethalComponent(deathReason))
                .zIndex(Z_DEBUG_TRIGGER)
                .build();
    }


    // =========================================================
    // Physics Helpers
    // =========================================================

    /**
     * 建立掉落物 PhysicsComponent。
     *
     * 掉落物：
     * - DYNAMIC
     * - category = FALLING_OBJECT
     * - mask = FLOOR
     *
     * 代表掉落物只會撞地板，不會與玩家或牆壁產生物理推擠。
     *
     * @return PhysicsComponent
     */
    private PhysicsComponent createFallingObjectPhysics() {
        PhysicsComponent physics = new PhysicsComponent();

        physics.setBodyType(BodyType.DYNAMIC);
        physics.setFixtureDef(createFallingObjectFixtureDef());

        return physics;
    }

    /**
     * 建立掉落物 FixtureDef。
     *
     * @return FixtureDef
     */
    private FixtureDef createFallingObjectFixtureDef() {
        FixtureDef fixtureDef = new FixtureDef()
                .density(FALLING_OBJECT_DENSITY)
                .friction(FALLING_OBJECT_FRICTION)
                .restitution(FALLING_OBJECT_RESTITUTION);

        FixtureFilterUtil.applyFilter(
                fixtureDef,
                CollisionCategory.FALLING_OBJECT,
                CollisionCategory.FLOOR
        );

        return fixtureDef;
    }


    // =========================================================
    // View Helpers
    // =========================================================

    /**
     * 建立一般純填色矩形。
     *
     * @param width 寬度
     * @param height 高度
     * @param fill 填色
     * @return Rectangle
     */
    private Rectangle createFilledRectangle(
            double width,
            double height,
            Color fill
    ) {
        return new Rectangle(width, height, fill);
    }

    /**
     * 建立圓角描邊矩形。
     *
     * 用於暫代街道物件美術。
     *
     * @param width 寬度
     * @param height 高度
     * @param arc 圓角
     * @param fill 填色
     * @param stroke 描邊色
     * @param strokeWidth 描邊寬度
     * @return Rectangle
     */
    private Rectangle createRoundedStrokeRectangle(
            double width,
            double height,
            double arc,
            Color fill,
            Color stroke,
            double strokeWidth
    ) {
        Rectangle view = new Rectangle(width, height);

        view.setArcWidth(arc);
        view.setArcHeight(arc);
        view.setFill(fill);
        view.setStroke(stroke);
        view.setStrokeWidth(strokeWidth);

        return view;
    }

    /**
     * 建立掉落物視覺。
     *
     * 目前使用色塊暫代。
     * 未來可依 FallingObjectVariant 改成不同貼圖。
     *
     * @param width 寬度
     * @param height 高度
     * @param variant 掉落物種類
     * @return Rectangle
     */
    private Rectangle createFallingObjectView(
            double width,
            double height,
            FallingObjectVariant variant
    ) {
        Color fill;
        Color stroke;

        switch (variant) {
            case FRIDGE -> {
                fill = Color.rgb(190, 60, 55);
                stroke = Color.rgb(255, 240, 180);
            }
            case HELI -> {
                fill = Color.rgb(120, 70, 45);
                stroke = Color.rgb(255, 170, 110);
            }
            default -> {
                fill = Color.GRAY;
                stroke = Color.WHITE;
            }
        }

        return createRoundedStrokeRectangle(
                width,
                height,
                8,
                fill,
                stroke,
                2.2
        );
    }

    /**
     * 建立 devMode 顯示框。
     *
     * devMode == true：
     * - 顯示指定大小與顏色的半透明矩形。
     *
     * devMode == false：
     * - 回傳透明 0x0 Rectangle。
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
    // SpawnData Helpers
    // =========================================================

    /**
     * 從 SpawnData 讀取 double。
     *
     * 若 key 不存在，回傳 defaultValue。
     */
    private double getDouble(
            SpawnData data,
            String key,
            double defaultValue
    ) {
        return data.hasKey(key) ? data.get(key) : defaultValue;
    }

    /**
     * 從 SpawnData 讀取 boolean。
     *
     * 若 key 不存在，回傳 defaultValue。
     */
    private boolean getBoolean(
            SpawnData data,
            String key,
            boolean defaultValue
    ) {
        return data.hasKey(key) ? data.get(key) : defaultValue;
    }

    /**
     * 從 SpawnData 讀取 StreetApartmentStyle。
     *
     * @param data SpawnData
     * @return StreetApartmentStyle
     */
    private StreetApartmentStyle getApartmentStyle(SpawnData data) {
        return StreetApartmentStyle.valueOf(data.get("style"));
    }

    /**
     * 從 SpawnData 讀取 FallingObjectVariant。
     *
     * @param data SpawnData
     * @return FallingObjectVariant
     */
    private FallingObjectVariant getFallingObjectVariant(SpawnData data) {
        return FallingObjectVariant.valueOf(data.get("variant"));
    }


    // =========================================================
    // Apartment Color Helpers
    // =========================================================

    /**
     * 取得公寓背景層顏色。
     *
     * 目前使用色塊暫代建築素材。
     */
    private Color getApartmentBackgroundColor(StreetApartmentStyle style) {
        return switch (style) {
            case LEFT -> Color.rgb(150, 95, 80);
            case RIGHT -> Color.rgb(110, 130, 160);
            case CENTER -> Color.rgb(130, 110, 150);
            case FILL -> Color.rgb(145, 145, 105);
            case EMPTY -> Color.TRANSPARENT;
        };
    }

    /**
     * 取得公寓前景層顏色。
     *
     * 目前使用半透明色塊暫代建築前景。
     */
    private Color getApartmentForegroundColor(StreetApartmentStyle style) {
        return switch (style) {
            case LEFT -> Color.rgb(220, 160, 120, 0.45);
            case RIGHT -> Color.rgb(170, 210, 240, 0.45);
            case CENTER -> Color.rgb(220, 180, 240, 0.45);
            case FILL -> Color.rgb(230, 230, 160, 0.45);
            case EMPTY -> Color.TRANSPARENT;
        };
    }
}