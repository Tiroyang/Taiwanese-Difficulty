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
 *
 * 例如：
 *
 * room_LIVING_ROOM_revealed
 * room_TOILET_revealed
 */
public enum RoomType {

    /**
     * 廁所。
     *
     * 通常位於場景右側。
     */
    TOILET,

    /**
     * 走廊。
     *
     * 通常是連接臥室、客廳、玄關等區域的中間空間。
     */
    HALLWAY,

    /**
     * 客廳。
     *
     * 通常由 Door1 開門後 reveal。
     */
    LIVING_ROOM,

    /**
     * 臥室。
     *
     * 玩家起床與棉被任務所在房間。
     */
    BEDROOM,

    /**
     * 玄關。
     *
     * 鞋櫃、出口門通常位於此區域。
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