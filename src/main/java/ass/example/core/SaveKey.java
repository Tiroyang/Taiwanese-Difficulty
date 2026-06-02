package ass.example.core;

/**
 * SaveKey
 *
 * 存檔系統使用的 key 常數集合。
 *
 * 用途：
 * 1. 避免在 SaveSystem / LoadSystem / SceneSystem 中到處硬寫字串。
 * 2. 降低 key 打錯造成存檔讀不到的風險。
 * 3. 讓所有存檔欄位集中管理。
 *
 * 注意：
 * 此類別只保存常數，不允許建立實例。
 */
public final class SaveKey {

    // =========================================================
    // Constructor
    // =========================================================

    /**
     * 工具類別不允許建立實例。
     */
    private SaveKey() {
    }


    // =========================================================
    // Bundle
    // =========================================================

    /**
     * FXGL Bundle 名稱。
     */
    public static final String BUNDLE_NAME = "storySaveData";


    // =========================================================
    // Scene
    // =========================================================

    /**
     * 目前場景類型。
     */
    public static final String SCENE_TYPE = "sceneType";


    // =========================================================
    // Player
    // =========================================================

    /**
     * 玩家 X 座標。
     */
    public static final String PLAYER_X = "playerX";

    /**
     * 玩家 Y 座標。
     */
    public static final String PLAYER_Y = "playerY";

    /**
     * 玩家是否死亡。
     */
    public static final String PLAYER_DEAD = "playerDead";

    /**
     * 上一次死亡原因。
     */
    public static final String LAST_DEATH_REASON = "lastDeathReason";

    /**
     * 死亡次數。
     */
    public static final String DEATH_COUNT = "deathCount";


    // =========================================================
    // Thumbnail / Metadata
    // =========================================================

    /**
     * 存檔縮圖 Base64。
     */
    public static final String THUMBNAIL_BASE64 = "thumbnailBase64";

    /**
     * 存檔時間。
     */
    public static final String SAVED_AT = "savedAt";


    // =========================================================
    // Quest
    // =========================================================

    /**
     * 任務 HUD 目前可見起始 index。
     */
    public static final String QUEST_VISIBLE_START_INDEX = "questVisibleStartIndex";

    /**
     * 任務進度 key 前綴。
     *
     * 完整格式：
     * quest_amount_{QuestType.name()}
     */
    public static final String QUEST_AMOUNT_PREFIX = "quest_amount_";

    /**
     * 任務完成狀態 key 前綴。
     *
     * 完整格式：
     * quest_completed_{QuestType.name()}
     */
    public static final String QUEST_COMPLETED_PREFIX = "quest_completed_";

    /**
     * 任務完成動畫是否已播放 key 前綴。
     *
     * 完整格式：
     * quest_anim_played_{QuestType.name()}
     */
    public static final String QUEST_ANIM_PLAYED_PREFIX = "quest_anim_played_";


    // =========================================================
    // HouseScene - Props / Interactions
    // =========================================================

    /**
     * 棉被是否已折。
     */
    public static final String QUILT_FOLDED = "quiltFolded";

    /**
     * 水是否已喝。
     */
    public static final String WATER_DRUNK = "waterDrunk";

    /**
     * 是否已刷牙。
     */
    public static final String TEETH_BRUSHED = "teethBrushed";

    /**
     * 玩家是否穿鞋。
     */
    public static final String SHOES_WORN = "shoesWorn";

    /**
     * 玩家是否在床 collider 上。
     */
    public static final String PLAYER_ON_BED_COLLIDER = "playerOnBedCollider";


    // =========================================================
    // HouseScene - Rooms
    // =========================================================

    /**
     * 客廳是否已揭露。
     */
    public static final String ROOM_LIVING_ROOM_REVEALED = "room_LIVING_ROOM_revealed";

    /**
     * 廁所是否已揭露。
     */
    public static final String ROOM_TOILET_REVEALED = "room_TOILET_revealed";


    // =========================================================
    // HouseScene - Doors
    // =========================================================

    /**
     * Door1 是否開啟。
     */
    public static final String DOOR_1_OPENED = "door_Door1_opened";

    /**
     * Door2 是否開啟。
     */
    public static final String DOOR_2_OPENED = "door_Door2_opened";


    // =========================================================
    // StreetScene
    // =========================================================

    /**
     * 街道區段資料。
     */
    public static final String STREET_SEGMENTS = "streetSegments";

    /**
     * 街道已生成的最左側 X。
     */
    public static final String STREET_LEFT_MOST_GENERATED_X = "streetLeftMostGeneratedX";

    /**
     * 街道區段生成是否已達上限。
     */
    public static final String STREET_SEGMENT_LIMIT_REACHED = "streetSegmentLimitReached";

    /**
     * 街道障礙物資料。
     */
    public static final String STREET_OBSTACLES = "streetObstacles";


    // =========================================================
    // Helper Methods
    // =========================================================

    /**
     * 取得指定任務的進度 key。
     *
     * @param questType 任務類型
     * @return 任務進度 key
     */
    public static String questAmount(QuestType questType) {
        return QUEST_AMOUNT_PREFIX + questType.name();
    }

    /**
     * 取得指定任務的完成狀態 key。
     *
     * @param questType 任務類型
     * @return 任務完成狀態 key
     */
    public static String questCompleted(QuestType questType) {
        return QUEST_COMPLETED_PREFIX + questType.name();
    }

    /**
     * 取得指定任務的完成動畫狀態 key。
     *
     * @param questType 任務類型
     * @return 任務完成動畫狀態 key
     */
    public static String questAnimPlayed(QuestType questType) {
        return QUEST_ANIM_PLAYED_PREFIX + questType.name();
    }

    /**
     * 取得指定門的開啟狀態 key。
     *
     * @param doorId 門 ID，例如 Door1
     * @return 門開啟狀態 key
     */
    public static String doorOpened(String doorId) {
        return "door_" + doorId + "_opened";
    }
}