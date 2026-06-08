package ass.example.core.HouseScene;

/**
 * RoomType
 *
 * HouseScene 中的房間類型。
 *
 * 用途：
 * 1. RoomSystem 判斷玩家目前所在房間。
 * 2. RoomSystem 控制房間遮罩 reveal。
 * 3. 判斷玩家離開房間時是否觸發死亡規則。
 * 4. 作為房間相關 game var key 的一部分。
 */
public enum RoomType {

    /**
     * 廁所。。
     */
    TOILET,

    /**
     * 走廊。
     */
    HALLWAY,

    /**
     * 客廳。
     */
    LIVING_ROOM,

    /**
     * 臥室。
     */
    BEDROOM,

    /**
     * 玄關。
     */
    FOYER,

    /**
     * 無房間。
     *
     * 用於：
     * 1. 玩家不在任何已定義房間內。
     * 2. 預設值。
     * 3. 防止 null 判斷。
     */
    NONE
}