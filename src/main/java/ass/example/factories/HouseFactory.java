package ass.example.factories;

import ass.example.Main;
import ass.example.components.HouseScene.*;
import ass.example.components.InteractableComponent;
import ass.example.core.CollisionCategory;
import ass.example.core.DeathReason;
import ass.example.core.EntityType;
import ass.example.core.FixtureFilterUtil;
import ass.example.core.QuestType;
import ass.example.core.SoundId;
import ass.example.scenes.HouseScene;
import ass.example.scenes.SceneManager;
import ass.example.system.AudioSystem;
import ass.example.system.DeathSystem;
import ass.example.system.LanguageSystem;
import ass.example.system.dialogue.DialogueSystem;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * HouseFactory
 *
 * HouseScene 專用 EntityFactory。
 *
 * 負責生成家中場景使用的所有 Entity：
 *
 * 1. 背景圖層
 *    - house_background
 *    - house_floor
 *    - house_ceiling
 *    - house_foreground
 *    - window_view
 *
 * 2. 門與門碰撞
 *    - door
 *    - door_collider
 *    - exit_door
 *
 * 3. 床相關物件
 *    - bed
 *    - bed_one_way_platform
 *    - bed_one_way_platform_collider
 *
 * 4. 互動道具
 *    - quilt
 *    - quilt_trigger
 *    - water
 *    - water_trigger
 *    - toothbrush_trigger
 *    - shoe_cabinet
 *    - shoe_trigger
 *
 * 5. 角色與對話
 *    - mom
 *    - mom_trigger
 *
 * 6. 純視覺道具
 *    - cabinet
 *    - kitchen
 *
 * 設計原則：
 * - Factory 只負責 Entity 組裝。
 * - 實際互動邏輯盡量交給 Component 或 System。
 * - 重複的 devMode 碰撞框、SpawnData 預設值讀取集中成共用方法。
 */
public class HouseFactory implements EntityFactory {

    // =========================================================
    // Texture Paths - Map
    // =========================================================

    private static final String TEXTURE_HOUSE_BACKGROUND = "/Scene1/map/Background.png";
    private static final String TEXTURE_HOUSE_FLOOR = "/Scene1/map/Floor.png";
    private static final String TEXTURE_HOUSE_CEILING = "/Scene1/map/Ceiling.png";
    private static final String TEXTURE_HOUSE_FOREGROUND = "/Scene1/map/Foreground.png";


    // =========================================================
    // Texture Paths - Props
    // =========================================================

    private static final String TEXTURE_QUILT_DEFAULT = "Scene1/props/Quilt.png";
    private static final String TEXTURE_QUILT_FOLDED = "Scene1/props/Quilt_folded.png";

    private static final String TEXTURE_BED = "Scene1/props/Bed.png";

    private static final String TEXTURE_CABINET = "/Scene1/props/Cabinet.png";
    private static final String TEXTURE_KITCHEN = "Scene1/props/Kitchen.png";

    private static final String TEXTURE_WATER = "Scene1/props/Water.png";

    private static final String TEXTURE_SHOES_DEFAULT = "Scene1/props/Shoes.png";
    private static final String TEXTURE_SHOES_WORN = "Scene1/props/Shoes_worn.png";

    private static final String TEXTURE_MOM = "/assets/textures/characters/mom/mom.png";


    // =========================================================
    // Default Interaction Settings
    // =========================================================

    private static final double DEFAULT_INTERACT_RANGE = 180.0;
    private static final boolean DEFAULT_PROMPT_ON_ENTITY = true;
    private static final double DEFAULT_PROMPT_OFFSET_Y = 45.0;


    // =========================================================
    // Z Index
    // =========================================================

    private static final int Z_WINDOW_VIEW = -300;
    private static final int Z_BACKGROUND = -200;
    private static final int Z_HOUSE_FLOOR = 1;
    private static final int Z_HOUSE_CEILING = -2;
    private static final int Z_HOUSE_FOREGROUND = 200;

    private static final int Z_INTERACTION_TRIGGER = 1000;
    private static final int Z_DOOR = -1;
    private static final int Z_BED = -3;
    private static final int Z_QUILT = -3;
    private static final int Z_WATER = -2;
    private static final int Z_SHOE_CABINET = -5;
    private static final int Z_CABINET = -1;
    private static final int Z_KITCHEN = -100;
    private static final int Z_MOM = -150;


    // =========================================================
    // Dev View Colors
    // =========================================================

    private static final Color COLOR_TRIGGER_DEBUG = Color.rgb(255, 255, 0, 0.35);
    private static final Color COLOR_DOOR_COLLIDER_DEBUG = Color.rgb(37, 255, 0, 0.5);
    private static final Color COLOR_BED_PLATFORM_DEBUG = Color.rgb(0, 180, 255, 0.35);
    private static final Color COLOR_BED_COLLIDER_DEBUG = Color.rgb(0, 255, 196, 0.35);
    private static final Color COLOR_BATHTUB_SENSOR_DEBUG = Color.rgb(0, 180, 255, 0.32);


    // =========================================================
    // Spawn - Map Background
    // =========================================================

    /**
     * 生成家中背景底圖。
     */
    @Spawns("house_background")
    public Entity newHouseBackground(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view(TEXTURE_HOUSE_BACKGROUND)
                .zIndex(Z_BACKGROUND)
                .build();
    }

    /**
     * 生成家中地板視覺圖。
     */
    @Spawns("house_floor")
    public Entity newHouseFloor(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view(TEXTURE_HOUSE_FLOOR)
                .zIndex(Z_HOUSE_FLOOR)
                .build();
    }

    /**
     * 生成家中天花板視覺圖。
     */
    @Spawns("house_ceiling")
    public Entity newHouseCeiling(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view(TEXTURE_HOUSE_CEILING)
                .zIndex(Z_HOUSE_CEILING)
                .build();
    }

    /**
     * 生成家中前景圖層。
     */
    @Spawns("house_foreground")
    public Entity newHouseForeground(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.MAP_BACKGROUND)
                .view(TEXTURE_HOUSE_FOREGROUND)
                .zIndex(Z_HOUSE_FOREGROUND)
                .build();
    }

    /**
     * 生成窗外視差背景。
     *
     * 這裡使用 HouseScene 內部的 ParallaxWindowComponent。
     *
     * SpawnData 需要：
     * - texture：窗外背景貼圖
     * - parallaxFactor：視差倍率
     */
    @Spawns("window_view")
    public Entity newWindowView(SpawnData data) {
        double baseX = data.getX();
        double baseY = data.getY();

        String texture = data.get("texture");
        double parallaxFactor = getDouble(data, "parallaxFactor", 0.01);

        return entityBuilder(data)
                .view(texture)
                .with(new ParallaxWindowComponent(
                        baseX,
                        baseY,
                        parallaxFactor
                ))
                .zIndex(Z_WINDOW_VIEW)
                .build();
    }


    // =========================================================
    // Spawn - Doors
    // =========================================================

    /**
     * 生成一般房間門。
     *
     * 門本身：
     * - 顯示關門 / 開門貼圖。
     * - 掛上 DoorComponent 管理狀態。
     * - 掛上 InteractableComponent 讓玩家可互動。
     *
     * 門關閉時真正阻擋玩家的 collider，
     * 由 DoorComponent 另外 spawn("door_collider") 生成。
     */
    @Spawns("door")
    public Entity newDoor(SpawnData data) {
        String id = data.get("id");

        String closedTexture = data.get("closedTexture");
        String openTexture = data.get("openTexture");

        double colliderOffsetX = data.get("colliderOffsetX");
        double colliderOffsetY = data.get("colliderOffsetY");
        double colliderWidth = data.get("colliderWidth");
        double colliderHeight = data.get("colliderHeight");

        double interactRange = getDouble(data, "interactRange", 120.0);
        boolean promptOnEntity = getBoolean(data, "promptOnEntity", false);
        double promptOffsetY = getDouble(data, "promptOffsetY", 35.0);

        AudioSystem audioSystem = getAudioSystem(data);

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
                        () -> doorComponent.isOpened()
                                ? "story.house.closeDoor"
                                : "story.house.openDoor",
                        doorComponent::toggle,
                        interactRange,
                        promptOnEntity,
                        promptOffsetY
                ))
                .zIndex(Z_DOOR)
                .build();
    }

    /**
     * 生成門關閉時的阻擋碰撞箱。
     *
     * 此 Entity 由 DoorComponent 動態生成與移除。
     */
    @Spawns("door_collider")
    public Entity newDoorCollider(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        PhysicsComponent physics = createStaticPhysics(createWallFixtureDef());

        return entityBuilder(data)
                .type(EntityType.DOOR_COLLIDER)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createDebugRectangle(width, height, COLOR_DOOR_COLLIDER_DEBUG))
                .with(physics)
                .with(new CollidableComponent(true))
                .zIndex(Z_INTERACTION_TRIGGER)
                .build();
    }

    /**
     * 生成離開家的門互動區。
     *
     * 互動條件：
     * 1. 玩家不能處於死亡狀態。
     * 2. 穿鞋任務必須完成。
     * 3. shoesWorn 必須為 true。
     *
     * 條件成立時：
     * - 播放 House -> Street 過場。
     * - 完成 EXIT_HOUSE 任務。
     */
    @Spawns("exit_door")
    public Entity newExitDoor(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        double interactRange = getDouble(data, "interactRange", DEFAULT_INTERACT_RANGE);
        boolean promptOnEntity = getBoolean(data, "promptOnEntity", false);
        double promptOffsetY = getDouble(data, "promptOffsetY", 35.0);

        SceneManager sceneManager = data.get("sceneManager");

        LanguageSystem languageSystem = LanguageSystem.getInstance();
        QuestSystem questSystem = QuestSystem.getInstance();

        return entityBuilder(data)
                .type(EntityType.INTERACTABLE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createInteractableDebugView(width, height))
                .with(new InteractableComponent(
                        () -> "story.house.exit",
                        () -> tryExitHouse(sceneManager, questSystem, languageSystem),
                        interactRange,
                        promptOnEntity,
                        promptOffsetY
                ))
                .zIndex(Z_INTERACTION_TRIGGER)
                .build();
    }

    /**
     * 嘗試離開家。
     *
     * 若條件不足，顯示提示。
     * 若條件足夠，播放轉場。
     */
    private void tryExitHouse(
            SceneManager sceneManager,
            QuestSystem questSystem,
            LanguageSystem languageSystem
    ) {
        if (getb("playerDead")) {
            return;
        }

        boolean wearShoesQuestCompleted =
                questSystem.isCompleted(QuestType.FOLD_QUILT) &&
                questSystem.isCompleted(QuestType.BRUSH_TEETH) &&
                questSystem.isCompleted(QuestType.TALK_TO_MOM) &&
                questSystem.isCompleted(QuestType.WEAR_SHOES);
        boolean shoesWorn = getb("shoesWorn");

        if (!wearShoesQuestCompleted || !shoesWorn) {
            showExitLockedNotice(languageSystem);
            return;
        }

        sceneManager.playHouseToStreetTransition(() ->
                questSystem.completeQuest(QuestType.EXIT_HOUSE)
        );
    }


    // =========================================================
    // Spawn - Quilt
    // =========================================================

    /**
     * 生成棉被視覺物件。
     *
     * 真正互動由 quilt_trigger 負責。
     */
    @Spawns("quilt")
    public Entity newQuiltVisual(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.INTERACTABLE)
                .view(TEXTURE_QUILT_DEFAULT)
                .zIndex(Z_QUILT)
                .build();
    }

    /**
     * 生成棉被互動 trigger。
     *
     * 功能：
     * - 玩家互動後折棉被。
     * - QuiltComponent 負責切換視覺與更新任務 / 存檔狀態。
     */
    @Spawns("quilt_trigger")
    public Entity newQuiltTrigger(SpawnData data) {
        Entity quiltVisual = data.get("visual");

        double width = data.get("width");
        double height = data.get("height");

        double interactRange = getDouble(data, "interactRange", 150.0);
        boolean promptOnEntity = getBoolean(data, "promptOnEntity", true);
        double promptOffsetY = getDouble(data, "promptOffsetY", 40.0);

        AudioSystem audioSystem = getAudioSystem(data);

        QuiltComponent quiltComponent = new QuiltComponent(
                quiltVisual,
                TEXTURE_QUILT_DEFAULT,
                TEXTURE_QUILT_FOLDED,
                audioSystem
        );

        return entityBuilder(data)
                .type(EntityType.TRIGGER)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createInteractableDebugView(width, height))
                .with(quiltComponent)
                .with(new InteractableComponent(
                        () -> "story.house.foldQuilt",
                        quiltComponent::fold,
                        interactRange,
                        promptOnEntity,
                        promptOffsetY
                ))
                .zIndex(Z_INTERACTION_TRIGGER)
                .build();
    }


    // =========================================================
    // Spawn - Bed
    // =========================================================

    /**
     * 生成床視覺物件。
     *
     * 實際站上床、跳床死亡等邏輯，
     * 由 bed_one_way_platform 與 BedSystem 負責。
     */
    @Spawns("bed")
    public Entity newBed(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.PROP)
                .view(TEXTURE_BED)
                .zIndex(Z_BED)
                .build();
    }

    /**
     * 生成床的一方通行入口平台。
     *
     * 這個 Entity 是固定存在的「偵測入口」。
     * 玩家從上方落到這個區域後，
     * BedSystem 會動態生成 bed_one_way_platform_collider。
     */
    @Spawns("bed_one_way_platform")
    public Entity newBedOneWayPlatform(SpawnData data) {
        String id = data.get("id");

        double width = data.get("width");
        double height = data.get("height");

        BedComponent.ColliderArea firstColliderArea = readFirstBedColliderArea(data);
        BedComponent.ColliderArea secondColliderArea = readSecondBedColliderArea(data);

        int playerZIndexOnBed = getInt(data, "playerZIndexOnBed", -3);
        int normalPlayerZIndex = getInt(data, "normalPlayerZIndex", 0);

        DeathReason deathReason = data.get("deathReason");

        BedComponent bedComponent = new BedComponent(
                BedComponent.Role.PLATFORM,
                id,
                width,
                height,
                firstColliderArea,
                secondColliderArea,
                playerZIndexOnBed,
                normalPlayerZIndex,
                deathReason
        );

        return entityBuilder(data)
                .type(EntityType.BED_ONE_WAY_PLATFORM)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createDebugRectangle(width, height, COLOR_BED_PLATFORM_DEBUG))
                .with(new CollidableComponent(true))
                .with(bedComponent)
                .zIndex(Z_INTERACTION_TRIGGER)
                .build();
    }

    /**
     * 生成床實體 collider。
     *
     * 這個 Entity 由 BedSystem 動態生成。
     *
     * 用途：
     * - 真正支撐玩家站在床上。
     * - 玩家離開床、下落、死亡重生時會被移除。
     */
    @Spawns("bed_one_way_platform_collider")
    public Entity newBedOneWayPlatformCollider(SpawnData data) {
        String id = data.get("id");

        double width = data.get("width");
        double height = data.get("height");

        int playerZIndexOnBed = getInt(data, "playerZIndexOnBed", -3);
        int normalPlayerZIndex = getInt(data, "normalPlayerZIndex", 0);

        DeathReason deathReason = data.get("deathReason");

        PhysicsComponent physics = createStaticPhysics(createBedFixtureDef());

        BedComponent.ColliderArea firstColliderArea = new BedComponent.ColliderArea(
                0,
                0,
                width,
                height
        );

        BedComponent bedComponent = new BedComponent(
                BedComponent.Role.COLLIDER,
                id,
                0,
                0,
                firstColliderArea,
                new BedComponent.ColliderArea(0, 0, 0, 0),
                playerZIndexOnBed,
                normalPlayerZIndex,
                deathReason
        );

        return entityBuilder(data)
                .type(EntityType.BED_ONE_WAY_PLATFORM_COLLIDER)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createDebugRectangle(width, height, COLOR_BED_COLLIDER_DEBUG))
                .with(physics)
                .with(new CollidableComponent(true))
                .with(bedComponent)
                .zIndex(Z_INTERACTION_TRIGGER)
                .build();
    }

    /**
     * 從 SpawnData 讀取第一組床 collider 資料。
     */
    private BedComponent.ColliderArea readFirstBedColliderArea(SpawnData data) {
        return new BedComponent.ColliderArea(
                getDouble(data, "collider1OffsetX", 0.0),
                getDouble(data, "collider1OffsetY", 0.0),
                getDouble(data, "collider1Width", 0.0),
                getDouble(data, "collider1Height", 0.0)
        );
    }

    /**
     * 從 SpawnData 讀取第二組床 collider 資料。
     *
     * 若沒有提供第二組資料，
     * 預設寬高為 0，代表不生成第二組 collider。
     */
    private BedComponent.ColliderArea readSecondBedColliderArea(SpawnData data) {
        return new BedComponent.ColliderArea(
                getDouble(data, "collider2OffsetX", 0.0),
                getDouble(data, "collider2OffsetY", 0.0),
                getDouble(data, "collider2Width", 0.0),
                getDouble(data, "collider2Height", 0.0)
        );
    }


    // =========================================================
    // Spawn - Water / Bathtub / Toothbrush
    // =========================================================

    /**
     * 生成水的視覺物件。
     *
     * 真正互動由 water_trigger 負責。
     */
    @Spawns("water")
    public Entity newWater(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.INTERACTABLE)
                .view(TEXTURE_WATER)
                .zIndex(Z_WATER)
                .build();
    }

    /**
     * 生成喝水互動 trigger。
     */
    @Spawns("water_trigger")
    public Entity newWaterTrigger(SpawnData data) {
        Entity visual = data.get("visual");
        Entity player = data.get("player");
        DeathSystem deathSystem = data.get("deathSystem");

        double width = data.get("width");
        double height = data.get("height");

        double interactRange = getDouble(data, "interactRange", 130.0);
        boolean promptOnEntity = getBoolean(data, "promptOnEntity", true);
        double promptOffsetY = getDouble(data, "promptOffsetY", 40.0);

        AudioSystem audioSystem = getAudioSystemOrNull(data);

        WaterComponent waterComponent = new WaterComponent(
                visual,
                player,
                deathSystem,
                audioSystem
        );

        return entityBuilder(data)
                .type(EntityType.INTERACTABLE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createInteractableDebugView(width, height))
                .with(waterComponent)
                .with(new InteractableComponent(
                        () -> "story.house.drinkWater",
                        waterComponent::drink,
                        interactRange,
                        promptOnEntity,
                        promptOffsetY
                ))
                .zIndex(Z_INTERACTION_TRIGGER)
                .build();
    }

    /**
     * 生成浴缸死亡 sensor。
     *
     * 玩家高速落入此 sensor 時，
     * BathtubComponent 會呼叫 DeathSystem 觸發死亡。
     */
    @Spawns("bathtub_sensor")
    public Entity newBathtubSensor(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        Entity player = data.get("player");
        DeathSystem deathSystem = data.get("deathSystem");
        DeathReason deathReason = data.get("deathReason");

        double deathSpeedThreshold = getDouble(
                data,
                "deathSpeedThreshold",
                520.0
        );

        return entityBuilder(data)
                .type(EntityType.TRIGGER)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createDebugRectangle(width, height, COLOR_BATHTUB_SENSOR_DEBUG))
                .with(new CollidableComponent(true))
                .with(new BathtubComponent(
                        player,
                        deathSystem,
                        deathReason,
                        deathSpeedThreshold
                ))
                .zIndex(Z_INTERACTION_TRIGGER)
                .build();
    }

    /**
     * 生成刷牙互動 trigger。
     *
     * 若 teethBrushed == true，
     * InteractableComponent 的條件會讓互動不可再次執行。
     */
    @Spawns("toothbrush_trigger")
    public Entity newToothbrushTrigger(SpawnData data) {
        double width = data.get("width");
        double height = data.get("height");

        double interactRange = getDouble(data, "interactRange", DEFAULT_INTERACT_RANGE);
        boolean promptOnEntity = getBoolean(data, "promptOnEntity", DEFAULT_PROMPT_ON_ENTITY);
        double promptOffsetY = getDouble(data, "promptOffsetY", 40.0);

        AudioSystem audioSystem = getAudioSystem(data);

        return entityBuilder(data)
                .type(EntityType.INTERACTABLE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createInteractableDebugView(width, height))
                .with(new InteractableComponent(
                        () -> "story.house.brush_teeth",
                        () -> brushTeeth(audioSystem),
                        interactRange,
                        promptOnEntity,
                        promptOffsetY,
                        () -> !getb("teethBrushed")
                ))
                .zIndex(Z_INTERACTION_TRIGGER)
                .build();
    }

    /**
     * 執行刷牙互動。
     */
    private void brushTeeth(AudioSystem audioSystem) {
        audioSystem.playSFX(SoundId.BRUSHING_TEETH);

        set("teethBrushed", true);

        QuestSystem.getInstance().completeQuest(QuestType.BRUSH_TEETH);
    }


    // =========================================================
    // Spawn - Shoes
    // =========================================================

    /**
     * 生成鞋櫃視覺物件。
     *
     * 真正互動由 shoe_trigger 負責。
     */
    @Spawns("shoe_cabinet")
    public Entity newShoeCabinet(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.PROP)
                .view(TEXTURE_SHOES_DEFAULT)
                .zIndex(Z_SHOE_CABINET)
                .build();
    }

    /**
     * 生成鞋櫃互動 trigger。
     *
     * 功能：
     * - 穿鞋 / 脫鞋。
     * - 更新鞋櫃貼圖。
     * - 更新玩家外觀。
     * - 更新 shoesWorn 變數。
     */
    @Spawns("shoe_trigger")
    public Entity newShoeTrigger(SpawnData data) {
        Entity shoeVisual = data.get("visual");
        Entity player = data.get("player");

        double width = data.get("width");
        double height = data.get("height");

        double interactRange = getDouble(data, "interactRange", DEFAULT_INTERACT_RANGE);
        boolean promptOnEntity = getBoolean(data, "promptOnEntity", DEFAULT_PROMPT_ON_ENTITY);
        double promptOffsetY = getDouble(data, "promptOffsetY", DEFAULT_PROMPT_OFFSET_Y);

        AudioSystem audioSystem = getAudioSystem(data);

        ShoeComponent shoeComponent = new ShoeComponent(
                shoeVisual,
                player,
                TEXTURE_SHOES_DEFAULT,
                TEXTURE_SHOES_WORN,
                audioSystem
        );

        return entityBuilder(data)
                .type(EntityType.INTERACTABLE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createInteractableDebugView(width, height))
                .with(shoeComponent)
                .with(new InteractableComponent(
                        () -> shoeComponent.isWorn()
                                ? "story.house.takeOffShoes"
                                : "story.house.wearShoes",
                        shoeComponent::toggle,
                        interactRange,
                        promptOnEntity,
                        promptOffsetY
                ))
                .zIndex(Z_INTERACTION_TRIGGER)
                .build();
    }


    // =========================================================
    // Spawn - Static Visual Props
    // =========================================================

    /**
     * 生成櫃子視覺物件。
     */
    @Spawns("cabinet")
    public Entity newCabinet(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.PROP)
                .view(TEXTURE_CABINET)
                .zIndex(Z_CABINET)
                .build();
    }

    /**
     * 生成廚房視覺物件。
     */
    @Spawns("kitchen")
    public Entity newKitchen(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.PROP)
                .view(TEXTURE_KITCHEN)
                .zIndex(Z_KITCHEN)
                .build();
    }


    // =========================================================
    // Spawn - Mom / Dialogue
    // =========================================================

    /**
     * 生成媽媽角色視覺物件。
     */
    @Spawns("mom")
    public Entity newMom(SpawnData data) {
        double height = getDouble(data, "height", 282.0);

        ImageView view = createImageView(TEXTURE_MOM, height);

        return entityBuilder(data)
                .type(EntityType.PROP)
                .view(view)
                .zIndex(Z_MOM)
                .build();
    }

    /**
     * 生成媽媽對話 trigger。
     */
    @Spawns("mom_trigger")
    public Entity newMomTrigger(SpawnData data) {
        Entity player = data.get("player");

        double width = data.get("width");
        double height = data.get("height");

        double interactRange = getDouble(data, "interactRange", DEFAULT_INTERACT_RANGE);
        boolean promptOnEntity = getBoolean(data, "promptOnEntity", DEFAULT_PROMPT_ON_ENTITY);
        double promptOffsetY = getDouble(data, "promptOffsetY", DEFAULT_PROMPT_OFFSET_Y);

        String sceneBgmPath = getString(
                data,
                "sceneBgmPath",
                "/assets/music/stage/house_bgm.mp3"
        );

        String dialogueBgmPath = getString(
                data,
                "dialogueBgmPath",
                "/assets/music/dialogue/mom_theme.mp3"
        );

        return entityBuilder(data)
                .type(EntityType.INTERACTABLE)
                .bbox(new HitBox(BoundingShape.box(width, height)))
                .view(createInteractableDebugView(width, height))
                .with(new InteractableComponent(
                        () -> "story.house.talkToMom",
                        () -> startMomDialogue(player, sceneBgmPath, dialogueBgmPath),
                        interactRange,
                        promptOnEntity,
                        promptOffsetY
                ))
                .zIndex(Z_INTERACTION_TRIGGER)
                .build();
    }

    /**
     * 開始媽媽對話。
     */
    private void startMomDialogue(
            Entity player,
            String sceneBgmPath,
            String dialogueBgmPath
    ) {
        DialogueSystem.getInstance().startDialogue(
                "mom_001",
                player,
                sceneBgmPath,
                dialogueBgmPath
        );
    }


    // =========================================================
    // Shared View Helpers
    // =========================================================

    /**
     * 建立互動 trigger 的 devMode 顯示框。
     *
     * 正式模式會回傳透明 0x0 Rectangle，
     * 避免顯示碰撞區。
     */
    private Rectangle createInteractableDebugView(double width, double height) {
        return createDebugRectangle(width, height, COLOR_TRIGGER_DEBUG);
    }

    /**
     * 根據 Main.devMode 建立 debug 矩形。
     *
     * devMode == true：
     * - 顯示指定大小與顏色的半透明矩形。
     *
     * devMode == false：
     * - 回傳透明 0x0 矩形。
     */
    private Rectangle createDebugRectangle(
            double width,
            double height,
            Color debugColor
    ) {
        if (Main.devMode) {
            return new Rectangle(width, height, debugColor);
        }

        return new Rectangle(0, 0, Color.TRANSPARENT);
    }

    /**
     * 建立指定高度的 ImageView。
     *
     * 用於角色立繪或比例需要固定的圖片。
     */
    private ImageView createImageView(String resourcePath, double fitHeight) {
        ImageView view = new ImageView(
                new Image(getClass().getResource(resourcePath).toExternalForm())
        );

        view.setFitHeight(fitHeight);
        view.setPreserveRatio(true);
        view.setSmooth(false);

        return view;
    }


    // =========================================================
    // Shared SpawnData Helpers
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
     * 從 SpawnData 讀取 int。
     *
     * FXGL SpawnData 有時會把數字存成 Double 或 Integer，
     * 因此統一用 Number 轉 int。
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
     * 從 SpawnData 讀取 String。
     *
     * 若 key 不存在，回傳 defaultValue。
     */
    private String getString(
            SpawnData data,
            String key,
            String defaultValue
    ) {
        return data.hasKey(key) ? data.get(key) : defaultValue;
    }

    /**
     * 從 SpawnData 讀取 AudioSystem。
     *
     * 若沒有提供 audioSystem，
     * 使用 AudioSystem 單例作為預設值。
     */
    private AudioSystem getAudioSystem(SpawnData data) {
        return data.hasKey("audioSystem")
                ? data.get("audioSystem")
                : AudioSystem.getInstance();
    }

    /**
     * 從 SpawnData 讀取 AudioSystem。
     *
     * 若沒有提供 audioSystem，回傳 null。
     *
     * 用於某些 Component 本身允許 audioSystem 為 null 的情況。
     */
    private AudioSystem getAudioSystemOrNull(SpawnData data) {
        return data.hasKey("audioSystem")
                ? data.get("audioSystem")
                : null;
    }


    // =========================================================
    // Physics Helpers
    // =========================================================

    /**
     * 建立靜態 PhysicsComponent。
     *
     * @param fixtureDef 碰撞材質與碰撞過濾設定
     * @return STATIC PhysicsComponent
     */
    private PhysicsComponent createStaticPhysics(FixtureDef fixtureDef) {
        PhysicsComponent physics = new PhysicsComponent();

        physics.setBodyType(BodyType.STATIC);
        physics.setFixtureDef(fixtureDef);

        return physics;
    }

    /**
     * 建立牆壁用 FixtureDef。
     *
     * 特性：
     * - 無摩擦。
     * - 無彈性。
     * - 只與 PLAYER 碰撞。
     */
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

    /**
     * 建立床 collider 用 FixtureDef。
     *
     * 基礎使用 floor 類型碰撞過濾，
     * 但摩擦與彈性調整成床面手感。
     */
    private FixtureDef createBedFixtureDef() {
        FixtureDef fixtureDef = createFloorFixtureDef();

        fixtureDef.friction(0.8f)
                .restitution(0.5f);

        return fixtureDef;
    }

    /**
     * 建立地板用 FixtureDef。
     *
     * 特性：
     * - 有摩擦。
     * - 少量彈性。
     * - 可與 PLAYER 和 FALLING_OBJECT 碰撞。
     */
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


    // =========================================================
    // Notification Helpers
    // =========================================================

    /**
     * 顯示出口尚未解鎖提示。
     */
    private void showExitLockedNotice(LanguageSystem languageSystem) {
        getNotificationService().pushNotification(
                languageSystem.text("story.house.exit.locked")
        );
    }
}